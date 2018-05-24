package com.ayyappasamaaj.tattvamasi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayyappasamaaj.tattvamasi.databinding.ActivityDonateBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class DonateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_donate);
        ActivityDonateBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_donate);
        Header header = new Header();
        header.setTitle("Donate");
        binding.setHeader(header);
    }
}
