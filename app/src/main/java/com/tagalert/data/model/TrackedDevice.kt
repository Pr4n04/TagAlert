package com.tagalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a tracked BLE device (your Ugreen Finder Pro).
 */
@Entity(tableName = "tracked_devices")
data class TrackedDevice(
    @PrimaryKey val id: String,
    val name: String,
    val bleAddress: String? = null,
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenLatitude: Double? = null,
    val lastSeenLongitude: Double? = null,
    val lastSeenAccuracy: Float? = null,
    val lastSeenLocationName: String? = null,
    val batteryLevel: Int? = null,
    val companionAssociationId: Int? = null
)
