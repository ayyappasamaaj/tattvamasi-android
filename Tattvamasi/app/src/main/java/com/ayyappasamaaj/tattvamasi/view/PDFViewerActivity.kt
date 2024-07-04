package com.ayyappasamaaj.tattvamasi.view

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPdfBinding
import com.ayyappasamaaj.tattvamasi.model.Header

class PDFViewerActivity : BaseActivity() {
    lateinit var binding: ActivityPdfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        // get the URL from intent
        val linkTo = this.intent.getStringExtra("URL")
        val title = this.intent.getStringExtra("TITLE")
        // set the header
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pdf)
        binding.setHeader(Header(title))

        if (savedInstanceState == null) {
            setupWebView(binding)
            binding.webview.loadUrl("https://docs.google.com/gview?embedded=true&url=$linkTo")
        }
    }

    private fun setupWebView(binding: ActivityPdfBinding) {
        with(binding.webview) {
            settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    loadUrl(REMOVE_TOOL_BAR)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isScreenRotated", true)
        binding.webview.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webview.restoreState(savedInstanceState)
    }

    fun backClicked(view: View?) {
        finish()
    }

    companion object {
        private const val TAG = "PDFViewerActivity"
        private const val REMOVE_TOOL_BAR = "javascript:(function() { " +
                "document.querySelector('[role=\"toolbar\"]').remove();" +
                "})()"
    }
}