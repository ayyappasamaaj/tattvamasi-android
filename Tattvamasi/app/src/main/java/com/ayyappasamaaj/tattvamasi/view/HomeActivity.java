package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.adapter.BhajansAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityHomeBinding;
import com.ayyappasamaaj.tattvamasi.model.Bhajan;
import com.ayyappasamaaj.tattvamasi.model.Header;
import com.ayyappasamaaj.tattvamasi.util.GridSpacingItemDecoration;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements BhajansAdapter.BhajanAdapterListener{

    private static final String TAG = "HomeActivity";
    private RecyclerView recyclerView;
    private ActivityHomeBinding binding;
    private ArrayList<Bhajan> bhajanList = new ArrayList<Bhajan>();
    private BhajansAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_home);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        Header header = new Header();
        binding.setHeader(header);

        // init the bhajans list
        loadBhajans();
        initRecyclerView();
    }

    private void loadBhajans() {
        bhajanList.add(new Bhajan("Bhajans"));
        bhajanList.add(new Bhajan("Pooja"));
        bhajanList.add(new Bhajan("Articles"));
        bhajanList.add(new Bhajan("Events"));
        bhajanList.add(new Bhajan("About"));
        bhajanList.add(new Bhajan("Donate"));
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;

        // This style is for row
        //RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        //recyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));

        // this style is for grid 2x2
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(4), true));

        mAdapter = new BhajansAdapter(bhajanList, this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onBhajanClicked(Bhajan bhajan) {
        Log.d(TAG, "Bhajan item clicked = "+bhajan.getName());
        switch (bhajan.getName()){
            case "Bhajans":
                Intent bhajanIntent = new Intent(this, BhajansActivity.class);
                this.startActivity(bhajanIntent);
                break;
            case "Pooja":
                Intent poojaIntent = new Intent(this, PoojaActivity.class);
                this.startActivity(poojaIntent);
                break;
            case "Articles":
                Intent articlesIntent = new Intent(this, ArticlesActivity.class);
                articlesIntent.putExtra("CATEGORY", "Articles");
                this.startActivity(articlesIntent);
                break;
            case "Events":
                Intent eventsIntent = new Intent(this, EventsActivity.class);
                this.startActivity(eventsIntent);
                break;
            case "About":
                Intent aboutIntent = new Intent(this, AboutUsActivity.class);
                this.startActivity(aboutIntent);
                break;
            case "Donate":
                Intent donateIntent = new Intent(this, DonateActivity.class);
                this.startActivity(donateIntent);
                break;
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    /*public void OnBhajanClick(View view){
        Intent intent = new Intent(this, BhajansActivity.class);
        this.startActivity(intent);
    }

    public void OnPoojaClick(View view){
        Intent intent = new Intent(this, PoojaActivity.class);
        this.startActivity(intent);
    }

    public void OnArticlesClick(View view){
        Intent intent = new Intent(this, ArticlesActivity.class);
        intent.putExtra("CATEGORY", "Articles");
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
    }*/
}
