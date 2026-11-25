package com.ayyappasamaaj.tattvamasi.ui.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * The person's account in the app: signed out, or signed in with the website's session token (AccountApi), and
 * what My account shows. Signing in is optional: nothing else in the app needs it.
 */
object AccountStore {
    data class State(
        val shown: Boolean = false,          // My account is open (over any tab)
        val user: AccountUser? = null,       // signed in as
        val busy: Boolean = false,
        val message: String? = null,         // the last sign-in problem, shown under the form
        val confirmEmail: Boolean = false,   // signed up but the address isn't confirmed yet
        val overview: Overview? = null,
        val loading: Boolean = false,
        val deletion: Deletion? = null,      // the answer to Delete my account
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private var store: TokenStore? = null
    private var token: String? = null

    /** The website's session token while signed in (for Donate's own calls, ui/donate), else null. */
    val sessionToken: String? get() = token

    /** Once, when the app starts: signed in from last time, if the token is still there. */
    fun init(context: Context) {
        if (store != null) return
        val tokens = TokenStore(context.applicationContext)
        store = tokens
        tokens.load()?.let { saved ->
            token = saved.token
            _state.update { it.copy(user = saved.user) }
        }
    }

    fun open() = _state.update { it.copy(shown = true, deletion = null) }
    fun close() = _state.update { it.copy(shown = false, message = null) }

    suspend fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(message = "Enter your email and password.") }
            return
        }
        finish { AccountApi.signIn(email, password) }
    }

    /** Continue with Google: Google's account picker, then the website (a new account follows its sign-up limits). */
    suspend fun signInWithGoogle(activity: Context) {
        _state.update { it.copy(busy = true, message = null, confirmEmail = false) }
        val google = try {
            GoogleSignIn.idToken(activity)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (google) {
            is GoogleSignIn.Result.Token -> finish { AccountApi.signInWithGoogle(google.idToken) }
            GoogleSignIn.Result.Cancelled -> {}
            is GoogleSignIn.Result.Problem -> _state.update { it.copy(message = google.message) }
        }
    }

    private suspend fun finish(call: suspend () -> SignIn) {
        _state.update { it.copy(busy = true, message = null, confirmEmail = false) }
        // Whatever happens (closing My account cancels the call), the button doesn't stay on "Signing In…"
        val result = try {
            call()
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (result) {
            is SignIn.Done -> {
                token = result.token
                store?.save(result.token, result.user)
                _state.update { it.copy(busy = false, user = result.user, overview = null) }
                refresh()
            }
            SignIn.ConfirmEmail -> _state.update { it.copy(busy = false, confirmEmail = true) }
            is SignIn.Refused -> _state.update { it.copy(busy = false, message = result.message) }
            SignIn.Offline -> _state.update {
                it.copy(busy = false, message = "Couldn't reach ayyappasamaaj.org. Check your connection and try again.")
            }
        }
    }

    /** My account's lists: registrations, donations and this year's totals. */
    suspend fun refresh() {
        val current = token ?: return
        _state.update { it.copy(loading = true) }
        val overview = try {
            AccountApi.overview(current, LocalDate.now().year)
        } finally {
            _state.update { it.copy(loading = false) }
        }
        if (overview == null) {
            forget("Your sign-in has ended. Please sign in again.")
            return
        }
        // The name the person gave (the sign-in answer has the username), kept for next time
        val user = _state.value.user?.let { u -> overview.name?.let { u.copy(name = it) } ?: u }
        if (user != null && user != _state.value.user) store?.save(current, user)
        _state.update { it.copy(loading = false, overview = overview, user = user ?: it.user) }
    }

    suspend fun signOut() {
        token?.let { AccountApi.signOut(it) }  // ends it on the website too; signed out here whatever the answer
        forget(null)
    }

    suspend fun requestDeletion() {
        val current = token ?: return
        _state.update { it.copy(busy = true, deletion = null) }
        val answer = try {
            AccountApi.requestDeletion(current)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        if (answer == Deletion.SignedOut) {
            forget("Your sign-in has ended. Please sign in again.")
            return
        }
        _state.update { it.copy(busy = false, deletion = answer) }
    }

    private fun forget(message: String?) {
        token = null
        store?.clear()
        _state.update { State(shown = it.shown, message = message) }
    }
}
