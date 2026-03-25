package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.repository.EdgeMqttRepository
import com.androidtrack.app.data.repository.SimulationManager
import com.androidtrack.app.data.repository.WifiInfoProvider
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DisconnectSimulatorUseCaseTest {

    private lateinit var edgeMqttRepository: EdgeMqttRepository
    private lateinit var simulationManager: SimulationManager
    private lateinit var wifiInfoProvider: WifiInfoProvider
    private lateinit var useCase: DisconnectSimulatorUseCase

    @Before
    fun setup() {
        edgeMqttRepository = mock()
        simulationManager = mock()
        wifiInfoProvider = mock()
        useCase = DisconnectSimulatorUseCase(edgeMqttRepository, simulationManager, wifiInfoProvider)
    }

    @Test
    fun `invoke calls stopSimulation`() {
        useCase()
        verify(simulationManager).stopSimulation()
    }

    @Test
    fun `invoke calls disconnect on EdgeMqttRepository`() {
        useCase()
        verify(edgeMqttRepository).disconnect()
    }

    @Test
    fun `invoke calls stopObserving on WifiInfoProvider`() {
        useCase()
        verify(wifiInfoProvider).stopObserving()
    }

    @Test
    fun `invoke calls all three dependencies in order`() {
        useCase()
        // Verify the ordering: stop simulation first, then disconnect, then stop Wi-Fi
        inOrder(simulationManager, edgeMqttRepository, wifiInfoProvider) {
            verify(simulationManager).stopSimulation()
            verify(edgeMqttRepository).disconnect()
            verify(wifiInfoProvider).stopObserving()
        }
    }
}
