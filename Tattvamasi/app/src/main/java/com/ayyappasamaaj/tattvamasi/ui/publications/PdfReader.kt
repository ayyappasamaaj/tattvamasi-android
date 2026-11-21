package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reads one text: the PDF slides up over the Publications page (the banner too), above the player bar and the
 * navigation bar, which stay. No title bar (the PDF shows its title): a grab handle, the PDF's pages one under another
 * (pinch to zoom), and round buttons at the bottom right: Save (on this phone), Share and the language. Dragging it
 * down (from the handle, or the PDF when it's at its top) slides it away; so does Back. The maintainer's design,
 * Nov 21 (https://claude.ai/artifact/8gcAHq92Jxy8nzg86EZdGC, "Latest"). As on the iOS app (PDFReaderView.swift).
 */
@Composable
fun PdfPopup(item: ReadingItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versions = item.text.versions
    var language by rememberSaveable(item.text.id) {
        mutableStateOf((versions.firstOrNull { it.language == item.startLanguage } ?: versions.first()).language)
    }
    val version = versions.firstOrNull { it.language == language } ?: versions.first()
    LaunchedEffect(Unit) { SavedTexts.load(context) }
    val savedList by SavedTexts.saved.collectAsState()
    val saved = savedList.any { it.slug == item.categorySlug && it.title == item.text.title }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val threshold = with(LocalDensity.current) { 140.dp.toPx() }
        // How far down the panel is: the full height while it slides in, 0 when open
        var down by remember(item.text.id) { mutableFloatStateOf(height) }
        var closing by remember(item.text.id) { mutableStateOf(false) }
        LaunchedEffect(item.text.id) { animate(down, 0f, animationSpec = tween(260)) { v, _ -> down = v } }

        fun close() {
            if (closing) return
            closing = true
            scope.launch {
                animate(down, height, animationSpec = tween(220)) { v, _ -> down = v }
                onClose()
            }
        }
        // Let go: far enough (or flung down) closes, otherwise it springs back
        suspend fun settle(velocity: Float) {
            if (down > threshold || velocity > 1500f) close()
            else animate(down, 0f, initialVelocity = velocity, animationSpec = tween(180)) { v, _ -> down = v }
        }
        BackHandler { close() }

        // Pulling the PDF down when it's at its top moves the panel; pushing up brings it back first
        val pull = remember(item.text.id) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f && down > 0f) {
                        val used = max(available.y, -down)
                        down += used
                        return Offset(0f, used)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput && !closing) {
                        down += available.y
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (down > 0f && !closing) {
                        settle(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f * (1f - down / height))))
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, down.roundToInt()) }
                .background(AppColors.cream),
        ) {
            Column(Modifier.fillMaxSize()) {
                // The grab handle (under the status bar): drag it down to close; TalkBack reads it as Close
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(24.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta -> if (!closing) down = (down + delta).coerceAtLeast(0f) },
                            onDragStopped = { velocity -> settle(velocity) },
                        )
                        .clickable(role = Role.Button, onClickLabel = "Close") { close() }
                        .semantics { contentDescription = "Close" },
                ) {
                    Box(Modifier.size(width = 40.dp, height = 5.dp).background(AppColors.lineStrong, RoundedCornerShape(3.dp)))
                }
                Box(Modifier.weight(1f).fillMaxWidth().nestedScroll(pull)) {
                    PdfPages(version.fileUrl)
                }
            }
            ReaderButtons(
                languages = versions.map { it.language },
                language = language,
                onLanguage = { language = it },
                saved = saved,
                onSave = {
                    val now = SavedTexts.toggle(context, item.categorySlug, item.text.title)
                    Toast.makeText(context, if (now) "Saved on this phone" else "Removed from Saved", Toast.LENGTH_SHORT).show()
                },
                onShare = { share(context, item.text.title, version.pageUrl) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

/** The round buttons at the bottom right, from the top: Save, Share, and the language (its other languages open to
 *  its left; none when the text has one language). */
@Composable
private fun ReaderButtons(
    languages: List<String>,
    language: String,
    onLanguage: (String) -> Unit,
    saved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundButton(48.dp, if (saved) "Saved on this phone, tap to remove" else "Save on this phone",
            Modifier.padding(end = 5.dp), onClick = { open = false; onSave() }) {
            Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null,
                tint = if (saved) AppColors.saffron else AppColors.ink)
        }
        RoundButton(48.dp, "Share", Modifier.padding(end = 5.dp), onClick = { open = false; onShare() }) {
            Icon(Icons.Outlined.Share, contentDescription = null, tint = AppColors.ink)
        }
        if (languages.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (open) {
                    languages.filter { it != language }.forEach { other ->
                        RoundButton(44.dp, other, onClick = { open = false; onLanguage(other) }) {
                            Text(LanguageCode.button(other), color = AppColors.ink, fontWeight = FontWeight(700), fontSize = 15.sp)
                        }
                    }
                }
                RoundButton(58.dp, "Language: $language. Choose another", background = AppColors.forest, border = false,
                    onClick = { open = !open }) {
                    Text(LanguageCode.button(language), color = AppColors.saffron, fontWeight = FontWeight(700), fontSize = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    size: Dp,
    description: String,
    modifier: Modifier = Modifier,
    background: Color = AppColors.card,
    border: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(background)
            .then(if (border) Modifier.border(1.dp, AppColors.lineStrong, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { content() }
}

private fun share(context: Context, title: String, url: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    context.startActivity(Intent.createChooser(send, "Share"))
}

/** Downloads (or reuses) the PDF, then shows its pages; a message and Try again if it can't. */
@Composable
private fun PdfPages(url: String) {
    val context = LocalContext.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var document by remember(url, attempt) { mutableStateOf<PdfFile?>(null) }
    LaunchedEffect(url, attempt) {
        document = withContext(Dispatchers.IO) { runCatching { PdfFile.open(download(context, url)) }.getOrNull() }
            ?: PdfFile.FAILED
    }
    // Close this PDF when another replaces it or the reader closes (capture it: the state changes first)
    val opened = document
    DisposableEffect(opened) { onDispose { opened?.close() } }

    when (val pdf = document) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.clay)
        }
        PdfFile.FAILED -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This text couldn't be opened. Check your connection and try again.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft, textAlign = TextAlign.Center)
            Button(onClick = { attempt++ }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton)) {
                Text("Try again")
            }
        }
        else -> ZoomablePages(pdf)
    }
}

/** The pages one under another at the screen's width (up to 720dp on a tablet, centered); two fingers zoom
 *  (1x to 4x), one finger scrolls. */
@Composable
private fun ZoomablePages(pdf: PdfFile) {
    var zoom by remember(pdf) { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(pdf) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            zoom = (zoom * event.calculateZoom()).coerceIn(1f, 4f)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val viewport = maxWidth
        val pageWidth = minOf(viewport, 720.dp) * zoom
        val widthPx = with(density) { pageWidth.toPx() }.toInt().coerceAtMost(3000)
        val scroll = rememberScrollState()
        Box(
            Modifier.fillMaxSize().horizontalScroll(scroll, enabled = pageWidth > viewport),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                modifier = Modifier.width(maxOf(pageWidth, viewport)).fillMaxSize().background(AppColors.cream),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(pdf.pageCount) { index ->
                    PdfPage(pdf, index, widthPx)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(pdf: PdfFile, index: Int, widthPx: Int) {
    var bitmap by remember(pdf, index, widthPx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdf, index, widthPx) { bitmap = pdf.render(index, widthPx) }
    val ratio = pdf.aspectRatio(index)
    Box(
        Modifier
            .width(with(LocalDensity.current) { widthPx.toDp() })
            .aspectRatio(ratio)
            .background(AppColors.card)
            .semantics { contentDescription = "Page ${index + 1} of ${pdf.pageCount}" },
    ) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/** An open PDF; Android's renderer handles one page at a time, so rendering takes turns. */
private class PdfFile private constructor(private val renderer: PdfRenderer?) {
    private val lock = Mutex()
    private val ratios = mutableMapOf<Int, Float>()
    private var closed = false
    val pageCount: Int = renderer?.pageCount ?: 0

    fun aspectRatio(index: Int): Float = ratios.getOrPut(index) {
        synchronized(this) {
            if (closed || renderer == null) 0.707f
            else renderer.openPage(index).use { it.width.toFloat() / it.height.coerceAtLeast(1) }
        }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            synchronized(this@PdfFile) {
                if (closed || renderer == null) return@withLock null
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }

    fun close() = synchronized(this) {
        if (!closed) {
            closed = true
            renderer?.close()
        }
    }

    companion object {
        val FAILED = PdfFile(null)
        fun open(file: File) = PdfFile(PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)))
    }
}

/** The PDF in the app's cache, downloaded the first time; reused after (cache files are cleared by Android). */
private fun download(context: Context, url: String): File {
    val name = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        .joinToString("") { "%02x".format(it) } + ".pdf"
    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(dir, name)
    if (file.length() > 0) return file
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 30000
    try {
        if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
        val part = File(dir, "$name.part")
        connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
        if (!part.renameTo(file)) error("couldn't save")
    } finally {
        connection.disconnect()
    }
    return file
}
