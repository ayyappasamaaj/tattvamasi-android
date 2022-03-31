package com.ayyappasamaaj.tattvamasi.view

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPdfBinding
import com.ayyappasamaaj.tattvamasi.model.Header

class PDFViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get the URL from intent
        val linkTo = this.intent.getStringExtra("URL")
        val title = this.intent.getStringExtra("TITLE")
        // set the header
        val binding: ActivityPdfBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_pdf)
        binding.setHeader(Header(title))

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
            loadUrl("https://docs.google.com/gview?embedded=true&url=$linkTo")
        }
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