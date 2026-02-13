package com.androidtrack.app.data.repository

import android.content.Context
import android.util.Log
import com.androidtrack.app.data.model.MqttConfig
import com.androidtrack.app.data.model.MqttConnectionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing MQTT connections and publishing data
 */
@Singleton
class MqttRepository @Inject constructor(
    private val context: Context
) {
    private val TAG = "MqttRepository"
    
    private var mqttClient: MqttAndroidClient? = null
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    /**
     * Connect to MQTT broker
     */
    suspend fun connect(config: MqttConfig) {
        try {
            _connectionState.value = MqttConnectionState.Connecting
            
            mqttClient = MqttAndroidClient(context, config.brokerUrl, config.clientId).apply {
                setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "Connected to MQTT broker: $serverURI")
                        _connectionState.value = MqttConnectionState.Connected(config.brokerUrl)
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "Connection lost", cause)
                        _connectionState.value = MqttConnectionState.Disconnected
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        // Not used for publishing only
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Message delivered
                    }
                })
            }

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 60
                
                config.username?.let { userName = it }
                config.password?.let { password = it.toCharArray() }
            }

            mqttClient?.connect(options)?.waitForCompletion(5000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MQTT broker", e)
            _connectionState.value = MqttConnectionState.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Publish message to MQTT topic
     */
    suspend fun publish(topic: String, payload: String, qos: Int = 1) {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    val message = MqttMessage(payload.toByteArray()).apply {
                        this.qos = qos
                        isRetained = false
                    }
                    client.publish(topic, message)
                    Log.d(TAG, "Published to $topic: $payload")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish message", e)
        }
    }

    /**
     * Disconnect from MQTT broker
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
            _connectionState.value = MqttConnectionState.Disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}
