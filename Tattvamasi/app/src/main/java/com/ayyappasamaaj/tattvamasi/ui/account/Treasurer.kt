package com.ayyappasamaaj.tattvamasi.ui.account

import org.json.JSONArray
import org.json.JSONObject

/**
 * The treasurer's Zelle and check/cash tools in the app (website: donations/treasurer_api.py, the same forms, rules,
 * receipts and notices as its /donate/zelle/review/ and /donate/checks/ pages). Only the treasurer's group gets them
 * (Volunteer tools shows them by the website's my-pages "app_screen"). Bank imports and re-sending receipts stay on the
 * website pages. As the iOS app's Treasurer.swift.
 *
 * Plain functions, unit-tested with the website's answers (TreasurerTest).
 */
data class WaitingZelle(val id: Int, val memoCode: String, val donor: String, val email: String, val amount: String,
                     val givenOn: String, val senderName: String, val category: String, val memo: String)

data class ZelleReviewed(val memoCode: String, val donor: String, val amount: String, val givenOn: String,
                         val state: String, val by: String, val note: String)

data class GiftCategory(val id: Int, val label: String)

data class ZelleList(val waiting: List<WaitingZelle>, val reviewed: List<ZelleReviewed>, val categories: List<GiftCategory>)

data class DonorAccount(val id: Int, val name: String, val email: String)

data class CheckGift(val id: Int, val donor: String, val amount: String, val paidBy: String, val date: String,
                     val category: String)

data class ChecksInfo(val accounts: List<DonorAccount>, val categories: List<GiftCategory>, val recent: List<CheckGift>)

/** The website's answer to a treasurer action: done (its message), or the problems by field ("" for the whole). */
sealed interface TreasurerAnswer {
    data class Done(val message: String) : TreasurerAnswer
    data class Problems(val errors: Map<String, String>) : TreasurerAnswer
    data object SignedOut : TreasurerAnswer
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun categoriesOf(json: JSONObject) =
    json.optJSONArray("categories").objects().map { GiftCategory(it.optInt("id"), it.optString("label")) }

fun parseZelle(body: String): ZelleList? = runCatching {
    val j = JSONObject(body)
    ZelleList(
        waiting = j.getJSONArray("waiting").objects().map {
            WaitingZelle(it.getInt("id"), it.optString("memo_code"), it.optString("donor"), it.optString("email"),
                it.optString("amount"), it.optString("given_on"), it.optString("sender_name"), it.optString("category"),
                it.optString("memo"))
        },
        reviewed = j.optJSONArray("reviewed").objects().map {
            ZelleReviewed(it.optString("memo_code"), it.optString("donor"), it.optString("amount"),
                it.optString("given_on"), it.optString("state"), it.optString("by"), it.optString("note"))
        },
        categories = categoriesOf(j),
    )
}.getOrNull()

fun parseChecks(body: String): ChecksInfo? = runCatching {
    val j = JSONObject(body)
    ChecksInfo(
        accounts = j.optJSONArray("accounts").objects().map {
            DonorAccount(it.getInt("id"), it.optString("name"), it.optString("email"))
        },
        categories = categoriesOf(j),
        recent = j.optJSONArray("recent").objects().map {
            CheckGift(it.optInt("id"), it.optString("donor"), it.optString("amount"), it.optString("paid_by"),
                it.optString("date"), it.optString("category"))
        },
    )
}.getOrNull()

fun parseTreasurerAnswer(code: Int, body: String): TreasurerAnswer {
    val json = runCatching { JSONObject(body) }.getOrNull()
    if (code in 200..299 && json != null && json.has("message")) return TreasurerAnswer.Done(json.optString("message"))
    if (code == 401) return TreasurerAnswer.SignedOut
    val errors = json?.optJSONObject("errors")?.let { e -> e.keys().asSequence().associateWith { e.optString(it) } }
    if (!errors.isNullOrEmpty()) return TreasurerAnswer.Problems(errors)
    val reason = json?.optString("error").orEmpty().ifBlank { json?.optString("detail").orEmpty() }.ifBlank {
        if (code == 0) "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
        else "It couldn't be saved just now. Please try again in a few minutes."
    }
    return TreasurerAnswer.Problems(mapOf("" to reason))
}

/** "zelle" or "checks": the treasurer screens the app has (the website's my-pages "app_screen"). */
val treasurerScreens = setOf("zelle", "checks")

object TreasurerApi {
    suspend fun zelle(token: String): Pair<Int, ZelleList?> {
        val reply = AccountApi.call("GET", "/api/treasurer/zelle/", token)
        return reply.code to if (reply.code == 200) parseZelle(reply.body) else null
    }

    /** [action]: "confirm" or "not-found". */
    suspend fun review(token: String, id: Int, action: String, note: String): TreasurerAnswer {
        val reply = AccountApi.call("POST", "/api/treasurer/zelle/$id/$action/", token, JSONObject().put("note", note.trim()))
        return parseTreasurerAnswer(reply.code, reply.body)
    }

    suspend fun addZelle(token: String, fields: Map<String, String>): TreasurerAnswer {
        val reply = AccountApi.call("POST", "/api/treasurer/zelle/add/", token, JSONObject(fields))
        return parseTreasurerAnswer(reply.code, reply.body)
    }

    suspend fun checks(token: String, query: String): Pair<Int, ChecksInfo?> {
        val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val reply = AccountApi.call("GET", "/api/treasurer/checks/" + (if (q.isEmpty()) "" else "?q=$q"), token)
        return reply.code to if (reply.code == 200) parseChecks(reply.body) else null
    }

    suspend fun addCheck(token: String, fields: Map<String, Any>): TreasurerAnswer {
        val reply = AccountApi.call("POST", "/api/treasurer/checks/", token, JSONObject(fields))
        return parseTreasurerAnswer(reply.code, reply.body)
    }
}
