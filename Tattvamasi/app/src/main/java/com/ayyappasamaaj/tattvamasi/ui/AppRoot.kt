package com.ayyappasamaaj.tattvamasi.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
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
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.view.AboutUsActivity
import com.ayyappasamaaj.tattvamasi.view.ArticlesActivity
import com.ayyappasamaaj.tattvamasi.view.BhajansActivity
import com.ayyappasamaaj.tattvamasi.view.EventsActivity
import com.ayyappasamaaj.tattvamasi.view.HomeActivity

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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { destination ->
                item(
                    selected = destination == current,
                    onClick = { current = destination },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(destination.label, maxLines = 1) },
                )
            }
        },
        containerColor = AppColors.cream,
    ) {
        AppScreen {
            when (current) {
                Destination.Home -> ComingNext(
                    "Swamiye Saranam", "The new Home screen comes in the next step.", HomeActivity::class.java)
                Destination.Events -> ComingNext(
                    "Events", "The new Events screen comes in the next step.", EventsActivity::class.java)
                Destination.Publications -> ComingNext(
                    "Publications", "Bhajans, shlokas and puja texts: the new screen comes in step 3.",
                    BhajansActivity::class.java)
                Destination.Sabarimala -> ComingNext(
                    "Sabarimala", "The guide, Traditional Path, Vratam, Virtual Q and House Puja: step 4.",
                    ArticlesActivity::class.java)
                Destination.About -> ComingNext(
                    "About us", "About us, our Board and Contact: step 4.", AboutUsActivity::class.java)
            }
        }
    }
}

/** The website's banner: the Sabarimala picture behind the logo and the tagline, reaching up behind the
 *  status bar (the app draws edge to edge). */
@Composable
fun BannerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cream),
    ) {
        Image(
            painter = painterResource(R.drawable.app_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomEnd,
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

/** Shown in a section whose new screen comes in a later step, with a button to the app's current screen. */
@Composable
fun ComingNext(title: String, note: String, legacy: Class<out Activity>) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(note, style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { context.startActivity(Intent(context, legacy)) },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest),
        ) {
            Text("Open the current screen")
        }
    }
}
