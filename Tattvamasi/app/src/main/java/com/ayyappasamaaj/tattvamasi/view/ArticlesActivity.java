package com.ayyappasamaaj.tattvamasi.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.adapter.ArticlesAdapter;
import com.ayyappasamaaj.tattvamasi.databinding.ActivityArticlesBinding;
import com.ayyappasamaaj.tattvamasi.model.Article;
import com.ayyappasamaaj.tattvamasi.model.Header;
import com.ayyappasamaaj.tattvamasi.util.SimpleDividerItemDecoration;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ArticlesActivity extends AppCompatActivity implements ArticlesAdapter.ArticleAdapterListener{

    private static final String TAG = "ArticlesActivity";
    private RecyclerView recyclerView;
    private ActivityArticlesBinding binding;
    private ArrayList<Article> articlesList = new ArrayList<Article>();
    private ArticlesAdapter mAdapter;
    private String category = "Articles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        category = getIntent().getStringExtra("CATEGORY");

        // binding the view
        binding = DataBindingUtil.setContentView(this, R.layout.activity_articles);
        // header model to create header
        Header header = new Header();
        header.setTitle(category);
        binding.setHeader(header);

        // init the articles list
        initRecyclerView();

        // read the articles from firebase
        readArticles();
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mAdapter = new ArticlesAdapter(articlesList, this);
        recyclerView.setAdapter(mAdapter);
    }

    private void readArticles(){
        // get reference to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        category = category.toLowerCase();
        Log.d(TAG, "category = "+ category);
        DatabaseReference myRef;
        if(category.contains("articles")) {
            myRef = database.getReference(category);
        } else {
            myRef = database.getReference("bhajans/" + category);
        }

        // get the list of events
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {

                    Article article = postSnapshot.getValue(Article.class);
                    articlesList.add(article);
                    mAdapter.notifyDataSetChanged();

                    Log.d(TAG, "Article Name = "+article.getItemTitle());
                    Log.d(TAG, "size = "+ articlesList.size());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onArticleClicked(Article article) {
        Log.d(TAG, "article clicked = "+article.getItemTitle());

        // call the pdf viewer
        Intent intent = new Intent(this, PDFViewerActivity.class);
        intent.putExtra("URL", article.getFileUrl());
        intent.putExtra("TITLE", article.getItemTitle());
        this.startActivity(intent);
    }

    public void backClicked(View view) {
        Log.d(TAG, "Back clicked");
        this.finish();
    }
}
