package com.ayyappasamaaj.tattvamasi.ui.yatra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** The yatra worked out as the website's script does (expected values from static/js/src/pages/sahasranamaYatra.js). */
class YatraTest {
    private val feed = parseYatraFeed(File("src/main/assets/yatra.json").readText())!!
    private val data = parseYatraData(File("src/main/assets/yatra-data.json").readText())!!
    private val yatra = Yatra(feed, data)
    private val start = LocalDate.of(2026, 11, 17)

    @Test
    fun readsTheWebsitesSettings() {
        assertEquals(4, feed.versesPerDay)
        assertEquals(10, feed.stages.size)
        assertEquals("Erumeli", feed.stages.first())
        assertEquals("Sannidhanam", feed.stages.last())
        assertEquals(298, feed.nameOfDay[11])
        assertTrue(feed.reflections[12]!!.carry.startsWith("Some poisons"))
        assertEquals(1002, feed.closingName)
        assertTrue(feed.data.url.startsWith("https://ayyappasamaaj.org/"))
        assertEquals(listOf("ta", "ml"), feed.languages.map { it.code })
        assertNull(parseYatraFeed("""{"version": 2}"""))
        assertNull(parseYatraFeed("not json"))
    }

    @Test
    fun fortyOneDaysOfVersesNamesAndStages() {
        assertEquals(155, yatra.totalVerses)
        assertEquals(39, yatra.verseDays)
        assertEquals(40, yatra.reciteDay)
        assertEquals(41, yatra.finalDay)
        assertEquals(1002, yatra.totalNames)
        assertEquals(26, yatra.namesPerDay)
        assertEquals(listOf(1, 2, 3, 4), yatra.versesOfDay(1))
        assertEquals(listOf(153, 154, 155), yatra.versesOfDay(39))
        assertEquals(emptyList<Int>(), yatra.versesOfDay(40))
        assertEquals(25, yatra.namesOfDay(1).size)
        // The closing name comes with the last verse
        assertEquals(listOf(1000, 1001, 1002), yatra.namesOfVerse(155).takeLast(3))
        assertEquals(listOf(104, 109, 125, 116, 118, 102, 117, 89, 74, 48), (0..9).map { yatra.namesOfStage(it) })
        assertEquals("Erumeli", yatra.stageName(1))
        assertEquals("Kaalaketti", yatra.stageName(5))
        assertEquals("Sannidhanam", yatra.stageName(40))
        assertEquals("Pūrṇāhuti", yatra.stageName(41))
    }

    @Test
    fun nameOfTheDayAndItsQuickCheckMatchTheWebsite() {
        assertEquals(1, yatra.nameOfDay(1)!!.n)
        assertEquals(298, yatra.nameOfDay(11)!!.n)
        val one = yatra.quiz(1)!!
        assertEquals(1, one.correct)
        assertEquals(listOf("He who delights the people", "He who is the Son of Lord Shiva",
            "The overlord of the gods and the Devas"), one.options)
        val twelve = yatra.quiz(12)!!
        assertEquals(313, twelve.name.n)
        assertEquals(0, twelve.correct)
        assertEquals(listOf("He who saves us from poison", "He whose mouth blazes like the submarine fire",
            "He who sits majestically like a lion"), twelve.options)
    }

    @Test
    fun aDayIsItsVersesThenTheNameThenTheLamp() {
        val steps = yatra.steps(1)
        assertEquals(listOf(Step.VerseStep(1), Step.VerseStep(2), Step.VerseStep(3), Step.VerseStep(4), Step.NameOfDay, Step.Lamp), steps)
        val partway = Progress(start = start, chanted = setOf(1, 2))
        assertEquals(2, yatra.firstStep(1, partway))
        assertFalse(yatra.dayDone(1, partway))
        val done = Progress(start = start, chanted = setOf(1, 2, 3, 4))
        assertTrue(yatra.dayDone(1, done))
        assertEquals(5, yatra.firstStep(1, done))
    }

    @Test
    fun daysOpenOneAtATimeFromDayOne() {
        val progress = Progress(start = start)
        assertEquals(-6, yatra.todayNumber(progress, start.minusDays(7)))
        assertEquals(1, yatra.currentDay(progress, start.minusDays(7)))
        assertEquals(3, yatra.currentDay(progress, start.plusDays(2)))
        assertTrue(yatra.isOpen(3, progress, start.plusDays(2)))
        assertFalse(yatra.isOpen(4, progress, start.plusDays(2)))
        assertEquals(41, yatra.currentDay(progress, start.plusDays(100)))
        assertEquals(LocalDate.of(2026, 12, 27), yatra.dateOf(41, progress, start))
        // Until Day 1 is chosen, it's today
        assertEquals(1, yatra.currentDay(Progress(), start.plusDays(5)))
    }

    @Test
    fun theLampStreakCountsDaysInARow() {
        val chanted = (1..12).toSet()  // days 1–3
        val progress = Progress(start = start, chanted = chanted)
        assertEquals(3, yatra.streak(progress, start.plusDays(3)))  // day 4 not done yet: up to yesterday
        assertEquals(3, yatra.streak(progress, start.plusDays(2)))
        assertEquals(0, yatra.streak(Progress(start = start, chanted = (1..4).toSet()), start.plusDays(5)))
        assertEquals(12, yatra.versesDone(progress))
    }

    @Test
    fun progressKeepsTheWebsitesShape() {
        val progress = Progress(start = start, chanted = setOf(1, 5), journal = mapOf(2 to "Saranam"), recited = true, intro = true)
        assertEquals(progress, Progress.fromJson(progress.toJson()))
        val web = Progress.fromJson("""{"chanted":{"1":true,"2":false},"journal":{"3":"x"},"recited":false,"intro":true,"start":"2026-11-17"}""")
        assertEquals(setOf(1), web.chanted)
        assertEquals(start, web.start)
        assertEquals(Progress(), Progress.fromJson("broken"))
    }

    @Test
    fun namesAreMarkedInTheirVerses() {
        val verse = data.verses[1]!!
        assertEquals(6, verse.marks.size)
        assertEquals("शिवपुत्रो", verse.text.substring(verse.marks[0].start, verse.marks[0].end))
        assertNotNull(data.names[1]!!.parts.firstOrNull())
        assertNull(data.language)
        assertEquals("ॐ", data.om)
    }

    @Test
    fun theDayBeforeSaysWhatComes() {
        assertEquals("The name of the one who frees you from poison", yatra.teaser(12))
        assertEquals("Verses 9–12 and the names they hold", yatra.teaser(3))
        assertEquals("All 155 verses/1000 names in one sitting", yatra.teaser(40))
        assertEquals("Pūrṇāhuti at Sannidhanam", yatra.teaser(41))
    }
}
