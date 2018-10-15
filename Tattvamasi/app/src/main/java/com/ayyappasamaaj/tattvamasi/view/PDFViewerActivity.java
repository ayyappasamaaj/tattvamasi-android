package com.ayyappasamaaj.tattvamasi.view;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPdfBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class PDFViewerActivity extends AppCompatActivity {

    WebView mWebView;
    private static final String TAG = "PDFViewerActivity";
    private static final String REMOVE_TOOL_BAR = "javascript:(function() { " +
                                                            "document.querySelector('[role=\"toolbar\"]').remove();" +
                                                        "})()";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get the URL from intent
        String linkTo = this.getIntent().getStringExtra("URL");
        String title = this.getIntent().getStringExtra("TITLE");

        // set the header
        ActivityPdfBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_pdf);
        Header header = new Header();
        header.setTitle(title);
        binding.setHeader(header);

        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebView.loadUrl(REMOVE_TOOL_BAR);
            }
        });
        mWebView.loadUrl("https://docs.google.com/gview?embedded=true&url="+linkTo);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
