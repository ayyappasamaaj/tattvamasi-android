package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The year-end summary and donations lists as the website sends them (core/year_summary.py, donations/api). */
class SummaryTest {
    private val summary = """{"year": 2026, "years": [2026, 2025], "scope": "me",
        "household": {"name": "Kumar household", "members": ["Lakshmi Kumar"]},
        "organization": {"name": "Ayyappa Samaaj", "ein": "45-1061428", "place": "San Jose, CA 95120",
          "status": "a 501(c)(3) nonprofit organization"},
        "letterhead": {"name": "Ayyappa Samaaj", "tagline": "Gathering Place for Ayyappa Devotees Worldwide",
          "place": "San Jose, CA 95120", "status": "a 501(c)(3) nonprofit organization", "ein": "45-1061428",
          "email": "info@ayyappasamaaj.org", "website": "ayyappasamaaj.org", "logo": "/static/images/logo_2x.png",
          "officers": [{"title": "President", "name": "A. Devotee"}]},
        "person": {"name": "Devotee Kumar", "email": "devotee@example.com"},
        "donations": [{"date": "2026-01-14", "description": "General (monthly)", "by": null, "amount": "54.00",
          "receipt_url": "/donate/12/confirm/"},
          {"date": "2026-03-02", "description": "Annadanam (Zelle)", "by": null, "amount": "1001.00", "receipt_url": ""}],
        "registrations": [{"date": "2026-11-02", "description": "Mandala Mahotsavam 2026-27",
          "items": "Adult Mala Dharana & Irumudi, 2 × Additional Dhoti", "code": "sDEnad7", "by": null,
          "amount": "277.00", "goods_value": "70.00"}],
        "totals": {"donations": "1055.00", "registrations": "277.00", "goods_value": "70.00", "all": "1332.00"},
        "statement": "Goods or services worth $70.00 were provided in exchange for the registrations marked with a value; apart from those, only intangible religious benefits were provided.",
        "generated": "2026-11-25"}"""

    @Test
    fun theSummaryReadsEverythingTheLetterNeeds() {
        val s = parseSummary(summary)!!
        assertEquals(2026, s.year)
        assertEquals(listOf(2026, 2025), s.years)
        assertEquals("Kumar household", s.householdName)
        assertEquals("Devotee Kumar", s.personName)
        assertEquals(2, s.donations.size)
        assertEquals("Annadanam (Zelle)", s.donations[1].description)
        assertNull(s.donations[0].by)
        assertEquals("70.00", s.registrations[0].goodsValue)
        assertEquals("1332.00", s.totalAll)
        assertEquals(listOf(Officer("President", "A. Devotee")), s.officers)
    }

    @Test
    fun theLetterHasTheTablesTotalsAndStatement() {
        val html = summaryLetterHtml(parseSummary(summary)!!, "https://ayyappasamaaj.org/static/images/logo_2x.png")
        assertTrue(html.contains("2026 year-end summary"))
        assertTrue(html.contains("<b>Devotee Kumar</b> (devotee@example.com)"))
        assertTrue(html.contains("EIN 45-1061428"))
        assertTrue(html.contains("$1,001.00"))
        assertTrue(html.contains("Goods received"))
        assertTrue(html.contains("2 × Additional Dhoti"))
        assertTrue(html.contains("Total for 2026: <b>$1,332.00</b>"))
        assertTrue(html.contains("Goods or services worth $70.00"))
        assertTrue(html.contains("A. Devotee"))
        // Names are text, never markup
        val odd = parseSummary(summary.replace("Devotee Kumar", "<b>Kumar</b> & Co"))!!
        assertTrue(summaryLetterHtml(odd, "").contains("&lt;b&gt;Kumar&lt;/b&gt; &amp; Co"))
    }

    @Test
    fun anEmptyYearSaysSo() {
        val empty = summary.replace(Regex(""""donations": \[.*?\],\s*"registrations": \[.*?\],""", RegexOption.DOT_MATCHES_ALL),
            """"donations": [], "registrations": [],""")
        val s = parseSummary(empty)!!
        assertTrue(s.donations.isEmpty() && s.registrations.isEmpty())
        val html = summaryLetterHtml(s, "")
        assertTrue(html.contains("Nothing recorded for 2026."))
        assertFalse(html.contains("Total for 2026"))
    }

    @Test
    fun aBrokenAnswerIsNull() {
        assertNull(parseSummary("<html>"))
        assertNull(parseSummary("""{"year": 2026}"""))
    }

    @Test
    fun donationPagesAndZelleGifts() {
        val page = parseDonationsPage("""{"count": 12, "next": "https://x/api/donations/mine/?page=2", "previous": null,
            "results": [{"id": 9, "title": "General", "date": "2026-11-20T10:15:00-08:00", "amount": "108.00",
              "donated_by": null, "method": "card", "memo_code": null},
              {"id": 8, "title": "Annadanam", "date": "2026-03-02", "amount": "1001.00", "donated_by": "Lakshmi Kumar",
              "method": "zelle", "memo_code": "AYS-4821"}]}""")
        assertTrue(page.more)
        assertEquals("2026-11-20", page.donations[0].date)
        assertEquals("AYS-4821", page.donations[1].memoCode)
        assertNull(page.donations[0].memoCode)
        assertEquals("Lakshmi Kumar", page.donations[1].donatedBy)
        assertFalse(parseDonationsPage("""{"next": null, "results": []}""").more)

        val zelle = parseZelleGifts("""[{"id": 3, "title": "General (Zelle)", "date": "2026-11-24", "amount": "51.00",
            "state": "waiting", "memo_code": "AYS-1234"}]""")
        assertEquals(ZelleGift(3, "General (Zelle)", "2026-11-24", "51.00", "waiting", "AYS-1234"), zelle.single())
    }

    @Test
    fun daysAndDollars() {
        assertEquals("Nov 25, 2026", summaryDay("2026-11-25"))
        assertEquals("$1,250.00", summaryDollars("1250"))
    }
}
