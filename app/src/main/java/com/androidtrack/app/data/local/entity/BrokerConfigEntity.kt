package com.androidtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the persisted MQTT broker configuration.
 * Only a single row (id = 1) is used.
 */
@Entity(tableName = "broker_config")
data class BrokerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val clientId: String,
    val secure: Boolean
)
