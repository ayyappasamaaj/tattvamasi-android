package com.ayyappasamaaj.tattvamasi.ui.register

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * A registration form open over the app (RegisterForm.kt): opened from an event's Register button. Loads the form,
 * keeps the answers, sends them, and after Stripe's payment sheet asks the website until the payment is recorded.
 */
object RegisterStore {
    data class State(
        val shown: Boolean = false,
        val slug: String = "",
        val link: String = "",                       // the website's form, if the app can't show it
        val form: RegisterForm? = null,              // null while loading
        val failed: Boolean = false,                 // couldn't load, or the app can't draw it: offer the website
        val answers: Answers = emptyMap(),
        val errors: Map<String, List<String>> = emptyMap(),
        val message: String? = null,
        val busy: Boolean = false,
        val pay: Submitted.Pay? = null,              // made and waiting for the payment sheet
        val done: RegistrationStatus? = null,        // confirmed: the thank-you
        val checking: Boolean = false,               // paid; waiting for the website to record it
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun open(slug: String, link: String) {
        if (_state.value.shown && _state.value.slug == slug) return
        _state.value = State(shown = true, slug = slug, link = link)
    }

    fun close() = _state.update { it.copy(shown = false) }

    suspend fun load() {
        val slug = _state.value.slug
        val reply = AccountApi.call("GET", "/api/signups/forms/$slug/", AccountStore.sessionToken)
        val form = if (reply.code == 200) parseForm(reply.body) else null
        _state.update {
            if (it.slug != slug) return@update it
            val start = buildMap {
                put("name", form?.prefillName.orEmpty())
                put("email", form?.prefillEmail.orEmpty())
                put("phone_on_whatsapp", "true")
            }
            it.copy(form = form, failed = form == null || !drawable(form) || form.state != "open",
                answers = it.answers.ifEmpty { start })
        }
    }

    /** A changed answer after the registration was made: it's sent again (and made again) before paying. */
    fun answer(key: String, value: String) = _state.update {
        it.copy(answers = it.answers + (key to value), errors = it.errors - key, pay = null,
            message = if (it.pay != null) null else it.message)
    }

    fun startOver() = _state.update { it.copy(pay = null, message = null) }

    /** Sends the answers: free → the thank-you; paid → [State.pay] for the payment sheet; else the problems. */
    suspend fun submit() {
        val form = _state.value.form ?: return
        _state.update { it.copy(busy = true, message = null) }
        val reply = try {
            AccountApi.call("POST", "/api/signups/forms/${form.slug}/submit/", AccountStore.sessionToken,
                answersJson(form, _state.value.answers))
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (val result = parseSubmitted(reply.code, reply.body)) {
            is Submitted.Confirmed -> finish(result.token)
            is Submitted.Pay -> _state.update { it.copy(pay = result, errors = emptyMap()) }
            is Submitted.Problems -> _state.update {
                it.copy(errors = result.errors, message = result.errors["__all__"]?.joinToString(" ")
                    ?: "Please check the answers marked in red above.")
            }
            is Submitted.Refused -> _state.update { it.copy(message = result.message) }
        }
    }

    /** The payment sheet closed without paying: the registration waits [holdMinutes] for another try. */
    fun notPaid(problem: String?) = _state.update {
        val minutes = it.pay?.holdMinutes ?: 30
        it.copy(message = (problem?.let { p -> "$p " } ?: "") +
            "Not paid yet: your place is held for $minutes minutes. Tap Pay to try again.")
    }

    /** Paid in the sheet: the website records it from Stripe's notice (or asks Stripe); wait for that briefly. */
    suspend fun paid() {
        val token = _state.value.pay?.token ?: return
        _state.update { it.copy(checking = true, message = null) }
        finish(token)
    }

    private suspend fun finish(token: String) {
        val slug = _state.value.slug
        var status: RegistrationStatus? = null
        for (attempt in 0 until 6) {
            val reply = AccountApi.call("GET", "/api/signups/forms/$slug/status/$token/", AccountStore.sessionToken)
            status = if (reply.code == 200) parseStatus(reply.body) else status
            if (status?.confirmed == true) break
            delay(2000)
        }
        _state.update {
            if (status?.confirmed == true) it.copy(done = status, checking = false, pay = null)
            else it.copy(checking = false, message = "Your payment went through. The confirmation is on its way by " +
                "email; it can take a minute to show here.", done = status?.copy(confirmed = true))
        }
    }
}
