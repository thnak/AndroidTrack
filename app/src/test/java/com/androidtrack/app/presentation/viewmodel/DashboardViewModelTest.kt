package com.androidtrack.app.presentation.viewmodel

import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.EdgeMqttRepository
import com.androidtrack.app.data.repository.SimulationManager
import com.androidtrack.app.data.repository.WifiInfoProvider
import com.androidtrack.app.domain.usecase.ConnectSimulatorUseCase
import com.androidtrack.app.domain.usecase.DisconnectSimulatorUseCase
import com.androidtrack.app.domain.usecase.ManagePinsUseCase
import com.androidtrack.app.domain.usecase.StartSimulationUseCase
import com.androidtrack.app.domain.usecase.StopSimulationUseCase
import com.androidtrack.app.domain.usecase.TriggerManualPinUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [DashboardViewModel].
 *
 * DashboardViewModel has an infinite RSSI polling loop (while(isActive) { delay(3_000) }).
 * Using runTest's implicit advanceUntilIdle() teardown would cause the scheduler to run forever,
 * so tests drive the scheduler manually via [TestCoroutineScheduler.runCurrent], which executes
 * only tasks already queued at the current virtual time without advancing the clock.
 *
 * Tests that stub/verify suspend use-case functions use [runBlocking] so the suspension is valid.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(scheduler)

    private lateinit var connectUseCase: ConnectSimulatorUseCase
    private lateinit var disconnectUseCase: DisconnectSimulatorUseCase
    private lateinit var startUseCase: StartSimulationUseCase
    private lateinit var stopUseCase: StopSimulationUseCase
    private lateinit var triggerUseCase: TriggerManualPinUseCase
    private lateinit var managePinsUseCase: ManagePinsUseCase
    private lateinit var edgeMqttRepository: EdgeMqttRepository
    private lateinit var simulationManager: SimulationManager
    private lateinit var wifiInfoProvider: WifiInfoProvider

    private val connectionStateFlow =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    private val isRunningFlow = MutableStateFlow(false)
    private val recentMessagesFlow = MutableStateFlow<List<String>>(emptyList())

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        connectUseCase = mock()
        disconnectUseCase = mock()
        startUseCase = mock()
        stopUseCase = mock()
        triggerUseCase = mock()
        managePinsUseCase = mock()
        edgeMqttRepository = mock()
        simulationManager = mock()
        wifiInfoProvider = mock()

        whenever(edgeMqttRepository.connectionState).thenReturn(connectionStateFlow)
        whenever(edgeMqttRepository.recentMessages).thenReturn(recentMessagesFlow)
        whenever(simulationManager.isRunning).thenReturn(isRunningFlow)
        whenever(managePinsUseCase.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(wifiInfoProvider.getRssi()).thenReturn(WifiInfoProvider.RSSI_UNKNOWN)

        viewModel = DashboardViewModel(
            connectUseCase, disconnectUseCase, startUseCase, stopUseCase,
            triggerUseCase, managePinsUseCase, edgeMqttRepository, simulationManager, wifiInfoProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Executes only tasks queued at the current virtual time without advancing the clock. */
    private fun drainCurrent() = scheduler.runCurrent()

    // --- initial state -------------------------------------------------------

    @Test
    fun `initial connectionState reflects repository`() {
        assertEquals(MqttConnectionState.Disconnected, viewModel.connectionState.value)
    }

    @Test
    fun `initial isRunning is false`() {
        assertEquals(false, viewModel.isRunning.value)
    }

    @Test
    fun `initial pins list is empty`() {
        assertEquals(emptyList<DiPin>(), viewModel.pins.value)
    }

    @Test
    fun `initial errorMessage is null`() {
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `initial rssi is RSSI_UNKNOWN`() {
        assertEquals(WifiInfoProvider.RSSI_UNKNOWN, viewModel.rssi.value)
    }

    @Test
    fun `logMessages reflect repository recentMessages`() {
        assertEquals(emptyList<String>(), viewModel.logMessages.value)
    }

    // --- connect / disconnect -------------------------------------------------

    @Test
    fun `connect delegates to ConnectSimulatorUseCase`() {
        runBlocking {
            viewModel.connect()
            drainCurrent()
            verify(connectUseCase).invoke()
        }
    }

    @Test
    fun `disconnect delegates to DisconnectSimulatorUseCase`() {
        viewModel.disconnect()
        verify(disconnectUseCase).invoke()
    }

    // --- start / stop simulation ---------------------------------------------

    @Test
    fun `startSimulation delegates to StartSimulationUseCase`() {
        runBlocking {
            viewModel.startSimulation()
            drainCurrent()
            verify(startUseCase).invoke()
        }
    }

    @Test
    fun `stopSimulation delegates to StopSimulationUseCase`() {
        viewModel.stopSimulation()
        verify(stopUseCase).invoke()
    }

    // --- triggerPin ----------------------------------------------------------

    @Test
    fun `triggerPin delegates to TriggerManualPinUseCase with correct pin`() {
        runBlocking {
            val pin = DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL, shootCount = 0L)
            whenever(triggerUseCase.invoke(pin)).thenReturn(pin.copy(shootCount = 1L))
            viewModel.triggerPin(pin)
            drainCurrent()
            verify(triggerUseCase).invoke(pin)
        }
    }

    // --- error lifecycle -----------------------------------------------------

    @Test
    fun `exception during connect sets errorMessage`() {
        runBlocking {
            whenever(connectUseCase.invoke()).thenThrow(RuntimeException("timeout"))
            viewModel.connect()
            drainCurrent()
            assertEquals("timeout", viewModel.errorMessage.value)
        }
    }

    @Test
    fun `clearError nullifies errorMessage`() {
        runBlocking {
            whenever(connectUseCase.invoke()).thenThrow(RuntimeException("MQTT boom"))
            viewModel.connect()
            drainCurrent()
            assertEquals("MQTT boom", viewModel.errorMessage.value)
            viewModel.clearError()
            assertNull(viewModel.errorMessage.value)
        }
    }

    // --- state propagation ---------------------------------------------------

    @Test
    fun `connectionState reflects upstream flow changes`() {
        connectionStateFlow.value = MqttConnectionState.Connected("tcp://localhost:1883")
        assertEquals(MqttConnectionState.Connected("tcp://localhost:1883"), viewModel.connectionState.value)
    }

    @Test
    fun `isRunning reflects upstream flow changes`() {
        isRunningFlow.value = true
        assertEquals(true, viewModel.isRunning.value)
    }

    @Test
    fun `logMessages reflect upstream recentMessages changes`() {
        recentMessagesFlow.value = listOf("-> device/init", "-> heartbeat")
        assertEquals(2, viewModel.logMessages.value.size)
    }

    @Test
    fun `pins list is populated from managePinsUseCase`() {
        val pinList = listOf(DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL))
        whenever(managePinsUseCase.observeAll()).thenReturn(flowOf(pinList))
        val vm = DashboardViewModel(
            connectUseCase, disconnectUseCase, startUseCase, stopUseCase,
            triggerUseCase, managePinsUseCase, edgeMqttRepository, simulationManager, wifiInfoProvider
        )
        drainCurrent()
        assertEquals(pinList, vm.pins.value)
    }
}
