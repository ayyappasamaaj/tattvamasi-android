package com.ayyappasamaaj.tattvamasi.ui.donate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The website's answers for giving in the app (website: docs/app-donations.md), as staging gives them. */
class DonateStoreTest {
    @Test
    fun theOptions() {
        val body = """{"enabled": true, "publishable_key": "pk_test_x", "min": "1", "max": "10000",
            "one_time": [{"amount": "54.00", "category": "General donation"}, {"amount": "108.00", "category": "General donation"}],
            "monthly": [{"id": 7, "amount": "116.00", "category": "Annadana"}], "website": "/donate/"}"""
        val options = parseOptions(body)!!
        assertTrue(options.enabled)
        assertEquals("pk_test_x", options.publishableKey)
        assertEquals(listOf("54.00", "108.00"), options.oneTime)
        assertEquals(listOf(MonthlyAmount(7, "116.00", "Annadana")), options.monthly)
    }

    @Test
    fun switchedOffMeansTheWebsite() {
        assertFalse(parseOptions("""{"enabled": false, "website": "/donate/"}""")!!.enabled)
        assertNull(parseOptions("not json"))
    }

    @Test
    fun aPaymentToConfirm() {
        assertEquals(PayStart.Ready("pi_1_secret_x", "54.00"),
            parsePayStart(200, """{"client_secret": "pi_1_secret_x", "amount": "54.00"}"""))
    }

    @Test
    fun theWebsitesReasonsAreShownAsTheyAre() {
        assertEquals(PayStart.Problem("The smallest gift by card is $1."),
            parsePayStart(400, """{"detail": "The smallest gift by card is $1."}"""))
        assertEquals(PayStart.Problem("Please sign in for a monthly gift.", signIn = true),
            parsePayStart(401, """{"detail": "Please sign in for a monthly gift."}"""))
        assertTrue((parsePayStart(0, "") as PayStart.Problem).message.startsWith("Couldn't reach"))
        assertTrue((parsePayStart(502, "oops") as PayStart.Problem).message.startsWith("Something went wrong"))
    }
}
