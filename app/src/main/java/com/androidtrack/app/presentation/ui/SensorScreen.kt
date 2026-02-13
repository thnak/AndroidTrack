package com.androidtrack.app.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidtrack.app.R
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.domain.model.SensorData
import com.androidtrack.app.presentation.viewmodel.SensorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    viewModel: SensorViewModel = hiltViewModel()
) {
    val sensorDataList by viewModel.sensorDataList.collectAsState()
    val mqttConnectionState by viewModel.mqttConnectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            MqttStatusCard(mqttConnectionState)
            Spacer(modifier = Modifier.height(16.dp))
            SensorDataList(sensorDataList)
        }
    }
}

@Composable
fun MqttStatusCard(connectionState: MqttConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is MqttConnectionState.Connected -> Color(0xFF4CAF50)
                is MqttConnectionState.Connecting -> Color(0xFFFFC107)
                is MqttConnectionState.Disconnected -> Color(0xFFCCCCCC)
                is MqttConnectionState.Error -> Color(0xFFF44336)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.mqtt_status),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (connectionState) {
                    is MqttConnectionState.Connected -> stringResource(
                        R.string.mqtt_broker,
                        connectionState.brokerUrl
                    )
                    is MqttConnectionState.Connecting -> stringResource(R.string.connecting)
                    is MqttConnectionState.Disconnected -> stringResource(R.string.disconnected)
                    is MqttConnectionState.Error -> connectionState.message
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun SensorDataList(sensorDataList: List<SensorData>) {
    Card(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sensor_data),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (sensorDataList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_sensors),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sensorDataList) { sensorData ->
                        SensorDataItem(sensorData)
                    }
                }
            }
        }
    }
}

@Composable
fun SensorDataItem(sensorData: SensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = sensorData.type,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = sensorData.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.sensor_value, sensorData.valuesAsString()),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
