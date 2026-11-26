package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * "Sign up as a volunteer" in My Account, for a signed-in person who isn't one: the website's double opt-in, the same
 * as My profile's button on the website (volunteers/optin.py), through /api/volunteers/sign-up/. A confirmed address
 * makes them a volunteer at once; otherwise the website emails a link, and opening it (on the website) finishes the
 * sign-up. As the iOS app's VolunteerSignUp in VolunteerHours.swift.
 */
data class VolunteerSignUp(val isVolunteer: Boolean, val pending: Boolean, val email: String,
                           val emailVerified: Boolean)

sealed interface VolunteerSignUpResult {
    /** [outcome]: "volunteer" (one now), "sent" (the confirmation email went out) or "wait" (one went out a moment ago) */
    data class Done(val outcome: String, val message: String, val state: VolunteerSignUp) : VolunteerSignUpResult
    data class Failed(val message: String) : VolunteerSignUpResult
    data object SignedOut : VolunteerSignUpResult
}

fun parseVolunteerSignUp(body: String): VolunteerSignUp? = runCatching {
    val json = JSONObject(body)
    if (!json.has("is_volunteer")) return@runCatching null
    VolunteerSignUp(json.optBoolean("is_volunteer"), json.optBoolean("pending"), json.optString("email"),
        json.optBoolean("email_verified"))
}.getOrNull()

fun parseVolunteerSignUpAnswer(code: Int, body: String): VolunteerSignUpResult {
    if (code == 401) return VolunteerSignUpResult.SignedOut
    val state = if (code == 200) parseVolunteerSignUp(body) else null
    if (state != null) {
        val json = JSONObject(body)
        return VolunteerSignUpResult.Done(json.optString("outcome"), json.optString("message"), state)
    }
    return VolunteerSignUpResult.Failed(
        if (code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
        else "It couldn't be done just now. Please try again in a few minutes.")
}

/** The card's words, as My profile's Volunteering card on the website. */
fun volunteerSignUpText(s: VolunteerSignUp): String =
    if (s.pending) "Almost there: we sent an email to ${s.email}. Click the link in it to confirm your address and " +
        "finish signing up as a volunteer."
    else "Help at our pujas and events, and log your volunteer hours here." +
        (if (s.emailVerified) "" else " We'll first send you an email to confirm your address.")

@Composable
fun VolunteerSignUpCard(signUp: VolunteerSignUp, state: AccountStore.State) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Volunteering")
        val message = state.volunteerMessage
        if (signUp.isVolunteer) {
            // Just signed up: the welcome, and My volunteer hours shows above
            message?.let { Said(it) }
            return@Column
        }
        Text(volunteerSignUpText(signUp), style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        if (message != null && message != volunteerSignUpText(signUp)) Said(message)
        val busy = state.volunteerBusy
        if (signUp.pending) {
            OutlinedButton(onClick = { scope.launch { AccountStore.signUpAsVolunteer() } }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (busy) "Sending…" else "Send the Email Again", color = AppColors.ink,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            }
            // After opening the link on the website: the card goes and My volunteer hours shows
            TextButton(onClick = { scope.launch { AccountStore.refresh() } }) {
                Text("I've confirmed it", color = AppColors.clayDark)
            }
        } else {
            Button(
                onClick = { scope.launch { AccountStore.signUpAsVolunteer() } }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(if (busy) "Signing Up…" else "Sign Up as a Volunteer",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            }
        }
    }
}

@Composable
private fun Said(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.clayDark,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}
