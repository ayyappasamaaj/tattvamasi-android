package com.ayyappasamaaj.tattvamasi.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private String category = "pooja_categories";
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progress = new ProgressDialog(this);
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
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mAdapter = new ParentListRowAdapter(poojaList, this);
        recyclerView.setAdapter(mAdapter);
    }

    private void readPoojas(){
        showLoader();
        // get reference to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        category = category.toLowerCase();
        Log.d(TAG, "category = "+ category);
        DatabaseReference myRef = database.getReference(category);

        // get the list of events
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dismissLoader();
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
                dismissLoader();
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

    private void showLoader(){
        //progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
    }

    private void dismissLoader(){
        // To dismiss the dialog
        progress.dismiss();
    }
}
