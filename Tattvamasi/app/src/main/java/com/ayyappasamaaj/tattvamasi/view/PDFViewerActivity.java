package com.ayyappasamaaj.tattvamasi.view;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPdfBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class PDFViewerActivity extends AppCompatActivity {

    private static final String TAG = "PDFViewerActivity";

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

        WebView mWebView=findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("https://docs.google.com/gview?embedded=true&url="+linkTo);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
