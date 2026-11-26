package com.ayyappasamaaj.tattvamasi.ui.donate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * "I sent a Zelle gift" in Donate: the website's /donate/zelle/ page in the app, through /api/donations/zelle/
 * (website: donations/api/views.py ZelleReportAPI). The website gives the Zelle instructions and a memo code (AYS-4821)
 * to type in the Zelle memo; the donor reports the gift with the page's form and rules; it then waits for the
 * treasurer and shows in My Account's donations (/api/donations/mine/zelle/). Signed in only. As the iOS app's
 * ZelleReport in Donate.swift.
 */
data class ZelleCategory(val id: Int, val label: String)

data class ZelleForm(
    val enabled: Boolean,
    val zelleEmail: String = "",
    val zelleName: String = "",
    val memoCode: String = "",
    val instructions: String = "",
    val note: String = "",
    val categories: List<ZelleCategory> = emptyList(),
    val givenOn: String = "",
    val senderName: String = "",
    val earliest: String = "",
    val latest: String = "",
)

/** What was reported, in the website page's words. */
data class ZelleReported(val memoCode: String, val amount: String, val givenOn: String, val category: String,
                         val state: String, val thanks: String, val memoText: String, val statusText: String)

sealed interface ZelleResult {
    data class Reported(val gift: ZelleReported) : ZelleResult
    /** The form's problems by field (amount, given_on, category, sender_name, memo, memo_code; "" for the whole form) */
    data class Problems(val errors: Map<String, String>) : ZelleResult
    data object SignedOut : ZelleResult
}

fun parseZelleForm(body: String): ZelleForm? = runCatching {
    val json = JSONObject(body)
    if (!json.optBoolean("enabled")) return@runCatching ZelleForm(enabled = false)
    val initial = json.optJSONObject("initial") ?: JSONObject()
    val list = json.optJSONArray("categories")
    ZelleForm(
        enabled = true,
        zelleEmail = json.getString("zelle_email"),
        zelleName = json.optString("zelle_name"),
        memoCode = json.getString("memo_code"),
        instructions = json.optString("instructions"),
        note = json.optString("note"),
        categories = if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optJSONObject(it) }
            .map { ZelleCategory(it.optInt("id"), it.optString("label")) },
        givenOn = initial.optString("given_on"),
        senderName = initial.optString("sender_name"),
        earliest = json.optString("earliest"),
        latest = json.optString("latest"),
    )
}.getOrNull()

fun parseZelleReport(code: Int, body: String): ZelleResult {
    if (code == 401) return ZelleResult.SignedOut
    val json = runCatching { JSONObject(body) }.getOrNull()
    if (code == 201 && json != null) {
        return ZelleResult.Reported(ZelleReported(json.optString("memo_code"), json.optString("amount"),
            json.optString("given_on"), json.optString("category"), json.optString("state"), json.optString("thanks"),
            json.optString("memo_text"), json.optString("status_text")))
    }
    val errors = json?.optJSONObject("errors")?.let { e ->
        e.keys().asSequence().associateWith { e.optString(it) }
    }
    if (!errors.isNullOrEmpty()) return ZelleResult.Problems(errors)
    val reason = json?.optString("error").orEmpty().ifBlank {
        if (code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
        else "It couldn't be sent just now. Please try again in a few minutes."
    }
    return ZelleResult.Problems(mapOf("" to reason))
}

private val dayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
private fun day(iso: String) = runCatching { LocalDate.parse(iso).format(dayFormat) }.getOrDefault(iso)

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Zelle memo code", text))
}

/** The Zelle page over Donate: Sign In first, the form, or what was reported. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZellePage(state: DonateStore.State, signedIn: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    if (!signedIn) {
        Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Zelle payments go straight to the bank, so we don't see them. Sign in to tell us about yours: it " +
                "then joins your donation history once the treasurer finds it in the bank.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
            Primary("Sign In") { AccountStore.open() }
        }
        return
    }
    LaunchedEffect(Unit) { if (state.zelleForm == null) DonateStore.loadZelle() }

    state.zelleReported?.let { gift ->
        Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Thank You", style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
            Text(gift.thanks, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(gift.memoCode, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight(700)),
                color = AppColors.clayDark)
            Text(gift.memoText, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            OutlinedButton(onClick = { copy(context, gift.memoCode) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Copy Code", color = AppColors.ink)
            }
            Text(gift.statusText, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            Primary("Go to My Donations") { DonateStore.close(); AccountStore.openDonations() }
            TextButton(onClick = { scope.launch { DonateStore.anotherZelle() } }) {
                Text("Tell us about another donation", color = AppColors.clayDark)
            }
        }
        return
    }

    val form = state.zelleForm
    if (form == null || !form.enabled) {
        Text(if (state.zelleFailed || form?.enabled == false) "Zelle donations can't be reported in the app right now. " +
            "Please try again later." else "Loading…", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        return
    }
    var amount by rememberSaveable(form.memoCode) { mutableStateOf("") }
    var givenOn by rememberSaveable(form.memoCode) { mutableStateOf(form.givenOn) }
    var category by rememberSaveable(form.memoCode) { mutableStateOf(form.categories.firstOrNull()?.id) }
    var sender by rememberSaveable(form.memoCode) { mutableStateOf(form.senderName) }
    var memo by rememberSaveable(form.memoCode) { mutableStateOf("") }
    var memoCode by rememberSaveable(form.memoCode) { mutableStateOf(form.memoCode) }
    var picking by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val errors = state.zelleErrors

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Send it first")
        Text(form.instructions, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        Text(form.memoCode, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight(700)),
            color = AppColors.clayDark)
        OutlinedButton(onClick = { copy(context, form.memoCode) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Copy Code", color = AppColors.ink)
        }
        OutlinedButton(onClick = { copy(context, form.zelleEmail) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Copy Email (${form.zelleEmail})", color = AppColors.ink)
        }
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Then tell us")
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($)") },
            singleLine = true, isError = errors["amount"] != null, supportingText = errors["amount"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = day(givenOn), onValueChange = {}, readOnly = true, label = { Text("Date sent") },
            isError = errors["given_on"] != null, supportingText = errors["given_on"]?.let { { Text(it) } },
            trailingIcon = { TextButton(onClick = { picking = true }) { Text("Change", color = AppColors.clayDark) } },
            modifier = Modifier.fillMaxWidth())
        ExposedDropdownMenuBox(expanded = choosing, onExpandedChange = { choosing = it }) {
            OutlinedTextField(
                value = form.categories.firstOrNull { it.id == category }?.label.orEmpty(), onValueChange = {},
                readOnly = true, label = { Text("For") }, isError = errors["category"] != null,
                supportingText = errors["category"]?.let { { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(choosing) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                form.categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c.label) }, onClick = { category = c.id; choosing = false })
                }
            }
        }
        OutlinedTextField(value = sender, onValueChange = { sender = it.take(150) },
            label = { Text("Sent from (the name on the bank account)") }, singleLine = true,
            isError = errors["sender_name"] != null, supportingText = errors["sender_name"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = memo, onValueChange = { memo = it.take(300) }, label = { Text("Note (optional)") },
            isError = errors["memo"] != null, supportingText = errors["memo"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = memoCode, onValueChange = { memoCode = it.take(20) },
            label = { Text("Memo code (what you typed in the Zelle memo)") }, singleLine = true,
            isError = errors["memo_code"] != null, supportingText = errors["memo_code"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth())
        errors[""]?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Primary(if (state.busy) "Sending…" else "Send", enabled = amount.isNotBlank() && sender.isNotBlank() &&
            category != null && !state.busy) {
            keyboard?.hide()
            scope.launch { DonateStore.reportZelle(amount, givenOn, category ?: return@launch, sender, memo, memoCode) }
        }
        Text(form.note, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
    }

    if (picking) {
        val earliest = remember(form) { runCatching { LocalDate.parse(form.earliest) }.getOrElse { LocalDate.now().minusDays(400) } }
        val latest = remember(form) { runCatching { LocalDate.parse(form.latest) }.getOrElse { LocalDate.now() } }
        val toMillis = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = toMillis(runCatching { LocalDate.parse(givenOn) }.getOrElse { latest }),
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
                        givenOn = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text("OK", color = AppColors.clayDark) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel", color = AppColors.ink) } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun Primary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
}
