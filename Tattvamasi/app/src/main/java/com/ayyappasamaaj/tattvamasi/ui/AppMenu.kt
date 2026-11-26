package com.ayyappasamaaj.tattvamasi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayyappasamaaj.tattvamasi.notifications.NotificationsCard
import com.ayyappasamaaj.tattvamasi.ui.about.AboutScreen
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.ui.theme.AppearanceCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * The top-right menu (the maintainer's call, Nov 25, 2026: About leaves the tab bar for the Festivals tab): My Account
 * (or Sign In), About Ayyappa Samaaj (About Us, Our Board, Contact, which was the About us tab), and Settings
 * (Notifications and Appearance, which were at the foot of About's Contact). Each opens as a page over any tab, as
 * My Account does. Home's About us tile opens About too. As the iOS app's AppMenu.swift.
 */
object AppMenu {
    enum class Page(val title: String) { About("About Ayyappa Samaaj"), Settings("Settings") }

    private val _page = MutableStateFlow<Page?>(null)
    val page: StateFlow<Page?> = _page

    fun open(page: Page) = _page.update { page }
    fun close() = _page.update { null }
}

/** The person button at the banner's corner, and its menu. */
@Composable
fun MenuButton(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val account by AccountStore.state.collectAsState()
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.AccountCircle,
                contentDescription = "Menu: My account, About, Settings",
                tint = Color(0xFF2A2620),  // dark in both modes: the backing stays white
                modifier = Modifier.background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(50)).padding(4.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (account.user != null) "My Account" else "Sign In") },
                leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                onClick = { expanded = false; AccountStore.open() },
            )
            DropdownMenuItem(
                text = { Text("About Ayyappa Samaaj") },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = { expanded = false; AppMenu.open(AppMenu.Page.About) },
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = { expanded = false; AppMenu.open(AppMenu.Page.Settings) },
            )
        }
    }
}

/** About or Settings over any tab, with Back to where the person was. */
@Composable
fun AppMenuSheet() {
    val page by AppMenu.page.collectAsState()
    val shown = page ?: return
    BackHandler { AppMenu.close() }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            // Takes every touch, so none reaches the tab underneath
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = { AppMenu.close() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.ink)
            }
            Text(shown.title, style = MaterialTheme.typography.headlineSmall, color = AppColors.ink,
                modifier = Modifier.semantics { heading() })
        }
        when (shown) {
            AppMenu.Page.About -> AboutScreen(showTitle = false)
            AppMenu.Page.Settings -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ReadableWidth(maxWidth = 640.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NotificationsCard()
                        AppearanceCard()
                        AddressFooter()
                    }
                }
            }
        }
    }
}
