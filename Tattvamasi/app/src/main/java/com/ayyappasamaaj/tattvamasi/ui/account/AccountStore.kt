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
        val page: Page? = null,              // a page over My account: the year-end summary or all donations
        val summary: YearSummary? = null,
        val summaryFailed: Boolean = false,
        val donations: List<MyDonation> = emptyList(),
        val donationsPage: Int = 0,          // pages read so far
        val moreDonations: Boolean = false,
        val donationsFailed: Boolean = false,
        val zelle: List<ZelleGift> = emptyList(),
    )

    enum class Page { SUMMARY, DONATIONS }

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
    fun close() = _state.update { it.copy(shown = false, message = null, page = null) }

    fun openPage(page: Page) = _state.update { it.copy(page = page) }
    fun closePage() = _state.update { it.copy(page = null) }

    /** The year-end summary of [year] (this year to start with) for "me" or "household". */
    suspend fun loadSummary(year: Int? = null, scope: String? = null) {
        val current = token ?: return
        val shown = _state.value.summary
        _state.update { it.copy(loading = true, summaryFailed = false) }
        val (code, summary) = try {
            AccountApi.summary(current, year ?: shown?.year ?: LocalDate.now().year, scope ?: shown?.scope ?: "me")
        } finally {
            _state.update { it.copy(loading = false) }
        }
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        _state.update { it.copy(summary = summary ?: it.summary, summaryFailed = summary == null) }
    }

    /** All donations: the first page (with the Zelle gifts waiting), or the next one. */
    suspend fun loadDonations(next: Boolean = false) {
        val current = token ?: return
        val page = if (next) _state.value.donationsPage + 1 else 1
        _state.update { it.copy(loading = true, donationsFailed = false) }
        val (code, answer) = try {
            AccountApi.donations(current, page)
        } finally {
            _state.update { it.copy(loading = false) }
        }
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        if (answer == null) return _state.update { it.copy(donationsFailed = true) }
        val zelle = if (page == 1) AccountApi.zelleGifts(current) else _state.value.zelle
        _state.update {
            it.copy(donations = if (page == 1) answer.donations else it.donations + answer.donations,
                donationsPage = page, moreDonations = answer.more, zelle = zelle)
        }
    }

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
