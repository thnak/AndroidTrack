package com.androidtrack.app.domain.usecase

import android.hardware.Sensor
import com.androidtrack.app.data.model.SensorReading
import com.androidtrack.app.data.repository.SensorRepository
import com.androidtrack.app.domain.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for observing sensor data
 */
class ObserveSensorDataUseCase @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    operator fun invoke(): Flow<SensorData> {
        val sensors = sensorRepository.getAvailableSensors()
        return sensorRepository.observeSensorReadings(sensors)
            .map { it.toDomainModel() }
    }

    fun getAvailableSensors(): List<Sensor> {
        return sensorRepository.getAvailableSensors()
    }

    private fun SensorReading.toDomainModel() = SensorData(
        type = sensorType,
        name = sensorName,
        values = values.toList(),
        timestamp = timestamp
    )
}
