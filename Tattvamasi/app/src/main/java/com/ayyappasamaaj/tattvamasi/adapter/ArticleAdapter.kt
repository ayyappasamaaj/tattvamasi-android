package com.ayyappasamaaj.tattvamasi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.ListRowItemBinding
import com.ayyappasamaaj.tattvamasi.model.ListItem

class ArticleAdapter(
    private var listItemList: List<ListItem>,
    private val listener: ListRowClickListener?
) : RecyclerView.Adapter<ArticleAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding: ListRowItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.list_row_item, parent, false
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

    fun updateData(listItemList: List<ListItem>) {
        this.listItemList = listItemList
        notifyDataSetChanged()
    }

    interface ListRowClickListener {
        fun onListRowItemClicked(listItem: ListItem?)
    }

    inner class MyViewHolder(val binding: ListRowItemBinding) : ViewHolder(
        binding.root
    )

}