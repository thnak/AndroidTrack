package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.repository.EdgeMqttRepository
import com.androidtrack.app.data.repository.SimulationManager
import com.androidtrack.app.data.repository.WifiInfoProvider
import javax.inject.Inject

/**
 * Stops the simulation (if running), disconnects from the MQTT broker, and stops Wi-Fi monitoring.
 */
class DisconnectSimulatorUseCase @Inject constructor(
    private val edgeMqttRepository: EdgeMqttRepository,
    private val simulationManager: SimulationManager,
    private val wifiInfoProvider: WifiInfoProvider
) {
    operator fun invoke() {
        simulationManager.stopSimulation()
        edgeMqttRepository.disconnect()
        wifiInfoProvider.stopObserving()
    }
}
