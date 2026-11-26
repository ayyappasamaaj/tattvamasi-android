package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * My account's volunteer hours (VolunteerHours.kt), instead of the website's dashboard card and /volunteers/add/ page:
 * a year's entries with the totals recorded and approved, "Add Hours" (the website form's rules) and "Print or Save
 * as PDF" (approved entries only, on the letterhead, as the dashboard prints it). The same as the iOS app's
 * VolunteerHoursView.swift.
 */

@Composable
private fun Busy() {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun Said(text: String, color: androidx.compose.ui.graphics.Color = AppColors.muted) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun FullButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
}

/** "Approved" on green, "Submitted" (waiting for a coordinator) on the soft clay, as the dashboard's badges. */
@Composable
private fun StatusBadge(entry: HoursEntry) {
    Surface(
        color = if (entry.approved) AppColors.forest else AppColors.claySoft,
        contentColor = if (entry.approved) AppColors.onForest else AppColors.clayDark,
        shape = RoundedCornerShape(50),
    ) {
        Text(entry.statusLabel.ifBlank { entry.status }, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

/** My account's card: this year's hours, opening the full page. Shown to volunteers (and anyone with hours). */
@Composable
fun HoursCard(hours: VolunteerHours) {
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("My volunteer hours")
        Text(hoursText(hours.recorded), style = MaterialTheme.typography.headlineMedium, color = AppColors.ink)
        Text("${hours.year} · Approved ${hoursText(hours.approved)}", style = MaterialTheme.typography.bodyMedium,
            color = AppColors.inkSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { AccountStore.openPage(AccountStore.Page.HOURS) }) {
                Text("See my hours", color = AppColors.clayDark)
            }
            if (hours.isVolunteer) {
                val scope = rememberCoroutineScope()
                TextButton(onClick = { scope.launch { AccountStore.openAddHours() } }) {
                    Text("Add hours", color = AppColors.clayDark)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerHoursPage(state: AccountStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AccountStore.loadHours() }  // fresh each time (a coordinator may have approved some)
    val h = state.hours
    if (h == null) {
        if (state.hoursFailed) Said("Your volunteer hours couldn't be loaded. Please try again later.", AppColors.clayDark)
        else Busy()
        return
    }
    state.hoursMessage?.let { Said(it, AppColors.forestInk) }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Year")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            h.years.forEach { year ->
                FilterChip(selected = year == h.year, onClick = { scope.launch { AccountStore.loadHours(year) } },
                    label = { Text(year.toString()) }, enabled = !state.loading,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.claySoft,
                        selectedLabelColor = AppColors.clayDark, labelColor = AppColors.ink))
            }
        }
        if (h.isVolunteer) {
            FullButton("Add Hours") { scope.launch { AccountStore.openAddHours() } }
        }
        if (state.hoursFailed) Said("That year couldn't be loaded. Please try again.", AppColors.clayDark)
    }
    if (state.loading) Busy()

    Column(Modifier.fillMaxWidth().card(16.dp)) {
        Text("${h.year} volunteer hours of ${h.personName}", style = MaterialTheme.typography.titleMedium,
            color = AppColors.ink)
        if (h.personEmail.isNotBlank()) Text(h.personEmail, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        if (h.entries.isEmpty()) {
            Said("No hours recorded in ${h.year}.")
        } else {
            Text("Recorded ${hoursText(h.recorded)} · Approved ${hoursText(h.approved)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)), color = AppColors.ink,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            h.entries.forEach { e ->
                HorizontalDivider(color = AppColors.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(e.activity, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                        Text("${summaryDay(e.date)} · ${clockTime(e.start)}–${clockTime(e.end)}",
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                        if (e.coordinator.isNotBlank()) {
                            Text("With ${e.coordinator}", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 12.dp)) {
                        Text(hoursText(e.hours), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                            color = AppColors.ink)
                        StatusBadge(e)
                    }
                }
            }
        }
        Text("Hours are approved by the volunteer coordinators. The PDF lists only approved hours.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.muted, modifier = Modifier.padding(top = 10.dp))
    }
    if (h.entries.isNotEmpty()) {
        OutlinedButton(onClick = { printLetter(context, "Ayyappa Samaaj ${h.year} volunteer hours", hoursLetterHtml(h, AccountApi.logoUrl)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Print or Save as PDF", color = AppColors.ink, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
    }
}

/** A list to pick from (times, coordinators), as the website's select boxes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Picker(label: String, choices: List<T>, chosen: T?, text: (T) -> String, error: String?,
                       onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = chosen?.let(text).orEmpty(), onValueChange = {}, readOnly = true, label = { Text(label) },
            placeholder = { Text("Choose") }, isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { c -> DropdownMenuItem(text = { Text(text(c)) }, onClick = { onPick(c); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHoursPage(state: AccountStore.State) {
    val scope = rememberCoroutineScope()
    val form = state.hoursForm
    if (form == null) {
        if (state.hoursFormFailed) Said("The form couldn't be loaded. Please try again later.", AppColors.clayDark)
        else Busy()
        return
    }
    val earliest = remember(form) { LocalDate.parse(form.earliest) }
    val latest = remember(form) { LocalDate.parse(form.latest) }
    var date by rememberSaveable { mutableStateOf(form.latest) }
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    var end by rememberSaveable { mutableStateOf<String?>(null) }
    var activity by rememberSaveable { mutableStateOf("") }
    var coordinator by rememberSaveable { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf(false) }
    val errors = state.hoursErrors
    val hours = hoursBetween(start, end)

    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("A day's volunteering: when, what you did and the coordinator you worked with. The coordinator " +
            "approves it.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        OutlinedTextField(
            value = summaryDay(date), onValueChange = {}, readOnly = true, label = { Text("Date") },
            isError = errors["date"] != null, supportingText = errors["date"]?.let { { Text(it) } },
            trailingIcon = { TextButton(onClick = { picking = true }) { Text("Change", color = AppColors.clayDark) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Picker("Start time", form.times, form.times.firstOrNull { it.value == start }, { it.label }, errors["start"]) {
            start = it.value
        }
        Picker("End time", form.times, form.times.firstOrNull { it.value == end }, { it.label }, errors["end"]) {
            end = it.value
        }
        if (start != null && end != null) {
            Said(hours?.let { "That's ${hoursText(it)}." } ?: "The end time must be after the start time.",
                if (hours == null) AppColors.clayDark else AppColors.inkSoft)
        }
        OutlinedTextField(
            value = activity, onValueChange = { activity = it.take(255) }, label = { Text("What you did") },
            isError = errors["activity"] != null, supportingText = errors["activity"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (form.coordinators.isNotEmpty()) {
            Picker("Volunteer coordinator", form.coordinators, form.coordinators.firstOrNull { it.id == coordinator },
                { it.name }, errors["coordinator"]) { coordinator = it.id }
        }
        errors[""]?.let { Said(it, AppColors.clayDark) }
        val ready = hours != null && activity.isNotBlank() && (!form.coordinatorRequired || coordinator != null)
        FullButton(if (state.busy) "Adding…" else hours?.let { "Add ${hoursText(it)}" } ?: "Add Hours",
            enabled = ready && !state.busy) {
            scope.launch { AccountStore.addHours(date, start!!, end!!, activity.trim(), coordinator) }
        }
    }

    if (picking) {
        val toMillis = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = toMillis(LocalDate.parse(date)),
            yearRange = earliest.year..latest.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !d.isBefore(earliest) && !d.isAfter(latest)
                }
                override fun isSelectableYear(year: Int) = year in earliest.year..latest.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text("OK", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel", color = AppColors.ink) } },
        ) { DatePicker(state = picker) }
    }
}
