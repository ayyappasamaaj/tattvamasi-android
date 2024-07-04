package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.ayyappasamaaj.tattvamasi.R

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}