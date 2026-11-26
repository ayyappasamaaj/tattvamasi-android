package com.ayyappasamaaj.tattvamasi.ui.festivals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Celebration
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ScreenTitle
import com.ayyappasamaaj.tattvamasi.ui.guides.Festival
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideContent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideScreen
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/** Until the website sends its Festivals & Pujas list (or offline with an older saved copy): House Puja, which the
 *  app has always had. */
private val fallback = listOf(
    Festival("house-puja", "House Puja and Bhajan", "Host an Ayyappa Puja and Bhajan at home", "",
        "https://ayyappasamaaj.org/house-puja-bhajan/"),
)

/**
 * Festivals & Pujas: the website menu's festival and puja pages (website: pages/app_guides.py "festivals"), each read
 * in the app as a guide (ui/guides). [open] is the page shown (and the section to go to), or null for the list; Back
 * returns to the list. A page the app has no copy of opens on ayyappasamaaj.org instead. As the iOS app's
 * FestivalsView.swift.
 */
@Composable
fun FestivalsScreen(
    content: List<GuideContent>,
    festivals: List<Festival>,
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
    val list = festivals.ifEmpty { fallback.filter { f -> content.any { it.slug == f.slug } } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { ScreenTitle("Festivals & Pujas") }
        items(list, key = { it.slug }) { festival ->
            FestivalRow(festival) {
                if (content.any { it.slug == festival.slug }) onOpen(GuideTarget(festival.slug))
                else openWebPage(context, festival.url)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { AddressFooter() }
    }
}

@Composable
private fun FestivalRow(festival: Festival, onClick: () -> Unit) {
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
            when {
                festival.image.isNotEmpty() -> AsyncImage(model = festival.image, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                festival.slug == "house-puja" -> Image(painterResource(R.drawable.guide_house_puja), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Icon(Icons.Outlined.Celebration, contentDescription = null, tint = AppColors.saffron,
                    modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(festival.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AppColors.ink)
            if (festival.summary.isNotBlank()) {
                Text(festival.summary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = AppColors.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
    }
}
