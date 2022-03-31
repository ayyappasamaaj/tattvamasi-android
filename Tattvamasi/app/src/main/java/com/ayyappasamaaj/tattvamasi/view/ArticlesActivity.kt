package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.ListRowAdapter
import com.ayyappasamaaj.tattvamasi.databinding.ActivityArticlesBinding
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.model.ListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.util.SimpleDividerItemDecoration
import com.google.firebase.database.*

class ArticlesActivity : AppCompatActivity(), ListRowAdapter.ListRowClickListener {
    private var recyclerView: RecyclerView? = null
    private val articlesList: ArrayList<ListItem> = ArrayList()
    private var mAdapter: ListRowAdapter? = null
    private var category = "Articles"
    private var parentCategory = ""
    private var progress: ProgressDialog? = null

    private var binding: ActivityArticlesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        category = intent.getStringExtra("CATEGORY").toString()
        parentCategory = intent.getStringExtra("PARENT-CATEGORY").toString()

        // binding the view
        binding = DataBindingUtil.setContentView(this, R.layout.activity_articles)
        // header model to create header
        binding?.setHeader(Header(category))

        // init the articles list
        initRecyclerView()

        // read the articles from firebase
        readArticles()
    }

    private fun initRecyclerView() {
        recyclerView = binding?.recyclerView
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
        recyclerView?.layoutManager = mLayoutManager
        recyclerView?.addItemDecoration(SimpleDividerItemDecoration(this))
        mAdapter = ListRowAdapter(articlesList, this)
        recyclerView?.adapter = mAdapter
    }

    private fun readArticles() {
        showLoader()

        // get reference to database
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        category = category.lowercase()
        AppLog.d(TAG, "category = $category")
        val myRef: DatabaseReference = if (category.contains("articles")) {
            database.getReference(category)
        } else {
            database.getReference("$parentCategory/$category")
        }

        // get the list of events
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dismissLoader()
                for (postSnapshot in dataSnapshot.children) {
                    val listItem = postSnapshot.getValue(ListItem::class.java)
                    if (parentCategory.equals("pooja", ignoreCase = true)) {
                        val name = listItem?.itemTitle
                        val lang = listItem?.language
                        listItem?.itemTitle = "$name ($lang)"
                    }
                    listItem?.let { articlesList.add(it) }
                    mAdapter?.notifyDataSetChanged()
                    AppLog.d(TAG, "ListItem Name = " + listItem?.itemTitle)
                    AppLog.d(TAG, "ListItem URL = " + listItem?.fileUrl)
                    AppLog.d(TAG, "size = " + articlesList.size)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                dismissLoader()

                // Getting Post failed, AppLog a message
                AppLog.w(TAG, "loadPost:onCancelled "+databaseError.toException())
            }
        })
    }

    override fun onListRowItemClicked(listItem: ListItem?) {
        AppLog.d(TAG, "listItem clicked = " + listItem?.itemTitle)
        // call the pdf viewer
        val intent = Intent(this, PDFViewerActivity::class.java)
        intent.putExtra("URL", listItem?.fileUrl)
        intent.putExtra("TITLE", listItem?.itemTitle)
        this.startActivity(intent)
    }

    fun backClicked(view: View?) {
        AppLog.d(TAG, "Back clicked")
        this.finish()
    }

    private fun showLoader() {
        //progress.setTitle("Loading");
        progress?.setMessage("Wait while loading...")
        progress?.setCancelable(false) // disable dismiss by tapping outside of the dialog
        progress?.show()
    }

    private fun dismissLoader() {
        // To dismiss the dialog
        progress?.dismiss()
    }

    companion object {
        private const val TAG = "ArticlesActivity"
    }
}