package com.ayyappasamaaj.tattvamasi.ui.about

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import org.json.JSONArray
import org.json.JSONObject

/*
 * About > Contact's two forms, the website's own (website: pages/contact.py and pages/mailing_list.py, through
 * /api/pages/contact/ and /api/pages/mailing-list/): Write to us (a topic, name, email, phone and message, emailed
 * to the Samaaj) and Join the mailing list (an email and a name; the website sends its confirmation email). The
 * website checks everything and gives its own words for problems and for the outcome; the app shows them. Signed in,
 * the website's session token goes along (the website fills in the person's name and email). Debug builds use
 * staging, as AccountApi does. As the iOS app's About > Contact (AboutView.swift).
 *
 * The parse functions are plain, so they're unit-tested with answers shaped as the website's (ContactFormsTest).
 */

/** A Write to us topic: the website's value and its label ("Sponsoring a puja"). */
data class ContactTopic(val value: String, val label: String)

/** What Write to us offers: the website's topics in order, the topic chosen at first, and the signed-in person's
 *  name and email ("" when signed out). */
data class ContactForm(val topics: List<ContactTopic>, val topic: String, val name: String, val email: String)

/** The website's answer to a form. */
sealed interface FormReply {
    /** Sent: the website's sentence to show; [status] for the mailing list ("sent": a confirmation email went out,
     *  "subscribed": done at once), "" for Write to us. */
    data class Done(val message: String, val status: String = "") : FormReply

    /** The form's problems by field (name, email, phone, topic, message); "" for the whole form (too many messages,
     *  no connection). */
    data class Problems(val errors: Map<String, String>) : FormReply
}

const val NO_CONNECTION = "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
private const val NOT_NOW = "It couldn't be sent just now. Please try again in a few minutes, or write to " +
    "info@ayyappasamaaj.org."

/** Write to us's GET answer, or null if it isn't usable (no topics). */
fun parseContactForm(body: String): ContactForm? = runCatching {
    val json = JSONObject(body)
    val list = json.getJSONArray("topics")
    val topics = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
        .map { ContactTopic(it.optString("value"), it.optString("label").ifBlank { it.optString("value") }) }
        .filter { it.value.isNotBlank() }
    if (topics.isEmpty()) return@runCatching null
    val topic = json.optString("topic").takeIf { t -> topics.any { it.value == t } } ?: topics.first().value
    ContactForm(topics, topic, json.optString("name"), json.optString("email"))
}.getOrNull()

/** A field's error text: the website's string (or the first of a list of them). */
private fun errorText(value: Any?): String = when (value) {
    is JSONArray -> (0 until value.length()).map { value.opt(it) }.joinToString(" ") { errorText(it) }.trim()
    is JSONObject -> value.optString("message")
    null, JSONObject.NULL -> ""
    else -> value.toString()
}

/** A form's answer: 200 with the website's sentence, 400 with its problems by field, 429 (too many) with its reason,
 *  or no connection (code 0). */
fun parseFormReply(code: Int, body: String): FormReply {
    val json = runCatching { JSONObject(body) }.getOrNull()
    if (code == 200 && json != null) {
        val message = json.optString("message")
        if (message.isNotBlank()) return FormReply.Done(message, json.optString("status"))
    }
    val errors = json?.optJSONObject("errors")?.let { e ->
        e.keys().asSequence().associateWith { errorText(e.opt(it)) }.filterValues { it.isNotBlank() }
    }
    // The website's whole-form problems ("__all__", "non_field_errors") show under the form
    if (!errors.isNullOrEmpty()) {
        return FormReply.Problems(errors.mapKeys { (key, _) -> if (key == "__all__" || key == "non_field_errors") "" else key })
    }
    val reason = json?.optString("error").orEmpty().ifBlank {
        if (code == 0) NO_CONNECTION else NOT_NOW
    }
    return FormReply.Problems(mapOf("" to reason))
}

object ContactApi {
    /** The website's call, with the session token when signed in; an ended token (401) is tried again without it,
     *  since neither form needs signing in. */
    private suspend fun call(method: String, path: String, body: JSONObject? = null): AccountApi.Reply {
        val token = AccountStore.sessionToken
        val reply = AccountApi.call(method, path, token, body)
        return if (reply.code == 401 && token != null) AccountApi.call(method, path, null, body) else reply
    }

    /** Write to us's topics and the person's name and email; null when the website didn't answer. */
    suspend fun contactForm(): ContactForm? {
        val reply = call("GET", "/api/pages/contact/")
        return if (reply.code == 200) parseContactForm(reply.body) else null
    }

    suspend fun sendMessage(name: String, email: String, phone: String, topic: String, message: String): FormReply =
        call("POST", "/api/pages/contact/", JSONObject()
            .put("name", name.trim()).put("email", email.trim()).put("phone", phone.trim())
            .put("topic", topic).put("message", message.trim()))
            .let { parseFormReply(it.code, it.body) }

    suspend fun joinMailingList(email: String, name: String): FormReply =
        call("POST", "/api/pages/mailing-list/", JSONObject().put("email", email.trim()).put("name", name.trim()))
            .let { parseFormReply(it.code, it.body) }
}
