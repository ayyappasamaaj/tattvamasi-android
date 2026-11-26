@file:OptIn(ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.events

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.housebhajan.HouseBhajanStore
import com.ayyappasamaaj.tattvamasi.ui.housebhajan.bhajanId
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The website's calendar colors (p-calendar.scss), one per kind. */
private fun CalendarItem.Kind.color(): Color = when (this) {
    CalendarItem.Kind.ONE -> Color(0xFFB4582B)
    CalendarItem.Kind.MULTI -> Color(0xFF2F6B3A)
    CalendarItem.Kind.HOUSE -> Color(0xFF6B3F7A)
    CalendarItem.Kind.ENTRY -> Color(0xFF4F6272)
}

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
private val dayTitle = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)

/**
 * The Calendar, as the website shows it on a phone: a compact month (a dot per item, a line under the days of an
 * event over several days) and the chosen day's items below. An event opens the app's event screen, a house bhajan
 * its page in the app (ui/housebhajan); another date opens its page on the website in the app's browser.
 */
@Composable
fun CalendarView(events: List<AppEvent>, onOpenEvent: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = calendarToday()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    fun show(to: YearMonth) {
        monthText = to.toString()
        // Today when it's in that month, else the 1st (as on the website)
        selectedText = (if (YearMonth.from(today) == to) today else to.atDay(1)).toString()
    }

    LaunchedEffect(monthText) { CalendarStore.load(month) }
    val loaded = CalendarStore.months.value[month]
    val items = eventItems(events) + (loaded?.items ?: emptyList())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().card(padding = 12.dp)) {
            // Month and its arrows
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    monthTitle.format(month),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.ink,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                        .semantics { heading(); liveRegion = LiveRegionMode.Polite },
                )
                if (month != YearMonth.from(today)) {
                    TextButton(onClick = { show(YearMonth.from(today)) }) { Text("Today", color = AppColors.clayDark) }
                }
                IconButton(onClick = { show(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month", tint = AppColors.ink)
                }
                IconButton(onClick = { show(month.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month", tint = AppColors.ink)
                }
            }
            // S M T W T F S
            Row(Modifier.fillMaxWidth().padding(top = 4.dp).clearAndSetSemantics {}) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, textAlign = TextAlign.Center, color = AppColors.muted, fontSize = 13.sp,
                        fontWeight = FontWeight(600), modifier = Modifier.weight(1f))
                }
            }
            monthDays(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f)) {
                            if (day != null) {
                                DayCell(day, itemsOn(items, day), day == selected, day == today) {
                                    selectedText = day.toString()
                                }
                            }
                        }
                    }
                }
            }
            Legend(Modifier.padding(top = 8.dp, start = 4.dp))
        }

        // The chosen day
        Text(
            dayTitle.format(selected),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.ink,
            modifier = Modifier.padding(top = 4.dp).semantics { heading(); liveRegion = LiveRegionMode.Polite },
        )
        val here = itemsOn(items, selected)
        Column(Modifier.fillMaxWidth().card(padding = 0.dp)) {
            if (here.isEmpty()) {
                Text("Nothing on this day.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.padding(16.dp))
            }
            here.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                ItemRow(item) {
                    when {
                        item.eventKey != null -> onOpenEvent(item.eventKey)
                        // A house bhajan's page, in the app (ui/housebhajan)
                        item.kind == CalendarItem.Kind.HOUSE && item.url != null && bhajanId(item.url) != null ->
                            scope.launch { HouseBhajanStore.openDetail(item.url) }
                        item.url != null -> openWebPage(context, item.url)
                    }
                }
            }
        }
        if (loaded?.failed == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Couldn't load the house bhajans and other dates for this month.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { CalendarStore.load(month, again = true) } }) {
                    Text("Try again", color = AppColors.clayDark)
                }
            }
        }

        // Host a house bhajan: the app's form (ui/housebhajan; sign in first), and a coordinator approves it
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().card()) {
            Text("Hosting an Ayyappa puja and bhajan at home?", style = MaterialTheme.typography.titleMedium,
                color = AppColors.ink)
            Text("Add it to the calendar: sign in, fill in the form, and a coordinator approves it first.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { scope.launch { HouseBhajanStore.openForm() } },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
                ) { Text("Add your house bhajan", fontWeight = FontWeight(700)) }
                TextButton(onClick = { scope.launch { HouseBhajanStore.openMine() } }) {
                    Text("My house bhajans", color = AppColors.clayDark)
                }
            }
        }

        // The whole calendar in the phone's own calendar app, kept up to date
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().card()) {
            Text("Add to my calendar", style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
            Text("Every event and house bhajan in your calendar app, updated by itself.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { openInBrowser(context, GOOGLE_SUBSCRIBE_URL) }) {
                    Icon(Icons.Outlined.EventAvailable, null, tint = AppColors.clay, modifier = Modifier.size(18.dp))
                    Text("Google Calendar", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { copyFeed(context) }) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = AppColors.clay, modifier = Modifier.size(18.dp))
                    Text("Copy the link", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, here: List<CalendarItem>, selected: Boolean, today: Boolean, choose: () -> Unit) {
    val count = here.size
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = choose)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = dayTitle.format(day) + (if (today) ", today" else "") +
                    (if (count > 0) ", $count ${if (count == 1) "item" else "items"}" else "")
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) AppColors.ink else Color.Transparent),
        ) {
            Text(
                day.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight(if (today || selected) 700 else 500),
                color = when {
                    selected -> AppColors.card  // on the ink circle: white, or dark on dark mode's light ink
                    today -> AppColors.clay
                    else -> AppColors.ink
                },
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        // A line for an event over several days, then a dot for each other item (up to 3)
        val multi = here.any { it.kind == CalendarItem.Kind.MULTI }
        Box(Modifier.padding(top = 3.dp).width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
            .background(if (multi) CalendarItem.Kind.MULTI.color() else Color.Transparent))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 3.dp).height(6.dp)) {
            here.filter { it.kind != CalendarItem.Kind.MULTI }.take(3).forEach {
                Box(Modifier.size(6.dp).clip(CircleShape).background(it.kind.color()))
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier) {
        listOf(
            CalendarItem.Kind.ONE to "Event",
            CalendarItem.Kind.MULTI to "Over several days",
            CalendarItem.Kind.HOUSE to "House bhajan",
            CalendarItem.Kind.ENTRY to "Other date",
        ).forEach { (kind, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(kind.color()))
                Text(label, fontSize = 13.sp, color = AppColors.muted, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun ItemRow(item: CalendarItem, open: () -> Unit) {
    val opens = item.eventKey != null || item.url != null
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (opens) Modifier.clickable(onClick = open, onClickLabel = if (item.eventKey != null) "Event details" else "Open on the website") else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(item.kind.color()))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.tag.isNotEmpty()) {
                    Text(item.tag, fontSize = 12.sp, fontWeight = FontWeight(700), color = Color.White,
                        modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(4.dp))
                            .background(item.kind.color()).padding(horizontal = 6.dp, vertical = 1.dp))
                }
                Text(item.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = AppColors.ink)
            }
            Text(item.whenText() + " · " + item.kindText(), style = MaterialTheme.typography.bodyMedium,
                color = AppColors.inkSoft)
            if (item.location.isNotBlank()) {
                Text(item.location, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted)
            }
        }
        if (opens) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AppColors.muted,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

private fun copyFeed(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Ayyappa Samaaj calendar", CALENDAR_FEED_URL))
    Toast.makeText(context, "Calendar link copied", Toast.LENGTH_SHORT).show()
}
