package com.ayyappasamaaj.tattvamasi.ui.guides

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.ayyappasamaaj.tattvamasi.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The pilgrimage guides, kept for offline reading (on the pilgrimage there's often no signal): the last copy
 * from the website is saved on the phone, and the app ships with a copy (assets/guides.json) for a first start
 * without a connection. Once per app run it checks the website for a newer copy, and keeps every guide's
 * pictures (about 3 MB) in the image cache so they show offline too.
 */
object GuidesStore {
    private const val TAG = "GuidesStore"
    private const val URL_JSON = "https://ayyappasamaaj.org/guides/app.json"
    private const val FILE = "guides.json"

    private val _state = MutableStateFlow<List<GuideContent>>(emptyList())
    val state: StateFlow<List<GuideContent>> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        scope.launch {
            val saved = File(app.filesDir, FILE)
            val local = runCatching { saved.readText() }.getOrNull()?.let(::parseGuides)
                ?: runCatching { app.assets.open(FILE).reader().readText() }.getOrNull()?.let(::parseGuides)
            if (local != null) _state.value = local

            val text = runCatching { download() }.onFailure { AppLog.w(TAG, "guides: " + it.message) }.getOrNull()
            val fresh = text?.let(::parseGuides)
            if (fresh != null) {
                _state.value = fresh
                runCatching { saved.writeText(text) }.onFailure { AppLog.w(TAG, "save: " + it.message) }
            }
            keepPictures(app, _state.value)
        }
    }

    private fun download(): String? {
        val connection = URL(URL_JSON).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        return try {
            if (connection.responseCode == 200) connection.inputStream.reader().readText() else null
        } finally {
            connection.disconnect()
        }
    }

    /** Fetches every picture into the image cache (the disk cache only: nothing is shown yet). */
    private fun keepPictures(context: Context, guides: List<GuideContent>) {
        val loader = SingletonImageLoader.get(context)
        guides.flatMap { it.pictures }.distinct().forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context).data(url).memoryCachePolicy(CachePolicy.DISABLED).build(),
            )
        }
    }
}
