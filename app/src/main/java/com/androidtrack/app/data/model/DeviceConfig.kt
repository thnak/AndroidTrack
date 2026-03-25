package com.androidtrack.app.data.model

/**
 * Domain model representing the simulated edge device identity.
 *
 * @param deviceId   User-supplied identifier (e.g. "DEV-001") used in MQTT topics and payloads.
 * @param deviceType User-supplied device class (e.g. "GATEWAY-V3") sent in the device/init message.
 */
data class DeviceConfig(
    val deviceId: String = "DEV-001",
    val deviceType: String = "GATEWAY-V3"
)
