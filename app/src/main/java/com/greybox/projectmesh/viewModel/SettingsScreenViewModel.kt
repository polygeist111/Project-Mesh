package com.greybox.projectmesh.viewModel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.greybox.projectmesh.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kodein.di.DI
import org.kodein.di.instance
import java.util.Locale

class SettingsScreenViewModel(di: DI): ViewModel() {
    // inject SharedPreferences
    private val settingPrefs: SharedPreferences by di.instance(tag = "settings")

    // Theme State
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme

    // Language State
    private val _lang = MutableStateFlow(loadLang())
    val lang: StateFlow<String> = _lang

    init {
        // Load the theme from SharedPreferences
        _theme.value = loadTheme()
        _lang.value = loadLang()
    }

    // Load theme from SharedPreferences
    private fun loadTheme(): AppTheme {
        val themeName = settingPrefs.getString(THEME_KEY, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return AppTheme.valueOf(themeName)
    }

    // Store Selected Theme to SharedPreferences
    fun saveTheme(theme: AppTheme) {
        _theme.value = theme
        settingPrefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    // Load Language from SharedPreferences
    private fun loadLang(): String {
        return settingPrefs.getString(LANGUAGE_KEY, "System") ?: "System"
    }

    // Store Selected Language to SharedPreferences
    fun saveLang(languageCode: String) {
        _lang.value = languageCode
        settingPrefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    fun getUpdatedLocaleContext(context: Context): Context {
        val locale = Locale(_lang.value)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    companion object{
        private const val THEME_KEY = "app_theme"
        private const val LANGUAGE_KEY = "language"
    }
}