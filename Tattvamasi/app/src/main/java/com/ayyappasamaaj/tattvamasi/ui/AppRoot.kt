package com.ayyappasamaaj.tattvamasi.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.notifications.AppNotifications
import com.ayyappasamaaj.tattvamasi.notifications.NotificationTarget
import com.ayyappasamaaj.tattvamasi.notifications.notificationTarget
import kotlinx.coroutines.delay
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.ui.about.AboutScreen
import com.ayyappasamaaj.tattvamasi.ui.events.EventsScreen
import com.ayyappasamaaj.tattvamasi.ui.events.EventsStore
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.guides.GuidesStore
import com.ayyappasamaaj.tattvamasi.ui.guides.guideTarget
import com.ayyappasamaaj.tattvamasi.ui.home.HomeScreen
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsScreen
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsStore
import com.ayyappasamaaj.tattvamasi.ui.sabarimala.SabarimalaScreen
import com.ayyappasamaaj.tattvamasi.ui.text.LocalLinkHandler
import com.ayyappasamaaj.tattvamasi.ui.yatra.YatraScreen

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
    var openEvent by rememberSaveable { mutableStateOf<String?>(null) }  // AppEvent.key of the event shown
    LaunchedEffect(Unit) { EventsStore.start() }
    val events by EventsStore.state.collectAsState()
    LaunchedEffect(Unit) { PublicationsStore.start() }
    val publications by PublicationsStore.state.collectAsState()
    var openGuide by rememberSaveable { mutableStateOf<String?>(null) }  // GuideTarget.encode() of the guide shown
    val context = LocalContext.current
    LaunchedEffect(Unit) { GuidesStore.start(context) }
    val guides by GuidesStore.state.collectAsState()
    var yatraOpen by rememberSaveable { mutableStateOf(false) }  // the Sahasranama Yatra, under Publications
    val openYatra = {
        yatraOpen = true
        current = Destination.Publications
    }

    // A link to one of the guides (in an event's text, or in another guide) opens the guide in the app
    val openLink: (String) -> Boolean = remember(guides) {
        { url ->
            val target = guideTarget(url, guides)
            if (target != null) {
                openGuide = target.encode()
                current = Destination.Sabarimala
            }
            target != null
        }
    }

    // A tapped notification: its event, guide or page (website: docs/app-notifications.md), once the events have
    // loaded (an event's page needs them)
    val tapped by AppNotifications.tapped.collectAsState()
    LaunchedEffect(tapped, events, guides) {
        val url = tapped ?: return@LaunchedEffect
        if (!events.loaded && !events.failed) return@LaunchedEffect
        when (val target = notificationTarget(url, events.events, guides)) {
            is NotificationTarget.Event -> { openEvent = target.key; current = Destination.Events }
            is NotificationTarget.Guide -> { openGuide = target.target.encode(); current = Destination.Sabarimala }
            is NotificationTarget.Web -> openWebPage(context, target.url)
            NotificationTarget.Events -> { openEvent = null; current = Destination.Events }
            NotificationTarget.Home -> current = Destination.Home
        }
        AppNotifications.opened()
    }

    // Notifications: the system prompt (Android 13+) the first time an event is opened, once its screen shows;
    // whatever the answer, the switches (all on) then take effect
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        AppNotifications.asked(context)
    }
    LaunchedEffect(openEvent) {
        if (openEvent == null || !AppNotifications.shouldAsk()) return@LaunchedEffect
        delay(1200)
        val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPrompt) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else AppNotifications.asked(context)
    }

    CompositionLocalProvider(LocalLinkHandler provides openLink) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Destination.entries.forEach { destination ->
                    item(
                        selected = destination == current,
                        onClick = {
                            if (destination == Destination.Events && current == Destination.Events) openEvent = null
                            if (destination == Destination.Sabarimala && current == Destination.Sabarimala) openGuide = null
                            if (destination == Destination.Publications && current == Destination.Publications) yatraOpen = false
                            current = destination
                        },
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
            // The yatra has its own bar at the top, in place of the banner
            if (current == Destination.Publications && yatraOpen) {
                YatraScreen(onClose = { yatraOpen = false })
                return@NavigationSuiteScaffold
            }
            AppScreen {
                when (current) {
                    Destination.Home -> HomeScreen(events, go = {
                        if (it == Destination.Events) openEvent = null  // "See events": the list
                        current = it
                    }, openEvent = { key ->
                        openEvent = key
                        current = Destination.Events
                    }, openYatra = openYatra)
                    Destination.Events -> EventsScreen(events, EventsStore::refresh, openEvent) { openEvent = it }
                    Destination.Publications -> PublicationsScreen(publications, openYatra)
                    Destination.Sabarimala -> SabarimalaScreen(guides, openGuide?.let(GuideTarget::decode)) {
                        openGuide = it?.encode()
                    }
                    Destination.About -> AboutScreen()
                }
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
