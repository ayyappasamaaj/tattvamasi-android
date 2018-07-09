package com.ayyappasamaaj.tattvamasi.adapter;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.GridRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.GridItem;

import java.util.List;

public class GridRowAdapter extends RecyclerView.Adapter<GridRowAdapter.MyViewHolder> {

    private List<GridItem> gridItemList;
    private LayoutInflater layoutInflater;
    private GridRowClickListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final GridRowItemBinding binding;

        public MyViewHolder(final GridRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public GridRowAdapter(List<GridItem> gridItemList, GridRowClickListener listener) {
        this.gridItemList = gridItemList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        GridRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.grid_row_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.binding.setGridItem(gridItemList.get(position));
        Resources res = layoutInflater.getContext().getResources();
        int resourceId = res.getIdentifier(gridItemList.get(position).getName().toLowerCase(), "drawable", layoutInflater.getContext().getPackageName());
        holder.binding.bhajanImage.setImageResource(resourceId);
        holder.binding.bhajanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onGridRowItemClicked(gridItemList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return gridItemList.size();
    }

    public interface GridRowClickListener {
        void onGridRowItemClicked(GridItem gridItem);
    }
}
