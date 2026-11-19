package com.ayyappasamaaj.tattvamasi.ui.sabarimala

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Sabarimala Opening Dates: when the temple is open in the year, as the website's /guides/app.json sends it beside the
 * guides ("opening_dates"; website: pages/app_guides.py, from the Travancore Devaswom Board's calendar). Kept on the
 * phone with the guides, so it reads offline. Times are India time, as the Board publishes them.
 */

/** A dated moment within an opening: the temple opens, a festival day, the temple closes. */
data class KeyDate(val date: LocalDate, val time: String, val what: String, val role: String?)

/** One stretch the temple is open; [closes] is null until the Board publishes it. */
data class OpeningPeriod(
    val id: String,
    val kind: String,
    val title: String,
    val opens: LocalDate,
    val opensTime: String,
    val closes: LocalDate?,
    val closesTime: String?,
    val crowd: String?,
    val about: String,
    val keyDates: List<KeyDate>,
    val url: String,
    val calendarUrl: String,
) {
    /** Over by [today]: its closing day has passed (one with no closing date yet isn't). */
    fun isPast(today: LocalDate): Boolean = closes != null && closes.isBefore(today)
    fun isOpenOn(today: LocalDate): Boolean = !opens.isAfter(today) && (closes == null || !closes.isBefore(today))
}

data class OpeningDates(
    val name: String,
    val season: String,
    val url: String,
    val sourceName: String,
    val sourceUrl: String,
    val periods: List<OpeningPeriod>,
) {
    /** The period the temple is open in on [today], else the next to open, else null. */
    fun nowOrNext(today: LocalDate): Pair<OpeningPeriod, Boolean>? =
        periods.firstOrNull { it.isOpenOn(today) }?.let { it to true }
            ?: periods.firstOrNull { it.opens.isAfter(today) }?.let { it to false }
}

private fun date(text: String?): LocalDate? = text?.takeIf { it.isNotBlank() && it != "null" }
    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** The opening dates in the website's /guides/app.json, or null if it has none (an older copy) or they're unusable. */
fun parseOpeningDates(json: String): OpeningDates? = runCatching {
    val root = JSONObject(json).optJSONObject("opening_dates") ?: return@runCatching null
    val list = root.optJSONArray("periods") ?: return@runCatching null
    val periods = (0 until list.length()).mapNotNull { list.optJSONObject(it) }.mapNotNull { p ->
        val opens = date(p.optString("opens")) ?: return@mapNotNull null
        val keys = p.optJSONArray("key_dates")
        OpeningPeriod(
            id = p.optString("id"),
            kind = p.optString("kind"),
            title = p.optString("title"),
            opens = opens,
            opensTime = p.optString("opens_time"),
            closes = date(if (p.isNull("closes")) null else p.optString("closes")),
            closesTime = if (p.isNull("closes_time")) null else p.optString("closes_time").ifBlank { null },
            crowd = if (p.isNull("crowd")) null else p.optString("crowd").ifBlank { null },
            about = p.optString("about"),
            keyDates = (0 until (keys?.length() ?: 0)).mapNotNull { keys!!.optJSONObject(it) }.mapNotNull { k ->
                date(k.optString("date"))?.let {
                    KeyDate(it, k.optString("time"), k.optString("what"), if (k.isNull("role")) null else k.optString("role"))
                }
            },
            url = p.optString("url").takeIf { it.startsWith("https://") } ?: "",
            calendarUrl = p.optString("calendar").takeIf { it.startsWith("https://") } ?: "",
        ).takeIf { it.title.isNotBlank() }
    }.sortedBy { it.opens }
    if (periods.isEmpty()) return@runCatching null
    val source = root.optJSONObject("source")
    OpeningDates(
        name = root.optString("name").ifBlank { "Sabarimala Opening Dates" },
        season = root.optString("season"),
        url = root.optString("url").takeIf { it.startsWith("https://") } ?: "https://ayyappasamaaj.org/sabarimala-opening-dates/",
        sourceName = source?.optString("name").orEmpty().ifBlank { "Travancore Devaswom Board" },
        sourceUrl = source?.optString("url").orEmpty(),
        periods = periods,
    )
}.getOrNull()

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

/** "Mon, Nov 16, 5:00 PM" */
fun dayAndTime(date: LocalDate, time: String?): String = date.format(dayFormat) + (time?.let { ", $it" } ?: "")

/** "Opens Mon, Nov 16, 5:00 PM · Closes Sun, Dec 27, 10:00 PM" (India time) */
fun openingLine(p: OpeningPeriod): String =
    "Opens ${dayAndTime(p.opens, p.opensTime)} · " +
        (p.closes?.let { "Closes ${dayAndTime(it, p.closesTime)}" } ?: "Closing date not yet announced")

/** The calendar app's "new event" screen with the opening as all-day days; without a calendar app, the website's .ics. */
fun addToCalendar(context: Context, p: OpeningPeriod) {
    val utc = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val last = p.closes ?: p.opens
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, "Sabarimala open: ${p.title}")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, "Sabarimala, Keralam")
        .putExtra(CalendarContract.Events.DESCRIPTION, openingLine(p) + " (India time)." + (p.crowd?.let { " Expected crowd: $it." } ?: ""))
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, utc(p.opens))
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, utc(last.plusDays(1)))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        if (p.calendarUrl.isNotEmpty()) openWebPage(context, p.calendarUrl)
    }
}

private fun crowdColor(crowd: String?): Pair<Color, Color> = when (crowd) {
    "Quiet" -> Color(0xFFE5F4E4) to Color(0xFF1F5A2A)
    "Moderate" -> Color(0xFFF9EEDA) to Color(0xFF7A5200)
    "High" -> Color(0xFFF9ECE7) to AppColors.clayDark
    "Extreme" -> AppColors.clay to Color.White
    else -> AppColors.cream to AppColors.muted
}

@Composable
private fun CrowdChip(crowd: String?) {
    val (bg, fg) = crowdColor(crowd)
    Text(
        if (crowd != null) "Crowd: $crowd" else "Crowd not rated",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(600)),
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * The opening dates, in the app: what's open now (or next), then each opening from now on by month, each opening up
 * to what it is, its key dates, Add to calendar and its page on the website. Past openings are behind a button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpeningDatesScreen(dates: OpeningDates, onBack: () -> Unit, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    var showPast by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = dates.periods.filter { showPast || !it.isPast(today) }.ifEmpty { dates.periods }
    val past = dates.periods.count { it.isPast(today) }
    val months = shown.groupBy { it.opens.withDayOfMonth(1) }
    val current = dates.nowOrNext(today)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "top") {
            ReadableWidth {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
                    Text("Sabarimala", color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        item(key = "title") {
            ReadableWidth {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dates.name, style = MaterialTheme.typography.headlineMedium, color = AppColors.ink,
                        modifier = Modifier.semantics { heading() })
                    Text(
                        (if (dates.season.isNotBlank()) "${dates.season} season. " else "") +
                            "Times are India time, from the ${dates.sourceName}.",
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.muted,
                    )
                }
            }
        }
        if (current != null) {
            item(key = "now") {
                ReadableWidth {
                    val (p, now) = current
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.forest).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(if (now) "OPEN NOW" else "NEXT OPENING", color = AppColors.saffron,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(700), letterSpacing = 1.sp))
                        Text(p.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(openingLine(p), color = AppColors.cream, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        months.forEach { (month, periods) ->
            item(key = "month-$month") {
                ReadableWidth {
                    Text(month.format(monthFormat), style = MaterialTheme.typography.titleMedium, color = AppColors.inkSoft,
                        modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                }
            }
            items(periods, key = { it.id }) { p ->
                ReadableWidth {
                    PeriodCard(p, open == p.id, p.isPast(today), onToggle = { open = if (open == p.id) null else p.id },
                        onCalendar = { addToCalendar(context, p) }, onPage = { if (p.url.isNotEmpty()) openWebPage(context, p.url) })
                }
            }
        }
        if (past > 0) {
            item(key = "past") {
                ReadableWidth {
                    TextButton(onClick = { showPast = !showPast }) {
                        Text(if (showPast) "Hide past openings" else "Show past openings ($past)", color = AppColors.clayDark)
                    }
                }
            }
        }
        item(key = "source") {
            ReadableWidth {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.muted)
                    Text(
                        "Dates can change: check the ${dates.sourceName}'s calendar before you travel.",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.muted, modifier = Modifier.weight(1f),
                    )
                }
                if (dates.sourceUrl.isNotEmpty() || dates.url.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dates.sourceUrl.startsWith("https://")) {
                            TextButton(onClick = { openWebPage(context, dates.sourceUrl) }) {
                                Text("Devaswom Board calendar", color = AppColors.clayDark)
                            }
                        }
                        TextButton(onClick = { openWebPage(context, dates.url) }) {
                            Text("On ayyappasamaaj.org", color = AppColors.clayDark)
                        }
                    }
                }
            }
        }
        item(key = "footer") { ReadableWidth { AddressFooter() } }
    }
}

/** One opening: what, when and the crowd; tap for what it is, its key dates, Add to calendar and its page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodCard(
    p: OpeningPeriod,
    expanded: Boolean,
    past: Boolean,
    onToggle: () -> Unit,
    onCalendar: () -> Unit,
    onPage: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(AppColors.card).border(1.dp, AppColors.line, shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Close" else "Open", onClick = onToggle)
                .padding(14.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(p.title, style = MaterialTheme.typography.titleMedium,
                    color = if (past) AppColors.muted else AppColors.ink)
                Text(openingLine(p), style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                CrowdChip(p.crowd)
            }
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null,
                tint = AppColors.muted)
        }
        if (expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(p.about, style = MaterialTheme.typography.bodyMedium, color = AppColors.ink)
                if (p.keyDates.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Key dates", style = MaterialTheme.typography.labelLarge, color = AppColors.inkSoft)
                        p.keyDates.forEach { k ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(dayAndTime(k.date, k.time), style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.clayDark, modifier = Modifier.padding(top = 1.dp))
                                Text(k.what, style = MaterialTheme.typography.bodySmall, color = AppColors.ink,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!past) {
                        OutlinedButton(onClick = onCalendar) {
                            Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = AppColors.clayDark)
                            Spacer(Modifier.padding(start = 6.dp))
                            Text("Add to calendar", color = AppColors.clayDark)
                        }
                    }
                    if (p.url.isNotEmpty()) {
                        TextButton(onClick = onPage) { Text("More on the website", color = AppColors.clayDark) }
                    }
                }
            }
        }
    }
}
