package com.ayyappasamaaj.tattvamasi.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequiredUpdateTest {
    @Test
    fun olderVersionsNeedTheUpdate() {
        assertTrue(isOlder("4.0", "4.0.1"))
        assertTrue(isOlder("4.0.1", "4.1"))
        assertTrue(isOlder("4.9", "10.0"))
        assertTrue(isOlder("3.3", "4"))
    }

    @Test
    fun theSameOrNewerVersionDoesNot() {
        assertFalse(isOlder("4.0.1", "4.0.1"))
        assertFalse(isOlder("4.0", "4"))
        assertFalse(isOlder("4.1", "4.0.1"))
        assertFalse(isOlder("10.0", "9.9"))
    }

    @Test
    fun noMinimumNeverBlocks() {
        assertFalse(isOlder("4.0", ""))
        assertFalse(isOlder("4.0", "  "))
    }

    @Test
    fun theConsoleValueCanBeTextOrANumber() {
        assertEquals("4.0.1", minimumText("4.0.1"))
        assertEquals("4.1", minimumText(4.1))
        assertEquals("", minimumText(null))
    }
}
