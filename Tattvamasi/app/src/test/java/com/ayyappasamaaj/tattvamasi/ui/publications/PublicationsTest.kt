package com.ayyappasamaaj.tattvamasi.ui.publications

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
}
