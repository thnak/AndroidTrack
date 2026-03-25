package com.androidtrack.app.data.repository

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SimulationManager].
 *
 * Dependencies ([EdgeMqttRepository], [DiPinRepository], [WifiInfoProvider]) are mocked so that
 * scheduler behaviour and state transitions can be verified without actual MQTT connections or a
 * database.
 */
class SimulationManagerTest {

    private lateinit var edgeMqttRepository: EdgeMqttRepository
    private lateinit var diPinRepository: DiPinRepository
    private lateinit var wifiInfoProvider: WifiInfoProvider
    private lateinit var appLogger: AppLogger
    private lateinit var manager: SimulationManager

    private val connectionStateFlow =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    @Before
    fun setup() {
        edgeMqttRepository = mock()
        diPinRepository = mock()
        wifiInfoProvider = mock()
        appLogger = mock()

        whenever(edgeMqttRepository.connectionState).thenReturn(connectionStateFlow)

        manager = SimulationManager(edgeMqttRepository, diPinRepository, wifiInfoProvider, appLogger)
    }

    @After
    fun tearDown() {
        // Ensure the scheduler is always shut down to avoid thread leaks between tests.
        manager.stopSimulation()
    }

    // --- initial state ------------------------------------------------------------

    @Test
    fun `initial isRunning is false`() {
        assertFalse(manager.isRunning.value)
    }

    // --- startSimulation ----------------------------------------------------------

    @Test
    fun `startSimulation sets isRunning to true`() {
        manager.startSimulation("DEV-001", emptyList())
        assertTrue(manager.isRunning.value)
    }

    @Test
    fun `startSimulation starts Wi-Fi observing`() {
        manager.startSimulation("DEV-001", emptyList())
        verify(wifiInfoProvider).startObserving()
    }

    @Test
    fun `startSimulation called twice is a no-op on second call`() {
        manager.startSimulation("DEV-001", emptyList())
        manager.startSimulation("DEV-001", emptyList())
        // startObserving should only be called once (second start is ignored)
        verify(wifiInfoProvider, org.mockito.kotlin.times(1)).startObserving()
        assertTrue(manager.isRunning.value)
    }

    // --- stopSimulation -----------------------------------------------------------

    @Test
    fun `stopSimulation sets isRunning to false`() {
        manager.startSimulation("DEV-001", emptyList())
        manager.stopSimulation()
        assertFalse(manager.isRunning.value)
    }

    @Test
    fun `stopSimulation stops Wi-Fi observing`() {
        manager.startSimulation("DEV-001", emptyList())
        manager.stopSimulation()
        verify(wifiInfoProvider).stopObserving()
    }

    @Test
    fun `stopSimulation without prior start does not throw`() {
        // Should be safe to call even if startSimulation was never called.
        manager.stopSimulation()
        assertFalse(manager.isRunning.value)
    }

    @Test
    fun `start then stop then start again succeeds`() {
        manager.startSimulation("DEV-001", emptyList())
        manager.stopSimulation()
        manager.startSimulation("DEV-002", emptyList())
        assertTrue(manager.isRunning.value)
    }

    // --- triggerManualPin ---------------------------------------------------------

    @Test
    fun `triggerManualPin increments shootCount and returns updated pin`() = runTest {
        val pin = DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL, shootCount = 10L)
        val updated = pin.copy(shootCount = 11L)
        whenever(diPinRepository.incrementShootCount(pin)).thenReturn(updated)

        val result = manager.triggerManualPin(pin)

        verify(diPinRepository).incrementShootCount(pin)
        assertEquals(updated, result)
        assertEquals(11L, result.shootCount)
    }

    @Test
    fun `triggerManualPin publishes DI data when MQTT is connected`() = runTest {
        connectionStateFlow.value = MqttConnectionState.Connected("tcp://localhost:1883")

        val pin = DiPin(id = 2, pinNumber = "02", mode = PinMode.MANUAL, shootCount = 0L)
        val updated = pin.copy(shootCount = 1L)
        whenever(diPinRepository.incrementShootCount(pin)).thenReturn(updated)

        manager.triggerManualPin(pin)

        verify(edgeMqttRepository).publishDiData(updated)
    }

    @Test
    fun `triggerManualPin does NOT publish DI data when MQTT is disconnected`() = runTest {
        connectionStateFlow.value = MqttConnectionState.Disconnected

        val pin = DiPin(id = 3, pinNumber = "03", mode = PinMode.MANUAL, shootCount = 5L)
        val updated = pin.copy(shootCount = 6L)
        whenever(diPinRepository.incrementShootCount(pin)).thenReturn(updated)

        manager.triggerManualPin(pin)

        verify(edgeMqttRepository, never()).publishDiData(any())
    }

    @Test
    fun `triggerManualPin does NOT publish DI data when MQTT is in Error state`() = runTest {
        connectionStateFlow.value = MqttConnectionState.Error("some error")

        val pin = DiPin(id = 4, pinNumber = "04", mode = PinMode.MANUAL, shootCount = 0L)
        val updated = pin.copy(shootCount = 1L)
        whenever(diPinRepository.incrementShootCount(pin)).thenReturn(updated)

        manager.triggerManualPin(pin)

        verify(edgeMqttRepository, never()).publishDiData(any())
    }
}
