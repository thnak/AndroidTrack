package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.repository.SimulationManager
import javax.inject.Inject

/**
 * Cancels all simulation schedulers (heartbeat + AUTO pins) via [SimulationManager].
 */
class StopSimulationUseCase @Inject constructor(
    private val simulationManager: SimulationManager
) {
    operator fun invoke() {
        simulationManager.stopSimulation()
    }
}
