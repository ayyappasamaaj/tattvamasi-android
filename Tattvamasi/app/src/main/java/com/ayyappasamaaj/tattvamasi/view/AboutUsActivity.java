package com.ayyappasamaaj.tattvamasi.view;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityAboutUsBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class AboutUsActivity extends AppCompatActivity {

    private static final String TAG = "AboutUsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_about_us);
        ActivityAboutUsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about_us);
        Header header = new Header();
        header.setTitle("About Us");
        binding.setHeader(header);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
