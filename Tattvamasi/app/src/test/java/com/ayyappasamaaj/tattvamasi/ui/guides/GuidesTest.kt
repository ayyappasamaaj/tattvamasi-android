package com.ayyappasamaaj.tattvamasi.ui.guides

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidesTest {
    private val sample = javaClass.classLoader!!.getResource("guides-sample.json")!!.readText()
    private val guides = parseGuides(sample)!!

    @Test
    fun readsTheWebsitesFiveGuides() {
        assertEquals(listOf("sabarimala", "traditional-path", "vratam", "virtual-q", "house-puja"), guides.map { it.slug })
        val sabarimala = guides[0]
        assertEquals("https://ayyappasamaaj.org/sabarimala/", sabarimala.url)
        assertEquals(listOf("kulathupuzha", "aryankavu", "achankovil", "erumeli", "sabarimala"), sabarimala.sections.map { it.id })
        assertTrue(sabarimala.sections[0].blocks[0] is GuideBlock.Picture)
        // Cards inside cards: the Yatra's stops
        val yatra = guides[1].sections.first { it.id == "the-yatra" }
        assertTrue("erumeli-petta-thullal" in yatra.sections.map { it.id })
        assertEquals(listOf("the-yatra", "karimala"), guides[1].pathTo("karimala"))
        assertEquals(emptyList<String>(), guides[1].pathTo("nowhere"))
        // Every picture, once, to keep for offline reading
        assertEquals(48, guides.flatMap { it.pictures }.distinct().size)
    }

    @Test
    fun skipsWhatItCantUse() {
        assertNull(parseGuides("""{"version": 2, "guides": []}"""))
        assertNull(parseGuides("<html>error</html>"))
        val one = parseGuides(
            """{"version": 1, "guides": [{"slug": "x", "title": "X", "url": "https://ayyappasamaaj.org/x/", "intro": [],
               "sections": [{"id": "", "title": "Empty", "blocks": [], "sections": []},
                            {"id": "", "title": "Kept", "blocks": [{"html": " "}, {"image": "http://insecure/a.webp", "alt": ""},
                              {"html": "<p>Hi</p>"}], "sections": []}]}]}""",
        )!!.single()
        assertEquals(listOf(GuideSection("Kept", "Kept", listOf(GuideBlock.Text("<p>Hi</p>")), emptyList())), one.sections)
    }

    @Test
    fun websiteLinksToAGuideOpenItInTheApp() {
        assertEquals(GuideTarget("vratam", "food"), guideTarget("https://ayyappasamaaj.org/vratam/#food", guides))
        assertEquals(GuideTarget("vratam"), guideTarget("https://www.ayyappasamaaj.org/vratam", guides))
        assertEquals(GuideTarget("virtual-q"), guideTarget("https://ayyappasamaaj.org/sabarimala-virtual-q/?x=1", guides))
        // A section the guide doesn't have: the guide's top
        assertEquals(GuideTarget("vratam"), guideTarget("https://ayyappasamaaj.org/vratam/#gone", guides))
        assertNull(guideTarget("https://ayyappasamaaj.org/events/", guides))
        assertNull(guideTarget("https://sabarimalaonline.org", guides))
        assertNull(guideTarget("https://ayyappasamaaj.org.evil.example/vratam/", guides))
        assertEquals(GuideTarget("vratam", "food"), GuideTarget.decode(GuideTarget("vratam", "food").encode()))
        assertEquals(GuideTarget("vratam"), GuideTarget.decode("vratam"))
    }
}
