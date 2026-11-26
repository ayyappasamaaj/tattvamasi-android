package com.ayyappasamaaj.tattvamasi.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNowTest {
    private val record = mapOf(
        "title" to "Mandala Puja", "youtubeId" to "abcDEF12345", "url" to "https://www.youtube.com/live/abcDEF12345",
        "facebookUrl" to "https://www.facebook.com/ayyappa.samaaj.us/videos/1", "pageUrl" to "https://ayyappasamaaj.org/live/",
        "startedAt" to 1795000000L, "thumbnailUrl" to "https://i.ytimg.com/vi/abcDEF12345/hqdefault.jpg",
        "eventName" to "Mandala Puja 2026", "eventLink" to "https://ayyappasamaaj.org/events/mandala-puja/2026-12-06/",
    )

    @Test fun `nothing live is null`() {
        assertNull(parseLive(null))
        assertNull(parseLive("x"))
        assertNull(parseLive(mapOf("title" to "No link")))
    }

    @Test fun `a youtube stream plays in the app`() {
        val live = parseLive(record)!!
        assertEquals("Mandala Puja", live.title)
        assertEquals("abcDEF12345", live.youtubeId)
        assertEquals(1795000000L, live.startedAt)
        assertEquals("Mandala Puja 2026", live.eventName)
        assertTrue(live.playsInApp)
    }

    @Test fun `a facebook only stream opens outside`() {
        val live = parseLive(record + mapOf("youtubeId" to "", "url" to "https://www.facebook.com/x/videos/1"))!!
        assertFalse(live.playsInApp)
    }

    @Test fun `an odd youtube id is not put in the player`() {
        assertFalse(parseLive(record + ("youtubeId" to "abc\"><script>"))!!.playsInApp)
    }

    @Test fun `missing fields have defaults`() {
        val live = parseLive(mapOf("title" to "Bhajans", "url" to "https://youtu.be/abcDEF12345", "startedAt" to 5.0))!!
        assertEquals("https://ayyappasamaaj.org/live/", live.pageUrl)
        assertEquals(5L, live.startedAt)
        assertEquals("", live.facebookUrl)
    }

    @Test fun `the live page is recognized`() {
        assertTrue(isLivePage("https://ayyappasamaaj.org/live/"))
        assertTrue(isLivePage("https://www.ayyappasamaaj.org/live"))
        assertFalse(isLivePage("https://ayyappasamaaj.org/live/old/"))
        assertFalse(isLivePage("https://ayyappasamaaj.org/events/"))
    }

    @Test fun `the player uses the privacy enhanced embed`() {
        assertTrue(livePlayerHtml("abcDEF12345").contains("https://www.youtube-nocookie.com/embed/abcDEF12345?autoplay=1"))
    }
}
