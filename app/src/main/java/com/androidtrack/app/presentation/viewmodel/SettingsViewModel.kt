package com.androidtrack.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidtrack.app.R
import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.AppSettingsRepository
import com.androidtrack.app.data.repository.ConfigRepository
import com.androidtrack.app.data.repository.WifiInfoProvider
import com.androidtrack.app.domain.usecase.ManagePinsUseCase
import com.androidtrack.app.domain.usecase.SaveBrokerConfigUseCase
import com.androidtrack.app.domain.usecase.SaveDeviceConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Form state classes – kept in this file to stay close to the ViewModel that
// owns them.
// ---------------------------------------------------------------------------

data class BrokerConfigFormState(
    val host: String = "broker.hivemq.com",
    val port: String = "1883",
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val secure: Boolean = false
)

data class DeviceConfigFormState(
    val deviceId: String = "DEV-001",
    val deviceType: String = "GATEWAY-V3"
)

data class PinDialogState(
    val isVisible: Boolean = false,
    val isEditing: Boolean = false,
    val editId: Int = 0,
    val pinNumber: String = "",
    val mode: PinMode = PinMode.MANUAL,
    val timerMs: String = "5000",
    val pulseTime: String = "1000",
    val pinNumberError: String? = null
)

// ---------------------------------------------------------------------------

/**
 * ViewModel for the Settings screen.
 *
 * Loads persisted [BrokerConfig] and [DeviceConfig] from the database on init and exposes
 * mutable form states for editing. Provides actions to validate and save each section, and
 * full CRUD for DI pins through a dialog flow.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val saveBrokerConfigUseCase: SaveBrokerConfigUseCase,
    private val saveDeviceConfigUseCase: SaveDeviceConfigUseCase,
    private val managePinsUseCase: ManagePinsUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val wifiInfoProvider: WifiInfoProvider,
    @ApplicationContext context: Context
) : ViewModel() {

    /** Stable device identifier used as the default MQTT Client ID (derived from MAC address). */
    val deviceClientId: String = wifiInfoProvider.getMacAddress()

    private val appContext: Context = context

    private val _brokerForm = MutableStateFlow(BrokerConfigFormState(clientId = deviceClientId))
    val brokerForm: StateFlow<BrokerConfigFormState> = _brokerForm.asStateFlow()

    private val _deviceForm = MutableStateFlow(DeviceConfigFormState())
    val deviceForm: StateFlow<DeviceConfigFormState> = _deviceForm.asStateFlow()

    val pins: StateFlow<List<DiPin>> = managePinsUseCase.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _pinDialog = MutableStateFlow(PinDialogState())
    val pinDialog: StateFlow<PinDialogState> = _pinDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    /** Mirrors [AppSettingsRepository.showConsoleLog] for the UI. */
    val showConsoleLog: StateFlow<Boolean> = appSettingsRepository.showConsoleLog

    fun toggleShowConsoleLog(enabled: Boolean) {
        appSettingsRepository.setShowConsoleLog(enabled)
    }

    init {
        viewModelScope.launch {
            configRepository.observeBrokerConfig().collect { config ->
                if (config != null) {
                    _brokerForm.value = BrokerConfigFormState(
                        host = config.host,
                        port = config.port.toString(),
                        username = config.username,
                        password = config.password,
                        clientId = config.clientId.ifBlank { deviceClientId },
                        secure = config.secure
                    )
                }
            }
        }
        viewModelScope.launch {
            configRepository.observeDeviceConfig().collect { config ->
                if (config != null) {
                    _deviceForm.value = DeviceConfigFormState(
                        deviceId = config.deviceId,
                        deviceType = config.deviceType
                    )
                }
            }
        }
    }

    // --- Broker form ---------------------------------------------------------

    fun updateBrokerHost(value: String) = _brokerForm.update { it.copy(host = value) }
    fun updateBrokerPort(value: String) = _brokerForm.update { it.copy(port = value) }
    fun updateBrokerUsername(value: String) = _brokerForm.update { it.copy(username = value) }
    fun updateBrokerPassword(value: String) = _brokerForm.update { it.copy(password = value) }
    fun updateClientId(value: String) = _brokerForm.update { it.copy(clientId = value) }

    fun updateBrokerSecure(secure: Boolean) {
        _brokerForm.update { state ->
            val autoPort = if (secure) "8883" else "1883"
            val newPort = if (state.port == "1883" || state.port == "8883") autoPort else state.port
            state.copy(secure = secure, port = newPort)
        }
    }

    fun saveBrokerConfig() {
        viewModelScope.launch {
            val form = _brokerForm.value
            val port = form.port.toIntOrNull() ?: 0
            val result = saveBrokerConfigUseCase(
                BrokerConfig(
                    host = form.host,
                    port = port,
                    username = form.username,
                    password = form.password,
                    clientId = form.clientId.ifBlank { deviceClientId },
                    secure = form.secure
                )
            )
            _snackbarMessage.value = result.fold(
                onSuccess = { appContext.getString(R.string.msg_broker_saved) },
                onFailure = { it.message }
            )
        }
    }

    // --- Device form ---------------------------------------------------------

    fun updateDeviceId(value: String) = _deviceForm.update { it.copy(deviceId = value) }
    fun updateDeviceType(value: String) = _deviceForm.update { it.copy(deviceType = value) }

    fun saveDeviceConfig() {
        viewModelScope.launch {
            val form = _deviceForm.value
            val result = saveDeviceConfigUseCase(
                DeviceConfig(deviceId = form.deviceId, deviceType = form.deviceType)
            )
            _snackbarMessage.value = result.fold(
                onSuccess = { appContext.getString(R.string.msg_device_saved) },
                onFailure = { it.message }
            )
        }
    }

    // --- Pin dialog ----------------------------------------------------------

    fun showAddPinDialog() {
        _pinDialog.value = PinDialogState(isVisible = true)
    }

    fun showEditPinDialog(pin: DiPin) {
        _pinDialog.value = PinDialogState(
            isVisible = true,
            isEditing = true,
            editId = pin.id,
            pinNumber = pin.pinNumber,
            mode = pin.mode,
            timerMs = pin.timerMs.toString(),
            pulseTime = pin.pulseTime.toString()
        )
    }

    fun dismissPinDialog() {
        _pinDialog.value = PinDialogState()
    }

    fun updatePinNumber(value: String) =
        _pinDialog.update { it.copy(pinNumber = value, pinNumberError = null) }

    fun updatePinMode(mode: PinMode) = _pinDialog.update { it.copy(mode = mode) }
    fun updatePinTimer(value: String) = _pinDialog.update { it.copy(timerMs = value) }
    fun updatePinPulse(value: String) = _pinDialog.update { it.copy(pulseTime = value) }

    fun savePinFromDialog() {
        viewModelScope.launch {
            val dialog = _pinDialog.value
            val timerMs = dialog.timerMs.toLongOrNull() ?: 5_000L
            val pulseTime = dialog.pulseTime.toLongOrNull() ?: 1_000L

            val pin = DiPin(
                id = if (dialog.isEditing) dialog.editId else 0,
                pinNumber = dialog.pinNumber.trim(),
                mode = dialog.mode,
                timerMs = timerMs,
                pulseTime = pulseTime
            )

            val result = if (dialog.isEditing) {
                managePinsUseCase.updatePin(pin)
            } else {
                managePinsUseCase.addPin(pin)
            }

            result.fold(
                onSuccess = { dismissPinDialog() },
                onFailure = { e -> _pinDialog.update { it.copy(pinNumberError = e.message) } }
            )
        }
    }

    fun deletePin(id: Int) {
        viewModelScope.launch { managePinsUseCase.deletePin(id) }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
