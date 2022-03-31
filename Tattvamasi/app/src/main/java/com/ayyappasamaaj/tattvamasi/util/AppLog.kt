package com.ayyappasamaaj.tattvamasi.util

import android.util.Log
import com.ayyappasamaaj.tattvamasi.BuildConfig

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
object AppLog {

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
    }
}