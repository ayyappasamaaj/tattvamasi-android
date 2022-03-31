package com.ayyappasamaaj.tattvamasi.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ActivityAboutUsBinding
import com.ayyappasamaaj.tattvamasi.model.Header

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAboutUsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_about_us)
        binding.setHeader(Header("About Us"))
    }

    fun backClicked(view: View?) {
        finish()
    }

    companion object {
        private const val TAG = "AboutUsActivity"
    }
}