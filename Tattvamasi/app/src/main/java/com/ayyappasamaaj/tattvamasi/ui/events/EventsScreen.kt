@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.events

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.DateBlock
import com.ayyappasamaaj.tattvamasi.ui.ScreenTitle
import com.ayyappasamaaj.tattvamasi.ui.SectionLabel
import com.ayyappasamaaj.tattvamasi.ui.addToCalendar
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openDirections
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Events: upcoming events by month, each with Details (the app's own event screen), Add to calendar and
 * Directions. Pull down to refresh. One column on a phone, two or more side by side on a tablet. [openKey] is the
 * event shown (its AppEvent.key), or null for the list; Back returns to the list.
 */
@Composable
fun EventsScreen(
    state: EventsState,
    refresh: suspend () -> Unit,
    openKey: String? = null,
    onOpen: (String?) -> Unit = {},
) {
    val open = openKey?.let { key -> state.events.firstOrNull { it.key == key } }
    if (open != null) {
        BackHandler { onOpen(null) }
        EventScreen(open, onBack = { onOpen(null) })
        return
    }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try { refresh() } finally { refreshing = false }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(340.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { ScreenTitle("Events") }
                when {
                    !state.loaded -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.clay)
                        }
                    }
                    state.events.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (state.failed) {
                                "Couldn't load the events. Check the connection, then pull down to try again."
                            } else {
                                "No upcoming events right now. New events appear here as soon as they're on " +
                                    "ayyappasamaaj.org."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.inkSoft,
                            modifier = Modifier.fillMaxWidth().card(),
                        )
                    }
                    else -> groupByMonth(state.events).forEach { month ->
                        item(key = "month-" + month.title, span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel(month.title, Modifier.padding(top = 6.dp))
                        }
                        items(month.events, key = { it.key }) { event -> EventCard(event) { onOpen(event.key) } }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { AddressFooter() }
            }
        }
    }
}

@Composable
fun EventCard(event: AppEvent, openDetails: () -> Unit) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Event details", onClick = openDetails)
            .card(padding = 14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateBlock(event.start)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(event.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AppColors.ink)
                Text(event.timeText, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                Text(event.shortVenue, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = AppColors.muted)
                event.extraSummary?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        // Side by side when they fit; they wrap onto more lines with large text.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = openDetails,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest),
            ) {
                Text("Details", fontWeight = FontWeight(700))
            }
            OutlinedButton(onClick = { addToCalendar(context, event) }) {
                Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = AppColors.clay,
                    modifier = Modifier.size(18.dp))
                Text("Add to calendar", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = { openDirections(context, event) }) {
                Icon(Icons.Outlined.Directions, contentDescription = null, tint = AppColors.clay,
                    modifier = Modifier.size(18.dp))
                Text("Directions", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
