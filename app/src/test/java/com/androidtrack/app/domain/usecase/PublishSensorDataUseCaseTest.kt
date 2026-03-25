package com.androidtrack.app.domain.usecase

import com.androidtrack.app.data.model.MqttConfig
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.repository.MqttRepository
import com.androidtrack.app.domain.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PublishSensorDataUseCaseTest {

    private lateinit var mqttRepository: MqttRepository
    private lateinit var useCase: PublishSensorDataUseCase

    @Before
    fun setup() {
        mqttRepository = mock()
        useCase = PublishSensorDataUseCase(mqttRepository)
    }

    @Test
    fun `connect should call repository connect`() = runTest {
        val config = MqttConfig()
        
        useCase.connect(config)
        
        verify(mqttRepository).connect(config)
    }

    @Test
    fun `publishSensorData should publish to repository`() = runTest {
        val sensorData = SensorData(
            type = "Accelerometer",
            name = "Test Sensor",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = System.currentTimeMillis()
        )
        val topic = "test/topic"
        
        useCase.publishSensorData(sensorData, topic)
        
        verify(mqttRepository).publish(
            org.mockito.kotlin.eq(topic),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

    @Test
    fun `disconnect should call repository disconnect`() {
        useCase.disconnect()
        
        verify(mqttRepository).disconnect()
    }

    @Test
    fun `connectionState should return repository connection state`() {
        val stateFlow = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Connected("test"))
        whenever(mqttRepository.connectionState).thenReturn(stateFlow)
        
        val result = PublishSensorDataUseCase(mqttRepository).connectionState
        
        assertEquals(stateFlow.value, result.value)
    }
}
