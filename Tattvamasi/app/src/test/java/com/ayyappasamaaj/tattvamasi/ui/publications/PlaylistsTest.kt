package com.ayyappasamaaj.tattvamasi.ui.publications

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistsTest {
    /** The website's answer to GET /api/bhajans/playlists/ (bhajans/playlists.py, all_json) */
    private val answer = """
        {"playlists": [
          {"id": 7, "name": "Mandalam evenings",
           "pdfs": [{"id": 27, "url": "/bhajans/pdf/27/", "title": "Aragarogara Swami", "language": "Tamil"},
                    {"id": 2, "url": "/bhajans/pdf/2/", "title": "Gananaatha Om", "language": ""}],
           "share_url": "https://ayyappasamaaj.org/bhajans/playlists/abc123/", "updated": "2026-11-20T10:00:00+00:00"},
          {"id": 9, "name": "Playlist 2", "pdfs": [], "share_url": "https://ayyappasamaaj.org/bhajans/playlists/def/",
           "updated": "2026-11-21T10:00:00+00:00"}
        ], "max_playlists": 5, "max_pdfs": 25}
    """.trimIndent()

    private val sample: Any? = plain(
        JSONObject(javaClass.classLoader!!.getResource("publications-sample.json")!!.readText()),
    )

    private fun plain(x: Any?): Any? = when (x) {
        is JSONObject -> x.keys().asSequence().associateWith { plain(x.get(it)) }
        is JSONArray -> (0 until x.length()).map { plain(x.get(it)) }
        JSONObject.NULL -> null
        else -> x
    }

    private val sections = parsePublications(sample)

    @Test
    fun readsTheWebsitesPlaylistsInOrderWithTheirLimits() {
        val data = parsePlaylists(answer)!!
        assertEquals(5, data.maxPlaylists)
        assertEquals(25, data.maxPdfs)
        assertEquals(listOf("Mandalam evenings", "Playlist 2"), data.playlists.map { it.name })
        val first = data.playlists[0]
        assertEquals(7, first.id)
        assertEquals(listOf(27, 2), first.pdfIds)
        assertEquals("Aragarogara Swami (Tamil)", first.pdfs[0].label)
        assertEquals("Gananaatha Om", first.pdfs[1].label)
        assertEquals("https://ayyappasamaaj.org/bhajans/playlists/abc123/", first.shareUrl)
        assertFalse(data.full)
    }

    @Test
    fun fivePlaylistsIsFull() {
        val one = """{"id": 1, "name": "P", "pdfs": [], "share_url": ""}"""
        val data = parsePlaylists("""{"playlists": [$one,$one,$one,$one,$one], "max_playlists": 5, "max_pdfs": 25}""")!!
        assertTrue(data.full)
    }

    @Test
    fun somethingElseIsNotPlaylists() {
        assertNull(parsePlaylists("<html>Server error</html>"))
        assertNull(parsePlaylists("""{"detail": "Authentication credentials were not provided."}"""))
    }

    @Test
    fun keepsTheWebsitesReasons() {
        val full = """{"error": "You can keep up to 5 playlists. Delete one in your dashboard to make another."}"""
        assertEquals("You can keep up to 5 playlists. Delete one in your dashboard to make another.",
            playlistError(400, full, "fallback"))
        assertEquals("fallback", playlistError(500, "", "fallback"))
        assertTrue(playlistError(0, "", "fallback").startsWith("Couldn't reach"))
    }

    @Test
    fun knowsAPdfByItsPageAddress() {
        assertEquals(27, pdfIdOf("https://ayyappasamaaj.org/bhajans/pdf/27/"))
        assertEquals(310, pdfIdOf("/bhajans/pdf/310/"))
        assertNull(pdfIdOf("https://ayyappasamaajdrive.s3.us-west-2.amazonaws.com/pdfs/x.pdf"))
    }

    @Test
    fun findsEachPdfInPublicationsInItsLanguage() {
        val playlist = parsePlaylists(answer)!!.playlists[0].copy(
            pdfs = parsePlaylists(answer)!!.playlists[0].pdfs + PlaylistPdf(999999, "Hidden", ""))
        val entries = resolvePlaylist(playlist, sections)
        assertEquals(3, entries.size)
        assertEquals("Aragarogara Swami", entries[0].item!!.text.title)
        assertEquals("Tamil", entries[0].item!!.startLanguage)
        assertEquals("Gananaatha Om", entries[1].item!!.text.title)
        assertNull(entries[2].item)  // not in the app

        // Play skips what isn't in the app; previous / next go through the rest in order
        val play = playFrom(playlist, entries)!!
        assertEquals("Aragarogara Swami", play.text.title)
        assertNull(play.playlist!!.previous)
        val next = play.playlist!!.next!!
        assertEquals("Gananaatha Om", next.text.title)
        assertEquals(1, next.playlist!!.at)
        assertNull(next.playlist!!.next)
        assertEquals("Aragarogara Swami", next.playlist!!.previous!!.text.title)
        assertEquals("Gananaatha Om", playFrom(playlist, entries, 1)!!.text.title)
        assertNull(playFrom(playlist.copy(pdfs = listOf(PlaylistPdf(999999, "Hidden", ""))),
            listOf(PlaylistEntry(PlaylistPdf(999999, "Hidden", ""), null))))
    }

    @Test
    fun movesUpAndDown() {
        assertEquals(listOf(2, 1, 3), movedPdf(listOf(1, 2, 3), 1, 0))
        assertEquals(listOf(1, 3, 2), movedPdf(listOf(1, 2, 3), 1, 2))
        assertEquals(listOf(1, 2, 3), movedPdf(listOf(1, 2, 3), 0, -1))
        assertEquals(listOf(1, 2, 3), movedPdf(listOf(1, 2, 3), 2, 3))
    }

    @Test
    fun addsAtTheEndUpToTheLimit() {
        assertEquals(AddToPlaylist.Added(listOf(1, 2, 3)), addingPdf(listOf(1, 2), 3, 25))
        assertEquals(AddToPlaylist.AlreadyThere, addingPdf(listOf(1, 2), 2, 25))
        assertEquals(AddToPlaylist.Full, addingPdf((1..25).toList(), 26, 25))
    }

    @Test
    fun savedTextsBecomePdfIdsInTheReadingLanguage() {
        val muruga = sections.flatMap { it.categories }.first { it.slug == "04_Muruga" }
        val text = muruga.texts.first { it.title == "Aragarogara Swami" }
        val hits = listOf(PublicationHit(muruga, text), PublicationHit(muruga, text))
        assertEquals(listOf(27), savedPdfIds(hits, "Tamil", 25))
        assertEquals(listOf(310), savedPdfIds(hits, "Telugu", 25))  // no Telugu: English
        assertNotNull(savedPdfIds(hits, "English", 0).takeIf { it.isEmpty() })
    }

    @Test
    fun countsTexts() {
        assertEquals("1 text", textCount(1))
        assertEquals("7 texts", textCount(7))
    }
}
