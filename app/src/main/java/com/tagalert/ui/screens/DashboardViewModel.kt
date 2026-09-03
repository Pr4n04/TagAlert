package com.tagalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagalert.data.local.UserPreferences
import com.tagalert.data.repository.TagRepository
import com.tagalert.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TagRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    val isTracking: StateFlow<Boolean> = preferences.trackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val deviceName: StateFlow<String> = preferences.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "My Keys")

    val countdownSeconds: Int
        get() = 90 // Default, observed via preferences

    val vibrationEnabled: Boolean
        get() = true // Observed via preferences

    private val _lastSeenText = MutableStateFlow("")
    val lastSeenText: StateFlow<String> = _lastSeenText.asStateFlow()

    private val _deviceState = MutableStateFlow(DeviceUiState.UNKNOWN)
    val deviceState: StateFlow<DeviceUiState> = _deviceState.asStateFlow()

    private val _lastSeenLocation = MutableStateFlow("")
    val lastSeenLocation: StateFlow<String> = _lastSeenLocation.asStateFlow()

    init {
        observeDeviceState()
    }

    private fun observeDeviceState() {
        viewModelScope.launch {
            preferences.deviceId.collectLatest { deviceId ->
                if (deviceId.isNotEmpty()) {
                    repository.getDevice(deviceId).collectLatest { device ->
                        if (device != null) {
                            _lastSeenText.value = TimeUtils.formatTimestampRelative(device.lastSeenTimestamp)
                            _deviceState.value = if (device.isOnline) {
                                DeviceUiState.IN_RANGE
                            } else {
                                DeviceUiState.OFFLINE
                            }
                            _lastSeenLocation.value = device.lastSeenLocationName ?: buildString {
                                device.lastSeenLatitude?.let { lat ->
                                    append(String.format("%.4f", lat))
                                    device.lastSeenLongitude?.let { lon ->
                                        append(", ${String.format("%.4f", lon)}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setTrackingEnabled(enabled)
        }
    }
}
