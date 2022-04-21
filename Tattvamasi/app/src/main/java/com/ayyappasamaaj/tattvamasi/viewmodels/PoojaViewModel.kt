package com.ayyappasamaaj.tattvamasi.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayyappasamaaj.tattvamasi.model.Event
import com.ayyappasamaaj.tattvamasi.model.PoojaListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.view.PoojaActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
class PoojaViewModel : ViewModel() {

    private var _poojaListLiveData: MutableLiveData<List<PoojaListItem>?> = MutableLiveData()
    val poojaLiveData: LiveData<List<PoojaListItem>?> = _poojaListLiveData

    fun loadPoojaData(category: String) {
        val database = FirebaseDatabase.getInstance()
        val poojaList = ArrayList<PoojaListItem>()
        // get the list of events
        database.getReference(category).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(String::class.java)?.let {
                        poojaList.add(PoojaListItem(it))
                    }
                }
                _poojaListLiveData.value = poojaList
            }

            override fun onCancelled(databaseError: DatabaseError) {
                _poojaListLiveData.value = null
                AppLog.w(TAG, "loadPost:onCancelled" + databaseError.toException())
            }
        })
    }

    companion object {
        const val TAG = "EventsViewModel"
    }
}