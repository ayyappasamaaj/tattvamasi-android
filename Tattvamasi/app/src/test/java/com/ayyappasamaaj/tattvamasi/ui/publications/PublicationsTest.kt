package com.ayyappasamaaj.tattvamasi.ui.publications

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicationsTest {
    /** A copy of the website's `/publications` (the iOS app's publications-sample.json), as Firebase hands it
     *  over: maps and lists. */
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
    fun readsTheWebsitesSectionsAndCategoriesInOrder() {
        assertEquals(listOf("Bhajans", "Shlokas & Puja"), sections.map { it.title })
        assertEquals(11, sections[0].categories.size)
        assertEquals("Ayyappa", sections[0].categories[9].name)
        assertEquals("114 bhajans", sections[0].count(sections[0].categories[9].texts.size))
        assertEquals("1 text", sections[1].count(1))
        val first = sections[0].categories[1].texts[0]
        assertEquals("Gananaatha Om", first.title)
        assertEquals(listOf("English"), first.languages)
    }

    @Test
    fun everyCategoryOnTheWebsiteHasItsIcon() {
        val missing = sections.flatMap { it.categories }.map { it.slug }.filterNot { hasIcon(it) }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndNeedsEveryWord() {
        assertTrue(titleMatches("Gaṇapati Stotram", "ganapati"))
        assertTrue(titleMatches("Harivarasanam Viswamohanam", "VISWA hari"))
        assertTrue(!titleMatches("Harivarasanam", "hari mohanam"))
        val hits = searchPublications(sections, "gananaatha")
        assertEquals("Ganesha", hits.first().category.name)
        assertEquals(emptyList<PublicationHit>(), searchPublications(sections, "  "))
    }

    @Test
    fun skipsMalformedEntries() {
        val value = mapOf("sections" to listOf(
            mapOf("key" to "bhajans", "title" to "Bhajans", "categories" to mapOf(
                "0" to mapOf("slug" to "x", "name" to "X", "texts" to listOf(
                    mapOf("title" to "Good", "versions" to listOf(mapOf("language" to "Tamil", "label" to "தமிழ்",
                        "fileUrl" to "https://example.org/a.pdf", "pageUrl" to "https://example.org/a/"))),
                    mapOf("title" to "No versions", "versions" to emptyList<Any>()),
                    null,
                )),
            )),
            mapOf("title" to "No key"),
        ))
        val parsed = parsePublications(value)
        assertEquals(1, parsed.size)
        assertEquals(listOf("Good"), parsed[0].categories[0].texts.map { it.title })
        assertEquals("தமிழ்", parsed[0].categories[0].texts[0].versions[0].label)
        assertEquals("TA", LanguageCode.short("Tamil"))
        assertEquals("SA", LanguageCode.short("sanskrit"))
    }

    private fun text(vararg languages: String) = PublicationText("T", languages.map {
        PublicationVersion(it, it, "https://example.org/$it.pdf", "https://example.org/")
    })

    @Test
    fun readingInOpensThePreferredLanguageElseEnglishElseTheFirst() {
        assertEquals("Tamil", openingLanguage(text("English", "Tamil"), "Tamil"))
        assertEquals("English", openingLanguage(text("Malayalam", "English"), "Tamil"))
        assertEquals("Sanskrit", openingLanguage(text("Sanskrit", "Hindi"), "Tamil"))
        assertEquals("Tamil", openingLanguage(text("English", "Tamil"), "tamil"))
        assertNull(openingLanguage(PublicationText("T", emptyList()), "Tamil"))
    }

    @Test
    fun readingLanguagesPutEnglishFirstThenTheMostUsed() {
        val languages = readingLanguages(sections.flatMap { it.categories })
        assertEquals("English", languages.first().first)
        assertTrue(languages.size > 1)
        assertEquals("தமிழ்", languages.first { it.first == "Tamil" }.second)
    }

    @Test
    fun countLineNamesTheLanguagesLikeTheWebsite() {
        val ayyappa = sections[0].categories[9]
        val line = countLine(sections[0], ayyappa)
        assertTrue(line, line.startsWith("114 bhajans in "))
        val one = PublicationCategory("x", "X", "", "", listOf(text("English")))
        assertEquals("1 bhajan in English", countLine(sections[0], one))
    }

    @Test
    fun mandalamShortlistInTheWebsitesOrderAndSeason() {
        val list = mandalamShortlist(sections)
        assertEquals(listOf("Sarana Ghosham", "Periya Padhai Sarana Ghosham", "Sabari Yatra Mala Wearing Mantra",
            "Sri Hariharatmaja Ashtakam (Harivarasanam)", "Hariharaputra Puja Vidhi", "Ayyappa Sahasranamam"),
            list.map { plainTitle(it.text.title) })
        assertEquals("08 Sarana Ghosham", list[0].text.title)
        assertTrue(inMandalamSeason(LocalDate.of(2026, 10, 1)))
        assertTrue(inMandalamSeason(LocalDate.of(2027, 1, 15)))
        assertFalse(inMandalamSeason(LocalDate.of(2027, 1, 16)))
        assertFalse(inMandalamSeason(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun recentSearchesNewestFirstNoRepeatsFiveAtMost() {
        var list = emptyList<String>()
        listOf("ganesha", "sarana", "Ganesha ", "a", "b", "c", "d", "  ").forEach { list = withRecent(list, it) }
        assertEquals(listOf("d", "c", "b", "a", "Ganesha"), list)
    }

    @Test
    fun previousAndNextCrossFromBhajansToShlokas() {
        val all = allCategories(sections)
        val lastBhajans = all.indexOfLast { it.first.key == "bhajans" }
        assertEquals("shlokas_puja", all[lastBhajans + 1].first.key)
        assertEquals(sections.sumOf { it.categories.size }, all.size)
    }
}
