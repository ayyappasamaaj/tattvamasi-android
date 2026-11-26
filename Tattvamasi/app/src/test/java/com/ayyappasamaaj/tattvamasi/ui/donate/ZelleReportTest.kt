package com.ayyappasamaaj.tattvamasi.ui.donate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "I sent a Zelle gift" as the website answers it (donations/api/views.py ZelleReportAPI). */
class ZelleReportTest {
    private val form = """{"enabled": true, "zelle_email": "treasurer@ayyappasamaaj.org", "zelle_name": "AYYAPPA SAMAAJ",
        "memo_code": "AYS-4821", "instructions": "Send it first: in your banking app, pay treasurer@ayyappasamaaj.org",
        "note": "The treasurer checks it in the bank, usually within a week.",
        "categories": [{"id": 1, "label": "General donation"}, {"id": 9, "label": "Something else (say what in the note)"}],
        "initial": {"given_on": "2026-11-26", "sender_name": "Devi R", "memo_code": "AYS-4821"},
        "earliest": "2025-10-22", "latest": "2026-11-26"}"""

    @Test
    fun theForm() {
        val f = parseZelleForm(form)!!
        assertTrue(f.enabled)
        assertEquals("AYS-4821", f.memoCode)
        assertEquals("treasurer@ayyappasamaaj.org", f.zelleEmail)
        assertEquals(listOf(ZelleCategory(1, "General donation"), ZelleCategory(9, "Something else (say what in the note)")),
            f.categories)
        assertEquals("2026-11-26", f.givenOn)
        assertEquals("Devi R", f.senderName)
        assertFalse(parseZelleForm("""{"enabled": false}""")!!.enabled)
        assertNull(parseZelleForm("not json"))
    }

    @Test
    fun reported() {
        val body = """{"id": 5, "memo_code": "AYS-4821", "amount": "${'$'}108", "given_on": "2026-11-26",
            "category": "General donation", "state": "waiting", "thanks": "We've noted your Zelle donation of ${'$'}108",
            "memo_text": "Its memo code is AYS-4821.", "status_text": "It shows in My donations as Waiting for the treasurer."}"""
        val result = parseZelleReport(201, body) as ZelleResult.Reported
        assertEquals("AYS-4821", result.gift.memoCode)
        assertEquals("waiting", result.gift.state)
        assertEquals("\$108", result.gift.amount)
    }

    @Test
    fun problems() {
        val result = parseZelleReport(400, """{"errors": {"given_on": "The date can't be in the future.", "": "Bad"}}""")
            as ZelleResult.Problems
        assertEquals("The date can't be in the future.", result.errors["given_on"])
        assertEquals(ZelleResult.SignedOut, parseZelleReport(401, ""))
        assertEquals("Couldn't reach ayyappasamaaj.org. Check your connection and try again.",
            (parseZelleReport(0, "") as ZelleResult.Problems).errors[""])
        assertEquals("Zelle donations aren't open right now.",
            (parseZelleReport(404, """{"error": "Zelle donations aren't open right now."}""") as ZelleResult.Problems).errors[""])
    }
}
