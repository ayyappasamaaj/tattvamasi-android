package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** My volunteer hours as the website sends them (volunteers/api.py: /api/volunteers/mine/ and mine/add/). */
class VolunteerHoursTest {
    private val hours = """{"is_volunteer": true, "year": 2026, "years": [2026, 2025],
        "records": [
          {"date": "2026-01-05", "start": "07:00", "end": "08:00", "hours": 1.0, "activity": "Setup <hall>",
           "coordinator": "", "status": "submitted", "status_label": "Submitted"},
          {"date": "2026-03-01", "start": "09:00", "end": "11:30", "hours": 2.5, "activity": "Annadanam kitchen",
           "coordinator": "Lakshmi", "status": "approved", "status_label": "Approved"}],
        "totals": {"recorded": 3.5, "approved": 2.5},
        "person": {"name": "Seva K", "email": "seva@example.com"},
        "letterhead": {"name": "Ayyappa Samaaj", "tagline": "Gathering Place for Ayyappa Devotees Worldwide",
          "place": "San Jose, CA 95120", "status": "a 501(c)(3) nonprofit organization", "ein": "45-1061428",
          "email": "info@ayyappasamaaj.org", "website": "ayyappasamaaj.org", "logo": "/static/images/logo_2x.png",
          "officers": [{"title": "President", "name": "A. Devotee"}]},
        "add_url": "/volunteers/add/", "generated": "2026-11-25"}"""

    @Test
    fun aYearsHours() {
        val h = parseVolunteerHours(hours)!!
        assertTrue(h.isVolunteer)
        assertEquals(listOf(2026, 2025), h.years)
        assertEquals(2, h.entries.size)
        assertEquals("Lakshmi", h.entries[1].coordinator)
        assertTrue(h.entries[1].approved)
        assertFalse(h.entries[0].approved)
        assertEquals(3.5, h.recorded, 0.0)
        assertEquals(2.5, h.approved, 0.0)
        assertEquals("A. Devotee", h.officers[0].name)
        assertNull(parseVolunteerHours("not json"))
    }

    @Test
    fun thePdfHasOnlyTheApprovedEntries() {
        val html = hoursLetterHtml(parseVolunteerHours(hours)!!, "https://ayyappasamaaj.org/static/images/logo_2x.png")
        assertTrue(html.contains("Annadanam kitchen"))
        assertTrue(html.contains("With Lakshmi"))
        assertFalse(html.contains("Setup"))  // waiting for a coordinator: on the screen only
        assertTrue(html.contains("Approved <b>2.5 hours</b>"))
        assertTrue(html.contains("Total approved"))
        assertTrue(html.contains("A. Devotee"))
        assertTrue(html.contains("9:00 AM–11:30 AM"))
    }

    @Test
    fun theAddHoursForm() {
        val form = parseAddHoursForm("""{"earliest": "2026-01-01", "latest": "2026-11-25",
            "times": [{"value": "09:00", "label": "9:00 AM"}, {"value": "09:15", "label": "9:15 AM"}],
            "coordinators": [{"id": 3, "name": "Lakshmi"}], "coordinator_required": true}""")!!
        assertEquals("2026-11-25", form.latest)
        assertEquals("9:15 AM", form.times[1].label)
        assertEquals(Coordinator(3, "Lakshmi"), form.coordinators[0])
        assertTrue(form.coordinatorRequired)
    }

    @Test
    fun theWebsitesAnswersToAddHours() {
        val added = parseAddHours(201, """{"message": "Added 2.5 hours of volunteering.", "year": 2026, "record": {}}""")
        assertEquals(AddHoursResult.Added("Added 2.5 hours of volunteering.", 2026), added)
        val problems = parseAddHours(400, """{"errors": {"end": ["The end time must be after the start time."],
            "activity": ["This field is required."]}}""") as AddHoursResult.Problems
        assertEquals("The end time must be after the start time.", problems.errors["end"])
        val refused = parseAddHours(403, """{"error": "Only volunteers can add hours."}""") as AddHoursResult.Problems
        assertEquals("Only volunteers can add hours.", refused.errors[""])
        assertTrue((parseAddHours(0, "") as AddHoursResult.Problems).errors[""]!!.startsWith("Couldn't reach"))
        assertEquals(AddHoursResult.SignedOut, parseAddHours(401, ""))
    }

    @Test
    fun hoursFromTheTimes() {
        assertEquals(2.5, hoursBetween("09:00", "11:30")!!, 0.0)
        assertNull(hoursBetween("11:30", "09:00"))
        assertNull(hoursBetween(null, "09:00"))
        assertEquals("2.5 hours", hoursText(2.5))
        assertEquals("1 hour", hoursText(1.0))
        assertEquals("0.25", hoursNumber(0.25))
        assertEquals("9:30 PM", clockTime("21:30"))
    }
}
