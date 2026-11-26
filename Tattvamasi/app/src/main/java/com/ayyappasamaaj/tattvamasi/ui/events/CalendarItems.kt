package com.ayyappasamaaj.tattvamasi.ui.events

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ayyappasamaaj.tattvamasi.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Calendar (Events tab), as on the website's Calendar page (ayyappasamaaj.org/calendar/, its phone month):
 * the events the app already has, and the website's calendar-only dates (approved house bhajans, "Temple closed",
 * board meetings), which aren't in Firebase and come from /events/calendar-entries.json a month at a time.
 */
const val SITE = "https://ayyappasamaaj.org"
const val CALENDAR_FEED_URL = "$SITE/events/calendar.ics"
/** Google Calendar's subscribe link for the feed, as on the website's "Add to My Calendar" menu (events/ics.py). */
const val GOOGLE_SUBSCRIBE_URL =
    "https://calendar.google.com/calendar/render?cid=webcal%3A%2F%2Fayyappasamaaj.org%2Fevents%2Fcalendar.ics"

data class CalendarItem(
    val title: String,
    /** First and last day, both included. */
    val start: LocalDate,
    val end: LocalDate,
    /** A one-day item's start and end time; null for all day (and for items over several days). */
    val time: LocalTime?,
    val endTime: LocalTime?,
    val location: String,
    val kind: Kind,
    /** A house bhajan's tag, set by the coordinators (e.g. "Full"), or "". */
    val tag: String = "",
    /** The website page it opens (absolute), or null. */
    val url: String? = null,
    /** An event's AppEvent.key: it opens the app's own event screen. */
    val eventKey: String? = null,
) {
    enum class Kind(val label: String) {
        ONE("Event"), MULTI("Event over several days"), HOUSE("House bhajan"), ENTRY("Calendar date")
    }

    fun isOn(day: LocalDate) = !day.isBefore(start) && !day.isAfter(end)
}

/** The website's calendar entries for a month (calendar-entries.json); unknown or broken entries are skipped. */
fun parseCalendarEntries(json: String): List<CalendarItem> {
    val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).mapNotNull { index ->
        runCatching {
            val entry = results.getJSONObject(index)
            val start = LocalDate.parse(entry.getString("start_date"))
            val end = entry.optString("end_date").takeIf { it.isNotEmpty() && it != "null" }?.let(LocalDate::parse) ?: start
            fun time(key: String) = entry.optString(key).takeIf { it.length >= 4 && it != "null" }?.let(LocalTime::parse)
            val oneDay = end == start
            val link = entry.optString("link").takeIf { it.isNotEmpty() && it != "null" }
            CalendarItem(
                title = entry.getString("title"),
                start = start,
                end = end,
                time = if (oneDay) time("start_time") else null,
                endTime = if (oneDay) time("end_time") else null,
                location = entry.optString("location").takeUnless { it == "null" }.orEmpty(),
                kind = if (entry.optString("kind") == "house_bhajan") CalendarItem.Kind.HOUSE else CalendarItem.Kind.ENTRY,
                tag = entry.optString("tag").takeUnless { it == "null" }.orEmpty(),
                url = link?.let { if (it.startsWith("/")) SITE + it else it }?.takeIf { it.startsWith("http") },
            )
        }.getOrNull()
    }
}

/** The app's events as calendar items, in the Bay Area's dates (EventTime). */
fun eventItems(events: List<AppEvent>): List<CalendarItem> = events.map { event ->
    val start = event.start.atZone(EventTime.zone).toLocalDate()
    // An event ending at midnight belongs to the day before
    val end = event.end.minusSeconds(1).atZone(EventTime.zone).toLocalDate().coerceAtLeast(start)
    val oneDay = end == start
    CalendarItem(
        title = event.name,
        start = start,
        end = end,
        time = if (oneDay) event.start.atZone(EventTime.zone).toLocalTime() else null,
        endTime = if (oneDay) event.end.atZone(EventTime.zone).toLocalTime() else null,
        location = event.shortVenue,
        kind = if (oneDay) CalendarItem.Kind.ONE else CalendarItem.Kind.MULTI,
        eventKey = event.key,
    )
}

/** A day's items as the website lists them: events over several days first, then by time, all-day ones last. */
fun itemsOn(items: List<CalendarItem>, day: LocalDate): List<CalendarItem> =
    items.filter { it.isOn(day) }.sortedWith(
        compareBy<CalendarItem> { it.kind != CalendarItem.Kind.MULTI }.thenBy { it.time ?: LocalTime.MAX }
    )

/** The month's grid, Sunday first: whole weeks, with null for the days of the months around it. */
fun monthDays(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value % 7  // Sunday 0
    val days = month.lengthOfMonth()
    val weeks = (lead + days + 6) / 7
    return (0 until weeks * 7).map { cell -> (cell - lead + 1).takeIf { it in 1..days }?.let(month::atDay) }
}

private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val shortDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "6:00 PM", "6:00 – 8:00 PM"-style times for a one-day item; "All day" otherwise. */
fun CalendarItem.whenText(): String = when {
    time == null -> "All day"
    endTime == null || endTime == time -> clock.format(time)
    else -> clock.format(time) + " – " + clock.format(endTime)
}

/** The line under a title: an event over several days shows its dates ("Nov 14 – Dec 26"), others their kind. */
fun CalendarItem.kindText(): String =
    if (kind == CalendarItem.Kind.MULTI) shortDay.format(start) + " – " + shortDay.format(end) else kind.label

/** The website's calendar entries, a month at a time, kept for this run of the app. */
object CalendarStore {
    data class Month(val items: List<CalendarItem> = emptyList(), val loaded: Boolean = false, val failed: Boolean = false)

    private val _months = mutableStateOf<Map<YearMonth, Month>>(emptyMap())
    val months: State<Map<YearMonth, Month>> = _months

    suspend fun load(month: YearMonth, again: Boolean = false) {
        if (!again && _months.value[month]?.loaded == true) return
        val items = withContext(Dispatchers.IO) {
            runCatching {
                val url = "$SITE/events/calendar-entries.json?year=${month.year}&month=${month.monthValue}"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                try {
                    if (connection.responseCode == 200) parseCalendarEntries(connection.inputStream.reader().readText())
                    else null
                } finally {
                    connection.disconnect()
                }
            }.onFailure { AppLog.w("CalendarStore", "entries: " + it.message) }.getOrNull()
        }
        _months.value = _months.value + (month to (items?.let { Month(it, loaded = true) } ?: Month(failed = true)))
    }
}

/** Today in the Bay Area, where the events are. */
fun calendarToday(now: Instant = Instant.now()): LocalDate = now.atZone(EventTime.zone).toLocalDate()
