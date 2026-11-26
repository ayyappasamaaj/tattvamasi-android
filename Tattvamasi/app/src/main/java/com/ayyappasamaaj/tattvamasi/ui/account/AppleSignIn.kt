package com.ayyappasamaaj.tattvamasi.ui.account

import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Continue with Apple on Android (website: docs/app-sign-in.md, "Apple on Android"). Apple has no native sign-in on
 * Android, so the app runs the website's own Sign in with Apple in a Custom Tab:
 *
 * 1. A random PKCE verifier and state (kept in the app's private storage for 10 minutes, in case Android stops the
 *    app while the tab is open), and the tab opens the website's start page with the verifier's SHA-256 (the
 *    challenge) and the state.
 * 2. The website signs the person in with Apple and redirects to [REDIRECT] with a one-time code and the state;
 *    AppleRedirectActivity hands that to MainActivity.
 * 3. The app takes only a code with its own state, then swaps it, with the verifier, for the session token
 *    (AccountApi.signInWithAppleCode). The code is good once, for 2 minutes.
 */
object AppleSignIn {
    /** The address the website sends the code to (fixed on the website: core/app_web_signin.py APP_REDIRECT). */
    const val SCHEME = "com.ayyappasamaaj.tattvamasi"
    const val HOST = "signin"
    const val PATH = "/apple"
    private const val PREFS = "apple_sign_in"
    private const val LIFETIME_MS = 10 * 60 * 1000L

    data class Pending(val verifier: String, val state: String)

    sealed interface Redirect {
        data class Code(val code: String, val verifier: String) : Redirect
        /** Not ours (another state, too old, or no code): ignored */
        data object Ignored : Redirect
    }

    private val random = SecureRandom()

    fun randomString(bytes: Int = 32): String {
        val data = ByteArray(bytes).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }

    /** BASE64URL(SHA-256(verifier)), no padding (RFC 7636 S256), as the website checks it. */
    fun challenge(verifier: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    fun startUrl(site: String, pending: Pending): String =
        "$site/accounts/app-sign-in/apple/?code_challenge=${challenge(pending.verifier)}" +
            "&code_challenge_method=S256&state=${pending.state}"

    /** A new sign-in: the verifier and state, kept until the website comes back. */
    fun begin(context: Context): Pending {
        val pending = Pending(randomString(48), randomString(24))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("verifier", pending.verifier).putString("state", pending.state)
            .putLong("at", System.currentTimeMillis()).apply()
        return pending
    }

    private fun take(context: Context): Pending? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString("verifier", null)
        val state = prefs.getString("state", null)
        val fresh = System.currentTimeMillis() - prefs.getLong("at", 0) < LIFETIME_MS
        prefs.edit().clear().apply()  // one use
        return if (verifier != null && state != null && fresh) Pending(verifier, state) else null
    }

    fun isRedirect(uri: Uri?): Boolean = uri != null && uri.scheme == SCHEME && uri.host == HOST && uri.path == PATH

    /** The website's redirect ([code] and [state] from it) against the sign-in under way. */
    fun match(pending: Pending?, code: String?, state: String?): Redirect =
        if (pending != null && !code.isNullOrBlank() && state != null && state == pending.state) {
            Redirect.Code(code, pending.verifier)
        } else Redirect.Ignored

    fun redirect(context: Context, uri: Uri): Redirect {
        if (!isRedirect(uri)) return Redirect.Ignored
        return match(take(context), uri.getQueryParameter("code"), uri.getQueryParameter("state"))
    }
}
