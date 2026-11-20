package com.ayyappasamaaj.tattvamasi.ui.yatra

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/** Every name with a recording, in order: its number, its clip's address, and a title for the lock screen. */
fun recordedNames(yatra: Yatra?): List<Triple<Int, String, String>> =
    yatra?.data?.names?.values.orEmpty().filter { it.au.isNotEmpty() }.sortedBy { it.n }
        .map { Triple(it.n, it.au, "Name ${it.n}: ${it.t}") }

private fun speedLabel(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "×"

/**
 * The Namavali's player bar, on every screen while something is playing or paused (from the first Listen or Play all
 * until Close). First row: the name (number and script; "Your turn" and a filling bar during Recite with me's pause)
 * and Recite with me. Second row: play / pause, previous, next, # (start from a name), the speed and close.
 */
@Composable
fun PlayerBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val listen by NamavaliPlayer.state.collectAsState()
    val yatraState by YatraStore.state.collectAsState()
    var asking by remember { mutableStateOf(false) }
    LaunchedEffect(listen.message) {
        listen.message?.let { toast(context, it); NamavaliPlayer.clearMessage() }
    }
    if (!listen.shown) return
    val yatra = yatraState.yatra
    val recorded = recordedNames(yatra)
    val n = listen.n ?: return
    val name = yatra?.data?.names?.get(n)
    val total = yatra?.data?.names?.size ?: 1002
    val at = recorded.indexOfFirst { it.first == n }
    Column(modifier.fillMaxWidth().background(AppColors.card)) {
        HorizontalDivider(color = AppColors.line)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    val turn = listen.turnMs != null
                    Text(
                        (if (turn) "Your turn · " else "") + "Name $n of ${"%,d".format(total)}" +
                            if (listen.all && listen.playing && !turn) " · with tanpura" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (turn) AppColors.clay else AppColors.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (name != null) {
                        Text("${yatra?.data?.om ?: "ॐ"} ${name.d} ${yatra?.data?.namah ?: "नमः"}", fontFamily = ScriptFont,
                            fontSize = 18.sp, color = AppColors.clay, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TurnBar(listen)
                }
                val on = listen.recite.isNotEmpty()
                Text(
                    "Recite with me" + if (on) " · ${NamavaliPlayer.RECITE_WORD[listen.recite]}" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) AppColors.card else AppColors.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) AppColors.clay else AppColors.card)
                        .border(1.dp, if (on) AppColors.clay else AppColors.line, RoundedCornerShape(50))
                        .clickable(role = Role.Button) { toast(context, NamavaliPlayer.cycleRecite()) }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .semantics {
                            contentDescription = "Recite with me: ${NamavaliPlayer.RECITE_WORD[listen.recite]}. Change"
                        },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { NamavaliPlayer.toggle(context, recorded) },
                    modifier = Modifier.size(48.dp).background(AppColors.clay, CircleShape),
                ) {
                    Icon(if (listen.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (listen.playing) "Pause" else "Play", tint = AppColors.card)
                }
                IconButton(onClick = { NamavaliPlayer.previous(context, recorded) }, enabled = at > 0) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous name")
                }
                IconButton(onClick = { NamavaliPlayer.next(context, recorded) }, enabled = at in 0 until recorded.size - 1) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next name")
                }
                TextButton(
                    onClick = { asking = true },
                    modifier = Modifier.heightIn(min = 44.dp).semantics { contentDescription = "Start from another name" },
                ) { Text("#", style = MaterialTheme.typography.titleLarge, color = AppColors.ink) }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { NamavaliPlayer.cycleSpeed() },
                    modifier = Modifier.heightIn(min = 44.dp).semantics { contentDescription = "Speed ${speedLabel(listen.speed)}. Change speed" },
                ) { Text(speedLabel(listen.speed), color = AppColors.ink) }
                IconButton(onClick = { NamavaliPlayer.close() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close the player", tint = AppColors.muted)
                }
            }
        }
    }
    if (asking) {
        StartFromDialog(
            current = n,
            onGo = { typed ->
                val names = yatra?.data?.names?.keys ?: (1..1002).toSet()
                val message = NamavaliPlayer.startFrom(context, recorded, names, typed)
                val bad = message != null && message.startsWith("Please")
                if (message != null) toast(context, message)
                if (!bad) asking = false
            },
            onDismiss = { asking = false },
        )
    }
}

/** During Recite with me's pause: a thin bar filling over the listener's turn. */
@Composable
private fun TurnBar(listen: ListenState) {
    val ms = listen.turnMs ?: return
    var progress by remember(listen.turnStart) { mutableFloatStateOf(0f) }
    LaunchedEffect(listen.turnStart) {
        while (true) {
            progress = ((SystemClock.elapsedRealtime() - listen.turnStart).toFloat() / ms).coerceIn(0f, 1f)
            if (progress >= 1f) break
            delay(50)
        }
    }
    LinearProgressIndicator(
        progress = { progress },
        color = AppColors.clay,
        trackColor = AppColors.line,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
    )
}

/** Start from name: a number; Go plays from it (or the next recorded name); a wrong number keeps the box open. */
@Composable
private fun StartFromDialog(current: Int, onGo: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start from name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                placeholder = { Text("$current") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo(text) }),
                label = { Text("Name number") },
            )
        },
        confirmButton = { TextButton(onClick = { onGo(text) }) { Text("Go", color = AppColors.clayDark) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** On a name's card: Listen to this name, and Play all from here over the tanpura. */
@Composable
internal fun ListenButtons(yatra: Yatra, name: Name) {
    if (name.au.isEmpty()) return
    val context = LocalContext.current
    val listen by NamavaliPlayer.state.collectAsState()
    val playingThis = listen.n == name.n && listen.playing && !listen.all
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            if (playingThis) NamavaliPlayer.pause() else NamavaliPlayer.play(context, recordedNames(yatra), name.n, all = false)
        }) {
            Icon(if (playingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = AppColors.clayDark)
            Text(if (playingThis) "Pause" else "Listen", color = AppColors.clayDark, modifier = Modifier.padding(start = 4.dp))
        }
        TextButton(onClick = { NamavaliPlayer.play(context, recordedNames(yatra), name.n, all = true) }) {
            Text("Play all from here, with tanpura", color = AppColors.clayDark)
        }
    }
}

/** On the path: the Namavali card, Play all (or Resume from where listening stopped, and Start from the first name). */
@Composable
internal fun ListenCard(yatra: Yatra) {
    val context = LocalContext.current
    val recorded = recordedNames(yatra)
    if (recorded.isEmpty()) return
    val first = recorded.first().first
    val from = NamavaliPlayer.savedPosition(context)?.takeIf { at -> recorded.any { it.first == at } }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().background(AppColors.card, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.line, RoundedCornerShape(16.dp)).padding(16.dp),
    ) {
        Eyebrow("Listen")
        Text("The Namavali, name by name", style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
        Text(
            "All ${"%,d".format(yatra.data.names.size)} names chanted one after another as “Om … namaḥ”, over a tanpura drone. " +
                "About an hour; it picks up where you stopped, and goes on with the screen off.",
            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(if (from != null && from > first) "Resume from name $from" else "Play all") {
                NamavaliPlayer.play(context, recorded, from ?: first, all = true)
            }
            if (from != null && from > first) {
                TextButton(onClick = { NamavaliPlayer.play(context, recorded, first, all = true) }) {
                    Text("Start from name $first", color = AppColors.clayDark)
                }
            }
        }
    }
}
