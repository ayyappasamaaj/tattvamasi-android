package com.ayyappasamaaj.tattvamasi.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The website's home page slides (/home/slides/app.json; website: pages/app_slides.py). */
class HomeSlidesTest {
    private val feed = """
        {"version": 1, "slides": [
          {"id": 1, "alt": "Ayyappa Mandala Mahotsavam", "image": "https://ayyappasamaaj.org/static/images/slider/mandala-mahotsavam_S.webp",
           "phone": true, "wide": "https://ayyappasamaaj.org/static/images/slider/mandala-mahotsavam_M.webp",
           "link": "https://ayyappasamaaj.org/events/mandalam-2026-2027/2026-11-14/", "from": null, "until": "2026-12-26"},
          {"id": 9, "alt": "Shastha Preethi", "image": "https://ayyappasamaaj.org/media/slider/9-x_M.webp",
           "phone": false, "wide": "https://ayyappasamaaj.org/media/slider/9-x_M.webp",
           "link": "https://ayyappasamaaj.org/events/shastha-preethi/2026-12-12/", "from": "2026-11-25", "until": "2026-12-12"},
          {"id": 7, "alt": "Ayyappa Samaaj", "image": "https://ayyappasamaaj.org/static/images/slider/ayyappa-samaaj_S.webp",
           "phone": true, "wide": "https://ayyappasamaaj.org/static/images/slider/ayyappa-samaaj_M.webp",
           "link": "https://ayyappasamaaj.org/about/", "from": null, "until": null},
          {"id": 8, "alt": "Not a picture address", "image": "/static/x.webp", "phone": true, "wide": "", "link": "", "from": null, "until": null}
        ]}
    """.trimIndent()

    @Test
    fun theSlidesAreReadInTheWebsitesOrder() {
        val slides = parseSlides(feed)!!
        // A slide without a full picture address is left out
        assertEquals(listOf(1, 9, 7), slides.map { it.id })
        assertTrue(slides[0].phone)
        assertFalse(slides[1].phone)
        assertEquals(LocalDate.of(2026, 12, 26), slides[0].until)
        assertNull(slides[0].from)
        assertNull(slides[2].until)
    }

    @Test
    fun todaysSlidesFromTheirDays() {
        val slides = parseSlides(feed)!!
        val nov20 = LocalDate.of(2026, 11, 20)
        assertEquals(listOf(1, 7), slides.filter { it.showingOn(nov20) }.map { it.id })
        assertEquals(listOf(1, 9, 7), slides.filter { it.showingOn(LocalDate.of(2026, 12, 12)) }.map { it.id })
        // Through its last day, gone the next
        assertEquals(listOf(1, 7), slides.filter { it.showingOn(LocalDate.of(2026, 12, 13)) }.map { it.id })
        assertEquals(listOf(7), slides.filter { it.showingOn(LocalDate.of(2026, 12, 27)) }.map { it.id })
    }

    @Test
    fun anUnusableFeedHasNone() {
        assertNull(parseSlides("""{"version": 2, "slides": []}"""))
        assertNull(parseSlides("not json"))
        assertEquals(emptyList<HomeSlide>(), parseSlides("""{"version": 1, "slides": []}"""))
    }

    @Test
    fun anEventSlideMatchesItsEventPage() {
        assertTrue(samePage("https://ayyappasamaaj.org/events/shastha-preethi/2026-12-12/",
            "https://ayyappasamaaj.org/events/shastha-preethi/2026-12-12"))
        assertTrue(samePage("http://www.ayyappasamaaj.org/about/", "https://ayyappasamaaj.org/about/"))
        assertFalse(samePage("https://ayyappasamaaj.org/events/shastha-preethi/2026-12-12/",
            "https://ayyappasamaaj.org/events/shastha-preethi/2027-12-11/"))
    }
}
