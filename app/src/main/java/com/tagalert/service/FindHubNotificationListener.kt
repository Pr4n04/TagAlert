package com.tagalert.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
 * Listens for Find Hub notifications to detect left-behind events.
 *
 * The Ugreen Finder Pro (CM916) is a Find Hub certified tracker. It uses
 * Google's Find Hub protocol, not standard BLE. The only way to detect
 * left-behind events is to monitor Find Hub's notifications.
 *
 * Find Hub sends notifications like:
 * - "You left [device] behind"
 * - "[device] is no longer with you"
 * - "Lost connection with [device]"
 *
 * This service detects these notifications and triggers the left-behind
 * countdown/alert in TagAlert.
 */
@AndroidEntryPoint
class FindHubNotificationListener : NotificationListenerService() {

    @Inject lateinit var preferences: UserPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        // Check if this is a Find Hub notification
        if (sbn.packageName != FIND_HUB_PACKAGE) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        Log.d(TAG, "Find Hub notification: title='$title' text='$text'")

        // Detect left-behind notifications
        val isLeftBehind = LEFT_BEHIND_PATTERNS.any { pattern ->
            title.contains(pattern, ignoreCase = true) ||
            text.contains(pattern, ignoreCase = true)
        }

        if (isLeftBehind) {
            Log.d(TAG, "Detected left-behind notification from Find Hub")
            handleLeftBehindNotification(title, text)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for left-behind detection
    }

    private fun handleLeftBehindNotification(title: String, text: String) {
        serviceScope.launch {
            // Extract device name from notification
            val deviceName = extractDeviceName(title, text)

            // Use the device name from preferences if available, otherwise use extracted name
            val resolvedName = if (deviceName.isNotEmpty()) {
                preferences.setDeviceName(deviceName)
                deviceName
            } else {
                preferences.deviceName.first()
            }

            // Ensure we have a device ID
            val deviceId = preferences.deviceId.first()
            if (deviceId.isEmpty()) {
                // No device registered yet — register with a placeholder
                preferences.setDeviceId("findhub-${System.currentTimeMillis()}")
                preferences.setDeviceName(resolvedName)
            }

            // Trigger the left-behind service
            val countdown = preferences.countdownSeconds.first()
            val intent = Intent(this@FindHubNotificationListener, LeftBehindService::class.java).apply {
                action = LeftBehindService.ACTION_TRACKER_OUT_OF_RANGE
                putExtra(LeftBehindService.EXTRA_COUNTDOWN_SECONDS, countdown)
            }
            startForegroundService(intent)

            Log.d(TAG, "Triggered left-behind countdown from Find Hub notification")
        }
    }

    private fun extractDeviceName(title: String, text: String): String {
        // Try to extract device name from notification text
        // Common patterns: "You left My Keys behind", "My Keys is no longer with you"
        val patterns = listOf(
            Regex("You left (.+?) behind", RegexOption.IGNORE_CASE),
            Regex("(.+?) is no longer", RegexOption.IGNORE_CASE),
            Regex("Lost connection with (.+)", RegexOption.IGNORE_CASE),
            Regex("(.+?) disconnected", RegexOption.IGNORE_CASE),
        )

        for (pattern in patterns) {
            val match = pattern.find(title) ?: pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        return ""
    }

    companion object {
        const val TAG = "FindHubNotificationListener"
        const val FIND_HUB_PACKAGE = "com.google.android.apps.adm"

        val LEFT_BEHIND_PATTERNS = listOf(
            "left",
            "behind",
            "no longer with you",
            "lost connection",
            "disconnected",
            "not with you",
            "separated",
        )
    }
}