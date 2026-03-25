package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.repository.ConfigRepository
import javax.inject.Inject

/**
 * Validates and persists the [BrokerConfig].
 *
 * Validation rules:
 * - [BrokerConfig.host] must not be blank.
 * - [BrokerConfig.port] must be in the range 1..65535.
 */
class SaveBrokerConfigUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(config: BrokerConfig): Result<Unit> {
        if (config.host.isBlank())
            return Result.failure(IllegalArgumentException("Host cannot be empty"))
        if (config.port < 1 || config.port > 65535)
            return Result.failure(IllegalArgumentException("Port must be between 1 and 65535"))
        configRepository.saveBrokerConfig(config)
        return Result.success(Unit)
    }
}
