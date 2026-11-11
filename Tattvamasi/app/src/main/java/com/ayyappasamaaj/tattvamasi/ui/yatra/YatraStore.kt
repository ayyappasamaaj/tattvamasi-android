package com.ayyappasamaaj.tattvamasi.ui.yatra

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import com.ayyappasamaaj.tattvamasi.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** What the Yatra screens show: the yatra (null until it's loaded), the script chosen, and the reader's progress. */
data class YatraState(
    val yatra: Yatra? = null,
    val script: String = "",            // "" is Devanagari, else a language's code ("ta")
    val loading: Boolean = true,
    val failed: Boolean = false,
    val message: String? = null,        // a passing note ("Tamil needs a connection the first time")
    val progress: Progress = Progress(),
)

/**
 * The yatra, kept for offline use: the website's settings and the stotram's data are saved on the phone (the app
 * ships with a copy of both in Devanagari, assets/yatra.json and assets/yatra-data.json, for a first start without a
 * connection). Once per app run it checks the website for newer settings, and fetches the data again only when its
 * version changes. A language's script is fetched the first time it's chosen, then kept.
 *
 * The reader's progress (Day 1, verses chanted, reflections) stays on the phone, in the same shape the website keeps
 * in the browser; nothing is sent anywhere.
 */
object YatraStore {
    private const val TAG = "YatraStore"
    private const val URL_JSON = "https://ayyappasamaaj.org/interactive/sahasranama-yatra/app.json"
    private const val FEED_FILE = "yatra.json"
    private const val DATA_ASSET = "yatra-data.json"
    private const val PREFS = "yatra"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_SCRIPT = "script"

    private val _state = MutableStateFlow(YatraState())
    val state: StateFlow<YatraState> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var started = false
    // What's needed from the app (not the Context itself, which an object mustn't hold)
    private lateinit var filesDir: File
    private lateinit var assets: AssetManager
    private lateinit var prefs: SharedPreferences
    private var feed: YatraFeed? = null

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        filesDir = app.filesDir
        assets = app.assets
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.update { it.copy(progress = Progress.fromJson(prefs.getString(KEY_PROGRESS, null))) }
        scope.launch {
            lock.withLock {
                val savedFeed = File(filesDir, FEED_FILE)
                feed = runCatching { savedFeed.readText() }.getOrNull()?.let(::parseYatraFeed)
                    ?: runCatching { assets.open(FEED_FILE).reader().readText() }.getOrNull()?.let(::parseYatraFeed)
                val wanted = prefs.getString(KEY_SCRIPT, "") ?: ""
                show(wanted, download = false)

                val text = runCatching { download(URL_JSON) }.onFailure { AppLog.w(TAG, "feed: " + it.message) }.getOrNull()
                val fresh = text?.let(::parseYatraFeed)
                if (fresh != null) {
                    feed = fresh
                    runCatching { savedFeed.writeText(text) }.onFailure { AppLog.w(TAG, "save: " + it.message) }
                }
                show(wanted, download = true)
            }
        }
    }

    /** Shows the verses and names in another script ("" for Devanagari). */
    fun chooseScript(code: String) {
        if (!started) return
        scope.launch {
            lock.withLock {
                prefs.edit().putString(KEY_SCRIPT, code).apply()
                show(code, download = true)
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun update(change: (Progress) -> Progress) {
        val next = change(_state.value.progress)
        _state.update { it.copy(progress = next) }
        if (started) prefs.edit().putString(KEY_PROGRESS, next.toJson()).apply()
    }

    /** Loads the data in [code]'s script, from the phone if it has this version, else (if allowed) the website. */
    private fun show(code: String, download: Boolean) {
        val feed = feed ?: run {
            _state.update { it.copy(loading = false, failed = true) }
            return
        }
        val language = feed.languages.firstOrNull { it.code == code }
        val script = if (language != null) code else ""
        val link = if (language != null) DataLink(language.url, feed.data.version) else feed.data
        val current = _state.value
        if (current.yatra != null && current.script == script && current.yatra.feed == feed &&
            savedVersion(script) == link.version) return

        var data = loadSaved(script, link.version)
        if (data == null && download) data = fetch(script, link)
        if (data == null) data = loadSaved(script, null)  // an older version beats none
        if (data == null && script == "") data = loadAsset()
        if (data != null) {
            _state.update { it.copy(yatra = Yatra(feed, data), script = script, loading = false, failed = false) }
            return
        }
        // The script isn't on the phone and couldn't be fetched: stay with what's shown, or Devanagari
        val shown = _state.value
        val (fallback, fallbackScript) = if (shown.yatra != null) shown.yatra.data to shown.script
            else (loadSaved("", null) ?: loadAsset()) to ""
        _state.update {
            it.copy(
                yatra = fallback?.let { d -> Yatra(feed, d) },
                script = fallbackScript,
                loading = false,
                failed = fallback == null,
                message = if (download && language != null) "${language.english} needs a connection the first time. " +
                    "Please try again when you're online." else it.message,
            )
        }
    }

    private fun dataFile(script: String) = File(filesDir, "yatra-data-${script.ifEmpty { "sa" }}.json")
    private fun versionFile(script: String) = File(filesDir, "yatra-data-${script.ifEmpty { "sa" }}.version")

    private fun savedVersion(script: String): String? = runCatching { versionFile(script).readText() }.getOrNull()

    private fun loadSaved(script: String, version: String?): YatraData? {
        if (version != null && savedVersion(script) != version) return null
        return runCatching { dataFile(script).readText() }.getOrNull()?.let(::parseYatraData)
    }

    private fun loadAsset(): YatraData? =
        runCatching { assets.open(DATA_ASSET).reader().readText() }.getOrNull()?.let(::parseYatraData)

    private fun fetch(script: String, link: DataLink): YatraData? {
        val text = runCatching { download(link.url) }.onFailure { AppLog.w(TAG, "data: " + it.message) }.getOrNull()
            ?: return null
        val data = parseYatraData(text) ?: return null
        runCatching {
            dataFile(script).writeText(text)
            versionFile(script).writeText(link.version)
        }.onFailure { AppLog.w(TAG, "save data: " + it.message) }
        return data
    }

    private fun download(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode == 200) connection.inputStream.reader().readText() else null
        } finally {
            connection.disconnect()
        }
    }
}
