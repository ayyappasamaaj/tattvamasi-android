package com.ayyappasamaaj.tattvamasi.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.theme.Appearance
import java.net.URLEncoder

/** The website's pages the app opens. */
object WebPages {
    const val DONATE = "https://ayyappasamaaj.org/donate/"
    const val PRIVACY = "https://ayyappasamaaj.org/privacy-policy/"
}

/**
 * Opens a page in the phone's browser app, outside the app. Only for what the browser has to hand on to another app
 * (Google Calendar's subscribe link); every website page opens in the app's own browser (openWebPage), the
 * maintainer's call, Nov 25.
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
        .setToolbarColor(0xFFF6F2EA.toInt())
        .setNavigationBarColor(0xFFF6F2EA.toInt())
        .build()
    // The website's dark page in dark mode (the tab follows the app's Appearance)
    val darkColors = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(0xFF17140F.toInt())
        .setNavigationBarColor(0xFF17140F.toInt())
        .build()
    val scheme = when (Appearance.choice) {
        Appearance.Choice.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        Appearance.Choice.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        Appearance.Choice.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
    }
    try {
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colors)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColors)
            .setColorScheme(scheme)
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser to open the page")
    }
}

/**
 * The Samaaj's Facebook, YouTube, Instagram or X page: in that app when it's installed, else in the app's own browser
 * (openWebPage). Android 11 and later say themselves whether an app other than a browser takes the link
 * (FLAG_ACTIVITY_REQUIRE_NON_BROWSER); before that, the app checks which apps take it ([socialApp]). As the iOS app's
 * About > Contact (AboutView.swift), which opens universal links only.
 */
fun openSocialLink(context: Context, url: String) {
    val uri = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER))
        } catch (_: ActivityNotFoundException) {
            openWebPage(context, url)
        }
        return
    }
    val packages = context.packageManager
    val handlers = packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo.packageName }
    // Any web address's apps: the browsers (a page with no app of its own goes to these)
    val browsers = packages.queryIntentActivities(
        Intent(Intent.ACTION_VIEW, "https://example.com/".toUri()).addCategory(Intent.CATEGORY_BROWSABLE),
        PackageManager.MATCH_ALL,
    ).map { it.activityInfo.packageName }
    val app = socialApp(handlers, browsers)
    if (app == null) {
        openWebPage(context, url)
        return
    }
    try {
        context.startActivity(intent.setPackage(app))
    } catch (_: ActivityNotFoundException) {
        openWebPage(context, url)
    }
}

/** The app (its package) that takes a social link before Android 11: the first of the link's [handlers] that isn't
 *  one of the [browsers], or null when only browsers take it (the app isn't installed). */
fun socialApp(handlers: List<String>, browsers: Collection<String>): String? =
    handlers.firstOrNull { it !in browsers }

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
