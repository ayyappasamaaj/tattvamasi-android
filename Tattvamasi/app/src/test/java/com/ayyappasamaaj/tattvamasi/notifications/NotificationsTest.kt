package com.ayyappasamaaj.tattvamasi.notifications

import com.ayyappasamaaj.tattvamasi.ui.events.AppEvent
import com.ayyappasamaaj.tattvamasi.ui.guides.GuideTarget
import com.ayyappasamaaj.tattvamasi.ui.guides.parseGuides
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class NotificationsTest {
    private val guides = parseGuides(javaClass.classLoader!!.getResource("guides-sample.json")!!.readText())!!
    private val puja = AppEvent(
        id = "0", name = "Ayyappa Puja", summary = "", venue = "SVCC Temple", start = Instant.ofEpochSecond(1_795_000_000),
        end = Instant.ofEpochSecond(1_795_010_000), timeText = "7:00 PM - 9:00 PM",
        pageUrl = "https://ayyappasamaaj.org/events/ayyappa-puja/2026-11-21/", latitude = null, longitude = null,
    )

    @Test
    fun aTappedNotificationOpensWhatItsUrlNames() {
        val events = listOf(puja)
        assertEquals(NotificationTarget.Event(puja.key),
            notificationTarget("https://ayyappasamaaj.org/events/ayyappa-puja/2026-11-21/", events, guides))
        assertEquals(NotificationTarget.Event(puja.key),
            notificationTarget("https://www.ayyappasamaaj.org/events/ayyappa-puja/2026-11-21", events, guides))
        // An event that has passed (no longer in /events): the list
        assertEquals(NotificationTarget.Events,
            notificationTarget("https://ayyappasamaaj.org/events/old-puja/2025-11-21/", events, guides))
        assertEquals(NotificationTarget.Guide(GuideTarget("vratam", "food")),
            notificationTarget("https://ayyappasamaaj.org/vratam/#food", events, guides))
        assertEquals(NotificationTarget.Web("https://ayyappasamaaj.org/donate/"),
            notificationTarget("https://ayyappasamaaj.org/donate/", events, guides))
        assertEquals(NotificationTarget.Home, notificationTarget("", events, guides))
        assertEquals(NotificationTarget.Home, notificationTarget(null, events, guides))
        assertEquals(NotificationTarget.Home, notificationTarget("https://example.com/", events, guides))
    }

    @Test
    fun switchesStartOnButNothingIsSubscribedBeforeAsking() {
        val start = NotificationSettings()
        assertEquals(emptySet<String>(), start.subscribed())
        assertEquals(setOf("events", "registration", "reminders", "announcements"), start.copy(asked = true).subscribed())
        assertEquals(setOf("events", "reminders", "announcements", "test"),
            start.copy(asked = true, on = start.on - "registration", test = true).subscribed())
    }
}
