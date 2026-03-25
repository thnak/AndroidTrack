package com.androidtrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.repository.AppLogger
import com.androidtrack.app.data.repository.AppSettingsRepository
import com.androidtrack.app.data.repository.EdgeMqttRepository
import com.androidtrack.app.data.repository.SimulationManager
import com.androidtrack.app.data.repository.WifiInfoProvider
import com.androidtrack.app.domain.usecase.ConnectSimulatorUseCase
import com.androidtrack.app.domain.usecase.DisconnectSimulatorUseCase
import com.androidtrack.app.domain.usecase.ManagePinsUseCase
import com.androidtrack.app.domain.usecase.StartSimulationUseCase
import com.androidtrack.app.domain.usecase.StopSimulationUseCase
import com.androidtrack.app.domain.usecase.TriggerManualPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 *
 * Exposes reactive state for:
 * - MQTT connection status
 * - Simulation running / starting / stopping flags
 * - Live DI pin list (shoot-count updated in real-time via Room Flow)
 * - Per-pin triggering state to prevent duplicate taps
 * - Wi-Fi RSSI (polled every 3 s)
 * - Console log of recent events (from [AppLogger])
 * - Console log visibility toggle (from [AppSettingsRepository])
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val connectSimulatorUseCase: ConnectSimulatorUseCase,
    private val disconnectSimulatorUseCase: DisconnectSimulatorUseCase,
    private val startSimulationUseCase: StartSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    private val triggerManualPinUseCase: TriggerManualPinUseCase,
    private val managePinsUseCase: ManagePinsUseCase,
    private val edgeMqttRepository: EdgeMqttRepository,
    private val simulationManager: SimulationManager,
    private val wifiInfoProvider: WifiInfoProvider,
    private val appLogger: AppLogger,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val connectionState: StateFlow<MqttConnectionState> = edgeMqttRepository.connectionState

    val isRunning: StateFlow<Boolean> = simulationManager.isRunning

    /** True while [startSimulation] coroutine is in-flight (before [isRunning] becomes true). */
    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    /** True while [stopSimulation] is being processed. */
    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

    /** Set of pin IDs that are currently being triggered (prevents double-tap). */
    private val _triggeringPinIds = MutableStateFlow<Set<Int>>(emptySet())
    val triggeringPinIds: StateFlow<Set<Int>> = _triggeringPinIds.asStateFlow()

    val pins: StateFlow<List<DiPin>> = managePinsUseCase.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Console log entries from the centralized [AppLogger]. */
    val logMessages: StateFlow<List<AppLogger.LogEntry>> = appLogger.logs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Whether the Console Log section should be shown on the Dashboard. */
    val showConsoleLog: StateFlow<Boolean> = appSettingsRepository.showConsoleLog

    private val _rssi = MutableStateFlow(WifiInfoProvider.RSSI_UNKNOWN)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _rssi.value = wifiInfoProvider.getRssi()
                delay(3_000)
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            appLogger.info("Initiating connection…")
            try {
                connectSimulatorUseCase()
            } catch (e: Exception) {
                val msg = e.message ?: "Connection failed"
                appLogger.error(msg)
                _errorMessage.value = msg
            }
        }
    }

    fun disconnect() {
        appLogger.info("Disconnecting…")
        disconnectSimulatorUseCase()
    }

    fun startSimulation() {
        viewModelScope.launch {
            _isStarting.value = true
            appLogger.info("Starting simulation…")
            try {
                startSimulationUseCase()
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to start simulation"
                appLogger.error(msg)
                _errorMessage.value = msg
            } finally {
                _isStarting.value = false
            }
        }
    }

    fun stopSimulation() {
        viewModelScope.launch {
            _isStopping.value = true
            appLogger.info("Stopping simulation…")
            try {
                stopSimulationUseCase()
            } finally {
                _isStopping.value = false
            }
        }
    }

    fun triggerPin(pin: DiPin) {
        if (_triggeringPinIds.value.contains(pin.id)) return
        viewModelScope.launch {
            _triggeringPinIds.update { it + pin.id }
            try {
                triggerManualPinUseCase(pin)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to trigger pin ${pin.pinNumber}"
                appLogger.error(msg)
                _errorMessage.value = msg
            } finally {
                _triggeringPinIds.update { it - pin.id }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnectSimulatorUseCase()
    }
}

