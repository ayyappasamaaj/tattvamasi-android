package com.ayyappasamaaj.tattvamasi.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTest {
    @Test
    fun aSavedChoiceComesBack() {
        for (choice in Appearance.Choice.entries) assertEquals(choice, Appearance.parse(choice.name))
    }

    @Test
    fun nothingSavedOrUnknownFollowsThePhone() {
        assertEquals(Appearance.Choice.SYSTEM, Appearance.parse(null))
        assertEquals(Appearance.Choice.SYSTEM, Appearance.parse("SEPIA"))
    }

    @Test
    fun theChoicesReadAsOnTheWebsite() {
        assertEquals(listOf("System", "Light", "Dark"), Appearance.Choice.entries.map { it.label })
    }
}
