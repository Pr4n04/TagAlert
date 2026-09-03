package com.tagalert.data.repository

import com.tagalert.data.local.LocationHistoryDao
import com.tagalert.data.local.TrackedDeviceDao
import com.tagalert.data.local.UserPreferences
import com.tagalert.data.model.LocationHistory
import com.tagalert.data.model.TrackedDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val deviceDao: TrackedDeviceDao,
    private val historyDao: LocationHistoryDao,
    val preferences: UserPreferences
) {

    // --- Device Operations ---

    fun getAllDevices(): Flow<List<TrackedDevice>> = deviceDao.getAllDevices()

    fun getDevice(deviceId: String): Flow<TrackedDevice?> = deviceDao.getDevice(deviceId)

    suspend fun getDeviceOnce(deviceId: String): TrackedDevice? = deviceDao.getDeviceOnce(deviceId)

    suspend fun upsertDevice(device: TrackedDevice) = deviceDao.upsertDevice(device)

    suspend fun updateDeviceLastSeen(
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        locationName: String? = null
    ) {
        val device = deviceDao.getDeviceOnce(deviceId) ?: return
        deviceDao.updateDevice(
            device.copy(
                lastSeenTimestamp = System.currentTimeMillis(),
                lastSeenLatitude = latitude,
                lastSeenLongitude = longitude,
                lastSeenAccuracy = accuracy,
                lastSeenLocationName = locationName,
                isOnline = true
            )
        )
    }

    suspend fun markDeviceOffline(deviceId: String) {
        val device = deviceDao.getDeviceOnce(deviceId) ?: return
        deviceDao.updateDevice(device.copy(isOnline = false))
    }

    // --- History Operations ---

    fun getHistoryForDevice(deviceId: String): Flow<List<LocationHistory>> =
        historyDao.getHistoryForDevice(deviceId)

    fun getRecentHistory(): Flow<List<LocationHistory>> = historyDao.getRecentHistory()

    fun getLeftBehindEvents(deviceId: String): Flow<List<LocationHistory>> =
        historyDao.getLeftBehindEvents(deviceId)

    suspend fun logLocation(entry: LocationHistory): Long = historyDao.insertEntry(entry)

    suspend fun markAsLeftBehind(entryId: Long) {
        // This would need a proper implementation with a suspend function
        // For now we handle this through the insert with wasLeftBehind = true
    }

    suspend fun pruneOldEntries(olderThanDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        historyDao.pruneOldEntries(cutoff)
    }
}
