package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.repository.ConfigRepository
import com.androidtrack.app.data.repository.EdgeMqttRepository
import com.androidtrack.app.data.repository.WifiInfoProvider
import javax.inject.Inject

/**
 * Loads the saved [BrokerConfig] and [DeviceConfig] from the database, resolves the device
 * Client ID from the MAC address (via [WifiInfoProvider]) when the stored clientId is blank,
 * then connects the [EdgeMqttRepository] and starts Wi-Fi RSSI monitoring.
 */
class ConnectSimulatorUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    private val edgeMqttRepository: EdgeMqttRepository,
    private val wifiInfoProvider: WifiInfoProvider
) {
    suspend operator fun invoke() {
        val brokerConfig = configRepository.getBrokerConfig() ?: BrokerConfig()
        val deviceConfig = configRepository.getDeviceConfig() ?: DeviceConfig()

        val clientId = brokerConfig.clientId.ifBlank {
            wifiInfoProvider.getMacAddress()
        }

        wifiInfoProvider.startObserving()
        edgeMqttRepository.connect(brokerConfig.copy(clientId = clientId), deviceConfig)
    }
}
