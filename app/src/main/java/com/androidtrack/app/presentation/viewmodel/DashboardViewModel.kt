package com.androidtrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 *
 * Exposes reactive state for:
 * - MQTT connection status
 * - Simulation running flag
 * - Live DI pin list (shoot-count updated in real-time via Room Flow)
 * - Wi-Fi RSSI (polled every 3 s)
 * - Console log of recent published messages
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
    private val wifiInfoProvider: WifiInfoProvider
) : ViewModel() {

    val connectionState: StateFlow<MqttConnectionState> = edgeMqttRepository.connectionState

    val isRunning: StateFlow<Boolean> = simulationManager.isRunning

    val pins: StateFlow<List<DiPin>> = managePinsUseCase.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val logMessages: StateFlow<List<String>> = edgeMqttRepository.recentMessages

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
            try {
                connectSimulatorUseCase()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun disconnect() {
        disconnectSimulatorUseCase()
    }

    fun startSimulation() {
        viewModelScope.launch {
            try {
                startSimulationUseCase()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun stopSimulation() {
        stopSimulationUseCase()
    }

    fun triggerPin(pin: DiPin) {
        viewModelScope.launch {
            try {
                triggerManualPinUseCase(pin)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Stop simulation & disconnect when ViewModel is cleared (app backgrounded / destroyed)
        disconnectSimulatorUseCase()
    }
}
