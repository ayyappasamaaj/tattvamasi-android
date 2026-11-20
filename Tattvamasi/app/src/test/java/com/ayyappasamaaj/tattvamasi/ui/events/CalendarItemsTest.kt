package com.ayyappasamaaj.tattvamasi.ui.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class CalendarItemsTest {
    /** The website's calendar-entries.json, as production answers it (Nov 2026), plus an all-day "other date". */
    private val json = """
        {"results": [
          {"title": "House bhajan: Ram Manikandan Family", "start_date": "2026-11-20", "end_date": "2026-11-20",
           "start_time": "18:00", "end_time": "20:00", "location": "Fremont", "link": "/calendar/house-bhajans/7/",
           "kind": "house_bhajan", "tag": "Full"},
          {"title": "Temple closed", "start_date": "2026-11-26", "end_date": "2026-11-27", "start_time": null,
           "end_time": null, "location": "", "link": "", "kind": "other", "tag": ""},
          {"title": "Broken", "start_date": "not a date"}
        ]}
    """.trimIndent()

    private fun event(name: String, start: String, end: String) = AppEvent(
        id = name, name = name, summary = "", venue = "SVCC Temple, Fremont", start = Instant.parse(start),
        end = Instant.parse(end), timeText = "", pageUrl = null, latitude = null, longitude = null,
    )

    @Test
    fun entriesFromTheWebsite() {
        val (bhajan, closed) = parseCalendarEntries(json)
        assertEquals(CalendarItem.Kind.HOUSE, bhajan.kind)
        assertEquals(LocalTime.of(18, 0), bhajan.time)
        assertEquals("https://ayyappasamaaj.org/calendar/house-bhajans/7/", bhajan.url)
        assertEquals("Full", bhajan.tag)
        assertEquals("6:00 PM – 8:00 PM", bhajan.whenText())
        assertEquals("House bhajan", bhajan.kindText())

        assertEquals(CalendarItem.Kind.ENTRY, closed.kind)
        assertNull(closed.url)
        assertNull(closed.time)
        assertEquals("All day", closed.whenText())
        assertEquals(LocalDate.of(2026, 11, 27), closed.end)
        assertEquals(2, parseCalendarEntries(json).size)  // the broken one is skipped
    }

    @Test
    fun eventsInBayAreaDates() {
        // 7 AM to 1 PM Pacific on Nov 14; and Nov 14 to Dec 26 (ending at midnight)
        val one = eventItems(listOf(event("Sunday puja", "2026-11-14T15:00:00Z", "2026-11-14T21:00:00Z"))).single()
        assertEquals(CalendarItem.Kind.ONE, one.kind)
        assertEquals(LocalDate.of(2026, 11, 14), one.start)
        assertEquals("7:00 AM – 1:00 PM", one.whenText())
        assertEquals("SVCC Temple", one.location)

        val season = eventItems(listOf(event("Mandalam", "2026-11-14T15:00:00Z", "2026-12-27T08:00:00Z"))).single()
        assertEquals(CalendarItem.Kind.MULTI, season.kind)
        assertEquals(LocalDate.of(2026, 12, 26), season.end)
        assertEquals("Nov 14 – Dec 26", season.kindText())
        assertEquals("All day", season.whenText())
    }

    @Test
    fun aDaysItemsInTheWebsitesOrder() {
        val items = eventItems(listOf(event("Mandalam", "2026-11-14T15:00:00Z", "2026-12-27T08:00:00Z"))) +
            parseCalendarEntries(json)
        val day = itemsOn(items, LocalDate.of(2026, 11, 20))
        assertEquals(listOf("Mandalam", "House bhajan: Ram Manikandan Family"), day.map { it.title })
        assertEquals(listOf("Mandalam", "Temple closed"), itemsOn(items, LocalDate.of(2026, 11, 27)).map { it.title })
    }

    @Test
    fun monthGridStartsOnSunday() {
        val november = monthDays(YearMonth.of(2026, 11))  // Nov 1, 2026 is a Sunday
        assertEquals(LocalDate.of(2026, 11, 1), november.first())
        assertEquals(35, november.size)
        val december = monthDays(YearMonth.of(2026, 12))  // Dec 1 is a Tuesday
        assertEquals(listOf(null, null), december.take(2))
        assertEquals(LocalDate.of(2026, 12, 1), december[2])
        assertEquals(0, december.size % 7)
    }
}
