package com.bihstudio.uvindex.presentation.screens.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.domain.model.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage

    fun selectLanguage(lang: AppLanguage) {
        _selectedLanguage.value = lang
    }

    fun saveLanguage() {
        viewModelScope.launch {
            preferencesManager.setLanguage(_selectedLanguage.value.code)
        }
    }
}
