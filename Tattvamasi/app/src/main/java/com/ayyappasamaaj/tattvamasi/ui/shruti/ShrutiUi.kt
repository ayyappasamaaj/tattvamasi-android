package com.ayyappasamaaj.tattvamasi.ui.shruti

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/*
 * The tanpura's screens (ShrutiPlayer.kt): the Tanpura row on Publications and each category, the sheet it opens
 * (key, Sa–Pa or Sa–Ma, volume, play), and the bar above the navigation while the tanpura is in use.
 */

/** "Tanpura · Shruti to sing along with · C · 1 · Pa": opens the sheet. */
@Composable
fun TanpuraRow(modifier: Modifier = Modifier) {
    val s by ShrutiPlayer.state.collectAsState()
    val label = Shruti.label(s.key, s.tuning)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.card)
            .border(1.dp, AppColors.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { ShrutiPlayer.openSheet() }
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Tanpura, ${Shruti.spoken(s.key)}, ${Shruti.tuningName(s.tuning)}" +
                    (if (s.playing) ", playing" else "") + ". Open"
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.claySoft),
        ) {
            Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = AppColors.clay)
        }
        Column(Modifier.weight(1f)) {
            Text("Tanpura", style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
            Text("Shruti to sing along with", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.clayDark,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AppColors.claySoft)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * The tanpura's bar, from its first Play until ×: play / pause, "Tanpura · C# · 1½ · Pa" (opens the sheet), ×. Above
 * the Namavali's bar when both show; [bottomInset] when it is the lowest (it then clears the phone's own bar).
 */
@Composable
fun ShrutiBar(bottomInset: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val s by ShrutiPlayer.state.collectAsState()
    if (!s.shown) return
    Column(modifier.fillMaxWidth().background(AppColors.forest)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (bottomInset) Modifier.navigationBarsPadding() else Modifier)
                .heightIn(min = 56.dp)
                .padding(start = 12.dp, end = 4.dp),
        ) {
            IconButton(
                onClick = { ShrutiPlayer.toggle(context) },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.saffron),
            ) {
                Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (s.playing) "Pause the tanpura" else "Play the tanpura", tint = OnSaffron)
            }
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button) { ShrutiPlayer.openSheet() }
                    .padding(vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Tanpura, ${Shruti.spoken(s.key)}, ${Shruti.tuningName(s.tuning)}" +
                            (if (s.playing) ", playing" else ", paused") + ". Change"
                    },
            ) {
                Text("Tanpura · ${Shruti.label(s.key, s.tuning)}", style = MaterialTheme.typography.titleSmall,
                    color = AppColors.onForest, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (s.playing) "Keeps playing while you read" else "Paused",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.onForest.copy(alpha = 0.8f))
            }
            IconButton(onClick = { ShrutiPlayer.stop() }) {
                Icon(Icons.Outlined.Close, contentDescription = "Stop the tanpura", tint = AppColors.onForest)
            }
        }
    }
}

/** Dark text on the saffron play button, in both modes. */
private val OnSaffron = androidx.compose.ui.graphics.Color(0xFF1E2A22)

/** The Tanpura sheet: the 12 keys with their kattai, Sa–Pa or Sa–Ma, volume, and Play. Shown once, from AppRoot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanpuraSheet() {
    val context = LocalContext.current
    val s by ShrutiPlayer.state.collectAsState()
    if (!s.sheet) return
    ModalBottomSheet(
        onDismissRequest = { ShrutiPlayer.closeSheet() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.card,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tanpura", style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                    modifier = Modifier.weight(1f).semantics { heading() })
                TextButton(onClick = { ShrutiPlayer.closeSheet() }) { Text("Done", color = AppColors.clay) }
            }
            // The 12 keys, four to a row
            Shruti.KEYS.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val on = key == s.key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) AppColors.claySoft else AppColors.card)
                                .border(if (on) 2.dp else 1.dp, if (on) AppColors.clay else AppColors.line, RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { ShrutiPlayer.choose(context, key) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = Shruti.spoken(key)
                                    selected = on
                                },
                        ) {
                            Text(key.note, style = MaterialTheme.typography.titleMedium,
                                color = if (on) AppColors.clayDark else AppColors.ink)
                            Text(key.kattai, style = MaterialTheme.typography.bodySmall,
                                color = if (on) AppColors.clayDark else AppColors.muted)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tuning", style = MaterialTheme.typography.titleSmall, color = AppColors.inkSoft, modifier = Modifier.width(72.dp))
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    listOf(Shruti.PA, Shruti.MA).forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = s.tuning == t,
                            onClick = { ShrutiPlayer.tuning(context, t) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = AppColors.claySoft, activeContentColor = AppColors.clayDark,
                                inactiveContainerColor = AppColors.card, inactiveContentColor = AppColors.ink,
                                activeBorderColor = AppColors.clay, inactiveBorderColor = AppColors.lineStrong,
                            ),
                        ) { Text(Shruti.tuningName(t)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Volume", style = MaterialTheme.typography.titleSmall, color = AppColors.inkSoft, modifier = Modifier.width(72.dp))
                Slider(
                    value = s.volume,
                    onValueChange = { ShrutiPlayer.volume(context, it) },
                    colors = SliderDefaults.colors(thumbColor = AppColors.clay, activeTrackColor = AppColors.clay,
                        inactiveTrackColor = AppColors.line),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Tanpura volume" },
                )
            }
            Button(
                onClick = { ShrutiPlayer.toggle(context) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            ) {
                Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                Text(if (s.playing) "Pause" else "Play", modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = AppColors.line)
            Text(
                "Keeps playing while you read PDFs, change tabs or lock the phone. Pauses while the Namavali plays, " +
                    "and the Namavali pauses when you start it.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.muted,
            )
        }
    }
}
