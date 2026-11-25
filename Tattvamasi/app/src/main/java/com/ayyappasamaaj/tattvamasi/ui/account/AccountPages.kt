package com.ayyappasamaaj.tattvamasi.ui.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch

/*
 * My account's two pages (Summary.kt): the year-end summary, with "Print or Save as PDF" (Android's print screen
 * makes the letter on the letterhead), and every donation. In the app, instead of the website's dashboard (the
 * maintainer's request, Nov 25). The same as the iOS app's YearSummaryView.swift.
 */

private val bold = FontWeight(600)

@Composable
private fun Loading() {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = AppColors.muted) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun Line(title: String, detail: List<String>, amount: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            detail.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
        }
        Text(amount, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (strong) FontWeight(700) else bold),
            color = AppColors.ink, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AppColors.claySoft, selectedLabelColor = AppColors.clayDark, labelColor = AppColors.ink)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearSummaryPage(state: AccountStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AccountStore.loadSummary() }  // fresh each time (a gift made meanwhile shows)
    val s = state.summary
    if (s == null) {
        if (state.summaryFailed) Note("The summary couldn't be loaded. Please try again later.", AppColors.clayDark)
        else Loading()
        return
    }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Everything you gave the Samaaj in a year, for your taxes: donations and paid registrations.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        SectionLabel("Year")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.years.forEach { year ->
                FilterChip(selected = year == s.year, onClick = { scope.launch { AccountStore.loadSummary(year = year) } },
                    label = { Text(year.toString()) }, enabled = !state.loading, colors = chipColors())
            }
        }
        s.householdName?.let { household ->
            SectionLabel("Letter for")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.scope == "me", onClick = { scope.launch { AccountStore.loadSummary(scope = "me") } },
                    label = { Text("Just me") }, enabled = !state.loading, colors = chipColors())
                FilterChip(selected = s.scope == "household",
                    onClick = { scope.launch { AccountStore.loadSummary(scope = "household") } },
                    label = { Text(household) }, enabled = !state.loading, colors = chipColors())
            }
        }
        if (state.summaryFailed) Note("That year couldn't be loaded. Please try again.", AppColors.clayDark)
    }
    if (state.loading) Loading()

    Column(Modifier.fillMaxWidth().card(16.dp)) {
        Text("${s.year} contributions of ${s.personName}", style = MaterialTheme.typography.titleMedium,
            color = AppColors.ink)
        if (s.personEmail.isNotBlank()) Text(s.personEmail, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        Text("${s.orgName} · ${s.orgStatus} · EIN ${s.ein}", style = MaterialTheme.typography.bodyMedium,
            color = AppColors.muted, modifier = Modifier.padding(top = 4.dp))
        if (s.donations.isEmpty() && s.registrations.isEmpty()) {
            Note("Nothing recorded for ${s.year}.")
        }
        if (s.donations.isNotEmpty()) {
            SectionLabel("Donations", Modifier.padding(top = 14.dp, bottom = 2.dp))
            s.donations.forEach { d ->
                Line(d.description, listOf(summaryDay(d.date), d.by?.let { "by $it" }.orEmpty()), summaryDollars(d.amount))
                HorizontalDivider(color = AppColors.line)
            }
            Line("Donations total", emptyList(), summaryDollars(s.totalDonations), strong = true)
        }
        if (s.registrations.isNotEmpty()) {
            SectionLabel("Registrations", Modifier.padding(top = 14.dp, bottom = 2.dp))
            s.registrations.forEach { r ->
                val goods = r.goodsValue?.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }
                Line(r.description, listOf(summaryDay(r.date), r.items, r.by?.let { "by $it" }.orEmpty(),
                    goods?.let { "goods received ${summaryDollars(it)}" }.orEmpty()), summaryDollars(r.amount))
                HorizontalDivider(color = AppColors.line)
            }
            Line("Registrations total", emptyList(), summaryDollars(s.totalRegistrations), strong = true)
        }
        if (s.donations.isNotEmpty() || s.registrations.isNotEmpty()) {
            HorizontalDivider(color = AppColors.lineStrong)
            Line("Total for ${s.year}", emptyList(), summaryDollars(s.totalAll), strong = true)
            Text(s.statement, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                modifier = Modifier.padding(top = 6.dp))
        }
        Text("For your records. Please check with your tax consultant on what you can deduct. Questions: ${s.email}.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.muted, modifier = Modifier.padding(top = 10.dp))
    }
    Button(
        onClick = { printSummary(context, s) },
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text("Print or Save as PDF", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)) }
}

@Composable
fun DonationsListPage(state: AccountStore.State) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AccountStore.loadDonations() }
    var confirm by remember { mutableStateOf<MonthlyGift?>(null) }
    if (state.monthly.isNotEmpty() || state.monthlyMessage != null || state.monthlyFailed) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Monthly gifts", Modifier.padding(bottom = 4.dp))
            state.monthly.forEachIndexed { index, g ->
                if (index > 0) HorizontalDivider(color = AppColors.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        Text("${summaryDollars(g.amount).removeSuffix(".00")} every month · ${g.category}",
                            style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
                        Text(listOfNotNull(g.since?.let { "Since ${summaryDay(it)}" }, g.nextPayment?.let { "Next ${summaryDay(it)}" },
                            "Last payment failed".takeIf { g.status == "past_due" }).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                    }
                    OutlinedButton(onClick = { confirm = g }, enabled = state.stopping == null) {
                        Text(if (state.stopping == g.id) "Stopping…" else "Stop", color = AppColors.ink)
                    }
                }
            }
            if (state.monthlyFailed) Note("Your monthly gifts couldn't be loaded. Please try again later.", AppColors.clayDark)
            state.monthlyMessage?.let { Note(it, AppColors.inkSoft) }
            Text("To change the amount, stop it and start a new one from Donate.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.muted, modifier = Modifier.padding(top = 6.dp))
        }
    }
    confirm?.let { g ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Stop this monthly gift?") },
            text = {
                Text("${summaryDollars(g.amount).removeSuffix(".00")} every month for ${g.category}. It stops now: no " +
                    "more payments will be taken. The months you gave stay on your account and your year-end summary.")
            },
            confirmButton = {
                TextButton(onClick = { confirm = null; scope.launch { AccountStore.stopMonthlyGift(g) } }) {
                    Text("Stop It", color = AppColors.clayDark)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep It", color = AppColors.ink) } },
        )
    }
    if (state.zelle.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel("Zelle gifts being checked", Modifier.padding(bottom = 4.dp))
            state.zelle.forEach { g ->
                val where = if (g.state == "not_found") "Not found in the bank: please write to treasurer@ayyappasamaaj.org"
                else "Waiting for the treasurer"
                Line(g.title, listOf(summaryDay(g.date), "Memo code ${g.memoCode}", where), summaryDollars(g.amount))
            }
        }
    }
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        SectionLabel("My donations", Modifier.padding(bottom = 4.dp))
        if (state.donationsPage == 0 && state.loading) Loading()
        if (state.donationsPage > 0 && state.donations.isEmpty()) Note("No donations yet.")
        state.donations.forEachIndexed { index, d ->
            if (index > 0) HorizontalDivider(color = AppColors.line)
            Line(d.title, listOf(summaryDay(d.date), d.memoCode?.let { "Zelle, memo code $it" }.orEmpty(),
                d.donatedBy?.let { "by $it" }.orEmpty()), summaryDollars(d.amount))
        }
        if (state.donationsFailed) Note("Your donations couldn't be loaded. Please try again later.", AppColors.clayDark)
        if (state.moreDonations) {
            OutlinedButton(onClick = { scope.launch { AccountStore.loadDonations(next = true) } }, enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (state.loading) "Loading…" else "Show More", color = AppColors.ink)
            }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Kept until the print screen has the page (Android's advice for printing from a WebView). */
private var printPage: WebView? = null

/**
 * Android's print screen with the year-end letter (summaryLetterHtml): print it, or "Save as PDF". The page is drawn
 * by a WebView that's never shown; it loads nothing but the letter and the website's logo.
 */
fun printSummary(context: Context, summary: YearSummary) {
    val activity = context.activity() ?: return
    val page = WebView(activity)
    page.settings.javaScriptEnabled = false
    page.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val name = "Ayyappa Samaaj ${summary.year} year-end summary"
            val printing = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printing.print(name, view.createPrintDocumentAdapter(name),
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.NA_LETTER).build())
        }
    }
    printPage = page
    page.loadDataWithBaseURL(AccountApi.site, summaryLetterHtml(summary, AccountApi.logoUrl), "text/html", "utf-8", null)
}
