package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.repository.SimulationManager
import javax.inject.Inject

/**
 * Increments a MANUAL-mode pin's shoot-count and publishes its DI data payload immediately.
 *
 * @return The updated [DiPin] with the new shoot-count, for live UI refresh.
 */
class TriggerManualPinUseCase @Inject constructor(
    private val simulationManager: SimulationManager
) {
    suspend operator fun invoke(pin: DiPin): DiPin {
        return simulationManager.triggerManualPin(pin)
    }
}
