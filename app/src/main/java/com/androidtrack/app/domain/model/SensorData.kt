package com.androidtrack.app.domain.model

/**
 * Domain model for sensor data display
 */
data class SensorData(
    val type: String,
    val name: String,
    val values: List<Float>,
    val timestamp: Long
) {
    fun valuesAsString(): String = values.joinToString(", ") { "%.2f".format(it) }
}
