package com.ayyappasamaaj.tattvamasi.ui.publications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The texts saved on this phone (the reader's bookmark button), newest first, kept in the app's own settings: no
 * account, so they stay on this phone only. Later, with sign-in, they can move into a website playlist. As on the
 * iOS app (SavedTexts.swift).
 */
object SavedTexts {
    private const val PREFS = "publications"
    private const val SAVED = "saved-texts"

    private val _saved = MutableStateFlow<List<SavedText>>(emptyList())
    val saved: StateFlow<List<SavedText>> = _saved.asStateFlow()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        _saved.value = decodeSaved(prefs(context).getString(SAVED, null))
    }

    fun isSaved(slug: String, title: String) = _saved.value.any { it.slug == slug && it.title == title }

    /** Saves the text, or takes it off the list; true when it's now saved. */
    fun toggle(context: Context, slug: String, title: String): Boolean {
        load(context)
        val list = toggledSaved(_saved.value, slug, title, System.currentTimeMillis())
        _saved.value = list
        prefs(context).edit().putString(SAVED, encodeSaved(list)).apply()
        return isSaved(slug, title)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
