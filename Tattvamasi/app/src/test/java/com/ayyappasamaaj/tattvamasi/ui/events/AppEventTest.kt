package com.ayyappasamaaj.tattvamasi.ui.events

import com.ayyappasamaaj.tattvamasi.ui.calendarNotes
import com.ayyappasamaaj.tattvamasi.ui.calendarWebUrl
import com.ayyappasamaaj.tattvamasi.ui.directionsUrl
import com.ayyappasamaaj.tattvamasi.ui.home.homeColumns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime

class AppEventTest {
    private val now = Instant.parse("2026-11-08T20:00:00Z")

    private fun seconds(text: String) = ZonedDateTime.parse(text).toEpochSecond()

    /** One entry as the website writes it (events/firebase.py app_events); Firebase gives numbers as Long or Double. */
    private fun entry(name: String, start: String, end: String, extra: Map<String, Any?> = emptyMap()) = mapOf(
        "name" to name,
        "desc" to "",
        "venue" to "Siddhi Vinayaka (SVCC) Temple, 40155 Blacow Road, Fremont, CA 94538",
        "registrationLink" to "https://ayyappasamaaj.org/events/mandala-puja/2026-11-14/",
        "date" to seconds(start),
        "endDate" to seconds(end),
        "time" to "7:00 AM - 1:00 PM",
        "latitude" to 37.5203,
        "longitude" to -121.9726,
    ) + extra

    @Test
    fun readsTheWebsitesListInDateOrderAndLeavesOutEndedEvents() {
        val value = listOf(
            entry("Later", "2026-12-05T07:00-08:00", "2026-12-05T13:00-08:00"),
            null, // Firebase leaves gaps as nulls
            entry("Ended", "2026-11-01T07:00-07:00", "2026-11-01T13:00-07:00"),
            entry("Multi-day, started", "2026-11-01T07:00-07:00", "2026-12-26T20:00-08:00"),
            entry("Sooner", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00"),
            mapOf("name" to "No dates"),
        )

        val events = parseEvents(value, now)

        assertEquals(listOf("Multi-day, started", "Sooner", "Later"), events.map { it.name })
        val sooner = events[1]
        assertEquals("4", sooner.id)
        assertEquals("7:00 AM - 1:00 PM", sooner.timeText)
        assertEquals(37.5203, sooner.latitude!!, 1e-9)
    }

    @Test
    fun alsoReadsAMapAndFillsInMissingTime() {
        val events = parseEvents(
            mapOf("a" to entry("x", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00", mapOf("time" to ""))),
            now,
        )
        assertEquals("a", events.single().id)
        assertEquals("7:00 AM", events.single().timeText) // Bay Area time, whatever the phone's zone
    }

    @Test
    fun shortVenueAndExtraSummary() {
        val event = parseEvents(listOf(entry("Mandala Puja", "2026-11-14T07:00-08:00",
            "2026-11-14T13:00-08:00", mapOf("desc" to "mandala puja "))), now).single()
        assertEquals("Siddhi Vinayaka (SVCC) Temple", event.shortVenue)
        assertNull(event.extraSummary) // the same as the name
        assertEquals(event.pageUrl, calendarNotes(event))
    }

    @Test
    fun groupsByMonthInTheBayAreasTime() {
        // 11 PM on Nov 30 in California is already Dec 1 in UTC
        val events = parseEvents(listOf(
            entry("Nov 30 late", "2026-11-30T23:00-08:00", "2026-11-30T23:30-08:00"),
            entry("Dec 5", "2026-12-05T07:00-08:00", "2026-12-05T13:00-08:00"),
            entry("Dec 6", "2026-12-06T07:00-08:00", "2026-12-06T13:00-08:00"),
        ), now)
        val months = groupByMonth(events)
        assertEquals(listOf("November 2026", "December 2026"), months.map { it.title })
        assertEquals(listOf(1, 2), months.map { it.events.size })
    }

    @Test
    fun directionsGoToTheMapPointOrTheAddress() {
        val event = parseEvents(listOf(entry("x", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00")), now).single()
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=37.5203,-121.9726&travelmode=driving",
            directionsUrl(event))
        val noPoint = event.copy(latitude = null, venue = "40155 Blacow Road, Fremont, CA")
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=40155+Blacow+Road%2C+Fremont%2C+CA" +
            "&travelmode=driving", directionsUrl(noPoint))
    }

    @Test
    fun homeTilesTwoAcrossOnAPhoneUpToFourOnATablet() {
        assertEquals(2, homeColumns(360f))
        assertEquals(2, homeColumns(412f))
        assertEquals(4, homeColumns(788f))
        assertEquals(4, homeColumns(1248f))
    }

    @Test
    fun calendarPageWithoutACalendarApp() {
        val event = parseEvents(listOf(entry("Mandala Puja", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00")),
            now).single()
        assertEquals(
            "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Mandala+Puja" +
                "&dates=20261114T150000Z/20261114T210000Z" +
                "&location=Siddhi+Vinayaka+%28SVCC%29+Temple%2C+40155+Blacow+Road%2C+Fremont%2C+CA+94538" +
                "&details=https%3A%2F%2Fayyappasamaaj.org%2Fevents%2Fmandala-puja%2F2026-11-14%2F",
            calendarWebUrl(event),
        )
    }

    @Test
    fun readsThePictureTextAndRegistrationForTheEventScreen() {
        val opens = Instant.parse("2026-11-01T16:00:00Z")
        val closes = Instant.parse("2026-12-05T17:00:00Z")
        val event = parseEvents(listOf(entry("Mandalam", "2026-11-14T07:00-08:00", "2026-12-26T20:00-08:00", mapOf(
            "imageUrl" to "https://ayyappasamaaj.org/media/events/cover_images/m.png",
            "details" to "<p>Swamiye Saranam</p>",
            "registration" to mapOf("url" to "https://ayyappasamaaj.org/forms/mandalam-2026/",
                "opensAt" to opens.epochSecond, "closesAt" to closes.epochSecond),
        ))), now).single()

        assertEquals("https://ayyappasamaaj.org/media/events/cover_images/m.png", event.imageUrl)
        assertEquals("<p>Swamiye Saranam</p>", event.details)
        val registration = event.registration!!
        assertEquals("https://ayyappasamaaj.org/forms/mandalam-2026/", registration.url)
        assertEquals(Registration.Status.OpensOn(opens), registration.status(Instant.parse("2026-10-30T00:00:00Z")))
        assertEquals(Registration.Status.Open, registration.status(Instant.parse("2026-11-20T00:00:00Z")))
        assertEquals(Registration.Status.Closed, registration.status(closes))
        assertEquals("Sat, Dec 5, 9:00 AM", com.ayyappasamaaj.tattvamasi.ui.events.registrationTime(closes))
        assertEquals("Saturday, November 14 to Saturday, December 26", dateLine(event))
        assertEquals("${event.start.epochSecond}|Mandalam", event.key)
    }

    @Test
    fun noRegistrationOrTimesAndOlderEntries() {
        val plain = parseEvents(listOf(entry("Bhajan", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00")), now).single()
        assertNull(plain.registration)  // the website leaves it out when the event takes none
        assertNull(plain.imageUrl)
        assertEquals("", plain.details)
        assertEquals("Saturday, November 14", dateLine(plain))
        val open = parseEvents(listOf(entry("x", "2026-11-14T07:00-08:00", "2026-11-14T13:00-08:00", mapOf(
            "registration" to mapOf("url" to "https://ayyappasamaaj.org/events/x/2026-11-14/register/", "opensAt" to 0L, "closesAt" to 0L),
        ))), now).single().registration!!
        assertEquals(Registration.Status.Open, open.status(Instant.parse("2027-01-01T00:00:00Z")))
    }
}
