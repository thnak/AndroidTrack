package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.DiPinRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ManagePinsUseCaseTest {

    private lateinit var diPinRepository: DiPinRepository
    private lateinit var useCase: ManagePinsUseCase

    private val manualPin = DiPin(id = 0, pinNumber = "01", mode = PinMode.MANUAL, timerMs = 5000L)
    private val autoPin = DiPin(id = 0, pinNumber = "02", mode = PinMode.AUTO, timerMs = 3000L)

    @Before
    fun setup() {
        diPinRepository = mock()
        useCase = ManagePinsUseCase(diPinRepository)
    }

    // --- observeAll ---------------------------------------------------------------

    @Test
    fun `observeAll delegates to repository`() {
        val pins = listOf(manualPin)
        whenever(diPinRepository.observeAll()).thenReturn(flowOf(pins))
        // Just verify it returns a flow – the actual emission is tested via integration.
        val flow = useCase.observeAll()
        // flow is not null and identical to the repository's flow
        assertEquals(diPinRepository.observeAll(), flow)
    }

    // --- addPin -------------------------------------------------------------------

    @Test
    fun `addPin with valid MANUAL pin succeeds and inserts`() = runTest {
        whenever(diPinRepository.isPinNumberUnique("01", excludeId = 0)).thenReturn(true)
        val result = useCase.addPin(manualPin)
        assertTrue(result.isSuccess)
        verify(diPinRepository).insert(manualPin)
    }

    @Test
    fun `addPin with valid AUTO pin succeeds and inserts`() = runTest {
        whenever(diPinRepository.isPinNumberUnique("02", excludeId = 0)).thenReturn(true)
        val result = useCase.addPin(autoPin)
        assertTrue(result.isSuccess)
        verify(diPinRepository).insert(autoPin)
    }

    @Test
    fun `addPin with blank pinNumber returns failure`() = runTest {
        val pin = DiPin(pinNumber = "  ", mode = PinMode.MANUAL)
        val result = useCase.addPin(pin)
        assertTrue(result.isFailure)
        assertEquals("Pin number cannot be empty", result.exceptionOrNull()?.message)
        verify(diPinRepository, never()).insert(pin)
    }

    @Test
    fun `addPin with empty pinNumber returns failure`() = runTest {
        val pin = DiPin(pinNumber = "", mode = PinMode.MANUAL)
        val result = useCase.addPin(pin)
        assertTrue(result.isFailure)
        assertEquals("Pin number cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `addPin with duplicate pinNumber returns failure`() = runTest {
        whenever(diPinRepository.isPinNumberUnique("01", excludeId = 0)).thenReturn(false)
        val result = useCase.addPin(manualPin)
        assertTrue(result.isFailure)
        assertEquals("Pin '01' is already in use", result.exceptionOrNull()?.message)
        verify(diPinRepository, never()).insert(manualPin)
    }

    @Test
    fun `addPin with AUTO mode and timerMs 0 returns failure`() = runTest {
        whenever(diPinRepository.isPinNumberUnique("03", excludeId = 0)).thenReturn(true)
        val pin = DiPin(pinNumber = "03", mode = PinMode.AUTO, timerMs = 0L)
        val result = useCase.addPin(pin)
        assertTrue(result.isFailure)
        assertEquals("Timer must be greater than 0 ms", result.exceptionOrNull()?.message)
        verify(diPinRepository, never()).insert(pin)
    }

    @Test
    fun `addPin with AUTO mode and negative timerMs returns failure`() = runTest {
        whenever(diPinRepository.isPinNumberUnique("03", excludeId = 0)).thenReturn(true)
        val pin = DiPin(pinNumber = "03", mode = PinMode.AUTO, timerMs = -100L)
        val result = useCase.addPin(pin)
        assertTrue(result.isFailure)
        assertEquals("Timer must be greater than 0 ms", result.exceptionOrNull()?.message)
    }

    @Test
    fun `addPin with MANUAL mode and timerMs 0 succeeds (timer not checked for MANUAL)`() = runTest {
        val pin = DiPin(pinNumber = "04", mode = PinMode.MANUAL, timerMs = 0L)
        whenever(diPinRepository.isPinNumberUnique("04", excludeId = 0)).thenReturn(true)
        val result = useCase.addPin(pin)
        assertTrue(result.isSuccess)
        verify(diPinRepository).insert(pin)
    }

    // --- updatePin ----------------------------------------------------------------

    @Test
    fun `updatePin with valid data succeeds and calls update`() = runTest {
        val existing = DiPin(id = 5, pinNumber = "01", mode = PinMode.MANUAL)
        // excludeId = pin.id = 5, so the unique check ignores itself
        whenever(diPinRepository.isPinNumberUnique("01", excludeId = 5)).thenReturn(true)
        val result = useCase.updatePin(existing)
        assertTrue(result.isSuccess)
        verify(diPinRepository).update(existing)
    }

    @Test
    fun `updatePin is not blocked by its own pinNumber (excludes self from uniqueness check)`() = runTest {
        // Simulates renaming a pin while it already owns that number.
        val pin = DiPin(id = 7, pinNumber = "99", mode = PinMode.AUTO, timerMs = 1000L)
        whenever(diPinRepository.isPinNumberUnique("99", excludeId = 7)).thenReturn(true)
        val result = useCase.updatePin(pin)
        assertTrue(result.isSuccess)
        verify(diPinRepository).update(pin)
    }

    @Test
    fun `updatePin with duplicate pinNumber used by other pin returns failure`() = runTest {
        val pin = DiPin(id = 3, pinNumber = "01", mode = PinMode.MANUAL)
        whenever(diPinRepository.isPinNumberUnique("01", excludeId = 3)).thenReturn(false)
        val result = useCase.updatePin(pin)
        assertTrue(result.isFailure)
        assertEquals("Pin '01' is already in use", result.exceptionOrNull()?.message)
        verify(diPinRepository, never()).update(pin)
    }

    @Test
    fun `updatePin with blank pinNumber returns failure`() = runTest {
        val pin = DiPin(id = 1, pinNumber = "", mode = PinMode.MANUAL)
        val result = useCase.updatePin(pin)
        assertTrue(result.isFailure)
        assertEquals("Pin number cannot be empty", result.exceptionOrNull()?.message)
    }

    // --- deletePin ----------------------------------------------------------------

    @Test
    fun `deletePin delegates to repository with correct id`() = runTest {
        useCase.deletePin(id = 42)
        verify(diPinRepository).deleteById(42)
    }
}
