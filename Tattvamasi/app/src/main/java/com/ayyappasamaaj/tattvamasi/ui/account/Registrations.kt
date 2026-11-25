package com.ayyappasamaaj.tattvamasi.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.addToCalendar
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * My registrations in the app, as the website's dashboard has them (NextEvent.vue, MyRegistrations.vue): "Your next
 * event" on My account, the registrations whose form is still open, every registration by year, and a page for each
 * (what was registered with each unit's name, what it came to and was paid, the answers given, the WhatsApp group,
 * Add to calendar). From the website's /api/signups/mine/. The same as the iOS app's Registrations.swift.
 */

private val pacific: ZoneId = ZoneId.of("America/Los_Angeles")

/** The soonest event still ahead that the person registered for (paid, free or being paid), as the dashboard picks. */
fun nextRegistration(list: List<Registration>): Registration? =
    list.filter { it.event?.upcoming == true }.minByOrNull { it.event!!.startDate }

/** The registrations on My account's card: those whose form is still open (the rest are under "past"). */
fun currentRegistrations(list: List<Registration>): List<Registration> = list.filter { it.formActive }

/** Past registrations (form closed), by year, newest year first. */
fun pastRegistrations(list: List<Registration>): List<Pair<Int, List<Registration>>> =
    list.filter { !it.formActive }.groupBy { it.year }.toList().sortedByDescending { it.first }

/** "2 × Kids Mala Dharana, Annadana" */
fun itemsText(r: Registration): String =
    r.items.joinToString(", ") { if (it.quantity > 1) "${it.quantity} × ${it.name}" else it.name }

/** "Sat, Nov 14 · 9:00 AM" or "Nov 14 – Dec 26" for several days. */
fun eventWhen(e: RegistrationEvent): String {
    val start = runCatching { LocalDate.parse(e.startDate) }.getOrNull() ?: return ""
    val end = runCatching { LocalDate.parse(e.endDate) }.getOrNull() ?: start
    if (end != start) {
        return start.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    val time = runCatching { LocalTime.parse(e.startTime).format(DateTimeFormatter.ofPattern("h:mm a")) }.getOrNull()
    return listOfNotNull(start.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")), time).joinToString(" · ")
}

/** The event as the app's calendar entry (Actions.kt addToCalendar). */
fun calendarEvent(r: Registration): AppEvent? {
    val e = r.event ?: return null
    val startDay = runCatching { LocalDate.parse(e.startDate) }.getOrNull() ?: return null
    val endDay = runCatching { LocalDate.parse(e.endDate) }.getOrNull() ?: startDay
    val startTime = runCatching { LocalTime.parse(e.startTime) }.getOrDefault(LocalTime.of(9, 0))
    val endTime = runCatching { LocalTime.parse(e.endTime) }.getOrDefault(startTime.plusHours(2))
    return AppEvent(
        id = r.code, name = e.name, summary = r.title, venue = e.venue,
        start = startDay.atTime(startTime).atZone(pacific).toInstant(),
        end = endDay.atTime(endTime).atZone(pacific).toInstant(),
        timeText = eventWhen(e), pageUrl = null, latitude = null, longitude = null,
    )
}

private fun money(amount: String?) = summaryDollars(amount).removeSuffix(".00")

@Composable
private fun RegistrationRow(r: Registration, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.weight(1f)) {
            Text(r.eventName ?: r.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                color = AppColors.ink)
            Text(listOfNotNull(r.event?.let { summaryDay(it.startDate) }?.ifBlank { null }, r.status,
                r.registeredBy?.let { "by $it" }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}

/** "Your next event", at the top of My account. */
@Composable
fun NextEventCard(r: Registration) {
    val e = r.event ?: return
    Column(Modifier.fillMaxWidth().card(16.dp).clickable(role = Role.Button) { AccountStore.openRegistration(r.code) },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel("Your next event")
        Text(e.name, style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
        Text(listOf(eventWhen(e), e.venue).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
        Text(listOf(itemsText(r).ifBlank { "Registered" }, r.status).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text("See your registration", style = MaterialTheme.typography.labelLarge, color = AppColors.clayDark,
            modifier = Modifier.padding(top = 4.dp))
    }
}

/** My account's card: the registrations whose form is open, and the way to all of them. */
@Composable
fun RegistrationsCard(list: List<Registration>) {
    val current = currentRegistrations(list)
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        SectionLabel("My registrations", Modifier.padding(bottom = 6.dp))
        if (current.isEmpty()) {
            Text(if (list.isEmpty()) "No registrations yet." else "None open right now.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        current.take(5).forEachIndexed { index, r ->
            if (index > 0) HorizontalDivider(color = AppColors.line)
            RegistrationRow(r) { AccountStore.openRegistration(r.code) }
        }
        if (list.isNotEmpty()) {
            androidx.compose.material3.TextButton(onClick = { AccountStore.openPage(AccountStore.Page.REGISTRATIONS) }) {
                Text(if (list.size > current.take(5).size) "See all my registrations" else "See past registrations",
                    color = AppColors.clayDark)
            }
        }
    }
}

/** Every registration: those open now, then the past ones by year. */
@Composable
fun RegistrationsListPage(state: AccountStore.State) {
    val list = state.overview?.registrations.orEmpty()
    val current = currentRegistrations(list)
    if (current.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Open now", Modifier.padding(bottom = 6.dp))
            current.forEachIndexed { index, r ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                RegistrationRow(r) { AccountStore.openRegistration(r.code) }
            }
        }
    }
    pastRegistrations(list).forEach { (year, items) ->
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Past registrations · $year", Modifier.padding(bottom = 6.dp))
            items.forEachIndexed { index, r ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                RegistrationRow(r) { AccountStore.openRegistration(r.code) }
            }
        }
    }
    if (list.isEmpty()) {
        Text("No registrations yet.", style = MaterialTheme.typography.bodyLarge, color = AppColors.muted)
    }
}

/** One registration, as its thank-you page and confirmation email have it. */
@Composable
fun RegistrationPage(state: AccountStore.State) {
    val context = LocalContext.current
    val r = state.overview?.registrations?.firstOrNull { it.code == state.registration } ?: return
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(r.eventName ?: r.title, style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
        r.event?.let { e ->
            Text(listOf(eventWhen(e), e.venue).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        }
        if (r.eventName != null && r.eventName != r.title) {
            Text(r.title, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        Text(listOfNotNull(r.status, "Registration ${r.code}", summaryDay(r.registeredAt).ifBlank { null }?.let { "made $it" },
            r.registeredBy?.let { "by $it" }).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        if (r.event?.upcoming == true) {
            OutlinedButton(onClick = { calendarEvent(r)?.let { addToCalendar(context, it) } },
                modifier = Modifier.padding(top = 4.dp)) {
                Text("Add to Calendar", color = AppColors.ink)
            }
        }
    }

    if (r.items.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("What you registered for", Modifier.padding(bottom = 4.dp))
            r.items.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(if (line.quantity > 1) "${line.quantity} × ${line.name}" else line.name,
                            style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                        if (line.details.isNotEmpty()) {
                            Text(line.details.joinToString(", "), style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.muted)
                        }
                    }
                    if ((line.amount.toBigDecimalOrNull()?.signum() ?: 0) > 0) {
                        Text(money(line.amount), style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                    }
                }
            }
            HorizontalDivider(color = AppColors.lineStrong)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).semantics(mergeDescendants = true) {}) {
                Text("Total", style = MaterialTheme.typography.titleMedium, color = AppColors.ink, modifier = Modifier.weight(1f))
                Text(money(r.total), style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
            }
            r.amountPaid?.takeIf { it != r.total }?.let {
                Text("Paid ${money(it)}", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
            if (r.state == "pending") {
                Text("Payment pending: if you didn't finish paying, register again from the event.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    if (r.answers.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Your answers")
            r.answers.forEach { (question, answer) ->
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(question, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                    Text(answer, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                }
            }
        }
    }

    if (r.whatsappUrl.isNotBlank()) {
        Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("WhatsApp group")
            Button(
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.whatsappUrl))) } },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Join the WhatsApp Group", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
            if (r.whatsappNote.isNotBlank()) {
                Text(r.whatsappNote, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
            }
        }
    }
}
