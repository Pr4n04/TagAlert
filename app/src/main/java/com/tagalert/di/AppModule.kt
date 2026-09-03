package com.tagalert.di

import android.content.Context
import androidx.room.Room
import com.tagalert.data.local.LocationHistoryDao
import com.tagalert.data.local.TagAlertDatabase
import com.tagalert.data.local.TrackedDeviceDao
import com.tagalert.data.local.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TagAlertDatabase {
        return Room.databaseBuilder(
            context,
            TagAlertDatabase::class.java,
            TagAlertDatabase.DB_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTrackedDeviceDao(db: TagAlertDatabase): TrackedDeviceDao = db.trackedDeviceDao()

    @Provides
    fun provideLocationHistoryDao(db: TagAlertDatabase): LocationHistoryDao = db.locationHistoryDao()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }
}
