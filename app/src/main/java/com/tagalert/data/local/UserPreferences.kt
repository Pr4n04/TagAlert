package com.tagalert.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tagalert_settings")

/**
 * User preferences for the TagAlert app.
 */
class UserPreferences(private val context: Context) {

    // Keys
    companion object {
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SAFE_ZONES_ENABLED = booleanPreferencesKey("safe_zones_enabled")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
    }

    // Countdown
    val countdownSeconds: Flow<Int> = context.dataStore.data.map { it[COUNTDOWN_SECONDS] ?: 90 }
    suspend fun setCountdownSeconds(seconds: Int) {
        context.dataStore.edit { it[COUNTDOWN_SECONDS] = seconds }
    }

    // Vibration
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    // Sound
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ENABLED] ?: true }
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    // Safe Zones
    val safeZonesEnabled: Flow<Boolean> = context.dataStore.data.map { it[SAFE_ZONES_ENABLED] ?: false }
    suspend fun setSafeZonesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SAFE_ZONES_ENABLED] = enabled }
    }

    // Tracking active
    val trackingEnabled: Flow<Boolean> = context.dataStore.data.map { it[TRACKING_ENABLED] ?: false }
    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TRACKING_ENABLED] = enabled }
    }

    // Device info
    val deviceName: Flow<String> = context.dataStore.data.map { it[DEVICE_NAME] ?: "My Keys" }
    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { it[DEVICE_NAME] = name }
    }

    val deviceId: Flow<String> = context.dataStore.data.map { it[DEVICE_ID] ?: "" }
    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { it[DEVICE_ID] = id }
    }

    // Theme
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
