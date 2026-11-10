package com.ayyappasamaaj.tattvamasi.ui.yatra

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/*
 * The Sahasranama Yatra, as on the website (ayyappasamaaj.org/interactive/sahasranama-yatra/, its script
 * static/js/src/pages/sahasranamaYatra.js): the Sri Dharma Shastha Sahasranamam chanted four verses a day through the
 * 41 days of Vratam. 155 verses make 39 verse days; Day 40 is the full recitation and Day 41 the Pūrṇāhuti. The days
 * follow the 10 stages of the trek, 4 days each. A day is walked verse by verse, then the Name of the day, then the
 * lamp.
 *
 * Two pieces come from the website: its settings (/interactive/sahasranama-yatra/app.json, website:
 * pages/yatra_app.py) and the stotram's data, in Devanagari or a live language's script (website: stotrams/payload.py).
 */

/** The website's settings for the yatra. */
data class YatraFeed(
    val url: String,
    val mandalaPujaDate: String,
    val credit: String,
    val versesPerDay: Int,
    val stages: List<String>,
    val nameOfDay: Map<Int, Int>,
    val reflections: Map<Int, Reflection>,
    val teasers: Map<Int, String>,
    val closingName: Int,
    val data: DataLink,
    val languages: List<YatraLanguage>,
    val comingLanguages: List<ComingLanguage> = emptyList(),
)

data class Reflection(val about: String, val carry: String)

/** Where a version of the stotram's data is. */
data class DataLink(val url: String, val version: String)

/** A language still being reviewed: listed greyed out, "(Coming Soon)", and not chosen. */
data class ComingLanguage(val code: String, val name: String, val english: String)

/** A script the verses and names can be shown in besides Devanagari ("ta": Tamil). */
data class YatraLanguage(val code: String, val name: String, val english: String, val url: String)

/** Where a name sits in its verse: name [n] is the text from [start] to [end] (UTF-16 offsets, as in Kotlin). */
data class Mark(val n: Int, val start: Int, val end: Int)

data class Verse(val number: Int, val first: Int, val last: Int, val text: String, val tr: String, val marks: List<Mark>)

/** A root word of a name; one without a [meaning] is another reading ("also: …"). */
data class Part(val word: String, val tr: String, val meaning: String)

/**
 * One of the 1,002 names: [n] its number, [d] in the script, [t] transliterated, [m] its meaning. [meaningReviewed]
 * is false while the meaning in the reader's language isn't reviewed yet (the English one is shown); [altReading] is
 * a reading the root words explain that differs from the name.
 */
data class Name(
    val n: Int,
    val d: String,
    val t: String,
    val parts: List<Part>,
    val m: String,
    val meaningReviewed: Boolean = true,
    val altReading: String = "",
)

/** The script the data is in: null is Devanagari. */
data class DataLanguage(val code: String, val english: String, val om: String, val namah: String)

data class YatraData(
    val nyasa: String,
    val dhyanam: String,
    val dhyanamMeaning: String,
    val verses: Map<Int, Verse>,
    val names: Map<Int, Name>,
    val language: DataLanguage?,
) {
    val om: String get() = language?.om ?: "ॐ"
    val namah: String get() = language?.namah ?: "नमः"
}

private fun JSONObject.intMap(key: String): Map<Int, Int> {
    val obj = optJSONObject(key) ?: return emptyMap()
    return obj.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to obj.optInt(k) } }
        .filter { it.second > 0 }.toMap()
}

private fun JSONObject.textMap(key: String): Map<Int, String> {
    val obj = optJSONObject(key) ?: return emptyMap()
    return obj.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to obj.optString(k) } }
        .filter { it.second.isNotBlank() }.toMap()
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

/** The website's /interactive/sahasranama-yatra/app.json, or null if it isn't usable. */
fun parseYatraFeed(text: String): YatraFeed? = runCatching {
    val root = JSONObject(text)
    if (root.optInt("version") != 1) return null
    val data = root.optJSONObject("data") ?: return null
    val dataUrl = data.optString("url")
    if (!dataUrl.startsWith("https://") && !dataUrl.startsWith("http://")) return null
    val stages = root.optJSONArray("stages").strings()
    if (stages.isEmpty()) return null
    val reflections = root.optJSONObject("reflections")?.let { obj ->
        obj.keys().asSequence().mapNotNull { k ->
            val day = k.toIntOrNull() ?: return@mapNotNull null
            val r = obj.optJSONObject(k) ?: return@mapNotNull null
            day to Reflection(r.optString("about"), r.optString("carry"))
        }.toMap()
    } ?: emptyMap()
    YatraFeed(
        url = root.optString("url", "https://ayyappasamaaj.org/interactive/sahasranama-yatra/"),
        mandalaPujaDate = root.optString("mandala_puja_date"),
        credit = root.optString("credit"),
        versesPerDay = root.optInt("verses_per_day", 4).coerceAtLeast(1),
        stages = stages,
        nameOfDay = root.intMap("name_of_day"),
        reflections = reflections,
        teasers = root.textMap("teasers"),
        closingName = root.optInt("closing_name", 1002),
        data = DataLink(dataUrl, data.optString("version")),
        languages = root.optJSONArray("languages").objects().mapNotNull { l ->
            val code = l.optString("code")
            val url = l.optString("url")
            if (code.isBlank() || !url.startsWith("http")) null
            else YatraLanguage(code, l.optString("name"), l.optString("english"), url)
        },
        comingLanguages = root.optJSONArray("languages_coming").objects().mapNotNull { l ->
            val english = l.optString("english")
            if (english.isBlank()) null else ComingLanguage(l.optString("code"), l.optString("name", english), english)
        },
    )
}.getOrNull()

/** The stotram's data (website: stotrams/payload.py), or null if it isn't usable. */
fun parseYatraData(text: String): YatraData? = runCatching {
    val root = JSONObject(text)
    val versesObj = root.optJSONObject("verses") ?: return null
    val verses = versesObj.keys().asSequence().mapNotNull { k ->
        val number = k.toIntOrNull() ?: return@mapNotNull null
        val v = versesObj.optJSONObject(k) ?: return@mapNotNull null
        val range = v.optJSONArray("names")
        val text = v.optString("text")
        val marks = v.optJSONArray("marks")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val m = arr.optJSONArray(i) ?: return@mapNotNull null
                val mark = Mark(m.optInt(0), m.optInt(1), m.optInt(2))
                if (mark.start in 0 until mark.end && mark.end <= text.length) mark else null
            }.sortedBy { it.start }
        } ?: emptyList()
        number to Verse(number, range?.optInt(0) ?: 0, range?.optInt(1) ?: -1, text, v.optString("tr"), marks)
    }.toMap()
    val names = root.optJSONArray("names").objects().map { e ->
        Name(
            n = e.optInt("n"),
            d = e.optString("d"),
            t = e.optString("t"),
            parts = e.optJSONArray("p").let { p ->
                if (p == null) emptyList() else (0 until p.length()).mapNotNull { i ->
                    val r = p.optJSONArray(i) ?: return@mapNotNull null
                    Part(r.optString(0), r.optString(1), if (r.isNull(2)) "" else r.optString(2))
                }
            },
            m = e.optString("m"),
            meaningReviewed = e.optBoolean("mok", true),
            altReading = e.optString("a"),
        )
    }.filter { it.n > 0 && it.d.isNotBlank() }.associateBy { it.n }
    if (verses.isEmpty() || names.isEmpty()) return null
    val dhyanam = root.optJSONObject("dhyanam")
    val lang = root.optJSONObject("lang")?.let {
        DataLanguage(it.optString("code"), it.optString("english"), it.optString("om", "ॐ"), it.optString("namah", "नमः"))
    }
    YatraData(root.optString("nyasa"), dhyanam?.optString("text") ?: "", dhyanam?.optString("meaning") ?: "",
        verses, names, lang)
}.getOrNull()

/** A step of a verse day: a verse, the Name of the day, then the lamp. */
sealed interface Step {
    data class VerseStep(val verse: Int) : Step
    data object NameOfDay : Step
    data object Lamp : Step
}

/** What's kept on the phone: the chosen Day 1, the verses chanted, reflections. Never sent anywhere. */
data class Progress(
    val start: LocalDate? = null,
    val chanted: Set<Int> = emptySet(),
    val journal: Map<Int, String> = emptyMap(),
    val recited: Boolean = false,
    val intro: Boolean = false,
) {
    fun toJson(): String = JSONObject().apply {
        put("start", start?.toString() ?: "")
        put("chanted", JSONObject().apply { chanted.sorted().forEach { put(it.toString(), true) } })
        put("journal", JSONObject().apply { journal.forEach { (d, t) -> put(d.toString(), t) } })
        put("recited", recited)
        put("intro", intro)
    }.toString()

    companion object {
        /** The same shape the website keeps in the browser ({chanted, journal, recited, intro, start}). */
        fun fromJson(text: String?): Progress {
            if (text.isNullOrBlank()) return Progress()
            return runCatching {
                val o = JSONObject(text)
                val chanted = o.optJSONObject("chanted")?.let { c ->
                    c.keys().asSequence().filter { c.optBoolean(it) }.mapNotNull { it.toIntOrNull() }.toSet()
                } ?: emptySet()
                val journal = o.optJSONObject("journal")?.let { j ->
                    j.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to j.optString(k) } }.toMap()
                } ?: emptyMap()
                Progress(
                    start = o.optString("start").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    chanted = chanted,
                    journal = journal,
                    recited = o.optBoolean("recited"),
                    intro = o.optBoolean("intro"),
                )
            }.getOrDefault(Progress())
        }
    }
}

/** The 41 days worked out from the settings and the data, as the website's script does. */
class Yatra(val feed: YatraFeed, val data: YatraData) {
    val totalVerses: Int = data.verses.keys.maxOrNull() ?: 0
    val verseDays: Int = (totalVerses + feed.versesPerDay - 1) / feed.versesPerDay
    val reciteDay: Int = verseDays + 1
    val finalDay: Int = verseDays + 2
    val stages: List<String> = feed.stages

    fun versesOfDay(day: Int): List<Int> {
        if (day < 1 || day > verseDays) return emptyList()
        val first = (day - 1) * feed.versesPerDay + 1
        return (first..minOf(totalVerses, first + feed.versesPerDay - 1)).toList()
    }

    fun verseRange(day: Int): String = versesOfDay(day).let { if (it.isEmpty()) "" else "${it.first()}–${it.last()}" }

    fun stageIndex(day: Int): Int = minOf(stages.size - 1, (minOf(day, reciteDay) - 1) / 4).coerceAtLeast(0)

    fun stageName(day: Int): String = if (day == finalDay) "Pūrṇāhuti" else stages[stageIndex(day)]

    /** The days of a stage: 4 each (the last stage's 4th is the full recitation). */
    fun daysOfStage(index: Int): IntRange = (index * 4 + 1)..(index * 4 + 4)

    private val namesOfVerseCache = HashMap<Int, List<Int>>()

    /** The names a verse holds, in order; the closing name comes with the last verse. */
    fun namesOfVerse(verse: Int): List<Int> = namesOfVerseCache.getOrPut(verse) {
        val v = data.verses[verse] ?: return@getOrPut emptyList()
        val out = (v.first..v.last).filter { it in data.names }.toMutableList()
        if (verse == totalVerses && feed.closingName in data.names) out += feed.closingName
        out
    }

    fun namesOfDay(day: Int): List<Int> = versesOfDay(day).flatMap { namesOfVerse(it) }

    fun verseOfName(n: Int): Int? = (1..totalVerses).firstOrNull { n in namesOfVerse(it) }

    fun nameOfDay(day: Int): Name? = (feed.nameOfDay[day] ?: namesOfDay(day).firstOrNull())?.let { data.names[it] }

    val totalNames: Int = (1..totalVerses).sumOf { namesOfVerse(it).size }
    val namesPerDay: Int = if (verseDays > 0) (totalNames.toDouble() / verseDays).roundToInt() else 0

    fun namesOfStage(index: Int): Int = daysOfStage(index).filter { it <= verseDays }.sumOf { namesOfDay(it).size }

    fun steps(day: Int): List<Step> {
        val out = mutableListOf<Step>()
        versesOfDay(day).forEach { out += Step.VerseStep(it) }
        if (nameOfDay(day) != null) out += Step.NameOfDay
        out += Step.Lamp
        return out
    }

    /** Where a day opens: its first verse not yet chanted, or the lamp once all are. */
    fun firstStep(day: Int, progress: Progress): Int {
        val verses = versesOfDay(day)
        val i = verses.indexOfFirst { it !in progress.chanted }
        return if (i >= 0) i else if (verses.isEmpty()) 0 else steps(day).size - 1
    }

    fun dayDone(day: Int, progress: Progress): Boolean {
        if (day == reciteDay) return progress.recited
        val verses = versesOfDay(day)
        return verses.isNotEmpty() && verses.all { it in progress.chanted }
    }

    /** Day 1 is the chosen date (today until one is chosen). */
    fun startDate(progress: Progress, today: LocalDate): LocalDate = progress.start ?: today

    /** Today's day number (below 1 before the yatra begins, above 41 after it ends). */
    fun todayNumber(progress: Progress, today: LocalDate): Int =
        ChronoUnit.DAYS.between(startDate(progress, today), today).toInt() + 1

    fun currentDay(progress: Progress, today: LocalDate): Int = todayNumber(progress, today).coerceIn(1, finalDay)

    fun isOpen(day: Int, progress: Progress, today: LocalDate): Boolean = day <= currentDay(progress, today)

    fun dateOf(day: Int, progress: Progress, today: LocalDate): LocalDate =
        startDate(progress, today).plusDays((day - 1).toLong())

    /** Days in a row with every verse chanted, up to today (or yesterday, if today isn't done yet). */
    fun streak(progress: Progress, today: LocalDate): Int {
        var d = minOf(currentDay(progress, today), verseDays)
        if (!dayDone(d, progress)) d--
        var s = 0
        while (d >= 1 && dayDone(d, progress)) { s++; d-- }
        return s
    }

    fun versesDone(progress: Progress): Int = (1..totalVerses).count { it in progress.chanted }

    fun namesDone(progress: Progress): Int = (1..totalVerses).filter { it in progress.chanted }.sumOf { namesOfVerse(it).size }

    /** The line about a day, shown the day before. */
    fun teaser(day: Int): String = feed.teasers[day] ?: when {
        day <= verseDays -> "Verses ${verseRange(day)} and the names they hold"
        day == reciteDay -> "All $totalVerses verses/1000 names in one sitting"
        else -> "Pūrṇāhuti at Sannidhanam"
    }

    /**
     * The lamp's quick check: three meanings for the Name of the day, the right one at [Quiz.correct]. Picked with
     * the website's own little random sequence from the day number, so the page and the apps ask the same.
     */
    fun quiz(day: Int): Quiz? {
        val name = nameOfDay(day) ?: return null
        val pool = namesOfDay(day).filter { it != name.n }.mapNotNull { data.names[it]?.m }
            .filter { it.isNotBlank() && it != name.m }.distinct()
        if (pool.size < 2) return null
        var seed = day.toLong() * 7919
        fun pick(n: Int): Int {
            seed = (seed * 9301 + 49297) % 233280
            return (seed.toDouble() / 233280 * n).toInt()
        }
        val wrong = mutableListOf<String>()
        while (wrong.size < 2) {
            val m = pool[pick(pool.size)]
            if (m !in wrong) wrong += m
        }
        val pos = pick(3)
        val options = wrong.toMutableList().apply { add(pos, name.m) }
        return Quiz(name, options, pos)
    }
}

data class Quiz(val name: Name, val options: List<String>, val correct: Int)
