package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.adapter.ParentListRowAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPoojaBinding;
import com.ayyappasamaaj.tattvamasi.model.Header;
import com.ayyappasamaaj.tattvamasi.model.ParentListItem;
import com.ayyappasamaaj.tattvamasi.util.SimpleDividerItemDecoration;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PoojaActivity extends AppCompatActivity implements ParentListRowAdapter.ParentListRowClickListener{

    private static final String TAG = "PoojaActivity";
    private RecyclerView recyclerView;
    private ActivityPoojaBinding binding;
    private ArrayList<ParentListItem> poojaList = new ArrayList<ParentListItem>();
    private ParentListRowAdapter mAdapter;
    private String category = "pooja-categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pooja);
        Header header = new Header();
        header.setTitle("Poojas");
        binding.setHeader(header);

        // init the pooja list
        initRecyclerView();

        // read the pooja from firebase
        readPoojas();
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mAdapter = new ParentListRowAdapter(poojaList, this);
        recyclerView.setAdapter(mAdapter);
    }

    private void readPoojas(){
        // get reference to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        category = category.toLowerCase();
        Log.d(TAG, "category = "+ category);
        DatabaseReference myRef = database.getReference(category);

        // get the list of events
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {

                    String name = postSnapshot.getValue(String.class);
                    poojaList.add(new ParentListItem(name));
                    mAdapter.notifyDataSetChanged();

                    Log.d(TAG, "ListItem Name = "+ name);
                    Log.d(TAG, "size = "+ poojaList.size());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onListRowItemClicked(ParentListItem listItem) {
        Log.d(TAG, "listItem clicked = "+ listItem.getName());
        Intent intent = new Intent(this, ArticlesActivity.class);
        intent.putExtra("PARENT-CATEGORY", "pooja");
        intent.putExtra("CATEGORY", listItem.getName());
        this.startActivity(intent);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
