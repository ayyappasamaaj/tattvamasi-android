package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ActivityDonateBinding
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.util.AppLog

class DonateActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDonateBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_donate)
        binding.setHeader(Header("Donate"))
    }

    fun backClicked(view: View?) {
        AppLog.d(TAG, "Back clicked")
        finish()
    }

    fun onDonateClicked(view: View?) {
        AppLog.d(TAG, "Donate clicked")
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(BuildConfig.PAY_PAL_URL)
        )
        startActivity(browserIntent)
    }

    companion object {
        private const val TAG = "DonateActivity"
    }
}