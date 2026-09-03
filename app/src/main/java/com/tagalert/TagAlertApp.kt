package com.tagalert

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TagAlertApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Left Behind Alert — high priority, heads-up
        val leftBehindChannel = NotificationChannel(
            CHANNEL_LEFT_BEHIND,
            "Left Behind Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you leave your tracker behind"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 500)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        // Background Service — low priority, silent
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active tracking service notification"
            setShowBadge(false)
        }

        manager.createNotificationChannel(leftBehindChannel)
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_LEFT_BEHIND = "left_behind_alerts"
        const val CHANNEL_SERVICE = "tracking_service"
    }
}
