package com.ayyappasamaaj.tattvamasi.adapter;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.BhajanRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.Bhajan;

import java.util.List;

public class BhajansAdapter extends RecyclerView.Adapter<BhajansAdapter.MyViewHolder> {

    private List<Bhajan> bhajanList;
    private LayoutInflater layoutInflater;
    private BhajanAdapterListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final BhajanRowItemBinding binding;

        public MyViewHolder(final BhajanRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public BhajansAdapter(List<Bhajan> bhajanList, BhajanAdapterListener listener) {
        this.bhajanList = bhajanList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        BhajanRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.bhajan_row_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.binding.setBhajan(bhajanList.get(position));
        Resources res = layoutInflater.getContext().getResources();
        int resourceId = res.getIdentifier(bhajanList.get(position).getName().toLowerCase(), "drawable", layoutInflater.getContext().getPackageName());
        holder.binding.bhajan.setImageResource(resourceId);
        holder.binding.bhajan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onBhajanClicked(bhajanList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bhajanList.size();
    }

    public interface BhajanAdapterListener {
        void onBhajanClicked(Bhajan bhajan);
    }
}
