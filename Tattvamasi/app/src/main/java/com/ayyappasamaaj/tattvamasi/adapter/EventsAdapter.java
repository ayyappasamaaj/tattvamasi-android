package com.ayyappasamaaj.tattvamasi.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ayyappasamaaj.tattvamasi.R;
import com.ayyappasamaaj.tattvamasi.databinding.EventRowItemBinding;
import com.ayyappasamaaj.tattvamasi.model.Event;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.MyViewHolder> {

    private List<Event> eventList;
    private LayoutInflater layoutInflater;
    private EventsAdapterListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final EventRowItemBinding binding;

        public MyViewHolder(final EventRowItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }


    public EventsAdapter(List<Event> eventList, EventsAdapterListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }
        EventRowItemBinding binding =
                DataBindingUtil.inflate(layoutInflater, R.layout.event_row_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.binding.setEvent(eventList.get(position));
        /*holder.binding.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEventClicked(eventList.get(position));
                }
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public interface EventsAdapterListener {
        void onEventClicked(Event event);
    }
}