package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.MqttConfig
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.repository.MqttRepository
import com.androidtrack.app.domain.model.SensorData
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject

/**
 * Use case for publishing sensor data to MQTT broker
 */
class PublishSensorDataUseCase @Inject constructor(
    private val mqttRepository: MqttRepository
) {
    val connectionState: StateFlow<MqttConnectionState> = mqttRepository.connectionState

    suspend fun connect(config: MqttConfig) {
        mqttRepository.connect(config)
    }

    suspend fun publishSensorData(sensorData: SensorData, topic: String) {
        val payload = createJsonPayload(sensorData)
        mqttRepository.publish(topic, payload)
    }

    fun disconnect() {
        mqttRepository.disconnect()
    }

    private fun createJsonPayload(sensorData: SensorData): String {
        return JSONObject().apply {
            put("type", sensorData.type)
            put("name", sensorData.name)
            put("values", sensorData.values)
            put("timestamp", sensorData.timestamp)
        }.toString()
    }
}
