package com.androidtrack.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorReadingTest {

    @Test
    fun `SensorReading should store values correctly`() {
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val timestamp = System.currentTimeMillis()
        
        val reading = SensorReading(
            sensorType = "Accelerometer",
            sensorName = "Test Sensor",
            values = values,
            accuracy = 3,
            timestamp = timestamp
        )
        
        assertEquals("Accelerometer", reading.sensorType)
        assertEquals("Test Sensor", reading.sensorName)
        assertEquals(3, reading.accuracy)
        assertEquals(timestamp, reading.timestamp)
        assertEquals(1.0f, reading.values[0], 0.001f)
        assertEquals(2.0f, reading.values[1], 0.001f)
        assertEquals(3.0f, reading.values[2], 0.001f)
    }

    @Test
    fun `SensorReading equals should work correctly`() {
        val values1 = floatArrayOf(1.0f, 2.0f)
        val values2 = floatArrayOf(1.0f, 2.0f)
        val timestamp = 123456L
        
        val reading1 = SensorReading("Type", "Name", values1, 1, timestamp)
        val reading2 = SensorReading("Type", "Name", values2, 1, timestamp)
        
        assertEquals(reading1, reading2)
    }
}
