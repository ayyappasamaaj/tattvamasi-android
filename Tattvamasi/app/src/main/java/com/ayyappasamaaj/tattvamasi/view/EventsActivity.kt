package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.EventsAdapter
import com.ayyappasamaaj.tattvamasi.adapter.EventsAdapter.EventsAdapterListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityEventsBinding
import com.ayyappasamaaj.tattvamasi.model.Event
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.viewmodels.EventsViewModel

class EventsActivity : AppCompatActivity(), EventsAdapterListener {

    private val eventsList = ArrayList<Event>()
    private var progress: ProgressDialog? = null

    private val viewModel: EventsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        // binding the view
        DataBindingUtil.setContentView<ActivityEventsBinding?>(this, R.layout.activity_events)
            ?.apply {
                // header model to create header
                setHeader(Header("Events"))
                recyclerView.layoutManager = LinearLayoutManager(applicationContext)
                readEvents(this)
            }
    }

    private fun readEvents(binding: ActivityEventsBinding) {
        showLoader()
        viewModel.loadEvents()
        viewModel.eventsLiveData.observe(this) { results ->
            dismissLoader()
            if (!results.isNullOrEmpty()) {
                binding.recyclerView.adapter = EventsAdapter(eventsList, this)
            } else {
                Toast.makeText(this, getString(R.string.no_events_to_display), Toast.LENGTH_SHORT)
                    .show()
            }
        }
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