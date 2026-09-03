package com.tagalert.data.local

import androidx.room.*
import com.tagalert.data.model.LocationHistory
import com.tagalert.data.model.TrackedDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedDeviceDao {

    @Query("SELECT * FROM tracked_devices")
    fun getAllDevices(): Flow<List<TrackedDevice>>

    @Query("SELECT * FROM tracked_devices WHERE id = :deviceId")
    fun getDevice(deviceId: String): Flow<TrackedDevice?>

    @Query("SELECT * FROM tracked_devices WHERE id = :deviceId")
    suspend fun getDeviceOnce(deviceId: String): TrackedDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: TrackedDevice)

    @Update
    suspend fun updateDevice(device: TrackedDevice)

    @Delete
    suspend fun deleteDevice(device: TrackedDevice)

    @Query("DELETE FROM tracked_devices WHERE id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
}

@Dao
interface LocationHistoryDao {

    @Query("SELECT * FROM location_history WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    fun getHistoryForDevice(deviceId: String, limit: Int = 100): Flow<List<LocationHistory>>

    @Query("SELECT * FROM location_history WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getAllHistoryForDevice(deviceId: String): Flow<List<LocationHistory>>

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<LocationHistory>>

    @Query("SELECT * FROM location_history WHERE deviceId = :deviceId AND wasLeftBehind = 1 ORDER BY timestamp DESC")
    fun getLeftBehindEvents(deviceId: String): Flow<List<LocationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LocationHistory): Long

    @Update
    suspend fun updateEntry(entry: LocationHistory)

    @Query("DELETE FROM location_history WHERE timestamp < :cutoffTime")
    suspend fun pruneOldEntries(cutoffTime: Long)

    @Query("DELETE FROM location_history")
    suspend fun clearAll()
}
