package com.ayyappasamaaj.tattvamasi.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

/*
 * The home page's slides, as on the website: the banners the Samaaj puts up in the website's admin (Pages > Home page
 * slides), from /home/slides/app.json (website: pages/app_slides.py), so they change without an app release. The last
 * copy is kept on the phone, so they show offline; today's slides are chosen here from their first and last days.
 */

/** A slide; [phone] says whether [image] is a 16:9 phone picture (else it's the 3.6:1 wide banner). */
data class HomeSlide(
    val id: Int,
    val alt: String,
    val image: String,
    val phone: Boolean,
    val wide: String,
    val link: String,
    val from: LocalDate?,
    val until: LocalDate?,
) {
    /** Shown on [day]: from its first day through its last (both in the website's time zone, Pacific). */
    fun showingOn(day: LocalDate): Boolean = (from == null || !from.isAfter(day)) && (until == null || !until.isBefore(day))
}

private fun day(value: Any?): LocalDate? =
    (value as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** The website's /home/slides/app.json, or null if it isn't usable (another version, or not JSON). */
fun parseSlides(json: String): List<HomeSlide>? = runCatching {
    val root = JSONObject(json)
    if (root.optInt("version") != 1) return@runCatching null
    val list = root.getJSONArray("slides")
    (0 until list.length()).mapNotNull { list.optJSONObject(it) }.mapNotNull { s ->
        val image = s.optString("image").takeIf { it.startsWith("https://") } ?: return@mapNotNull null
        HomeSlide(
            id = s.optInt("id"),
            alt = s.optString("alt"),
            image = image,
            phone = s.optBoolean("phone"),
            wide = s.optString("wide").takeIf { it.startsWith("https://") } ?: image,
            link = s.optString("link").takeIf { it.startsWith("https://") } ?: "",
            from = day(if (s.isNull("from")) null else s.opt("from")),
            until = day(if (s.isNull("until")) null else s.opt("until")),
        )
    }
}.getOrNull()

/** Today in the website's time zone, which the slides' days are in. */
fun websiteToday(): LocalDate = LocalDate.now(ZoneId.of("America/Los_Angeles"))

/** The slides, kept on the phone; refreshed from the website once per app run. */
object SlidesStore {
    private const val TAG = "SlidesStore"
    private const val URL_JSON = "https://ayyappasamaaj.org/home/slides/app.json"
    private const val FILE = "home-slides.json"

    private val _state = MutableStateFlow<List<HomeSlide>>(emptyList())
    val state: StateFlow<List<HomeSlide>> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        scope.launch {
            val saved = File(app.filesDir, FILE)
            runCatching { saved.readText() }.getOrNull()?.let(::parseSlides)?.let { _state.value = it }
            val text = runCatching { download() }.onFailure { AppLog.w(TAG, "slides: " + it.message) }.getOrNull()
            val fresh = text?.let(::parseSlides)
            if (fresh != null) {
                _state.value = fresh
                runCatching { saved.writeText(text) }.onFailure { AppLog.w(TAG, "save: " + it.message) }
            }
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
}

/**
 * Today's slides, one at a time, swiped or moving on every few seconds; a tap opens where the slide links (the
 * caller opens an event in the app). Nothing shows when there are none. On a phone a 16:9 picture; on a tablet the
 * wide banner, as on the website.
 */
@Composable
fun HomeSlides(slides: List<HomeSlide>, open: (HomeSlide) -> Unit) {
    if (slides.isEmpty()) return
    val pager = rememberPagerState { slides.size }
    LaunchedEffect(slides.size) {
        if (slides.size < 2) return@LaunchedEffect
        while (true) {
            delay(6000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            HorizontalPager(state = pager, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { page ->
                val slide = slides[page]
                val url = if (wide || !slide.phone) slide.wide else slide.image
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (wide || !slide.phone) 3.6f else 16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.claySoft)
                        .clickable(role = Role.Button, onClickLabel = "Open") { open(slide) }
                        .semantics { contentDescription = slide.alt },
                ) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                }
            }
        }
        if (slides.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Slide ${pager.currentPage + 1} of ${slides.size}" },
            ) {
                slides.indices.forEach { i ->
                    Box(Modifier.size(if (i == pager.currentPage) 8.dp else 6.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) AppColors.clay else AppColors.lineStrong))
                }
            }
        }
    }
}
