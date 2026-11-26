package com.ayyappasamaaj.tattvamasi.ui.housebhajan

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * House bhajans over any tab (HouseBhajanStore), instead of the website's pages: a bhajan's page (from the calendar),
 * the form to host one or change one, and My house bhajans (from My Account and the calendar). As the iOS app's
 * HouseBhajanViews.swift.
 */

@Composable
fun HouseBhajanSheet() {
    val state by HouseBhajanStore.state.collectAsState()
    val page = state.page ?: return
    BackHandler { HouseBhajanStore.back() }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = { HouseBhajanStore.back() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text(when (page) {
                HouseBhajanStore.Page.DETAIL -> "House Bhajan"
                HouseBhajanStore.Page.FORM -> if (state.editing == null) "Host a House Bhajan" else "Change Your House Bhajan"
                HouseBhajanStore.Page.MINE -> "My House Bhajans"
            }, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink, modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            ReadableWidth(maxWidth = 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (page) {
                        HouseBhajanStore.Page.DETAIL -> DetailPage(state)
                        HouseBhajanStore.Page.FORM -> FormPage(state)
                        HouseBhajanStore.Page.MINE -> MinePage(state)
                    }
                    AddressFooter()
                }
            }
        }
    }
}

@Composable
private fun Busy() {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = AppColors.clayDark) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun MainButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
}

@Composable
private fun Tag(text: String) {
    Surface(color = AppColors.claySoft, contentColor = AppColors.clayDark, shape = RoundedCornerShape(6.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun Fact(label: String, value: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.muted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
        action?.invoke()
    }
}

// MARK: A bhajan's page

@Composable
private fun DetailPage(state: HouseBhajanStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by AccountStore.state.collectAsState()
    val b = state.detail
    if (b == null) {
        if (state.detailFailed) Note("This house bhajan couldn't be loaded. It may have been canceled, or check your connection.")
        else Busy()
        return
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("House puja and bhajan", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text(b.hostName, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink)
        if (b.tag.isNotBlank()) Tag(b.tag)
        Text("${b.whenText} · ${b.city}", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        if (!b.approved) {
            Note("${b.statusLabel}. Only you and the coordinators can see this; it isn't on the calendar.")
        }
    }
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        Fact("Host", b.hostName)
        HorizontalDivider(color = AppColors.line)
        Fact("When", b.whenText)
        HorizontalDivider(color = AppColors.line)
        if (b.address.isNotBlank()) {
            Fact("Where", b.address) {
                TextButton(onClick = {
                    openInBrowser(context, "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(b.address))
                }) { Text("Map", color = AppColors.clayDark) }
            }
        } else {
            Fact("Where", b.city + when {
                b.hiddenDetails.isNotBlank() -> " (sign in to see the ${b.hiddenDetails})"
                b.rsvp == "link" -> " (the address is on the RSVP page)"
                b.canMessage -> " (message the host for the address)"
                else -> ""
            })
        }
        if (b.phone.isNotBlank()) {
            HorizontalDivider(color = AppColors.line)
            Fact("Phone", b.phone) {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + b.phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text("Call", color = AppColors.clayDark) }
            }
        }
        if (b.email.isNotBlank()) {
            HorizontalDivider(color = AppColors.line)
            Fact("Email", b.email)
        }
        if (b.guestNote.isNotBlank()) {
            HorizontalDivider(color = AppColors.line)
            Fact("Note from the host", b.guestNote)
        }
    }
    if (b.rsvp == "link" && b.rsvpLink.isNotBlank()) {
        MainButton("RSVP") { openWebPage(context, b.rsvpLink) }
    } else {
        Text("No RSVP needed: all devotees are welcome.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
    }
    if (b.isHost) {
        OutlinedButton(onClick = { scope.launch { HouseBhajanStore.openMine() } }, modifier = Modifier.fillMaxWidth()) {
            Text("This is your bhajan: change or cancel it", color = AppColors.ink)
        }
    } else if (b.canMessage) {
        MessageHost(state, signedIn = account.user != null && state.detailSite == AccountApi.site)
    }
}

@Composable
private fun MessageHost(state: HouseBhajanStore.State, signedIn: Boolean) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Message the host")
        Text("Your message is emailed to the host with your name and your account's email, so they can reply to you. " +
            "Their email address isn't shown to you unless they reply.",
            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
        when {
            state.messageSent != null -> Note(state.messageSent, AppColors.forestInk)
            !signedIn -> OutlinedButton(onClick = { AccountStore.open() }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in to message the host", color = AppColors.ink)
            }
            else -> {
                OutlinedTextField(value = text, onValueChange = { text = it.take(2000) }, label = { Text("Message") },
                    minLines = 3, isError = state.messageErrors["message"] != null,
                    supportingText = state.messageErrors["message"]?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it.take(30) }, label = { Text("Your phone (optional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth())
                state.messageErrors[""]?.let { Note(it) }
                MainButton(if (state.busy) "Sending…" else "Send Message", enabled = text.isNotBlank() && !state.busy) {
                    scope.launch { HouseBhajanStore.message(text, phone) }
                }
            }
        }
    }
}

// MARK: My house bhajans

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)

private fun day(iso: String) = runCatching { LocalDate.parse(iso).format(dayFormat) }.getOrDefault(iso)

@Composable
private fun MinePage(state: HouseBhajanStore.State) {
    val scope = rememberCoroutineScope()
    var cancelling by remember { mutableStateOf<HouseBhajan?>(null) }
    state.notice?.let { Note(it, AppColors.forestInk) }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your house pujas and bhajans on the Samaaj calendar. A coordinator approves each one, and any change, " +
            "before it shows.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        if (state.canAdd) {
            MainButton("Host a House Bhajan") { scope.launch { HouseBhajanStore.openForm() } }
        } else {
            Note("You have the most upcoming house bhajans one account can have (10). Write to events@ayyappasamaaj.org to add more.",
                AppColors.inkSoft)
        }
    }
    val mine = state.mine
    when {
        mine == null && state.mineFailed -> Note("Your house bhajans couldn't be loaded. Please try again later.")
        mine == null -> Busy()
        mine.isEmpty() -> Text("No house bhajans yet.", style = MaterialTheme.typography.bodyLarge, color = AppColors.muted)
        else -> mine.forEach { b ->
            Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.hostName, style = MaterialTheme.typography.titleMedium, color = AppColors.ink, modifier = Modifier.weight(1f))
                    Surface(
                        color = if (b.approved) AppColors.forest else AppColors.claySoft,
                        contentColor = if (b.approved) AppColors.onForest else AppColors.clayDark,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(when (b.status) {
                            "pending" -> "Waiting for approval"
                            "approved" -> "On the calendar"
                            "declined" -> "Declined"
                            "canceled" -> "Canceled"
                            else -> b.statusLabel
                        }, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                }
                Text("${day(b.date)} · ${clock(b.startTime)} to ${clock(b.endTime)} · ${b.city}",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                if (b.declineReason.isNotBlank()) Text("Reason: ${b.declineReason}", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                val shown = listOfNotNull("street address".takeIf { b.showAddress }, "phone".takeIf { b.showPhone }, "email".takeIf { b.showEmail })
                Text("Its page shows: ${shown.joinToString(", ").ifEmpty { "only the city" }}",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { scope.launch { HouseBhajanStore.openOwn(b) } }) { Text("Open", color = AppColors.clayDark) }
                    if (b.canChange) {
                        TextButton(onClick = { scope.launch { HouseBhajanStore.openForm(b) } }) { Text("Change", color = AppColors.clayDark) }
                        TextButton(onClick = { cancelling = b }, enabled = !state.busy) { Text("Cancel it", color = AppColors.clayDark) }
                    }
                }
            }
        }
    }
    cancelling?.let { b ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("Cancel this house bhajan?") },
            text = { Text("${b.hostName}, ${day(b.date)}. It comes off the calendar, and the coordinators are told.") },
            confirmButton = {
                TextButton(onClick = { cancelling = null; scope.launch { HouseBhajanStore.cancel(b) } }) {
                    Text("Cancel It", color = AppColors.clayDark)
                }
            },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Keep It", color = AppColors.ink) } },
        )
    }
}

// MARK: The form

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, error: String?, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = if (value.isBlank()) "" else clock(value), onValueChange = {}, readOnly = true, label = { Text(label) },
            placeholder = { Text("Choose") }, isError = error != null, supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            bhajanTimes.forEach { t -> DropdownMenuItem(text = { Text(clock(t)) }, onClick = { onPick(t); open = false }) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, error: String?, help: String? = null, lines: Int = 1, max: Int = 200,
                  keyboard: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                  onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(max)) }, label = { Text(label) }, singleLine = lines == 1,
        minLines = lines, isError = error != null, keyboardOptions = keyboard,
        supportingText = (error ?: help)?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Tick(checked: Boolean, label: String, error: String? = null, onChange: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 4.dp)) {
            Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = AppColors.clay),
                modifier = Modifier.padding(top = 2.dp, end = 10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.ink)
        }
        error?.let { Note(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormPage(state: HouseBhajanStore.State) {
    val scope = rememberCoroutineScope()
    val info = state.formInfo
    if (info == null) {
        if (state.formFailed) Note("The form couldn't be loaded. Please try again later.") else Busy()
        return
    }
    if (state.editing == null && !info.canAdd) {
        Note("You have the most upcoming house bhajans one account can have (10). Write to events@ayyappasamaaj.org to add more.")
        return
    }
    var f by remember(state.editing?.id) {
        mutableStateOf(state.editing?.let { BhajanFields.of(it) } ?: BhajanFields(hostName = info.hostName, phone = info.phone))
    }
    var picking by remember { mutableStateOf(false) }
    val e = state.errors

    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (state.editing == null) "Hosting an Ayyappa puja and bhajan at home? Send it here: a coordinator approves it, " +
            "then it's on the Samaaj calendar. The calendar shows the host's name, the date, the puja times and the city."
            else "A change goes back to a coordinator before it shows, unless you only change what the page shows.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        Field("Host", f.hostName, e["host_name"], "Shown on the calendar, e.g. \"Iyer family\".", max = 80) { f = f.copy(hostName = it) }
        OutlinedTextField(
            value = if (f.date.isBlank()) "" else day(f.date), onValueChange = {}, readOnly = true, label = { Text("Date") },
            isError = e["date"] != null, supportingText = e["date"]?.let { { Text(it) } },
            trailingIcon = { TextButton(onClick = { picking = true }) { Text(if (f.date.isBlank()) "Choose" else "Change", color = AppColors.clayDark) } },
            modifier = Modifier.fillMaxWidth(),
        )
        TimeField("Puja starts", f.startTime, e["start_time"]) { f = f.copy(startTime = it) }
        TimeField("Puja ends", f.endTime, e["end_time"]) { f = f.copy(endTime = it) }
        Field("Street address", f.street, e["street"], "For the coordinators, unless you choose below to show it.") { f = f.copy(street = it) }
        Field("City", f.city, e["city"], "Shown on the calendar.", max = 80) { f = f.copy(city = it) }
        Field("ZIP code", f.zipCode, e["zip_code"], max = 10, keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) { f = f.copy(zipCode = it) }
        Field("Host's phone", f.phone, e["phone"], "For the coordinators, unless you choose below to show it.", max = 30,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone)) { f = f.copy(phone = it) }

        SectionLabel("How guests RSVP")
        info.rsvpChoices.forEach { c ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()
                .selectable(selected = f.rsvp == c.value, role = Role.RadioButton, onClick = { f = f.copy(rsvp = c.value) })
                .padding(vertical = 4.dp)) {
                RadioButton(selected = f.rsvp == c.value, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AppColors.clay),
                    modifier = Modifier.padding(top = 2.dp, end = 10.dp))
                Text(c.label, style = MaterialTheme.typography.bodyMedium, color = AppColors.ink)
            }
        }
        if (f.rsvp == "link") {
            Field("RSVP link", f.rsvpLink, e["rsvp_link"],
                "Your Evite, Punchbowl, SignUpGenius or Google Form page. Guests find the address there. That page may show your address to anyone who has the link.",
                max = 500, keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri)) { f = f.copy(rsvpLink = it) }
        }
        Field("Note for guests (optional)", f.guestNote, e["guest_note"],
            "Shown on the calendar, e.g. \"Please park on the street.\" No phone numbers or addresses here.", lines = 2, max = 300,
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) { f = f.copy(guestNote = it) }
        Field("Note for the coordinators (optional)", f.coordinatorNote, e["coordinator_note"],
            "e.g. \"We would like a Guruswami to lead the bhajan.\"", lines = 2, max = 1000,
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) { f = f.copy(coordinatorNote = it) }

        SectionLabel("What the bhajan's page shows")
        Text("Signed-in visitors only, and only what you tick. Either way, guests can message you without seeing your email address.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
        Tick(f.showAddress, "I agree that my street address is shown on the bhajan's page to signed-in visitors") { f = f.copy(showAddress = it) }
        Tick(f.showPhone, "I agree that my phone number is shown on the bhajan's page to signed-in visitors") { f = f.copy(showPhone = it) }
        Tick(f.showEmail, "I agree that my email address is shown on the bhajan's page to signed-in visitors") { f = f.copy(showEmail = it) }
        HorizontalDivider(color = AppColors.line)
        Tick(f.agree, "I am the host, or I have the host's permission, and the host's name, the date, the puja times, the " +
            "city and the note for guests may be shown on the public calendar.", e["agree"]) { f = f.copy(agree = it) }
        e[""]?.let { Note(it) }
        if (e.isNotEmpty() && e.keys.any { it.isNotEmpty() }) Note("Please check the fields marked above.")
        MainButton(if (state.busy) "Sending…" else if (state.editing == null) "Send for Approval" else "Save Changes",
            enabled = !state.busy) { scope.launch { HouseBhajanStore.submit(f) } }
    }

    if (picking) {
        val earliest = LocalDate.parse(info.earliest)
        val latest = LocalDate.parse(info.latest)
        val toMillis = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = toMillis(runCatching { LocalDate.parse(f.date) }.getOrDefault(earliest)),
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
                    picker.selectedDateMillis?.let { f = f.copy(date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    picking = false
                }) { Text("OK", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel", color = AppColors.ink) } },
        ) { DatePicker(state = picker) }
    }
}
