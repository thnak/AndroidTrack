package com.androidtrack.app.presentation.viewmodel

import android.hardware.Sensor
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.domain.model.SensorData
import com.androidtrack.app.domain.usecase.ObserveSensorDataUseCase
import com.androidtrack.app.domain.usecase.PublishSensorDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModelTest {

    private lateinit var observeSensorDataUseCase: ObserveSensorDataUseCase
    private lateinit var publishSensorDataUseCase: PublishSensorDataUseCase
    private lateinit var viewModel: SensorViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        observeSensorDataUseCase = mock()
        publishSensorDataUseCase = mock()
        
        val connectionStateFlow = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
        whenever(publishSensorDataUseCase.connectionState).thenReturn(connectionStateFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial sensor data list should be empty`() {
        val sensorDataFlow = flowOf<SensorData>()
        whenever(observeSensorDataUseCase.invoke()).thenReturn(sensorDataFlow)
        whenever(observeSensorDataUseCase.getAvailableSensors()).thenReturn(emptyList())
        
        viewModel = SensorViewModel(observeSensorDataUseCase, publishSensorDataUseCase)
        
        assertEquals(emptyList<SensorData>(), viewModel.sensorDataList.value)
    }

    @Test
    fun `getAvailableSensorCount should return count from use case`() {
        val sensors = listOf(mock<Sensor>(), mock<Sensor>())
        whenever(observeSensorDataUseCase.getAvailableSensors()).thenReturn(sensors)
        whenever(observeSensorDataUseCase.invoke()).thenReturn(flowOf())
        
        viewModel = SensorViewModel(observeSensorDataUseCase, publishSensorDataUseCase)
        
        assertEquals(2, viewModel.getAvailableSensorCount())
    }
}
