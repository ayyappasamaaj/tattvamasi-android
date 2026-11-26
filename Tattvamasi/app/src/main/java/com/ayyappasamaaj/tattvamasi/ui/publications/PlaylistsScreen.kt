@file:OptIn(ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch

/*
 * Playlists in Publications (Playlists.kt): the account's playlists, the same ones as on the website and the
 * dashboard. The list (make one, or one from the phone's Saved texts), and a playlist's page: Play, Share its link,
 * Rename, Delete, and its texts (tap one to play from it; move up or down; remove). Signed out, it offers Sign In, as
 * Donate's monthly gifts do. The reader's Playlist button opens [AddToPlaylistDialog]. As on the iOS app
 * (PlaylistsView.swift).
 */

/** "Playlists · 3": the way to the account's playlists, under the Saved row. */
@Composable
internal fun PlaylistsRow(count: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.claySoft else AppColors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) AppColors.clay else AppColors.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Playlists" + (count?.let { ", $it" } ?: "")
            },
    ) {
        Icon(Icons.AutoMirrored.Outlined.QueueMusic, contentDescription = null, tint = AppColors.clay)
        Text("Playlists" + (count?.let { " · $it" } ?: ""), style = MaterialTheme.typography.titleMedium,
            color = AppColors.ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}

@Composable
private fun BackTo(label: String, onBack: () -> Unit) {
    TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
        Text(label, color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Heading(title: String, detail: String?) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
            color = AppColors.ink, modifier = Modifier.semantics { heading() })
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted) }
    }
}

@Composable
private fun mainButtonColors() = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton)

/** The website's reason a change didn't happen, until the next change. */
@Composable
private fun Problem(message: String?) {
    if (message == null) return
    Text(message, style = MaterialTheme.typography.bodyLarge, color = AppColors.clayDark,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).semantics { contentDescription = "Problem: $message" })
}

/**
 * The account's playlists, or one of them. [saved]: the phone's Saved texts (a new playlist can start from them);
 * [onPlay] opens the reader on a playlist's text.
 */
@Composable
internal fun PlaylistsContent(
    sections: List<PublicationSection>,
    saved: List<PublicationHit>,
    onPlay: (ReadingItem) -> Unit,
    onBack: (() -> Unit)?,
) {
    val account by AccountStore.state.collectAsState()
    val state by PlaylistStore.state.collectAsState()
    val preferred by ReadingPrefs.language.collectAsState()
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(account.user) { if (account.user != null) PlaylistStore.load() }
    val open = state.data?.playlists?.firstOrNull { it.id == openId }
    if (open != null && account.user != null) {
        BackHandler { openId = null; PlaylistStore.clearMessage() }
        PlaylistContent(open, state.data?.maxPdfs ?: 25, sections, state.message, onPlay,
            onBack = { openId = null; PlaylistStore.clearMessage() })
        return
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = Modifier.fillMaxSize()) {
        if (onBack != null) item { BackTo("Publications", onBack) }
        val data = state.data
        item {
            Heading("Playlists", when {
                account.user == null -> "With your ayyappasamaaj.org account"
                data != null -> "${data.playlists.size} of ${data.maxPlaylists} · the same as on the website"
                else -> null
            })
        }
        when {
            account.user == null -> item {
                Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sign in to keep up to 5 playlists of bhajans and shlokas on your account, the same ones as " +
                        "on the website. Play a playlist in order, change it here, and share its link.",
                        style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                    Text("Without signing in, the bookmark in the reader keeps texts on this phone (Saved).",
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                    Button(onClick = { AccountStore.open() }, colors = mainButtonColors()) { Text("Sign In") }
                }
            }
            data == null && state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.clay)
                }
            }
            data == null -> item {
                Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your playlists couldn't be loaded. Check your connection and try again.",
                        style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                    Button(onClick = { scope.launch { PlaylistStore.load() } }, colors = mainButtonColors()) { Text("Try Again") }
                }
            }
            else -> {
                item { Problem(state.message) }
                item {
                    if (data.playlists.isEmpty()) {
                        Text("No playlists yet. Make one here, or open a text and tap its playlist button to add it. " +
                            "Up to ${data.maxPlaylists} playlists of ${data.maxPdfs} texts each.",
                            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                            modifier = Modifier.fillMaxWidth().card())
                    } else {
                        Column(Modifier.fillMaxWidth().card(padding = 14.dp)) {
                            data.playlists.forEachIndexed { i, playlist ->
                                if (i > 0) HorizontalDivider(color = AppColors.line)
                                PlaylistRow(playlist) { PlaylistStore.clearMessage(); openId = playlist.id }
                            }
                        }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp)) {
                        Button(onClick = {
                            scope.launch { PlaylistStore.create()?.let { openId = it.id } }
                        }, enabled = !data.full, colors = mainButtonColors()) { Text("New Playlist") }
                        // The phone's Saved texts, into a playlist on the account (each in its reading language)
                        val fromSaved = savedPdfIds(saved, preferred, data.maxPdfs)
                        if (fromSaved.isNotEmpty()) {
                            OutlinedButton(onClick = {
                                scope.launch { PlaylistStore.create(pdfs = fromSaved)?.let { openId = it.id } }
                            }, enabled = !data.full) {
                                Text("New from Saved (${fromSaved.size})", color = AppColors.clayDark)
                            }
                        }
                    }
                }
                if (data.full) {
                    item {
                        Text("You can keep up to ${data.maxPlaylists} playlists. Delete one to make another.",
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
                item {
                    Text("Share links open only for people who are signed in on the website.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted,
                        modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item { AddressFooter() }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            Text(textCount(playlist.pdfs.size), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = AppColors.muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}

/** "1 text", "7 texts" */
fun textCount(n: Int) = "$n text" + if (n == 1) "" else "s"

/** One playlist: Play, Share, Rename, Delete, and its texts in order. */
@Composable
private fun PlaylistContent(
    playlist: Playlist,
    maxPdfs: Int,
    sections: List<PublicationSection>,
    message: String?,
    onPlay: (ReadingItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries = resolvePlaylist(playlist, sections)
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // One change at a time (each sends the whole order to the website)
    fun change(ids: List<Int>) {
        if (busy) return
        busy = true
        scope.launch { try { PlaylistStore.setPdfs(playlist.id, ids) } finally { busy = false } }
    }

    if (renaming) RenameDialog(playlist.name, onDismiss = { renaming = false }) { name ->
        renaming = false
        scope.launch { PlaylistStore.rename(playlist.id, name) }
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete ${playlist.name}?") },
            text = { Text("It goes from your account, on the website too, and its share link stops working.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    scope.launch { if (PlaylistStore.delete(playlist.id)) onBack() }
                }) { Text("Delete", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Keep It", color = AppColors.ink) } },
        )
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = Modifier.fillMaxSize()) {
        item { BackTo("Playlists", onBack) }
        item { Heading(playlist.name, "${playlist.pdfs.size} of $maxPdfs texts") }
        item { Problem(message) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)) {
                val first = playFrom(playlist, entries)
                if (first != null) Button(onClick = { onPlay(first) }, colors = mainButtonColors()) { Text("▶ Play") }
                OutlinedButton(onClick = { sharePlaylist(context, playlist) }) { Text("Share", color = AppColors.clayDark) }
                OutlinedButton(onClick = { renaming = true }) { Text("Rename", color = AppColors.clayDark) }
                OutlinedButton(onClick = { deleting = true }) { Text("Delete", color = AppColors.clayDark) }
            }
        }
        item {
            if (entries.isEmpty()) {
                Text("Empty. Open a text and tap its playlist button to add it here.",
                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.fillMaxWidth().card())
            } else {
                Column(Modifier.fillMaxWidth().card(padding = 14.dp)) {
                    entries.forEachIndexed { i, entry ->
                        if (i > 0) HorizontalDivider(color = AppColors.line)
                        EntryRow(
                            entry, i, entries.size,
                            onOpen = { playFrom(playlist, entries, i)?.let(onPlay) },
                            onMove = { to -> change(movedPdf(playlist.pdfIds, i, to)) },
                            onRemove = { change(playlist.pdfIds.filterIndexed { at, _ -> at != i }) },
                        )
                    }
                }
            }
        }
        item {
            Text("Tap a title to play from it. Share links open only for people who are signed in on the website.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted,
                modifier = Modifier.padding(top = 12.dp))
        }
        item { AddressFooter() }
    }
}

@Composable
private fun EntryRow(
    entry: PlaylistEntry,
    index: Int,
    count: Int,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val label = entry.pdf.label
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(enabled = entry.item != null, role = Role.Button, onClick = onOpen)
                .padding(vertical = 8.dp),
        ) {
            Text("${index + 1}. $label", style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            Text(entry.item?.categoryName ?: "Not in the app", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = AppColors.muted)
        }
        IconButton(onClick = { onMove(index - 1) }, enabled = index > 0) {
            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move $label up",
                tint = if (index > 0) AppColors.ink else AppColors.line)
        }
        IconButton(onClick = { onMove(index + 1) }, enabled = index < count - 1) {
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move $label down",
                tint = if (index < count - 1) AppColors.ink else AppColors.line)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove $label", tint = AppColors.muted)
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(PLAYLIST_NAME_MAX) },
                label = { Text("Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.clay, focusedLabelColor = AppColors.clayDark,
                    cursorColor = AppColors.clay),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && name.trim() != current) onSave(name.trim()) else onDismiss() }) {
                Text("Save", color = AppColors.clayDark)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.ink) } },
    )
}

/** The website's longest playlist name (Playlist.name's max_length) */
const val PLAYLIST_NAME_MAX = 60

private fun sharePlaylist(context: Context, playlist: Playlist) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, playlist.name)
        .putExtra(Intent.EXTRA_TEXT, "${playlist.name}\n${playlist.shareUrl}")
    context.startActivity(Intent.createChooser(send, "Share"))
}

/**
 * The reader's Playlist button: add the open text, in the language shown ([pdfId]), to one of the account's
 * playlists, or to a new one.
 */
@Composable
internal fun AddToPlaylistDialog(pdfId: Int, language: String, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by PlaylistStore.state.collectAsState()
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { PlaylistStore.clearMessage(); PlaylistStore.load() }
    fun done(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        onDismiss()
    }
    fun addTo(playlist: Playlist, max: Int) {
        if (busy) return
        when (val result = addingPdf(playlist.pdfIds, pdfId, max)) {
            AddToPlaylist.AlreadyThere -> done("Already in ${playlist.name}")
            AddToPlaylist.Full -> done("A playlist holds up to $max texts.")
            is AddToPlaylist.Added -> {
                busy = true
                scope.launch {
                    val changed = try { PlaylistStore.setPdfs(playlist.id, result.ids) } finally { busy = false }
                    if (changed != null) done("Added to ${changed.name}")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to a playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (language.isBlank()) title else "$title ($language)", style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.muted, modifier = Modifier.padding(bottom = 8.dp))
                val data = state.data
                state.message?.let { Text(it, color = AppColors.clayDark) }
                when {
                    data == null && state.loading -> CircularProgressIndicator(color = AppColors.clay)
                    data == null -> Text("Your playlists couldn't be loaded. Check your connection and try again.",
                        color = AppColors.inkSoft)
                    else -> {
                        data.playlists.forEach { playlist ->
                            val inIt = pdfId in playlist.pdfIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) { addTo(playlist, data.maxPdfs) }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(playlist.name, color = AppColors.ink, style = MaterialTheme.typography.bodyLarge)
                                    Text(if (inIt) "Already in it" else "${playlist.pdfs.size} of ${data.maxPdfs} texts",
                                        color = AppColors.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
                                }
                            }
                        }
                        if (!data.full) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) {
                                        if (busy) return@clickable
                                        busy = true
                                        scope.launch {
                                            val made = try { PlaylistStore.create(pdfs = listOf(pdfId)) } finally { busy = false }
                                            if (made != null) done("Added to ${made.name}")
                                        }
                                    }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null, tint = AppColors.clay)
                                Text("New playlist", color = AppColors.clayDark, style = MaterialTheme.typography.bodyLarge)
                            }
                        } else if (data.playlists.isEmpty()) {
                            Text("No playlists.", color = AppColors.inkSoft)
                        } else {
                            Text("You can keep up to ${data.maxPlaylists} playlists.", color = AppColors.muted,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.ink) } },
    )
}
