package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.repository.ConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SaveBrokerConfigUseCaseTest {

    private lateinit var configRepository: ConfigRepository
    private lateinit var useCase: SaveBrokerConfigUseCase

    @Before
    fun setup() {
        configRepository = mock()
        useCase = SaveBrokerConfigUseCase(configRepository)
    }

    // --- successful saves ----------------------------------------------------------

    @Test
    fun `valid config is saved and returns success`() = runTest {
        val config = BrokerConfig(host = "mqtt.example.com", port = 1883)
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveBrokerConfig(config)
    }

    @Test
    fun `minimum valid port 1 is accepted`() = runTest {
        val config = BrokerConfig(host = "host.example.com", port = 1)
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveBrokerConfig(config)
    }

    @Test
    fun `maximum valid port 65535 is accepted`() = runTest {
        val config = BrokerConfig(host = "host.example.com", port = 65535)
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveBrokerConfig(config)
    }

    @Test
    fun `config with secure true is saved`() = runTest {
        val config = BrokerConfig(host = "secure.example.com", port = 8883, secure = true)
        val result = useCase(config)
        assertTrue(result.isSuccess)
        verify(configRepository).saveBrokerConfig(config)
    }

    // --- host validation ----------------------------------------------------------

    @Test
    fun `blank host returns failure and does not save`() = runTest {
        val config = BrokerConfig(host = "  ", port = 1883)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Host cannot be empty", result.exceptionOrNull()?.message)
        verify(configRepository, never()).saveBrokerConfig(config)
    }

    @Test
    fun `empty host string returns failure`() = runTest {
        val config = BrokerConfig(host = "", port = 1883)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Host cannot be empty", result.exceptionOrNull()?.message)
    }

    // --- port validation ----------------------------------------------------------

    @Test
    fun `port 0 returns failure and does not save`() = runTest {
        val config = BrokerConfig(host = "host.example.com", port = 0)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Port must be between 1 and 65535", result.exceptionOrNull()?.message)
        verify(configRepository, never()).saveBrokerConfig(config)
    }

    @Test
    fun `negative port returns failure`() = runTest {
        val config = BrokerConfig(host = "host.example.com", port = -1)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Port must be between 1 and 65535", result.exceptionOrNull()?.message)
    }

    @Test
    fun `port 65536 returns failure`() = runTest {
        val config = BrokerConfig(host = "host.example.com", port = 65536)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Port must be between 1 and 65535", result.exceptionOrNull()?.message)
        verify(configRepository, never()).saveBrokerConfig(config)
    }

    @Test
    fun `blank host takes precedence over invalid port`() = runTest {
        val config = BrokerConfig(host = "", port = 0)
        val result = useCase(config)
        assertTrue(result.isFailure)
        assertEquals("Host cannot be empty", result.exceptionOrNull()?.message)
    }
}
