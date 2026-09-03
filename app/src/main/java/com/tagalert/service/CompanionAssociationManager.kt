package com.tagalert.service

import android.app.Activity
import android.annotation.TargetApi
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.IntentSender
import android.util.Log
import com.tagalert.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manages the companion device association with a BLE tracker.
 *
 * NOTE: The Ugreen Finder Pro (CM916) is a Find Hub certified tracker that uses
 * Google's proprietary Find Hub protocol. It does NOT appear in the standard
 * companion device association dialog because it doesn't advertise as a standard
 * BLE peripheral.
 *
 * This class provides two paths:
 * 1. Standard BLE companion association (for non-Find Hub trackers)
 * 2. Manual device registration (for Find Hub trackers)
 *
 * For Find Hub trackers, the user should:
 * 1. Pair the tracker via the Find Hub app
 * 2. Enter the tracker's name/ID manually in TagAlert
 */
@TargetApi(36)
class CompanionAssociationManager(
    private val activity: Activity,
    private val preferences: UserPreferences
) {

    /**
     * Try to associate with a standard BLE device via the companion device API.
     * Returns true if association was initiated, false if no devices found.
     *
     * NOTE: This will NOT find Find Hub trackers like the Ugreen Finder Pro.
     * Use registerManualDevice() for Find Hub trackers instead.
     */
    fun tryAssociate(): Boolean {
        var initiated = false
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val manager = activity.getSystemService(CompanionDeviceManager::class.java)
            if (manager == null) {
                Log.w(TAG, "CompanionDeviceManager not available")
                return@launch
            }

            // Already associated? Nothing to do.
            val existing = manager.getMyAssociations()
            if (existing.isNotEmpty()) {
                Log.d(TAG, "Already associated with ${existing.size} device(s)")
                existing.firstOrNull()?.let { association ->
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        establishIdentity(association)
                    }
                }
                return@launch
            }

            // Try standard BLE association (may not find Find Hub trackers)
            Log.d(TAG, "Attempting standard BLE companion association")

            val filter = BluetoothDeviceFilter.Builder().build()

            val request = AssociationRequest.Builder()
                .setSingleDevice(false)
                .addDeviceFilter(filter)
                .build()

            try {
                manager.associate(request, associationCallback(), null)
                initiated = true
            } catch (e: Exception) {
                Log.w(TAG, "Companion association failed: ${e.message}")
            }
        }
        return initiated
    }

    /**
     * Register a device manually by name/ID.
     * This is the primary path for Find Hub trackers like the Ugreen Finder Pro.
     */
    suspend fun registerManualDevice(name: String, id: String) {
        preferences.setDeviceId(id)
        preferences.setDeviceName(name)
        Log.d(TAG, "Registered device manually: $name ($id)")
    }

    /**
     * Build the callback that handles the association lifecycle.
     */
    private fun associationCallback(): CompanionDeviceManager.Callback {
        return object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                Log.d(TAG, "Association pending — launching confirmation UI")
                try {
                    activity.startIntentSenderForResult(intentSender, -1, null, 0, 0, 0)
                } catch (e: IntentSender.SendIntentException) {
                    Log.w(TAG, "Failed to launch association UI", e)
                }
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                Log.d(TAG, "Association created: ${associationInfo.getDisplayName()}")
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    establishIdentity(associationInfo)
                }
            }

            override fun onFailure(error: CharSequence?) {
                Log.w(TAG, "Association failed: $error")
            }
        }
    }

    /**
     * Persist the device identity from the association.
     */
    private suspend fun establishIdentity(association: AssociationInfo) {
        val mac = association.getDeviceMacAddress()?.toString()
        val deviceId = mac ?: "association-${association.getId()}"
        val deviceName = association.getDisplayName()?.toString() ?: "My Keys"

        preferences.setDeviceId(deviceId)
        preferences.setDeviceName(deviceName)
        Log.d(TAG, "Established device identity: $deviceName ($deviceId)")
    }

    companion object {
        const val TAG = "CompanionAssociationManager"
    }
}