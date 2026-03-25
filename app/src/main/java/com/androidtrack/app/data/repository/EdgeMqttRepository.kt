package com.androidtrack.app.data.repository

import android.util.Log
import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

/**
 * MQTT repository for the Edge Device Simulator protocol.
 *
 * Supports four publish operations that map to the topics defined in the technical spec:
 * - [publishDeviceInit]   → device/init
 * - [publishLog]          → devices/{device_id}/log/info
 * - [publishHeartbeat]    → uplink/heartbeat/v1/{device_id}
 * - [publishDiData]       → uplink/v3/di/{pin_number}
 *
 * SSL/TLS is enabled when [BrokerConfig.secure] is true; the system default trust store is used
 * (suitable for publicly-signed certificates).
 */
@Singleton
class EdgeMqttRepository @Inject constructor() {

    private val tag = "EdgeMqttRepository"

    private var mqttClient: MqttAsyncClient? = null

    private val _connectionState =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    // --- Connection ----------------------------------------------------------

    /**
     * Connects to the broker described by [brokerConfig] and, on success, immediately sends the
     * device/init message for [deviceConfig].
     */
    suspend fun connect(brokerConfig: BrokerConfig, deviceConfig: DeviceConfig) {
        try {
            _connectionState.value = MqttConnectionState.Connecting

            mqttClient = MqttAsyncClient(
                brokerConfig.toUrl(),
                brokerConfig.clientId.ifBlank { MqttAsyncClient.generateClientId() },
                MemoryPersistence()
            ).apply {
                setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(tag, "Connected to broker: $serverURI (reconnect=$reconnect)")
                        _connectionState.value =
                            MqttConnectionState.Connected(brokerConfig.toUrl())
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w(tag, "Connection lost", cause)
                        _connectionState.value = MqttConnectionState.Disconnected
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) = Unit

                    override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
                })
            }

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 60

                if (brokerConfig.username.isNotBlank()) userName = brokerConfig.username
                if (brokerConfig.password.isNotBlank()) password = brokerConfig.password.toCharArray()

                if (brokerConfig.secure) {
                    socketFactory = SSLContext.getInstance("TLS").apply {
                        init(null, null, null)
                    }.socketFactory
                }
            }

            try {
                mqttClient?.connect(options)?.waitForCompletion(5_000)
            } catch (e: MqttException) {
                throw Exception("Connection timeout or MQTT error: ${e.message}", e)
            }

            // Send device/init immediately after a successful connection.
            // A publish failure here is non-fatal: the MQTT session remains open.
            try {
                publishDeviceInit(deviceConfig)
            } catch (e: Exception) {
                Log.w(tag, "device/init publish failed after connect – continuing", e)
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to connect", e)
            _connectionState.value = MqttConnectionState.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Disconnects the MQTT client and resets the connection state.
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting", e)
        } finally {
            _connectionState.value = MqttConnectionState.Disconnected
        }
    }

    // --- Publish helpers -----------------------------------------------------

    /**
     * Sends the device initialisation message immediately after connection.
     *
     * Topic:   device/init
     * Payload: {"time_stamp":"<ISO-8601>","device_type":"<device_type>"}
     */
    suspend fun publishDeviceInit(deviceConfig: DeviceConfig) {
        val payload = """{"time_stamp":"${now()}","device_type":"${deviceConfig.deviceType}"}"""
        publish("device/init", payload)
    }

    /**
     * Sends a timestamped log message for the given device.
     *
     * Topic:   devices/{device_id}/log/info
     * Payload: {"message":"<msg>","time_stamp":"<ISO-8601>"}
     */
    suspend fun publishLog(deviceId: String, message: String) {
        val payload = """{"message":"${message.escape()}","time_stamp":"${now()}"}"""
        publish("devices/$deviceId/log/info", payload)
    }

    /**
     * Sends an uplink heartbeat for the given device.
     *
     * Topic:   uplink/heartbeat/v1/{device_id}
     * Payload: {"serial_no":"…","attribute_name":"…","device_status":"online",
     *            "time_stamp":"…","rssi":<int>}
     *
     * @param attributeName One of: counter, status, temp, humidity.
     * @param rssi          Current Wi-Fi RSSI in dBm ([WifiInfoProvider.RSSI_UNKNOWN] is sent as 0).
     */
    suspend fun publishHeartbeat(
        deviceId: String,
        attributeName: String,
        rssi: Int
    ) {
        val rssiValue = if (rssi == WifiInfoProvider.RSSI_UNKNOWN) 0 else rssi
        val payload = """{"serial_no":"$deviceId","attribute_name":"$attributeName",""" +
                """"device_status":"online","time_stamp":"${now()}","rssi":$rssiValue}"""
        publish("uplink/heartbeat/v1/$deviceId", payload)
    }

    /**
     * Sends a DI data message for the given pin.
     *
     * Topic:   uplink/v3/di/{pin_number}
     * Payload: {"time_stamp":"…","shoot_count":<long>,"pulse_time":<long>}
     */
    suspend fun publishDiData(pin: DiPin) {
        val payload = """{"time_stamp":"${now()}",""" +
                """"shoot_count":${pin.shootCount},"pulse_time":${pin.pulseTime}}"""
        publish("uplink/v3/di/${pin.pinNumber}", payload)
    }

    // --- Internal ------------------------------------------------------------

    private suspend fun publish(topic: String, payload: String, qos: Int = 1) {
        try {
            val client = mqttClient ?: return
            if (!client.isConnected) return
            val msg = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                this.isRetained = false
            }
            client.publish(topic, msg)
            Log.d(tag, "→ $topic  $payload")
        } catch (e: Exception) {
            Log.e(tag, "Failed to publish to $topic", e)
        }
    }

    private fun now(): String = Instant.now().toString()

    /** Escapes double-quotes inside a string value for safe embedding in a JSON string. */
    private fun String.escape(): String = replace("\"", "\\\"")
}
