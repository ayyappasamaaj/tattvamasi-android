package com.ayyappasamaaj.tattvamasi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayyappasamaaj.tattvamasi.databinding.ActivityEventsBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class EventsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_events);
        ActivityEventsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_events);
        Header header = new Header();
        header.setTitle("Events");
        binding.setHeader(header);
    }
}
