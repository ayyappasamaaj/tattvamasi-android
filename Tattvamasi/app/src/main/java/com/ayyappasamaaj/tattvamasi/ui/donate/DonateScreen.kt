package com.ayyappasamaaj.tattvamasi.ui.donate

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.launch

private fun dollars(amount: String) = amount.toBigDecimalOrNull()?.let { "$" + "%,.2f".format(it).removeSuffix(".00") } ?: amount

/** Donate, over any tab (from Home's Donate tile when the website has app giving on): a one-time gift from anyone,
 *  or a monthly gift for a signed-in member, paid in Stripe's payment sheet (card, Google Pay, Link). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonateSheet() {
    val state by DonateStore.state.collectAsState()
    if (!state.shown) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val account by AccountStore.state.collectAsState()
    val signedIn = account.user != null

    var monthly by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }       // the one-time amount, "108" or typed
    var other by rememberSaveable { mutableStateOf(false) }      // "Other" chosen: the box shows
    var monthlyId by rememberSaveable { mutableStateOf<Int?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var paying by remember { mutableStateOf<Pair<String, Boolean>?>(null) }  // amount, monthly: shown on the sheet

    val sheet = rememberPaymentSheet { result ->
        val (paidAmount, wasMonthly) = paying ?: ("" to false)
        paying = null
        when (result) {
            is PaymentSheetResult.Completed -> DonateStore.paid(paidAmount, wasMonthly)
            is PaymentSheetResult.Canceled -> {}
            is PaymentSheetResult.Failed ->
                DonateStore.problem(result.error.localizedMessage ?: "The payment didn't go through. Please try again.")
        }
    }

    LaunchedEffect(Unit) { if (state.options == null) DonateStore.load() }
    BackHandler { DonateStore.close() }

    fun present(secret: String, shown: String, isMonthly: Boolean) {
        val options = state.options ?: return
        PaymentConfiguration.init(context, options.publishableKey)
        paying = shown to isMonthly
        val billing = account.user?.let { PaymentSheet.BillingDetails(name = it.name, email = it.email) }
            ?: PaymentSheet.BillingDetails(name = name.trim(), email = email.trim())
        sheet.presentWithPaymentIntent(
            secret,
            PaymentSheet.Configuration.Builder("Ayyappa Samaaj")
                .defaultBillingDetails(billing)
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
            IconButton(onClick = { DonateStore.close() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text("Donate", style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            ReadableWidth(maxWidth = 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val options = state.options
                    when {
                        state.thanks != null -> Thanks(state.thanks!!) { DonateStore.close() }
                        options == null && !state.failed -> Row(Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
                        }
                        options == null || !options.enabled -> Column(Modifier.fillMaxWidth().card(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Give on our website: by card, Apple Pay or Google Pay, Zelle or check.",
                                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                            Primary("Donate on the Website") { openInBrowser(context, AccountApi.site + "/donate/") }
                        }
                        else -> {
                            Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Support Lord Ayyappa's seva. Ayyappa Samaaj is a 501(c)(3) nonprofit " +
                                    "(EIN 45-1061428): your gift is tax-deductible, and Stripe emails your receipt.",
                                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                    listOf(false to "One-time", true to "Monthly").forEachIndexed { index, (isMonthly, label) ->
                                        SegmentedButton(
                                            selected = monthly == isMonthly,
                                            onClick = { monthly = isMonthly; DonateStore.problem("") },
                                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                                            colors = SegmentedButtonDefaults.colors(activeContainerColor = AppColors.claySoft,
                                                activeContentColor = AppColors.clayDark, inactiveContainerColor = AppColors.card),
                                        ) { Text(label) }
                                    }
                                }
                                if (!monthly) {
                                    SectionLabel("Amount")
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        options.oneTime.forEach { preset ->
                                            Chip(dollars(preset), !other && amount == preset) { other = false; amount = preset }
                                        }
                                        Chip("Other", other) { other = true; amount = "" }
                                    }
                                    if (other) {
                                        OutlinedTextField(value = amount, onValueChange = { amount = it },
                                            label = { Text("Amount in dollars (${dollars(options.min)} to ${dollars(options.max)})") },
                                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
                                    }
                                    if (!signedIn) {
                                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") },
                                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                                        OutlinedTextField(value = email, onValueChange = { email = it },
                                            label = { Text("Email for your receipt") }, singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done))
                                    } else {
                                        Text("As ${account.user?.name?.ifBlank { null } ?: account.user?.email}: it goes on your account.",
                                            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                                    }
                                    state.message?.ifBlank { null }?.let { Problem(it) }
                                    val ready = amount.isNotBlank() && !state.busy
                                    Primary(if (state.busy) "Starting…" else if (amount.isBlank()) "Choose an Amount"
                                        else "Donate ${dollars(amount.removePrefix("$"))}", enabled = ready) {
                                        keyboard?.hide()
                                        scope.launch {
                                            when (val start = DonateStore.startOneTime(amount, name, email)) {
                                                is PayStart.Ready -> present(start.clientSecret, start.amount, false)
                                                is PayStart.Problem -> {}
                                            }
                                        }
                                    }
                                } else if (!signedIn) {
                                    Text("Monthly gifts are for signed-in members: each month's gift goes on your account and " +
                                        "in your year-end summary.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
                                    Primary("Sign In") { AccountStore.open() }
                                } else {
                                    SectionLabel("Every month")
                                    options.monthly.forEach { m ->
                                        Chip("${dollars(m.amount)} · ${m.category}", monthlyId == m.id, wide = true) { monthlyId = m.id }
                                    }
                                    if (options.monthly.isEmpty()) {
                                        Text("No monthly amounts are offered right now.", color = AppColors.muted)
                                    }
                                    state.message?.ifBlank { null }?.let { Problem(it) }
                                    val chosen = options.monthly.firstOrNull { it.id == monthlyId }
                                    Primary(if (state.busy) "Starting…" else chosen?.let { "Give ${dollars(it.amount)} Every Month" }
                                        ?: "Choose an Amount", enabled = chosen != null && !state.busy) {
                                        val pick = chosen ?: return@Primary
                                        scope.launch {
                                            when (val start = DonateStore.startMonthly(pick.id)) {
                                                is PayStart.Ready -> present(start.clientSecret, start.amount, true)
                                                is PayStart.Problem -> if (start.signIn) AccountStore.open()
                                            }
                                        }
                                    }
                                    // Neither the website nor the app can stop one yet (found Nov 25): the treasurer does it
                                    Text("To stop or change a monthly gift, email treasurer@ayyappasamaaj.org.",
                                        style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                                }
                            }
                            TextButton(onClick = { openInBrowser(context, AccountApi.site + "/donate/") }) {
                                Text("Prefer Zelle or a check? Give on our website", color = AppColors.clayDark)
                            }
                        }
                    }
                    AddressFooter()
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, wide: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.claySoft,
            selectedLabelColor = AppColors.clayDark, containerColor = AppColors.card),
        modifier = (if (wide) Modifier.fillMaxWidth() else Modifier).heightIn(min = 44.dp),
    )
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
private fun Thanks(text: String, done: () -> Unit) {
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Swamiye Saranam", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(600)), color = AppColors.ink,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Primary("Done", onClick = done)
    }
}
