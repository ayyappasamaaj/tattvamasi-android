package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.housebhajan.HouseBhajanStore
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** My account, over any tab (opened from the person button on the banner): sign in, or the person's registrations,
 *  donations, this year's totals and (volunteers) volunteer hours, with Sign out and Delete my account. */
@Composable
fun AccountSheet() {
    val state by AccountStore.state.collectAsState()
    if (!state.shown) return
    // Back from the summary or the donations list goes to My account; from My account, out of it
    BackHandler { if (state.page != null) AccountStore.closePage() else AccountStore.close() }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            // Takes every touch, so none reaches the tab underneath
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = { if (state.page != null) AccountStore.closePage() else AccountStore.close() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text(when (state.page) {
                AccountStore.Page.SUMMARY -> "Year-End Summary"
                AccountStore.Page.DONATIONS -> "My Donations"
                AccountStore.Page.REGISTRATIONS -> "My Registrations"
                AccountStore.Page.REGISTRATION -> "Registration"
                AccountStore.Page.HOURS -> "My Volunteer Hours"
                AccountStore.Page.ADD_HOURS -> "Add Hours"
                AccountStore.Page.REPORT -> "Report"
                AccountStore.Page.ZELLE -> "Zelle Donations"
                AccountStore.Page.CHECKS -> "Check and Cash Donations"
                null -> "My Account"
            }, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ReadableWidth(maxWidth = 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        state.user == null -> SignInForm(state)
                        state.page == AccountStore.Page.SUMMARY -> YearSummaryPage(state)
                        state.page == AccountStore.Page.DONATIONS -> DonationsListPage(state)
                        state.page == AccountStore.Page.REGISTRATIONS -> RegistrationsListPage(state)
                        state.page == AccountStore.Page.REGISTRATION -> RegistrationPage(state)
                        state.page == AccountStore.Page.HOURS -> VolunteerHoursPage(state)
                        state.page == AccountStore.Page.ADD_HOURS -> AddHoursPage(state)
                        state.page == AccountStore.Page.REPORT -> ReportPage(state)
                        state.page == AccountStore.Page.ZELLE -> ZellePage()
                        state.page == AccountStore.Page.CHECKS -> ChecksPage()
                        else -> SignedIn(state)
                    }
                    AddressFooter()
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
private fun SignInForm(state: AccountStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }  // never kept past the screen
    val submit = {
        // Hide the keyboard, but leave focus on the field: moving it let a keyboard's Enter land on Back too and
        // close the screen (found on the Chromebook, Nov 24)
        keyboard?.hide()
        scope.launch { AccountStore.signIn(email, password) }
        Unit
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sign in with your ayyappasamaaj.org account to see your registrations, donations and year-end summary " +
            "here. Everything else in the app works without signing in.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        // Google's own account picker; a first-time Google sign-in makes the account (the website's sign-up limits)
        OutlinedButton(
            onClick = { keyboard?.hide(); scope.launch { AccountStore.signInWithGoogle(context) } },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Image(painterResource(R.drawable.google_g), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text("Continue with Google", color = AppColors.ink, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
        // Apple has no native sign-in on Android: the website's, in a Custom Tab (AppleSignIn.kt). Shown when the
        // website offers it; Apple's style: black, with the Apple logo
        LaunchedEffect(Unit) { AccountStore.checkApple() }
        if (state.appleOffered) {
            Button(
                onClick = { keyboard?.hide(); AccountStore.signInWithApple(context) },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(painterResource(R.drawable.apple_logo), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Text("Continue with Apple", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(color = AppColors.line, modifier = Modifier.weight(1f))
            Text("or with your email", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            HorizontalDivider(color = AppColors.line, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.confirmEmail) {
            Problem("Please confirm your email address first: open the link we emailed you when you made the " +
                "account, then sign in again.")
        }
        state.message?.let { Problem(it) }
        PrimaryButton(if (state.busy) "Signing In…" else "Sign In", enabled = !state.busy) { submit() }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { openWebPage(context, AccountApi.resetPasswordUrl) }) {
                Text("Forgot password?", color = AppColors.clayDark)
            }
            TextButton(onClick = { openWebPage(context, AccountApi.signUpUrl) }) {
                Text("Create an account", color = AppColors.clayDark)
            }
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun day(iso: String?): String =
    iso?.let { runCatching { LocalDate.parse(it.take(10)).format(dayFormat) }.getOrNull() } ?: ""

private fun dollars(amount: String) = amount.toBigDecimalOrNull()?.let { "$" + "%,.2f".format(it) } ?: amount

@Composable
private fun SignedIn(state: AccountStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = state.user ?: return
    LaunchedEffect(user) { if (state.overview == null) AccountStore.refresh() }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Swamiye Saranam", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
        if (user.name.isNotBlank() && user.email.isNotBlank()) {
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
        }
    }

    val overview = state.overview
    if (overview == null) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
        }
    } else {
        if (overview.failed) Problem("Some of your details couldn't be loaded. Please try again later.")
        nextRegistration(overview.registrations)?.let { NextEventCard(it) }
        overview.year?.let { year ->
            Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("${year.year} so far")
                Text(dollars(year.all), style = MaterialTheme.typography.headlineMedium, color = AppColors.ink)
                Text("Donations ${dollars(year.donations)} · Registrations ${dollars(year.registrations)}",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                TextButton(onClick = { AccountStore.openPage(AccountStore.Page.SUMMARY) }) {
                    Text("Year-end summary for taxes", color = AppColors.clayDark)
                }
            }
        }
        RegistrationsCard(overview.registrations)
        overview.hours?.takeIf { it.isVolunteer || it.entries.isNotEmpty() }?.let { HoursCard(it) }
        VolunteerToolsCard(overview.tools)
        // Not a volunteer (or just became one: the welcome): the website's double opt-in
        overview.signUp?.takeIf { !it.isVolunteer || state.volunteerMessage != null }?.let { VolunteerSignUpCard(it, state) }
        Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("My house bhajans")
            Text("Host an Ayyappa puja and bhajan at home on the Samaaj calendar, or change or cancel yours.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
            TextButton(onClick = { scope.launch { HouseBhajanStore.openMine() } }) {
                Text("See my house bhajans", color = AppColors.clayDark)
            }
        }
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("My donations", Modifier.padding(bottom = 6.dp))
            if (overview.donations.isEmpty()) {
                Text("No donations yet.", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
            overview.donations.take(10).forEachIndexed { index, d ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                        Text(listOfNotNull(day(d.date), d.donatedBy?.let { "by $it" }).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                    }
                    Text(dollars(d.amount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                        color = AppColors.ink)
                }
            }
            TextButton(onClick = { AccountStore.openPage(AccountStore.Page.DONATIONS) }) {
                Text("See all my donations", color = AppColors.clayDark)
            }
        }
    }

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { scope.launch { AccountStore.signOut() } }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out", color = AppColors.ink)
        }
        when (val answer = state.deletion) {
            is Deletion.Sent -> Problem("We emailed a link to ${answer.to}. Open it within ${answer.hours} hours and " +
                "press Delete My Account. Nothing is deleted until then.")
            is Deletion.Refused -> Problem(answer.message)
            Deletion.Failed -> Problem("The email couldn't be sent. Please try again later.")
            else -> {}
        }
        TextButton(onClick = { confirmDelete = true }, enabled = !state.busy) {
            Text("Delete my account", color = AppColors.clayDark)
        }
    }
    Spacer(Modifier.size(4.dp))

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Text("We'll email a link to ${user.email.ifBlank { "your address" }}. When you open it and press " +
                    "Delete My Account, your account and your details are deleted, and you're signed out " +
                    "everywhere. Donation records stay for 10 years, as tax law requires. It can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { AccountStore.requestDeletion() } }) {
                    Text("Email Me the Link", color = AppColors.clayDark)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = AppColors.ink) } },
        )
    }
}
