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
    /** The logo on the year-end letter (the website's letterhead's) */
    val logoUrl get() = "$site/static/images/logo_2x.png"
    val profileUrl get() = "$site/profile/"

    data class Reply(val code: Int, val body: String)

    /** One request to the website ([base]: another site than the app's, e.g. the calendar's). A network failure is
     *  code 0. */
    suspend fun call(method: String, path: String, token: String? = null, body: JSONObject? = null,
                     base: String = site): Reply =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(base + path).openConnection() as HttpURLConnection
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
        // My volunteer hours shows for volunteers (and anyone with hours from before)
        val hours = call("GET", "/api/volunteers/mine/", token)
        if (donations.code == 401 || summary.code == 401) return null
        return Overview(
            name = if (info.code == 200) parseName(info.body) else null,
            registrations = if (registrations.code == 200) parseRegistrations(registrations.body) else emptyList(),
            donations = if (donations.code == 200) parseDonations(donations.body) else emptyList(),
            year = if (summary.code == 200) parseYear(summary.body) else null,
            failed = listOf(registrations, donations, summary).any { it.code != 200 },
            hours = if (hours.code == 200) parseVolunteerHours(hours.body) else null,
        )
    }

    /** The year-end summary ("me" or "household"); null data when the website didn't answer. 401: signed out. */
    suspend fun summary(token: String, year: Int, scope: String): Pair<Int, YearSummary?> {
        val reply = call("GET", "/api/core/year-summary/?year=$year&scope=$scope", token)
        return reply.code to if (reply.code == 200) parseSummary(reply.body) else null
    }

    /** One page (10, newest first) of the person's confirmed donations and their household's sharing ones. */
    suspend fun donations(token: String, page: Int): Pair<Int, DonationsPage?> {
        val reply = call("GET", "/api/donations/mine/?page=$page", token)
        return reply.code to if (reply.code == 200) runCatching { parseDonationsPage(reply.body) }.getOrNull() else null
    }

    /** Zelle gifts the treasurer hasn't confirmed yet (shown first, outside the totals). */
    suspend fun zelleGifts(token: String): List<ZelleGift> {
        val reply = call("GET", "/api/donations/mine/zelle/", token)
        return if (reply.code == 200) runCatching { parseZelleGifts(reply.body) }.getOrDefault(emptyList()) else emptyList()
    }

    /** The running monthly gifts (null: couldn't be loaded). 401: signed out. */
    suspend fun monthlyGifts(token: String): Pair<Int, MonthlyGifts?> {
        val reply = call("GET", "/api/donations/mine/monthly/", token)
        return reply.code to if (reply.code == 200) runCatching { parseMonthlyGifts(reply.body) }.getOrNull() else null
    }

    /** Another amount (an amount id) and/or day (1-28). The gift as it is now, or the website's reason. */
    suspend fun changeMonthlyGift(token: String, id: String, amountId: Int?, day: Int?): Triple<Int, MonthlyGift?, String?> {
        val body = JSONObject()
        amountId?.let { body.put("amount", it) }
        day?.let { body.put("day", it) }
        val reply = call("POST", "/api/donations/mine/monthly/$id/change/", token, body)
        val json = runCatching { JSONObject(reply.body) }.getOrNull()
        if (reply.code == 200) json?.optJSONObject("changed")?.let { return Triple(200, parseMonthlyGift(it), null) }
        val detail = json?.optString("detail").orEmpty()
        return Triple(reply.code, null, detail.ifBlank {
            if (reply.code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
            else "It couldn't be changed just now. Please try again in a few minutes."
        })
    }

    /** Stops one at once. null when stopped; else the website's reason. */
    suspend fun stopMonthlyGift(token: String, id: String): Pair<Int, String?> {
        val reply = call("POST", "/api/donations/mine/monthly/$id/stop/", token)
        if (reply.code == 200) return 200 to null
        val detail = runCatching { JSONObject(reply.body).optString("detail") }.getOrNull().orEmpty()
        return reply.code to detail.ifBlank {
            if (reply.code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
            else "It couldn't be stopped just now. Please try again in a few minutes."
        }
    }

    /** A year's volunteer hours (null: not loaded). 401: signed out. */
    suspend fun volunteerHours(token: String, year: Int?): Pair<Int, VolunteerHours?> {
        val reply = call("GET", "/api/volunteers/mine/" + (year?.let { "?year=$it" } ?: ""), token)
        return reply.code to if (reply.code == 200) parseVolunteerHours(reply.body) else null
    }

    /** What Add hours offers (null: not loaded, or not a volunteer). */
    suspend fun addHoursForm(token: String): Pair<Int, AddHoursForm?> {
        val reply = call("GET", "/api/volunteers/mine/add/", token)
        return reply.code to if (reply.code == 200) parseAddHoursForm(reply.body) else null
    }

    /** Adds an entry, with the website form's rules. */
    suspend fun addHours(token: String, date: String, start: String, end: String, activity: String,
                         coordinator: Int?): AddHoursResult {
        val body = JSONObject().put("date", date).put("start", start).put("end", end).put("activity", activity)
            .put("coordinator", coordinator ?: JSONObject.NULL)
        val reply = call("POST", "/api/volunteers/mine/add/", token, body)
        return parseAddHours(reply.code, reply.body)
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
                        val eventDate: String?, val upcoming: Boolean, val registeredBy: String?,
                        val code: String = "",
                        val state: String = "",              // paid, free, pending
                        val items: List<RegistrationItem> = emptyList(),
                        val amountPaid: String? = null,
                        val answers: List<Pair<String, String>> = emptyList(),
                        val whatsappUrl: String = "",
                        val whatsappNote: String = "",
                        val formActive: Boolean = false,
                        val year: Int = 0,
                        val registeredAt: String = "",
                        val event: RegistrationEvent? = null)

data class RegistrationItem(val name: String, val quantity: Int, val amount: String, val details: List<String>)

/** The event using the registration's form, and its next date (or its last, once over). */
data class RegistrationEvent(val name: String, val startDate: String, val endDate: String, val startTime: String,
                             val endTime: String, val venue: String, val upcoming: Boolean)

data class Donation(val title: String, val date: String, val amount: String, val donatedBy: String?)

data class YearTotals(val year: Int, val donations: String, val registrations: String, val all: String,
                      val donationCount: Int, val registrationCount: Int)

data class Overview(val name: String? = null, val registrations: List<Registration>, val donations: List<Donation>,
                    val year: YearTotals?, val failed: Boolean, val hours: VolunteerHours? = null)

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
            code = r.optString("code"),
            state = r.optString("status"),
            items = (r.optJSONArray("items") ?: JSONArray()).let { list ->
                (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { line ->
                    val details = line.optJSONArray("details") ?: JSONArray()
                    RegistrationItem(line.optString("name"), line.optInt("quantity", 1), line.optString("amount"),
                        (0 until details.length()).map { details.optString(it) }.filter { it.isNotBlank() })
                }
            },
            amountPaid = r.text("amount_paid"),
            answers = (r.optJSONArray("answers") ?: JSONArray()).let { list ->
                (0 until list.length()).mapNotNull { list.optJSONObject(it) }
                    .map { it.optString("question") to it.optString("answer") }
            },
            whatsappUrl = r.optString("whatsapp_group_url"),
            whatsappNote = r.optString("whatsapp_group_note"),
            formActive = r.optBoolean("form_active"),
            year = r.optInt("year"),
            registeredAt = r.optString("registered_at"),
            event = event?.let {
                RegistrationEvent(it.optString("name"), it.optString("start_date"), it.optString("end_date"),
                    it.optString("start_time"), it.optString("end_time"), it.optString("venue"),
                    it.optBoolean("is_upcoming"))
            },
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
