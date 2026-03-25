package com.androidtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a virtual Digital Input (DI) pin.
 * Mode is stored as a String ("MANUAL" or "AUTO") for Room compatibility.
 */
@Entity(tableName = "di_pin")
data class DiPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pinNumber: String,
    val mode: String,
    val timerMs: Long,
    val shootCount: Long,
    val pulseTime: Long
)
