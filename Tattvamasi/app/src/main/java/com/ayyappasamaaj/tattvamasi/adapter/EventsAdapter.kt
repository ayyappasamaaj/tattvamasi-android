package com.ayyappasamaaj.tattvamasi.adapter

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.EventRowItemBinding
import com.ayyappasamaaj.tattvamasi.model.Event

class EventsAdapter(
    private val eventList: List<Event>,
    private val listener: EventsAdapterListener?
) : RecyclerView.Adapter<EventsAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding: EventRowItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.event_row_item, parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder.binding) {
            event = eventList[position]
            val content = SpannableString(eventList[position].venue)
            content.setSpan(UnderlineSpan(), 0, content.length, 0)
            venueTxt.text = content
            venueTxt.setOnClickListener {
                listener?.onVenueClicked(
                    eventList[holder.absoluteAdapterPosition]
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return eventList.size
    }

    interface EventsAdapterListener {
        fun onVenueClicked(event: Event?)
    }

    inner class MyViewHolder(val binding: EventRowItemBinding) : ViewHolder(
        binding.root
    )
}