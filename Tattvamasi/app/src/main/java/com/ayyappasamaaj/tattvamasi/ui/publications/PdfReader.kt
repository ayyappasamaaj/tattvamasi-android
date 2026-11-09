@file:OptIn(ExperimentalMaterial3Api::class)

package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Reads one text full screen: the PDF's pages one under another, pinch to zoom; Close and Share at the top, the
 * language switch at the bottom. As on the iOS app (PDFReaderView.swift), with Android's own PDF renderer.
 */
@Composable
fun PdfReader(item: ReadingItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val versions = item.text.versions
    var language by rememberSaveable(item.text.id) {
        mutableStateOf((versions.firstOrNull { it.language == item.startLanguage } ?: versions.first()).language)
    }
    val version = versions.firstOrNull { it.language == language } ?: versions.first()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = AppColors.cream,
            topBar = {
                TopAppBar(
                    title = {
                        Text(item.text.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium)
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
                    },
                    actions = {
                        IconButton(onClick = { share(context, item.text.title, version.pageUrl) }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.card),
                )
            },
            bottomBar = {
                if (versions.size > 1) {
                    Surface(color = AppColors.card) {
                        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                            contentAlignment = Alignment.Center) {
                            LanguageChoice(
                                options = versions.map { it.language },
                                labels = versions.associate { it.language to it.label },
                                selected = language,
                                onSelect = { language = it },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                PdfPages(version.fileUrl)
            }
        }
    }
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
            Button(onClick = { attempt++ }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest)) {
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
