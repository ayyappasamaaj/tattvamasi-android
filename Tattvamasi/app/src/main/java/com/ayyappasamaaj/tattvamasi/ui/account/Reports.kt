package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * One screen for every Volunteer tools report (registration forms, board members, volunteers, volunteer hours, event
 * expenses): the website sends each in the same shape (website: core/app_reports.py, /api/reports/<kind>/), sections
 * of headline numbers and rows, each row with its details and the names under it. Filters (a year, an event's year)
 * ask the website again. "Share or Save as PDF" prints what's shown; "Open on the website" opens the page in the
 * phone's browser for what only it does (CSV, approving, WhatsApp ticks). As the iOS app's Reports.swift.
 */
data class ReportFilterOption(val value: String, val label: String)
data class ReportFilter(val name: String, val label: String, val value: String, val options: List<ReportFilterOption>)
data class ReportRow(val title: String, val value: String, val lines: List<String>, val children: List<String>, val tone: String)
data class ReportSection(val title: String, val note: String, val search: Boolean, val stats: List<Pair<String, String>>,
                         val rows: List<ReportRow>)
data class Report(val title: String, val subtitle: String, val webUrl: String, val filters: List<ReportFilter>,
                  val sections: List<ReportSection>)

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.texts(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

fun parseReport(body: String): Report? = runCatching {
    val j = JSONObject(body)
    Report(
        title = j.optString("title"),
        subtitle = j.optString("subtitle"),
        webUrl = j.optString("web_url"),
        filters = j.optJSONArray("filters").objects().map { f ->
            ReportFilter(f.optString("name"), f.optString("label"), f.optString("value"),
                f.optJSONArray("options").objects().map { ReportFilterOption(it.optString("value"), it.optString("label")) })
        },
        sections = j.getJSONArray("sections").objects().map { s ->
            ReportSection(s.optString("title"), s.optString("note"), s.optBoolean("search"),
                s.optJSONArray("stats").objects().map { it.optString("label") to it.optString("value") },
                s.optJSONArray("rows").objects().map { r ->
                    ReportRow(r.optString("title"), r.optString("value"), r.optJSONArray("lines").texts(),
                        r.optJSONArray("children").texts(), r.optString("tone"))
                })
        },
    )
}.getOrNull()

/** A report's address with a filter set: "/api/reports/board/" + year 2025 → "/api/reports/board/?year=2025". */
fun reportPath(path: String, name: String, value: String): String {
    val base = path.substringBefore("?")
    val kept = path.substringAfter("?", "").split("&").filter { it.isNotBlank() && it.substringBefore("=") != name }
    return base + "?" + (kept + "$name=${URLEncoder.encode(value, "UTF-8")}").joinToString("&")
}

/** The rows a search keeps: the title, value, details or names contain every word. */
fun searchRows(rows: List<ReportRow>, query: String): List<ReportRow> {
    val words = query.lowercase().split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return rows
    return rows.filter { r ->
        val text = (listOf(r.title, r.value) + r.lines + r.children).joinToString(" ").lowercase()
        words.all { it in text }
    }
}

/** The report as a page to print or save as PDF: every section, row, detail and name. */
fun reportHtml(r: Report): String {
    val body = r.sections.joinToString("") { s ->
        val stats = if (s.stats.isEmpty()) "" else "<p class=stats>" +
            s.stats.joinToString(" · ") { (label, value) -> "${esc(label)}: <b>${esc(value)}</b>" } + "</p>"
        val rows = s.rows.joinToString("") { row ->
            "<tr><td><b>${esc(row.title)}</b>" + row.lines.joinToString("") { "<br><span class=s>${esc(it)}</span>" } +
                (if (row.children.isEmpty()) "" else "<div class=kids>${row.children.joinToString(", ") { esc(it) }}</div>") +
                "</td><td class=n>${esc(row.value)}</td></tr>"
        }
        "<h2>${esc(s.title)}</h2>$stats" + (if (s.note.isNotBlank()) "<p class=s>${esc(s.note)}</p>" else "") +
            (if (rows.isEmpty()) "" else "<table>$rows</table>")
    }
    return """<!doctype html><html><head><meta charset="utf-8"><title>${esc(r.title)}</title><style>
        @page { margin: 14mm 12mm; }
        body { font-family: Georgia, serif; color: #2b2118; font-size: 10.5pt; margin: 0; }
        h1 { font-size: 16pt; margin: 0; color: #7a3410; } h2 { font-size: 12pt; margin: 16px 0 6px; }
        table { width: 100%; border-collapse: collapse; }
        td { text-align: left; padding: 5px 4px; border-bottom: 1px solid #e2d6c4; vertical-align: top; }
        .n { text-align: right; white-space: nowrap; } .s { color: #6b5a48; font-size: 9pt; }
        .kids { color: #5b4a3a; font-size: 8.5pt; margin-top: 3px; } .stats { margin: 4px 0; }
        </style></head><body><h1>Ayyappa Samaaj · ${esc(r.title)}</h1><p class=s>${esc(r.subtitle)}</p>$body</body></html>"""
}

@Composable
private fun ReportRowView(row: ReportRow) {
    var open by remember(row) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                    color = if (row.tone == "warn") AppColors.clayDark else AppColors.ink)
                row.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted) }
            }
            if (row.value.isNotBlank()) {
                Text(row.value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(700)),
                    color = AppColors.ink, modifier = Modifier.padding(start = 12.dp))
            }
        }
        if (row.children.isNotEmpty()) {
            Text(if (open) "Hide names" else "Show ${row.children.size} ${if (row.children.size == 1) "name" else "names"}",
                style = MaterialTheme.typography.labelLarge, color = AppColors.clayDark,
                modifier = Modifier.padding(top = 4.dp).clickable { open = !open }.padding(vertical = 6.dp))
            if (open) {
                Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.children.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPicker(filter: ReportFilter, enabled: Boolean, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) open = it }) {
        OutlinedTextField(
            value = filter.options.firstOrNull { it.value == filter.value }?.label ?: filter.value, onValueChange = {},
            readOnly = true, label = { Text(filter.label) }, enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            filter.options.forEach { o -> DropdownMenuItem(text = { Text(o.label) }, onClick = { open = false; onPick(o.value) }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportPage(state: AccountStore.State) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.reportPath) { AccountStore.loadReport() }
    val r = state.report
    if (r == null) {
        if (state.reportFailed) {
            Text("The report couldn't be loaded. Please try again later.", style = MaterialTheme.typography.bodyMedium,
                color = AppColors.clayDark)
        } else {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(32.dp))
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().card(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(r.title, style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
        if (r.subtitle.isNotBlank()) Text(r.subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        r.filters.forEach { f ->
            FilterPicker(f, enabled = !state.loading) { value -> scope.launch { AccountStore.filterReport(f.name, value) } }
        }
        if (state.reportFailed) Text("That couldn't be loaded. Please try again.", color = AppColors.clayDark)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { printLetter(context, "Ayyappa Samaaj ${r.title} ${r.subtitle}".trim(), reportHtml(r)) }) {
                Text("Print or Save as PDF", color = AppColors.ink)
            }
            if (r.webUrl.isNotBlank()) {
                OutlinedButton(onClick = { openInBrowser(context, r.webUrl) }) { Text("Open on the website", color = AppColors.ink) }
            }
        }
    }
    if (state.loading) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            CircularProgressIndicator(color = AppColors.clay, modifier = Modifier.size(28.dp))
        }
    }
    r.sections.forEach { s ->
        var query by remember(s.title, r) { mutableStateOf("") }
        var shown by remember(s.title, r) { mutableStateOf(50) }  // long lists grow as asked
        Column(Modifier.fillMaxWidth().card(16.dp)) {
            SectionLabel(s.title)
            if (s.stats.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)) {
                    s.stats.forEach { (label, value) ->
                        Surface(color = AppColors.claySoft, shape = RoundedCornerShape(10.dp)) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(value, fontSize = 20.sp, fontWeight = FontWeight(700), color = AppColors.ink)
                                Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.inkSoft)
                            }
                        }
                    }
                }
            }
            if (s.note.isNotBlank()) {
                Text(s.note, style = MaterialTheme.typography.bodySmall, color = AppColors.muted, modifier = Modifier.padding(top = 6.dp))
            }
            if (s.search) {
                OutlinedTextField(value = query, onValueChange = { query = it; shown = 50 }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("Search names, emails, phones") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            val rows = searchRows(s.rows, query)
            if (s.search && query.isNotBlank()) {
                Text("${rows.size} of ${s.rows.size}", style = MaterialTheme.typography.bodySmall, color = AppColors.muted,
                    modifier = Modifier.padding(top = 4.dp))
            }
            rows.take(shown).forEachIndexed { i, row ->
                if (i > 0 || s.stats.isNotEmpty() || s.search) HorizontalDivider(color = AppColors.line)
                ReportRowView(row)
            }
            if (rows.size > shown) {
                OutlinedButton(onClick = { shown += 100 }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    Text("Show more (${rows.size - shown} left)", color = AppColors.ink)
                }
            }
        }
    }
}
