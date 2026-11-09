package com.ayyappasamaaj.tattvamasi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.ui.about.AboutScreen
import com.ayyappasamaaj.tattvamasi.ui.events.EventsScreen
import com.ayyappasamaaj.tattvamasi.ui.events.EventsStore
import com.ayyappasamaaj.tattvamasi.ui.home.HomeScreen
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsScreen
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsStore
import com.ayyappasamaaj.tattvamasi.ui.sabarimala.SabarimalaScreen

/** The app's five sections, as on the iOS app: a bottom bar on a phone, a side rail on a tablet or an
 *  unfolded foldable, chosen by NavigationSuiteScaffold from the window's size. */
enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Events("Events", Icons.Outlined.CalendarMonth),
    Publications("Publications", Icons.AutoMirrored.Outlined.MenuBook),
    Sabarimala("Sabarimala", Icons.Outlined.Landscape),
    About("About us", Icons.Outlined.Info),
}

@Composable
fun AppRoot() {
    var current by rememberSaveable { mutableStateOf(Destination.Home) }
    LaunchedEffect(Unit) { EventsStore.start() }
    val events by EventsStore.state.collectAsState()
    val appContext = LocalContext.current
    LaunchedEffect(Unit) { PublicationsStore.start(appContext) }
    val publications by PublicationsStore.state.collectAsState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { destination ->
                item(
                    selected = destination == current,
                    onClick = { current = destination },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    // 12sp so "Publications" fits the narrowest phones' five-item bar
                    label = {
                        Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                             style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                    },
                )
            }
        },
        containerColor = AppColors.cream,
    ) {
        AppScreen {
            when (current) {
                Destination.Home -> HomeScreen(events) { current = it }
                Destination.Events -> EventsScreen(events, EventsStore::refresh)
                Destination.Publications -> PublicationsScreen(publications)
                Destination.Sabarimala -> SabarimalaScreen()
                Destination.About -> AboutScreen()
            }
        }
    }
}

/** The website's banner: the Sabarimala picture behind the logo and the tagline, reaching up behind the
 *  status bar (the app draws edge to edge). A tablet gets the website's tablet header picture (2048px wide,
 *  aligned right as on the website), which stays sharp at that width. */
@Composable
fun BannerHeader() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cream),
    ) {
        Image(
            painter = painterResource(if (maxWidth >= 600.dp) R.drawable.app_banner_wide else R.drawable.app_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = if (maxWidth >= 600.dp) Alignment.CenterEnd else Alignment.BottomEnd,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
                .semantics(mergeDescendants = true) { heading() },
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Ayyappa Samaaj",
                contentScale = ContentScale.Fit,
                modifier = Modifier.widthIn(max = 236.dp),
            )
            Text(
                "Gathering Place for Ayyappa Devotees Worldwide",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}

/** A section's screen: the banner, then the content on the cream page. */
@Composable
fun AppScreen(content: @Composable () -> Unit) {
    Scaffold(
        containerColor = AppColors.cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),  // the banner takes the status bar; the bar or rail the rest
        topBar = { BannerHeader() },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
        }
    }
}
