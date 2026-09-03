package com.tagalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagalert.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    val countdownSeconds: StateFlow<Int> = preferences.countdownSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    val vibrationEnabled: StateFlow<Boolean> = preferences.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = preferences.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val deviceName: StateFlow<String> = preferences.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "My Keys")

    val safeZonesEnabled: StateFlow<Boolean> = preferences.safeZonesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setCountdownSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setCountdownSeconds(seconds) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setVibrationEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch { preferences.setDeviceName(name) }
    }

    fun setSafeZonesEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSafeZonesEnabled(enabled) }
    }
}
