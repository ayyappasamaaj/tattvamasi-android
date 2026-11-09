package com.ayyappasamaaj.tattvamasi.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/**
 * About us, Contact: the notification switches, one per kind (all on to start). When the system doesn't let the
 * app show notifications, a button asks (Android 13+, the first time) or opens the app's notification settings.
 * Tapping the heading seven times shows the test switch (the website's test sends).
 */
@Composable
fun NotificationsCard() {
    val context = LocalContext.current
    val settings by AppNotifications.settings.collectAsState()
    var allowed by remember { mutableStateOf(AppNotifications.allowed(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = AppNotifications.allowed(context)
        onPauseOrDispose { }
    }
    var taps by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        AppNotifications.asked(context)
        allowed = AppNotifications.allowed(context)
    }

    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Notifications",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.ink,
            modifier = Modifier.semantics { heading() }.clickable(onClickLabel = null, role = null) { taps += 1 },
        )
        if (!allowed) {
            Text("Notifications are off for this app on this phone.", style = MaterialTheme.typography.bodyMedium,
                color = AppColors.inkSoft)
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AppNotifications.shouldAsk()) {
                        ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        AppNotifications.asked(context)
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest),
            ) { Text("Allow notifications", fontWeight = FontWeight(700)) }
        }
        Topic.entries.forEachIndexed { index, topic ->
            if (index > 0) HorizontalDivider(color = AppColors.line)
            SwitchRow(topic.title, topic.description, topic.id in settings.on, enabled = allowed) {
                AppNotifications.setTopic(context, topic, it)
            }
        }
        if (taps >= 7 || settings.test) {
            HorizontalDivider(color = AppColors.line)
            SwitchRow("Test notifications", "The website's test sends, for checking announcements", settings.test,
                enabled = allowed) { AppNotifications.setTest(context, it) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, on: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = on, enabled = enabled, role = Role.Switch, onValueChange = change),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) AppColors.ink else AppColors.muted)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
        }
        Switch(
            checked = on,
            onCheckedChange = null,  // the whole row toggles
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.clay),
        )
    }
}
