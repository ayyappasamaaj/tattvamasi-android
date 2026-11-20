package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Publications' settings, kept on the phone, as the website keeps them in the browser: "Reading in" (the language
 * every title opens in; English when not set, website localStorage "aysa-pdf-language") and the last five searches
 * ("aysa-pubs-recent-searches"). As on the iOS app (ReadingPrefs.swift).
 */
object ReadingPrefs {
    private const val PREFS = "publications"
    private const val LANGUAGE = "reading-in"
    private const val RECENTS = "recent-searches"
    private const val SEPARATOR = "\n"

    private val _language = MutableStateFlow(ENGLISH)
    val language: StateFlow<String> = _language.asStateFlow()
    private val _recents = MutableStateFlow<List<String>>(emptyList())
    val recents: StateFlow<List<String>> = _recents.asStateFlow()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = prefs(context)
        _language.value = prefs.getString(LANGUAGE, null) ?: ENGLISH
        _recents.value = prefs.getString(RECENTS, null).orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun setLanguage(context: Context, language: String) {
        _language.value = language
        prefs(context).edit().apply {
            // English is the default, so choosing it clears the setting (as on the website)
            if (language.equals(ENGLISH, ignoreCase = true)) remove(LANGUAGE) else putString(LANGUAGE, language)
        }.apply()
    }

    fun addRecent(context: Context, query: String) {
        val list = withRecent(_recents.value, query.replace(SEPARATOR, " "))
        if (list == _recents.value) return
        _recents.value = list
        prefs(context).edit().putString(RECENTS, list.joinToString(SEPARATOR)).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
