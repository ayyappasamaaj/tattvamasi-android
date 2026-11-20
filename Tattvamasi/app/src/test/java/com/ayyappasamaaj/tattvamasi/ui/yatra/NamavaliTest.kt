package com.ayyappasamaaj.tattvamasi.ui.yatra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The names' recordings, for the Namavali player (the website's "au": each name's clip). */
class NamavaliTest {
    private val feed = parseYatraFeed(File("src/main/assets/yatra.json").readText())!!
    private val data = parseYatraData(File("src/main/assets/yatra-data.json").readText())!!

    @Test
    fun everyNameHasItsRecordingAsAFullAddress() {
        assertEquals(1002, data.names.size)
        assertTrue(data.names.values.all { it.au.startsWith("https://ayyappasamaaj.org/media/stotrams/namavali/") })
        assertEquals("https://ayyappasamaaj.org/media/stotrams/namavali/dharma-shastha-sahasranamam/0001.m4a", data.names[1]!!.au)
    }

    @Test
    fun thePlayerGoesThroughTheRecordedNamesInOrder() {
        val recorded = recordedNames(Yatra(feed, data))
        assertEquals((1..1002).toList(), recorded.map { it.first })
        assertTrue(recorded.first().third.startsWith("Name 1: "))
        assertEquals(data.names[1002]!!.au, recorded.last().second)
    }

    @Test
    fun aNameWithoutARecordingIsLeftOut() {
        val text = """{"verses": {"1": {"names": [1, 2], "text": "a b", "marks": []}},
            "names": [{"n": 1, "d": "क", "t": "ka", "m": "one", "au": "/media/x/0001.m4a"},
                      {"n": 2, "d": "ख", "t": "kha", "m": "two"}]}"""
        val small = parseYatraData(text)!!
        assertEquals("https://ayyappasamaaj.org/media/x/0001.m4a", small.names[1]!!.au)
        assertEquals("", small.names[2]!!.au)
        assertEquals(listOf(1), recordedNames(Yatra(feed, small)).map { it.first })
    }

    @Test
    fun theSpeedsAreTheWebsites() {
        assertEquals(listOf(0.75f, 1f, 1.25f, 1.5f, 2f), NamavaliPlayer.SPEEDS)
    }

    @Test
    fun reciteWithMeGivesTheListenerTheNamesOwnLength() {
        // A 1.45 s clip has 1.0 s of name (0.45 s of built-in silence), plus a half-second breath
        assertEquals(1300L, NamavaliPlayer.reciteMs(1450, "standard"))
        assertEquals(1500L, NamavaliPlayer.reciteMs(1450, "long"))
        assertEquals(1100L, NamavaliPlayer.reciteMs(1450, "short"))
        // Unknown length: as if 2.5 s of name
        assertEquals(2500L, NamavaliPlayer.reciteMs(0, "standard"))
        assertEquals(listOf("", "standard", "long", "short"), NamavaliPlayer.RECITES)
        assertEquals("Off", NamavaliPlayer.RECITE_WORD[""])
    }
}
