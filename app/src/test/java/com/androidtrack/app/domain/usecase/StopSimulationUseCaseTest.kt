package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.repository.SimulationManager
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class StopSimulationUseCaseTest {

    private lateinit var simulationManager: SimulationManager
    private lateinit var useCase: StopSimulationUseCase

    @Before
    fun setup() {
        simulationManager = mock()
        useCase = StopSimulationUseCase(simulationManager)
    }

    @Test
    fun `invoke calls SimulationManager stopSimulation`() {
        useCase()
        verify(simulationManager).stopSimulation()
    }

    @Test
    fun `invoke can be called multiple times without error`() {
        useCase()
        useCase()
        // Both calls should delegate; no exception thrown.
        verify(simulationManager, org.mockito.kotlin.times(2)).stopSimulation()
    }
}
