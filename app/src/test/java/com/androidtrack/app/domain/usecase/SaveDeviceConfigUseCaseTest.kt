package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.repository.ConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SaveDeviceConfigUseCaseTest {

    private lateinit var configRepository: ConfigRepository
    private lateinit var useCase: SaveDeviceConfigUseCase

    @Before
    fun setup() {
        configRepository = mock()
        useCase = SaveDeviceConfigUseCase(configRepository)
    }

    // --- successful saves ----------------------------------------------------------

    @Test
    fun `valid config is saved and returns success`() = runTest {
        val config = DeviceConfig(deviceId = "DEV-001", deviceType = "GATEWAY-V3")
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveDeviceConfig(config)
    }

    @Test
    fun `single character deviceId is accepted`() = runTest {
        val config = DeviceConfig(deviceId = "X", deviceType = "TYPE-1")
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveDeviceConfig(config)
    }

    // --- deviceId validation ------------------------------------------------------

    @Test
    fun `blank deviceId returns failure and does not save`() = runTest {
        val config = DeviceConfig(deviceId = "  ", deviceType = "GATEWAY-V3")
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Device ID cannot be empty", result.exceptionOrNull()?.message)
        verify(configRepository, never()).saveDeviceConfig(config)
    }

    @Test
    fun `empty deviceId returns failure`() = runTest {
        val config = DeviceConfig(deviceId = "", deviceType = "GATEWAY-V3")
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Device ID cannot be empty", result.exceptionOrNull()?.message)
    }

    // --- deviceType validation ----------------------------------------------------

    @Test
    fun `blank deviceType returns failure and does not save`() = runTest {
        val config = DeviceConfig(deviceId = "DEV-001", deviceType = "   ")
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Device type cannot be empty", result.exceptionOrNull()?.message)
        verify(configRepository, never()).saveDeviceConfig(config)
    }

    @Test
    fun `empty deviceType returns failure`() = runTest {
        val config = DeviceConfig(deviceId = "DEV-001", deviceType = "")
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Device type cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank deviceId takes precedence over blank deviceType`() = runTest {
        val config = DeviceConfig(deviceId = "", deviceType = "")
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Device ID cannot be empty", result.exceptionOrNull()?.message)
    }
}
