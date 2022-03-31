package com.ayyappasamaaj.tattvamasi.model

import android.view.View
import androidx.databinding.BindingAdapter

data class Header(val title: String? = null) {
    var isTitleRequired = false // when title is set, logo is set to false
    var isLogoRequired = true // by default logo is enabled

    init {
        title?.let {
            isTitleRequired = true
            isLogoRequired = false
        }
    }

    companion object {
        @BindingAdapter("visibleIf")
        @JvmStatic
        fun changeVisibility(view: View, visible: Boolean) {
            view.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}