package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityHomeBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_home);
        ActivityHomeBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        Header header = new Header();
        binding.setHeader(header);
    }

    public void OnBhajanClick(View view){
        Intent intent = new Intent(this, BhajansActivity.class);
        this.startActivity(intent);
    }

    public void OnPoojaClick(View view){
        Intent intent = new Intent(this, PoojaActivity.class);
        this.startActivity(intent);
    }

    public void OnArticlesClick(View view){
        Intent intent = new Intent(this, ArticlesActivity.class);
        this.startActivity(intent);
    }

    public void OnEventsClick(View view){
        Intent intent = new Intent(this, EventsActivity.class);
        this.startActivity(intent);
    }

    public void OnAboutUsClick(View view){
        Intent intent = new Intent(this, AboutUsActivity.class);
        this.startActivity(intent);
    }

    public void OnDonateClick(View view){
        Intent intent = new Intent(this, DonateActivity.class);
        this.startActivity(intent);
    }
}
