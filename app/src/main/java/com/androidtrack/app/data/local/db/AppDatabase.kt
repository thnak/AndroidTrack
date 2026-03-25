package com.androidtrack.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidtrack.app.data.local.dao.BrokerConfigDao
import com.androidtrack.app.data.local.dao.DeviceConfigDao
import com.androidtrack.app.data.local.dao.DiPinDao
import com.androidtrack.app.data.local.entity.BrokerConfigEntity
import com.androidtrack.app.data.local.entity.DeviceConfigEntity
import com.androidtrack.app.data.local.entity.DiPinEntity

@Database(
    entities = [
        BrokerConfigEntity::class,
        DeviceConfigEntity::class,
        DiPinEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerConfigDao(): BrokerConfigDao
    abstract fun deviceConfigDao(): DeviceConfigDao
    abstract fun diPinDao(): DiPinDao
}
