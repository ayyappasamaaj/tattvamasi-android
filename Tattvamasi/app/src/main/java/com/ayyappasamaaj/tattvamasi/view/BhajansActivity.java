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
import com.ayyappasamaaj.tattvamasi.adapter.BhajansAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityBhajansBinding;
import com.ayyappasamaaj.tattvamasi.model.Bhajan;
import com.ayyappasamaaj.tattvamasi.model.Header;

import java.util.ArrayList;

public class BhajansActivity extends AppCompatActivity implements BhajansAdapter.BhajanAdapterListener{

    private static final String TAG = "BhajansActivity";
    private RecyclerView recyclerView;
    private ActivityBhajansBinding binding;
    private ArrayList<Bhajan> bhajanList = new ArrayList<Bhajan>();
    private BhajansAdapter mAdapter;

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
        bhajanList.add(new Bhajan("Ayyappan"));
        bhajanList.add(new Bhajan("Devi"));
        bhajanList.add(new Bhajan("Ganesha"));
        bhajanList.add(new Bhajan("Guru"));
        bhajanList.add(new Bhajan("Hanuman"));
        bhajanList.add(new Bhajan("Murugan"));
        bhajanList.add(new Bhajan("Rama"));
        bhajanList.add(new Bhajan("Shiva"));
        bhajanList.add(new Bhajan("Vishnu"));
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        //RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        //recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        //recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mAdapter = new BhajansAdapter(bhajanList, this);
        recyclerView.setAdapter(mAdapter);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }

    @Override
    public void onBhajanClicked(Bhajan bhajan) {
        Log.d(TAG, "Bhajan item clicked = "+bhajan.getName());
        Intent intent = new Intent(this, ArticlesActivity.class);
        intent.putExtra("CATEGORY", bhajan.getName());
        this.startActivity(intent);
    }
}
