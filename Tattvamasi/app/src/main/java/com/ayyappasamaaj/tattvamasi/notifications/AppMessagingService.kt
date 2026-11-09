package com.ayyappasamaaj.tattvamasi.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ayyappasamaaj.tattvamasi.MainActivity
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Notifications that arrive while the app is open (in the background the system shows them itself, and a tap
 * starts MainActivity with the message's data as extras: "url"). Subscriptions are by topic, so the device
 * token isn't needed.
 */
class AppMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        if (!AppNotifications.allowed(this)) return
        val url = message.data["url"].orEmpty()
        val channel = notification.channelId ?: message.from?.removePrefix("/topics/") ?: Topic.Announcements.id
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_URL, url)
        val tap = PendingIntent.getActivity(
            this, url.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val shown = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(AppColors.clay.toArgb())
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((message.messageId ?: url).hashCode(), shown)
        } catch (_: SecurityException) {
            // The permission was taken back meanwhile
        }
    }

    override fun onNewToken(token: String) {
        // Topics follow the device to its new token; nothing to do
    }
}
