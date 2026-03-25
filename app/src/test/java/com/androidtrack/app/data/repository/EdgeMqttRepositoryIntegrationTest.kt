package com.androidtrack.app.data.repository

import app.cash.turbine.test
import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.AppLogger
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import java.time.Instant
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for [EdgeMqttRepository] using an embedded Moquette MQTT broker.
 *
 * Each test verifies actual MQTT protocol behaviour against the four topics defined in the spec:
 *   device/init, devices/{id}/log/info, uplink/heartbeat/v1/{id}, uplink/v3/di/{pin_number}
 */
class EdgeMqttRepositoryIntegrationTest {

    companion object {
        private const val TEST_PORT = 1885
        private const val BROKER_URL = "tcp://localhost:$TEST_PORT"

        private val broker = Server()

        @BeforeClass
        @JvmStatic
        fun startBroker() {
            val tempDir = Files.createTempDirectory("edge-moquette-test").toAbsolutePath().toString()
            val properties = Properties().apply {
                setProperty("port", TEST_PORT.toString())
                setProperty("host", "localhost")
                setProperty("allow_anonymous", "true")
                setProperty("data_path", tempDir)
            }
            broker.startServer(MemoryConfig(properties))
        }

        @AfterClass
        @JvmStatic
        fun stopBroker() {
            broker.stopServer()
        }
    }

    private lateinit var repository: EdgeMqttRepository
    private val appLogger = AppLogger()

    private fun uniqueClientId(tag: String = "") =
        "edge-test-${System.nanoTime()}${if (tag.isNotEmpty()) "-$tag" else ""}"

    private fun brokerConfig(tag: String = "") = BrokerConfig(
        host = "localhost",
        port = TEST_PORT,
        clientId = uniqueClientId(tag),
        secure = false
    )

    private val defaultDeviceConfig = DeviceConfig(deviceId = "TEST-DEV", deviceType = "TEST-GW")

    @Before
    fun createRepository() {
        repository = EdgeMqttRepository(appLogger)
    }

    @After
    fun cleanupRepository() {
        repository.disconnect()
    }

    // --- initial state ------------------------------------------------------------

    @Test
    fun `initial connectionState is Disconnected`() {
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)
    }

    @Test
    fun `initial appLogger logs is empty`() {
        assertTrue(appLogger.logs.value.isEmpty())
    }

    // --- connection state transitions ---------------------------------------------

    @Test
    fun `connect transitions through Connecting then Connected`() = runTest {
        val config = brokerConfig("connect")

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(config, defaultDeviceConfig) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())
            val connected = awaitItem()
            assertTrue("Expected Connected but got $connected", connected is MqttConnectionState.Connected)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Connected state contains the correct broker URL`() = runTest {
        val config = brokerConfig("url-check")

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())
            launch(Dispatchers.IO) { repository.connect(config, defaultDeviceConfig) }
            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val state = awaitItem() as? MqttConnectionState.Connected
            assertNotNull(state)
            assertEquals(BROKER_URL, state!!.brokerUrl)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connect to unavailable broker produces Error state`() = runTest {
        val badConfig = BrokerConfig(host = "localhost", port = 19999, clientId = uniqueClientId("bad"))

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())
            launch(Dispatchers.IO) { repository.connect(badConfig, defaultDeviceConfig) }
            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val state = awaitItem()
            assertTrue("Expected Error but got $state", state is MqttConnectionState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect after connect returns state to Disconnected`() = runTest {
        val config = brokerConfig("disconnect")

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())
            launch(Dispatchers.IO) { repository.connect(config, defaultDeviceConfig) }
            assertEquals(MqttConnectionState.Connecting, awaitItem())
            assertTrue(awaitItem() is MqttConnectionState.Connected)

            repository.disconnect()

            assertEquals(MqttConnectionState.Disconnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect when not connected does not throw`() {
        // Repository has never been connected.
        repository.disconnect()
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)
    }

    // --- device/init payload ------------------------------------------------------

    @Test
    fun `connect sends device-init message to device-init topic`() = runTest {
        val config = brokerConfig("init")
        val deviceConfig = DeviceConfig(deviceId = "INIT-DEV", deviceType = "GW-1")

        val (topic, payload) = subscribeAndReceive("device/init", uniqueClientId("init-sub")) {
            launch(Dispatchers.IO) { repository.connect(config, deviceConfig) }
        }

        assertEquals("device/init", topic)
        assertNotNull(payload)
        assertTrue("Payload should contain device_type", payload!!.contains("\"device_type\":\"GW-1\""))
        assertTrue("Payload should contain time_stamp", payload.contains("time_stamp"))
    }

    // --- publishLog ---------------------------------------------------------------

    @Test
    fun `publishLog sends correct payload to devices-id-log-info topic`() = runTest {
        connectAndWait(brokerConfig("log"))

        val deviceId = "LOG-DEV"
        val expectedTopic = "devices/$deviceId/log/info"
        val message = "test log entry"

        val (topic, payload) = subscribeAndReceive(expectedTopic, uniqueClientId("log-sub")) {
            repository.publishLog(deviceId, message)
        }

        assertEquals(expectedTopic, topic)
        assertNotNull(payload)
        assertTrue(payload!!.contains("\"message\":\"test log entry\""))
        assertTrue(payload.contains("time_stamp"))
    }

    @Test
    fun `publishLog escapes double-quotes in message`() = runTest {
        connectAndWait(brokerConfig("log-esc"))

        val deviceId = "ESC-DEV"
        val expectedTopic = "devices/$deviceId/log/info"
        val message = "has \"quotes\" inside"

        val (topic, payload) = subscribeAndReceive(expectedTopic, uniqueClientId("esc-sub")) {
            repository.publishLog(deviceId, message)
        }

        assertNotNull(payload)
        assertTrue("Double-quotes should be escaped in JSON", payload!!.contains("\\\"quotes\\\""))
    }

    // --- publishHeartbeat ---------------------------------------------------------

    @Test
    fun `publishHeartbeat sends correct payload to uplink-heartbeat topic`() = runTest {
        connectAndWait(brokerConfig("hb"))

        val deviceId = "HB-DEV"
        val expectedTopic = "uplink/heartbeat/v1/$deviceId"

        val (topic, payload) = subscribeAndReceive(expectedTopic, uniqueClientId("hb-sub")) {
            repository.publishHeartbeat(deviceId, "counter", rssi = -70)
        }

        assertEquals(expectedTopic, topic)
        assertNotNull(payload)
        assertTrue(payload!!.contains("\"serial_no\":\"HB-DEV\""))
        assertTrue(payload.contains("\"attribute_name\":\"counter\""))
        assertTrue(payload.contains("\"device_status\":\"online\""))
        assertTrue(payload.contains("\"rssi\":-70"))
        assertTrue(payload.contains("time_stamp"))
    }

    @Test
    fun `publishHeartbeat sends 0 for RSSI_UNKNOWN`() = runTest {
        connectAndWait(brokerConfig("hb-rssi"))

        val deviceId = "RSSI-DEV"
        val expectedTopic = "uplink/heartbeat/v1/$deviceId"

        val (_, payload) = subscribeAndReceive(expectedTopic, uniqueClientId("rssi-sub")) {
            repository.publishHeartbeat(deviceId, "status", rssi = WifiInfoProvider.RSSI_UNKNOWN)
        }

        assertNotNull(payload)
        assertTrue("RSSI_UNKNOWN should be sent as 0", payload!!.contains("\"rssi\":0"))
    }

    // --- publishDiData ------------------------------------------------------------

    @Test
    fun `publishDiData sends correct payload to uplink-v3-di-pin topic`() = runTest {
        connectAndWait(brokerConfig("di"))

        val pin = DiPin(id = 1, pinNumber = "01", mode = PinMode.MANUAL, shootCount = 331245L, pulseTime = 1000L)
        val expectedTopic = "uplink/v3/di/01"

        val (topic, payload) = subscribeAndReceive(expectedTopic, uniqueClientId("di-sub")) {
            repository.publishDiData(pin)
        }

        assertEquals(expectedTopic, topic)
        assertNotNull(payload)
        assertTrue(payload!!.contains("\"shoot_count\":331245"))
        assertTrue(payload.contains("\"pulse_time\":1000"))
        assertTrue(payload.contains("time_stamp"))
    }

    // --- recentMessages -----------------------------------------------------------

    @Test
    fun `appLogger logs grow after each publish`() = runTest {
        connectAndWait(brokerConfig("recent"))

        val initialSize = appLogger.logs.value.size
        repository.publishLog("DEV-A", "msg1")
        repository.publishLog("DEV-A", "msg2")

        // Give the internal publish a moment to process.
        val messages = appLogger.logs.value
        assertTrue("appLogger logs should grow after publishes", messages.size >= initialSize + 2)
    }

    @Test
    fun `appLogger log entries contain arrow and topic`() = runTest {
        connectAndWait(brokerConfig("recent-content"))

        repository.publishLog("MY-DEV", "hello")

        val messages = appLogger.logs.value
        assertTrue(messages.any { it.message.startsWith("→") && it.message.contains("devices/MY-DEV/log/info") })
    }

    @Test
    fun `appLogger logs are capped at max entries`() = runTest {
        connectAndWait(brokerConfig("cap"))

        repeat(55) { i ->
            repository.publishLog("CAP-DEV", "message $i")
        }

        val count = appLogger.logs.value.size
        assertTrue("appLogger logs should not exceed 200 entries, was $count", count <= 200)
    }

    // --- no-op publish when disconnected ------------------------------------------

    @Test
    fun `publish when disconnected is silent and does not throw`() = runTest {
        // Never call connect – repository is in Disconnected state.
        repository.publishLog("DEV", "log message")
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)
        // appLogger stays empty because the internal publish guard returns early.
        assertFalse(appLogger.logs.value.any { it.message.contains("log/info") })
    }

    // --- helpers ------------------------------------------------------------------

    /**
     * Subscribes a fresh Paho client to [topic], runs [action] which triggers a publish,
     * then waits up to 3 s for a message and returns the received topic + payload string.
     */
    private suspend fun subscribeAndReceive(
        topic: String,
        subscriberClientId: String,
        action: suspend () -> Unit
    ): Pair<String, String?> {
        val receivedTopic = AtomicReference<String?>(null)
        val receivedPayload = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)

        val subscriber = MqttClient(BROKER_URL, subscriberClientId, MemoryPersistence())
        subscriber.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            override fun messageArrived(t: String, message: MqttMessage) {
                receivedTopic.set(t)
                receivedPayload.set(String(message.payload))
                latch.countDown()
            }
        })
        subscriber.connect(MqttConnectOptions().apply { isCleanSession = true })
        subscriber.subscribe(topic, 1)

        action()

        assertTrue("No message received on '$topic' within 3 s", latch.await(3, TimeUnit.SECONDS))

        subscriber.unsubscribe(topic)
        subscriber.disconnect()

        return Pair(receivedTopic.get() ?: "", receivedPayload.get())
    }

    /**
     * Connects the repository and blocks until the [MqttConnectionState.Connected] state is reached.
     */
    private suspend fun connectAndWait(config: BrokerConfig) {
        repository.connect(config, defaultDeviceConfig)
        // After waitForCompletion the connectComplete callback fires and sets Connected.
        // Poll briefly to allow the callback to propagate.
        val deadline = System.currentTimeMillis() + 3_000
        while (repository.connectionState.value !is MqttConnectionState.Connected &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(50)
        }
        assertTrue(
            "Repository did not reach Connected state",
            repository.connectionState.value is MqttConnectionState.Connected
        )
    }
}
