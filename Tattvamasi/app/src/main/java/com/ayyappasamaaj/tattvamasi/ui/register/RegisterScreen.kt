package com.ayyappasamaaj.tattvamasi.ui.register

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A registration form over any tab (RegisterStore), from an event's Register button: the form drawn natively,
 *  paid in Stripe's payment sheet (card, Google Pay, Link), then the thank-you with the WhatsApp group. */
@Composable
fun RegisterSheet() {
    val state by RegisterStore.state.collectAsState()
    if (!state.shown) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> scope.launch { RegisterStore.paid() }
            is PaymentSheetResult.Canceled -> RegisterStore.notPaid(null)
            is PaymentSheetResult.Failed -> RegisterStore.notPaid(result.error.localizedMessage)
        }
    }
    fun present(pay: Submitted.Pay) {
        PaymentConfiguration.init(context, pay.publishableKey)
        val answers = state.answers
        sheet.presentWithPaymentIntent(
            pay.clientSecret,
            PaymentSheet.Configuration.Builder("Ayyappa Samaaj")
                .defaultBillingDetails(PaymentSheet.BillingDetails(name = answers["name"]?.trim(),
                    email = answers["email"]?.trim()))
                .googlePay(PaymentSheet.GooglePayConfiguration(
                    environment = if (BuildConfig.DEBUG) PaymentSheet.GooglePayConfiguration.Environment.Test
                    else PaymentSheet.GooglePayConfiguration.Environment.Production,
                    countryCode = "US",
                    currencyCode = "USD",
                ))
                .allowsDelayedPaymentMethods(false)
                .build(),
        )
    }
    // The sheet opens by itself once the registration is made
    LaunchedEffect(state.pay?.clientSecret) { state.pay?.let { present(it) } }
    LaunchedEffect(state.slug) { if (state.form == null) RegisterStore.load() }
    BackHandler { RegisterStore.close() }

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
            IconButton(onClick = { RegisterStore.close() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text("Register", style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            ReadableWidth(maxWidth = 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val form = state.form
                    when {
                        state.done != null -> ThankYou(state.done!!, state.message)
                        form == null && !state.failed -> Row(horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
                        }
                        state.failed || form == null -> Website(form, state.link)
                        else -> Form(form, state) { pay -> present(pay) }
                    }
                    AddressFooter()
                }
            }
        }
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

@Composable
private fun Problem(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun Errors(state: RegisterStore.State, key: String) {
    state.errors[key]?.takeIf { it.isNotEmpty() }?.let { Problem(it.joinToString(" ")) }
}

/** The app can't show it (closed, full, a new kind of question, or no answer): the website's form instead. */
@Composable
private fun Website(form: RegisterForm?, link: String) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        form?.let { Text(it.title, style = MaterialTheme.typography.titleLarge, color = AppColors.ink) }
        Text(when (form?.state) {
            "full" -> "This registration is full."
            "closed" -> "Registration has closed."
            "not_open" -> "Registration hasn't opened yet."
            else -> "This form opens on our website."
        }, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        if (form == null || form.state == "open") {
            Primary("Open the Form on the Website") {
                openWebPage(context, form?.let { AccountApi.site + it.website } ?: link)
            }
        }
    }
}

@Composable
private fun ThankYou(done: RegistrationStatus, note: String?) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Swamiye Saranam", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text("You're registered for ${done.title}.", style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text(listOfNotNull("Registration ${done.code}", done.total.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }
            ?.let { "paid ${formDollars(it)}" }).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft) }
        if (done.thankYou.isNotBlank()) {
            Text(done.thankYou, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        }
        Text("We've emailed your confirmation. It's also in My Account when you're signed in.",
            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
        if (done.whatsappUrl.isNotBlank()) {
            Primary("Join the WhatsApp Group") {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(done.whatsappUrl))) }
            }
            if (done.whatsappNote.isNotBlank()) {
                Text(done.whatsappNote, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
            }
        }
        OutlinedButton(onClick = { RegisterStore.close() }, modifier = Modifier.fillMaxWidth()) {
            Text("Done", color = AppColors.ink)
        }
    }
}

@Composable
private fun Form(form: RegisterForm, state: RegisterStore.State, present: (Submitted.Pay) -> Unit) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val answers = state.answers
    val totals = totals(form, answers)

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(form.title, style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
            modifier = Modifier.semantics { heading() })
        form.intro.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        }
    }

    // Your details
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Your details")
        Field(state, "name", "Full name")
        Field(state, "email", "Email", KeyboardType.Email)
        if (form.phone != "off") {
            Field(state, "phone", if (form.phone == "required") "Phone" else "Phone (optional)", KeyboardType.Phone)
            if (form.askWhatsapp) {
                Tick("My WhatsApp number is the same as my phone number", answers["phone_on_whatsapp"] != "false") {
                    RegisterStore.answer("phone_on_whatsapp", it.toString())
                }
                if (answers["phone_on_whatsapp"] == "false") Field(state, "whatsapp", "WhatsApp number", KeyboardType.Phone)
            }
        }
    }

    // The items, under their groups' headings
    val groupOrder = form.groups.ifEmpty { form.items.map { it.group }.distinct().map { FormGroup(it, "", false) } }
    groupOrder.forEach { group ->
        val items = form.items.filter { it.group == group.name }
        if (items.isNotEmpty()) ItemGroupCard(group, items, state)
    }

    // The questions
    val questions = form.questions.filter { shown(it, form, answers) }
    if (questions.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("A few questions")
            questions.forEach { Question(it, needed(it, form, answers), state) }
        }
    }

    // Total and send
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (form.offerFeeCover && totals.items.signum() > 0) {
            Tick("Add ${formDollars(processingFee(form, totals.items))} to cover the card processing fee, so the " +
                "Samaaj receives the full amount", answers["cover_fee"] == "true") {
                RegisterStore.answer("cover_fee", it.toString())
            }
        }
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
            Text("Total", style = MaterialTheme.typography.titleMedium, color = AppColors.ink, modifier = Modifier.weight(1f))
            Text(formDollars(totals.total).ifBlank { "$0" }, style = MaterialTheme.typography.titleMedium, color = AppColors.ink)
        }
        Errors(state, "__all__")
        state.message?.let { if (state.errors["__all__"] == null) Problem(it) }
        val pay = state.pay
        when {
            state.checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(22.dp))
                Text("Confirming your payment…", style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            }
            pay != null -> {
                Primary("Pay ${formDollars(pay.total)}") { present(pay) }
                TextButton(onClick = { RegisterStore.startOver() }) {
                    Text("Change my answers", color = AppColors.clayDark)
                }
            }
            else -> Primary(
                when {
                    state.busy -> "Sending…"
                    totals.total.signum() > 0 -> "Continue to Payment"
                    else -> "Register"
                },
                enabled = !state.busy,
            ) {
                keyboard?.hide()
                scope.launch { RegisterStore.submit() }
            }
        }
        if (totals.total.signum() > 0 && pay == null) {
            Text("You'll pay in Stripe's secure payment screen: card or Google Pay. A receipt is emailed to you.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
        }
    }
}

@Composable
private fun Field(state: RegisterStore.State, key: String, label: String, type: KeyboardType = KeyboardType.Text,
                  lines: Int = 1) {
    OutlinedTextField(
        value = state.answers[key].orEmpty(), onValueChange = { RegisterStore.answer(key, it) },
        label = { Text(label) }, singleLine = lines == 1, minLines = lines,
        isError = state.errors.containsKey(key),
        supportingText = state.errors[key]?.let { { Text(it.joinToString(" ")) } },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Tick(text: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = AppColors.clay))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ItemGroupCard(group: FormGroup, items: List<FormItem>, state: RegisterStore.State) {
    val anyChosen = items.any { chosen(it, state.answers) }
    var open by rememberSaveable(group.name) { mutableStateOf(!group.folded || anyChosen) }
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        if (group.name.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().then(if (group.folded) Modifier.clickable(role = Role.Button) { open = !open } else Modifier)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                    modifier = Modifier.weight(1f).semantics { heading() })
                if (group.folded) Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (open) "Close" else "Open", tint = AppColors.muted)
            }
            if (group.note.isNotBlank() && open) {
                Text(group.note, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (open) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                Item(item, state)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Item(item: FormItem, state: RegisterStore.State) {
    val answers = state.answers
    val value = answers[item.key].orEmpty()
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val price = when (item.kind) {
            "quantity" -> item.price?.takeIf { it.signum() > 0 }?.let { "${formDollars(it)}${if (item.maxQuantity > 1) " each" else ""}" }
            "checkbox" -> item.price?.takeIf { it.signum() > 0 }?.let { formDollars(it) }
            else -> null
        }
        val left = when {
            item.taken -> if ((item.price?.signum() ?: 0) > 0 && item.maxQuantity == 1) "Already sponsored" else "All taken"
            item.remaining != null && item.remaining <= 10 -> "${item.remaining} left"
            else -> null
        }
        val title = item.name + if (item.required) " (required)" else ""
        if (item.kind == "checkbox") {
            Tick(listOfNotNull(title, price).joinToString(" · "), value == "true", enabled = !item.taken || value == "true") {
                RegisterStore.answer(item.key, it.toString())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                    color = AppColors.ink, modifier = Modifier.weight(1f))
                price?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink) }
            }
        }
        if (item.description.isNotBlank()) {
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        left?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark) }
        when (item.kind) {
            "quantity" -> if (!item.taken) {
                val count = value.toIntOrNull() ?: 0
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { RegisterStore.answer(item.key, (count - 1).coerceAtLeast(0).toString()) },
                        enabled = count > 0) { Text("−", color = AppColors.ink) }
                    Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    OutlinedButton(onClick = { RegisterStore.answer(item.key, (count + 1).coerceAtMost(item.most).toString()) },
                        enabled = count < item.most) { Text("+", color = AppColors.ink) }
                }
                if (item.detailLabel.isNotBlank()) {
                    for (n in 1..count) Field(state, "${item.key}_detail_$n", "${item.detailLabel} $n")
                }
            }
            "amount" -> Field(state, item.key,
                "Amount in dollars" + (item.minAmount?.takeIf { it > java.math.BigDecimal.ONE }?.let { " (at least ${formDollars(it)})" } ?: ""),
                KeyboardType.Decimal)
            "choice" -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.claySoft,
                        selectedLabelColor = AppColors.clayDark, labelColor = AppColors.ink)
                    item.amounts.forEach { amount ->
                        FilterChip(selected = value == amount,
                            onClick = { RegisterStore.answer(item.key, if (value == amount) "" else amount) },
                            label = { Text(formDollars(amount)) }, colors = colors)
                    }
                    if (item.other) {
                        FilterChip(selected = value == OTHER,
                            onClick = { RegisterStore.answer(item.key, if (value == OTHER) "" else OTHER) },
                            label = { Text("Other") }, colors = colors)
                    }
                }
                if (value == OTHER) Field(state, "${item.key}_other",
                    "Amount in dollars" + (item.minAmount?.let { " (at least ${formDollars(it)})" } ?: ""), KeyboardType.Decimal)
            }
        }
        if (item.kind != "amount") Errors(state, item.key)
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Question(q: FormQuestion, required: Boolean, state: RegisterStore.State) {
    val label = q.label + if (required) "" else " (optional)"
    val value = state.answers[q.key].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (q.kind) {
            "checkbox" -> {
                Tick(q.label, value == "true") { RegisterStore.answer(q.key, it.toString()) }
                Errors(state, q.key)
            }
            "choice" -> {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
                        placeholder = { Text("Choose…") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        isError = state.errors.containsKey(q.key),
                        supportingText = state.errors[q.key]?.let { { Text(it.joinToString(" ")) } },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        q.choices.forEach { choice ->
                            DropdownMenuItem(text = { Text(choice) }, onClick = {
                                RegisterStore.answer(q.key, choice)
                                expanded = false
                            })
                        }
                    }
                }
            }
            "date" -> {
                var picking by remember { mutableStateOf(false) }
                val shownDate = runCatching { LocalDate.parse(value).format(dayFormat) }.getOrNull().orEmpty()
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (shownDate.isNotEmpty()) "$label: $shownDate" else "$label: choose a date", color = AppColors.ink)
                }
                Errors(state, q.key)
                if (picking) {
                    val picker = rememberDatePickerState(
                        initialSelectedDateMillis = runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull())
                    DatePickerDialog(
                        onDismissRequest = { picking = false },
                        confirmButton = {
                            TextButton(onClick = {
                                picker.selectedDateMillis?.let {
                                    RegisterStore.answer(q.key, Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                                }
                                picking = false
                            }) { Text("OK", color = AppColors.clayDark) }
                        },
                        dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel", color = AppColors.ink) } },
                    ) { DatePicker(state = picker) }
                }
            }
            else -> Field(state, q.key, label, lines = if (q.kind == "textarea") 3 else 1)
        }
        if (q.help.isNotBlank()) Text(q.help, style = MaterialTheme.typography.bodySmall, color = AppColors.muted)
    }
}
