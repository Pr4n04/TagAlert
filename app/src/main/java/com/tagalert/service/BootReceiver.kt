package com.tagalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Restarts the tracking services after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d(TAG, "Boot/Update received, checking if tracking should resume")

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val trackingEnabled = preferences.trackingEnabled.first()
                if (trackingEnabled) {
                    Log.d(TAG, "Tracking was enabled — restarting services")
                    val serviceIntent = Intent(context, LeftBehindService::class.java).apply {
                        action = LeftBehindService.ACTION_START_SCANNING
                    }
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }

    companion object {
        const val TAG = "BootReceiver"
    }
}
