package com.ayyappasamaaj.tattvamasi.ui.publications

import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.Normalizer
import java.time.LocalDate

/*
 * The website's Bhajans and Shlokas & Puja pages, from Firebase `/publications`, which the website publishes on
 * every save (website: bhajans/firebase.py, app_publications). Same sections, categories, order and names as the
 * website. As on the iOS app (PublicationsModel.swift).
 */

data class PublicationVersion(
    val language: String,  // as typed on the website: "Tamil"
    val label: String,     // the language in its own script: "தமிழ்"
    val fileUrl: String,   // the PDF
    val pageUrl: String,   // the website's page for it (for Share)
)

/** One text (a bhajan, shloka...) with a version per language; the first is the one the title opens. */
data class PublicationText(val title: String, val versions: List<PublicationVersion>) {
    val id: String get() = title + "|" + (versions.firstOrNull()?.fileUrl ?: "")
    val languages: List<String> get() = versions.map { it.language }
}

data class PublicationCategory(
    val slug: String,
    val name: String,
    val imageUrl: String,
    val pageUrl: String,
    val texts: List<PublicationText>,
) {
    /** Every language in this category, in the order they first appear (English usually first). */
    val languages: List<String> get() = texts.flatMap { it.languages }.distinct()
}

data class PublicationSection(val key: String, val title: String, val categories: List<PublicationCategory>) {
    /** What the texts are called in a count: "117 bhajans", "12 texts". */
    fun count(n: Int): String = "$n " + when {
        key == "bhajans" -> if (n == 1) "bhajan" else "bhajans"
        else -> if (n == 1) "text" else "texts"
    }
}

/** A search result: a text and the category it's in. */
data class PublicationHit(val category: PublicationCategory, val text: PublicationText)

/** What the reader opens: a text, and the language to start in (null: the text's first version). [categorySlug]
 *  names the text on this phone's Saved list; [playlist]: opened from a playlist, for its previous / next
 *  (Playlists.kt). */
data class ReadingItem(
    val text: PublicationText,
    val categoryName: String,
    val startLanguage: String?,
    val categorySlug: String = "",
    val playlist: PlaylistPlay? = null,
)

object LanguageCode {
    private val buttonCodes = mapOf(
        "english" to "En", "sanskrit" to "Sa", "tamil" to "Ta", "telugu" to "Te", "malayalam" to "Ma", "kannada" to "Ka",
    )

    /** "Tamil" -> "Ta": the reader's round language button and the badges in lists (the maintainer's choice, Nov 21). */
    fun button(language: String): String =
        buttonCodes[language.lowercase()] ?: language.take(2).lowercase().replaceFirstChar { it.uppercase() }
}

/** A text kept on this phone's Saved list: its category's slug and title (together they name it), and when. */
data class SavedText(val slug: String, val title: String, val at: Long)

fun savedKey(slug: String, title: String) = "$slug|$title"

/** Saving a text puts it first; saving it again takes it off. */
fun toggledSaved(saved: List<SavedText>, slug: String, title: String, now: Long): List<SavedText> =
    if (saved.any { it.slug == slug && it.title == title }) saved.filterNot { it.slug == slug && it.title == title }
    else listOf(SavedText(slug, title, now)) + saved

/** The Saved list as stored on the phone: a line per text, "time<TAB>slug<TAB>title". */
fun encodeSaved(saved: List<SavedText>): String = saved.joinToString("\n") { item ->
    listOf(item.at.toString(), item.slug, item.title).joinToString("\t") { it.replace(Regex("[\t\n]"), " ") }
}

fun decodeSaved(text: String?): List<SavedText> = text.orEmpty().split("\n").mapNotNull { line ->
    val parts = line.split("\t")
    if (parts.size != 3) return@mapNotNull null
    val at = parts[0].toLongOrNull() ?: return@mapNotNull null
    SavedText(parts[1], parts[2], at).takeIf { it.slug.isNotEmpty() && it.title.isNotEmpty() }
}

/** The saved texts that are still published, newest first; one the website took away is left out. */
fun resolveSaved(saved: List<SavedText>, sections: List<PublicationSection>): List<PublicationHit> {
    val categories = sections.flatMap { it.categories }.associateBy { it.slug }
    return saved.sortedByDescending { it.at }.mapNotNull { item ->
        val category = categories[item.slug] ?: return@mapNotNull null
        category.texts.firstOrNull { it.title == item.title }?.let { PublicationHit(category, it) }
    }
}

/** Firebase's `/publications` value as sections; anything malformed is left out. */
fun parsePublications(value: Any?): List<PublicationSection> {
    fun list(x: Any?): List<Map<*, *>> = when (x) {
        is List<*> -> x.filterIsInstance<Map<*, *>>()
        is Map<*, *> -> x.values.filterIsInstance<Map<*, *>>()  // Firebase turns a list with gaps into a map
        else -> emptyList()
    }
    fun Map<*, *>.str(key: String) = this[key] as? String ?: ""

    val root = value as? Map<*, *> ?: return emptyList()
    return list(root["sections"]).mapNotNull { section ->
        val categories = list(section["categories"]).mapNotNull { category ->
            val texts = list(category["texts"]).mapNotNull { text ->
                val versions = list(text["versions"]).mapNotNull { v ->
                    PublicationVersion(v.str("language"), v.str("label").ifEmpty { v.str("language") },
                        v.str("fileUrl"), v.str("pageUrl")).takeIf { it.fileUrl.startsWith("http") }
                }
                PublicationText(text.str("title"), versions).takeIf { it.title.isNotEmpty() && versions.isNotEmpty() }
            }
            PublicationCategory(category.str("slug"), category.str("name"), category.str("imageUrl"),
                category.str("pageUrl"), texts).takeIf { it.slug.isNotEmpty() }
        }
        PublicationSection(section.str("key"), section.str("title"), categories).takeIf { it.key.isNotEmpty() }
    }
}

/** Lower case without accents, so "ganapati" finds "Gaṇapati". */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Whether every word typed is in the title (case and accents ignored). */
fun titleMatches(title: String, query: String): Boolean {
    val words = foldForSearch(query).split(" ").filter { it.isNotBlank() }
    val folded = foldForSearch(title)
    return words.all { folded.contains(it) }
}

/** Texts whose title contains the words typed, in every category. */
fun searchPublications(sections: List<PublicationSection>, query: String): List<PublicationHit> {
    if (query.isBlank()) return emptyList()
    return sections.flatMap { section ->
        section.categories.flatMap { category ->
            category.texts.filter { titleMatches(it.title, query) }.map { PublicationHit(category, it) }
        }
    }
}

/** The reader's language for every text (the website's "Reading in"): its version in [preferred] if it has one, else
 *  English, else its first. Null when that's the first anyway. */
fun openingLanguage(text: PublicationText, preferred: String): String? {
    val pick = text.versions.firstOrNull { it.language.equals(preferred, ignoreCase = true) }
        ?: text.versions.firstOrNull { it.language.equals(ENGLISH, ignoreCase = true) }
        ?: text.versions.firstOrNull()
    return pick?.language
}

const val ENGLISH = "English"

/** The languages to read in, English first, then by how many texts have them: (language, its own name). */
fun readingLanguages(categories: List<PublicationCategory>): List<Pair<String, String>> {
    val versions = categories.flatMap { it.texts }.flatMap { it.versions }
    val counts = versions.groupingBy { it.language }.eachCount()
    val labels = versions.associate { it.language to it.label }
    return counts.keys.sortedWith(compareBy<String>({ !it.equals(ENGLISH, ignoreCase = true) }, { -counts.getValue(it) }, { it }))
        .map { it to (labels[it] ?: it) }
}

/** The count line under a category's name, as on the website: "118 bhajans in 4 languages". */
fun countLine(section: PublicationSection, category: PublicationCategory): String {
    val languages = category.languages
    return section.count(category.texts.size) + when (languages.size) {
        0 -> ""
        1 -> " in " + languages[0]
        else -> " in ${languages.size} languages"
    }
}

/** The title without a leading number: "06 Sabari Yatra Mala Wearing Mantra" -> "Sabari Yatra Mala Wearing Mantra". */
fun plainTitle(title: String): String = title.replace(Regex("^\\d+\\.?\\s+"), "").ifEmpty { title }

private fun titleKey(title: String) = plainTitle(title).split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ").lowercase()

/** "For Mandalam Season": the texts devotees look for most in the season, in this order, as on the website
 *  (bhajans/views.py, MANDALAM_SHORTLIST); a title that isn't found is left out. */
val MANDALAM_SHORTLIST = listOf(
    "Sarana Ghosham",
    "Periya Padhai Sarana Ghosham",
    "Sabari Yatra Mala Wearing Mantra",
    "Sri Hariharatmaja Ashtakam (Harivarasanam)",
    "Hariharaputra Puja Vidhi",
    "Ayyappa Sahasranamam",
)

/** The website shows the shortlist from October 1 to January 15 (its season guides' window). */
fun inMandalamSeason(day: LocalDate): Boolean =
    day.monthValue >= 10 || (day.monthValue == 1 && day.dayOfMonth <= 15)

fun mandalamShortlist(sections: List<PublicationSection>): List<PublicationHit> {
    val found = mutableMapOf<String, PublicationHit>()
    sections.forEach { section ->
        section.categories.forEach { category ->
            category.texts.forEach { text -> found.putIfAbsent(titleKey(text.title), PublicationHit(category, text)) }
        }
    }
    return MANDALAM_SHORTLIST.mapNotNull { found[titleKey(it)] }
}

/** Recent searches: the newest first, a repeat moved to the top (case ignored), five at most. */
fun withRecent(recents: List<String>, query: String): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return recents
    return (listOf(q) + recents.filterNot { it.equals(q, ignoreCase = true) }).take(5)
}

/** Every category in order (Bhajans, then Shlokas & Puja), for previous / next. */
fun allCategories(sections: List<PublicationSection>): List<Pair<PublicationSection, PublicationCategory>> =
    sections.flatMap { s -> s.categories.map { s to it } }

data class PublicationsState(
    val sections: List<PublicationSection> = emptyList(),
    val loaded: Boolean = false,
)

/** `/publications`, kept up to date while the app runs: a change on the website shows at once. */
object PublicationsStore {
    private val _state = MutableStateFlow(PublicationsState())
    val state: StateFlow<PublicationsState> = _state.asStateFlow()
    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        FirebaseDatabase.getInstance().getReference("publications").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _state.value = PublicationsState(parsePublications(snapshot.value), loaded = true)
            }

            override fun onCancelled(error: DatabaseError) {
                AppLog.w("PublicationsStore", "publications: " + error.message)
                _state.value = _state.value.copy(loaded = true)
            }
        })
    }
}
