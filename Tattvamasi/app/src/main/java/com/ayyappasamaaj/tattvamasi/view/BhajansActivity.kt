package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter.GridRowClickListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityBhajansBinding
import com.ayyappasamaaj.tattvamasi.model.GridItem
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.view.ArticlesActivity

class BhajansActivity : AppCompatActivity(), GridRowClickListener {

    private var recyclerView: RecyclerView? = null
    private var binding: ActivityBhajansBinding? = null
    private val gridItemList = ArrayList<GridItem>()
    private var mAdapter: GridRowAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bhajans)
        binding?.setHeader(Header("Bhajans"))

        // init the bhajans list
        loadBhajans()
        initRecyclerView()
    }

    private fun loadBhajans() {
        gridItemList.add(GridItem("Ganesha"))
        gridItemList.add(GridItem("Guru"))
        gridItemList.add(GridItem("Muruga"))
        gridItemList.add(GridItem("Devi"))
        gridItemList.add(GridItem("Shiva"))
        gridItemList.add(GridItem("Vishnu"))
        gridItemList.add(GridItem("Rama"))
        gridItemList.add(GridItem("Hanuman"))
        gridItemList.add(GridItem("Ayyappan"))
    }

    private fun initRecyclerView() {
        recyclerView = binding?.recyclerView
        recyclerView?.layoutManager = GridLayoutManager(this, 2)
        mAdapter = GridRowAdapter(gridItemList, this)
        recyclerView?.adapter = mAdapter
    }

    fun backClicked(view: View?) {
        finish()
    }

    override fun onGridRowItemClicked(gridItem: GridItem?) {
        AppLog.d(TAG, "GridItem item clicked = " + gridItem?.name)
        val intent = Intent(this, ArticlesActivity::class.java)
        intent.putExtra("PARENT-CATEGORY", "bhajans")
        intent.putExtra("CATEGORY", gridItem?.name)
        this.startActivity(intent)
    }

    companion object {
        private const val TAG = "BhajansActivity"
    }
}