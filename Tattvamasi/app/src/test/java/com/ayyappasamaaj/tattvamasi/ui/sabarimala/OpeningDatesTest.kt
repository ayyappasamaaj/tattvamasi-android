package com.ayyappasamaaj.tattvamasi.ui.sabarimala

import com.ayyappasamaaj.tattvamasi.ui.guides.parseGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The opening dates beside the guides in /guides/app.json (website: pages/app_guides.py), and the guides' names. */
class OpeningDatesTest {
    private val feed = """
        {"version": 1,
         "guides": [{"slug": "vratam", "name": "Observing the Vratam", "title": "Observing the Vratam: a guide",
                     "url": "https://ayyappasamaaj.org/vratam/", "intro": [],
                     "sections": [{"id": "food", "title": "Food", "blocks": [{"html": "<p>Simple food.</p>"}], "sections": []}]},
                    {"slug": "virtual-q", "title": "Sabarimala Virtual Q booking guide",
                     "url": "https://ayyappasamaaj.org/sabarimala-virtual-q/", "intro": [],
                     "sections": [{"id": "book", "title": "Book", "blocks": [{"html": "<p>Book.</p>"}], "sections": []}]}],
         "opening_dates": {
           "name": "Sabarimala Opening Dates", "season": "2026–27",
           "url": "https://ayyappasamaaj.org/sabarimala-opening-dates/",
           "calendar": "https://ayyappasamaaj.org/sabarimala-opening-dates/calendar.ics",
           "source": {"name": "Travancore Devaswom Board", "url": "https://sabarimalaonline.org/#/devaswom-calender"},
           "time_zone": "Asia/Kolkata", "crowds": ["Quiet", "Moderate", "High", "Extreme"],
           "periods": [
             {"id": "makaravilakku-2026-12-30", "kind": "makaravilakku", "name": "Makaravilakku",
              "title": "Makaravilakku festival", "opens": "2026-12-30", "opens_time": "5:00 PM",
              "closes": null, "closes_time": null, "crowd": "Extreme", "about": "The temple reopens.",
              "key_dates": [{"date": "2027-01-14", "time": "5:00 PM", "what": "Makaravilakku Day", "role": null}],
              "url": "https://ayyappasamaaj.org/sabarimala-opening-dates/makaravilakku-2026-12-30/",
              "calendar": "https://ayyappasamaaj.org/sabarimala-opening-dates/makaravilakku-2026-12-30.ics"},
             {"id": "mandala-2026-11-16", "kind": "mandala", "name": "Mandala season", "title": "Mandala Puja Mahotsavam",
              "opens": "2026-11-16", "opens_time": "5:00 PM", "closes": "2026-12-27", "closes_time": "10:00 PM",
              "crowd": "Extreme", "about": "The Mandala season.",
              "key_dates": [{"date": "2026-11-16", "time": "5:00 PM", "what": "Mandala Puja Mahotsavam: temple opens", "role": "opens"},
                            {"date": "2026-12-27", "time": "5:00 PM", "what": "Mandala Puja", "role": null}],
              "url": "https://ayyappasamaaj.org/sabarimala-opening-dates/mandala-2026-11-16/",
              "calendar": "https://ayyappasamaaj.org/sabarimala-opening-dates/mandala-2026-11-16.ics"},
             {"id": "utsavam-2026-03-22", "kind": "utsavam", "name": "Utsavam", "title": "Utsavam",
              "opens": "2026-03-22", "opens_time": "5:00 PM", "closes": "2026-04-01", "closes_time": "10:00 PM",
              "crowd": null, "about": "The festival.", "key_dates": [],
              "url": "https://ayyappasamaaj.org/sabarimala-opening-dates/utsavam-2026-03-22/",
              "calendar": "https://ayyappasamaaj.org/sabarimala-opening-dates/utsavam-2026-03-22.ics"}]}}
    """.trimIndent()

    @Test
    fun guidesUseTheirShortNameAndFallBackToTheTitle() {
        val guides = parseGuides(feed)!!
        assertEquals("Observing the Vratam", guides[0].name)
        assertEquals("Observing the Vratam: a guide", guides[0].title)
        // An older copy without "name" keeps working with its title
        assertEquals("Sabarimala Virtual Q booking guide", guides[1].name)
    }

    @Test
    fun theOpeningDatesAreReadInDateOrder() {
        val dates = parseOpeningDates(feed)!!
        assertEquals("Sabarimala Opening Dates", dates.name)
        assertEquals("2026–27", dates.season)
        assertEquals("Travancore Devaswom Board", dates.sourceName)
        assertEquals(listOf("utsavam-2026-03-22", "mandala-2026-11-16", "makaravilakku-2026-12-30"), dates.periods.map { it.id })
        val mandala = dates.periods[1]
        assertEquals(LocalDate.of(2026, 11, 16), mandala.opens)
        assertEquals(LocalDate.of(2026, 12, 27), mandala.closes)
        assertEquals("Extreme", mandala.crowd)
        assertEquals(listOf("opens", null), mandala.keyDates.map { it.role })
        // Not yet published, and not rated
        assertNull(dates.periods[2].closes)
        assertNull(dates.periods[2].closesTime)
        assertNull(dates.periods[0].crowd)
    }

    @Test
    fun theCopyShippedWithTheAppHasTheOpeningDates() {
        val bundled = java.io.File("src/main/assets/guides.json").readText()
        val dates = parseOpeningDates(bundled)
        assertTrue("the bundled guides.json has no usable opening dates", dates != null && dates.periods.size >= 10)
        assertEquals(5, parseGuides(bundled)!!.size)
    }

    @Test
    fun anOlderCopyWithoutOpeningDatesHasNone() {
        assertNull(parseOpeningDates("""{"version": 1, "guides": []}"""))
        assertNull(parseOpeningDates("not json"))
    }

    @Test
    fun whatIsOpenNowOrNext() {
        val dates = parseOpeningDates(feed)!!
        val (now, isNow) = dates.nowOrNext(LocalDate.of(2026, 11, 19))!!
        assertEquals("mandala-2026-11-16", now.id)
        assertTrue(isNow)
        val (next, isNext) = dates.nowOrNext(LocalDate.of(2026, 12, 28))!!
        assertEquals("makaravilakku-2026-12-30", next.id)
        assertFalse(isNext)
        // With no closing date yet, Makaravilakku counts as open once it opens
        assertTrue(dates.periods[2].isOpenOn(LocalDate.of(2027, 1, 5)))
        assertTrue(dates.periods[0].isPast(LocalDate.of(2026, 11, 19)))
        assertFalse(dates.periods[2].isPast(LocalDate.of(2027, 6, 1)))
    }

    @Test
    fun theOpeningLineSaysWhenItOpensAndCloses() {
        val dates = parseOpeningDates(feed)!!
        assertEquals("Opens Mon, Nov 16, 5:00 PM · Closes Sun, Dec 27, 10:00 PM", openingLine(dates.periods[1]))
        assertEquals("Opens Wed, Dec 30, 5:00 PM · Closing date not yet announced", openingLine(dates.periods[2]))
    }

    @Test
    fun theOpeningDatesCardIsBesideTheGuidesWithTheWebsitesNames() {
        assertEquals(
            listOf("Traditional Path", "Sabarimala Opening Dates", "Observing the Vratam", "Sabarimala Virtual Q", "House Puja and Bhajan"),
            guides.map { it.title },
        )
        assertEquals(OPENING_DATES, guides[1].slug)
        assertEquals("https://ayyappasamaaj.org/sabarimala-opening-dates/", guides[1].url)
    }
}
