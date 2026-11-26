package com.ayyappasamaaj.tattvamasi.ui.housebhajan

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** House bhajans as the website sends them (events/house_bhajan_api.py). */
class HouseBhajansTest {
    private val page = """{"id": 412, "host_name": "Iyer family", "date": "2026-12-05", "start_time": "18:00",
        "end_time": "20:30", "when": "Saturday, Dec 5, 2026, 6:00 PM to 8:30 PM", "city": "San Jose", "tag": "Mandala Puja",
        "guest_note": "Please park on the street.", "rsvp": "link", "rsvp_link": "https://www.evite.com/event/abc",
        "status": "approved", "status_label": "Approved (on the calendar)", "approved": true, "upcoming": true,
        "can_message": true, "address": "", "phone": "", "email": "", "hidden_details": "address and phone number",
        "url": "https://ayyappasamaaj.org/calendar/house-bhajans/412/", "is_host": false}"""

    @Test
    fun aBhajansPage() {
        val b = parseBhajanPage(page)!!
        assertEquals(412, b.id)
        assertEquals("Iyer family", b.hostName)
        assertEquals("Mandala Puja", b.tag)
        assertTrue(b.approved && b.canMessage)
        assertEquals("address and phone number", b.hiddenDetails)
        assertFalse(b.isHost)
        assertNull(parseBhajanPage("nope"))
    }

    @Test
    fun myBhajansAndTheirFieldsBackInTheForm() {
        val (mine, canAdd) = parseMine("""{"bhajans": [${page.dropLast(1)}, "street": "123 Lotus Lane", "zip_code": "95120",
            "host_phone": "408 555 0100", "coordinator_note": "", "show_address": true, "show_phone": false,
            "show_email": false, "decline_reason": "", "can_change": true, "can_cancel": true}], "can_add": false}""")!!
        assertFalse(canAdd)
        val f = BhajanFields.of(mine[0])
        assertEquals("123 Lotus Lane", f.street)
        assertEquals("408 555 0100", f.phone)
        assertTrue(f.showAddress && f.agree)
        val json = f.copy(rsvp = "none").json()
        assertEquals("", json.getString("rsvp_link"))  // no RSVP needed: no link
        assertEquals(true, json.getBoolean("agree"))
    }

    @Test
    fun theFormsStart() {
        val info = parseFormInfo("""{"host_name": "Lakshmi Iyer", "phone": "", "earliest": "2026-11-26",
            "latest": "2027-11-26", "rsvp_choices": [{"value": "link", "label": "Guests RSVP through a link"},
            {"value": "none", "label": "No RSVP needed"}], "can_add": true}""")!!
        assertEquals("Lakshmi Iyer", info.hostName)
        assertEquals(listOf("link", "none"), info.rsvpChoices.map { it.value })
    }

    @Test
    fun theWebsitesAnswers() {
        val done = parseAnswer(201, """{"message": "Thank you.", "bhajan": $page}""") as BhajanAnswer.Done
        assertEquals("Thank you.", done.message)
        assertEquals(412, done.bhajan!!.id)
        val problems = parseAnswer(400, """{"errors": {"date": "The date has already passed.", "zip_code": ["Please enter a 5-digit ZIP code."]}}""")
        assertEquals(mapOf("date" to "The date has already passed.", "zip_code" to "Please enter a 5-digit ZIP code."),
            (problems as BhajanAnswer.Problems).errors)
        assertEquals(mapOf("" to "This host can't be messaged now."),
            (parseAnswer(400, """{"error": "This host can't be messaged now."}""") as BhajanAnswer.Problems).errors)
        assertEquals(BhajanAnswer.SignedOut, parseAnswer(401, ""))
        assertTrue((parseAnswer(0, "") as BhajanAnswer.Problems).errors[""]!!.startsWith("Couldn't reach"))
    }

    @Test
    fun linksAndTimes() {
        assertEquals(412, bhajanId("https://ayyappasamaaj.org/calendar/house-bhajans/412/"))
        assertNull(bhajanId("https://ayyappasamaaj.org/calendar/house-bhajans/new/"))
        assertEquals("https://staging.ayyappasamaaj.org", siteOf("https://staging.ayyappasamaaj.org/calendar/house-bhajans/9/"))
        assertEquals("6:00 PM", clock("18:00"))
        assertEquals("12:15 PM", clock("12:15"))
        assertEquals("06:00", bhajanTimes.first())
        assertEquals("23:45", bhajanTimes.last())
        assertTrue(JSONObject(BhajanFields(hostName = " Iyer ").json().toString()).getString("host_name") == "Iyer")
    }
}
