package com.ayyappasamaaj.tattvamasi.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import org.json.JSONObject

/**
 * Volunteer tools in My Account, as the dashboard's (AYSA-VUE-App VolunteerTools.vue): the pages this person can open
 * beyond the public site (check-in, reports, treasurer, house bhajan review, admin), from the website's
 * /api/core/my-pages/, which decides who sees what. Grouped by section, each opening and closing. A page the app has
 * its own screen for opens there; the rest open in the phone's browser (Chrome), where the person signs in to the
 * website once (the maintainer's call, Nov 26). As the iOS app's VolunteerTools.swift.
 */
data class ToolPage(val section: String, val title: String, val url: String, val description: String)

fun parseToolPages(body: String): List<ToolPage> = runCatching {
    val pages = JSONObject(body).optJSONArray("pages") ?: return emptyList()
    (0 until pages.length()).mapNotNull { pages.optJSONObject(it) }.map {
        ToolPage(it.optString("section").ifBlank { "Other" }, it.optString("title"), it.optString("url"),
            it.optString("description"))
    }.filter { it.title.isNotBlank() && it.url.isNotBlank() }
}.getOrDefault(emptyList())

/** The pages by section, in the order the sections first come (the website's). */
fun toolSections(pages: List<ToolPage>): List<Pair<String, List<ToolPage>>> =
    pages.groupBy { it.section }.toList()

/** A page's full address: the website gives its path. */
fun toolUrl(page: ToolPage): String = if (page.url.startsWith("http")) page.url else AccountApi.site + page.url

@Composable
fun VolunteerToolsCard(pages: List<ToolPage>) {
    if (pages.isEmpty()) return
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.forest).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Volunteer tools", style = MaterialTheme.typography.titleLarge, color = AppColors.onForest,
            modifier = Modifier.semantics { heading() })
        Text("Pages open in your phone's browser; sign in to the website there once.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.onForest.copy(alpha = 0.8f))
        toolSections(pages).forEach { (section, list) ->
            var open by rememberSaveable(section) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f))) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = { open = !open })
                        .padding(horizontal = 14.dp)
                        .semantics(mergeDescendants = true) { stateDescription = if (open) "Open" else "Closed" },
                ) {
                    Text(section.uppercase(), fontSize = 13.sp, fontWeight = FontWeight(700), letterSpacing = 1.sp,
                        color = Color(0xFFF2C46D), modifier = Modifier.weight(1f))
                    Text("${list.size}", fontSize = 13.sp, fontWeight = FontWeight(600), color = AppColors.onForest,
                        modifier = Modifier.padding(end = 8.dp))
                    Icon(if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null,
                        tint = AppColors.onForest)
                }
                if (open) {
                    Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        list.forEach { page ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable(onClickLabel = "Open in the browser") { openInBrowser(context, toolUrl(page)) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .semantics(mergeDescendants = true) {},
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(page.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                                        color = AppColors.onForest)
                                    if (page.description.isNotBlank()) {
                                        Text(page.description, style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.onForest.copy(alpha = 0.8f))
                                    }
                                }
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = AppColors.onForest.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
