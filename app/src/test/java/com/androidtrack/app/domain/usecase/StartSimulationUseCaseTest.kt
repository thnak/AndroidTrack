package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.ConfigRepository
import com.androidtrack.app.data.repository.DiPinRepository
import com.androidtrack.app.data.repository.SimulationManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StartSimulationUseCaseTest {

    private lateinit var configRepository: ConfigRepository
    private lateinit var diPinRepository: DiPinRepository
    private lateinit var simulationManager: SimulationManager
    private lateinit var useCase: StartSimulationUseCase

    @Before
    fun setup() {
        configRepository = mock()
        diPinRepository = mock()
        simulationManager = mock()
        useCase = StartSimulationUseCase(configRepository, diPinRepository, simulationManager)
    }

    @Test
    fun `invoke starts simulation with deviceId and all pins from DB`() = runTest {
        val config = DeviceConfig(deviceId = "DEV-001", deviceType = "GW")
        val pins = listOf(
            DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL),
            DiPin(id = 2, pinNumber = "02", mode = PinMode.AUTO, timerMs = 5000L)
        )
        whenever(configRepository.getDeviceConfig()).thenReturn(config)
        whenever(diPinRepository.getAll()).thenReturn(pins)

        useCase()

        verify(simulationManager).startSimulation("DEV-001", pins)
    }

    @Test
    fun `invoke uses default DeviceConfig when none is saved`() = runTest {
        whenever(configRepository.getDeviceConfig()).thenReturn(null)
        whenever(diPinRepository.getAll()).thenReturn(emptyList())

        useCase()

        // DefaultDeviceConfig().deviceId is "DEV-001"
        verify(simulationManager).startSimulation(DeviceConfig().deviceId, emptyList())
    }

    @Test
    fun `invoke passes empty pin list when no pins are configured`() = runTest {
        val config = DeviceConfig(deviceId = "DEV-X", deviceType = "GW")
        whenever(configRepository.getDeviceConfig()).thenReturn(config)
        whenever(diPinRepository.getAll()).thenReturn(emptyList())

        useCase()

        verify(simulationManager).startSimulation("DEV-X", emptyList())
    }
}
