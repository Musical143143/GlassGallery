package com.example.glassgallery.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun save(keys: Set<String>) {
        prefs.edit().putStringSet(KEY, keys.toSet()).apply()
    }

    private companion object {
        const val KEY = "keys"
    }
}
