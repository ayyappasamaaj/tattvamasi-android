package com.ayyappasamaaj.tattvamasi.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ParentListRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.ParentListItem;

import java.util.List;

public class ParentListRowAdapter extends RecyclerView.Adapter<ParentListRowAdapter.MyViewHolder> {

    private List<ParentListItem> listItemList;
    private LayoutInflater layoutInflater;
    private ParentListRowClickListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final ParentListRowItemBinding binding;

        public MyViewHolder(final ParentListRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public ParentListRowAdapter(List<ParentListItem> listItemList, ParentListRowClickListener listener) {
        this.listItemList = listItemList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        ParentListRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.parent_list_row_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.binding.setListItem(listItemList.get(position));
        holder.binding.listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onListRowItemClicked(listItemList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return listItemList.size();
    }

    public interface ParentListRowClickListener {
        void onListRowItemClicked(ParentListItem listItem);
    }
}

