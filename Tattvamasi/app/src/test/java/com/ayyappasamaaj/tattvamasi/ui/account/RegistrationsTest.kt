package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** My registrations from the website's /api/signups/mine/ (website #727: amounts, details, answers, WhatsApp). */
class RegistrationsTest {
    private val body = """{"registrations": [
        {"code": "ca9ptVQ", "title": "Mandala Mahotsavam 2026-27", "status": "paid", "status_label": "Paid",
         "total": "162.00", "registered_at": "2026-11-25T09:50:00-08:00",
         "items": [{"name": "Kids Mala Dharana", "quantity": 2, "amount": "108.00", "details": ["Anu", "Ravi"]},
                   {"name": "Pushpabhishekam Puja Sponsor", "quantity": 1, "amount": "54.00", "details": []}],
         "amount_paid": "162.00", "answers": [{"question": "City / Location", "answer": "Fremont / Vicinity"}],
         "whatsapp_group_url": "https://chat.whatsapp.com/x", "whatsapp_group_note": "Join for updates.",
         "url": "/forms/mandalam-2026/thanks/t/", "form_active": true, "year": 2026, "registered_by": null,
         "event": {"name": "Mandalam 2026-27", "url": "/e", "calendar_url": "/c", "start_date": "2026-12-20",
           "end_date": "2026-12-20", "start_time": "09:00", "end_time": "13:00", "venue": "SVCC Temple", "is_upcoming": true}},
        {"code": "KGP0001", "title": "Kids Ganesha Puja 2027", "status": "pending", "status_label": "Payment pending",
         "total": "35.00", "registered_at": "2026-11-25T10:00:00-08:00", "items": [], "amount_paid": null, "answers": [],
         "whatsapp_group_url": "", "whatsapp_group_note": "", "url": "/x", "form_active": true, "year": 2026,
         "registered_by": "Priya Kumar",
         "event": {"name": "Kids Ganesha Puja", "url": "/e2", "calendar_url": "/c2", "start_date": "2027-08-29",
           "end_date": "2027-08-29", "start_time": "09:30", "end_time": "12:00", "venue": "SVCC Temple", "is_upcoming": true}},
        {"code": "OLD2025", "title": "Mandala Mahotsavam 2025-26", "status": "free", "status_label": "Registered",
         "total": "0.00", "registered_at": "2025-11-10T10:00:00-08:00", "items": [], "url": "/y", "form_active": false,
         "year": 2025, "registered_by": null, "event": null}]}"""

    private val list = parseRegistrations(body)

    @Test
    fun everythingThePageShows() {
        val r = list[0]
        assertEquals("paid", r.state)
        assertEquals(RegistrationItem("Kids Mala Dharana", 2, "108.00", listOf("Anu", "Ravi")), r.items[0])
        assertEquals("162.00", r.amountPaid)
        assertEquals(listOf("City / Location" to "Fremont / Vicinity"), r.answers)
        assertEquals("https://chat.whatsapp.com/x", r.whatsappUrl)
        assertEquals("2 × Kids Mala Dharana, Pushpabhishekam Puja Sponsor", itemsText(r))
        assertNull(list[1].amountPaid)
    }

    @Test
    fun theNextEventAndThePast() {
        assertEquals("ca9ptVQ", nextRegistration(list)?.code)  // Dec 20, 2026 before Aug 29, 2027
        assertEquals(listOf("ca9ptVQ", "KGP0001"), currentRegistrations(list).map { it.code })
        assertEquals(listOf(2025 to listOf("OLD2025")), pastRegistrations(list).map { (y, l) -> y to l.map { it.code } })
        assertNull(nextRegistration(list.drop(2)))
    }

    @Test
    fun whenAndTheCalendar() {
        assertEquals("Sun, Dec 20, 2026 · 9:00 AM", eventWhen(list[0].event!!))
        val event = calendarEvent(list[0])!!
        assertEquals(9, event.start.atZone(ZoneId.of("America/Los_Angeles")).hour)
        assertEquals(13, event.end.atZone(ZoneId.of("America/Los_Angeles")).hour)
        assertTrue(event.venue == "SVCC Temple")
        assertEquals("Nov 14 – Dec 26, 2026",
            eventWhen(RegistrationEvent("M", "2026-11-14", "2026-12-26", "07:00", "13:00", "SVCC Temple", true)))
    }
}
