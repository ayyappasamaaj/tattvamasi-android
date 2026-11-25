package com.ayyappasamaaj.tattvamasi.ui.account

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The year-end summary and the full My donations list, in the app instead of the website's dashboard (the website's
 * /api/core/year-summary/ and /api/donations/mine/, the same answers the dashboard reads; core/year_summary.py). The
 * same as the iOS app's YearSummary.swift.
 *
 * Plain functions, unit-tested with the website's answers (SummaryTest).
 */
data class SummaryLine(val date: String, val description: String, val items: String, val by: String?,
                       val amount: String, val goodsValue: String?)

data class Officer(val title: String, val name: String)

data class YearSummary(
    val year: Int,
    val years: List<Int>,
    val scope: String,                    // "me" or "household"
    val householdName: String?,           // offered as "Letter for" when the person has a household sharing gifts
    val personName: String,
    val personEmail: String,
    val orgName: String,
    val orgPlace: String,
    val orgStatus: String,
    val ein: String,
    val tagline: String,
    val email: String,
    val website: String,
    val officers: List<Officer>,
    val donations: List<SummaryLine>,
    val registrations: List<SummaryLine>,
    val totalDonations: String,
    val totalRegistrations: String,
    val totalGoods: String,
    val totalAll: String,
    val statement: String,
    val generated: String,
)

/** One of the person's confirmed donations; a Zelle one has its memo code. */
data class MyDonation(val id: Int, val title: String, val date: String, val amount: String, val donatedBy: String?,
                      val memoCode: String?)

/** A Zelle gift the treasurer hasn't confirmed: "waiting", or "not_found" in the bank. */
data class ZelleGift(val id: Int, val title: String, val date: String, val amount: String, val state: String,
                     val memoCode: String)

data class DonationsPage(val donations: List<MyDonation>, val more: Boolean)

/** A running monthly gift (website: donations/monthly.py): Stripe's subscription id, and what the page shows. */
data class MonthlyGift(val id: String, val amount: String, val category: String, val since: String?,
                       val nextPayment: String?, val status: String, val amountId: Int? = null, val day: Int? = null)

/** A monthly amount a gift can change to (the Donate page's). */
data class MonthlyAmountChoice(val id: Int, val amount: String, val category: String)

data class MonthlyGifts(val gifts: List<MonthlyGift>, val amounts: List<MonthlyAmountChoice>)

fun parseMonthlyGift(g: JSONObject): MonthlyGift =
    MonthlyGift(g.optString("id"), g.optString("amount"), g.optString("category"), g.text("since"),
        g.text("next_payment"), g.optString("status"), if (g.isNull("amount_id")) null else g.optInt("amount_id"),
        if (g.isNull("day")) null else g.optInt("day"))

private fun JSONObject.text(name: String): String? = if (isNull(name)) null else optString(name).ifBlank { null }

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

fun parseSummary(body: String): YearSummary? = runCatching {
    val json = JSONObject(body)
    val org = json.optJSONObject("organization") ?: JSONObject()
    val head = json.optJSONObject("letterhead") ?: JSONObject()
    val person = json.optJSONObject("person") ?: JSONObject()
    val totals = json.getJSONObject("totals")
    fun lines(name: String) = json.optJSONArray(name).objects().map {
        SummaryLine(it.optString("date"), it.optString("description"), it.optString("items"), it.text("by"),
            it.optString("amount"), it.text("goods_value"))
    }
    val years = json.optJSONArray("years")
    YearSummary(
        year = json.getInt("year"),
        years = if (years == null) emptyList() else (0 until years.length()).map { years.getInt(it) },
        scope = json.optString("scope", "me"),
        householdName = json.optJSONObject("household")?.text("name"),
        personName = person.optString("name"),
        personEmail = person.optString("email"),
        orgName = org.optString("name", "Ayyappa Samaaj"),
        orgPlace = org.optString("place"),
        orgStatus = org.optString("status"),
        ein = org.optString("ein"),
        tagline = head.optString("tagline"),
        email = head.optString("email"),
        website = head.optString("website"),
        officers = head.optJSONArray("officers").objects().map { Officer(it.optString("title"), it.optString("name")) },
        donations = lines("donations"),
        registrations = lines("registrations"),
        totalDonations = totals.optString("donations"),
        totalRegistrations = totals.optString("registrations"),
        totalGoods = totals.optString("goods_value"),
        totalAll = totals.optString("all"),
        statement = json.optString("statement"),
        generated = json.optString("generated"),
    )
}.getOrNull()

fun parseDonationsPage(body: String): DonationsPage {
    val json = JSONObject(body)
    val donations = json.optJSONArray("results").objects().map { d ->
        MyDonation(d.optInt("id"), d.optString("title").ifBlank { "Donation" }, d.optString("date").take(10),
            d.optString("amount"), d.text("donated_by"), d.text("memo_code"))
    }
    return DonationsPage(donations, more = json.text("next") != null)
}

fun parseMonthlyGifts(body: String): MonthlyGifts {
    val json = JSONObject(body)
    return MonthlyGifts(
        json.optJSONArray("gifts").objects().map { parseMonthlyGift(it) },
        json.optJSONArray("amounts").objects().map { MonthlyAmountChoice(it.optInt("id"), it.optString("amount"), it.optString("category")) },
    )
}

/** "5th" */
fun ordinal(n: Int): String = "$n" + if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

fun parseZelleGifts(body: String): List<ZelleGift> = JSONArray(body).objects().map { g ->
    ZelleGift(g.optInt("id"), g.optString("title"), g.optString("date"), g.optString("amount"), g.optString("state"),
        g.optString("memo_code"))
}

private val dayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** "Nov 25, 2026" from "2026-11-25" (or a time starting with that day). */
fun summaryDay(iso: String?): String =
    iso?.let { runCatching { LocalDate.parse(it.take(10)).format(dayFormat) }.getOrNull() } ?: ""

/** "$1,250.00" */
fun summaryDollars(amount: String?): String =
    amount?.toBigDecimalOrNull()?.let { "$" + "%,.2f".format(it) } ?: amount.orEmpty()

private fun esc(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;")

/**
 * The year-end letter as a page to print or save as PDF, laid out like the website's (the dashboard's Letterhead.vue
 * and YearSummary.vue): the letterhead with the office bearers down the left, the person, the two tables with their
 * totals, the statement and the date it was made. [logoUrl] is the website's logo (the page loads it).
 */
fun summaryLetterHtml(s: YearSummary, logoUrl: String): String {
    val goods = (s.totalGoods.toBigDecimalOrNull()?.signum() ?: 0) > 0
    fun table(title: String, lines: List<SummaryLine>, total: String, registrations: Boolean): String {
        if (lines.isEmpty()) return ""
        val goodsColumn = registrations && goods
        val rows = lines.joinToString("") { l ->
            val detail = listOfNotNull(l.items.ifBlank { null }, l.by?.let { "by $it" })
            val worth = l.goodsValue?.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }?.let { summaryDollars(it) }
            "<tr><td class=d>${esc(summaryDay(l.date))}</td><td>${esc(l.description)}" +
                detail.joinToString("") { "<br><span class=s>${esc(it)}</span>" } + "</td>" +
                (if (goodsColumn) "<td class=n>${esc(worth.orEmpty())}</td>" else "") +
                "<td class=n>${esc(summaryDollars(l.amount))}</td></tr>"
        }
        val head = if (registrations) "<th>Registration</th>" + (if (goodsColumn) "<th class=n>Goods received</th>" else "") +
            "<th class=n>Paid</th>" else "<th>Donation</th><th class=n>Amount</th>"
        return "<h3>${esc(title)}</h3><table><thead><tr><th>Date</th>$head</tr></thead><tbody>$rows</tbody>" +
            "<tfoot><tr><th colspan=${if (goodsColumn) 3 else 2}>${esc(title)} total</th>" +
            "<td class=n>${esc(summaryDollars(total))}</td></tr></tfoot></table>"
    }
    val officers = s.officers.joinToString("") { "<p><b>${esc(it.name)}</b><br>${esc(it.title)}</p>" }
    val title = "${s.year} year-end summary" + if (s.scope == "household") " (household)" else ""
    val empty = if (s.donations.isEmpty() && s.registrations.isEmpty()) "<p>Nothing recorded for ${s.year}.</p>" else ""
    val all = if (s.donations.isNotEmpty() || s.registrations.isNotEmpty())
        "<p class=all>Total for ${s.year}: <b>${esc(summaryDollars(s.totalAll))}</b></p><p class=note>${esc(s.statement)}</p>"
    else ""
    return """<!doctype html><html><head><meta charset="utf-8"><title>${esc(s.orgName)} ${esc(title)}</title><style>
        @page { margin: 16mm 14mm; }
        body { font-family: Georgia, serif; color: #2b2118; font-size: 11pt; margin: 0; }
        .head { display: flex; align-items: center; gap: 14px; border-bottom: 2px solid #b5541c; padding-bottom: 10px; }
        .head img { width: 64px; height: 64px; }
        .head h1 { font-size: 18pt; margin: 0; color: #7a3410; }
        .head p { margin: 2px 0; font-size: 9pt; }
        .body { display: flex; gap: 18px; margin-top: 14px; }
        .side { width: 120px; font-size: 8.5pt; color: #5b4a3a; border-right: 1px solid #e2d6c4; padding-right: 10px; }
        .side p { margin: 0 0 10px; }
        .main { flex: 1; }
        h2 { font-size: 14pt; margin: 0 0 8px; }
        h3 { font-size: 11.5pt; margin: 16px 0 6px; }
        table { width: 100%; border-collapse: collapse; font-size: 10pt; }
        th, td { text-align: left; padding: 5px 4px; border-bottom: 1px solid #e2d6c4; vertical-align: top; }
        tfoot th, tfoot td { border-bottom: none; font-weight: bold; }
        .n { text-align: right; white-space: nowrap; }
        .d { white-space: nowrap; width: 90px; }
        .s { color: #6b5a48; font-size: 9pt; }
        .all { margin-top: 14px; font-size: 11.5pt; }
        .note { margin-top: 16px; font-size: 9.5pt; }
        .gen { margin-top: 18px; font-size: 8.5pt; color: #6b5a48; }
        </style></head><body>
        <div class=head><img src="${esc(logoUrl)}" alt=""><div><h1>${esc(s.orgName)}</h1>
        <p>${esc(s.tagline)}</p><p>${esc(s.orgPlace)} · ${esc(s.email)} · ${esc(s.website)}</p></div></div>
        <div class=body>${if (officers.isNotEmpty()) "<div class=side><p><b>Office bearers</b></p>$officers</div>" else ""}
        <div class=main><h2>${esc(title)}</h2>
        <p>${esc(s.orgName)}, ${esc(s.orgPlace)}, ${esc(s.orgStatus)}, EIN ${esc(s.ein)}.</p>
        <p>${s.year} contributions of <b>${esc(s.personName)}</b>${if (s.personEmail.isNotBlank()) " (${esc(s.personEmail)})" else ""}</p>
        $empty${table("Donations", s.donations, s.totalDonations, false)}${table("Registrations", s.registrations, s.totalRegistrations, true)}$all
        <p class=gen>For your records. Please check with your tax consultant on what you can deduct. Questions:
        ${esc(s.email)}. Printed from ayyappasamaaj.org on ${esc(summaryDay(s.generated))}.</p>
        </div></div></body></html>"""
}
