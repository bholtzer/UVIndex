package com.bihstudio.uvindex.presentation.screens.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.service.scheduleUVChecks
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun onLocationGranted() {
        viewModelScope.launch {
            preferencesManager.setLocationGranted(true)
        }
    }

    fun onNotificationGranted() {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(true)
            preferencesManager.setFirstLaunch(false)
            scheduleUVChecks(context)
        }
    }

    fun onPermissionsHandled() {
        viewModelScope.launch {
            preferencesManager.setFirstLaunch(false)
        }
    }
}
