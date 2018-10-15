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
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityHomeBinding;
import com.ayyappasamaaj.tattvamasi.model.GridItem;
import com.ayyappasamaaj.tattvamasi.model.Header;
import com.ayyappasamaaj.tattvamasi.util.GridSpacingItemDecoration;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements GridRowAdapter.GridRowClickListener {

    private static final String TAG = "HomeActivity";
    private RecyclerView recyclerView;
    private ActivityHomeBinding binding;
    private ArrayList<GridItem> gridItemList = new ArrayList<GridItem>();
    private GridRowAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        Header header = new Header();
        binding.setHeader(header);

        // init the bhajans list
        loadBhajans();
        initRecyclerView();
    }

    private void loadBhajans() {
        gridItemList.add(new GridItem("Bhajans"));
        gridItemList.add(new GridItem("Pooja"));
        gridItemList.add(new GridItem("Articles"));
        gridItemList.add(new GridItem("Events"));
        gridItemList.add(new GridItem("Donate"));
        gridItemList.add(new GridItem("About"));
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;

        // this style is for grid 2x2
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(4), true));

        mAdapter = new GridRowAdapter(gridItemList, this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onGridRowItemClicked(GridItem gridItem) {
        Log.d(TAG, "GridItem item clicked = "+ gridItem.getName());
        switch (gridItem.getName()){
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
}
