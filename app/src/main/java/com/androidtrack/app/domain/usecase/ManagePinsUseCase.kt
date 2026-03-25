package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.DiPinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Manages the lifecycle of virtual DI pins: observe, add, update, delete.
 *
 * Validation (enforced on add and update):
 * - [DiPin.pinNumber] must not be blank and must be unique across all persisted pins
 *   (excluding the pin being edited, identified by its [DiPin.id]).
 * - [DiPin.timerMs] must be > 0 for AUTO-mode pins.
 */
class ManagePinsUseCase @Inject constructor(
    private val diPinRepository: DiPinRepository
) {
    fun observeAll(): Flow<List<DiPin>> = diPinRepository.observeAll()

    suspend fun addPin(pin: DiPin): Result<Unit> {
        val error = validate(pin, excludeId = 0)
        if (error != null) return Result.failure(IllegalArgumentException(error))
        diPinRepository.insert(pin)
        return Result.success(Unit)
    }

    suspend fun updatePin(pin: DiPin): Result<Unit> {
        val error = validate(pin, excludeId = pin.id)
        if (error != null) return Result.failure(IllegalArgumentException(error))
        diPinRepository.update(pin)
        return Result.success(Unit)
    }

    suspend fun deletePin(id: Int) {
        diPinRepository.deleteById(id)
    }

    private suspend fun validate(pin: DiPin, excludeId: Int): String? {
        if (pin.pinNumber.isBlank()) return "Pin number cannot be empty"
        if (!diPinRepository.isPinNumberUnique(pin.pinNumber, excludeId))
            return "Pin '${pin.pinNumber}' is already in use"
        if (pin.mode == PinMode.AUTO && pin.timerMs <= 0)
            return "Timer must be greater than 0 ms"
        return null
    }
}
