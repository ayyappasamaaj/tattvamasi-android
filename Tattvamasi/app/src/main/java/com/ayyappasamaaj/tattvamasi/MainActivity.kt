package com.ayyappasamaaj.tattvamasi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ayyappasamaaj.tattvamasi.notifications.AppNotifications
import com.ayyappasamaaj.tattvamasi.ui.AppRoot
import com.ayyappasamaaj.tattvamasi.ui.theme.Appearance
import com.ayyappasamaaj.tattvamasi.ui.theme.TattvamasiTheme
import com.ayyappasamaaj.tattvamasi.ui.theme.appIsDark

/** The redesigned app (Compose), one activity for every section. The system splash screen shows the app
 *  icon while it starts; the app then draws edge to edge, behind the status and navigation bars. The bars'
 *  icons follow the app's look (Appearance: the phone's setting, or Light or Dark), not the phone's alone:
 *  dark icons on the cream, light ones on the dark page. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        Appearance.load(this)
        super.onCreate(savedInstanceState)
        AppNotifications.start(this)
        if (savedInstanceState == null) AppNotifications.tapped(intent?.getStringExtra(EXTRA_URL))
        setContent {
            val dark = appIsDark()
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            TattvamasiTheme {
                AppRoot()
            }
        }
    }

    /** A notification tapped while the app is open (the activity is singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppNotifications.tapped(intent.getStringExtra(EXTRA_URL))
    }

    companion object {
        /** The website puts the page to open in the notification's data as "url" (docs/app-notifications.md). */
        const val EXTRA_URL = "url"
    }
}
