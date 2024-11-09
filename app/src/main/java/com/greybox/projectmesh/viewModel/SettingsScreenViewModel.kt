package com.greybox.projectmesh.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI

//class SettingsScreenViewModel(di: DI): ViewModel() {
//    private val _theme = MutableStateFlow(loadTheme())
//    val theme: Flow<AppTheme> = _theme.asStateFlow()
//
//    private fun loadTheme(): AppTheme {
//        val themeName = prefs.getString("app_theme", AppTheme.SYSTEM.name)
//        return AppTheme.valueOf(themeName!!)
//    }
//
//    fun saveTheme(theme: AppTheme) {
//        _theme.value = theme
//        viewModelScope.launch {
//            prefs.edit().putString("app_theme", theme.name).apply()
//        }
//    }
//}