package com.ayyappasamaaj.tattvamasi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayyappasamaaj.tattvamasi.databinding.ActivityBhajansBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class BhajansActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_bhajans);
        ActivityBhajansBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_bhajans);
        Header header = new Header();
        header.setTitle("Bhajans");
        binding.setHeader(header);
    }
}
