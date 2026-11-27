package com.ayyappasamaaj.tattvamasi.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideScreen
import com.ayyappasamaaj.tattvamasi.ui.guides.GuidesStore
import com.ayyappasamaaj.tattvamasi.ui.guides.aboutGuide
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch

/** One of About > Contact's pages, opened over About (AppMenu.openContact). */
sealed interface ContactPage {
    /** Write to us: the website's contact form (ContactForms.kt) */
    data object WriteToUs : ContactPage
    /** Join the mailing list: the website's sign-up (ContactForms.kt) */
    data object MailingList : ContactPage
    /** A website footer page (Privacy policy and the like) read in the app: the guides feed's page [slug], tab "about" */
    data class Website(val slug: String) : ContactPage
}

/**
 * A Contact page over About, with Back to Contact: Write to us or Join the mailing list as the website's own forms
 * (ContactForms.kt), or a website page in the guide reader (ui/guides). As the iOS app's About > Contact
 * (AboutView.swift).
 */
@Composable
fun ContactPageScreen(page: ContactPage, onBack: () -> Unit) {
    when (page) {
        is ContactPage.Website -> WebsitePage(page.slug, onBack)
        ContactPage.WriteToUs -> FormPage("Write to us", onBack) { WriteToUs(onBack) }
        ContactPage.MailingList -> FormPage("Join the mailing list", onBack) { MailingList(onBack) }
    }
}

@Composable
private fun WebsitePage(slug: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val guides by GuidesStore.state.collectAsState()
    val guide = aboutGuide(slug, guides)
    if (guide != null) {
        GuideScreen(guide, null, onBack = onBack, backLabel = "Contact")
        return
    }
    // Not in the copy on the phone (shouldn't happen: the row checks first): the page on the website instead
    val url = contactLinks.firstOrNull { (it.action as? ContactAction.Page)?.slug == slug }?.url
        ?: "https://ayyappasamaaj.org/"
    LaunchedEffect(slug) {
        openWebPage(context, url)
        onBack()
    }
}

/** A form's page: Back and its title, then the form, scrolling, a readable width on a tablet. */
@Composable
private fun FormPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            ReadableWidth(maxWidth = 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                    AddressFooter()
                }
            }
        }
    }
}

/** The website's answer once sent: its sentence (read out by TalkBack), and Done back to Contact. */
@Composable
private fun Sent(title: String?, message: String, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        title?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        Text(message, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Primary("Done", onClick = onDone)
    }
}

/** A problem with the whole form (too many messages, no connection), under the fields. */
@Composable
private fun FormProblem(text: String?) {
    text ?: return
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

/** Write to us: the website's topics, name, email, phone (optional) and message; filled in when signed in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteToUs(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var form by remember { mutableStateOf<ContactForm?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        failed = false
        form = ContactApi.contactForm()
        failed = form == null
    }
    var topic by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var choosing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }

    done?.let {
        Sent(null, it, onDone)
        return
    }
    val loaded = form
    if (loaded == null) {
        Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (failed) NO_CONNECTION else "Loading…", style = MaterialTheme.typography.bodyLarge,
                color = AppColors.inkSoft, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (failed) {
                Primary("Try Again") { attempt++ }
                OutlinedButton(onClick = { openWebPage(context, "https://ayyappasamaaj.org/contact/") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Open on ayyappasamaaj.org", color = AppColors.ink)
                }
            }
        }
        return
    }
    // The website's defaults, once: its first topic, and the signed-in person's name and email
    LaunchedEffect(loaded) {
        if (loaded.topics.none { it.value == topic }) topic = loaded.topic
        if (name.isBlank()) name = loaded.name
        if (email.isBlank()) email = loaded.email
    }

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Whether you are wearing the mala for the first time, sponsoring a puja, or want to volunteer during " +
            "Mandalam, we are happy to help. We answer by email.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        ExposedDropdownMenuBox(expanded = choosing, onExpandedChange = { choosing = it }) {
            OutlinedTextField(
                value = loaded.topics.firstOrNull { it.value == topic }?.label.orEmpty(), onValueChange = {},
                readOnly = true, label = { Text("Topic") }, isError = errors["topic"] != null,
                supportingText = errors["topic"]?.let { { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(choosing) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                loaded.topics.forEach { t ->
                    DropdownMenuItem(text = { Text(t.label) }, onClick = { topic = t.value; choosing = false })
                }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it.take(150) }, label = { Text("Name") },
            singleLine = true, isError = errors["name"] != null, supportingText = errors["name"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it.take(254) }, label = { Text("Email") },
            singleLine = true, isError = errors["email"] != null, supportingText = errors["email"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it.take(30) }, label = { Text("Phone (optional)") },
            singleLine = true, isError = errors["phone"] != null, supportingText = errors["phone"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = message, onValueChange = { message = it.take(5000) }, label = { Text("Message") },
            minLines = 5, isError = errors["message"] != null, supportingText = errors["message"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth())
        // Any other field the website names (not one of the app's), with the whole form's problems
        FormProblem(errors.filterKeys { it !in setOf("topic", "name", "email", "phone", "message") }.values
            .joinToString("\n").ifBlank { null })
        Primary(if (busy) "Sending…" else "Send", enabled = !busy) {
            keyboard?.hide()
            busy = true
            scope.launch {
                when (val reply = ContactApi.sendMessage(name, email, phone, topic, message)) {
                    is FormReply.Done -> done = reply.message
                    is FormReply.Problems -> errors = reply.errors
                }
                busy = false
            }
        }
    }
}

/** Join the mailing list: an email (the signed-in person's, filled in) and a name, optional. */
@Composable
private fun MailingList(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val account by AccountStore.state.collectAsState()
    var email by rememberSaveable { mutableStateOf(account.user?.email.orEmpty()) }
    var name by rememberSaveable { mutableStateOf(account.user?.name.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }

    done?.let {
        Sent(
            when (status) {
                "sent" -> "Check your email"
                "subscribed" -> "You're on the list"
                else -> null
            },
            it, onDone,
        )
        return
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hear about our events, pujas and bhajans by email.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        OutlinedTextField(value = email, onValueChange = { email = it.take(254) }, label = { Text("Email") },
            singleLine = true, isError = errors["email"] != null, supportingText = errors["email"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it.take(150) }, label = { Text("Name (optional)") },
            singleLine = true, isError = errors["name"] != null, supportingText = errors["name"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth())
        FormProblem(errors.filterKeys { it != "email" && it != "name" }.values.joinToString("\n").ifBlank { null })
        Primary(if (busy) "Sending…" else "Join", enabled = !busy && email.isNotBlank()) {
            keyboard?.hide()
            busy = true
            scope.launch {
                when (val reply = ContactApi.joinMailingList(email, name)) {
                    is FormReply.Done -> { status = reply.status; done = reply.message }
                    is FormReply.Problems -> errors = reply.errors
                }
                busy = false
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
