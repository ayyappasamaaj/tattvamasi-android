package com.ayyappasamaaj.tattvamasi.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ArticleRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.Article;

import java.util.List;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.MyViewHolder> {

    private List<Article> articleList;
    private LayoutInflater layoutInflater;
    private ArticleAdapterListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final ArticleRowItemBinding binding;

        public MyViewHolder(final ArticleRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public ArticlesAdapter(List<Article> articleList, ArticleAdapterListener listener) {
        this.articleList = articleList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        ArticleRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.article_row_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.binding.setArticle(articleList.get(position));
        holder.binding.article.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onArticleClicked(articleList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }

    public interface ArticleAdapterListener {
        void onArticleClicked(Article article);
    }
}
