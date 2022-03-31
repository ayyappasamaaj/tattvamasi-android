package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.EventsAdapter
import com.ayyappasamaaj.tattvamasi.adapter.EventsAdapter.EventsAdapterListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityEventsBinding
import com.ayyappasamaaj.tattvamasi.model.Event
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EventsActivity : AppCompatActivity(), EventsAdapterListener {
    private var recyclerView: RecyclerView? = null
    private var binding: ActivityEventsBinding? = null
    private val eventsList = ArrayList<Event>()
    private var mAdapter: EventsAdapter? = null
    private var progress: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        // binding the view
        binding = DataBindingUtil.setContentView(this, R.layout.activity_events)
        // header model to create header
        binding?.setHeader(Header("Events"))
        // init the events list
        initRecyclerView()

        // read the events from firebase
        readEvents()
    }

    private fun initRecyclerView() {
        recyclerView = binding?.recyclerView
        val mLayoutManager: LayoutManager = LinearLayoutManager(applicationContext)
        recyclerView?.layoutManager = mLayoutManager
        mAdapter = EventsAdapter(eventsList, this)
        recyclerView?.adapter = mAdapter
    }

    private fun readEvents() {
        showLoader()
        // get reference to database
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("events")
        // get the list of events
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dismissLoader()
                for (postSnapshot in dataSnapshot.children) {
                    val event = postSnapshot.getValue(
                        Event::class.java
                    )
                    val eventTime = event?.date
                    val currentTime = System.currentTimeMillis() / 1000
                    if (eventTime != null) {
                        if (eventTime > currentTime) {
                            eventsList.add(event)
                            mAdapter?.notifyDataSetChanged()
                        }
                    }
                    AppLog.d(TAG, "Event Name = " + event?.name)
                    AppLog.d(TAG, "size = " + eventsList.size)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                dismissLoader()
                // Getting Post failed, AppLog a message
                AppLog.w(TAG, "loadPost:onCancelled"+databaseError.toException())
            }
        })
    }

    override fun onVenueClicked(event: Event?) {
        val uri = "http://maps.google.co.in/maps?q=" + event?.venue
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    fun backClicked(view: View?) {
        finish()
    }

    private fun showLoader() {
        progress?.setMessage("Wait while loading...")
        progress?.setCancelable(false) // disable dismiss by tapping outside of the dialog
        progress?.show()
    }

    private fun dismissLoader() {
        // To dismiss the dialog
        progress?.dismiss()
    }

    companion object {
        private const val TAG = "EventsActivity"
    }
}