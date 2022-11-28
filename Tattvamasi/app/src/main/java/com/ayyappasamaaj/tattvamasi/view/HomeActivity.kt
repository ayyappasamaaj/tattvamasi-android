package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter
import com.ayyappasamaaj.tattvamasi.adapter.GridRowAdapter.GridRowClickListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityHomeBinding
import com.ayyappasamaaj.tattvamasi.model.GridItem
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.util.GridItemDecoration
import com.ayyappasamaaj.tattvamasi.viewmodels.AppViewModel

class HomeActivity : AppCompatActivity(), GridRowClickListener {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityHomeBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.setHeader(Header())

        initRecyclerView(binding)
    }

    private fun initRecyclerView(binding: ActivityHomeBinding) {
        setPrivacyText(binding)
        with(binding) {
            // this style is for grid 2x2
            recyclerView.layoutManager = GridLayoutManager(this@HomeActivity, 2)
            val itemDecoration = GridItemDecoration(this@HomeActivity, R.dimen.item_offset)
            recyclerView.addItemDecoration(itemDecoration)
            recyclerView.adapter = GridRowAdapter(viewModel.bhajansList, this@HomeActivity)
        }
    }

    private fun setPrivacyText(binding: ActivityHomeBinding) {
        binding.privacyTextView.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(viewModel.privacyUrl)
            })
        }
    }

    override fun onGridRowItemClicked(gridItem: GridItem?) {
        AppLog.d(TAG, "GridItem item clicked = " + gridItem?.name)
        when (gridItem?.name) {
            getString(R.string.bhajans) -> {
                val bhajanIntent = Intent(this, BhajansActivity::class.java)
                this.startActivity(bhajanIntent)
            }
            getString(R.string.pooja) -> {
                val poojaIntent = Intent(this, PoojaActivity::class.java)
                this.startActivity(poojaIntent)
            }
            getString(R.string.articles) -> {
                val articlesIntent = Intent(this, ArticlesActivity::class.java)
                articlesIntent.putExtra("CATEGORY", "Articles")
                this.startActivity(articlesIntent)
            }
            getString(R.string.events) -> {
                val eventsIntent = Intent(this, EventsActivity::class.java)
                this.startActivity(eventsIntent)
            }
            getString(R.string.about) -> {
                val aboutIntent = Intent(this, AboutUsActivity::class.java)
                this.startActivity(aboutIntent)
            }
            getString(R.string.donate) -> {
                val donateIntent = Intent(this, DonateActivity::class.java)
                this.startActivity(donateIntent)
            }
        }
    }

    companion object {
        private const val TAG = "HomeActivity"
    }
}