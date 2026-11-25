package com.ayyappasamaaj.tattvamasi.ui.register

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A registration form in the app (website: docs/app-forms.md, signups/app_forms.py; the apps doc's Phase 4). The
 * website describes the form (GET /api/signups/forms/<slug>/), checks the answers with its own web form and makes
 * the registration (POST …/submit/), and says when it's paid (GET …/status/<token>/). The app only draws it, keeps
 * the answers under the web form's names, and shows the total as it will be; the website works everything out again.
 *
 * Plain values and functions, unit-tested with staging's answers (RegisterFormTest). The same as the iOS app's
 * RegisterForm.swift.
 */
data class FormGroup(val name: String, val note: String, val folded: Boolean)

data class FormItem(
    val id: Int,
    val group: String,
    val name: String,
    val description: String,
    val kind: String,              // quantity, checkbox, amount, choice
    val price: BigDecimal?,
    val maxQuantity: Int,
    val amounts: List<String>,     // choice: sent back exactly as given
    val other: Boolean,            // choice: "Other amount" with a box
    val minAmount: BigDecimal?,
    val remaining: Int?,           // null: no limit
    val detailLabel: String,       // quantity: one detail per unit, e.g. "Child's first name"
    val required: Boolean,
) {
    val key get() = "item_$id"
    /** The most one person can choose now. */
    val most get() = remaining?.let { minOf(it, maxQuantity) } ?: maxQuantity
    val taken get() = remaining == 0
}

data class FormQuestion(
    val id: Int,
    val label: String,
    val kind: String,              // text, textarea, date, choice, checkbox
    val required: Boolean,
    val help: String,
    val choices: List<String>,
    val requiredWith: List<Int>,
    val onlyWhenChosen: Boolean,
) {
    val key get() = "field_$id"
}

data class RegisterForm(
    val slug: String,
    val title: String,
    val intro: String,
    val state: String,             // open, not_open, full, closed
    val website: String,
    val phone: String,             // off, optional, required
    val askWhatsapp: Boolean,
    val offerFeeCover: Boolean,
    val feePercent: BigDecimal,
    val feeFixed: BigDecimal,
    val requireItem: Boolean,
    val holdMinutes: Int,
    val supported: Boolean,
    val groups: List<FormGroup>,
    val items: List<FormItem>,
    val questions: List<FormQuestion>,
    val prefillName: String,
    val prefillEmail: String,
)

/** What the website said to the answers. */
sealed interface Submitted {
    data class Confirmed(val code: String, val token: String, val total: String) : Submitted
    data class Pay(val code: String, val token: String, val total: String, val clientSecret: String,
                   val publishableKey: String, val holdMinutes: Int) : Submitted
    /** Field by field, under the web form's names; "__all__" for the form as a whole. */
    data class Problems(val errors: Map<String, List<String>>) : Submitted
    data class Refused(val message: String) : Submitted
}

data class RegistrationStatus(val code: String, val confirmed: Boolean, val total: String, val title: String,
                              val thankYou: String, val whatsappUrl: String, val whatsappNote: String)

/** The slug of a registration link the website gave the app: ".../forms/mandalam-2026/" → "mandalam-2026". */
fun formSlug(url: String): String? = Regex("""/forms/([a-z0-9_-]+)/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    .find(url)?.groupValues?.get(1)

private fun JSONObject.money(name: String): BigDecimal? = if (isNull(name)) null else optString(name).toBigDecimalOrNull()

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }

private fun JSONArray?.ints(): List<Int> =
    if (this == null) emptyList() else (0 until length()).map { optInt(it) }

fun parseForm(body: String): RegisterForm? = runCatching {
    val json = JSONObject(body)
    val fee = json.optJSONObject("fee") ?: JSONObject()
    val prefill = json.optJSONObject("prefill") ?: JSONObject()
    RegisterForm(
        slug = json.getString("slug"),
        title = json.optString("title"),
        intro = json.optString("intro"),
        state = json.optString("state", "closed"),
        website = json.optString("website"),
        phone = json.optString("phone", "off"),
        askWhatsapp = json.optBoolean("ask_whatsapp"),
        offerFeeCover = json.optBoolean("offer_fee_cover"),
        feePercent = fee.optString("percent").toBigDecimalOrNull() ?: BigDecimal("0.029"),
        feeFixed = fee.optString("fixed").toBigDecimalOrNull() ?: BigDecimal("0.30"),
        requireItem = json.optBoolean("require_item", true),
        holdMinutes = json.optInt("hold_minutes", 30),
        supported = json.optBoolean("supported"),
        groups = json.optJSONArray("groups").objects().map {
            FormGroup(it.optString("name"), it.optString("note"), it.optBoolean("folded"))
        },
        items = json.optJSONArray("items").objects().map {
            FormItem(
                id = it.getInt("id"), group = it.optString("group"), name = it.optString("name"),
                description = it.optString("description"), kind = it.optString("kind"), price = it.money("price"),
                maxQuantity = it.optInt("max_quantity", 1), amounts = it.optJSONArray("amounts").strings(),
                other = it.optBoolean("other"), minAmount = it.money("min_amount"),
                remaining = if (it.isNull("remaining")) null else it.optInt("remaining"),
                detailLabel = it.optString("detail_label"), required = it.optBoolean("required"),
            )
        },
        questions = json.optJSONArray("fields").objects().map {
            FormQuestion(
                id = it.getInt("id"), label = it.optString("label"), kind = it.optString("kind"),
                required = it.optBoolean("required"), help = it.optString("help"),
                choices = it.optJSONArray("choices").strings(), requiredWith = it.optJSONArray("required_with").ints(),
                onlyWhenChosen = it.optBoolean("only_when_chosen"),
            )
        },
        prefillName = prefill.optString("name"),
        prefillEmail = prefill.optString("email"),
    )
}.getOrNull()

/** The app can draw every part of it (else it opens the website). */
fun drawable(form: RegisterForm): Boolean = form.supported &&
    form.items.all { it.kind in setOf("quantity", "checkbox", "amount", "choice") } &&
    form.questions.all { it.kind in setOf("text", "textarea", "date", "choice", "checkbox") }

const val OTHER = "other"

/**
 * The person's answers, under the web form's names: text as typed, "true" for a ticked box, a quantity as a
 * number, a choice's amount as given (or "other").
 */
typealias Answers = Map<String, String>

private fun Answers.ticked(key: String) = this[key] == "true"

/** What one item comes to with these answers (zero when not chosen or not a number yet). */
fun itemAmount(item: FormItem, answers: Answers): BigDecimal {
    val value = answers[item.key].orEmpty().trim()
    return when (item.kind) {
        "quantity" -> (item.price ?: BigDecimal.ZERO) * (value.toIntOrNull() ?: 0).toBigDecimal()
        "checkbox" -> if (value == "true") item.price ?: BigDecimal.ZERO else BigDecimal.ZERO
        "amount" -> value.removePrefix("$").replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
        "choice" -> if (value == OTHER) answers["${item.key}_other"].orEmpty().trim().removePrefix("$").replace(",", "")
            .toBigDecimalOrNull() ?: BigDecimal.ZERO else value.toBigDecimalOrNull() ?: BigDecimal.ZERO
        else -> BigDecimal.ZERO
    }.max(BigDecimal.ZERO)
}

fun chosen(item: FormItem, answers: Answers): Boolean {
    val value = answers[item.key].orEmpty().trim()
    return when (item.kind) {
        "quantity" -> (value.toIntOrNull() ?: 0) > 0
        "checkbox" -> value == "true"
        else -> itemAmount(item, answers).signum() > 0
    }
}

/** The web form's processing fee: what to add so the Samaaj receives the total after Stripe's fee. */
fun processingFee(form: RegisterForm, total: BigDecimal): BigDecimal {
    if (total.signum() <= 0) return BigDecimal("0.00")
    val gross = (total + form.feeFixed).divide(BigDecimal.ONE - form.feePercent, 10, RoundingMode.HALF_UP)
    return (gross - total).setScale(2, RoundingMode.HALF_UP)
}

data class Totals(val items: BigDecimal, val fee: BigDecimal, val total: BigDecimal)

fun totals(form: RegisterForm, answers: Answers): Totals {
    val items = form.items.fold(BigDecimal.ZERO) { sum, item -> sum + itemAmount(item, answers) }.setScale(2, RoundingMode.HALF_UP)
    val fee = if (form.offerFeeCover && answers.ticked("cover_fee")) processingFee(form, items) else BigDecimal("0.00")
    return Totals(items, fee, items + fee)
}

/** Shown only when one of its items is chosen (a question with "only when chosen"). */
fun shown(question: FormQuestion, form: RegisterForm, answers: Answers): Boolean =
    !question.onlyWhenChosen || form.items.any { it.id in question.requiredWith && chosen(it, answers) }

/** Required now: always, or because one of its items is chosen. */
fun needed(question: FormQuestion, form: RegisterForm, answers: Answers): Boolean =
    question.required || form.items.any { it.id in question.requiredWith && chosen(it, answers) }

/** The answers as the website takes them: numbers and true/false where the web form expects them. */
fun answersJson(form: RegisterForm, answers: Answers): JSONObject {
    val json = JSONObject()
    json.put("name", answers["name"].orEmpty().trim())
    json.put("email", answers["email"].orEmpty().trim())
    if (form.phone != "off") {
        json.put("phone", answers["phone"].orEmpty().trim())
        if (form.askWhatsapp) {
            val same = answers["phone_on_whatsapp"] != "false"  // ticked to start with, as on the website
            json.put("phone_on_whatsapp", same)
            if (!same) json.put("whatsapp", answers["whatsapp"].orEmpty().trim())
        }
    }
    if (form.offerFeeCover) json.put("cover_fee", answers.ticked("cover_fee"))
    for (item in form.items) {
        val value = answers[item.key].orEmpty().trim()
        when (item.kind) {
            "quantity" -> {
                val count = value.toIntOrNull() ?: 0
                json.put(item.key, count)
                if (item.detailLabel.isNotBlank()) {
                    for (n in 1..count) json.put("${item.key}_detail_$n", answers["${item.key}_detail_$n"].orEmpty().trim())
                }
            }
            "checkbox" -> json.put(item.key, value == "true")
            "amount" -> json.put(item.key, value.removePrefix("$").replace(",", ""))
            "choice" -> {
                json.put(item.key, value)
                if (value == OTHER) json.put("${item.key}_other",
                    answers["${item.key}_other"].orEmpty().trim().removePrefix("$").replace(",", ""))
            }
        }
    }
    for (q in form.questions) {
        if (!shown(q, form, answers)) continue
        if (q.kind == "checkbox") json.put(q.key, answers.ticked(q.key)) else json.put(q.key, answers[q.key].orEmpty().trim())
    }
    return json
}

private const val OFFLINE = "Couldn't reach ayyappasamaaj.org. Check your connection and try again."

fun parseSubmitted(code: Int, body: String): Submitted {
    if (code == 0) return Submitted.Refused(OFFLINE)
    val json = runCatching { JSONObject(body) }.getOrNull()
        ?: return Submitted.Refused("Something went wrong. Please try again, or register on our website.")
    val errors = json.optJSONObject("errors")
    if (errors != null) {
        return Submitted.Problems(errors.keys().asSequence().associateWith { key -> errors.optJSONArray(key).strings() })
    }
    if (code == 200) {
        val total = json.optString("total")
        when (json.optString("status")) {
            "confirmed" -> return Submitted.Confirmed(json.optString("code"), json.optString("token"), total)
            "pay" -> {
                val secret = json.optString("client_secret")
                if (secret.isNotBlank()) return Submitted.Pay(json.optString("code"), json.optString("token"), total,
                    secret, json.optString("publishable_key"), json.optInt("hold_minutes", 30))
            }
        }
    }
    return Submitted.Refused(json.optString("detail").ifBlank { "Something went wrong. Please try again, or register on our website." })
}

fun parseStatus(body: String): RegistrationStatus? = runCatching {
    val json = JSONObject(body)
    RegistrationStatus(
        code = json.getString("code"), confirmed = json.optBoolean("confirmed"), total = json.optString("total"),
        title = json.optString("title"), thankYou = json.optString("thank_you"),
        whatsappUrl = json.optString("whatsapp_group_url"), whatsappNote = json.optString("whatsapp_group_note"),
    )
}.getOrNull()

/** "$27", "$25.50", "$1,250". */
fun formDollars(amount: BigDecimal?): String =
    amount?.let { "$" + "%,.2f".format(it).removeSuffix(".00") } ?: ""

fun formDollars(amount: String): String = amount.toBigDecimalOrNull()?.let { formDollars(it) } ?: amount
