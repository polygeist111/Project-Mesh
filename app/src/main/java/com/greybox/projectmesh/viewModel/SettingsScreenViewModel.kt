package com.greybox.projectmesh.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.helper.ThemePreferences
import com.greybox.projectmesh.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class SettingsScreenViewModel(di: DI): ViewModel() {
    private val themePreferences: ThemePreferences by di.instance()

    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    val theme: StateFlow<AppTheme> = _theme

    init {
        viewModelScope.launch {
            _theme.value = themePreferences.loadTheme()
        }
    }

    fun saveTheme(theme: AppTheme) {
        _theme.value = theme
        viewModelScope.launch {
            themePreferences.saveTheme(theme)
        }
    }
}