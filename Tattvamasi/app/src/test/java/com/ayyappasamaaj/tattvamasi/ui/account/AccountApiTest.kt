package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The website's answers (django-allauth 0.63.3's app sign-in and the /api/... views), as they come. */
class AccountApiTest {
    @Test
    fun signedInGivesTheTokenAndTheUser() {
        val body = """{"status": 200, "data": {"user": {"id": 6, "display": "Devotee Kumar",
            "has_usable_password": true, "email": "devotee@example.com", "username": "devotee"},
            "methods": [{"method": "password", "at": 1.0, "email": "devotee@example.com"}]},
            "meta": {"is_authenticated": true, "session_token": "abc123"}}"""
        val result = parseSignIn(AccountApi.Reply(200, body))
        assertEquals(SignIn.Done("abc123", AccountUser("Devotee Kumar", "devotee@example.com")), result)
    }

    @Test
    fun anUnconfirmedAddressAsksToConfirmIt() {
        val body = """{"status": 401, "data": {"flows": [{"id": "login"}, {"id": "signup"},
            {"id": "verify_email", "is_pending": true}]}, "meta": {"is_authenticated": false, "session_token": "x"}}"""
        assertEquals(SignIn.ConfirmEmail, parseSignIn(AccountApi.Reply(401, body)))
    }

    @Test
    fun aWrongPasswordShowsTheWebsitesMessage() {
        // Staging's answer, Nov 24
        val body = """{"status": 400, "errors": [{"message": "The email address and/or password you specified """ +
            """are not correct.", "code": "email_password_mismatch", "param": "password"}]}"""
        val result = parseSignIn(AccountApi.Reply(400, body)) as SignIn.Refused
        assertTrue(result.message.startsWith("The email address and/or password"))
    }

    @Test
    fun noNetworkAndTooManyTries() {
        assertEquals(SignIn.Offline, parseSignIn(AccountApi.Reply(0, "")))
        val tooMany = parseSignIn(AccountApi.Reply(429, """{"status": 429}""")) as SignIn.Refused
        assertTrue(tooMany.message.startsWith("Too many tries"))
    }

    @Test
    fun registrationsWithTheirEventAndWhoRegistered() {
        val body = """{"registrations": [{"code": "R1", "title": "Mandala Mahotsavam 2026", "status": "paid",
            "status_label": "Paid", "total": "25.00", "registered_at": "2026-11-20T10:00:00-08:00", "items": [],
            "url": "/forms/m/thanks/t/", "form_active": true, "year": 2026, "registered_by": "Priya Kumar",
            "event": {"name": "Mandala Mahotsavam", "url": "/events/m/2026-12-12/", "calendar_url": "/c",
            "start_date": "2026-12-12", "end_date": "2026-12-12", "start_time": "09:00", "end_time": "13:00",
            "venue": "SVCC Temple", "is_upcoming": true}},
            {"code": "R2", "title": "Kids Mala 2025", "status": "free", "status_label": "Registered", "total": "0.00",
            "registered_at": "2025-01-01T10:00:00-08:00", "items": [], "url": "/x", "form_active": false,
            "year": 2025, "registered_by": null, "event": null}]}"""
        val list = parseRegistrations(body)
        assertEquals(2, list.size)
        assertEquals(Registration("Mandala Mahotsavam 2026", "Paid", "25.00", "Mandala Mahotsavam", "2026-12-12",
            true, "Priya Kumar"), list[0])
        assertNull(list[1].eventName)
        assertNull(list[1].registeredBy)
    }

    @Test
    fun donationsFromThePagedList() {
        val body = """{"count": 2, "next": null, "previous": null, "results": [
            {"id": 9, "title": "General Donation", "date": "2026-11-02T18:04:11.123456-08:00", "amount": "101.00",
             "donated_by": null, "method": "card", "memo_code": ""},
            {"id": 8, "title": "Annadanam", "date": "2026-10-30", "amount": "51.00", "donated_by": "Priya Kumar",
             "method": "zelle", "memo_code": "AS-1"}]}"""
        val list = parseDonations(body)
        assertEquals(Donation("General Donation", "2026-11-02", "101.00", null), list[0])
        assertEquals("Priya Kumar", list[1].donatedBy)
    }

    @Test
    fun theYearsTotals() {
        val body = """{"year": 2026, "years": [2026], "scope": "me", "household": null,
            "donations": [{"date": "2026-11-02", "amount": "101.00"}], "registrations": [],
            "totals": {"donations": "101.00", "registrations": "0.00", "goods_value": "0.00", "all": "101.00"}}"""
        assertEquals(YearTotals(2026, "101.00", "0.00", "101.00", 1, 0), parseYear(body))
        assertNull(parseYear("""{"oops": true}"""))
    }

    @Test
    fun theNameTheyGave() {
        assertEquals("App Tester", parseName("""{"id": 90, "username": "apptest", "first_name": "App",
            "last_name": "Tester", "is_active": true, "groups": []}"""))
        assertNull(parseName("""{"id": 90, "username": "apptest", "first_name": "", "last_name": ""}"""))
    }
}
