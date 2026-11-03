package com.example.glassgallery.data

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun themeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun gridCellDp(): Int = prefs.getInt("cell", 112)

    fun saveGridCellDp(dp: Int) {
        prefs.edit().putInt("cell", dp).apply()
    }
}
