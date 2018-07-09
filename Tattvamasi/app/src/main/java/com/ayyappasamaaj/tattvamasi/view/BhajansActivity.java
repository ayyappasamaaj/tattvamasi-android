package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityBhajansBinding;
import com.ayyappasamaaj.tattvamasi.model.GridItem;
import com.ayyappasamaaj.tattvamasi.model.Header;

import java.util.ArrayList;

public class BhajansActivity extends AppCompatActivity implements GridRowAdapter.GridRowClickListener {

    private static final String TAG = "BhajansActivity";
    private RecyclerView recyclerView;
    private ActivityBhajansBinding binding;
    private ArrayList<GridItem> gridItemList = new ArrayList<GridItem>();
    private GridRowAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bhajans);
        Header header = new Header();
        header.setTitle("Bhajans");
        binding.setHeader(header);

        // init the bhajans list
        loadBhajans();
        initRecyclerView();
    }

    private void loadBhajans() {
        gridItemList.add(new GridItem("Ayyappan"));
        gridItemList.add(new GridItem("Devi"));
        gridItemList.add(new GridItem("Ganesha"));
        gridItemList.add(new GridItem("Guru"));
        gridItemList.add(new GridItem("Hanuman"));
        gridItemList.add(new GridItem("Muruga"));
        gridItemList.add(new GridItem("Rama"));
        gridItemList.add(new GridItem("Shiva"));
        gridItemList.add(new GridItem("Vishnu"));
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        //RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        //recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        //recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mAdapter = new GridRowAdapter(gridItemList, this);
        recyclerView.setAdapter(mAdapter);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }

    @Override
    public void onGridRowItemClicked(GridItem gridItem) {
        Log.d(TAG, "GridItem item clicked = "+ gridItem.getName());
        Intent intent = new Intent(this, ArticlesActivity.class);
        intent.putExtra("CATEGORY", gridItem.getName());
        this.startActivity(intent);
    }
}
