package com.ayyappasamaaj.tattvamasi.view

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.adapter.ArticleAdapter
import com.ayyappasamaaj.tattvamasi.databinding.ActivityArticlesBinding
import com.ayyappasamaaj.tattvamasi.model.Header
import com.ayyappasamaaj.tattvamasi.model.ListItem
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.ayyappasamaaj.tattvamasi.util.StickyHeaderItemDecoration
import com.ayyappasamaaj.tattvamasi.viewmodels.ArticleViewModel

class ArticlesActivity : AppCompatActivity(), ArticleAdapter.ListRowClickListener {

    private var category = "Articles"
    private var parentCategory = ""
    private var progress: ProgressDialog? = null

    private lateinit var viewAdapter: RecyclerView.Adapter<*>

    private val viewModel: ArticleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressDialog(this)
        category = intent.getStringExtra("CATEGORY").toString()
        parentCategory = intent.getStringExtra("PARENT-CATEGORY").toString()
        viewAdapter = ArticleAdapter(this)

        // binding the view
        val binding: ActivityArticlesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_articles)
        // header model to create header
        binding.setHeader(Header(category))

        // init the articles list
        initRecyclerView(binding)

        // read the articles from firebase
        viewModel.loadArticles(category.lowercase(), parentCategory)
        observeArticleData()
    }

    private fun initRecyclerView(binding: ActivityArticlesBinding) {
        binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        binding.recyclerView.apply {
            addItemDecoration(StickyHeaderItemDecoration(viewAdapter as ArticleAdapter))
            adapter = viewAdapter
        }
    }

    private fun observeArticleData() {
        showLoader()
        viewModel.articleListLiveData.observe(this) { result ->
            dismissLoader()
            if (!result.isNullOrEmpty()) {
                (viewAdapter as ArticleAdapter).updateData(result)
            }
        }
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