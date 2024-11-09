package com.greybox.projectmesh.helper

import android.content.Context
import android.content.SharedPreferences
import com.greybox.projectmesh.ui.theme.AppTheme

class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTheme(): AppTheme {
        val themeName = prefs.getString(THEME_KEY, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return AppTheme.valueOf(themeName)
    }

    fun saveTheme(theme: AppTheme) {
        prefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val THEME_KEY = "app_theme"
    }
}
