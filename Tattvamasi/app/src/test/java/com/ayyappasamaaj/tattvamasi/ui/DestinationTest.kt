package com.ayyappasamaaj.tattvamasi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationTest {
    @Test
    fun theSectionsMatchTheIosApp() {
        // Same five sections, same order and names as the iOS app's tab bar
        assertEquals(
            listOf("Home", "Events", "Publications", "Sabarimala", "About us"),
            Destination.entries.map { it.label },
        )
    }
}
