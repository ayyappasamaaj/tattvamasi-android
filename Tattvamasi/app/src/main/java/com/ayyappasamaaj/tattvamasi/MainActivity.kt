package com.ayyappasamaaj.tattvamasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ayyappasamaaj.tattvamasi.ui.AppRoot
import com.ayyappasamaaj.tattvamasi.ui.theme.TattvamasiTheme

/** The redesigned app (Compose), one activity for every section. The system splash screen shows the app
 *  icon while it starts; the app then draws edge to edge, behind the status and navigation bars. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TattvamasiTheme {
                AppRoot()
            }
        }
    }
}
