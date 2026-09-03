package com.tagalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A record of when and where a device was last seen.
 * This forms the "last seen log" history.
 */
@Entity(tableName = "location_history")
data class LocationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val locationName: String? = null,
    val wasLeftBehind: Boolean = false,
    val wasRecovered: Boolean = false,
    val recoveryTimestamp: Long? = null
)
