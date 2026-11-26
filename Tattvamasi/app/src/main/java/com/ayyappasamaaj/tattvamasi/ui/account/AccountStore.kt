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
        val monthly: List<MonthlyGift> = emptyList(),
        val monthlyAmounts: List<MonthlyAmountChoice> = emptyList(),
        val monthlyFailed: Boolean = false,
        val stopping: String? = null,               // the monthly gift being stopped
        val monthlyMessage: String? = null,         // the last stop's outcome
        val registration: String? = null,           // the registration's code its page shows
        val registrationFrom: Page? = null,         // where its page was opened from (Back goes there)
        val hours: VolunteerHours? = null,          // My volunteer hours: the year shown
        val hoursFailed: Boolean = false,
        val hoursMessage: String? = null,           // "Added 2.5 hours of volunteering."
        val hoursForm: AddHoursForm? = null,        // Add hours: what it offers
        val hoursFormFailed: Boolean = false,
        val hoursErrors: Map<String, String> = emptyMap(),  // Add hours: the website's problems by field
        val reportPath: String? = null,             // a Volunteer tools report (Reports.kt): its API address
        val report: Report? = null,
        val reportFailed: Boolean = false,
    )

    enum class Page { SUMMARY, DONATIONS, REGISTRATIONS, REGISTRATION, HOURS, ADD_HOURS, REPORT, ZELLE, CHECKS }

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
    fun closePage() = _state.update {
        when (it.page) {
            Page.REGISTRATION -> it.copy(page = it.registrationFrom, registration = null)
            Page.ADD_HOURS -> it.copy(page = Page.HOURS, hoursErrors = emptyMap())
            else -> it.copy(page = null, hoursMessage = null)
        }
    }

    /** A Volunteer tools report (Reports.kt), from its API address (the website's my-pages "app_report"). */
    fun openReport(path: String) = _state.update {
        it.copy(page = Page.REPORT, reportPath = path, report = if (it.reportPath == path) it.report else null, reportFailed = false)
    }

    suspend fun loadReport() {
        val current = token ?: return
        val path = _state.value.reportPath ?: return
        _state.update { it.copy(loading = true, reportFailed = false) }
        val reply = try { AccountApi.call("GET", path, current) } finally { _state.update { it.copy(loading = false) } }
        if (reply.code == 401) return forget("Your sign-in has ended. Please sign in again.")
        val report = if (reply.code == 200) parseReport(reply.body) else null
        _state.update { if (it.reportPath == path) it.copy(report = report ?: it.report, reportFailed = report == null) else it }
    }

    /** Another year (or event's year) of the report shown. */
    suspend fun filterReport(name: String, value: String) {
        val path = _state.value.reportPath ?: return
        _state.update { it.copy(reportPath = reportPath(path, name, value)) }
        loadReport()
    }

    /** My volunteer hours of [year] (the year shown, or this year to start with). */
    suspend fun loadHours(year: Int? = null) {
        val current = token ?: return
        _state.update { it.copy(loading = true, hoursFailed = false) }
        val (code, hours) = try {
            AccountApi.volunteerHours(current, year ?: _state.value.hours?.year)
        } finally {
            _state.update { it.copy(loading = false) }
        }
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        _state.update { it.copy(hours = hours ?: it.hours, hoursFailed = hours == null) }
    }

    /** Add hours: opens its page and loads what it offers (dates, times, coordinators). */
    suspend fun openAddHours() {
        val current = token ?: return
        _state.update { it.copy(page = Page.ADD_HOURS, hoursErrors = emptyMap(), hoursMessage = null, hoursFormFailed = false) }
        val (code, form) = AccountApi.addHoursForm(current)
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        _state.update { it.copy(hoursForm = form ?: it.hoursForm, hoursFormFailed = form == null) }
    }

    /** Adds an entry; back on My volunteer hours (showing its year) with the website's message when it's added. */
    suspend fun addHours(date: String, start: String, end: String, activity: String, coordinator: Int?) {
        val current = token ?: return
        _state.update { it.copy(busy = true, hoursErrors = emptyMap()) }
        val result = try {
            AccountApi.addHours(current, date, start, end, activity, coordinator)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (result) {
            AddHoursResult.SignedOut -> forget("Your sign-in has ended. Please sign in again.")
            is AddHoursResult.Problems -> _state.update { it.copy(hoursErrors = result.errors) }
            is AddHoursResult.Added -> {
                _state.update { it.copy(page = Page.HOURS, hoursMessage = result.message) }
                loadHours(result.year)
                refresh()  // My account's card shows the new total
            }
        }
    }

    /** One registration's page (from My account or the full list). */
    fun openRegistration(code: String) = _state.update {
        it.copy(page = Page.REGISTRATION, registration = code, registrationFrom = it.page)
    }

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
        val monthly = if (page == 1) AccountApi.monthlyGifts(current).second else null
        _state.update {
            it.copy(donations = if (page == 1) answer.donations else it.donations + answer.donations,
                donationsPage = page, moreDonations = answer.more, zelle = zelle,
                monthly = monthly?.gifts ?: it.monthly, monthlyAmounts = monthly?.amounts ?: it.monthlyAmounts,
                monthlyFailed = page == 1 && monthly == null)
        }
    }

    /** Stops a monthly gift at once (website: donations/monthly.py); the donor gets an email. */
    suspend fun stopMonthlyGift(gift: MonthlyGift) {
        val current = token ?: return
        _state.update { it.copy(stopping = gift.id, monthlyMessage = null) }
        val (code, problem) = try {
            AccountApi.stopMonthlyGift(current, gift.id)
        } finally {
            _state.update { it.copy(stopping = null) }
        }
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        _state.update {
            if (problem == null) it.copy(monthly = it.monthly.filter { g -> g.id != gift.id },
                monthlyMessage = "Your monthly gift of ${summaryDollars(gift.amount).removeSuffix(".00")} has stopped. " +
                    "We've emailed you to confirm.")
            else it.copy(monthlyMessage = problem)
        }
    }

    /** Another amount and/or day for a monthly gift (website: donations/monthly.py change); the donor gets an email. */
    suspend fun changeMonthlyGift(gift: MonthlyGift, amountId: Int?, day: Int?) {
        val current = token ?: return
        val amount = amountId?.takeIf { it != gift.amountId }
        val newDay = day?.takeIf { it != gift.day }
        if (amount == null && newDay == null) return
        _state.update { it.copy(stopping = gift.id, monthlyMessage = null) }
        val (code, changed, problem) = try {
            AccountApi.changeMonthlyGift(current, gift.id, amount, newDay)
        } finally {
            _state.update { it.copy(stopping = null) }
        }
        if (code == 401) return forget("Your sign-in has ended. Please sign in again.")
        _state.update {
            if (changed != null) it.copy(monthly = it.monthly.map { g -> if (g.id == gift.id) changed else g },
                monthlyMessage = "Changed: ${summaryDollars(changed.amount).removeSuffix(".00")} every month" +
                    (changed.nextPayment?.let { d -> ", next on ${summaryDay(d)}" } ?: "") + ". We've emailed you to confirm.")
            else it.copy(monthlyMessage = problem)
        }
    }

    /** From Donate: My account's donations page, where monthly gifts are changed or stopped. */
    fun openDonations() = _state.update { it.copy(shown = true, page = Page.DONATIONS, deletion = null) }

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

    /** The website said the sign-in has ended (another screen got a 401). */
    fun ended() = forget("Your sign-in has ended. Please sign in again.")

    private fun forget(message: String?) {
        token = null
        store?.clear()
        _state.update { State(shown = it.shown, message = message) }
    }
}
