package com.ayyappasamaaj.tattvamasi.ui.housebhajan

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * House bhajans in the app instead of the website's pages (website: events/house_bhajan_api.py, the same rules,
 * review and emails as /calendar/house-bhajans/): a bhajan's page from the calendar, messaging its host, hosting one,
 * and My house bhajans. The same as the iOS app's HouseBhajans.swift.
 *
 * Plain functions, unit-tested with the website's answers (HouseBhajansTest).
 */
data class HouseBhajan(
    val id: Int,
    val hostName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val whenText: String,
    val city: String,
    val tag: String,
    val guestNote: String,
    val rsvp: String,              // "link" or "none"
    val rsvpLink: String,
    val status: String,            // pending, approved, declined, canceled
    val statusLabel: String,
    val approved: Boolean,
    val upcoming: Boolean,
    val canMessage: Boolean,
    val address: String,           // shown to signed-in visitors when the host chose to
    val phone: String,
    val email: String,
    val hiddenDetails: String,     // what signing in would show, e.g. "address and phone number"
    val url: String,               // its page on the website
    val isHost: Boolean = false,
    // The host's own (My house bhajans)
    val street: String = "",
    val zipCode: String = "",
    val hostPhone: String = "",
    val coordinatorNote: String = "",
    val showAddress: Boolean = false,
    val showPhone: Boolean = false,
    val showEmail: Boolean = false,
    val declineReason: String = "",
    val canChange: Boolean = false,
)

data class RsvpChoice(val value: String, val label: String)

/** What a new bhajan's form starts with, and its limits. */
data class BhajanFormInfo(val hostName: String, val phone: String, val earliest: String, val latest: String,
                          val rsvpChoices: List<RsvpChoice>, val canAdd: Boolean)

/** The form's fields, as the website's HouseBhajanForm takes them. */
data class BhajanFields(
    val hostName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val street: String = "",
    val city: String = "",
    val zipCode: String = "",
    val phone: String = "",
    val rsvp: String = "link",
    val rsvpLink: String = "",
    val guestNote: String = "",
    val coordinatorNote: String = "",
    val showAddress: Boolean = false,
    val showPhone: Boolean = false,
    val showEmail: Boolean = false,
    val agree: Boolean = false,
) {
    fun json(): JSONObject = JSONObject()
        .put("host_name", hostName.trim()).put("date", date).put("start_time", startTime).put("end_time", endTime)
        .put("street", street.trim()).put("city", city.trim()).put("zip_code", zipCode.trim()).put("phone", phone.trim())
        .put("rsvp", rsvp).put("rsvp_link", if (rsvp == "link") rsvpLink.trim() else "")
        .put("guest_note", guestNote.trim()).put("coordinator_note", coordinatorNote.trim())
        .put("show_address", showAddress).put("show_phone", showPhone).put("show_email", showEmail).put("agree", agree)

    companion object {
        /** A bhajan's own details back in the form, to change it (the host agreed when they sent it). */
        fun of(b: HouseBhajan) = BhajanFields(b.hostName, b.date, b.startTime, b.endTime, b.street, b.city, b.zipCode,
            b.hostPhone, b.rsvp, b.rsvpLink, b.guestNote, b.coordinatorNote, b.showAddress, b.showPhone, b.showEmail,
            agree = true)
    }
}

/** The website's answer to sending, changing, cancelling or messaging. */
sealed interface BhajanAnswer {
    data class Done(val message: String, val bhajan: HouseBhajan?) : BhajanAnswer
    /** Problems by field (host_name, date, ...; "" for the whole form). */
    data class Problems(val errors: Map<String, String>) : BhajanAnswer
    data object SignedOut : BhajanAnswer
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONObject.text(name: String): String = if (isNull(name)) "" else optString(name)

fun parseBhajan(j: JSONObject) = HouseBhajan(
    id = j.getInt("id"),
    hostName = j.text("host_name"),
    date = j.text("date"),
    startTime = j.text("start_time"),
    endTime = j.text("end_time"),
    whenText = j.text("when"),
    city = j.text("city"),
    tag = j.text("tag"),
    guestNote = j.text("guest_note"),
    rsvp = j.optString("rsvp", "link"),
    rsvpLink = j.text("rsvp_link"),
    status = j.text("status"),
    statusLabel = j.text("status_label"),
    approved = j.optBoolean("approved"),
    upcoming = j.optBoolean("upcoming"),
    canMessage = j.optBoolean("can_message"),
    address = j.text("address"),
    phone = j.text("phone"),
    email = j.text("email"),
    hiddenDetails = j.text("hidden_details"),
    url = j.text("url"),
    isHost = j.optBoolean("is_host"),
    street = j.text("street"),
    zipCode = j.text("zip_code"),
    hostPhone = j.text("host_phone"),
    coordinatorNote = j.text("coordinator_note"),
    showAddress = j.optBoolean("show_address"),
    showPhone = j.optBoolean("show_phone"),
    showEmail = j.optBoolean("show_email"),
    declineReason = j.text("decline_reason"),
    canChange = j.optBoolean("can_change"),
)

fun parseBhajanPage(body: String): HouseBhajan? = runCatching { parseBhajan(JSONObject(body)) }.getOrNull()

/** My house bhajans, and whether another can be added. */
fun parseMine(body: String): Pair<List<HouseBhajan>, Boolean>? = runCatching {
    val json = JSONObject(body)
    json.optJSONArray("bhajans").objects().map(::parseBhajan) to json.optBoolean("can_add", true)
}.getOrNull()

fun parseFormInfo(body: String): BhajanFormInfo? = runCatching {
    val j = JSONObject(body)
    BhajanFormInfo(j.text("host_name"), j.text("phone"), j.getString("earliest"), j.getString("latest"),
        j.optJSONArray("rsvp_choices").objects().map { RsvpChoice(it.optString("value"), it.optString("label")) },
        j.optBoolean("can_add", true))
}.getOrNull()

fun parseAnswer(code: Int, body: String): BhajanAnswer {
    val json = runCatching { JSONObject(body) }.getOrNull()
    if (code in 200..299 && json != null) {
        return BhajanAnswer.Done(json.optString("message"), json.optJSONObject("bhajan")?.let { runCatching { parseBhajan(it) }.getOrNull() })
    }
    if (code == 401) return BhajanAnswer.SignedOut
    val errors = json?.optJSONObject("errors")?.let { e ->
        e.keys().asSequence().associateWith { field ->
            e.optJSONArray(field)?.let { list -> (0 until list.length()).joinToString(" ") { list.optString(it) } }
                ?: e.optString(field)
        }
    }
    if (!errors.isNullOrEmpty()) return BhajanAnswer.Problems(errors)
    val reason = json?.optString("error").orEmpty().ifBlank {
        when (code) {
            0 -> "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
            404 -> "That house bhajan isn't there any more."
            else -> "It couldn't be saved just now. Please try again in a few minutes."
        }
    }
    return BhajanAnswer.Problems(mapOf("" to reason))
}

/** A house bhajan's calendar entry id from its page link ("…/calendar/house-bhajans/123/"), or null. */
fun bhajanId(url: String): Int? = Regex("""/calendar/house-bhajans/(\d+)/?""").find(url)?.groupValues?.get(1)?.toIntOrNull()

/** The site a link is on ("https://ayyappasamaaj.org"), so a bhajan is read from the site its calendar came from. */
fun siteOf(url: String): String? = Regex("""^(https?://[^/]+)""").find(url)?.groupValues?.get(1)

/** "18:00" → "6:00 PM" */
fun clock(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return hhmm
    return "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
}

/** The times the form offers: every 15 minutes, 6:00 AM to 11:45 PM. */
val bhajanTimes: List<String> = (6 * 4 until 24 * 4).map { "%02d:%02d".format(it / 4, it % 4 * 15) }

object HouseBhajanApi {
    private const val PATH = "/api/house-bhajans/"

    /** A bhajan's page, read from [site] (the calendar's); the token only goes to the app's own site. */
    suspend fun bhajan(site: String, id: Int, token: String?): Pair<Int, HouseBhajan?> {
        val reply = AccountApi.call("GET", "$PATH$id/", token?.takeIf { site == AccountApi.site }, base = site)
        return reply.code to if (reply.code == 200) parseBhajanPage(reply.body) else null
    }

    suspend fun message(token: String, id: Int, message: String, phone: String): BhajanAnswer {
        val reply = AccountApi.call("POST", "$PATH$id/message/", token,
            JSONObject().put("message", message.trim()).put("phone", phone.trim()))
        return parseAnswer(reply.code, reply.body)
    }

    suspend fun mine(token: String): Pair<Int, Pair<List<HouseBhajan>, Boolean>?> {
        val reply = AccountApi.call("GET", PATH, token)
        return reply.code to if (reply.code == 200) parseMine(reply.body) else null
    }

    suspend fun formInfo(token: String): Pair<Int, BhajanFormInfo?> {
        val reply = AccountApi.call("GET", "${PATH}form/", token)
        return reply.code to if (reply.code == 200) parseFormInfo(reply.body) else null
    }

    suspend fun send(token: String, fields: BhajanFields): BhajanAnswer {
        val reply = AccountApi.call("POST", PATH, token, fields.json())
        return parseAnswer(reply.code, reply.body)
    }

    suspend fun change(token: String, id: Int, fields: BhajanFields): BhajanAnswer {
        val reply = AccountApi.call("POST", "$PATH$id/change/", token, fields.json())
        return parseAnswer(reply.code, reply.body)
    }

    suspend fun cancel(token: String, id: Int): BhajanAnswer {
        val reply = AccountApi.call("POST", "$PATH$id/cancel/", token)
        return parseAnswer(reply.code, reply.body)
    }
}
