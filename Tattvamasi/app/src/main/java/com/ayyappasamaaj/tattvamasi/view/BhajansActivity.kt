package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter.GridRowClickListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityBhajansBinding
import com.ayyappasamaaj.tattvamasi.model.GridItem
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.viewmodels.AppViewModel

class BhajansActivity : AppCompatActivity(), GridRowClickListener {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityBhajansBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_bhajans)
        binding.setHeader(Header("Bhajans"))

        // init the bhajans list
        initRecyclerView(binding)
    }

    private fun initRecyclerView(binding: ActivityBhajansBinding) {
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = GridRowAdapter(viewModel.bhajanItemsList, this)
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