package com.ayyappasamaaj.tattvamasi.ui.housebhajan

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * The house bhajan pages over any tab (HouseBhajanSheet): a bhajan's page (from the calendar), the form to host one
 * or change one, and My house bhajans (from My Account). Pages stack: Back returns to the one before.
 */
object HouseBhajanStore {
    enum class Page { DETAIL, FORM, MINE }

    data class State(
        val pages: List<Page> = emptyList(),   // the pages open, last on top
        val busy: Boolean = false,
        // A bhajan's page
        val detail: HouseBhajan? = null,
        val detailFailed: Boolean = false,
        val detailSite: String = "",
        val messageSent: String? = null,
        val messageErrors: Map<String, String> = emptyMap(),
        // My house bhajans
        val mine: List<HouseBhajan>? = null,
        val mineFailed: Boolean = false,
        val canAdd: Boolean = true,
        val notice: String? = null,            // the last change's message, shown on My house bhajans
        // The form
        val formInfo: BhajanFormInfo? = null,
        val formFailed: Boolean = false,
        val editing: HouseBhajan? = null,      // null: a new one
        val errors: Map<String, String> = emptyMap(),
    ) {
        val page: Page? get() = pages.lastOrNull()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private fun push(page: Page) = _state.update { it.copy(pages = it.pages.filter { p -> p != page } + page) }
    fun back() = _state.update { it.copy(pages = it.pages.dropLast(1), errors = emptyMap(), messageErrors = emptyMap()) }
    fun close() = _state.update { State() }

    private fun signedOut() {
        AccountStore.ended()
        close()
        AccountStore.open()
    }

    /** A bhajan's page, from its calendar link. */
    suspend fun openDetail(url: String) {
        val id = bhajanId(url) ?: return
        val site = siteOf(url) ?: AccountApi.site
        _state.update { it.copy(detail = null, detailFailed = false, detailSite = site, messageSent = null, messageErrors = emptyMap()) }
        push(Page.DETAIL)
        loadDetail(site, id)
    }

    private suspend fun loadDetail(site: String, id: Int) {
        val (_, bhajan) = HouseBhajanApi.bhajan(site, id, AccountStore.sessionToken)
        _state.update { it.copy(detail = bhajan ?: it.detail, detailFailed = bhajan == null) }
    }

    /** Message the host (signed in): the website emails it, with the account's email to reply to. */
    suspend fun message(text: String, phone: String) {
        val token = AccountStore.sessionToken ?: return AccountStore.open()
        val id = _state.value.detail?.id ?: return
        _state.update { it.copy(busy = true, messageErrors = emptyMap(), messageSent = null) }
        val answer = try { HouseBhajanApi.message(token, id, text, phone) } finally { _state.update { it.copy(busy = false) } }
        when (answer) {
            is BhajanAnswer.Done -> _state.update { it.copy(messageSent = answer.message) }
            is BhajanAnswer.Problems -> _state.update { it.copy(messageErrors = answer.errors) }
            BhajanAnswer.SignedOut -> signedOut()
        }
    }

    /** My house bhajans (signed in). */
    suspend fun openMine() {
        if (AccountStore.sessionToken == null) return AccountStore.open()
        _state.update { it.copy(notice = null) }
        push(Page.MINE)
        loadMine()
    }

    suspend fun loadMine() {
        val token = AccountStore.sessionToken ?: return
        _state.update { it.copy(mineFailed = false) }
        val (code, answer) = HouseBhajanApi.mine(token)
        if (code == 401) return signedOut()
        _state.update {
            it.copy(mine = answer?.first ?: it.mine, canAdd = answer?.second ?: it.canAdd, mineFailed = answer == null)
        }
    }

    /** The form: a new bhajan ([editing] null) or a change to one of the host's own. Signed out: My Account first. */
    suspend fun openForm(editing: HouseBhajan? = null) {
        val token = AccountStore.sessionToken ?: return AccountStore.open()
        _state.update { it.copy(editing = editing, errors = emptyMap(), formFailed = false) }
        push(Page.FORM)
        val (code, info) = HouseBhajanApi.formInfo(token)
        if (code == 401) return signedOut()
        _state.update { it.copy(formInfo = info ?: it.formInfo, formFailed = info == null && it.formInfo == null) }
    }

    /** Sends the form; done, it goes to My house bhajans with the website's message. */
    suspend fun submit(fields: BhajanFields) {
        val token = AccountStore.sessionToken ?: return AccountStore.open()
        val editing = _state.value.editing
        _state.update { it.copy(busy = true, errors = emptyMap()) }
        val answer = try {
            if (editing == null) HouseBhajanApi.send(token, fields) else HouseBhajanApi.change(token, editing.id, fields)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (answer) {
            is BhajanAnswer.Problems -> _state.update { it.copy(errors = answer.errors) }
            BhajanAnswer.SignedOut -> signedOut()
            is BhajanAnswer.Done -> {
                _state.update { s ->
                    val below = s.pages.dropLast(1).filter { it != Page.MINE }
                    s.copy(pages = below + Page.MINE, notice = answer.message, editing = null)
                }
                loadMine()
            }
        }
    }

    suspend fun cancel(bhajan: HouseBhajan) {
        val token = AccountStore.sessionToken ?: return
        _state.update { it.copy(busy = true, notice = null) }
        val answer = try { HouseBhajanApi.cancel(token, bhajan.id) } finally { _state.update { it.copy(busy = false) } }
        when (answer) {
            is BhajanAnswer.Done -> { _state.update { it.copy(notice = answer.message) }; loadMine() }
            is BhajanAnswer.Problems -> _state.update { it.copy(notice = answer.errors.values.joinToString(" ")) }
            BhajanAnswer.SignedOut -> signedOut()
        }
    }

    /** A bhajan of the host's own, opened from My house bhajans. */
    suspend fun openOwn(bhajan: HouseBhajan) {
        _state.update { it.copy(detail = bhajan, detailFailed = false, detailSite = AccountApi.site, messageSent = null) }
        push(Page.DETAIL)
        loadDetail(AccountApi.site, bhajan.id)
    }
}
