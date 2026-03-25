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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
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
                    onValueChange = viewModel::updateBrokerHost,
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.port,
                    onValueChange = viewModel::updateBrokerPort,
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.username,
                    onValueChange = viewModel::updateBrokerUsername,
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = brokerForm.password,
                    onValueChange = viewModel::updateBrokerPassword,
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
                        onCheckedChange = viewModel::updateBrokerSecure
                    )
                }
                Button(
                    onClick = viewModel::saveBrokerConfig,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Broker Settings") }
            }

            // --- Device section ------------------------------------------
            SectionCard(title = "Device Settings") {
                OutlinedTextField(
                    value = viewModel.deviceClientId,
                    onValueChange = {},
                    label = { Text("Client ID (auto)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true
                )
                OutlinedTextField(
                    value = deviceForm.deviceId,
                    onValueChange = viewModel::updateDeviceId,
                    label = { Text("Device ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = deviceForm.deviceType,
                    onValueChange = viewModel::updateDeviceType,
                    label = { Text("Device Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = viewModel::saveDeviceConfig,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Device Settings") }
            }

            // --- Pin management section ----------------------------------
            SectionCard(title = "DI Pin Management") {
                pins.forEachIndexed { index, pin ->
                    if (index > 0) HorizontalDivider()
                    PinListItem(
                        pin = pin,
                        onEdit = { viewModel.showEditPinDialog(pin) },
                        onDelete = { viewModel.deletePin(pin.id) }
                    )
                }
                if (pins.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = viewModel::showAddPinDialog,
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
                        onCheckedChange = viewModel::toggleShowConsoleLog
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
            onDismiss = viewModel::dismissPinDialog,
            onPinNumberChange = viewModel::updatePinNumber,
            onModeChange = viewModel::updatePinMode,
            onTimerChange = viewModel::updatePinTimer,
            onPulseChange = viewModel::updatePinPulse,
            onSave = viewModel::savePinFromDialog
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
