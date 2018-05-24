package com.ayyappasamaaj.tattvamasi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayyappasamaaj.tattvamasi.databinding.ActivityPoojaBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class PoojaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_pooja);
        ActivityPoojaBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_pooja);
        Header header = new Header();
        header.setTitle("Poojas");
        binding.setHeader(header);
    }
}
