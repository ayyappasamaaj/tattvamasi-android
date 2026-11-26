package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Volunteer tools reports as the website sends them (core/app_reports.py, /api/reports/<kind>/). */
class ReportsTest {
    private val body = """{"title": "Mandalam 2026-27", "subtitle": "Registration form report",
        "web_url": "https://ayyappasamaaj.org/forms/mandalam/reports/",
        "filters": [{"name": "year", "label": "Year", "value": "2026", "options": [{"value": "2026", "label": "2026"}, {"value": "2025", "label": "2025"}]}],
        "sections": [
          {"title": "RSVP", "note": "", "search": false, "stats": [{"label": "Devotees attending", "value": "42"}], "rows": []},
          {"title": "Everyone", "note": "", "search": true, "stats": [],
           "rows": [{"title": "Arjun", "value": "${'$'}54", "lines": ["Registration", "l@example.com"], "children": [], "tone": ""},
                    {"title": "Meera <K>", "value": "", "lines": ["Registration"], "children": ["✓ Meera"], "tone": "warn"}]}]}"""

    @Test
    fun aReport() {
        val r = parseReport(body)!!
        assertEquals("Mandalam 2026-27", r.title)
        assertEquals("2025", r.filters[0].options[1].value)
        assertEquals(listOf("Devotees attending" to "42"), r.sections[0].stats)
        assertTrue(r.sections[1].search)
        assertEquals("warn", r.sections[1].rows[1].tone)
        assertEquals(listOf("✓ Meera"), r.sections[1].rows[1].children)
        assertNull(parseReport("{}"))
    }

    @Test
    fun searchingAndFilters() {
        val rows = parseReport(body)!!.sections[1].rows
        assertEquals(listOf("Arjun"), searchRows(rows, "l@EXAMPLE").map { it.title })
        assertEquals(2, searchRows(rows, " ").size)
        assertEquals("/api/reports/board/?year=2025", reportPath("/api/reports/board/", "year", "2025"))
        assertEquals("/api/reports/expenses/?a=1&period=vishu%3A2027", reportPath("/api/reports/expenses/?a=1&period=x", "period", "vishu:2027"))
    }

    @Test
    fun thePdfHasEverything() {
        val html = reportHtml(parseReport(body)!!)
        assertTrue(html.contains("Devotees attending: <b>42</b>"))
        assertTrue(html.contains("Meera &lt;K&gt;"))
        assertTrue(html.contains("l@example.com"))
        assertFalse(html.contains("<K>"))
    }
}
