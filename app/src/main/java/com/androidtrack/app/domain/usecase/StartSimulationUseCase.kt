package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.repository.ConfigRepository
import com.androidtrack.app.data.repository.DiPinRepository
import com.androidtrack.app.data.repository.SimulationManager
import javax.inject.Inject

/**
 * Loads the current device ID and all DI pins from the database, then starts the simulation
 * (heartbeat + AUTO-mode pin schedulers) via [SimulationManager].
 */
class StartSimulationUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
    private val diPinRepository: DiPinRepository,
    private val simulationManager: SimulationManager
) {
    suspend operator fun invoke() {
        val deviceConfig = configRepository.getDeviceConfig() ?: DeviceConfig()
        val pins = diPinRepository.getAll()
        simulationManager.startSimulation(deviceConfig.deviceId, pins)
    }
}
