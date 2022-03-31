package com.ayyappasamaaj.tattvamasi.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.adapter.EventsAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityEventsBinding;
import com.ayyappasamaaj.tattvamasi.model.Event;
import com.ayyappasamaaj.tattvamasi.model.Header;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class EventsActivity extends AppCompatActivity implements EventsAdapter.EventsAdapterListener{

    private static final String TAG = "EventsActivity";
    private RecyclerView recyclerView;
    private ActivityEventsBinding binding;
    private ArrayList<Event> eventsList = new ArrayList<Event>();
    private EventsAdapter mAdapter;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progress = new ProgressDialog(this);
        // binding the view
        binding = DataBindingUtil.setContentView(this, R.layout.activity_events);
        // header model to create header
        Header header = new Header();
        header.setTitle("Events");
        binding.setHeader(header);
        // init the events list
        initRecyclerView();

        // read the events from firebase
        readEvents();
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new EventsAdapter(eventsList, this);
        recyclerView.setAdapter(mAdapter);
    }

    private void readEvents(){
        showLoader();

        // get reference to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("events");

        // get the list of events
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dismissLoader();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {

                    Event event = postSnapshot.getValue(Event.class);

                    long eventTime = event.getDate();
                    long currentTime = System.currentTimeMillis()/1000;

                    if (eventTime > currentTime) {
                        eventsList.add(event);
                        mAdapter.notifyDataSetChanged();
                    }

                    Log.d(TAG, "Event Name = "+event.getName());
                    Log.d(TAG, "size = "+eventsList.size());
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
    public void onVenueClicked(Event event) {
        String uri = "http://maps.google.co.in/maps?q=" + event.getVenue();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }

    private void showLoader(){
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
    }

    private void dismissLoader(){
        // To dismiss the dialog
        progress.dismiss();
    }

}
