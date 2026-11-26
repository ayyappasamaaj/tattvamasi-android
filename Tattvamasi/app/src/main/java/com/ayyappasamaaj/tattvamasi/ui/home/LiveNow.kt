package com.ayyappasamaaj.tattvamasi.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The stream on now, as the website publishes it at Firebase `/live` (website pages/live_firebase.py; live streams
 * plan, phase 5). Gone from Firebase when nothing is live.
 */
data class AppLive(
    val title: String,
    val youtubeId: String,      // "" when the stream isn't on YouTube: it opens outside the app
    val url: String,            // the stream's link
    val facebookUrl: String,    // the same stream on Facebook, or ""
    val pageUrl: String,        // the website's /live/
    val startedAt: Long,        // Unix seconds
    val thumbnailUrl: String,
    val eventName: String,
    val eventLink: String,      // its event's page (the events' registrationLink), or ""
) {
    val playsInApp: Boolean get() = youtubeId.isNotEmpty()
}

/** `/live`'s value (a map, or null when nothing is live) as an [AppLive], or null. */
fun parseLive(value: Any?): AppLive? {
    val map = value as? Map<*, *> ?: return null
    fun text(key: String) = (map[key] as? String).orEmpty().trim()
    val title = text("title")
    val url = text("url")
    if (title.isEmpty() || url.isEmpty()) return null
    val youtubeId = text("youtubeId").takeIf { Regex("^[A-Za-z0-9_-]{6,20}$").matches(it) }.orEmpty()
    return AppLive(
        title = title,
        youtubeId = youtubeId,
        url = url,
        facebookUrl = text("facebookUrl"),
        pageUrl = text("pageUrl").ifEmpty { "https://ayyappasamaaj.org/live/" },
        startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L,
        thumbnailUrl = text("thumbnailUrl"),
        eventName = text("eventName"),
        eventLink = text("eventLink"),
    )
}

/** Whether a link is the website's /live/ page (a "We're live now" notification opens it). */
fun isLivePage(url: String): Boolean = Regex("""^https://(www\.)?ayyappasamaaj\.org/live/?(\?.*)?$""").matches(url)

/** The YouTube player for the stream: the privacy-enhanced embed, as on the website's /live/. */
fun livePlayerHtml(youtubeId: String): String = """
    <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
    <style>html,body{margin:0;height:100%;background:#000}iframe{border:0;width:100%;height:100%}</style></head>
    <body><iframe src="https://www.youtube-nocookie.com/embed/$youtubeId?autoplay=1&playsinline=1&rel=0"
    allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe></body></html>
""".trimIndent()

/** Kept up to date from Firebase `/live` while the app runs, so the card comes and goes by itself. */
object LiveStore {
    private const val TAG = "LiveStore"
    private val _live = MutableStateFlow<AppLive?>(null)
    val live: StateFlow<AppLive?> = _live.asStateFlow()
    /** Whether /live has been heard from (or 5 seconds have passed, offline): a tapped "We're live now" waits for it. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private var listening = false
    private const val WAIT_MS = 5_000L

    fun start() {
        if (listening) return
        listening = true
        FirebaseDatabase.getInstance().getReference("live").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _live.value = parseLive(snapshot.value)
                _loaded.value = true
            }

            override fun onCancelled(error: DatabaseError) {
                AppLog.w(TAG, "live: " + error.message)
                _live.value = null
                _loaded.value = true
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({ _loaded.value = true }, WAIT_MS)
    }
}

/** Opens a link in the app that handles it (YouTube or Facebook when installed), else the browser. */
fun openOutside(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        AppLog.w("LiveNow", "no app for $url: ${e.message}")
    }
}

/** Home's "Live now" card: the stream's picture, title and event; Watch plays it. */
@Composable
fun LiveCard(live: AppLive, watch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Watch the live stream", onClick = watch)
            .background(AppColors.forest, RoundedCornerShape(18.dp)),
    ) {
        if (live.thumbnailUrl.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                AsyncImage(model = live.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LiveBadge()
            Text(live.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp), color = AppColors.onForest)
            if (live.eventName.isNotEmpty() && live.eventName != live.title) {
                Text(live.eventName, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = AppColors.onForest.copy(alpha = 0.85f))
            }
            Button(
                onClick = watch,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.saffron, contentColor = AppColors.forest),
            ) {
                Text(if (live.playsInApp) "Watch now" else "Watch on Facebook", fontWeight = FontWeight(700))
            }
        }
    }
}

/** A red "LIVE NOW" pill with a dot. */
@Composable
private fun LiveBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFFC62828), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .semantics { contentDescription = "Live now" },
    ) {
        Box(Modifier.size(8.dp).background(Color.White, CircleShape))
        Text("LIVE NOW", color = Color.White, fontWeight = FontWeight(700),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp))
    }
}

/**
 * The player: the stream full screen over the app, closed with ✕ or Back. "Open in YouTube" for its chat and
 * comments; "Also on Facebook" when the website has the Facebook link.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePlayer(live: AppLive, close: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                IconButton(onClick = close) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(live.title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                // Links inside the player (the YouTube logo, "Watch on YouTube") open outside
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    openOutside(view.context, request.url.toString())
                                    return true
                                }
                            }
                            // The website's address as the page's origin: YouTube's embeds need one
                            loadDataWithBaseURL("https://ayyappasamaaj.org/live/", livePlayerHtml(live.youtubeId),
                                "text/html", "utf-8", null)
                        }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (live.eventName.isNotEmpty() && live.eventName != live.title) {
                    Text(live.eventName, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                OutsideLink("Open in YouTube") { openOutside(context, live.url) }
                if (live.facebookUrl.isNotEmpty()) OutsideLink("Also live on Facebook") { openOutside(context, live.facebookUrl) }
            }
        }
    }
}

@Composable
private fun OutsideLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = AppColors.saffron, fontWeight = FontWeight(600))
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = AppColors.saffron,
            modifier = Modifier.padding(start = 6.dp).size(16.dp))
    }
}

/** Watch: the player for a YouTube stream; a Facebook or other stream opens outside the app. */
fun watchLive(context: Context, live: AppLive, showPlayer: () -> Unit) {
    if (live.playsInApp) showPlayer() else openOutside(context, live.url)
}
