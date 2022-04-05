package com.ayyappasamaaj.tattvamasi.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ayyappasamaaj.tattvamasi.model.ListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.*


/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
class ArticleViewModel : ViewModel() {

    private var _articleListLiveData: MutableLiveData<List<ListItem>?> = MutableLiveData()
    val articleListLiveData: LiveData<List<ListItem>?> = _articleListLiveData

    fun loadArticles(category: String, parentCategory: String) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference: DatabaseReference = if (category.contains("articles")) {
            database.getReference(category)
        } else {
            database.getReference("$parentCategory/$category")
        }
        val articlesList: ArrayList<ListItem> = ArrayList()

        // get the list of events
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val innerList: ArrayList<ListItem> = ArrayList()
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(ListItem::class.java)?.let {
                        innerList.add(it)
                    }
                }
                innerList.groupBy {
                    it.language
                }.map {
                    articlesList.add(ListItem(header = true, itemTitle = it.key))
                    articlesList.addAll(it.value)
                }
                _articleListLiveData.value = articlesList
            }

            override fun onCancelled(databaseError: DatabaseError) {
                _articleListLiveData.value = null
                // Getting Post failed, AppLog a message
                AppLog.w(TAG, "loadPost:onCancelled " + databaseError.toException())
            }
        })
    }

    companion object {
        const val TAG = "ArticleViewModel"
    }
}