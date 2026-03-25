package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.SimulationManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TriggerManualPinUseCaseTest {

    private lateinit var simulationManager: SimulationManager
    private lateinit var useCase: TriggerManualPinUseCase

    @Before
    fun setup() {
        simulationManager = mock()
        useCase = TriggerManualPinUseCase(simulationManager)
    }

    @Test
    fun `invoke delegates to SimulationManager triggerManualPin`() = runTest {
        val pin = DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL, shootCount = 5L)
        val updated = pin.copy(shootCount = 6L)
        whenever(simulationManager.triggerManualPin(pin)).thenReturn(updated)

        val result = useCase(pin)

        verify(simulationManager).triggerManualPin(pin)
        assertEquals(updated, result)
    }

    @Test
    fun `returned pin has incremented shootCount`() = runTest {
        val pin = DiPin(id = 2, pinNumber = "02", mode = PinMode.MANUAL, shootCount = 99L)
        val updated = pin.copy(shootCount = 100L)
        whenever(simulationManager.triggerManualPin(pin)).thenReturn(updated)

        val result = useCase(pin)

        assertEquals(100L, result.shootCount)
    }

    @Test
    fun `invoke works for AUTO mode pin as well (not restricted by use case)`() = runTest {
        val pin = DiPin(id = 3, pinNumber = "03", mode = PinMode.AUTO, shootCount = 0L)
        val updated = pin.copy(shootCount = 1L)
        whenever(simulationManager.triggerManualPin(pin)).thenReturn(updated)

        val result = useCase(pin)

        verify(simulationManager).triggerManualPin(pin)
        assertEquals(1L, result.shootCount)
    }
}
