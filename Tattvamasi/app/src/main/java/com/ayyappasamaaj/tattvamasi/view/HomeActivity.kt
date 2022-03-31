package com.ayyappasamaaj.tattvamasi.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
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
import com.ayyappasamaaj.tattvamasi.util.GridSpacingItemDecoration
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity(), GridRowClickListener {

    private lateinit var binding: ActivityHomeBinding
    private val gridItemList = ArrayList<GridItem>()
    private var mAdapter: GridRowAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.setHeader(Header())

        // init the bhajans list
        loadBhajans()
        initRecyclerView()
    }

    private fun loadBhajans() {
        gridItemList.add(GridItem("Bhajans"))
        gridItemList.add(GridItem("Pooja"))
        gridItemList.add(GridItem("Articles"))
        gridItemList.add(GridItem("Events"))
        gridItemList.add(GridItem("Donate"))
        gridItemList.add(GridItem("About"))
    }

    private fun initRecyclerView() {
        with(binding) {
            // this style is for grid 2x2
            recyclerView.layoutManager = GridLayoutManager(this@HomeActivity, 2)
            recyclerView.addItemDecoration(GridSpacingItemDecoration(2, dpToPx(), true))
            mAdapter = GridRowAdapter(gridItemList, this@HomeActivity)
            recyclerView.adapter = mAdapter
        }
    }

    override fun onGridRowItemClicked(gridItem: GridItem?) {
        AppLog.d(TAG, "GridItem item clicked = " + gridItem?.name)
        when (gridItem?.name) {
            "Bhajans" -> {
                val bhajanIntent = Intent(this, BhajansActivity::class.java)
                this.startActivity(bhajanIntent)
            }
            "Pooja" -> {
                val poojaIntent = Intent(this, PoojaActivity::class.java)
                this.startActivity(poojaIntent)
            }
            "Articles" -> {
                val articlesIntent = Intent(this, ArticlesActivity::class.java)
                articlesIntent.putExtra("CATEGORY", "Articles")
                this.startActivity(articlesIntent)
            }
            "Events" -> {
                val eventsIntent = Intent(this, EventsActivity::class.java)
                this.startActivity(eventsIntent)
            }
            "About" -> {
                val aboutIntent = Intent(this, AboutUsActivity::class.java)
                this.startActivity(aboutIntent)
            }
            "Donate" -> {
                val donateIntent = Intent(this, DonateActivity::class.java)
                this.startActivity(donateIntent)
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private fun dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
        ).roundToInt()
    }

    companion object {
        private const val TAG = "HomeActivity"
    }
}