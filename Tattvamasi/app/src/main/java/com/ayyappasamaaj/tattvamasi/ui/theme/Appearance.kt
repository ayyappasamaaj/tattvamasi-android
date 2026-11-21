package com.ayyappasamaaj.tattvamasi.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The reader's choice of look, as in the website's menu: System (follows the phone, the default), Light or Dark.
 * Kept on the phone; chosen under About us, Contact (AppearanceCard).
 */
object Appearance {
    enum class Choice(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

    private const val PREFS = "appearance"
    private const val KEY = "choice"

    var choice by mutableStateOf(Choice.SYSTEM)
        private set

    /** Reads the saved choice (MainActivity, before the first frame). */
    fun load(context: Context) {
        choice = parse(prefs(context).getString(KEY, null))
    }

    fun set(context: Context, value: Choice) {
        choice = value
        prefs(context).edit().putString(KEY, value.name).apply()
    }

    /** A saved value back to a choice; anything unknown is System. */
    fun parse(saved: String?): Choice = Choice.entries.firstOrNull { it.name == saved } ?: Choice.SYSTEM

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
