package com.tagalert.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tagalert.data.model.LocationHistory
import com.tagalert.data.model.TrackedDevice

@Database(
    entities = [
        TrackedDevice::class,
        LocationHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TagAlertDatabase : RoomDatabase() {

    abstract fun trackedDeviceDao(): TrackedDeviceDao
    abstract fun locationHistoryDao(): LocationHistoryDao

    companion object {
        const val DB_NAME = "tagalert.db"
    }
}
