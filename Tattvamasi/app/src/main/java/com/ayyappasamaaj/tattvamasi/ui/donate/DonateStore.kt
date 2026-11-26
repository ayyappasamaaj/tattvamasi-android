package com.ayyappasamaaj.tattvamasi.ui.donate

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Giving in the app (website: docs/app-donations.md). The website says what to offer (GET /api/donations/app/) and
 * creates the payment Stripe's payment sheet confirms (POST /api/donations/app/pay/); the gift is recorded by the
 * website exactly as for its own Donate page. When the website has app giving switched off, Donate opens the website.
 */
data class DonateOptions(
    val enabled: Boolean,
    val publishableKey: String = "",
    val min: String = "1",
    val max: String = "10000",
    val oneTime: List<String> = emptyList(),        // "108.00"
    val monthly: List<MonthlyAmount> = emptyList(),
    /** What a gift can be for ("GENERAL" to "General donation", ...); empty from an older website: no choice */
    val categories: List<Pair<String, String>> = emptyList(),
    val monthlyMin: String = "1",
    val monthlyMax: String = "10000",
)

/** [forWhat]: its category's handle ("ANNADANA"), or "" from an older website */
data class MonthlyAmount(val id: Int, val amount: String, val category: String, val forWhat: String = "")

sealed interface PayStart {
    data class Ready(val clientSecret: String, val amount: String) : PayStart
    data class Problem(val message: String, val signIn: Boolean = false) : PayStart
}

fun parseOptions(body: String): DonateOptions? = runCatching {
    val json = JSONObject(body)
    if (!json.optBoolean("enabled")) return@runCatching DonateOptions(enabled = false)
    val oneTime = json.optJSONArray("one_time") ?: JSONArray()
    val monthly = json.optJSONArray("monthly") ?: JSONArray()
    DonateOptions(
        enabled = true,
        publishableKey = json.getString("publishable_key"),
        min = json.optString("min", "1"),
        max = json.optString("max", "10000"),
        oneTime = (0 until oneTime.length()).mapNotNull { oneTime.optJSONObject(it)?.optString("amount") },
        monthly = (0 until monthly.length()).mapNotNull { monthly.optJSONObject(it) }.map {
            MonthlyAmount(it.getInt("id"), it.getString("amount"), it.optString("category"), it.optString("for"))
        },
        categories = (json.optJSONArray("categories") ?: JSONArray()).let { list ->
            (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { it.optString("handle") to it.optString("name") }
        },
        monthlyMin = json.optString("monthly_min", "1"),
        monthlyMax = json.optString("monthly_max", "10000"),
    )
}.getOrNull()

fun parsePayStart(code: Int, body: String): PayStart {
    if (code == 0) return PayStart.Problem("Couldn't reach ayyappasamaaj.org. Check your connection and try again.")
    val json = runCatching { JSONObject(body) }.getOrNull()
    val secret = json?.optString("client_secret").orEmpty()
    if (code == 200 && secret.isNotBlank()) return PayStart.Ready(secret, json?.optString("amount").orEmpty())
    val detail = json?.optString("detail").orEmpty().ifBlank { "Something went wrong. Please try again, or give on our website." }
    return PayStart.Problem(detail, signIn = code == 401)
}

object DonateStore {
    data class State(
        val shown: Boolean = false,
        val options: DonateOptions? = null,   // null while loading
        val failed: Boolean = false,          // couldn't load: offer the website
        val busy: Boolean = false,
        val message: String? = null,
        val thanks: String? = null,           // after a gift went through
        val zelle: Boolean = false,           // "I sent a Zelle gift" (ZelleReport.kt) is open
        val zelleForm: ZelleForm? = null,
        val zelleFailed: Boolean = false,
        val zelleErrors: Map<String, String> = emptyMap(),
        val zelleReported: ZelleReported? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun open() = _state.update { it.copy(shown = true, message = null, thanks = null, zelle = false) }
    fun close() = _state.update { it.copy(shown = false, zelle = false) }

    /** "I sent a Zelle gift" (ZelleReport.kt): a fresh form each time, with the memo code the website holds. */
    fun openZelle() = _state.update {
        it.copy(zelle = true, zelleForm = null, zelleFailed = false, zelleErrors = emptyMap(), zelleReported = null)
    }
    fun closeZelle() = _state.update { it.copy(zelle = false) }

    suspend fun loadZelle() {
        val token = AccountStore.sessionToken ?: return
        val reply = AccountApi.call("GET", "/api/donations/zelle/", token)
        if (reply.code == 401) return AccountStore.ended()
        val form = if (reply.code == 200) parseZelleForm(reply.body) else null
        _state.update { it.copy(zelleForm = form, zelleFailed = form == null) }
    }

    suspend fun reportZelle(amount: String, givenOn: String, category: Int, sender: String, memo: String, memoCode: String) {
        val token = AccountStore.sessionToken ?: return
        _state.update { it.copy(busy = true, zelleErrors = emptyMap()) }
        val body = JSONObject().put("amount", amount.trim().removePrefix("$").replace(",", "")).put("given_on", givenOn)
            .put("category", category).put("sender_name", sender.trim()).put("memo", memo.trim())
            .put("memo_code", memoCode.trim())
        val reply = try {
            AccountApi.call("POST", "/api/donations/zelle/", token, body)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        when (val result = parseZelleReport(reply.code, reply.body)) {
            ZelleResult.SignedOut -> AccountStore.ended()
            is ZelleResult.Problems -> _state.update { it.copy(zelleErrors = result.errors) }
            is ZelleResult.Reported -> _state.update { it.copy(zelleReported = result.gift) }
        }
    }

    /** Another one: a new form, with a new memo code. */
    suspend fun anotherZelle() {
        openZelle()
        loadZelle()
    }

    suspend fun load() {
        val reply = AccountApi.call("GET", "/api/donations/app/")
        val options = if (reply.code == 200) parseOptions(reply.body) else null
        _state.update { it.copy(options = options, failed = options == null) }
    }

    /** One-time: [name] and [email] are ignored when signed in (the website uses the account's). [category]: what it's
     *  for ("GENERAL", "ANNADANA"). */
    suspend fun startOneTime(amount: String, name: String, email: String, category: String = "GENERAL"): PayStart =
        start(JSONObject().put("amount", amount.trim().removePrefix("$").replace(",", ""))
            .put("name", name.trim()).put("email", email.trim()).put("category", category))

    suspend fun startMonthly(id: Int): PayStart = start(JSONObject().put("monthly", id))

    /** A monthly gift of the member's own amount (whole dollars) for [category]. */
    suspend fun startMonthlyOwn(dollars: String, category: String): PayStart =
        start(JSONObject().put("monthly_own", dollars.trim().removePrefix("$").replace(",", "")).put("category", category))

    private suspend fun start(body: JSONObject): PayStart {
        _state.update { it.copy(busy = true, message = null) }
        val reply = try {
            AccountApi.call("POST", "/api/donations/app/pay/", AccountStore.sessionToken, body)
        } finally {
            _state.update { it.copy(busy = false) }
        }
        return parsePayStart(reply.code, reply.body).also { result ->
            if (result is PayStart.Problem) _state.update { it.copy(message = result.message) }
        }
    }

    fun paid(amount: String, monthly: Boolean) = _state.update {
        it.copy(thanks = if (monthly) "Thank you! Your monthly gift of $${amount} has started. Stripe emails your " +
            "receipt, and it shows in My Account." else "Thank you for your gift of $${amount}! Stripe emails your " +
            "receipt, and it shows in My Account when you're signed in.", message = null)
    }

    fun problem(text: String) = _state.update { it.copy(message = text) }
}
