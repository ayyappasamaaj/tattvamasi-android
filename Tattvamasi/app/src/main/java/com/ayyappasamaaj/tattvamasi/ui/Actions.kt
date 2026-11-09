package com.ayyappasamaaj.tattvamasi.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.net.URLEncoder

/** The website's pages the app opens. */
object WebPages {
    const val DONATE = "https://ayyappasamaaj.org/donate/"
    const val PRIVACY = "https://ayyappasamaaj.org/privacy-policy/"
}

/**
 * Opens a website page in the phone's browser app, outside the app. Used for Donate: giving happens on the
 * website, and the app only links out to it (as the iOS app does in Safari, which the App Store requires of
 * nonprofits it hasn't approved for in-app giving).
 */
fun openInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    start(context, intent, "No browser to open the page")
}

/**
 * Opens a website page in a Chrome Custom Tab: the browser inside the app, where the person stays signed in to
 * the website (Android's equivalent of the iOS app's Safari sheet). Falls back to any browser.
 */
fun openWebPage(context: Context, url: String) {
    val colors = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(AppColors.cream.toArgb())
        .setNavigationBarColor(AppColors.cream.toArgb())
        .build()
    try {
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colors)
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser to open the page")
    }
}

/**
 * Add to calendar: the phone's calendar app opens with the event filled in, and the person saves it there. Needs
 * no calendar permission. Without a calendar app (a Chromebook, some phones), Google Calendar's add-event page
 * opens instead.
 */
fun addToCalendar(context: Context, event: AppEvent) {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.toEpochMilli())
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli())
        .putExtra(CalendarContract.Events.TITLE, event.name)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.venue)
        .putExtra(CalendarContract.Events.DESCRIPTION, calendarNotes(event))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        openWebPage(context, calendarWebUrl(event))
    }
}

/** Google Calendar's add-event page with the event filled in (times in UTC). */
fun calendarWebUrl(event: AppEvent): String {
    val stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC)
    fun enc(text: String) = URLEncoder.encode(text, "UTF-8")
    return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
        "&text=" + enc(event.name) +
        "&dates=" + stamp.format(event.start) + "/" + stamp.format(event.end) +
        "&location=" + enc(event.venue) +
        "&details=" + enc(calendarNotes(event))
}

/** The calendar entry's notes: the description, then the event's page for registering. */
fun calendarNotes(event: AppEvent): String =
    listOfNotNull(event.extraSummary, event.pageUrl).joinToString("\n\n")

/** Directions: Google Maps (the app if installed, else the website) with driving directions to the venue. */
fun openDirections(context: Context, event: AppEvent) {
    start(context, Intent(Intent.ACTION_VIEW, Uri.parse(directionsUrl(event))), "No maps app found")
}

/** Google Maps' directions link: to the event's map point, or by its address. */
fun directionsUrl(event: AppEvent): String {
    val destination = if (event.latitude != null && event.longitude != null) {
        "${event.latitude},${event.longitude}"
    } else {
        URLEncoder.encode(event.venue, "UTF-8")
    }
    return "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=driving"
}

private fun start(context: Context, intent: Intent, failure: String) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
    }
}
