@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.publications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ScreenTitle
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.LocalDate

/**
 * Publications: Bhajans, then Shlokas & Puja, as on the website, with one search for everything. A tile opens the
 * category's texts; a title opens the reader ([read]: AppRoot shows it over the page, PdfReader.kt). On a phone the
 * category replaces the tiles (Back returns); on a tablet the tiles stay on the left and the category shows on the
 * right, like the iPad's split view. Texts saved on this phone get a "Saved" row at the top, opening their list.
 */
@Composable
fun PublicationsScreen(state: PublicationsState, openYatra: () -> Unit = {}, read: (ReadingItem) -> Unit) {
    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { ReadingPrefs.load(context) }
    LaunchedEffect(Unit) { SavedTexts.load(context) }
    val preferred by ReadingPrefs.language.collectAsState()
    val savedList by SavedTexts.saved.collectAsState()
    val saved = resolveSaved(savedList, state.sections)
    val savedKeys = savedList.map { savedKey(it.slug, it.title) }.toSet()
    val all = allCategories(state.sections)
    val selected = all.firstOrNull { it.second.slug == selectedSlug }
    // Every title opens in the "Reading in" language when it has it, else English (as on the website)
    val open = { item: ReadingItem -> read(item.copy(startLanguage = openingLanguage(item.text, preferred))) }
    val choose = { category: PublicationCategory -> selectedSlug = category.slug }
    val showSaved = { selectedSlug = SAVED }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            val shown = selected ?: state.sections.firstOrNull()?.let { s -> s.categories.firstOrNull()?.let { s to it } }
            val savedShown = selectedSlug == SAVED
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(340.dp).fillMaxHeight()) {
                    PublicationsHome(state, selectedSlug = if (savedShown) SAVED else shown?.second?.slug,
                        onCategory = { selectedSlug = it.slug }, onRead = open, minTile = 96, openYatra = openYatra,
                        saved = saved.size, savedKeys = savedKeys, onSaved = showSaved)
                }
                VerticalDivider(color = AppColors.line)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (savedShown) {
                        SavedContent(saved, onRead = open, onBack = null)
                    } else {
                        shown?.let { (section, category) ->
                            CategoryContent(section, category, all, savedKeys, onRead = open, onCategory = choose, onBack = null)
                        }
                    }
                }
            }
        } else if (selectedSlug == SAVED) {
            BackHandler { selectedSlug = null }
            SavedContent(saved, onRead = open, onBack = { selectedSlug = null })
        } else if (selected != null) {
            BackHandler { selectedSlug = null }
            CategoryContent(selected.first, selected.second, all, savedKeys, onRead = open, onCategory = choose,
                onBack = { selectedSlug = null })
        } else {
            PublicationsHome(state, selectedSlug = null, onCategory = { selectedSlug = it.slug }, onRead = open,
                minTile = 100, openYatra = openYatra, saved = saved.size, savedKeys = savedKeys, onSaved = showSaved)
        }
    }
}

/** [PublicationsScreen]'s selection when the Saved list shows (no category has this slug). */
private const val SAVED = "__saved"

/** The texts saved on this phone, newest first, each with its category. */
@Composable
private fun SavedContent(saved: List<PublicationHit>, onRead: (ReadingItem) -> Unit, onBack: (() -> Unit)?) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (onBack != null) {
            item {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
                    Text("Publications", color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("Saved", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                    color = AppColors.ink, modifier = Modifier.semantics { heading() })
                Text("Kept on this phone", style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
            }
        }
        item {
            if (saved.isEmpty()) {
                Text("Nothing saved yet. Open a text and tap the bookmark to keep it here.",
                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.fillMaxWidth().card())
            } else {
                Column(Modifier.fillMaxWidth().card(padding = 14.dp)) {
                    saved.forEachIndexed { i, hit ->
                        if (i > 0) HorizontalDivider(color = AppColors.line)
                        TextRow(hit.text, detail = hit.category.name, saved = true) {
                            onRead(ReadingItem(hit.text, hit.category.name, null, hit.category.slug))
                        }
                    }
                }
            }
        }
        item { AddressFooter() }
    }
}

/** "Saved · 3": the way to the texts saved on this phone, at the top of Publications once there are some. */
@Composable
private fun SavedRow(count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.claySoft else AppColors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) AppColors.clay else AppColors.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Saved, $count" },
    ) {
        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = AppColors.saffron)
        Text("Saved · $count", style = MaterialTheme.typography.titleMedium, color = AppColors.ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}

/** The title, the search for everything, and each section's tiles (or the search's results). */
@Composable
private fun PublicationsHome(
    state: PublicationsState,
    selectedSlug: String?,
    onCategory: (PublicationCategory) -> Unit,
    onRead: (ReadingItem) -> Unit,
    minTile: Int,
    openYatra: () -> Unit,
    saved: Int,
    savedKeys: Set<String>,
    onSaved: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recents by ReadingPrefs.recents.collectAsState()
    val preferred by ReadingPrefs.language.collectAsState()
    val languages = readingLanguages(state.sections.flatMap { it.categories })
    val shortlist = if (inMandalamSeason(LocalDate.now())) mandalamShortlist(state.sections) else emptyList()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minTile.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) =
            { GridItemSpan(maxLineSpan) }
        item(span = full) { ScreenTitle("Publications") }
        item(span = full) {
            SearchField("Search all bhajans and shlokas", query, onFocus = { focused = it },
                onSearch = { ReadingPrefs.addRecent(context, query) }) { query = it }
        }
        if (languages.size > 1) {
            item(span = full) {
                ReadingIn(languages, preferred) { ReadingPrefs.setLanguage(context, it) }
            }
        }
        if (query.isBlank() && saved > 0) {
            item(span = full) { SavedRow(saved, selected = selectedSlug == SAVED, onClick = onSaved) }
        }
        // Before anything is typed: this phone's recent searches (while the box has the cursor)
        if (query.isBlank() && focused && recents.isNotEmpty()) {
            item(span = full) { RecentSearches(recents) { query = it } }
        }
        when {
            query.isNotBlank() -> {
                val hits = searchPublications(state.sections, query)
                item(span = full) {
                    if (hits.isEmpty()) {
                        Text("No titles match “$query”.", style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.inkSoft, modifier = Modifier.fillMaxWidth().card())
                    } else {
                        Column(Modifier.fillMaxWidth().card(padding = 14.dp)) {
                            hits.forEachIndexed { i, hit ->
                                if (i > 0) HorizontalDivider(color = AppColors.line)
                                TextRow(hit.text, detail = hit.category.name,
                                    saved = savedKey(hit.category.slug, hit.text.title) in savedKeys) {
                                    ReadingPrefs.addRecent(context, query)  // a title opened from a search
                                    onRead(ReadingItem(hit.text, hit.category.name, null, hit.category.slug))
                                }
                            }
                        }
                    }
                }
            }
            !state.loaded -> item(span = full) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.clay)
                }
            }
            state.sections.isEmpty() -> item(span = full) {
                Text("Publications couldn't be loaded. Check your connection and try again.",
                    style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.fillMaxWidth().card())
            }
            else -> {
              if (shortlist.isNotEmpty()) {
                item(key = "season", span = full) {
                    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("For Mandalam Season", style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
                            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() })
                        Column(Modifier.fillMaxWidth().card(padding = 14.dp)) {
                            shortlist.forEachIndexed { i, hit ->
                                if (i > 0) HorizontalDivider(color = AppColors.line)
                                TextRow(hit.text, detail = hit.category.name, title = plainTitle(hit.text.title),
                                    saved = savedKey(hit.category.slug, hit.text.title) in savedKeys) {
                                    onRead(ReadingItem(hit.text, hit.category.name, null, hit.category.slug))
                                }
                            }
                        }
                    }
                }
              }
              state.sections.forEach { section ->
                item(key = "section-" + section.key, span = full) {
                    Text(section.title, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                }
                items(section.categories, key = { it.slug }) { category ->
                    CategoryTile(category, selected = category.slug == selectedSlug) { onCategory(category) }
                }
              }
            }
        }
        if (query.isBlank()) item(span = full) { YatraCard(openYatra) }
        item(span = full) { AddressFooter() }
    }
}

/** The Sahasranama Yatra, after the publications (it's the Sahasranamam, chanted a day at a time). */
@Composable
private fun YatraCard(open: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = open)
            .background(AppColors.forest, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Icon(painterResource(R.drawable.ic_yatra_lamp), contentDescription = null, tint = AppColors.saffron,
            modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f)) {
            Text("Sahasranama Yatra", style = MaterialTheme.typography.titleLarge, color = AppColors.onForest)
            Text("Chant the Sri Dharma Shastha Sahasranamam four verses a day through the 41 days of Vratam, with the " +
                "meaning of every name.", style = MaterialTheme.typography.bodyMedium, color = AppColors.onForest.copy(alpha = 0.85f))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.saffron)
    }
}

/** One category's texts, with its own search and a language switch. */
@Composable
private fun CategoryContent(
    section: PublicationSection,
    category: PublicationCategory,
    all: List<Pair<PublicationSection, PublicationCategory>>,
    savedKeys: Set<String>,
    onRead: (ReadingItem) -> Unit,
    onCategory: (PublicationCategory) -> Unit,
    onBack: (() -> Unit)?,
) {
    var query by rememberSaveable(category.slug) { mutableStateOf("") }
    val context = LocalContext.current
    val preferred by ReadingPrefs.language.collectAsState()
    val shown = category.texts.filter { titleMatches(it.title, query) }
    val at = all.indexOfFirst { it.second.slug == category.slug }
    val previous = all.getOrNull(at - 1)?.takeIf { at > 0 }
    val next = all.getOrNull(at + 1)?.takeIf { at >= 0 }
    // Each category starts at its top (previous / next and the strip keep the same screen)
    val listState = rememberSaveable(category.slug, saver = LazyListState.Saver) { LazyListState() }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (onBack != null) {
            item {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
                    Text("Publications", color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
                }
            }
            // Phones: every category as a row of pictures, to switch without going back (the website's strip)
            item { CategoryStrip(all, category.slug, onCategory) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)) {
                PublicationIcon(category, Modifier.size(56.dp))
                Column {
                    Text(category.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = AppColors.ink, modifier = Modifier.semantics { heading() })
                    Text(countLine(section, category), style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.muted)
                }
            }
        }
        item { SearchField("Search ${category.name}", query) { query = it } }
        val languages = readingLanguages(listOf(category))
        if (languages.size > 1) {
            item {
                Box(Modifier.padding(top = 12.dp)) {
                    ReadingIn(languages, preferred) { ReadingPrefs.setLanguage(context, it) }
                }
            }
        }
        item { Spacer(Modifier.padding(top = 12.dp)) }
        if (shown.isEmpty()) {
            item {
                Text("No titles match.", style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft,
                    modifier = Modifier.fillMaxWidth().card())
            }
        }
        itemsIndexed(shown, key = { _, text -> text.id }) { index, text ->
            val shape = when {
                shown.size == 1 -> RoundedCornerShape(16.dp)
                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                index == shown.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Column(Modifier.fillMaxWidth().background(AppColors.card, shape).padding(horizontal = 14.dp)) {
                if (index > 0) HorizontalDivider(color = AppColors.line)
                TextRow(text, saved = savedKey(category.slug, text.title) in savedKeys) {
                    onRead(ReadingItem(text, category.name, null, category.slug))
                }
            }
        }
        item {
            Text("Tap a title to read it. You can zoom in, switch language, share and save it on this phone.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted,
                modifier = Modifier.padding(top = 12.dp))
        }
        // After the last title, the natural next step: the previous or next category
        if (previous != null || next != null) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (previous != null) {
                        TextButton(onClick = { onCategory(previous.second) }) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null, tint = AppColors.clayDark)
                            Text(previous.second.name, color = AppColors.clayDark,
                                modifier = Modifier.semantics { contentDescription = "Previous category: " + previous.second.name })
                        }
                    } else Spacer(Modifier)
                    if (next != null) {
                        TextButton(onClick = { onCategory(next.second) }) {
                            Text(next.second.name, color = AppColors.clayDark,
                                modifier = Modifier.semantics { contentDescription = "Next category: " + next.second.name })
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.clayDark)
                        }
                    }
                }
            }
        }
        item { AddressFooter() }
    }
}

/** A row of round category pictures that swipes sideways; the current one ringed and scrolled into view, and
 *  Shlokas & Puja after a thin line. */
@Composable
private fun CategoryStrip(
    all: List<Pair<PublicationSection, PublicationCategory>>,
    current: String,
    onCategory: (PublicationCategory) -> Unit,
) {
    val list = rememberLazyListState()
    val at = all.indexOfFirst { it.second.slug == current }
    LaunchedEffect(current) { if (at >= 0) list.animateScrollToItem(maxOf(0, at - 2)) }
    LazyRow(
        state = list,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        all.forEachIndexed { i, (section, category) ->
            if (i > 0 && all[i - 1].first.key != section.key) {
                item(key = "line-" + section.key) {
                    VerticalDivider(color = AppColors.lineStrong, modifier = Modifier.padding(top = 8.dp).heightIn(max = 48.dp))
                }
            }
            item(key = category.slug) {
                val on = category.slug == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = { onCategory(category) })
                        .padding(vertical = 4.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = category.name + if (on) ", current category" else ""
                        },
                ) {
                    PublicationIcon(category, Modifier
                        .size(52.dp)
                        .border(if (on) 3.dp else 1.dp, if (on) AppColors.clay else AppColors.line, CircleShape)
                        .padding(if (on) 3.dp else 1.dp)
                        .clip(CircleShape), round = true)
                    Text(category.name, style = MaterialTheme.typography.labelSmall,
                        color = if (on) AppColors.clayDark else AppColors.inkSoft,
                        fontWeight = if (on) FontWeight(700) else null,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** "Reading in": the language every title opens in, kept on the phone (the website's Reading in list). */
@Composable
private fun ReadingIn(languages: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = languages.firstOrNull { it.first.equals(selected, ignoreCase = true) } ?: languages.first()
    fun label(l: Pair<String, String>) = if (l.second == l.first) l.first else l.first + " · " + l.second
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it },
        modifier = Modifier.padding(top = 10.dp)) {
        OutlinedTextField(
            value = label(current),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Reading in") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.card, unfocusedContainerColor = AppColors.card,
                unfocusedBorderColor = AppColors.lineStrong, focusedBorderColor = AppColors.clay,
                focusedLabelColor = AppColors.clayDark,
            ),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(label(language), fontWeight = if (language == current) FontWeight(700) else null) },
                    onClick = { onSelect(language.first); expanded = false },
                )
            }
        }
    }
}

/** This phone's last five searches, as chips that search again. */
@Composable
private fun RecentSearches(recents: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text("Recent searches", style = MaterialTheme.typography.labelLarge, color = AppColors.muted,
            modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { q ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppColors.card)
                        .border(1.dp, AppColors.line, RoundedCornerShape(50))
                        .clickable(role = Role.Button) { onPick(q) }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(16.dp))
                    Text(q, style = MaterialTheme.typography.bodyMedium, color = AppColors.ink,
                        modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** The app's own icon for each of the website's categories, as on the iOS app. */
private val icons = mapOf(
    "01_thodayamangalam" to R.drawable.pub_01_thodayamangalam,
    "02_ganesha" to R.drawable.pub_02_ganesha,
    "03_guru" to R.drawable.pub_03_guru,
    "04_muruga" to R.drawable.pub_04_muruga,
    "05_devi" to R.drawable.pub_05_devi,
    "06_shiva" to R.drawable.pub_06_shiva,
    "07_vishnu" to R.drawable.pub_07_vishnu,
    "08_rama" to R.drawable.pub_08_rama,
    "09_panduranga" to R.drawable.pub_09_panduranga,
    "10_ayyappa" to R.drawable.pub_10_ayyappa,
    "11_hanuman" to R.drawable.pub_11_hanuman,
    "12_shlokas" to R.drawable.pub_12_shlokas,
    "13_pooja" to R.drawable.pub_13_pooja,
    "14_stotram" to R.drawable.pub_14_stotram,
    "15_namavali" to R.drawable.pub_15_namavali,
    "16_thirukalyanam" to R.drawable.pub_16_thirukalyanam,
)

/** Whether the app has its own icon for a category (a new one on the website gets its first letter). */
fun hasIcon(slug: String) = slug.lowercase() in icons

@Composable
private fun PublicationIcon(category: PublicationCategory, modifier: Modifier, round: Boolean = false) {
    val icon = icons[category.slug.lowercase()]
    Box(modifier.clip(if (round) CircleShape else RoundedCornerShape(8.dp)).background(AppColors.claySoft),
        contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Text(category.name.take(1), style = MaterialTheme.typography.headlineMedium, color = AppColors.clay)
        }
    }
}

@Composable
private fun CategoryTile(category: PublicationCategory, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.claySoft else AppColors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) AppColors.clay else AppColors.line,
                RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        PublicationIcon(category, Modifier.size(72.dp))
        FitText(category.name, MaterialTheme.typography.titleSmall)
    }
}

/** One line of text that shrinks (down to 70%) to fit its width rather than being cut: "Thodayamangalam". */
@Composable
private fun FitText(text: String, style: TextStyle) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val limit = constraints.maxWidth
        val size = remember(text, limit, style) {
            generateSequence(1f) { it - 0.05f }.takeWhile { it >= 0.7f }.firstOrNull { scale ->
                measurer.measure(text, style.copy(fontSize = style.fontSize * scale), maxLines = 1).size.width <= limit
            }?.let { style.fontSize * it } ?: (style.fontSize * 0.7f)
        }
        Text(text, style = style.copy(fontSize = size), color = AppColors.ink, maxLines = 1,
            overflow = TextOverflow.Ellipsis, softWrap = false)
    }
}

/** A title in a list, with its languages as small badges (and a bookmark when it's saved on this phone). */
@Composable
private fun TextRow(
    text: PublicationText,
    detail: String? = null,
    title: String = text.title,
    saved: Boolean = false,
    open: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = open)
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted)
            }
        }
        if (saved) {
            Icon(Icons.Filled.Bookmark, contentDescription = "Saved", tint = AppColors.saffron, modifier = Modifier.size(16.dp))
        }
        Text(
            text.languages.joinToString(" · ") { LanguageCode.button(it) },
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.clayDark,
            modifier = Modifier
                .background(AppColors.claySoft, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
                .semantics { contentDescription = text.languages.joinToString(", ") },
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}

@Composable
private fun SearchField(
    prompt: String,
    text: String,
    onFocus: (Boolean) -> Unit = {},
    onSearch: () -> Unit = {},
    onChange: (String) -> Unit,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        placeholder = { Text(prompt, color = AppColors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColors.muted) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search", tint = AppColors.muted)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(); focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.card, unfocusedContainerColor = AppColors.card,
            unfocusedBorderColor = AppColors.lineStrong, focusedBorderColor = AppColors.clay,
            cursorColor = AppColors.clay,
        ),
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocus(it.isFocused) },
    )
}
