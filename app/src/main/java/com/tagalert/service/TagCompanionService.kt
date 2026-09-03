package com.tagalert.service

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.companion.DevicePresenceEvent
import android.companion.CompanionDeviceService
import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tagalert.data.local.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Companion Device Service — the primary, most battery-efficient layer.
 *
 * The OS automatically binds/unbinds this service when the companion device
 * (your Ugreen Finder Pro) enters/leaves BLE range. No scanning needed.
 *
 * On Android 16+ (API 36): uses onDevicePresenceEvent()
 * On older: uses onDeviceAppeared() / onDeviceDisappeared()
 *
 * The companion device APIs require Android 16+ (API 36); this service is only
 * registered on devices that support the companion device feature.
 */
@TargetApi(36)
@AndroidEntryPoint
class TagCompanionService : CompanionDeviceService() {

    @Inject lateinit var preferences: UserPreferences

    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        Log.d(TAG, "Device presence event: ${event.getEvent()}")

        when (event.getEvent()) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> {
                Log.d(TAG, "Tracker IN RANGE (CompanionDevice)")
                notifyInRange(event.getAssociationId())
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> {
                Log.d(TAG, "Tracker OUT OF RANGE (CompanionDevice)")
                notifyOutOfRange()
            }
            DevicePresenceEvent.EVENT_BT_CONNECTED -> {
                Log.d(TAG, "Tracker BT CONNECTED")
                notifyInRange(event.getAssociationId())
            }
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> {
                Log.d(TAG, "Tracker BT DISCONNECTED")
                notifyOutOfRange()
            }
            else -> {
                Log.d(TAG, "Other event: ${event.getEvent()}")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        Log.d(TAG, "Tracker appeared (legacy callback)")
        notifyInRange(associationInfo.getId())
    }

    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        Log.d(TAG, "Tracker disappeared (legacy callback)")
        notifyOutOfRange()
    }

    /**
     * Establish a stable device ID/name from the companion association, then notify
     * LeftBehindService that the tracker is in range, passing the device ID along.
     */
    private fun notifyInRange(associationId: Int) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val deviceId = establishDeviceIdentity(associationId)
            val intent = Intent(this@TagCompanionService, LeftBehindService::class.java).apply {
                action = LeftBehindService.ACTION_TRACKER_IN_RANGE
                if (deviceId != null) {
                    putExtra(LeftBehindService.EXTRA_DEVICE_ID, deviceId)
                }
            }
            startForegroundService(intent)
        }
    }

    /**
     * Establish a stable device ID and name from the companion association so the
     * rest of the app (Dashboard, History, LeftBehindService) can track this device.
     *
     * @return the established device ID, or null if it couldn't be determined.
     */
    private suspend fun establishDeviceIdentity(associationId: Int): String? {
        if (associationId == DevicePresenceEvent.NO_ASSOCIATION) return null

        val manager = getSystemService(CompanionDeviceManager::class.java)
        val association = manager?.getMyAssociations()
            ?.find { it.getId() == associationId }
            ?: return null

        // Use the MAC address as a stable device ID, falling back to the
        // association ID if no MAC is available.
        val mac = association.getDeviceMacAddress()?.toString()
        val deviceId = mac ?: "association-${association.getId()}"
        val deviceName = association.getDisplayName()?.toString() ?: "My Keys"

        preferences.setDeviceId(deviceId)
        preferences.setDeviceName(deviceName)
        Log.d(TAG, "Established device identity: $deviceName ($deviceId)")
        return deviceId
    }

    private fun notifyOutOfRange() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val countdown = preferences.countdownSeconds.first()
            val intent = Intent(this@TagCompanionService, LeftBehindService::class.java).apply {
                action = LeftBehindService.ACTION_TRACKER_OUT_OF_RANGE
                putExtra(LeftBehindService.EXTRA_COUNTDOWN_SECONDS, countdown)
            }
            startForegroundService(intent)
        }
    }

    companion object {
        const val TAG = "TagCompanionService"
    }
}
