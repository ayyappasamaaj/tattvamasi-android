package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.ParentListRowAdapter
import com.ayyappasamaaj.tattvamasi.adapter.ParentListRowAdapter.ParentListRowClickListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPoojaBinding
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.model.ParentListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.util.SimpleDividerItemDecoration
import com.ayyappasamaaj.tattvamasi.view.ArticlesActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PoojaActivity : AppCompatActivity(), ParentListRowClickListener {

    // private var binding: ActivityPoojaBinding? = null
    private val poojaList = ArrayList<ParentListItem>()
    private var mAdapter: ParentListRowAdapter? = null
    private var category = "pooja_categories"
    private var progress: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        val binding: ActivityPoojaBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_pooja)
        binding.setHeader(Header("Poojas"))

        // init the pooja list
        initRecyclerView(binding)

        // read the pooja from firebase
        readPoojas()
    }

    private fun initRecyclerView(binding: ActivityPoojaBinding) {
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(this@PoojaActivity)
            addItemDecoration(SimpleDividerItemDecoration(this@PoojaActivity))
            mAdapter = ParentListRowAdapter(poojaList, this@PoojaActivity)
            adapter = mAdapter
        }
    }

    private fun readPoojas() {
        showLoader()
        // get reference to database
        val database = FirebaseDatabase.getInstance()
        category = category.lowercase()
        AppLog.d(TAG, "category = $category")
        // get the list of events
        database.getReference(category).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dismissLoader()
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(String::class.java)?.let {
                        poojaList.add(ParentListItem(it))
                        mAdapter?.notifyDataSetChanged()
                        AppLog.d(TAG, "ListItem Name = $it")
                        AppLog.d(TAG, "size = " + poojaList.size)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                dismissLoader()
                // Getting Post failed, log a message
                AppLog.w(TAG, "loadPost:onCancelled " + databaseError.toException())
            }
        })
    }

    override fun onListRowItemClicked(listItem: ParentListItem?) {
        AppLog.d(TAG, "listItem clicked = " + listItem?.name)
        val intent = Intent(this, ArticlesActivity::class.java)
        intent.putExtra("PARENT-CATEGORY", "pooja")
        intent.putExtra("CATEGORY", listItem?.name)
        this.startActivity(intent)
    }

    fun backClicked(view: View?) {
        AppLog.d(TAG, "Back clicked")
        finish()
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
        private const val TAG = "PoojaActivity"
    }
}