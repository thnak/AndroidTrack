package com.androidtrack.app.data.model

/**
 * Sealed class representing MQTT connection state
 */
sealed class MqttConnectionState {
    data object Disconnected : MqttConnectionState()
    data object Connecting : MqttConnectionState()
    data class Connected(val brokerUrl: String) : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}
