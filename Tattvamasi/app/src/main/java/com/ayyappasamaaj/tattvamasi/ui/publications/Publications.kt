package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import android.content.res.AssetManager
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

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

/** What the reader opens: a text, and the language to start in (null: the text's first version). */
data class ReadingItem(val text: PublicationText, val categoryName: String, val startLanguage: String?)

object LanguageCode {
    private val codes = mapOf(
        "english" to "EN", "tamil" to "TA", "malayalam" to "ML", "sanskrit" to "SA", "telugu" to "TE",
        "kannada" to "KN", "hindi" to "HI", "marathi" to "MR", "gujarati" to "GU", "bengali" to "BN",
    )

    /** "Tamil" -> "TA", for the small badges in lists. */
    fun short(language: String): String = codes[language.lowercase()] ?: language.take(2).uppercase()
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

data class PublicationsState(
    val sections: List<PublicationSection> = emptyList(),
    val loaded: Boolean = false,
)

/** `/publications`, kept up to date while the app runs: a change on the website shows at once. */
object PublicationsStore {
    private val _state = MutableStateFlow(PublicationsState())
    val state: StateFlow<PublicationsState> = _state.asStateFlow()
    private var listening = false

    fun start(context: Context) {
        if (listening) return
        listening = true
        val assets = context.applicationContext.assets
        FirebaseDatabase.getInstance().getReference("publications").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                show(parsePublications(snapshot.value), assets)
            }

            override fun onCancelled(error: DatabaseError) {
                AppLog.w("PublicationsStore", "publications: " + error.message)
                show(emptyList(), assets)
            }
        })
    }

    /**
     * Debug builds only: until the app can read `/publications` (the Firebase rules must allow it), show the copy
     * of the website's data in src/debug/assets, as the iOS app's Debug builds do. Release builds have no such
     * file, so they show the "couldn't be loaded" message instead.
     */
    private fun show(sections: List<PublicationSection>, assets: AssetManager) {
        val shown = sections.ifEmpty {
            if (!BuildConfig.DEBUG) emptyList()
            else runCatching {
                parsePublications(plainJson(JSONObject(assets.open("publications-sample.json").reader().readText())))
            }.getOrDefault(emptyList())
        }
        _state.value = PublicationsState(shown, loaded = true)
    }

    private fun plainJson(x: Any?): Any? = when (x) {
        is JSONObject -> x.keys().asSequence().associateWith { plainJson(x.get(it)) }
        is JSONArray -> (0 until x.length()).map { plainJson(x.get(it)) }
        JSONObject.NULL -> null
        else -> x
    }
}
