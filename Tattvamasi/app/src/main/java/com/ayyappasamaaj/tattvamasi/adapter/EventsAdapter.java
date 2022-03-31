package com.ayyappasamaaj.tattvamasi.adapter;

import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

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

        SpannableString content = new SpannableString(eventList.get(position).getVenue());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        holder.binding.venue.setText(content);

        holder.binding.venue.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVenueClicked(eventList.get(holder.getAbsoluteAdapterPosition()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public interface EventsAdapterListener {
        void onVenueClicked(Event event);
    }
}