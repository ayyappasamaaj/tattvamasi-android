package com.ayyappasamaaj.tattvamasi.ui.account

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * My volunteer hours, in the app instead of the website's dashboard (the dashboard's VolunteerHours.vue): the website's
 * /api/volunteers/mine/ (a year's entries, the totals recorded and approved, the letterhead) and
 * /api/volunteers/mine/add/ (the Add hours form, with the rules of the website's /volunteers/add/ page). The same as
 * the iOS app's VolunteerHours.swift.
 *
 * Plain functions, unit-tested with the website's answers (VolunteerHoursTest).
 */
data class HoursEntry(val date: String, val start: String, val end: String, val hours: Double, val activity: String,
                      val coordinator: String, val status: String, val statusLabel: String) {
    val approved get() = status == "approved"
}

data class VolunteerHours(
    val isVolunteer: Boolean,
    val year: Int,
    val years: List<Int>,
    val entries: List<HoursEntry>,
    val recorded: Double,
    val approved: Double,
    val personName: String,
    val personEmail: String,
    val orgName: String,
    val tagline: String,
    val place: String,
    val email: String,
    val website: String,
    val officers: List<Officer>,
    val generated: String,
)

data class TimeChoice(val value: String, val label: String)
data class Coordinator(val id: Int, val name: String)

/** What Add hours offers: the dates allowed (the current year, and last year during January), the 15-minute times and
 *  the active coordinators (one must be picked when there are any). */
data class AddHoursForm(val earliest: String, val latest: String, val times: List<TimeChoice>,
                        val coordinators: List<Coordinator>, val coordinatorRequired: Boolean)

/** The answer to Add hours: added (the website's message and the entry's year), or the problems by field (date,
 *  start, end, activity, coordinator; "" for the whole form). */
sealed interface AddHoursResult {
    data class Added(val message: String, val year: Int) : AddHoursResult
    data class Problems(val errors: Map<String, String>) : AddHoursResult
    data object SignedOut : AddHoursResult
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

fun parseHoursEntry(it: JSONObject) = HoursEntry(it.optString("date"), it.optString("start"), it.optString("end"),
    it.optDouble("hours", 0.0), it.optString("activity"), it.optString("coordinator"), it.optString("status"),
    it.optString("status_label"))

fun parseVolunteerHours(body: String): VolunteerHours? = runCatching {
    val json = JSONObject(body)
    val head = json.optJSONObject("letterhead") ?: JSONObject()
    val person = json.optJSONObject("person") ?: JSONObject()
    val totals = json.optJSONObject("totals") ?: JSONObject()
    val years = json.optJSONArray("years")
    VolunteerHours(
        isVolunteer = json.optBoolean("is_volunteer"),
        year = json.getInt("year"),
        years = if (years == null) emptyList() else (0 until years.length()).map { years.getInt(it) },
        entries = json.optJSONArray("records").objects().map(::parseHoursEntry),
        recorded = totals.optDouble("recorded", 0.0),
        approved = totals.optDouble("approved", 0.0),
        personName = person.optString("name"),
        personEmail = person.optString("email"),
        orgName = head.optString("name", "Ayyappa Samaaj"),
        tagline = head.optString("tagline"),
        place = head.optString("place"),
        email = head.optString("email"),
        website = head.optString("website"),
        officers = head.optJSONArray("officers").objects().map { Officer(it.optString("title"), it.optString("name")) },
        generated = json.optString("generated"),
    )
}.getOrNull()

fun parseAddHoursForm(body: String): AddHoursForm? = runCatching {
    val json = JSONObject(body)
    AddHoursForm(
        earliest = json.getString("earliest"),
        latest = json.getString("latest"),
        times = json.optJSONArray("times").objects().map { TimeChoice(it.optString("value"), it.optString("label")) },
        coordinators = json.optJSONArray("coordinators").objects().map { Coordinator(it.optInt("id"), it.optString("name")) },
        coordinatorRequired = json.optBoolean("coordinator_required"),
    )
}.getOrNull()

/** The website's answer to Add hours: 201 added; 400 the form's messages by field; 403 not a volunteer. */
fun parseAddHours(code: Int, body: String): AddHoursResult {
    val json = runCatching { JSONObject(body) }.getOrNull()
    if (code == 201 && json != null) {
        return AddHoursResult.Added(json.optString("message", "Hours added."), json.optInt("year"))
    }
    if (code == 401) return AddHoursResult.SignedOut
    val errors = json?.optJSONObject("errors")?.let { e ->
        e.keys().asSequence().associate { field ->
            val messages = e.optJSONArray(field)
            (if (field == "__all__") "" else field) to
                (if (messages == null) e.optString(field) else (0 until messages.length()).joinToString(" ") { messages.optString(it) })
        }
    }
    if (!errors.isNullOrEmpty()) return AddHoursResult.Problems(errors)
    val reason = json?.optString("error").orEmpty().ifBlank {
        if (code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
        else "The hours couldn't be added just now. Please try again in a few minutes."
    }
    return AddHoursResult.Problems(mapOf("" to reason))
}

/** The hours between two picked times ("09:00", "11:30" → 2.5), or null if either is missing or the end isn't after
 *  the start (the website's hours_between). */
fun hoursBetween(start: String?, end: String?): Double? {
    val a = start?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
    val b = end?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
    val minutes = b.toSecondOfDay() / 60 - a.toSecondOfDay() / 60
    return if (minutes > 0) minutes / 60.0 else null
}

/** 2.5 → "2.5", 3.0 → "3" */
fun hoursNumber(hours: Double): String =
    if (hours == Math.floor(hours)) "%,d".format(hours.toLong()) else "%,.2f".format(hours).trimEnd('0').trimEnd('.')

/** 2.5 → "2.5 hours", 1 → "1 hour" (the website's hours_text) */
fun hoursText(hours: Double): String = "${hoursNumber(hours)} hour${if (hours == 1.0) "" else "s"}"

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** "09:30" → "9:30 AM" */
fun clockTime(hhmm: String): String = runCatching { LocalTime.parse(hhmm).format(timeFormat) }.getOrDefault(hhmm)

/**
 * The volunteer hours record to print or save as PDF, as the dashboard prints it: on the letterhead, with only the
 * approved entries and the approved total (entries waiting for a coordinator are on the screen only).
 */
fun hoursLetterHtml(h: VolunteerHours, logoUrl: String): String {
    val approved = h.entries.filter { it.approved }
    val rows = approved.joinToString("") { e ->
        "<tr><td class=d>${esc(summaryDay(e.date))}<br><span class=s>${esc(clockTime(e.start))}–${esc(clockTime(e.end))}</span></td>" +
            "<td>${esc(e.activity)}" + (if (e.coordinator.isNotBlank()) "<br><span class=s>With ${esc(e.coordinator)}</span>" else "") +
            "</td><td class=n>${esc(hoursNumber(e.hours))}</td></tr>"
    }
    val table = if (approved.isEmpty()) "<p>No approved hours in ${h.year} yet.</p>" else
        "<table><thead><tr><th>Date</th><th>Activity</th><th class=n>Hours</th></tr></thead><tbody>$rows</tbody>" +
            "<tfoot><tr><th colspan=2>Total approved</th><td class=n>${esc(hoursNumber(h.approved))}</td></tr></tfoot></table>"
    val title = "${h.year} volunteer hours"
    return letterPage("${h.orgName} $title", h.orgName, h.tagline, h.place, h.email, h.website, h.officers, logoUrl, """
        <h2>${esc(title)}</h2>
        <p>${h.year} volunteer hours of <b>${esc(h.personName)}</b>${if (h.personEmail.isNotBlank()) " (${esc(h.personEmail)})" else ""}</p>
        <p class=all>Approved <b>${esc(hoursText(h.approved))}</b></p>
        $table
        <p class=gen>Hours are approved by the volunteer coordinators; only approved hours are listed. Printed from
        ayyappasamaaj.org on ${esc(summaryDay(h.generated))}.</p>""")
}
