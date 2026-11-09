package com.ayyappasamaaj.tattvamasi.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideContent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.guides.guideTarget
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Push notifications from the website (website: notifications/, docs/app-notifications.md). The website sends to
 * Firebase Cloud Messaging topics; the app subscribes to the ones whose switch is on. Decided Nov 9, 2026 (the apps
 * doc, work item 4): the switches start on once notifications are allowed; the permission prompt shows the first
 * time someone opens an event; tapping a notification opens what its `url` names.
 */

/** The website's topics; each is a switch in About us, Contact, and its own channel in the system settings. */
enum class Topic(val id: String, val title: String, val description: String) {
    Events("events", "New events", "When the Samaaj adds an event"),
    Registration("registration", "Registration", "When registration opens, and the day before it closes"),
    Reminders("reminders", "Event reminders", "The evening before an event"),
    Announcements("announcements", "Announcements", "Messages from the Samaaj"),
}

/** Phones that get the website's test sends (a hidden switch: tap the Notifications heading seven times). */
const val TEST_TOPIC = "test"

/** The switches as saved on the phone. */
data class NotificationSettings(
    val asked: Boolean = false,
    val on: Set<String> = Topic.entries.map { it.id }.toSet(),
    val test: Boolean = false,
) {
    /** The topics to be subscribed to: none until the person has been asked. */
    fun subscribed(): Set<String> = if (!asked) emptySet() else on + (if (test) setOf(TEST_TOPIC) else emptySet())
}

/** Where tapping a notification goes. */
sealed interface NotificationTarget {
    data class Event(val key: String) : NotificationTarget
    data class Guide(val target: GuideTarget) : NotificationTarget
    data class Web(val url: String) : NotificationTarget
    data object Events : NotificationTarget
    data object Home : NotificationTarget
}

private fun samePage(a: String, b: String): Boolean {
    fun clean(url: String) = url.substringBefore('#').substringBefore('?').trimEnd('/')
        .replace("://www.", "://").replaceFirst("http://", "https://").lowercase()
    return clean(a) == clean(b)
}

/** An event's page opens the event (the events list if it has passed); a guide opens the guide; another page on
 *  the website opens in the browser tab; nothing, the app's home. */
fun notificationTarget(url: String?, events: List<AppEvent>, guides: List<GuideContent>): NotificationTarget {
    if (url.isNullOrBlank()) return NotificationTarget.Home
    events.firstOrNull { event -> event.pageUrl?.let { samePage(it, url) } == true }
        ?.let { return NotificationTarget.Event(it.key) }
    guideTarget(url, guides)?.let { return NotificationTarget.Guide(it) }
    if (Regex("""^https?://(www\.)?ayyappasamaaj\.org/events/""").containsMatchIn(url)) return NotificationTarget.Events
    if (Regex("""^https://(www\.)?ayyappasamaaj\.org(/|$)""").containsMatchIn(url)) return NotificationTarget.Web(url)
    return NotificationTarget.Home
}

object AppNotifications {
    private const val TAG = "AppNotifications"
    private const val PREFS = "notifications"

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    /** The url of a tapped notification, until the app has opened it. */
    private val _tapped = MutableStateFlow<String?>(null)
    val tapped: StateFlow<String?> = _tapped.asStateFlow()

    private var started = false

    /** At startup: the channels, the saved switches, and the subscriptions brought in line with them. */
    fun start(context: Context) {
        if (started) return
        started = true
        createChannels(context)
        val prefs = prefs(context)
        _settings.value = NotificationSettings(
            asked = prefs.getBoolean("asked", false),
            on = prefs.getStringSet("on", null)?.toSet() ?: Topic.entries.map { it.id }.toSet(),
            test = prefs.getBoolean("test", false),
        )
        sync(context)
    }

    fun tapped(url: String?) {
        if (!url.isNullOrBlank()) _tapped.value = url
    }

    fun opened() {
        _tapped.value = null
    }

    /** Whether the system lets the app show notifications (the permission on Android 13+, and not turned off). */
    fun allowed(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** True the first time: the caller then shows the system prompt (Android 13+). */
    fun shouldAsk(): Boolean = !_settings.value.asked

    /** After the first prompt (whatever the answer): the switches take effect. */
    fun asked(context: Context) = update(context) { it.copy(asked = true) }

    fun setTopic(context: Context, topic: Topic, on: Boolean) =
        update(context) { it.copy(asked = true, on = if (on) it.on + topic.id else it.on - topic.id) }

    fun setTest(context: Context, on: Boolean) = update(context) { it.copy(asked = true, test = on) }

    private fun update(context: Context, change: (NotificationSettings) -> NotificationSettings) {
        val next = change(_settings.value)
        _settings.value = next
        prefs(context).edit()
            .putBoolean("asked", next.asked).putStringSet("on", next.on).putBoolean("test", next.test).apply()
        sync(context)
    }

    /** Subscribes to the topics that are on and unsubscribes from the others. FCM keeps subscriptions itself and
     *  retries while offline, so this only needs to run when something changes (and once at startup). */
    private fun sync(context: Context) {
        val wanted = _settings.value.subscribed()
        val messaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: IllegalStateException) {
            AppLog.w(TAG, "Firebase isn't set up: " + e.message)
            return
        }
        (Topic.entries.map { it.id } + TEST_TOPIC).forEach { topic ->
            val task = if (topic in wanted) messaging.subscribeToTopic(topic) else messaging.unsubscribeFromTopic(topic)
            task.addOnFailureListener { AppLog.w(TAG, "topic $topic: " + it.message) }
        }
    }

    private fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = Topic.entries.map {
            NotificationChannel(it.id, it.title, NotificationManager.IMPORTANCE_DEFAULT).apply { description = it.description }
        } + NotificationChannel(TEST_TOPIC, "Test notifications", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannels(channels)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
