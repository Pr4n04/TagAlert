package com.tagalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles notification actions (e.g., "I'm Back" button).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            LeftBehindService.ACTION_I_AM_BACK -> {
                Log.d(TAG, "User acknowledged: I'm Back")
                val serviceIntent = Intent(context, LeftBehindService::class.java).apply {
                    action = LeftBehindService.ACTION_ACKNOWLEDGE
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    companion object {
        const val TAG = "NotificationActionReceiver"
    }
}
