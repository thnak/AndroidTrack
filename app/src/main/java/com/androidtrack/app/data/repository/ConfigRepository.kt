package com.androidtrack.app.data.repository

import com.androidtrack.app.data.local.dao.BrokerConfigDao
import com.androidtrack.app.data.local.dao.DeviceConfigDao
import com.androidtrack.app.data.local.entity.BrokerConfigEntity
import com.androidtrack.app.data.local.entity.DeviceConfigEntity
import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for persisting and retrieving [BrokerConfig] and [DeviceConfig] via Room.
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val brokerConfigDao: BrokerConfigDao,
    private val deviceConfigDao: DeviceConfigDao
) {

    // --- Broker config -------------------------------------------------------

    fun observeBrokerConfig(): Flow<BrokerConfig?> =
        brokerConfigDao.observe().map { it?.toDomain() }

    suspend fun getBrokerConfig(): BrokerConfig? =
        brokerConfigDao.get()?.toDomain()

    suspend fun saveBrokerConfig(config: BrokerConfig) {
        brokerConfigDao.save(config.toEntity())
    }

    // --- Device config -------------------------------------------------------

    fun observeDeviceConfig(): Flow<DeviceConfig?> =
        deviceConfigDao.observe().map { it?.toDomain() }

    suspend fun getDeviceConfig(): DeviceConfig? =
        deviceConfigDao.get()?.toDomain()

    suspend fun saveDeviceConfig(config: DeviceConfig) {
        deviceConfigDao.save(config.toEntity())
    }

    // --- Mapping helpers -----------------------------------------------------

    private fun BrokerConfigEntity.toDomain() = BrokerConfig(
        host = host,
        port = port,
        username = username,
        password = password,
        clientId = clientId,
        secure = secure
    )

    private fun BrokerConfig.toEntity() = BrokerConfigEntity(
        id = 1,
        host = host,
        port = port,
        username = username,
        password = password,
        clientId = clientId,
        secure = secure
    )

    private fun DeviceConfigEntity.toDomain() = DeviceConfig(
        deviceId = deviceId,
        deviceType = deviceType
    )

    private fun DeviceConfig.toEntity() = DeviceConfigEntity(
        id = 1,
        deviceId = deviceId,
        deviceType = deviceType
    )
}
