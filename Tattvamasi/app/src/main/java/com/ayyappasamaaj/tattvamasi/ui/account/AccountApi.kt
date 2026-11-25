package com.ayyappasamaaj.tattvamasi.ui.account

import com.ayyappasamaaj.tattvamasi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The person's ayyappasamaaj.org account, from the app (website: docs/app-sign-in.md). Signing in goes through the
 * website's app sign-in (/_allauth/app/v1/), which answers with a session token; the website's own API
 * (/api/...) then takes that token in the X-Session-Token header, as the dashboard takes its cookie. Debug builds
 * use staging (BuildConfig.SITE_URL), release builds the live site.
 *
 * The parse functions are plain, so they're unit-tested with the website's real answers (AccountApiTest).
 */
object AccountApi {
    val site: String = BuildConfig.SITE_URL

    /** The website's pages the app opens in the browser for what it doesn't do itself. */
    val signUpUrl get() = "$site/accounts/signup/"
    val resetPasswordUrl get() = "$site/accounts/password/reset/"
    val dashboardUrl get() = "$site/app/dashboard/"
    val profileUrl get() = "$site/profile/"

    data class Reply(val code: Int, val body: String)

    /** One request to the website. A network failure is code 0. */
    suspend fun call(method: String, path: String, token: String? = null, body: JSONObject? = null): Reply =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(site + path).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 10000
                connection.readTimeout = 20000
                connection.setRequestProperty("Accept", "application/json")
                if (token != null) connection.setRequestProperty("X-Session-Token", token)
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                try {
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    Reply(code, stream?.reader()?.readText().orEmpty())
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { Reply(0, "") }
        }

    suspend fun signIn(email: String, password: String): SignIn =
        parseSignIn(call("POST", "/_allauth/app/v1/auth/login", body = JSONObject()
            .put("email", email.trim()).put("password", password)))

    /** Continue with Google: the ID token from Google (GoogleSignIn) for the website's own client. */
    suspend fun signInWithGoogle(idToken: String): SignIn =
        parseSignIn(call("POST", "/_allauth/app/v1/auth/provider/token", body = JSONObject()
            .put("provider", "google").put("process", "login")
            .put("token", JSONObject().put("client_id", GoogleSignIn.webClientId).put("id_token", idToken))))

    suspend fun signOut(token: String) {
        call("DELETE", "/_allauth/app/v1/auth/session", token)
    }

    /** Everything My account shows, or null when the token has ended (signed out elsewhere, password changed). */
    suspend fun overview(token: String, year: Int): Overview? {
        val registrations = call("GET", "/api/signups/mine/", token)
        if (registrations.code == 401) return null
        val donations = call("GET", "/api/donations/mine/?page=1", token)
        val summary = call("GET", "/api/core/year-summary/?year=$year", token)
        val info = call("GET", "/api/core/user-info/", token)
        if (donations.code == 401 || summary.code == 401) return null
        return Overview(
            name = if (info.code == 200) parseName(info.body) else null,
            registrations = if (registrations.code == 200) parseRegistrations(registrations.body) else emptyList(),
            donations = if (donations.code == 200) parseDonations(donations.body) else emptyList(),
            year = if (summary.code == 200) parseYear(summary.body) else null,
            failed = listOf(registrations, donations, summary).any { it.code != 200 },
        )
    }

    /** Delete my account: the website emails a link to the account's address (core/account_deletion.py). */
    suspend fun requestDeletion(token: String): Deletion {
        val reply = call("POST", "/api/core/delete-account/", token)
        val json = runCatching { JSONObject(reply.body) }.getOrNull()
        return when (reply.code) {
            200 -> Deletion.Sent(json?.optString("sent_to").orEmpty(), json?.optInt("hours", 24) ?: 24)
            401 -> Deletion.SignedOut
            403 -> Deletion.Refused(json?.optString("detail").orEmpty().ifBlank { "This account can't be deleted here." })
            else -> Deletion.Failed
        }
    }
}

data class AccountUser(val name: String, val email: String)

sealed interface SignIn {
    data class Done(val token: String, val user: AccountUser) : SignIn
    /** The address isn't confirmed yet: the person clicks the link in the website's email, then signs in again. */
    data object ConfirmEmail : SignIn
    data class Refused(val message: String) : SignIn
    data object Offline : SignIn
}

sealed interface Deletion {
    data class Sent(val to: String, val hours: Int) : Deletion
    data class Refused(val message: String) : Deletion
    data object SignedOut : Deletion
    data object Failed : Deletion
}

data class Registration(val title: String, val status: String, val total: String, val eventName: String?,
                        val eventDate: String?, val upcoming: Boolean, val registeredBy: String?)

data class Donation(val title: String, val date: String, val amount: String, val donatedBy: String?)

data class YearTotals(val year: Int, val donations: String, val registrations: String, val all: String,
                      val donationCount: Int, val registrationCount: Int)

data class Overview(val name: String? = null, val registrations: List<Registration>, val donations: List<Donation>,
                    val year: YearTotals?, val failed: Boolean)

private fun JSONObject.text(name: String): String? = if (isNull(name)) null else optString(name).ifBlank { null }

fun parseSignIn(reply: AccountApi.Reply): SignIn {
    if (reply.code == 0) return SignIn.Offline
    val json = runCatching { JSONObject(reply.body) }.getOrNull() ?: return SignIn.Refused(
        "The website didn't answer as expected. Please try again later.")
    if (reply.code == 200) {
        val token = json.optJSONObject("meta")?.text("session_token")
        val user = json.optJSONObject("data")?.optJSONObject("user")
        if (token != null && user != null) {
            return SignIn.Done(token, AccountUser(user.text("display").orEmpty(), user.text("email").orEmpty()))
        }
    }
    if (reply.code == 401) {
        val flows = json.optJSONObject("data")?.optJSONArray("flows") ?: JSONArray()
        val confirm = (0 until flows.length()).mapNotNull { flows.optJSONObject(it) }
            .any { it.optString("id") == "verify_email" && it.optBoolean("is_pending") }
        if (confirm) return SignIn.ConfirmEmail
    }
    if (reply.code == 429) return SignIn.Refused("Too many tries. Please wait a few minutes and try again.")
    val message = json.optJSONArray("errors")?.optJSONObject(0)?.text("message")
    return SignIn.Refused(message ?: "That email and password don't match an account.")
}

fun parseRegistrations(body: String): List<Registration> {
    val list = JSONObject(body).optJSONArray("registrations") ?: return emptyList()
    return (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { r ->
        val event = r.optJSONObject("event")
        Registration(
            title = r.optString("title"),
            status = r.text("status_label") ?: r.optString("status"),
            total = r.optString("total"),
            eventName = event?.text("name"),
            eventDate = event?.text("start_date"),
            upcoming = event?.optBoolean("is_upcoming") ?: false,
            registeredBy = r.text("registered_by"),
        )
    }
}

fun parseDonations(body: String): List<Donation> {
    val list = JSONObject(body).optJSONArray("results") ?: return emptyList()
    return (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { d ->
        Donation(
            title = d.optString("title"),
            date = d.optString("date").take(10),  // a date, or a time whose day is the first 10 characters
            amount = d.optString("amount"),
            donatedBy = d.text("donated_by"),
        )
    }
}

fun parseYear(body: String): YearTotals? = runCatching {
    val json = JSONObject(body)
    val totals = json.getJSONObject("totals")
    YearTotals(
        year = json.getInt("year"),
        donations = totals.optString("donations"),
        registrations = totals.optString("registrations"),
        all = totals.optString("all"),
        donationCount = json.optJSONArray("donations")?.length() ?: 0,
        registrationCount = json.optJSONArray("registrations")?.length() ?: 0,
    )
}.getOrNull()

/** The person's name from /api/core/user-info/ ("App Tester"); null when they haven't given one. */
fun parseName(body: String): String? = runCatching {
    val json = JSONObject(body)
    listOf(json.optString("first_name"), json.optString("last_name")).filter { it.isNotBlank() }
        .joinToString(" ").ifBlank { null }
}.getOrNull()
