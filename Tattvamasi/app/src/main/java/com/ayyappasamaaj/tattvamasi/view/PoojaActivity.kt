package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.ParentListRowAdapter
import com.ayyappasamaaj.tattvamasi.adapter.ParentListRowAdapter.ParentListRowClickListener
import com.ayyappasamaaj.tattvamasi.databinding.ActivityPoojaBinding
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.model.PoojaListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.util.SimpleDividerItemDecoration
import com.ayyappasamaaj.tattvamasi.viewmodels.PoojaViewModel

class PoojaActivity : AppCompatActivity(), ParentListRowClickListener {

    private var progress: ProgressDialog? = null
    private val viewModel: PoojaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        DataBindingUtil.setContentView<ActivityPoojaBinding?>(this, R.layout.activity_pooja).apply {
            setHeader(Header(getString(R.string.poojas)))
            recyclerView.layoutManager = LinearLayoutManager(this@PoojaActivity)
            recyclerView.addItemDecoration(SimpleDividerItemDecoration(this@PoojaActivity))
        }
        showLoader()
        viewModel.loadPoojaData("pooja_categories")
        viewModel.poojaLiveData.observe(this) { result ->
            dismissLoader()
            if (!result.isNullOrEmpty()) {
                ParentListRowAdapter(result, this@PoojaActivity)
            }
        }
    }

    override fun onListRowItemClicked(listItem: PoojaListItem?) {
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
        progress?.setMessage(getString(R.string.loading_msg))
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