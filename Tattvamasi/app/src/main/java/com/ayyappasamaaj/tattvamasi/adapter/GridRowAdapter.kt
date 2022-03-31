package com.ayyappasamaaj.tattvamasi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.databinding.GridRowItemBinding
import com.ayyappasamaaj.tattvamasi.model.GridItem

class GridRowAdapter(
    private val gridItemList: List<GridItem>,
    private val listener: GridRowClickListener?
) : RecyclerView.Adapter<GridRowAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding: GridRowItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.grid_row_item, parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder.binding) {
            gridItem = gridItemList[position]
            val context = bhajanImage.context
            val resourceId = context.resources.getIdentifier(
                gridItemList[position].name?.lowercase(),
                "drawable",
                context.packageName
            )
            bhajanImage.setImageResource(resourceId)
            bhajanImage.setOnClickListener {
                listener?.onGridRowItemClicked(
                    gridItemList[holder.absoluteAdapterPosition]
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return gridItemList.size
    }

    interface GridRowClickListener {
        fun onGridRowItemClicked(gridItem: GridItem?)
    }

    inner class MyViewHolder(val binding: GridRowItemBinding) : ViewHolder(
        binding.root
    )

}