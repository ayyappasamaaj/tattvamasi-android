package com.ayyappasamaaj.tattvamasi.ui.shruti

import org.junit.Assert.assertEquals
import org.junit.Test

/** The tanpura's keys, their kattai, the loops' names and the remembered settings (ShrutiPlayer.kt). */
class ShrutiTest {
    @Test
    fun keysInTheSheetsOrderWithTheirKattai() {
        assertEquals(listOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"), Shruti.KEYS.map { it.note })
        assertEquals(listOf("6", "6½", "7", "1", "1½", "2", "2½", "3", "4", "4½", "5", "5½"), Shruti.KEYS.map { it.kattai })
        assertEquals("C", Shruti.DEFAULT_KEY.note)
    }

    @Test
    fun everyKeyAndTuningHasItsLoop() {
        assertEquals("tanpura_cs_pa", Shruti.fileName(Shruti.key("C#"), Shruti.PA))
        assertEquals("tanpura_a_ma", Shruti.fileName(Shruti.key("A"), Shruti.MA))
        assertEquals("tanpura_gs_pa", Shruti.fileName(Shruti.key("G#"), "anything else"))
        val names = Shruti.KEYS.flatMap { k -> listOf(Shruti.PA, Shruti.MA).map { Shruti.fileName(k, it) } }
        assertEquals(24, names.toSet().size)
        assertEquals(names.toSet(), Shruti.RAW.keys)
    }

    @Test
    fun labelsTitlesAndWhatAScreenReaderSays() {
        val cs = Shruti.key("C#")
        assertEquals("C# · 1½ · Pa", Shruti.label(cs, Shruti.PA))
        assertEquals("Tanpura · C# (1½) · Sa–Ma", Shruti.title(cs, Shruti.MA))
        assertEquals("C sharp, kattai one and a half", Shruti.spoken(cs))
    }

    @Test
    fun rememberedSettingsAreCheckedAndFallBackToCPa80() {
        assertEquals(ShrutiSettings(Shruti.key("D#"), Shruti.MA, 0.5f), Shruti.settings("D#", "ma", 0.5f))
        assertEquals(ShrutiSettings(Shruti.DEFAULT_KEY, Shruti.PA, 0.8f), Shruti.settings(null, null, null))
        assertEquals(ShrutiSettings(Shruti.DEFAULT_KEY, Shruti.PA, 1f), Shruti.settings("H", "ni", 3f))
        assertEquals(0.8f, Shruti.settings("C", "pa", Float.NaN).volume)
    }
}
