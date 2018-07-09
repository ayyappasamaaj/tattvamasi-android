package com.ayyappasamaaj.tattvamasi.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.ListRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.ListItem;

import java.util.List;

public class ListRowAdapter extends RecyclerView.Adapter<ListRowAdapter.MyViewHolder> {

    private List<ListItem> listItemList;
    private LayoutInflater layoutInflater;
    private ListRowClickListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final ListRowItemBinding binding;

        public MyViewHolder(final ListRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public ListRowAdapter(List<ListItem> listItemList, ListRowClickListener listener) {
        this.listItemList = listItemList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        ListRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.list_row_item, parent, false);
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

    public interface ListRowClickListener {
        void onListRowItemClicked(ListItem listItem);
    }
}
