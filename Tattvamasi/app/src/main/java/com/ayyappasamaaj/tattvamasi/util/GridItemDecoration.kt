package com.ayyappasamaaj.tattvamasi.util

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration


/**
 * Created by Gangadhar Kondati on 10,April,2022
 */
class GridItemDecoration(context: Context, @DimenRes itemOffsetId: Int) : ItemDecoration() {
    private val itemOffset = context.resources.getDimensionPixelSize(itemOffsetId)
    private val offSetTop = itemOffset/3

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(itemOffset, offSetTop, itemOffset, offSetTop)
    }
}