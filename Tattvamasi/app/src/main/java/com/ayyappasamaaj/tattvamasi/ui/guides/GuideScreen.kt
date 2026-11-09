@file:OptIn(ExperimentalFoundationApi::class)

package com.ayyappasamaaj.tattvamasi.ui.guides

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.text.RichText
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.android.awaitFrame

/**
 * One of the pilgrimage guides, in the app: the introduction, then each section as a card that opens and closes
 * (as on the website), with sub-sections inside. Reads from the copy kept on the phone, so it works offline.
 * [section], when set, is opened and scrolled to (a link to ayyappasamaaj.org/vratam/#food); [onShown] is then
 * called so it isn't scrolled to again.
 */
@Composable
fun GuideScreen(guide: GuideContent, section: String?, onBack: () -> Unit, onShown: () -> Unit = {}) {
    val context = LocalContext.current
    val expanded = rememberSaveable(guide.slug, saver = expandedSaver) { mutableStateOf(emptySet<String>()) }
    val list = rememberLazyListState()
    val all = remember(guide) { guide.sections.flatMap { it.all() }.map { it.id } }
    val headerItems = 3  // the Back row, the introduction and Open all

    LaunchedEffect(guide.slug, section) {
        if (section == null) return@LaunchedEffect
        val path = guide.pathTo(section)
        if (path.isNotEmpty()) {
            expanded.value += path
            list.scrollToItem(headerItems + guide.sections.indexOfFirst { it.id == path.first() })
        }
        onShown()
    }

    LazyColumn(
        state = list,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "top") {
            ReadableWidth {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
                        Text("Sabarimala", color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, guide.title).putExtra(Intent.EXTRA_TEXT, "${guide.title}\n${guide.url}")
                        context.startActivity(Intent.createChooser(send, "Share"))
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Share", tint = AppColors.ink) }
                }
            }
        }
        item(key = "intro") { ReadableWidth { GuideBlocks(guide.intro) } }
        item(key = "toggle") {
            ReadableWidth {
                val allOpen = expanded.value.containsAll(all)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { expanded.value = if (allOpen) emptySet() else all.toSet() }) {
                        Text(if (allOpen) "Close all" else "Open all", color = AppColors.clayDark)
                    }
                }
            }
        }
        items(guide.sections, key = { "section-" + it.id }) { item ->
            ReadableWidth {
                SectionCard(item, 0, expanded.value, section) { id ->
                    expanded.value = if (id in expanded.value) expanded.value - id else expanded.value + id
                }
            }
        }
        item(key = "footer") { ReadableWidth { AddressFooter() } }
    }
}

private val expandedSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

/** A section: its title (tap to open or close), then its content and its sub-sections. */
@Composable
private fun SectionCard(
    section: GuideSection,
    depth: Int,
    expanded: Set<String>,
    focus: String?,
    toggle: (String) -> Unit,
) {
    val open = section.id in expanded
    val shape = RoundedCornerShape(if (depth == 0) 16.dp else 12.dp)
    val requester = remember { BringIntoViewRequester() }
    if (focus == section.id && depth > 0) {
        LaunchedEffect(Unit) {
            awaitFrame()
            requester.bringIntoView()
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .clip(shape)
            .background(if (depth == 0) AppColors.card else AppColors.cream)
            .border(1.dp, AppColors.line, shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClickLabel = if (open) "Close" else "Open") { toggle(section.id) }
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = if (open) "Expanded" else "Collapsed"
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                section.title,
                style = if (depth == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = AppColors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = AppColors.clay,
                modifier = Modifier.rotate(if (open) 180f else 0f))
        }
        AnimatedVisibility(open) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            ) {
                GuideBlocks(section.blocks)
                section.sections.forEach { SectionCard(it, depth + 1, expanded, focus, toggle) }
            }
        }
    }
}

/** A short caption with no paragraphs or lists: the House Puja checklist's item names. */
private fun isCaption(block: GuideBlock?): Boolean = block is GuideBlock.Text && block.html.length <= 120 &&
    listOf("<p", "<ul", "<ol", "<h").none { block.html.contains(it, ignoreCase = true) }

/** Text and pictures in page order. A picture followed by a short caption is a row: a thumbnail and its name. */
@Composable
fun GuideBlocks(blocks: List<GuideBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var index = 0
        while (index < blocks.size) {
            val block = blocks[index]
            val next = blocks.getOrNull(index + 1)
            when {
                block is GuideBlock.Picture && isCaption(next) -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.alt.ifBlank { null },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.claySoft),
                        )
                        Box(Modifier.weight(1f)) { RichText((next as GuideBlock.Text).html) }
                    }
                    index += 2
                    continue
                }
                block is GuideBlock.Picture -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.alt.ifBlank { null },
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(min = 80.dp)
                            .clip(RoundedCornerShape(12.dp)).background(AppColors.card),  // white: some pictures have clear edges
                    )
                }
                block is GuideBlock.Text -> RichText(block.html)
            }
            index += 1
        }
    }
}
