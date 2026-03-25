package com.androidtrack.app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.presentation.ui.theme.AndroidTrackTheme
import com.androidtrack.app.presentation.viewmodel.BrokerConfigFormState
import com.androidtrack.app.presentation.viewmodel.DeviceConfigFormState
import com.androidtrack.app.presentation.viewmodel.PinDialogState
import com.androidtrack.app.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val brokerForm by viewModel.brokerForm.collectAsState()
    val deviceForm by viewModel.deviceForm.collectAsState()
    val pins by viewModel.pins.collectAsState()
    val pinDialog by viewModel.pinDialog.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val showConsoleLog by viewModel.showConsoleLog.collectAsState()

    SettingsScreenContent(
        brokerForm = brokerForm,
        deviceForm = deviceForm,
        pins = pins,
        pinDialog = pinDialog,
        snackbarMessage = snackbarMessage,
        showConsoleLog = showConsoleLog,
        deviceClientId = viewModel.deviceClientId,
        updateBrokerHost = viewModel::updateBrokerHost,
        updateBrokerPort = viewModel::updateBrokerPort,
        updateBrokerUsername = viewModel::updateBrokerUsername,
        updateBrokerPassword = viewModel::updateBrokerPassword,
        updateBrokerSecure = viewModel::updateBrokerSecure,
        saveBrokerConfig = viewModel::saveBrokerConfig,
        updateDeviceId = viewModel::updateDeviceId,
        updateDeviceType = viewModel::updateDeviceType,
        saveDeviceConfig = viewModel::saveDeviceConfig,
        showEditPinDialog = viewModel::showEditPinDialog,
        deletePin = viewModel::deletePin,
        showAddPinDialog = viewModel::showAddPinDialog,
        toggleShowConsoleLog = viewModel::toggleShowConsoleLog,
        clearSnackbar = viewModel::clearSnackbar,
        dismissPinDialog = viewModel::dismissPinDialog,
        updatePinNumber = viewModel::updatePinNumber,
        updatePinMode = viewModel::updatePinMode,
        updatePinTimer = viewModel::updatePinTimer,
        updatePinPulse = viewModel::updatePinPulse,
        savePinFromDialog = viewModel::savePinFromDialog
    )
}

@Composable
fun SettingsScreenContent(
    brokerForm: BrokerConfigFormState,
    deviceForm: DeviceConfigFormState,
    pins: List<DiPin>,
    pinDialog: PinDialogState,
    snackbarMessage: String?,
    showConsoleLog: Boolean,
    deviceClientId: String,
    updateBrokerHost: (String) -> Unit,
    updateBrokerPort: (String) -> Unit,
    updateBrokerUsername: (String) -> Unit,
    updateBrokerPassword: (String) -> Unit,
    updateBrokerSecure: (Boolean) -> Unit,
    saveBrokerConfig: () -> Unit,
    updateDeviceId: (String) -> Unit,
    updateDeviceType: (String) -> Unit,
    saveDeviceConfig: () -> Unit,
    showEditPinDialog: (DiPin) -> Unit,
    deletePin: (Int) -> Unit,
    showAddPinDialog: () -> Unit,
    toggleShowConsoleLog: (Boolean) -> Unit,
    clearSnackbar: () -> Unit,
    dismissPinDialog: () -> Unit,
    updatePinNumber: (String) -> Unit,
    updatePinMode: (PinMode) -> Unit,
    updatePinTimer: (String) -> Unit,
    updatePinPulse: (String) -> Unit,
    savePinFromDialog: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Broker section ------------------------------------------
            SectionCard(title = "Broker Settings") {
                OutlinedTextField(
                    value = brokerForm.host,
                    onValueChange = updateBrokerHost,
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.port,
                    onValueChange = updateBrokerPort,
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.username,
                    onValueChange = updateBrokerUsername,
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.password,
                    onValueChange = updateBrokerPassword,
                    label = { Text("Password (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Secure (SSL/TLS)",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = brokerForm.secure,
                        onCheckedChange = updateBrokerSecure
                    )
                }
                Button(
                    onClick = saveBrokerConfig,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Broker Settings") }
            }

            // --- Device section ------------------------------------------
            SectionCard(title = "Device Settings") {
                OutlinedTextField(
                    value = deviceClientId,
                    onValueChange = {},
                    label = { Text("Client ID (auto)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true
                )
                OutlinedTextField(
                    value = deviceForm.deviceId,
                    onValueChange = updateDeviceId,
                    label = { Text("Device ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = deviceForm.deviceType,
                    onValueChange = updateDeviceType,
                    label = { Text("Device Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = saveDeviceConfig,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Device Settings") }
            }

            // --- Pin management section ----------------------------------
            SectionCard(title = "DI Pin Management") {
                pins.forEachIndexed { index, pin ->
                    if (index > 0) HorizontalDivider()
                    PinListItem(
                        pin = pin,
                        onEdit = { showEditPinDialog(pin) },
                        onDelete = { deletePin(pin.id) }
                    )
                }
                if (pins.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = showAddPinDialog,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Add New Pin") }
            }

            // --- App Settings section ------------------------------------
            SectionCard(title = "App Settings") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Console Log",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Display MQTT and application events on the Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showConsoleLog,
                        onCheckedChange = toggleShowConsoleLog
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }

    if (pinDialog.isVisible) {
        PinDialog(
            state = pinDialog,
            onDismiss = dismissPinDialog,
            onPinNumberChange = updatePinNumber,
            onModeChange = updatePinMode,
            onTimerChange = updatePinTimer,
            onPulseChange = updatePinPulse,
            onSave = savePinFromDialog
        )
    }
}

// ---------------------------------------------------------------------------
// Section card wrapper
// ---------------------------------------------------------------------------

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Pin list item row
// ---------------------------------------------------------------------------

@Composable
private fun PinListItem(pin: DiPin, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val modeText = if (pin.mode == PinMode.AUTO) "Auto (${pin.timerMs} ms)" else "Manual"
            Text(
                text = "Pin ${pin.pinNumber}  •  $modeText",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onDelete) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
        }
    }
}

// ---------------------------------------------------------------------------
// Add / Edit Pin dialog
// ---------------------------------------------------------------------------

@Composable
private fun PinDialog(
    state: PinDialogState,
    onDismiss: () -> Unit,
    onPinNumberChange: (String) -> Unit,
    onModeChange: (PinMode) -> Unit,
    onTimerChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isEditing) "Edit DI Pin" else "Add DI Pin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.pinNumber,
                    onValueChange = onPinNumberChange,
                    label = { Text("Pin Number") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.pinNumberError != null,
                    singleLine = true
                )
                state.pinNumberError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode: ${if (state.mode == PinMode.AUTO) "Auto" else "Manual"}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = state.mode == PinMode.AUTO,
                        onCheckedChange = { on ->
                            onModeChange(if (on) PinMode.AUTO else PinMode.MANUAL)
                        }
                    )
                }

                if (state.mode == PinMode.AUTO) {
                    OutlinedTextField(
                        value = state.timerMs,
                        onValueChange = onTimerChange,
                        label = { Text("Timer (ms)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = state.pulseTime,
                    onValueChange = onPulseChange,
                    label = { Text("Pulse Time (ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AndroidTrackTheme {
        SettingsScreenContent(
            brokerForm = BrokerConfigFormState(),
            deviceForm = DeviceConfigFormState(),
            pins = listOf(
                DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL),
                DiPin(id = 2, pinNumber = "02", mode = PinMode.AUTO, timerMs = 5000)
            ),
            pinDialog = PinDialogState(),
            snackbarMessage = null,
            showConsoleLog = true,
            deviceClientId = "preview-client-id",
            updateBrokerHost = {},
            updateBrokerPort = {},
            updateBrokerUsername = {},
            updateBrokerPassword = {},
            updateBrokerSecure = {},
            saveBrokerConfig = {},
            updateDeviceId = {},
            updateDeviceType = {},
            saveDeviceConfig = {},
            showEditPinDialog = {},
            deletePin = {},
            showAddPinDialog = {},
            toggleShowConsoleLog = {},
            clearSnackbar = {},
            dismissPinDialog = {},
            updatePinNumber = {},
            updatePinMode = {},
            updatePinTimer = {},
            updatePinPulse = {},
            savePinFromDialog = {}
        )
    }
}
