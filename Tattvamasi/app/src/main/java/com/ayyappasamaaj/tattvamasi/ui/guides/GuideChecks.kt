package com.ayyappasamaaj.tattvamasi.ui.guides

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What's ticked in the guides' checklists (House Puja's items), kept on the phone: the items are often gathered
 * over several days, so the ticks stay until "Clear all ticks". Keyed by the website's checkbox ids.
 */
object GuideChecks {
    private const val PREFS = "guide-checks"
    private const val KEY = "ticked"

    private val _ticked = MutableStateFlow<Set<String>>(emptySet())
    val ticked: StateFlow<Set<String>> = _ticked.asStateFlow()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        _ticked.value = prefs(context).getStringSet(KEY, emptySet()).orEmpty().toSet()
    }

    fun toggle(context: Context, id: String) = save(context, if (id in _ticked.value) _ticked.value - id else _ticked.value + id)

    fun clear(context: Context, ids: Collection<String>) = save(context, _ticked.value - ids.toSet())

    private fun save(context: Context, ticked: Set<String>) {
        _ticked.value = ticked
        prefs(context).edit().putStringSet(KEY, ticked).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
