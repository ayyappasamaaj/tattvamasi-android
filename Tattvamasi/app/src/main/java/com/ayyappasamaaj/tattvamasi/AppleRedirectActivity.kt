package com.ayyappasamaaj.tattvamasi

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Receives the website's redirect after Continue with Apple (ui/account/AppleSignIn.kt) and hands it to MainActivity,
 * closing the Custom Tab above it (MainActivity is singleTop, so it gets the redirect in onNewIntent). Shows nothing.
 */
class AppleRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).setData(intent?.data)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
