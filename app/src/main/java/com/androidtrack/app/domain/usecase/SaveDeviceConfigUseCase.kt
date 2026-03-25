package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.repository.ConfigRepository
import javax.inject.Inject

/**
 * Validates and persists the [DeviceConfig].
 *
 * Validation rules:
 * - [DeviceConfig.deviceId] must not be blank.
 * - [DeviceConfig.deviceType] must not be blank.
 */
class SaveDeviceConfigUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(config: DeviceConfig): Result<Unit> {
        if (config.deviceId.isBlank())
            return Result.failure(IllegalArgumentException("Device ID cannot be empty"))
        if (config.deviceType.isBlank())
            return Result.failure(IllegalArgumentException("Device type cannot be empty"))
        configRepository.saveDeviceConfig(config)
        return Result.success(Unit)
    }
}
