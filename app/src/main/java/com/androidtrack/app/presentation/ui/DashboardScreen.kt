package com.androidtrack.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.WifiInfoProvider
import com.androidtrack.app.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val pins by viewModel.pins.collectAsState()
    val rssi by viewModel.rssi.collectAsState()
    val logMessages by viewModel.logMessages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // --- Status header -----------------------------------------------
            StatusHeaderRow(connectionState, rssi)

            Spacer(modifier = Modifier.height(10.dp))

            // --- Control buttons --------------------------------------------
            ControlButtonsRow(
                connectionState = connectionState,
                isRunning = isRunning,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onStart = viewModel::startSimulation,
                onStop = viewModel::stopSimulation
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- DI Pins list -----------------------------------------------
            Text("DI Pins", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))

            if (pins.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pins configured.\nGo to Settings to add pins.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pins, key = { it.id }) { pin ->
                        DiPinCard(
                            pin = pin,
                            isSimulationRunning = isRunning,
                            onTrigger = { viewModel.triggerPin(pin) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Console log ------------------------------------------------
            ConsoleLogSection(logMessages)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

// ---------------------------------------------------------------------------
// Status header
// ---------------------------------------------------------------------------

@Composable
private fun StatusHeaderRow(connectionState: MqttConnectionState, rssi: Int) {
    val (ledColor, statusText) = when (connectionState) {
        is MqttConnectionState.Connected -> Color(0xFF2E7D32) to "Connected"
        is MqttConnectionState.Connecting -> Color(0xFFF9A825) to "Connecting..."
        is MqttConnectionState.Disconnected -> Color(0xFF9E9E9E) to "Disconnected"
        is MqttConnectionState.Error -> Color(0xFFC62828) to "Error"
    }

    val rssiText = if (rssi == WifiInfoProvider.RSSI_UNKNOWN) "RSSI: N/A" else "RSSI: ${rssi} dBm"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(ledColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rssiText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Control buttons
// ---------------------------------------------------------------------------

@Composable
private fun ControlButtonsRow(
    connectionState: MqttConnectionState,
    isRunning: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isConnected = connectionState is MqttConnectionState.Connected
    val isConnecting = connectionState is MqttConnectionState.Connecting

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onConnect,
                enabled = !isConnected && !isConnecting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("Connect") }

            OutlinedButton(
                onClick = onDisconnect,
                enabled = isConnected || isConnecting,
                modifier = Modifier.weight(1f)
            ) { Text("Disconnect") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = isConnected && !isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) { Text("Start Simulation") }

            OutlinedButton(
                onClick = onStop,
                enabled = isRunning,
                modifier = Modifier.weight(1f)
            ) { Text("Stop Simulation") }
        }
    }
}

// ---------------------------------------------------------------------------
// DI Pin card
// ---------------------------------------------------------------------------

@Composable
fun DiPinCard(pin: DiPin, isSimulationRunning: Boolean, onTrigger: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val modeLabel = if (pin.mode == PinMode.AUTO) "Auto (${pin.timerMs} ms)" else "Manual"
                Text(
                    text = "Pin ${pin.pinNumber}  •  $modeLabel",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Count: ${pin.shootCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when (pin.mode) {
                PinMode.MANUAL -> {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrigger()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) { Text("Trigger") }
                }
                PinMode.AUTO -> {
                    Text(
                        text = if (isSimulationRunning) "Running…" else "Idle",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSimulationRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Console log
// ---------------------------------------------------------------------------

@Composable
private fun ConsoleLogSection(messages: List<String>) {
    Text("Console Log", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(4.dp))

    val scrollState = rememberScrollState()
    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 160.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            if (messages.isEmpty()) {
                Text(
                    text = "No messages yet…",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF9E9E9E)
                )
            } else {
                messages.forEach { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF80CBC4)
                    )
                }
            }
        }
    }
}
