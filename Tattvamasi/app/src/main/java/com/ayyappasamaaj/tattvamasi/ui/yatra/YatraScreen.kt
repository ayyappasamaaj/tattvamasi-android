@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.yatra

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The yatra's own colors beside the app's: the gold of a finished day (the website's --gold). */
internal object YatraColors {
    val gold = Color(0xFF8A5A12)
    val goldSoft = Color(0xFFF7EBD3)
    val forestSoft = Color(0xFFE6EEE8)
}

/** The verses and names, in Devanagari or a language's script: the phone's own fonts, which have every script. */
internal val ScriptFont: FontFamily = FontFamily.Default

internal fun shortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

internal fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

/** Shares a name as the website does: the name, its meaning and the day, with the page's link. */
internal fun shareName(context: Context, yatra: Yatra, name: Name, day: Int) {
    val data = yatra.data
    val text = "${data.om} ${name.d} ${data.namah} (Om ${name.t} namaḥ) — ${name.m} · Sahasranama Yatra, Day $day"
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Sahasranama Yatra")
        .putExtra(Intent.EXTRA_TEXT, "$text ${yatra.feed.url}#day-$day")
    context.startActivity(Intent.createChooser(send, "Share this name"))
}

/** Where the reader is: choosing Day 1, the path, or a day. Saved as text across rotation. */
private sealed interface View {
    data object Start : View
    data object Path : View
    data class Day(val day: Int) : View

    fun encode(): String = when (this) {
        Start -> "start"
        Path -> "path"
        is Day -> "day-$day"
    }

    companion object {
        fun decode(text: String): View = when {
            text == "start" -> Start
            text.startsWith("day-") -> text.removePrefix("day-").toIntOrNull()?.let { Day(it) } ?: Path
            else -> Path
        }
    }
}

/**
 * The Sahasranama Yatra, as on the website: choose Day 1, then walk the path of the trek a day at a time. Opened from
 * Home or Publications; [onClose] goes back there.
 */
@Composable
fun YatraScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { YatraStore.start(context) }
    val state by YatraStore.state.collectAsState()
    val yatra = state.yatra
    val progress = state.progress
    val today = LocalDate.now()

    var viewText by rememberSaveable { mutableStateOf("path") }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var introOpen by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val view = if (progress.start == null) View.Start else View.decode(viewText)

    LaunchedEffect(state.message) {
        state.message?.let { toast(context, it); YatraStore.clearMessage() }
    }

    fun openDay(day: Int) {
        val y = yatra ?: return
        viewText = View.Day(day).encode()
        step = y.firstStep(day, progress)
    }

    BackHandler {
        when {
            view is View.Day -> viewText = "path"
            view is View.Start && progress.start != null -> viewText = "path"
            else -> onClose()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .statusBarsPadding(),
    ) {
        TopBar(
            back = if (view is View.Day) "Path" else "Publications",
            onBack = { if (view is View.Day || (view is View.Start && progress.start != null)) viewText = "path" else onClose() },
            state = state,
            onHowTo = { introOpen = true },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                yatra == null && state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.clay)
                }
                yatra == null -> Text(
                    "The yatra couldn't be loaded. Check your connection and try again.",
                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.padding(16.dp).fillMaxWidth().card(),
                )
                view is View.Start -> StartView(yatra, progress, today) { chosen ->
                    YatraStore.update { it.copy(start = chosen) }
                    toast(context, "Your yatra starts on ${shortDate(chosen)}.")
                    viewText = "path"
                }
                view is View.Day -> DayView(yatra, progress, today, view.day.coerceIn(1, yatra.finalDay), step,
                    onStep = { step = it }, onPath = { viewText = "path" }, onDay = ::openDay)
                else -> PathView(yatra, progress, today, onDay = ::openDay, onChangeStart = { viewText = "start" },
                    onStartOver = {
                        YatraStore.update { Progress() }
                        toast(context, "Cleared. Choose your Day 1 to begin again.")
                        viewText = "start"
                    })
            }
        }
    }

    // The welcome: on the first visit, and from "How it works"
    if (yatra != null && (introOpen ?: !progress.intro)) {
        val target = if (view is View.Day) view.day else yatra.currentDay(progress, today)
        Intro(
            yatra, progress, today, inDay = view is View.Day, target = target,
            onBegin = {
                introOpen = false
                YatraStore.update { it.copy(intro = true) }
                if (!(view is View.Day && view.day == target)) openDay(target)
            },
            onClose = {
                introOpen = false
                YatraStore.update { it.copy(intro = true) }
            },
        )
    }
}

@Composable
private fun TopBar(back: String, onBack: () -> Unit, state: YatraState, onHowTo: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
            Text(back, color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
        }
        Text(
            "Sahasranama Yatra",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.ink,
            maxLines = 1,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        ScriptMenu(state)
        IconButton(onClick = onHowTo) {
            Icon(Icons.Outlined.Info, contentDescription = "How it works", tint = AppColors.ink)
        }
    }
    HorizontalDivider(color = AppColors.line)
}

/** "Aa": the script the verses and names are shown in (Devanagari, or a live language's). */
@Composable
private fun ScriptMenu(state: YatraState) {
    val languages = state.yatra?.feed?.languages.orEmpty()
    if (languages.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = "Script for the verses and names" }) {
            Text("Aa", style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val options = listOf("" to "देवनागरी · Devanagari") + languages.map { it.code to "${it.name} · ${it.english}" }
            options.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontFamily = ScriptFont) },
                    leadingIcon = {
                        if (code == state.script) Icon(Icons.Outlined.Check, contentDescription = "Chosen", tint = AppColors.clay)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        if (code != state.script) YatraStore.chooseScript(code)
                    },
                )
            }
        }
    }
}

/** A dark green button, as on the website. */
@Composable
internal fun PrimaryButton(text: String, modifier: Modifier = Modifier, lamp: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest, contentColor = AppColors.cream),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        if (lamp) {
            Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight(600), textAlign = TextAlign.Center)
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = AppColors.muted) {
    Text(text.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
        color = color)
}

// ---------- Start: choosing Day 1 ----------

@Composable
private fun StartView(yatra: Yatra, progress: Progress, today: LocalDate, onChosen: (LocalDate) -> Unit) {
    var chosen by rememberSaveable { mutableStateOf((progress.start ?: today).toString()) }
    var picking by remember { mutableStateOf(false) }
    val date = LocalDate.parse(chosen)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        ReadableWidth(640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Eyebrow("${yatra.totalVerses} verses · 1000 names · ${yatra.finalDay} days")
                Text(if (progress.start != null) "Change your start date" else "Begin your yatra",
                    style = MaterialTheme.typography.displaySmall, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })
                Text(
                    "Chant the Sri Dharma Shastha Sahasranamam four verses (about ${yatra.namesPerDay} names) a day, " +
                        "from Erumeli to the Sannidhanam. Choose your Day 1, for example the first day of Vratam or " +
                        "simply today. Each day’s verses and names open on their day.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = AppColors.inkSoft,
                )
                Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Day 1 of my yatra", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())),
                            color = AppColors.ink)
                    }
                    PrimaryButton(if (progress.start != null) "Save start date" else "Start my yatra", lamp = true) {
                        onChosen(date)
                    }
                }
                if (progress.start != null) {
                    Text("The verses and names you have already chanted stay marked.",
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                }
                Text("Your start date and progress are saved only on this device.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
        }
    }
    if (picking) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        chosen = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}

// ---------- The path ----------

@Composable
private fun PathView(
    yatra: Yatra,
    progress: Progress,
    today: LocalDate,
    onDay: (Int) -> Unit,
    onChangeStart: () -> Unit,
    onStartOver: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val current = yatra.currentDay(progress, today)
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = full) { TodayCard(yatra, progress, today) { onDay(current) } }
        item(span = full) {
            Column(Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your path", style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    val streak = yatra.streak(progress, today)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(YatraColors.goldSoft, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null, tint = YatraColors.gold,
                            modifier = Modifier.size(16.dp))
                        Text(" $streak-day lamp", style = MaterialTheme.typography.bodyMedium, color = YatraColors.gold)
                    }
                }
                Text(
                    "${yatra.versesDone(progress)} of ${yatra.totalVerses} verses · " +
                        "${"%,d".format(yatra.namesDone(progress))} of ${"%,d".format(yatra.totalNames)} names",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                )
                Text("${yatra.stages.size} stages of the trek · 4 days each · tap a day",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
        }
        itemsIndexed(yatra.stages, key = { i, _ -> "stage-$i" }) { i, name ->
            StageCard(yatra, progress, today, i, name, current, onDay)
        }
        item(span = full) { FinalRow(yatra, progress, today, current) { onDay(yatra.finalDay) } }
        item(span = full) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text("Your progress is saved on this device.", style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onChangeStart) { Text("Change your start date", color = AppColors.clayDark) }
                    TextButton(onClick = { confirming = true }) { Text("Start over", color = AppColors.clayDark) }
                }
                if (yatra.feed.credit.isNotBlank()) {
                    Text(yatra.feed.credit, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
                }
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Start over?") },
            text = { Text("This clears your start date, chanted verses and names, and reflections on this device.") },
            confirmButton = {
                TextButton(onClick = { confirming = false; onStartOver() }) { Text("Start over", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }
}

/** The dark green card for today: where you are on the trek, and the button to walk today's steps. */
@Composable
private fun TodayCard(yatra: Yatra, progress: Progress, today: LocalDate, open: () -> Unit) {
    val day = yatra.currentDay(progress, today)
    val started = yatra.todayNumber(progress, today) >= 1
    val verses = yatra.versesOfDay(day)
    val done = yatra.dayDone(day, progress)
    val chanted = verses.count { it in progress.chanted }
    val puja = yatra.feed.mandalaPujaDate
    val button = when {
        day == yatra.finalDay -> "See the Pūrṇāhuti"
        day == yatra.reciteDay -> if (done) "Review the recitation" else "Begin the full recitation"
        done -> "Day complete · review"
        chanted > 0 -> "Continue · verse ${yatra.firstStep(day, progress) + 1} of ${verses.size}"
        else -> "Begin today’s ${verses.size} verses, ${yatra.namesOfDay(day).size} names"
    }
    val sub = when {
        day <= yatra.verseDays -> "Verses ${yatra.verseRange(day)} · ${yatra.namesOfDay(day).size} names · about 10 minutes"
        day == yatra.reciteDay -> "All ${yatra.totalVerses} verses/1000 names in one sitting"
        else -> "Chant together at Sannidhanam" + if (puja.isNotBlank()) ", $puja" else ""
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.forest, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Eyebrow(
            if (started) "Today · Day $day · ${shortDate(yatra.dateOf(day, progress, today))}"
            else "Begins ${shortDate(yatra.dateOf(1, progress, today))} · Day 1",
            color = AppColors.saffron,
        )
        Text(yatra.stageName(day), style = MaterialTheme.typography.headlineMedium, color = AppColors.cream,
            modifier = Modifier.semantics { heading() })
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = AppColors.cream.copy(alpha = 0.85f))
        if (verses.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clearAndSetSemantics {
                contentDescription = "$chanted of ${verses.size} verses chanted"
            }) {
                verses.forEach { v ->
                    Box(Modifier.width(36.dp).height(6.dp).background(
                        if (v in progress.chanted) AppColors.saffron else AppColors.cream.copy(alpha = 0.25f),
                        RoundedCornerShape(3.dp)))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = open,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.saffron, contentColor = AppColors.forest),
            modifier = Modifier.heightIn(min = 52.dp),
        ) { Text("$button →", fontWeight = FontWeight(700)) }
    }
}

/** A stage of the trek: its name, its days and names, and its four day-stones. */
@Composable
private fun StageCard(
    yatra: Yatra,
    progress: Progress,
    today: LocalDate,
    index: Int,
    name: String,
    current: Int,
    onDay: (Int) -> Unit,
) {
    val days = yatra.daysOfStage(index)
    val done = days.all { yatra.dayDone(it, progress) }
    val now = current in days
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (now) YatraColors.goldSoft else AppColors.card, RoundedCornerShape(16.dp))
            .border(1.dp, if (now) AppColors.saffron else AppColors.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Box(
            Modifier.size(32.dp).background(
                when { done -> YatraColors.gold; now -> AppColors.clay; else -> AppColors.claySoft }, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else "${index + 1}", style = MaterialTheme.typography.labelLarge,
                color = if (done || now) AppColors.card else AppColors.clayDark)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                Text(name, style = MaterialTheme.typography.titleLarge,
                    color = if (now) AppColors.clayDark else if (done) YatraColors.gold else AppColors.ink)
                Text("Days ${days.first}–${days.last} · ${yatra.namesOfStage(index)} names" + if (now) " · You are here" else "",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d -> Stone(yatra, progress, today, d, current, Modifier.weight(1f)) { onDay(d) } }
            }
        }
    }
}

/** A day-stone: gold once done, outlined today, closed until its day. */
@Composable
internal fun Stone(
    yatra: Yatra,
    progress: Progress,
    today: LocalDate,
    day: Int,
    current: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val done = day < yatra.finalDay && yatra.dayDone(day, progress)
    val isToday = day == current
    val open = yatra.isOpen(day, progress, today)
    val label = when {
        done -> "Done"
        isToday -> "Today"
        day == yatra.reciteDay -> "Recite"
        else -> ""
    }
    val spoken = "Day $day" + (if (done) ", done" else "") + (if (isToday) ", today" else "") +
        (if (open) "" else ", opens ${shortDate(yatra.dateOf(day, progress, today))}")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when { done -> YatraColors.gold; isToday -> AppColors.card; open -> AppColors.cream; else -> AppColors.cream.copy(alpha = 0.5f) },
                RoundedCornerShape(12.dp),
            )
            .border(if (isToday && !done) 2.dp else 1.dp,
                if (isToday && !done) AppColors.clay else if (done) YatraColors.gold else AppColors.line,
                RoundedCornerShape(12.dp))
            .clickable(enabled = open, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(vertical = 6.dp),
    ) {
        Text("$day", style = MaterialTheme.typography.titleMedium,
            color = when { done -> AppColors.card; open -> AppColors.ink; else -> AppColors.muted.copy(alpha = 0.6f) })
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = if (done) AppColors.card else AppColors.clay)
        }
    }
}

@Composable
private fun FinalRow(yatra: Yatra, progress: Progress, today: LocalDate, current: Int, open: () -> Unit) {
    val isOpen = yatra.isOpen(yatra.finalDay, progress, today)
    val puja = yatra.feed.mandalaPujaDate
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isOpen, role = Role.Button, onClick = open)
            .card(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null,
            tint = if (current == yatra.finalDay) AppColors.clay else YatraColors.gold, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text("Pūrṇāhuti · Day ${yatra.finalDay}", style = MaterialTheme.typography.titleLarge,
                color = if (isOpen) AppColors.ink else AppColors.muted)
            Text("All ${yatra.totalVerses} verses/1000 names together at Sannidhanam" + if (puja.isNotBlank()) ", $puja" else "",
                style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
        }
    }
}

// ---------- The welcome ----------

@Composable
private fun Intro(
    yatra: Yatra,
    progress: Progress,
    today: LocalDate,
    inDay: Boolean,
    target: Int,
    onBegin: () -> Unit,
    onClose: () -> Unit,
) {
    val started = yatra.todayNumber(progress, today) >= 1
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = AppColors.cream,
        title = {
            Column {
                Eyebrow("Welcome · Swamiye Saranam Ayyappa", color = AppColors.clay)
                Text("The Sahasranama Yatra", style = MaterialTheme.typography.headlineSmall, color = AppColors.ink)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(buildAnnotatedString {
                    append("Through the 41 days of Vratam, chant the ")
                    withStyle(SpanStyle(fontWeight = FontWeight(700))) { append("Śrī Dharmaśāstā Sahasranāmam") }
                    append(": ${yatra.totalVerses} verses as Stotram or 1000 names as Namavali. Four verses (about " +
                        "${yatra.namesPerDay} names) a day, and the meaning of every name.")
                }, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                when {
                    progress.start == null -> Note("First, choose your Day 1: the first day of Vratam, or simply today.")
                    !started -> Note("Your yatra begins on ${shortDate(yatra.dateOf(1, progress, today))}. You can look around and try Day 1 now.")
                }
                Text("How each day works", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })
                listOf(
                    "Open today" to "Tap today’s stone on the path, or the Today card.",
                    "Chant the 4 verses" to "Each verse is shown in Sanskrit with an English transliteration. Chant it, then tap Chanted.",
                    "Learn the names in each verse" to "Each verse holds a handful of names (usually 5 to 8). Go through them one by one and see how each name is built from its root words.",
                    "Light your lamp" to "Meet the Name of the Day, then light your lamp. Today’s stone on the path turns gold.",
                ).forEachIndexed { i, (title, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(26.dp).background(AppColors.claySoft, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = AppColors.clayDark)
                        }
                        Column {
                            Text(title, style = MaterialTheme.typography.labelLarge, color = AppColors.ink)
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                        }
                    }
                }
                Text("Your path to Sabarimala", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })
                Text("The 41 days follow the ${yatra.stages.size} stages of the trek, from Erumeli to the Sannidhanam, " +
                    "four days each. Day ${yatra.reciteDay} is the full recitation of all ${yatra.totalVerses} verses/1000 " +
                    "names. Day ${yatra.finalDay} is the Pūrṇāhuti: we chant together at Sannidhanam.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                Text("About 10 minutes a day · Missed a day? Every past day stays open · No sign-up: your progress is " +
                    "saved on this device", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
        },
        confirmButton = {
            if (progress.start == null) {
                PrimaryButton("Choose your Day 1 →", onClick = onClose)
            } else {
                PrimaryButton(if (inDay) "Continue to Day $target →" else "Begin Day $target →", onClick = onBegin)
            }
        },
        dismissButton = if (progress.start == null) null else {
            { TextButton(onClick = onClose) { Text(if (inDay) "Close" else "Show me the path first", color = AppColors.clayDark) } }
        },
    )
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.cream,
        modifier = Modifier.fillMaxWidth().background(AppColors.forest, RoundedCornerShape(12.dp)).padding(12.dp))
}
