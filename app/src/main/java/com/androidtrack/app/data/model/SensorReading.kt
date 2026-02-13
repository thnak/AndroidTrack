package com.androidtrack.app.data.model

/**
 * Data class representing a sensor reading
 */
data class SensorReading(
    val sensorType: String,
    val sensorName: String,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorReading

        if (sensorType != other.sensorType) return false
        if (sensorName != other.sensorName) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensorType.hashCode()
        result = 31 * result + sensorName.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
