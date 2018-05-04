package com.ayyappasamaaj.tattvamasi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
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
