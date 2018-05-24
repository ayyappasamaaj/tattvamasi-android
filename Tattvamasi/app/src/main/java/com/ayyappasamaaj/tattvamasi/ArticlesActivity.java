package com.ayyappasamaaj.tattvamasi;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayyappasamaaj.tattvamasi.databinding.ActivityArticlesBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;

public class ArticlesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_articles);
        ActivityArticlesBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_articles);
        Header header = new Header();
        header.setTitle("Articles");
        binding.setHeader(header);
    }
}
