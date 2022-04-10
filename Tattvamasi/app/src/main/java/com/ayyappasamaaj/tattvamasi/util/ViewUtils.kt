package com.ayyappasamaaj.tattvamasi.util

import android.content.res.Resources
import android.content.res.Resources.getSystem
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
object ViewUtils {

    /**
     * Converting dp to pixel
     */
    fun Resources.dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            displayMetrics
        ).roundToInt()
    }

    fun dpToPx(dp: Float): Int {
        return (dp * getSystem().displayMetrics.density).toInt()
    }

}