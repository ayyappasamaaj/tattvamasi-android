package com.ayyappasamaaj.tattvamasi.view;

import android.databinding.DataBindingUtil;
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
}
