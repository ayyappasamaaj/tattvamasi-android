package com.ayyappasamaaj.tattvamasi.view;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPoojaBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class PoojaActivity extends AppCompatActivity {

    private static final String TAG = "PoojaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPoojaBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_pooja);
        Header header = new Header();
        header.setTitle("Poojas");
        binding.setHeader(header);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
