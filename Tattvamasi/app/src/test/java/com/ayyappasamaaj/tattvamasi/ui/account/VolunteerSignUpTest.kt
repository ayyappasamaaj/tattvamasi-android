package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sign up as a volunteer as the website answers it (volunteers/api.py SignUpAPI). */
class VolunteerSignUpTest {
    @Test
    fun theState() {
        val s = parseVolunteerSignUp("""{"is_volunteer": false, "pending": false, "email": "a@example.com", "email_verified": false}""")!!
        assertEquals(VolunteerSignUp(false, false, "a@example.com", false), s)
        assertTrue(volunteerSignUpText(s).endsWith("We'll first send you an email to confirm your address."))
        assertTrue(volunteerSignUpText(s.copy(pending = true)).startsWith("Almost there: we sent an email to a@example.com."))
        assertNull(parseVolunteerSignUp("{}"))
        assertNull(parseVolunteerSignUp("not json"))
    }

    @Test
    fun theAnswers() {
        val done = parseVolunteerSignUpAnswer(200, """{"outcome": "sent", "message": "Almost there", "is_volunteer": false,
            "pending": true, "email": "a@example.com", "email_verified": false}""") as VolunteerSignUpResult.Done
        assertEquals("sent", done.outcome)
        assertTrue(done.state.pending)
        assertEquals(VolunteerSignUpResult.SignedOut, parseVolunteerSignUpAnswer(401, ""))
        assertTrue(parseVolunteerSignUpAnswer(500, "") is VolunteerSignUpResult.Failed)
    }
}
