package com.ayyappasamaaj.tattvamasi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.model.ListItem
import com.ayyappasamaaj.tattvamasi.util.StickyHeaderItemDecoration

const val TYPE_HEADER = 0
const val TYPE_ITEM = 1

class ArticleAdapter(private val listener: ListRowClickListener?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyHeaderItemDecoration.StickyHeaderInterface {

    private var listItemList: List<ListItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_row_sticky, parent, false)
            )
        } else {
            MyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_row_item, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItemList[position]
        if (holder is MyViewHolder) {
            holder.nameTxt.text = listItem.itemTitle
            holder.listItemView.setOnClickListener {
                listener?.onListRowItemClicked(
                    listItemList[holder.absoluteAdapterPosition]
                )
            }
        } else if (holder is HeaderViewHolder) {
            holder.headerTxt.text = listItem.itemTitle
        }
    }

    override fun getItemCount(): Int {
        return listItemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (listItemList[position].header) {
            TYPE_HEADER
        } else {
            TYPE_ITEM
        }
    }

    fun updateData(listItemList: List<ListItem>) {
        this.listItemList = listItemList
        notifyDataSetChanged()
    }

    interface ListRowClickListener {
        fun onListRowItemClicked(listItem: ListItem?)
    }

    inner class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val nameTxt: TextView = view.findViewById(R.id.nameTxt)
        val listItemView: View = view.findViewById(R.id.listItem)
    }

    inner class HeaderViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val headerTxt: TextView = view.findViewById(R.id.stickyHeader)
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return listItemList[itemPosition].header
    }

    override fun bindHeaderData(header: View, headerPosition: Int) {
        ((header as ConstraintLayout).getChildAt(0) as TextView).text =
            listItemList[headerPosition].itemTitle
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.list_row_sticky
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = 0
        var position = itemPosition
        do {
            if (this.isHeader(position)) {
                headerPosition = position
                break
            }
            position -= 1
        } while (position >= 0)
        return headerPosition
    }

}