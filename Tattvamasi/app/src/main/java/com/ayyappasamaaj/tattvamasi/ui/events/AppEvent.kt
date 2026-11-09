package com.ayyappasamaaj.tattvamasi.ui.events

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One event date from Firebase `/events`, which the website publishes on every save (website:
 * events/firebase.py). As on the iOS app (EventsModel.swift).
 */
data class AppEvent(
    val id: String,
    val name: String,
    val summary: String,
    val venue: String,
    val start: Instant,
    val end: Instant,
    /** The website's display text: "7:00 AM - 1:00 PM", or "Nov 14 - Dec 26" for a longer event. */
    val timeText: String,
    /** The event date's page on the website, which has its Register button. */
    val pageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    /** The cover picture (absolute), or null. */
    val imageUrl: String? = null,
    /** The event's text in the website's small HTML subset (events/firebase.py app_html), or "". */
    val details: String = "",
    /** How to register, when the event takes registration. */
    val registration: Registration? = null,
) {
    /** Stays the same for this event date when the list changes (Firebase's list index doesn't). */
    val key: String get() = "${start.epochSecond}|$name"

    /** The venue in a few words: "SVCC Temple" rather than the whole address. */
    val shortVenue: String
        get() = venue.split(",").first().trim().ifEmpty { venue }

    /** The description, when it says more than the name. */
    val extraSummary: String?
        get() = summary.trim().takeUnless { it.isEmpty() || it.equals(name.trim(), ignoreCase = true) }
}

/**
 * Where and when to register: the website's registration form (it stays on the website: sign-in, payment and
 * emails). [opensAt] / [closesAt] are null when there's no such limit.
 */
data class Registration(val url: String, val opensAt: Instant?, val closesAt: Instant?) {
    sealed interface Status {
        data object Open : Status
        data class OpensOn(val at: Instant) : Status
        data object Closed : Status
    }

    /** Worked out from the times, since they change without the website publishing anything. Whether a form is
     *  full shows on its page. */
    fun status(now: Instant): Status = when {
        opensAt != null && now.isBefore(opensAt) -> Status.OpensOn(opensAt)
        closesAt != null && !now.isBefore(closesAt) -> Status.Closed
        else -> Status.Open
    }
}

/** Event dates are shown in the Bay Area's time, where the events are. */
object EventTime {
    val zone: ZoneId = ZoneId.of("America/Los_Angeles")

    fun format(instant: Instant, pattern: String): String =
        DateTimeFormatter.ofPattern(pattern, Locale.US).withZone(zone).format(instant)
}

/** A month's heading and its events: "November 2026". */
data class EventMonth(val title: String, val events: List<AppEvent>)

/**
 * The upcoming events in Firebase's `/events` value, in date order. The website writes a list, which Firebase
 * hands back as a List (a Map if keys ever have gaps). An event is listed until it ends, so a multi-day event
 * stays on after its first day. Entries without a name or dates are skipped.
 */
fun parseEvents(value: Any?, now: Instant): List<AppEvent> {
    val entries: List<Pair<String, Any?>> = when (value) {
        is Map<*, *> -> value.map { (key, item) -> key.toString() to item }
        is List<*> -> value.mapIndexed { index, item -> index.toString() to item }
        else -> emptyList()
    }
    return entries.mapNotNull { (key, raw) ->
        val item = raw as? Map<*, *> ?: return@mapNotNull null
        val name = item["name"] as? String ?: return@mapNotNull null
        val startSeconds = (item["date"] as? Number)?.toLong() ?: return@mapNotNull null
        val endSeconds = (item["endDate"] as? Number)?.toLong() ?: return@mapNotNull null
        val start = Instant.ofEpochSecond(startSeconds)
        val end = Instant.ofEpochSecond(endSeconds)
        if (!end.isAfter(now)) return@mapNotNull null
        AppEvent(
            id = key,
            name = name,
            summary = item["desc"] as? String ?: "",
            venue = item["venue"] as? String ?: "",
            start = start,
            end = end,
            timeText = (item["time"] as? String)?.takeIf { it.isNotBlank() } ?: EventTime.format(start, "h:mm a"),
            pageUrl = (item["registrationLink"] as? String)?.takeIf { it.startsWith("http") },
            latitude = (item["latitude"] as? Number)?.toDouble(),
            longitude = (item["longitude"] as? Number)?.toDouble(),
            imageUrl = (item["imageUrl"] as? String)?.takeIf { it.startsWith("http") },
            details = item["details"] as? String ?: "",
            registration = (item["registration"] as? Map<*, *>)?.let { reg ->
                fun time(key: String) = (reg[key] as? Number)?.toLong()?.takeIf { it > 0 }?.let(Instant::ofEpochSecond)
                (reg["url"] as? String)?.takeIf { it.startsWith("http") }?.let { Registration(it, time("opensAt"), time("closesAt")) }
            },
        )
    }.sortedBy { it.start }
}

/** Events grouped by month, in date order. */
fun groupByMonth(events: List<AppEvent>): List<EventMonth> {
    val months = mutableListOf<EventMonth>()
    for (event in events) {
        val title = EventTime.format(event.start, "MMMM yyyy")
        val last = months.lastOrNull()
        if (last?.title == title) {
            months[months.lastIndex] = last.copy(events = last.events + event)
        } else {
            months += EventMonth(title, listOf(event))
        }
    }
    return months
}
