package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
 * The treasurer's Zelle donations and Check and cash donations pages (Treasurer.kt), instead of the website's: the
 * same checks and receipts, through donations/treasurer_api.py. As the iOS app's TreasurerViews.swift.
 */

@Composable
private fun Waiting() {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun Told(text: String, good: Boolean) {
    Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(600)),
        color = if (good) AppColors.forestInk else AppColors.clayDark,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun Primary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
    }
}

@Composable
private fun Input(label: String, value: String, error: String?, help: String? = null,
                  keyboard: KeyboardOptions = KeyboardOptions.Default, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        isError = error != null, keyboardOptions = keyboard, supportingText = (error ?: help)?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(categories: List<GiftCategory>, chosen: Int?, error: String?, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(value = categories.firstOrNull { it.id == chosen }?.label.orEmpty(), onValueChange = {},
            readOnly = true, label = { Text("For") }, isError = error != null, supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            categories.forEach { c -> DropdownMenuItem(text = { Text(c.label) }, onClick = { onPick(c.id); open = false }) }
        }
    }
}

/** A date field with the picker, from [earliest] to [latest]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInput(label: String, value: String, error: String?, help: String?, earliest: LocalDate, latest: LocalDate,
                      onPick: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(value = if (value.isBlank()) "" else summaryDay(value), onValueChange = {}, readOnly = true,
        label = { Text(label) }, isError = error != null, supportingText = (error ?: help)?.let { { Text(it) } },
        trailingIcon = { TextButton(onClick = { picking = true }) { Text("Change", color = AppColors.clayDark) } },
        modifier = Modifier.fillMaxWidth())
    if (picking) {
        val toMillis = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = toMillis(runCatching { LocalDate.parse(value) }.getOrDefault(latest.coerceAtMost(LocalDate.now()))),
            yearRange = earliest.year..latest.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !d.isBefore(earliest) && !d.isAfter(latest)
                }
                override fun isSelectableYear(year: Int) = year in earliest.year..latest.year
            })
        DatePickerDialog(onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    picking = false
                }) { Text("OK", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel", color = AppColors.ink) } },
        ) { DatePicker(state = picker) }
    }
}

// MARK: Zelle donations

@Composable
fun ZellePage() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<ZelleList?>(null) }
    var failed by remember { mutableStateOf(false) }
    var told by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf<Pair<WaitingZelle, String>?>(null) }  // the gift and "confirm" / "not-found"
    var note by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }

    suspend fun load() {
        val token = AccountStore.sessionToken ?: return
        val (code, fresh) = TreasurerApi.zelle(token)
        if (code == 401) return AccountStore.ended()
        if (fresh != null) list = fresh
        failed = fresh == null
    }
    LaunchedEffect(Unit) { load() }

    told?.let { Told(it.first, it.second) }
    val l = list
    if (l == null) {
        if (failed) Told("The Zelle donations couldn't be loaded. Please try again later.", false) else Waiting()
        return
    }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Confirm each reported gift against the bank by its memo code; the donor is emailed the receipt. If it isn't " +
            "in the bank, mark it not found.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        if (!adding) OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add a Gift Nobody Reported", color = AppColors.ink)
        }
    }
    if (adding) {
        AddZelleCard(l.categories, onCancel = { adding = false }) { message ->
            adding = false
            told = message to true
            scope.launch { load() }
        }
    }
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        SectionLabel("Waiting (${l.waiting.size})")
        if (l.waiting.isEmpty()) {
            Text("Nothing waiting.", style = MaterialTheme.typography.bodyLarge, color = AppColors.muted,
                modifier = Modifier.padding(top = 6.dp))
        }
        l.waiting.forEachIndexed { i, g ->
            if (i > 0) HorizontalDivider(color = AppColors.line)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.memoCode, style = MaterialTheme.typography.titleMedium, color = AppColors.ink, modifier = Modifier.weight(1f))
                    Text(g.amount, style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
                }
                Text("${g.donor} · ${g.email}", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                Text("Sent ${summaryDay(g.givenOn)} from ${g.senderName} · ${g.category}",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                if (g.memo.isNotBlank()) Text("Note: ${g.memo}", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(onClick = { note = ""; asking = g to "confirm" }, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton)) {
                        Text("Confirm")
                    }
                    OutlinedButton(onClick = { note = ""; asking = g to "not-found" }, enabled = !busy) {
                        Text("Not Found", color = AppColors.ink)
                    }
                }
            }
        }
    }
    if (l.reviewed.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Latest reviewed")
            l.reviewed.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = AppColors.line)
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {}) {
                    Row {
                        Text("${r.memoCode} · ${r.donor}", style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
                            modifier = Modifier.weight(1f))
                        Text(r.amount, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)), color = AppColors.ink)
                    }
                    Text(listOf(when (r.state) { "confirmed" -> "Confirmed"; "not_found" -> "Not found"; else -> r.state },
                        r.by.takeIf { it.isNotBlank() }?.let { "by $it" }, r.note.takeIf { it.isNotBlank() })
                        .filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                }
            }
        }
    }

    asking?.let { (gift, action) ->
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(if (action == "confirm") "Confirm ${gift.memoCode}?" else "Not found in the bank?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (action == "confirm") "${gift.amount} from ${gift.senderName}, sent ${summaryDay(gift.givenOn)}. " +
                        "It counts as a donation, and ${gift.email} is emailed the receipt."
                    else "${gift.memoCode}, ${gift.amount}: it stays out of every total, and the donor sees it wasn't found.")
                    OutlinedTextField(value = note, onValueChange = { note = it.take(300) }, label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    asking = null
                    scope.launch {
                        val token = AccountStore.sessionToken ?: return@launch
                        busy = true
                        val answer = try { TreasurerApi.review(token, gift.id, action, note) } finally { busy = false }
                        when (answer) {
                            is TreasurerAnswer.Done -> told = answer.message to !answer.message.contains("couldn't")
                            is TreasurerAnswer.Problems -> told = answer.errors.values.joinToString(" ") to false
                            TreasurerAnswer.SignedOut -> AccountStore.ended()
                        }
                        load()
                    }
                }) { Text(if (action == "confirm") "Confirm" else "Mark Not Found", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { asking = null }) { Text("Cancel", color = AppColors.ink) } },
        )
    }
}

@Composable
private fun AddZelleCard(categories: List<GiftCategory>, onCancel: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var givenOn by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var sender by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Add a gift nobody reported")
        Text("Already confirmed, from the bank. An account with this verified email gets it; otherwise it's kept by email " +
            "until they sign up. The donor is emailed the receipt.", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
        Input("Donor's email", email, errors["email"], keyboard = KeyboardOptions(keyboardType = KeyboardType.Email)) { email = it.trim() }
        Input("Memo code (optional)", code, errors["memo_code"], "The code in the Zelle memo, e.g. AYS-4821. Empty: a new one.") { code = it }
        Input("Amount ($)", amount, errors["amount"], keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)) { amount = it }
        DateInput("Date sent", givenOn, errors["given_on"], null, LocalDate.now().minusDays(400), LocalDate.now()) { givenOn = it }
        CategoryPicker(categories, category, errors["category"]) { category = it }
        Input("Sent from (the name on the bank account)", sender, errors["sender_name"],
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words)) { sender = it }
        Input("Note (optional)", memo, errors["memo"]) { memo = it.take(300) }
        errors[""]?.let { Told(it, false) }
        Primary(if (busy) "Adding…" else "Add and Email the Receipt", enabled = !busy) {
            scope.launch {
                val token = AccountStore.sessionToken ?: return@launch
                busy = true
                val answer = try {
                    TreasurerApi.addZelle(token, mapOf("email" to email, "memo_code" to code, "amount" to amount.replace("$", "").replace(",", ""),
                        "given_on" to givenOn, "category" to (category?.toString() ?: ""), "sender_name" to sender, "memo" to memo))
                } finally { busy = false }
                when (answer) {
                    is TreasurerAnswer.Done -> onDone(answer.message)
                    is TreasurerAnswer.Problems -> errors = answer.errors
                    TreasurerAnswer.SignedOut -> AccountStore.ended()
                }
            }
        }
        TextButton(onClick = onCancel) { Text("Cancel", color = AppColors.clayDark) }
    }
}

// MARK: Check and cash donations

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecksPage() {
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ChecksInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    var told by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<DonorAccount>?>(null) }
    var picked by remember { mutableStateOf<DonorAccount?>(null) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var cash by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf<Int?>(null) }
    var memo by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf(true) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var busy by remember { mutableStateOf(false) }

    suspend fun load(q: String = "") {
        val token = AccountStore.sessionToken ?: return
        val (code, fresh) = TreasurerApi.checks(token, q)
        if (code == 401) return AccountStore.ended()
        if (fresh != null) {
            info = fresh
            if (category == null) category = fresh.categories.firstOrNull()?.id
            if (q.isNotBlank()) found = fresh.accounts
        }
        failed = fresh == null
    }
    LaunchedEffect(Unit) { load() }

    told?.let { Told(it, !it.contains("couldn't")) }
    val i = info
    if (i == null) {
        if (failed) Told("The page couldn't be loaded. Please try again later.", false) else Waiting()
        return
    }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("The donor")
        val p = picked
        if (p != null) {
            Text("${p.name.ifBlank { p.email }} · ${p.email}", style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            TextButton(onClick = { picked = null }) { Text("Choose someone else", color = AppColors.clayDark) }
        } else {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Find their account (name or email)") }, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { scope.launch { load(query) } }, enabled = query.isNotBlank()) {
                        Text("Find", color = AppColors.clayDark)
                    }
                })
            found?.let { list ->
                if (list.isEmpty()) Text("No account matches. Type their email below instead.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                list.forEach { a ->
                    Text("${a.name.ifBlank { a.email }} · ${a.email}", style = MaterialTheme.typography.bodyLarge, color = AppColors.clayDark,
                        modifier = Modifier.fillMaxWidth().clickable { picked = a; name = a.name; found = null }.padding(vertical = 8.dp))
                }
            }
            Input("Or the donor's email", email, errors["email"],
                "With no account for this email, one is made and the receipt invites them to set a password.",
                KeyboardOptions(keyboardType = KeyboardType.Email)) { email = it.trim() }
        }
        Input("Donor's name", name, errors["name"], keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words)) { name = it }
    }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("The gift")
        val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.claySoft,
            selectedLabelColor = AppColors.clayDark, labelColor = AppColors.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !cash, onClick = { cash = false }, label = { Text("Check") }, colors = chip)
            FilterChip(selected = cash, onClick = { cash = true }, label = { Text("Cash") }, colors = chip)
        }
        Input("Amount ($)", amount, errors["amount"], keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)) { amount = it }
        if (!cash) {
            Input("Check number", number, errors["check_number"], keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) { number = it }
            Input("Bank", bank, errors["bank_name"], "The bank the check is drawn on.",
                KeyboardOptions(capitalization = KeyboardCapitalization.Words)) { bank = it }
        }
        DateInput("Date", date, errors["check_date"], "The date on the check, or the day the cash was given.",
            LocalDate.now().minusDays(400), LocalDate.now().plusDays(180)) { date = it }
        CategoryPicker(i.categories, category, errors["category"]) { category = it }
        Input("Note (optional)", memo, errors["memo"], "E.g. the check's memo line, or who handed in the cash.") { memo = it.take(300) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Email the receipt to the donor", style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
                modifier = Modifier.weight(1f))
            Switch(checked = receipt, onCheckedChange = { receipt = it }, colors = SwitchDefaults.colors(checkedTrackColor = AppColors.clay))
        }
        errors[""]?.let { Told(it, false) }
        Primary(if (busy) "Adding…" else if (cash) "Add the Cash" else "Add the Check", enabled = !busy) {
            scope.launch {
                val token = AccountStore.sessionToken ?: return@launch
                busy = true
                val fields = mutableMapOf<String, Any>("name" to name, "paid_by" to if (cash) "cash" else "check",
                    "amount" to amount.replace("$", "").replace(",", ""), "check_number" to number, "bank_name" to bank,
                    "check_date" to date, "category" to (category?.toString() ?: ""), "memo" to memo, "send_receipt" to receipt)
                picked?.let { fields["donor"] = it.id.toString() } ?: run { fields["email"] = email }
                val answer = try { TreasurerApi.addCheck(token, fields) } finally { busy = false }
                when (answer) {
                    is TreasurerAnswer.Done -> {
                        told = answer.message
                        errors = emptyMap()
                        picked = null; query = ""; found = null; email = ""; name = ""; amount = ""; number = ""; bank = ""; memo = ""
                        load()
                    }
                    is TreasurerAnswer.Problems -> errors = answer.errors
                    TreasurerAnswer.SignedOut -> AccountStore.ended()
                }
            }
        }
    }
    if (i.recent.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Latest checks and cash")
            i.recent.forEachIndexed { n, c ->
                if (n > 0) HorizontalDivider(color = AppColors.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {}) {
                    Column(Modifier.weight(1f)) {
                        Text(c.donor, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                        Text("${summaryDay(c.date)} · ${c.paidBy} · ${c.category}", style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.muted)
                    }
                    Text(c.amount, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)), color = AppColors.ink)
                }
            }
        }
    }
}

// MARK: Add an expense

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpensePage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ExpenseFormInfo?>(null) }
    var failed by remember { mutableStateOf(false) }
    var told by remember { mutableStateOf<String?>(null) }
    var event by remember { mutableStateOf<Int?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf<Int?>(null) }
    var weekend by remember { mutableStateOf("auto") }
    var amount by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = AccountStore.sessionToken ?: return@LaunchedEffect
        val (code, fresh) = TreasurerApi.expenseForm(token)
        if (code == 401) return@LaunchedEffect AccountStore.ended()
        info = fresh
        failed = fresh == null
        if (fresh != null && event == null) event = fresh.defaultEvent ?: fresh.events.firstOrNull()?.id
    }

    told?.let { Told(it, true) }
    val i = info
    if (i == null) {
        if (failed) Told("The form couldn't be loaded. Please try again later.", false) else Waiting()
        return
    }
    val picked = i.events.firstOrNull { it.id == event }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What was spent for an event, by category. Imports from the bank or Wave, categories, weekends and dates " +
            "are on the website.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        SectionLabel("Event")
        val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.claySoft,
            selectedLabelColor = AppColors.clayDark, labelColor = AppColors.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            i.events.forEach { e ->
                FilterChip(selected = e.id == event, onClick = { event = e.id; category = null }, label = { Text(e.name) }, colors = chip)
            }
        }
        errors["event"]?.let { Told(it, false) }
        DateInput("Date", date, errors["spent_on"], null, LocalDate.now().minusYears(3), LocalDate.now().plusDays(31)) { date = it }
        var open by remember { mutableStateOf(false) }
        val groups = i.categories[event] ?: emptyList()
        val chosenLabel = groups.flatMap { g -> g.options.map { g.group to it } }.firstOrNull { it.second.id == category }
            ?.let { (g, o) -> if (o.label.endsWith("(general)")) o.label else "$g › ${o.label}" }.orEmpty()
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(value = chosenLabel, onValueChange = {}, readOnly = true, label = { Text("Category") },
                isError = errors["category"] != null, supportingText = errors["category"]?.let { { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                groups.forEach { g ->
                    Text(g.group.uppercase(), fontSize = 12.sp, fontWeight = FontWeight(700), color = AppColors.muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    g.options.forEach { o ->
                        DropdownMenuItem(text = { Text(o.label) }, onClick = { category = o.id; open = false })
                    }
                }
            }
        }
        if (picked?.hasWeekends == true) {
            var weekendOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = weekendOpen, onExpandedChange = { weekendOpen = it }) {
                OutlinedTextField(value = i.weekends.firstOrNull { it.first == weekend }?.second.orEmpty(), onValueChange = {},
                    readOnly = true, label = { Text("Weekend") }, isError = errors["weekend"] != null,
                    supportingText = { Text(errors["weekend"] ?: "Monday and Tuesday count toward the weekend just past, " +
                        "Wednesday to Sunday toward the coming one.") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(weekendOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(expanded = weekendOpen, onDismissRequest = { weekendOpen = false }) {
                    i.weekends.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { weekend = value; weekendOpen = false })
                    }
                }
            }
        }
        Input("Amount ($)", amount, errors["amount"], keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)) { amount = it }
        Input("Paid to", paidTo, errors["paid_to"], "The shop or person paid.",
            KeyboardOptions(capitalization = KeyboardCapitalization.Words)) { paidTo = it.take(150) }
        Input("Note", note, errors["note"], "E.g. what was bought, or the receipt or check number.") { note = it.take(300) }
        errors[""]?.let { Told(it, false) }
        Primary(if (busy) "Adding…" else "Add the Expense", enabled = !busy) {
            scope.launch {
                val token = AccountStore.sessionToken ?: return@launch
                busy = true
                val answer = try {
                    TreasurerApi.addExpense(token, mapOf("event" to (event?.toString() ?: ""), "spent_on" to date,
                        "category" to (category?.toString() ?: ""), "weekend" to if (picked?.hasWeekends == true) weekend else "",
                        "amount" to amount.replace("$", "").replace(",", ""), "paid_to" to paidTo, "note" to note))
                } finally { busy = false }
                when (answer) {
                    is TreasurerAnswer.Done -> { told = answer.message; errors = emptyMap(); amount = ""; paidTo = ""; note = "" }
                    is TreasurerAnswer.Problems -> { errors = answer.errors; told = null }
                    TreasurerAnswer.SignedOut -> AccountStore.ended()
                }
            }
        }
        if (i.webUrl.isNotBlank()) {
            TextButton(onClick = { com.ayyappasamaaj.tattvamasi.ui.openInBrowser(context, i.webUrl) }) {
                Text("Expense management on the website", color = AppColors.clayDark)
            }
        }
    }
}
