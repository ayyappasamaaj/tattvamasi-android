package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityDonateBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class DonateActivity extends AppCompatActivity {

    private static final String TAG = "DonateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_donate);
        ActivityDonateBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_donate);
        Header header = new Header();
        header.setTitle("Donate");
        binding.setHeader(header);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }

    public void onDonateClicked(View view) {
        Log.d(TAG, "Donate clicked");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=4M73DVTGB3BNY"));
        startActivity(browserIntent);
    }
}
