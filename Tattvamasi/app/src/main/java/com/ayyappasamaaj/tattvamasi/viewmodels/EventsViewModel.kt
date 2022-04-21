package com.ayyappasamaaj.tattvamasi.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayyappasamaaj.tattvamasi.model.Event
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
class EventsViewModel : ViewModel() {

    private var _eventsListLiveData: MutableLiveData<List<Event>?> = MutableLiveData()
    val eventsLiveData: LiveData<List<Event>?> = _eventsListLiveData

    fun loadEvents() {
        val database = FirebaseDatabase.getInstance()
        val eventsList = ArrayList<Event>()
        // get the list of events
        database.getReference("events").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // dismissLoader()
                for (postSnapshot in dataSnapshot.children) {
                    val event = postSnapshot.getValue(Event::class.java)
                    val eventTime = event?.date
                    val currentTime = System.currentTimeMillis() / 1000
                    if (eventTime != null) {
                        if (eventTime > currentTime) {
                            eventsList.add(event)
                        }
                    }
                    AppLog.d(TAG, "Event Name = " + event?.name)
                    AppLog.d(TAG, "size = " + eventsList.size)
                }
                _eventsListLiveData.value = eventsList
            }

            override fun onCancelled(databaseError: DatabaseError) {
                _eventsListLiveData.value = null
                // Getting Post failed, AppLog a message
                AppLog.w(TAG, "loadPost:onCancelled" + databaseError.toException())
            }
        })
    }

    companion object {
        const val TAG = "EventsViewModel"
    }
}