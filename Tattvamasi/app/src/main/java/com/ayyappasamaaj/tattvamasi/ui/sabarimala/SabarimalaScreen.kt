@file:OptIn(ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.sabarimala

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ScreenTitle
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideContent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideScreen
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/** One of the website's pilgrimage guides, as a card: [slug] is its slug in /guides/app.json. */
data class Guide(val slug: String, val title: String, val note: String, val image: Int?, val path: String) {
    val url: String get() = "https://ayyappasamaaj.org/$path"
}

/** The guides after the main Sabarimala guide, as on the iOS app and the website. */
val guides = listOf(
    Guide("traditional-path", "Traditional Path", "The Periya Paatha, the long route Lord Ayyappa took",
        R.drawable.guide_traditional_path, "traditional-path/"),
    Guide("vratam", "Vratam", "The vow for the pilgrimage: what to do and not do", R.drawable.guide_vratam, "vratam/"),
    Guide("virtual-q", "Virtual Q", "Book your Sannidhanam darshan slot, step by step", null, "sabarimala-virtual-q/"),
    Guide("house-puja", "House Puja", "Host an Ayyappa Puja and Bhajan at home", R.drawable.guide_house_puja, "house-puja-bhajan/"),
)

private val shrines = listOf("Kulathupuzha", "Aryankavu", "Achankovil", "Erumeli", "Sabarimala")

/**
 * Sabarimala: the website's pilgrimage guides, read in the app (ui/guides): each card opens its guide's screen.
 * [open] is the guide shown (and the section to go to), or null for the cards; Back returns to the cards. A guide
 * the app has no copy of opens on ayyappasamaaj.org instead. Two columns of guides on a tablet.
 */
@Composable
fun SabarimalaScreen(
    content: List<GuideContent>,
    open: GuideTarget? = null,
    onOpen: (GuideTarget?) -> Unit = {},
) {
    val context = LocalContext.current
    val shown = open?.let { target -> content.firstOrNull { it.slug == target.slug } }
    if (shown != null) {
        BackHandler { onOpen(null) }
        GuideScreen(shown, open.section, onBack = { onOpen(null) }, onShown = { onOpen(GuideTarget(shown.slug)) })
        return
    }
    fun show(slug: String, url: String) {
        if (content.any { it.slug == slug }) onOpen(GuideTarget(slug)) else openWebPage(context, url)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { ScreenTitle("Sabarimala") }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MainGuide { show("sabarimala", "https://ayyappasamaaj.org/sabarimala/") }
        }
        items(guides, key = { it.path }) { guide -> GuideRow(guide) { show(guide.slug, guide.url) } }
        item(span = { GridItemSpan(maxLineSpan) }) { AddressFooter() }
    }
}

private val cardShape = RoundedCornerShape(18.dp)

/** The Sabarimala guide: the Sannidhanam picture, and the five shrines it covers. */
@Composable
private fun MainGuide(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(AppColors.card)
            .border(1.dp, AppColors.line, cardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {},
    ) {
        // 150dp tall on a phone, taller on a tablet so the wide card doesn't crop it to a strip
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Image(
                painterResource(R.drawable.guide_sabarimala),
                contentDescription = "The Sabarimala Sannidhanam",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height((maxWidth / 2.6f).coerceIn(150.dp, 300.dp)),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sabarimala guide", style = MaterialTheme.typography.titleLarge, color = AppColors.ink)
            Text(
                "The five shrines of the Western Ghats, from the child at Kulathupuzha to the Yogi on the Hill.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                shrines.forEach { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight(600)),
                        color = AppColors.clayDark,
                        modifier = Modifier
                            .background(AppColors.claySoft, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** A guide card: its picture (or the Virtual Q icon), title and a line about it. */
@Composable
private fun GuideRow(guide: Guide, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.card)
            .border(1.dp, AppColors.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.forest),
            contentAlignment = Alignment.Center,
        ) {
            if (guide.image != null) {
                Image(painterResource(guide.image), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = AppColors.saffron,
                    modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(guide.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AppColors.ink)
            Text(guide.note, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}
