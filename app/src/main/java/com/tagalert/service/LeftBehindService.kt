package com.tagalert.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tagalert.R
import com.tagalert.TagAlertApp
import com.tagalert.data.local.UserPreferences
import com.tagalert.data.model.DevicePresenceState
import com.tagalert.data.model.LeftBehindEvent
import com.tagalert.data.model.LocationHistory
import com.tagalert.data.model.TrackedDevice
import com.tagalert.data.repository.TagRepository
import com.tagalert.ui.MainActivity
import com.tagalert.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Core service that performs BLE scanning to detect tracker presence.
 * Acts as a fallback when CompanionDeviceManager doesn't trigger.
 *
 * Strategy:
 *  - Scans every 30 seconds (low power) for the tracker
 *  - If tracker seen → update last-seen timestamp, cancel any countdown
 *  - If tracker NOT seen for countdown_seconds → trigger left-behind alert
 */
@AndroidEntryPoint
class LeftBehindService : Service() {

    @Inject lateinit var repository: TagRepository
    @Inject lateinit var locationHelper: LocationHelper

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var scanPendingIntent: PendingIntent? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Device tracking state
    private var countdownJob: Job? = null
    private var currentEvent: LeftBehindEvent? = null
    private var isDeviceInRange = false
    private var lastScanTime = 0L

    // Scan interval
    private val SCAN_INTERVAL_MS = 30_000L // 30 seconds

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF — ensuring scan continues")
                    scheduleNextScan()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON")
                    scheduleNextScan()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        registerScreenReceiver()
        startForeground(NOTIFICATION_ID, createServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCANNING -> startScanning()
            ACTION_STOP_SCANNING -> stopScanning()
            ACTION_TRACKER_IN_RANGE -> onTrackerInRange(intent)
            ACTION_TRACKER_OUT_OF_RANGE -> {
                val countdown = intent.getIntExtra(EXTRA_COUNTDOWN_SECONDS, 90)
                onTrackerOutOfRange(countdown)
            }
            ACTION_ACKNOWLEDGE -> acknowledgeAlert()
            ACTION_CANCEL_COUNTDOWN -> cancelCountdown()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopScanning()
        unregisterScreenReceiver()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- BLE Scanning ---

    private fun startScanning() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or not enabled")
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Failed to get BluetoothLeScanner")
            return
        }

        // Start callback-based scan
        startCallbackScan()

        // Also schedule periodic scans
        scheduleNextScan()

        Log.d(TAG, "BLE scanning started")
    }

    private fun startCallbackScan() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code: $errorCode")
                // Retry after delay
                serviceScope.launch {
                    delay(5_000)
                    startCallbackScan()
                }
            }
        }

        // Scan for all BLE devices and filter in the callback. The Ugreen Finder
        // Pro uses a rotating MAC and may advertise under slightly different names,
        // so an OS-level name filter can miss it.
        val filter = ScanFilter.Builder().build()

        // Explicitly check BLUETOOTH_SCAN permission before scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted")
            return
        }

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting scan: ${e.message}")
        }
    }

    private fun handleScanResult(result: ScanResult) {
        // Accessing scan results requires BLUETOOTH_SCAN permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val deviceName = result.device.name ?: ""
        // Match the Ugreen Finder Pro by name (case-insensitive). Also log every
        // scan result so we can diagnose if the device isn't being seen.
        Log.d(TAG, "BLE scan result: name='$deviceName' addr=${result.device.address}")
        if (!deviceName.contains("UGREEN", ignoreCase = true)) return

        lastScanTime = System.currentTimeMillis()

        // Establish the device ID from the scan result's MAC address so the
        // BLE scan fallback path works even before the companion service runs.
        val mac = result.device.address
        if (mac != null && mac.isNotEmpty()) {
            serviceScope.launch {
                repository.preferences.setDeviceId(mac)
                if (repository.preferences.deviceName.first().isEmpty()) {
                    repository.preferences.setDeviceName(deviceName)
                }
            }
        }

        onTrackerInRange(deviceId = mac)
    }

    private fun scheduleNextScan() {
        serviceScope.launch {
            delay(SCAN_INTERVAL_MS)

            // Check if we've seen the device recently
            val timeSinceLastSeen = System.currentTimeMillis() - lastScanTime
            val countdown = repository.preferences.countdownSeconds.first() * 1000L

            if (isDeviceInRange && timeSinceLastSeen > SCAN_INTERVAL_MS * 2) {
                // Device hasn't been seen in a while — might be out of range
                isDeviceInRange = false
                onTrackerOutOfRange(countdown.toInt())
            }
        }
    }

    // --- Presence State Management ---

    private fun onTrackerInRange(intent: Intent? = null, deviceId: String? = null) {
        val wasOutOfRange = !isDeviceInRange
        isDeviceInRange = true
        lastScanTime = System.currentTimeMillis()

        // Cancel any active countdown
        cancelCountdown()

        serviceScope.launch {
            // Use the explicitly passed device ID (from BLE scan), then the one
            // passed by the companion service via intent, then the stored preference.
            val intentDeviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)
            val resolvedDeviceId = deviceId
                ?: (if (intentDeviceId != null && intentDeviceId.isNotEmpty()) intentDeviceId else null)
                ?: repository.preferences.deviceId.first()
            if (resolvedDeviceId.isEmpty()) return@launch

            // Persist the device as online so the UI reflects "With you"
            val location = locationHelper.getCurrentLocation()
            repository.updateDeviceLastSeen(
                resolvedDeviceId,
                location?.latitude ?: 0.0,
                location?.longitude ?: 0.0,
                location?.accuracy ?: 0f
            )

            if (wasOutOfRange && currentEvent?.state == DevicePresenceState.LEFT_BEHIND) {
                // Device recovered! Log it
                repository.logLocation(
                    LocationHistory(
                        deviceId = resolvedDeviceId,
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0,
                        accuracy = location?.accuracy ?: 0f,
                        wasRecovered = true,
                        recoveryTimestamp = System.currentTimeMillis()
                    )
                )
                currentEvent = null
            }
        }
    }

    private fun onTrackerOutOfRange(countdownSeconds: Int) {
        if (isDeviceInRange) return // Already in range, ignore

        if (currentEvent?.state == DevicePresenceState.COUNTDOWN) {
            // Already counting down
            return
        }

        // Start countdown
        isDeviceInRange = false
        val now = System.currentTimeMillis()

        serviceScope.launch {
            val deviceId = repository.preferences.deviceId.first()
            val deviceName = repository.preferences.deviceName.first()
            val location = locationHelper.getCurrentLocation()

            // Mark the device offline so the UI reflects the lost state
            if (deviceId.isNotEmpty()) {
                repository.markDeviceOffline(deviceId)
            }

            currentEvent = LeftBehindEvent(
                deviceId = deviceId,
                deviceName = deviceName,
                lostTimestamp = now,
                lostLatitude = location?.latitude,
                lostLongitude = location?.longitude,
                lostLocationName = null,
                state = DevicePresenceState.COUNTDOWN,
                countdownSeconds = countdownSeconds
            )

            // Log the potential left-behind event
            repository.logLocation(
                LocationHistory(
                    deviceId = deviceId,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    accuracy = location?.accuracy ?: 0f,
                    wasLeftBehind = true
                )
            )

            // Start countdown
            countdownJob = launch {
                Log.d(TAG, "Countdown started: ${countdownSeconds}s")
                delay(countdownSeconds * 1000L)

                // Check if device came back during countdown
                if (!isDeviceInRange) {
                    triggerLeftBehindAlert()
                }
            }
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (currentEvent?.state == DevicePresenceState.COUNTDOWN) {
            currentEvent = currentEvent?.copy(state = DevicePresenceState.IN_RANGE)
        }
    }

    private fun triggerLeftBehindAlert() {
        val event = currentEvent ?: return
        currentEvent = event.copy(state = DevicePresenceState.LEFT_BEHIND)

        serviceScope.launch {
            val vibration = repository.preferences.vibrationEnabled.first()
            val sound = repository.preferences.soundEnabled.first()
            sendLeftBehindNotification(event, vibration, sound)
        }
        Log.d(TAG, "LEFT BEHIND ALERT: ${event.deviceName}")
    }

    private fun acknowledgeAlert() {
        currentEvent = currentEvent?.copy(state = DevicePresenceState.ACKNOWLEDGED)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_LEFT_BEHIND)
    }

    // --- Notifications ---

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TagAlertApp.CHANNEL_SERVICE)
            .setContentTitle("TagAlert Active")
            .setContentText("Monitoring your tracker")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun sendLeftBehindNotification(event: LeftBehindEvent, vibrate: Boolean, sound: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_left_behind", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "I'm Back" action
        val backIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_I_AM_BACK
        }
        val backPendingIntent = PendingIntent.getBroadcast(
            this, 1, backIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, TagAlertApp.CHANNEL_LEFT_BEHIND)
            .setContentTitle("You left your ${event.deviceName} behind!")
            .setContentText("Last seen ${formatTimeSince(event.lostTimestamp)} ago")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, "I'm Back", backPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (vibrate) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300, 200, 500))
        }
        if (sound) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_LEFT_BEHIND, builder.build())
    }

    private fun formatTimeSince(timestamp: Long): String {
        val seconds = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    // --- Lifecycle Helpers ---

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TagAlert::BLEScanWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
    }

    private fun stopScanning() {
        // Stopping a scan requires BLUETOOTH_SCAN permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            scanCallback = null
            countdownJob?.cancel()
            return
        }
        scanCallback?.let { callback ->
            try {
                scanner?.stopScan(callback)
            } catch (_: Exception) {}
        }
        scanCallback = null
        countdownJob?.cancel()
    }

    companion object {
        const val TAG = "LeftBehindService"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_LEFT_BEHIND = 2001

        const val ACTION_START_SCANNING = "com.tagalert.START_SCANNING"
        const val ACTION_STOP_SCANNING = "com.tagalert.STOP_SCANNING"
        const val ACTION_TRACKER_IN_RANGE = "com.tagalert.TRACKER_IN_RANGE"
        const val ACTION_TRACKER_OUT_OF_RANGE = "com.tagalert.TRACKER_OUT_OF_RANGE"
        const val ACTION_ACKNOWLEDGE = "com.tagalert.ACKNOWLEDGE"
        const val ACTION_CANCEL_COUNTDOWN = "com.tagalert.CANCEL_COUNTDOWN"
        const val ACTION_I_AM_BACK = "com.tagalert.I_AM_BACK"

        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}
