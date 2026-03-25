package com.androidtrack.app.di

import android.content.Context
import androidx.room.Room
import com.androidtrack.app.data.local.dao.BrokerConfigDao
import com.androidtrack.app.data.local.dao.DeviceConfigDao
import com.androidtrack.app.data.local.dao.DiPinDao
import com.androidtrack.app.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "androidtrack.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBrokerConfigDao(db: AppDatabase): BrokerConfigDao = db.brokerConfigDao()

    @Provides
    @Singleton
    fun provideDeviceConfigDao(db: AppDatabase): DeviceConfigDao = db.deviceConfigDao()

    @Provides
    @Singleton
    fun provideDiPinDao(db: AppDatabase): DiPinDao = db.diPinDao()
}
