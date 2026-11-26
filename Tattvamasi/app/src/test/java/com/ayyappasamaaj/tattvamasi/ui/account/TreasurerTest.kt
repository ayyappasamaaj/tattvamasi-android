package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The treasurer's tools as the website answers them (donations/treasurer_api.py). */
class TreasurerTest {
    @Test
    fun theZelleList() {
        val l = parseZelle("""{"waiting": [{"id": 7, "memo_code": "AYS-4821", "donor": "Devi R", "email": "donor@example.com",
            "amount": "${'$'}108", "given_on": "2026-11-02", "sender_name": "Devi R", "category": "General", "memo": "Mandalam"}],
            "reviewed": [{"memo_code": "AYS-1000", "donor": "Ravi", "email": "", "amount": "${'$'}51", "given_on": "2026-10-01",
              "state": "not_found", "by": "Tara", "note": ""}],
            "categories": [{"id": 1, "label": "General"}]}""")!!
        assertEquals(7, l.waiting[0].id)
        assertEquals("AYS-4821", l.waiting[0].memoCode)
        assertEquals("not_found", l.reviewed[0].state)
        assertEquals(GiftCategory(1, "General"), l.categories[0])
        assertNull(parseZelle("{}"))
    }

    @Test
    fun theChecksPage() {
        val c = parseChecks("""{"accounts": [{"id": 3, "name": "Devi R", "email": "donor@example.com"}],
            "categories": [{"id": 1, "label": "General"}],
            "recent": [{"id": 9, "donor": "Devi R", "amount": "${'$'}251", "paid_by": "Check #1042, Chase", "date": "2026-11-20", "category": "General"}]}""")!!
        assertEquals(DonorAccount(3, "Devi R", "donor@example.com"), c.accounts[0])
        assertEquals("Check #1042, Chase", c.recent[0].paidBy)
    }

    @Test
    fun theWebsitesAnswers() {
        assertEquals(TreasurerAnswer.Done("Confirmed AYS-4821; donor@example.com gets a receipt."),
            parseTreasurerAnswer(200, """{"message": "Confirmed AYS-4821; donor@example.com gets a receipt."}"""))
        assertEquals(mapOf("check_number" to "This check is already recorded."),
            (parseTreasurerAnswer(400, """{"errors": {"check_number": "This check is already recorded."}}""") as TreasurerAnswer.Problems).errors)
        assertEquals(mapOf("" to "AYS-4821 was already reviewed."),
            (parseTreasurerAnswer(400, """{"error": "AYS-4821 was already reviewed."}""") as TreasurerAnswer.Problems).errors)
        assertEquals(mapOf("" to "Only the treasurer's group can use this."),
            (parseTreasurerAnswer(403, """{"detail": "Only the treasurer's group can use this."}""") as TreasurerAnswer.Problems).errors)
        assertEquals(TreasurerAnswer.SignedOut, parseTreasurerAnswer(401, ""))
    }

    @Test
    fun whichToolsOpenInTheApp() {
        assertTrue(ToolPage("Treasurer", "Zelle donations (2 waiting)", "/donate/zelle/review/", "", appScreen = "zelle").inApp)
        assertTrue(ToolPage("Reports", "Volunteers", "/volunteers/reports/volunteers/", "", appReport = "/api/reports/volunteers/").inApp)
        assertFalse(ToolPage("Treasurer", "Expense management", "/donate/event-expenses/", "").inApp)
        assertEquals("checks", parseToolPages("""{"pages": [{"section": "Treasurer", "title": "Check and cash donations",
            "url": "/donate/checks/", "app_screen": "checks"}]}""")[0].appScreen)
    }
}

class ExpenseFormTest {
    @Test
    fun theExpenseForm() {
        val f = parseExpenseForm("""{"events": [{"id": 1, "name": "Mandalam", "has_weekends": true}, {"id": 2, "name": "Vishu", "has_weekends": false}],
            "default_event": 1, "categories": {"1": [{"group": "Flowers", "options": [{"id": 5, "label": "Flowers (general)"}, {"id": 6, "label": "Garlands"}]}],
              "2": []},
            "weekends": [{"value": "auto", "label": "The nearest weekend to the date"}, {"value": "none", "label": "Not a weekend"}],
            "web_url": "https://ayyappasamaaj.org/donate/event-expenses/"}""")!!
        assertEquals(1, f.defaultEvent)
        assertTrue(f.events[0].hasWeekends)
        assertEquals("Garlands", f.categories[1]!![0].options[1].label)
        assertEquals(emptyList<CategoryGroup>(), f.categories[2])
        assertEquals("auto" to "The nearest weekend to the date", f.weekends[0])
        assertTrue(ToolPage("Treasurer", "Expense management", "/donate/event-expenses/", "", appScreen = "expense").inApp)
    }
}
