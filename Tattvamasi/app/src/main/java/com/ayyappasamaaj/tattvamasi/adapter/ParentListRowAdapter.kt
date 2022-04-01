package com.ayyappasamaaj.tattvamasi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ParentListRowItemBinding
import com.ayyappasamaaj.tattvamasi.model.PoojaListItem

class ParentListRowAdapter(
    private val listItemList: List<PoojaListItem>,
    private val listener: ParentListRowClickListener?
) : RecyclerView.Adapter<ParentListRowAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding: ParentListRowItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.parent_list_row_item, parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.setListItem(listItemList[position])
        holder.binding.listItem.setOnClickListener {
            listener?.onListRowItemClicked(
                listItemList[holder.absoluteAdapterPosition]
            )
        }
    }

    override fun getItemCount(): Int {
        return listItemList.size
    }

    interface ParentListRowClickListener {
        fun onListRowItemClicked(listItem: PoojaListItem?)
    }

    inner class MyViewHolder(val binding: ParentListRowItemBinding) : ViewHolder(
        binding.root
    )
}