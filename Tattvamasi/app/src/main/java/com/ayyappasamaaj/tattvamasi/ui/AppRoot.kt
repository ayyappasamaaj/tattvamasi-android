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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.ayyappasamaaj.tattvamasi.ui.festivals.FestivalsScreen
import com.ayyappasamaaj.tattvamasi.ui.account.AccountSheet
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.donate.DonateSheet
import com.ayyappasamaaj.tattvamasi.ui.register.RegisterSheet
import com.ayyappasamaaj.tattvamasi.ui.events.EventsScreen
import com.ayyappasamaaj.tattvamasi.ui.events.EventsStore
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideContent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.guides.GuidesStore
import com.ayyappasamaaj.tattvamasi.ui.home.SlidesStore
import com.ayyappasamaaj.tattvamasi.ui.guides.guideTarget
import com.ayyappasamaaj.tattvamasi.ui.home.HomeScreen
import com.ayyappasamaaj.tattvamasi.ui.publications.PdfPopup
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsScreen
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.ui.publications.ReadingItem
import com.ayyappasamaaj.tattvamasi.ui.publications.PublicationsStore
import com.ayyappasamaaj.tattvamasi.ui.sabarimala.SabarimalaScreen
import com.ayyappasamaaj.tattvamasi.ui.text.LocalLinkHandler
import com.ayyappasamaaj.tattvamasi.ui.yatra.PlayerBar
import com.ayyappasamaaj.tattvamasi.ui.shruti.ShrutiBar
import com.ayyappasamaaj.tattvamasi.ui.shruti.ShrutiPlayer
import com.ayyappasamaaj.tattvamasi.ui.shruti.TanpuraSheet
import com.ayyappasamaaj.tattvamasi.ui.yatra.NamavaliPlayer
import com.ayyappasamaaj.tattvamasi.ui.yatra.YatraScreen

/** The app's five sections, as on the iOS app and in the website menu's order: a bottom bar on a phone, a side rail
 *  on a tablet or an unfolded foldable, chosen by NavigationSuiteScaffold from the window's size. About moved to the
 *  top-right menu for Festivals (AppMenu.kt, Nov 25, 2026). */
enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Events("Events", Icons.Outlined.CalendarMonth),
    Festivals("Festivals", Icons.Outlined.Celebration),
    Sabarimala("Sabarimala", Icons.Outlined.Landscape),
    Publications("Publications", Icons.AutoMirrored.Outlined.MenuBook),
}

/** The screen a Debug build was started on for the store screenshots (MainActivity, `--es shot <screen>`). */
object Shot {
    var screen: String? = null
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
    var openFestival by rememberSaveable { mutableStateOf<String?>(null) }  // the same, on the Festivals tab
    val context = LocalContext.current
    LaunchedEffect(Unit) { AccountStore.init(context) }  // signed in from last time (ui/account)
    LaunchedEffect(Unit) { ShrutiPlayer.init(context) }  // the tanpura's remembered key, for the Tanpura row
    LaunchedEffect(Unit) { GuidesStore.start(context) }
    LaunchedEffect(Unit) { SlidesStore.start(context) }
    val slides by SlidesStore.state.collectAsState()
    val guides by GuidesStore.state.collectAsState()
    val openingDates by GuidesStore.dates.collectAsState()
    val festivals by GuidesStore.festivals.collectAsState()
    // The Sahasranama Yatra, open on the tab it was opened from (Home or Publications), as on iOS
    var homeYatra by rememberSaveable { mutableStateOf(false) }
    var publicationsYatra by rememberSaveable { mutableStateOf(false) }
    var reading by remember { mutableStateOf<ReadingItem?>(null) }  // the text open on Publications (PdfReader.kt)
    // Keeps each tab's yatra where it was (a day, its step) while another tab is shown
    val yatraPlaces = rememberSaveableStateHolder()

    // The store screenshots (scripts/store-screenshots.sh), Debug builds only: started with `--es shot <screen>`, the
    // app opens that screen by itself, so no taps are needed
    LaunchedEffect(publications.loaded) {
        if (!BuildConfig.DEBUG) return@LaunchedEffect
        when (Shot.screen) {
            "publications" -> current = Destination.Publications
            "tanpura" -> { current = Destination.Publications; ShrutiPlayer.openSheet() }
            "reader" -> {
                current = Destination.Publications
                // A text in several languages, so its round language button shows
                val category = publications.sections.flatMap { it.categories }
                    .firstOrNull { c -> c.texts.any { it.versions.size >= 4 } }
                val text = category?.texts?.firstOrNull { it.versions.size >= 4 }
                if (reading == null && category != null && text != null) {
                    reading = ReadingItem(text, category.name, null, category.slug)
                }
            }
            "yatra" -> homeYatra = true
            "events" -> current = Destination.Events
            "sabarimala" -> current = Destination.Sabarimala
        }
    }

    // A link to one of the guides (in an event's text, or in another guide) opens the guide in the app
    val openLink: (String) -> Boolean = remember(guides) {
        { url ->
            val target = guideTarget(url, guides)
            if (target != null) showGuide(target, guides, { openGuide = it }, { openFestival = it }) { current = it }
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
            is NotificationTarget.Guide -> showGuide(target.target, guides, { openGuide = it }, { openFestival = it }) { current = it }
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
                            if (destination == Destination.Festivals && current == Destination.Festivals) openFestival = null
                            if (destination == Destination.Home && current == Destination.Home) { homeYatra = false; yatraPlaces.removeState("home") }
                            if (destination == Destination.Publications && current == Destination.Publications) {
                                reading = null
                                publicationsYatra = false
                                yatraPlaces.removeState("publications")
                            }
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
            // The yatra has its own bar at the top, in place of the banner; Back returns to the tab's own page
            if (current == Destination.Home && homeYatra) {
                yatraPlaces.SaveableStateProvider("home") {
                    YatraScreen(back = "Home", onClose = { homeYatra = false; yatraPlaces.removeState("home") })
                }
                return@NavigationSuiteScaffold
            }
            if (current == Destination.Publications && publicationsYatra) {
                yatraPlaces.SaveableStateProvider("publications") {
                    YatraScreen(back = "Publications", onClose = { publicationsYatra = false; yatraPlaces.removeState("publications") })
                }
                return@NavigationSuiteScaffold
            }
            // A text from Publications slides up over the page (the banner too), above the player and navigation bars
            AppScreen(overlay = {
                if (current == Destination.Publications) reading?.let { PdfPopup(it) { reading = null } }
            }) {
                when (current) {
                    Destination.Home -> HomeScreen(events, go = {
                        if (it == Destination.Events) openEvent = null  // "See events": the list
                        current = it
                    }, openEvent = { key ->
                        openEvent = key
                        current = Destination.Events
                    }, openYatra = { homeYatra = true }, slides = slides)
                    Destination.Events -> EventsScreen(events, EventsStore::refresh, openEvent) { openEvent = it }
                    Destination.Publications -> PublicationsScreen(publications, openYatra = { publicationsYatra = true },
                        read = { reading = it })
                    Destination.Sabarimala -> SabarimalaScreen(guides, openGuide?.let(GuideTarget::decode), openingDates) {
                        openGuide = it?.encode()
                    }
                    Destination.Festivals -> FestivalsScreen(guides, festivals, openFestival?.let(GuideTarget::decode)) {
                        openFestival = it?.encode()
                    }
                }
            }
        }
        // The tanpura's sheet, over any screen (opened from the Tanpura row or the tanpura's bar)
        TanpuraSheet()
        // Donate, over any screen (Home's Donate tile), and My account above it (Donate's Sign In opens it)
        DonateSheet()
        RegisterSheet()
        // About or Settings (the top-right menu), then My account above them
        AppMenuSheet()
        AccountSheet()
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
                color = Color(0xFF2A2620),  // dark in both modes: the backing stays white
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
        // The menu (AppMenu.kt): My account (signing in is optional), About, Settings; a small button at the corner, on
        // a white backing so it shows on the picture in both modes
        MenuButton(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 2.dp, end = 6.dp))
    }
}

/** The tanpura's bar above the Namavali's player bar, each while in use; the lowest clears the phone's own bar. */
@Composable
fun PlayerBars() {
    val listen by NamavaliPlayer.state.collectAsState()
    Column {
        ShrutiBar(bottomInset = !listen.shown)
        PlayerBar()
    }
}

/** A section's screen: the banner, then the content on the cream page; [overlay] (a text being read) covers both,
 *  above the player bar. */
@Composable
fun AppScreen(overlay: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = AppColors.cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),  // the banner takes the status bar; the bar or rail the rest
        // The tanpura's bar and the Namavali's, while each is in use (ui/shruti, ui/yatra/PlayerBar.kt)
        bottomBar = { PlayerBars() },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                BannerHeader()
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            }
            overlay()
        }
    }
}


/** A guide opens on its own tab: a Festivals & Pujas page on Festivals, the others on Sabarimala. */
private fun showGuide(target: GuideTarget, guides: List<GuideContent>, sabarimala: (String) -> Unit,
                      festivals: (String) -> Unit, go: (Destination) -> Unit) {
    if (guides.firstOrNull { it.slug == target.slug }?.tab == "festivals") {
        festivals(target.encode())
        go(Destination.Festivals)
    } else {
        sabarimala(target.encode())
        go(Destination.Sabarimala)
    }
}
