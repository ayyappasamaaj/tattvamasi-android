package com.ayyappasamaaj.tattvamasi.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Continue with Apple's PKCE pieces, as the website checks them (core/app_web_signin.py, docs/app-sign-in.md). */
class AppleSignInTest {
    @Test
    fun theChallengeIsTheWebsitesS256() {
        // The same value the website's challenge_for gives (Python: BASE64URL(SHA-256), no padding)
        assertEquals("mUyOVQN4Q5S0RMY_ZBkzXna3GBEA1mU4PHMH7BBvCh0",
            AppleSignIn.challenge("dBjftJeZ4CVP-mJ92K9qO7Nsfz1G3rWb4tTWRxx8Ik8"))
    }

    @Test
    fun verifiersAndStatesFitTheWebsitesRules() {
        val verifier = AppleSignIn.randomString(48)
        val state = AppleSignIn.randomString(24)
        assertTrue(Regex("^[A-Za-z0-9._~-]{43,128}$").matches(verifier))
        assertTrue(Regex("^[A-Za-z0-9_-]{16,128}$").matches(state))
        assertEquals(43, AppleSignIn.challenge(verifier).length)
        assertNotEquals(verifier, AppleSignIn.randomString(48))
    }

    @Test
    fun theStartAddress() {
        val pending = AppleSignIn.Pending("dBjftJeZ4CVP-mJ92K9qO7Nsfz1G3rWb4tTWRxx8Ik8", "state-1234567890abcdef")
        assertEquals("https://staging.ayyappasamaaj.org/accounts/app-sign-in/apple/?code_challenge=" +
            "mUyOVQN4Q5S0RMY_ZBkzXna3GBEA1mU4PHMH7BBvCh0&code_challenge_method=S256&state=state-1234567890abcdef",
            AppleSignIn.startUrl("https://staging.ayyappasamaaj.org", pending))
    }

    @Test
    fun onlyACodeWithOurStateCounts() {
        val pending = AppleSignIn.Pending("v".repeat(43), "our-state-123456789")
        assertEquals(AppleSignIn.Redirect.Code("abc", "v".repeat(43)), AppleSignIn.match(pending, "abc", "our-state-123456789"))
        assertEquals(AppleSignIn.Redirect.Ignored, AppleSignIn.match(pending, "abc", "someone-elses-state"))
        assertEquals(AppleSignIn.Redirect.Ignored, AppleSignIn.match(null, "abc", "our-state-123456789"))
        assertEquals(AppleSignIn.Redirect.Ignored, AppleSignIn.match(pending, "", "our-state-123456789"))
    }

    @Test
    fun appleIsOfferedWhenTheWebsiteListsIt() {
        assertTrue(parseAppleOffered("""{"status": 200, "data": {"socialaccount": {"providers": [
            {"id": "google", "name": "Google"}, {"id": "apple", "name": "Apple"}]}}}"""))
        assertFalse(parseAppleOffered("""{"status": 200, "data": {"socialaccount": {"providers": [{"id": "google"}]}}}"""))
        assertFalse(parseAppleOffered("not json"))
    }

    @Test
    fun theTokenAnswerSignsIn() {
        val reply = AccountApi.Reply(200, """{"status": 200, "data": {"user": {"id": 7, "display": "Devi R",
            "email": "devotee@icloud.com"}}, "meta": {"is_authenticated": true, "session_token": "tok"}}""")
        assertEquals(SignIn.Done("tok", AccountUser("Devi R", "devotee@icloud.com")), parseSignIn(reply))
        val refused = AccountApi.Reply(400, """{"status": 400, "errors": [{"message": "This sign-in has expired. Please go back to the app and try again."}]}""")
        assertEquals(SignIn.Refused("This sign-in has expired. Please go back to the app and try again."), parseSignIn(refused))
    }
}
