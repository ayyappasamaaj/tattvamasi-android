package com.ayyappasamaaj.tattvamasi.util

import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
object ViewUtils {

    /**
     * Converting dp to pixel
     */
    fun Resources.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            displayMetrics
        ).roundToInt()
    }

    fun dpToPx(dp: Float): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

}