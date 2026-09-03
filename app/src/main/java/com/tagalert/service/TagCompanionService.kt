package com.tagalert.service

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.DevicePresenceEvent
import android.companion.CompanionDeviceService
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Companion Device Service — the primary, most battery-efficient layer.
 *
 * The OS automatically binds/unbinds this service when the companion device
 * (your Ugreen Finder Pro) enters/leaves BLE range. No scanning needed.
 *
 * On Android 16+ (API 36): uses onDevicePresenceEvent()
 * On older: uses onDeviceAppeared() / onDeviceDisappeared()
 */
class TagCompanionService : CompanionDeviceService() {

    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        Log.d(TAG, "Device presence event: ${event.eventType}")

        when (event.eventType) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> {
                Log.d(TAG, "Tracker IN RANGE (CompanionDevice)")
                notifyLeftBehindService(LeftBehindService.ACTION_TRACKER_IN_RANGE)
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> {
                Log.d(TAG, "Tracker OUT OF RANGE (CompanionDevice)")
                val countdown = getPrefs().getInt("countdown_seconds", 90)
                val intent = Intent(this, LeftBehindService::class.java).apply {
                    action = LeftBehindService.ACTION_TRACKER_OUT_OF_RANGE
                    putExtra(LeftBehindService.EXTRA_COUNTDOWN_SECONDS, countdown)
                }
                startForegroundService(intent)
            }
            DevicePresenceEvent.EVENT_BT_CONNECTED -> {
                Log.d(TAG, "Tracker BT CONNECTED")
                notifyLeftBehindService(LeftBehindService.ACTION_TRACKER_IN_RANGE)
            }
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> {
                Log.d(TAG, "Tracker BT DISCONNECTED")
                val countdown = getPrefs().getInt("countdown_seconds", 90)
                val intent = Intent(this, LeftBehindService::class.java).apply {
                    action = LeftBehindService.ACTION_TRACKER_OUT_OF_RANGE
                    putExtra(LeftBehindService.EXTRA_COUNTDOWN_SECONDS, countdown)
                }
                startForegroundService(intent)
            }
            else -> {
                Log.d(TAG, "Other event: ${event.eventType}")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        Log.d(TAG, "Tracker appeared (legacy callback)")
        notifyLeftBehindService(LeftBehindService.ACTION_TRACKER_IN_RANGE)
    }

    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        Log.d(TAG, "Tracker disappeared (legacy callback)")
        val countdown = getPrefs().getInt("countdown_seconds", 90)
        val intent = Intent(this, LeftBehindService::class.java).apply {
            action = LeftBehindService.ACTION_TRACKER_OUT_OF_RANGE
            putExtra(LeftBehindService.EXTRA_COUNTDOWN_SECONDS, countdown)
        }
        startForegroundService(intent)
    }

    private fun notifyLeftBehindService(action: String) {
        val intent = Intent(this, LeftBehindService::class.java).apply {
            this.action = action
        }
        startForegroundService(intent)
    }

    private fun getPrefs() = getSharedPreferences("tagalert_settings", MODE_PRIVATE)

    companion object {
        const val TAG = "TagCompanionService"
    }
}
