package com.ayyappasamaaj.tattvamasi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.DateBlock
import com.ayyappasamaaj.tattvamasi.ui.Destination
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.WebPages
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.events.EventsState
import com.ayyappasamaaj.tattvamasi.ui.openInBrowser
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/** Home: the next event, then the app's sections and Donate. As on the iOS app (HomeView.swift). */
@Composable
fun HomeScreen(events: EventsState, go: (Destination) -> Unit, openEvent: (String) -> Unit = {}) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ReadableWidth {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Swamiye Saranam",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                NextEvent(events, seeEvents = { go(Destination.Events) }, open = openEvent)
                Tiles(
                    listOf(
                        Tile("Publications", "Bhajans, shlokas and puja texts", Icons.AutoMirrored.Outlined.MenuBook) {
                            go(Destination.Publications)
                        },
                        Tile("Sabarimala", "Guide, Vratam, Virtual Q and more", Icons.Outlined.Landscape) {
                            go(Destination.Sabarimala)
                        },
                        Tile("About us", "Who we are, our Board, contact", Icons.Outlined.Info) {
                            go(Destination.About)
                        },
                        Tile("Donate", "Support Lord Ayyappa's seva; tax-deductible", Icons.Outlined.FavoriteBorder) {
                            openInBrowser(context, WebPages.DONATE)
                        },
                    ),
                )
                AddressFooter()
            }
        }
    }
}

@Composable
private fun NextEvent(events: EventsState, seeEvents: () -> Unit, open: (String) -> Unit) {
    val event = events.nextEvent
    when {
        event != null -> NextEventCard(event, seeEvents) { open(event.key) }
        !events.loaded -> Box(Modifier.fillMaxWidth().heightIn(min = 100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.clay)
        }
    }
}

/** The dark green card with the next event. */
@Composable
private fun NextEventCard(event: AppEvent, seeEvents: () -> Unit, open: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Event details", onClick = open)
            .background(AppColors.forest, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        DateBlock(event.start, size = 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "NEXT EVENT",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
                color = AppColors.saffron,
            )
            Text(event.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp), color = AppColors.cream)
            Text(
                "${event.timeText} · ${event.shortVenue}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = AppColors.cream.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = seeEvents,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.saffron, contentColor = AppColors.forest),
            ) {
                Text("See events", fontWeight = FontWeight(700))
            }
        }
    }
}

private data class Tile(val title: String, val note: String, val icon: ImageVector, val onClick: () -> Unit)

/** The section tiles: two across on a phone, more on a tablet (each at least 160dp wide). */
@Composable
private fun Tiles(tiles: List<Tile>) {
    BoxWithConstraints {
        val columns = homeColumns(maxWidth.value)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tiles.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { tile -> TileCard(tile, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** How many tiles fit across: 2 on a phone, up to 4 on a tablet. */
fun homeColumns(widthDp: Float): Int = (widthDp / 172f).toInt().coerceIn(2, 4)

@Composable
private fun TileCard(tile: Tile, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 124.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = tile.onClick)
            .card(),
    ) {
        Box(
            Modifier.size(40.dp).background(AppColors.claySoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = AppColors.clay)
        }
        Text(tile.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AppColors.ink)
        Text(tile.note, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = AppColors.muted)
    }
}
