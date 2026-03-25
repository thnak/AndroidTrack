package com.androidtrack.app.data.repository

import com.androidtrack.app.data.local.dao.DiPinDao
import com.androidtrack.app.data.local.entity.DiPinEntity
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for CRUD operations on virtual [DiPin] records persisted in Room.
 */
@Singleton
class DiPinRepository @Inject constructor(
    private val diPinDao: DiPinDao
) {

    fun observeAll(): Flow<List<DiPin>> =
        diPinDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<DiPin> =
        diPinDao.getAll().map { it.toDomain() }

    suspend fun insert(pin: DiPin): Long =
        diPinDao.insert(pin.toEntity())

    suspend fun update(pin: DiPin) =
        diPinDao.update(pin.toEntity())

    suspend fun deleteById(id: Int) =
        diPinDao.deleteById(id)

    /**
     * Returns true when no other pin (excluding [excludeId]) already uses [pinNumber].
     */
    suspend fun isPinNumberUnique(pinNumber: String, excludeId: Int = 0): Boolean =
        diPinDao.countByPinNumber(pinNumber, excludeId) == 0

    /**
     * Atomically increments [pin]'s shoot-count in the database and returns the updated model.
     */
    suspend fun incrementShootCount(pin: DiPin): DiPin {
        val updated = pin.copy(shootCount = pin.shootCount + 1)
        diPinDao.updateShootCount(pin.id, updated.shootCount)
        return updated
    }

    // --- Mapping helpers -----------------------------------------------------

    private fun DiPinEntity.toDomain() = DiPin(
        id = id,
        pinNumber = pinNumber,
        mode = PinMode.valueOf(mode),
        timerMs = timerMs,
        shootCount = shootCount,
        pulseTime = pulseTime
    )

    private fun DiPin.toEntity() = DiPinEntity(
        id = id,
        pinNumber = pinNumber,
        mode = mode.name,
        timerMs = timerMs,
        shootCount = shootCount,
        pulseTime = pulseTime
    )
}
