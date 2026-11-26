package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Volunteer tools as the website lists them (/api/core/my-pages/, core/user_pages.py). */
class VolunteerToolsTest {
    private val body = """{"pages": [
        {"section": "Check-in", "title": "Mandalam 2026-27", "url": "/forms/mandalam/checkin/", "description": "Check people in at the event"},
        {"section": "Reports", "title": "Mandalam 2026-27", "url": "/forms/mandalam/reports/"},
        {"section": "Check-in", "title": "All check-in pages", "url": "/forms/checkin/"},
        {"section": "Treasurer", "title": "Zelle donations (2 waiting)", "url": "/donate/zelle/review/"},
        {"section": "", "title": "Something new", "url": "/new/"},
        {"section": "Admin", "title": "", "url": "/admin/"}]}"""

    @Test
    fun pagesBySectionInTheWebsitesOrder() {
        val pages = parseToolPages(body)
        assertEquals(5, pages.size)  // one without a title is left out
        assertEquals(listOf("Check-in", "Reports", "Treasurer", "Other"), toolSections(pages).map { it.first })
        assertEquals(2, toolSections(pages)[0].second.size)
        assertEquals("Check people in at the event", pages[0].description)
        assertTrue(toolUrl(pages[0]).endsWith("/forms/mandalam/checkin/"))
        assertTrue(toolUrl(pages[0]).startsWith("http"))
        assertEquals(emptyList<ToolPage>(), parseToolPages("not json"))
    }
}
