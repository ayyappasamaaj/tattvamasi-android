@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Publications: Bhajans, then Shlokas & Puja, as on the website, with one search for everything. A tile opens the
 * category's texts; a title opens the reader. On a phone the category replaces the tiles (Back returns); on a
 * tablet the tiles stay on the left and the category shows on the right, like the iPad's split view.
 */
@Composable
fun PublicationsScreen(state: PublicationsState) {
    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf<ReadingItem?>(null) }
    val selected = state.sections.flatMap { s -> s.categories.map { s to it } }.firstOrNull { it.second.slug == selectedSlug }
    val open = { item: ReadingItem -> reading = item }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            val shown = selected ?: state.sections.firstOrNull()?.let { s -> s.categories.firstOrNull()?.let { s to it } }
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(340.dp).fillMaxHeight()) {
                    PublicationsHome(state, selectedSlug = shown?.second?.slug, onCategory = { selectedSlug = it.slug },
                        onRead = open, minTile = 96)
                }
                VerticalDivider(color = AppColors.line)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    shown?.let { (section, category) ->
                        CategoryContent(section, category, onRead = open, onBack = null)
                    }
                }
            }
        } else if (selected != null) {
            BackHandler { selectedSlug = null }
            CategoryContent(selected.first, selected.second, onRead = open, onBack = { selectedSlug = null })
        } else {
            PublicationsHome(state, selectedSlug = null, onCategory = { selectedSlug = it.slug }, onRead = open,
                minTile = 100)
        }
    }

    reading?.let { PdfReader(it) { reading = null } }
}

/** The title, the search for everything, and each section's tiles (or the search's results). */
@Composable
private fun PublicationsHome(
    state: PublicationsState,
    selectedSlug: String?,
    onCategory: (PublicationCategory) -> Unit,
    onRead: (ReadingItem) -> Unit,
    minTile: Int,
) {
    var query by rememberSaveable { mutableStateOf("") }
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
        item(span = full) { SearchField("Search all bhajans and shlokas", query) { query = it } }
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
                                TextRow(hit.text, detail = hit.category.name) {
                                    onRead(ReadingItem(hit.text, hit.category.name, null))
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
            else -> state.sections.forEach { section ->
                item(key = "section-" + section.key, span = full) {
                    Text(section.title, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                }
                items(section.categories, key = { it.slug }) { category ->
                    CategoryTile(category, selected = category.slug == selectedSlug) { onCategory(category) }
                }
            }
        }
        item(span = full) { AddressFooter() }
    }
}

/** One category's texts, with its own search and a language switch. */
@Composable
private fun CategoryContent(
    section: PublicationSection,
    category: PublicationCategory,
    onRead: (ReadingItem) -> Unit,
    onBack: (() -> Unit)?,
) {
    var query by rememberSaveable(category.slug) { mutableStateOf("") }
    var language by rememberSaveable(category.slug) { mutableStateOf(ALL) }
    val shown = category.texts.filter {
        (language == ALL || language in it.languages) && titleMatches(it.title, query)
    }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)) {
                PublicationIcon(category, Modifier.size(56.dp))
                Column {
                    Text(category.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = AppColors.ink, modifier = Modifier.semantics { heading() })
                    Text(section.count(category.texts.size), style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.muted)
                }
            }
        }
        item { SearchField("Search ${category.name}", query) { query = it } }
        if (category.languages.size > 1) {
            item {
                Box(Modifier.padding(top = 12.dp)) {
                    LanguageChoice(listOf(ALL) + category.languages, emptyMap(), language) { language = it }
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
                TextRow(text) {
                    onRead(ReadingItem(text, category.name, language.takeUnless { it == ALL }))
                }
            }
        }
        item {
            Text("Tap a title to read it. You can zoom in, switch language and share.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted,
                modifier = Modifier.padding(top = 12.dp))
        }
        item { AddressFooter() }
    }
}

private const val ALL = "All"

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
private fun PublicationIcon(category: PublicationCategory, modifier: Modifier) {
    val icon = icons[category.slug.lowercase()]
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.claySoft), contentAlignment = Alignment.Center) {
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

/** A title in a list, with its languages as small badges. */
@Composable
private fun TextRow(text: PublicationText, detail: String? = null, open: () -> Unit) {
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
            Text(text.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted)
            }
        }
        Text(
            text.languages.joinToString(" · ") { LanguageCode.short(it) },
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
private fun SearchField(prompt: String, text: String, onChange: (String) -> Unit) {
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
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.card, unfocusedContainerColor = AppColors.card,
            unfocusedBorderColor = AppColors.lineStrong, focusedBorderColor = AppColors.clay,
            cursorColor = AppColors.clay,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * All / English / Tamil / ...: segmented buttons for up to four choices, a drop-down for more. [labels] gives a
 * choice's own name ("தமிழ்"), when there is one.
 */
@Composable
fun LanguageChoice(options: List<String>, labels: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    if (options.size <= 4) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AppColors.claySoft, activeContentColor = AppColors.clayDark,
                        inactiveContainerColor = AppColors.card,
                    ),
                ) {
                    Text(labels[option] ?: option, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = labels[selected] ?: selected,
                onValueChange = {},
                readOnly = true,
                label = { Text("Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.card, unfocusedContainerColor = AppColors.card,
                    focusedBorderColor = AppColors.clay, focusedLabelColor = AppColors.clayDark,
                ),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labels[option] ?: option, fontWeight = if (option == selected) FontWeight(700) else null) },
                        onClick = { onSelect(option); expanded = false },
                    )
                }
            }
        }
    }
}
