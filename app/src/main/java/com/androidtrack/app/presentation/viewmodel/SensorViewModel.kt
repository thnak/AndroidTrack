package com.androidtrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidtrack.app.domain.model.SensorData
import com.androidtrack.app.domain.usecase.ObserveSensorDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for observing device sensor readings.
 *
 * Sensor data is collected and stored in-memory for future display.
 * MQTT publishing of sensor readings is intentionally omitted here –
 * it will be wired up in a dedicated phase once the Edge Simulator UI is complete.
 */
@HiltViewModel
class SensorViewModel @Inject constructor(
    private val observeSensorDataUseCase: ObserveSensorDataUseCase
) : ViewModel() {

    private val _sensorDataList = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorDataList: StateFlow<List<SensorData>> = _sensorDataList.asStateFlow()

    init {
        observeSensors()
    }

    private fun observeSensors() {
        viewModelScope.launch {
            observeSensorDataUseCase()
                .collect { sensorData ->
                    // Keep the latest reading per sensor type (max 20 entries)
                    _sensorDataList.update { currentList ->
                        val filtered = currentList.filter { it.type != sensorData.type }
                        (filtered + sensorData).takeLast(20)
                    }
                }
        }
    }

    fun getAvailableSensorCount(): Int {
        return observeSensorDataUseCase.getAvailableSensors().size
    }
}
