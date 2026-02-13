package com.androidtrack.app.data.model

/**
 * Represents MQTT connection configuration
 */
data class MqttConfig(
    val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    val clientId: String = "AndroidTrack_${System.currentTimeMillis()}",
    val username: String? = null,
    val password: String? = null,
    val topic: String = "androidtrack/sensors"
)
