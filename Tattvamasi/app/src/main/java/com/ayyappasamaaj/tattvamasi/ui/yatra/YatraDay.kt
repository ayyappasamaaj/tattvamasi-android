@file:OptIn(ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.yatra

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.LocalDate

/** A day: its steps across the top, the step, and the buttons to move on at the bottom. */
@Composable
internal fun DayView(
    yatra: Yatra,
    progress: Progress,
    today: LocalDate,
    day: Int,
    stepIndex: Int,
    onStep: (Int) -> Unit,
    onPath: () -> Unit,
    onDay: (Int) -> Unit,
) {
    val date = yatra.dateOf(day, progress, today)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Day $day · ${yatra.stageName(day)}", style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
            Text(shortDate(date), style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
        }
        when {
            !yatra.isOpen(day, progress, today) -> Centered {
                Text("Day $day opens on ${shortDate(date)}", style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.ink, textAlign = TextAlign.Center)
                Text("One day at a time.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                PrimaryButton("Back to the path", onClick = onPath)
            }
            day == yatra.reciteDay -> ReciteDay(yatra, progress, day, onPath)
            day == yatra.finalDay -> FinalDay(yatra, progress, day, onPath)
            else -> {
                val steps = yatra.steps(day)
                val i = stepIndex.coerceIn(0, steps.size - 1)
                Stepper(yatra, progress, day, steps, i, onStep)
                HorizontalDivider(color = AppColors.line)
                var selectedName by rememberSaveable(day, i) { mutableStateOf<Int?>(null) }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val step = steps[i]) {
                        is Step.VerseStep -> VerseStep(yatra, day, i, step.verse, step.verse in progress.chanted, selectedName) { selectedName = it }
                        Step.NameOfDay -> NameStep(yatra, day)
                        Step.Lamp -> LampStep(yatra, progress, today, day, onDay)
                    }
                }
                // The way on: back a step, and the step's main button
                val previous = steps.getOrNull(i - 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.card)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (previous != null) {
                        TextButton(onClick = { onStep(i - 1) }) {
                            Text("← " + when (previous) {
                                is Step.VerseStep -> "Verse ${previous.verse}"
                                Step.NameOfDay -> "Name of the day"
                                Step.Lamp -> "Lamp"
                            }, color = AppColors.clayDark)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    when (val step = steps[i]) {
                        is Step.VerseStep -> {
                            val next = steps[i + 1]
                            val nextLabel = when (next) {
                                is Step.VerseStep -> "next verse"
                                Step.NameOfDay -> "Name of the day"
                                Step.Lamp -> "light the lamp"
                            }
                            val chanted = step.verse in progress.chanted
                            PrimaryButton(if (chanted) "Next: $nextLabel" else "✓ Chanted · $nextLabel") {
                                if (!chanted) YatraStore.update { it.copy(chanted = it.chanted + step.verse) }
                                onStep(i + 1)
                            }
                        }
                        Step.NameOfDay -> {
                            val missing = yatra.versesOfDay(day).filter { it !in progress.chanted }
                            if (missing.isNotEmpty()) {
                                PrimaryButton("Mark verse ${missing.first()} first") {
                                    onStep(yatra.versesOfDay(day).indexOf(missing.first()))
                                }
                            } else {
                                PrimaryButton("Light my lamp", lamp = true) { onStep(steps.size - 1) }
                            }
                        }
                        Step.Lamp -> PrimaryButton("Back to the path", onClick = onPath)
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) { content() }
}

/** The day's steps: each verse, the Name of the day, the lamp. */
@Composable
private fun Stepper(yatra: Yatra, progress: Progress, day: Int, steps: List<Step>, current: Int, onStep: (Int) -> Unit) {
    val done = yatra.dayDone(day, progress)
    val scroll = rememberScrollState()
    // Keeps the current step in view (a phone shows about three)
    LaunchedEffect(current, scroll.maxValue) { scroll.animateScrollTo(current * scroll.maxValue / (steps.size - 1).coerceAtLeast(1)) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = "Today’s steps" },
    ) {
        steps.forEachIndexed { i, step ->
            val now = i == current
            val stepDone = when (step) {
                is Step.VerseStep -> step.verse in progress.chanted
                else -> done && current > i
            }
            val label = when (step) {
                is Step.VerseStep -> "Verse ${step.verse}"
                Step.NameOfDay -> "Name"
                Step.Lamp -> "Lamp"
            }
            val sub = when (step) {
                is Step.VerseStep -> "${yatra.namesOfVerse(step.verse).size} names"
                Step.NameOfDay -> "of the day"
                Step.Lamp -> "Light it"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (now) AppColors.forest else if (stepDone) YatraColors.goldSoft else AppColors.card, RoundedCornerShape(50))
                    .border(1.dp, if (now) AppColors.forest else AppColors.line, RoundedCornerShape(50))
                    .clickable(role = Role.Tab, onClick = { onStep(i) })
                    .semantics(mergeDescendants = true) { selected = now }
                    .heightIn(min = 48.dp)
                    .padding(start = 6.dp, end = 14.dp),
            ) {
                Box(
                    Modifier.size(30.dp).background(if (now) AppColors.saffron else if (stepDone) YatraColors.gold else AppColors.claySoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        stepDone && !now -> Icon(Icons.Outlined.Check, contentDescription = "done", tint = AppColors.card, modifier = Modifier.size(16.dp))
                        step is Step.VerseStep -> Text("${step.verse}", style = MaterialTheme.typography.labelMedium, color = AppColors.forest)
                        step is Step.NameOfDay -> Text("★", color = AppColors.forest)
                        else -> Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null, tint = AppColors.forest, modifier = Modifier.size(16.dp))
                    }
                }
                Column {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (now) AppColors.cream else AppColors.ink)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = if (now) AppColors.cream.copy(alpha = 0.8f) else AppColors.muted)
                }
            }
        }
    }
}

/** A part that opens and closes, like the website's <details>. */
@Composable
private fun Expandable(
    title: String,
    initiallyOpen: Boolean = false,
    key: Any = title,
    content: @Composable () -> Unit,
) {
    var open by rememberSaveable(key) { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth().card(0.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClickLabel = if (open) "Close" else "Open") { open = !open }
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppColors.ink, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = AppColors.clay)
        }
        if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Tag(text: String, note: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.card,
            modifier = Modifier.background(AppColors.clay, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
    }
}

/** The verse with each name highlighted (tap one to see it in the Namavali), its number raised after it. */
private fun markedVerse(verse: Verse, selected: Int?, onSelect: (Int) -> Unit): AnnotatedString = buildAnnotatedString {
    var at = 0
    verse.marks.forEach { m ->
        if (m.start < at) return@forEach
        append(verse.text.substring(at, m.start))
        val on = m.n == selected
        val style = SpanStyle(
            background = if (on) AppColors.saffron.copy(alpha = 0.55f) else YatraColors.goldSoft,
            color = if (on) AppColors.forest else YatraColors.gold,
        )
        withLink(LinkAnnotation.Clickable("name-${m.n}", TextLinkStyles(style)) { onSelect(m.n) }) {
            append(verse.text.substring(m.start, m.end))
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 11.sp)) { append("${m.n}") }
        }
        at = m.end
    }
    append(verse.text.substring(at))
}

/** A verse step: the Stotram (the verse, its names highlighted) and the Namavali (its names, one at a time). */
@Composable
private fun VerseStep(
    yatra: Yatra,
    day: Int,
    index: Int,
    verseNumber: Int,
    chanted: Boolean,
    selectedName: Int?,
    onSelect: (Int) -> Unit,
) {
    val data = yatra.data
    val verse = data.verses[verseNumber] ?: return
    val names = yatra.namesOfVerse(verseNumber)
    if (names.isEmpty()) return
    val selected = selectedName?.takeIf { it in names } ?: names.first()
    val stotram = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().card()) {
            Tag("Stotram", "The hymn: chant the whole verse")
            Eyebrow("Verse $verseNumber of ${yatra.totalVerses}" + if (chanted) " · chanted" else "")
            Text(markedVerse(verse, selected, onSelect), fontFamily = ScriptFont, fontSize = 22.sp, lineHeight = 38.sp,
                color = AppColors.ink)
            if (verse.tr.isNotBlank()) {
                Text(verse.tr, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic), color = AppColors.inkSoft)
            }
            Text("The highlighted words are the names. Tap one to see it in the Namavali.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
            Text("→ This verse holds ${names.size} names (${names.first()}–${names.last()}). Chant each one on its own in the Namavali.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark)
        }
    }
    val namavali = @Composable { Namavali(yatra, day, verseNumber, names, selected, onSelect) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Expandable("Stotram and Namavali: how they fit together", initiallyOpen = day == 1 && index == 0, key = "sn-$day-$index") {
                Text("The Stotram is the hymn: ${yatra.totalVerses} verses that string the names of Sri Dharma Shastha " +
                    "together. The Namavali is the same 1,000 names chanted one at a time, each as “Om … namaḥ” " +
                    "(salutations to …).", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                Text("Verse $verseNumber holds names ${names.first()}–${names.last()}. Chant the verse, then chant its " +
                    "names one by one.", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            }
            if (index == 0 && (data.nyasa.isNotBlank() || data.dhyanam.isNotBlank())) {
                Expandable("Begin with the nyāsa and dhyānam", key = "nyasa-$day") {
                    Text(data.nyasa, fontFamily = ScriptFont, fontSize = 17.sp, lineHeight = 28.sp, color = AppColors.inkSoft)
                    Text(data.dhyanam, fontFamily = ScriptFont, fontSize = 20.sp, lineHeight = 32.sp, color = YatraColors.gold)
                    Text(data.dhyanamMeaning, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { stotram() }
                    Box(Modifier.weight(1f)) { namavali() }
                }
            } else {
                ReadableWidth { stotram() }
                ReadableWidth { namavali() }
            }
        }
    }
}

/** The names of a verse, one at a time: chosen with the numbers above, or the arrows below. */
@Composable
private fun Namavali(yatra: Yatra, day: Int, verseNumber: Int, names: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    val context = LocalContext.current
    val data = yatra.data
    val name = data.names[selected] ?: return
    val k = names.indexOf(selected)
    val nameOfDay = yatra.nameOfDay(day)?.n
    val verse = data.verses[verseNumber]
    val mark = verse?.marks?.firstOrNull { it.n == selected }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().card()) {
        Tag("Namavali", "The same names, one at a time")
        Row(verticalAlignment = Alignment.Bottom) {
            Text("The ${names.size} names of verse $verseNumber", style = MaterialTheme.typography.titleLarge,
                color = AppColors.ink, modifier = Modifier.weight(1f).semantics { heading() })
            Text("Name ${k + 1} of ${names.size}", style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEach { n ->
                val on = n == selected
                val star = n == nameOfDay
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .widthIn(min = 52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) AppColors.forest else if (star) YatraColors.goldSoft else AppColors.cream, RoundedCornerShape(50))
                        .border(1.dp, if (on) AppColors.forest else AppColors.line, RoundedCornerShape(50))
                        .clickable(role = Role.Tab) { onSelect(n) }
                        .semantics {
                            this.selected = on
                            contentDescription = "Name $n" + if (star) ", name of the day" else ""
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Text("$n" + if (star) " ★" else "", style = MaterialTheme.typography.labelLarge,
                        color = if (on) AppColors.cream else AppColors.ink)
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().background(AppColors.cream, RoundedCornerShape(12.dp)).padding(14.dp),
        ) {
            Eyebrow("Namavali · name #$selected" + if (selected == nameOfDay) " · ★ Name of the day" else "")
            if (verse != null && mark != null) {
                Text(buildAnnotatedString {
                    append("In the verse: ")
                    withStyle(SpanStyle(fontFamily = ScriptFont)) { append(verse.text.substring(mark.start, mark.end)) }
                    append(" · on its own:")
                }, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
            }
            NameBlock(yatra, name)
            HorizontalDivider(color = AppColors.line, modifier = Modifier.padding(vertical = 4.dp))
            Text("How the name is built", style = MaterialTheme.typography.titleSmall, color = AppColors.ink)
            Roots(name)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (k > 0) {
                    OutlinedButton(onClick = { onSelect(names[k - 1]) }) { Text("← #${names[k - 1]}", color = AppColors.ink) }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { shareName(context, yatra, name, day) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share this name", tint = AppColors.clayDark)
                }
                Spacer(Modifier.weight(1f))
                if (k < names.size - 1) {
                    OutlinedButton(onClick = { onSelect(names[k + 1]) }) { Text("#${names[k + 1]} →", color = AppColors.ink) }
                } else {
                    Text("Last name", style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
                }
            }
        }
    }
}

/** "Om … namaḥ" in the script and transliterated, and the name's meaning. */
@Composable
private fun NameBlock(yatra: Yatra, name: Name, large: Boolean = false) {
    val data = yatra.data
    Text("${data.om} ${name.d} ${data.namah}", fontFamily = ScriptFont, fontSize = if (large) 30.sp else 26.sp,
        lineHeight = if (large) 44.sp else 38.sp, color = AppColors.clay)
    Text("Om ${name.t} namaḥ", style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
    Text("“${name.m}”", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = AppColors.inkSoft)
    val language = data.language
    if (language != null && !name.meaningReviewed) {
        Text("${language.english} meaning coming soon; shown in English", style = MaterialTheme.typography.bodySmall,
            color = AppColors.muted)
    }
}

/** The root words, each with its transliteration and meaning, joined by "+"; other readings after "also:". */
@Composable
private fun Roots(name: Name) {
    val parts = name.parts.filter { it.meaning.isNotBlank() }
    val alternates = name.parts.filter { it.meaning.isBlank() }
    if (parts.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics { contentDescription = "Root words" },
        ) {
            parts.forEachIndexed { i, part ->
                if (i > 0) Text("+", style = MaterialTheme.typography.titleMedium, color = AppColors.muted,
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics { })
                Column(Modifier.background(AppColors.card, RoundedCornerShape(10.dp))
                    .border(1.dp, AppColors.line, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(part.word, fontFamily = ScriptFont, fontSize = 18.sp, color = AppColors.clay)
                    Text(part.tr, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = AppColors.inkSoft)
                    Text(part.meaning, style = MaterialTheme.typography.bodySmall, color = AppColors.ink)
                }
            }
        }
    }
    if (alternates.isNotEmpty()) {
        Text("also: " + alternates.joinToString("; ") { it.word }, fontFamily = ScriptFont,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = AppColors.muted)
    }
    if (name.altReading.isNotBlank()) {
        Text("Root words explain the reading ${name.altReading} [to be reconciled]",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = AppColors.muted)
    }
}

/** The Name of the day: the name, how it's built, and the day's reflection. */
@Composable
private fun NameStep(yatra: Yatra, day: Int) {
    val context = LocalContext.current
    val name = yatra.nameOfDay(day) ?: return
    val verse = yatra.verseOfName(name.n)
    val reflection = yatra.feed.reflections[day]
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val left = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().card()) {
                Eyebrow("Name of the day · #${name.n}" + if (verse != null) " · from verse $verse" else "", color = AppColors.clay)
                NameBlock(yatra, name, large = true)
                OutlinedButton(onClick = { shareName(context, yatra, name, day) }) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = AppColors.clayDark, modifier = Modifier.size(18.dp))
                    Text("  Share this name", color = AppColors.clayDark)
                }
            }
        }
        val right = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().card()) {
                Text("How the name is built", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })
                Roots(name)
                Text("→ ${name.m}", style = MaterialTheme.typography.bodyLarge, color = AppColors.clayDark)
                if (!reflection?.about.isNullOrBlank()) {
                    Text("About this name", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                        modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                    Text(reflection!!.about, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                }
                if (!reflection?.carry.isNullOrBlank()) {
                    Text("Carry it today", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                        modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                    Text(reflection!!.carry, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { left() }
                    Box(Modifier.weight(1f)) { right() }
                }
            } else {
                ReadableWidth { left() }
                ReadableWidth { right() }
            }
        }
    }
}

/** The lamp: lit once every verse of the day is chanted; then the stage, tomorrow, a quick check and a reflection. */
@Composable
private fun LampStep(yatra: Yatra, progress: Progress, today: LocalDate, day: Int, onDay: (Int) -> Unit) {
    val context = LocalContext.current
    val verses = yatra.versesOfDay(day)
    val done = yatra.dayDone(day, progress)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        ReadableWidth(640.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!done) {
                    val missing = verses.filter { it !in progress.chanted }
                    Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null, tint = AppColors.muted,
                        modifier = Modifier.size(56.dp))
                    Text("Almost there", style = MaterialTheme.typography.headlineMedium, color = AppColors.ink,
                        modifier = Modifier.semantics { heading() })
                    Text("Mark verse" + (if (missing.size > 1) "s " else " ") + missing.joinToString(", ") +
                        " as chanted to light today’s lamp.", style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.inkSoft, textAlign = TextAlign.Center)
                    Text("Use the steps above to go back to a verse.", style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.muted)
                    return@Column
                }
                val streak = yatra.streak(progress, today)
                Image(painterResource(R.drawable.ic_yatra_lamp_lit), contentDescription = "Lit lamp", modifier = Modifier.size(64.dp))
                Text("Your lamp is lit", style = MaterialTheme.typography.headlineMedium, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })
                Text("${verses.size} verses and ${yatra.namesOfDay(day).size} names chanted. " +
                    if (streak > 1) "That’s $streak days in a row." else "",
                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft, textAlign = TextAlign.Center)

                // The stage, with its four stones
                val stage = yatra.stageIndex(day)
                val days = yatra.daysOfStage(stage)
                val current = yatra.currentDay(progress, today)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().card()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(yatra.stages[stage], style = MaterialTheme.typography.titleLarge, color = AppColors.clay,
                            modifier = Modifier.weight(1f))
                        Text("Stage ${stage + 1} of ${yatra.stages.size}", style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.forEach { d -> Stone(yatra, progress, today, d, current, Modifier.weight(1f)) { onDay(d) } }
                    }
                    if (stage < yatra.stages.size - 1) {
                        val left = days.last - day
                        Text(
                            if (left == 0) "Tomorrow you reach ${yatra.stages[stage + 1]}."
                            else "$left more day${if (left > 1) "s" else ""} and you reach ${yatra.stages[stage + 1]}.",
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                        )
                    }
                }
                // Tomorrow
                val next = day + 1
                if (next <= yatra.finalDay) {
                    Column(Modifier.fillMaxWidth().background(YatraColors.goldSoft, RoundedCornerShape(16.dp))
                        .border(1.dp, AppColors.saffron, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Eyebrow("Tomorrow · Day $next", color = AppColors.clay)
                        Text(yatra.teaser(next), style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                    }
                }
                yatra.quiz(day)?.let { QuickCheck(yatra, it) }
                Reflection(day, progress) { text ->
                    YatraStore.update { it.copy(journal = it.journal + (day to text)) }
                    toast(context, "Saved on this device")
                }
            }
        }
    }
}

@Composable
private fun QuickCheck(yatra: Yatra, quiz: Quiz) {
    var answer by rememberSaveable(quiz.name.n) { mutableStateOf<Int?>(null) }
    Expandable("Quick check · 30 seconds", key = "quiz-${quiz.name.n}") {
        Text(buildAnnotatedString {
            append("What does ")
            withStyle(SpanStyle(fontFamily = ScriptFont, color = AppColors.clay)) { append(quiz.name.d) }
            append(" mean?")
        }, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = AppColors.ink)
        quiz.options.forEachIndexed { i, option ->
            val right = answer != null && i == quiz.correct
            val wrong = answer == i && i != quiz.correct
            OutlinedButton(
                onClick = { answer = i },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = when {
                    right -> YatraColors.forestSoft
                    wrong -> AppColors.claySoft
                    else -> AppColors.card
                }),
                border = BorderStroke(if (right || wrong) 2.dp else 1.dp,
                    when { right -> AppColors.forest; wrong -> AppColors.clay; else -> AppColors.line }),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(option, color = when { right -> AppColors.forest; wrong -> AppColors.clayDark; else -> AppColors.ink },
                    fontWeight = if (right) FontWeight(700) else FontWeight(400), modifier = Modifier.fillMaxWidth())
            }
        }
        answer?.let {
            Text(if (it == quiz.correct) "Right." else "Not quite. It means “${quiz.name.m}.”",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight(600),
                color = if (it == quiz.correct) AppColors.forest else AppColors.clayDark,
                modifier = Modifier.semantics { this.contentDescription = if (it == quiz.correct) "Right." else "Not quite. It means ${quiz.name.m}." })
        }
    }
}

/** A private note for the day, kept only on the phone. */
@Composable
private fun Reflection(day: Int, progress: Progress, onSave: (String) -> Unit) {
    var text by rememberSaveable(day) { mutableStateOf(progress.journal[day] ?: "") }
    Expandable("Write a reflection", initiallyOpen = !progress.journal[day].isNullOrBlank(), key = "journal-$day") {
        Text("Which name stayed with you today? Saved only on this device.", style = MaterialTheme.typography.bodyMedium,
            color = AppColors.muted)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reflection for day $day" },
        )
        OutlinedButton(onClick = { onSave(text) }) { Text("Save", color = AppColors.clayDark) }
    }
}

/** Day 40: the whole stotram, from the dhyānam to the last verse. */
@Composable
private fun ReciteDay(yatra: Yatra, progress: Progress, day: Int, onPath: () -> Unit) {
    val context = LocalContext.current
    val data = yatra.data
    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            ReadableWidth(860.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Eyebrow("Day $day · Full recitation")
                    Text("All ${yatra.totalVerses} verses/1000 names, in one sitting", style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.ink, modifier = Modifier.semantics { heading() })
                    Text("You have walked every step. Chant the whole stotram from the dhyānam to the last verse.",
                        style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                    Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(data.dhyanam, fontFamily = ScriptFont, fontSize = 20.sp, lineHeight = 32.sp, color = YatraColors.gold)
                        (1..yatra.totalVerses).forEach { v ->
                            val verse = data.verses[v] ?: return@forEach
                            HorizontalDivider(color = AppColors.line)
                            Text(verse.text, fontFamily = ScriptFont, fontSize = 21.sp, lineHeight = 34.sp, color = AppColors.ink)
                            if (verse.tr.isNotBlank()) {
                                Text(verse.tr, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    color = AppColors.inkSoft)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(AppColors.card).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End) {
            if (progress.recited) {
                PrimaryButton("✓ Recited · back to the path", onClick = onPath)
            } else {
                PrimaryButton("I have recited all ${yatra.totalVerses} verses/1000 names", lamp = true) {
                    YatraStore.update { it.copy(recited = true) }
                    toast(context, "All ${yatra.totalVerses} verses/1000 names recited. Swamiye Saranam.")
                }
            }
        }
    }
}

/** Day 41: the Pūrṇāhuti, chanted together at Sannidhanam. */
@Composable
private fun FinalDay(yatra: Yatra, progress: Progress, day: Int, onPath: () -> Unit) {
    val data = yatra.data
    val closing = data.names[yatra.feed.closingName]
    val puja = yatra.feed.mandalaPujaDate
    Column(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            Image(painterResource(R.drawable.ic_yatra_lamp_lit), contentDescription = null, modifier = Modifier.size(64.dp))
            Eyebrow("Day $day")
            Text("Pūrṇāhuti", style = MaterialTheme.typography.displaySmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
            Text("Join us at Sannidhanam" + (if (puja.isNotBlank()) " on $puja" else "") +
                " to chant all ${yatra.totalVerses} verses/1000 names together.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft, textAlign = TextAlign.Center)
            if (closing != null) {
                Text("${data.om} ${closing.d} ${data.namah}", fontFamily = ScriptFont, fontSize = 26.sp, lineHeight = 38.sp,
                    color = AppColors.clay, textAlign = TextAlign.Center)
                Text(closing.m, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted, textAlign = TextAlign.Center)
            }
            Text(
                "${yatra.versesDone(progress)} of ${yatra.totalVerses} verses and " +
                    "${"%,d".format(yatra.namesDone(progress))} of ${"%,d".format(yatra.totalNames)} names chanted on your yatra",
                style = MaterialTheme.typography.titleMedium, color = YatraColors.gold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().card(),
            )
            Text("Swamiye Saranam Ayyappa", style = MaterialTheme.typography.headlineSmall, color = YatraColors.gold)
        }
        Row(Modifier.fillMaxWidth().background(AppColors.card).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End) {
            PrimaryButton("Back to the path", onClick = onPath)
        }
    }
}
