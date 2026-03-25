package com.androidtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the persisted device configuration.
 * Only a single row (id = 1) is used.
 */
@Entity(tableName = "device_config")
data class DeviceConfigEntity(
    @PrimaryKey val id: Int = 1,
    val deviceId: String,
    val deviceType: String
)
