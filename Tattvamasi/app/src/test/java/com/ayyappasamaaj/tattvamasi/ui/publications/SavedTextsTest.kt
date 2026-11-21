package com.ayyappasamaaj.tattvamasi.ui.publications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTextsTest {
    private fun text(title: String) =
        PublicationText(title, listOf(PublicationVersion("English", "English", "https://x.org/$title.pdf", "https://x.org/p/")))

    private val sections = listOf(
        PublicationSection("shlokas_puja", "Shlokas & Puja", listOf(
            PublicationCategory("14_stotram", "Stotram", "", "", listOf(text("Vishnu Sahasranamam"), text("Shastha Dashakam"))),
            PublicationCategory("15_namavali", "Namavali", "", "", listOf(text("Hariharaputra Ashtottaram"))),
        )),
    )

    @Test
    fun savingPutsATextFirstAndSavingAgainTakesItOff() {
        val one = toggledSaved(emptyList(), "14_stotram", "Vishnu Sahasranamam", 1)
        val two = toggledSaved(one, "15_namavali", "Hariharaputra Ashtottaram", 2)
        assertEquals(listOf("Hariharaputra Ashtottaram", "Vishnu Sahasranamam"), two.map { it.title })
        val back = toggledSaved(two, "14_stotram", "Vishnu Sahasranamam", 3)
        assertEquals(listOf("Hariharaputra Ashtottaram"), back.map { it.title })
    }

    @Test
    fun theSameTitleInAnotherCategoryIsAnotherText() {
        val saved = toggledSaved(toggledSaved(emptyList(), "a", "Title", 1), "b", "Title", 2)
        assertEquals(2, saved.size)
    }

    @Test
    fun theListSurvivesBeingStoredAndReadBack() {
        val saved = listOf(SavedText("14_stotram", "Vishnu Sahasranamam", 20), SavedText("15_namavali", "A\tB", 10))
        val read = decodeSaved(encodeSaved(saved))
        assertEquals(listOf("Vishnu Sahasranamam", "A B"), read.map { it.title })
        assertEquals(listOf(20L, 10L), read.map { it.at })
        assertTrue(decodeSaved(null).isEmpty())
        assertTrue(decodeSaved("broken line").isEmpty())
    }

    @Test
    fun savedTextsShowNewestFirstAndOnesNoLongerPublishedAreLeftOut() {
        val saved = listOf(
            SavedText("14_stotram", "Shastha Dashakam", 5),
            SavedText("14_stotram", "Gone From The Website", 9),
            SavedText("15_namavali", "Hariharaputra Ashtottaram", 7),
        )
        val hits = resolveSaved(saved, sections)
        assertEquals(listOf("Hariharaputra Ashtottaram", "Shastha Dashakam"), hits.map { it.text.title })
        assertEquals(listOf("Namavali", "Stotram"), hits.map { it.category.name })
    }

    @Test
    fun theLanguageButtonShowsTwoLetters() {
        assertEquals(listOf("En", "Sa", "Ta", "Te", "Ma", "Ka"),
            listOf("English", "Sanskrit", "Tamil", "Telugu", "Malayalam", "Kannada").map(LanguageCode::button))
        assertEquals("Hi", LanguageCode.button("Hindi"))
        assertEquals("Ma", LanguageCode.button("Malayalam"))  // the list badges use these codes too
    }
}
