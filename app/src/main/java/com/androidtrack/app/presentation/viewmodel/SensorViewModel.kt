package com.androidtrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidtrack.app.data.model.MqttConfig
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.domain.model.SensorData
import com.androidtrack.app.domain.usecase.ObserveSensorDataUseCase
import com.androidtrack.app.domain.usecase.PublishSensorDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing sensor and MQTT state
 */
@HiltViewModel
class SensorViewModel @Inject constructor(
    private val observeSensorDataUseCase: ObserveSensorDataUseCase,
    private val publishSensorDataUseCase: PublishSensorDataUseCase
) : ViewModel() {

    private val _sensorDataList = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorDataList: StateFlow<List<SensorData>> = _sensorDataList.asStateFlow()

    val mqttConnectionState: StateFlow<MqttConnectionState> = 
        publishSensorDataUseCase.connectionState

    private val mqttConfig = MqttConfig()

    init {
        connectToMqtt()
        observeSensors()
    }

    private fun connectToMqtt() {
        viewModelScope.launch {
            publishSensorDataUseCase.connect(mqttConfig)
        }
    }

    private fun observeSensors() {
        viewModelScope.launch {
            observeSensorDataUseCase()
                .collect { sensorData ->
                    // Keep only the latest reading from each sensor
                    _sensorDataList.update { currentList ->
                        val filtered = currentList.filter { it.type != sensorData.type }
                        (filtered + sensorData).takeLast(20) // Keep max 20 sensors
                    }
                    
                    // Publish to MQTT if connected
                    if (mqttConnectionState.value is MqttConnectionState.Connected) {
                        publishSensorDataUseCase.publishSensorData(
                            sensorData,
                            mqttConfig.topic
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        publishSensorDataUseCase.disconnect()
    }

    fun getAvailableSensorCount(): Int {
        return observeSensorDataUseCase.getAvailableSensors().size
    }
}
