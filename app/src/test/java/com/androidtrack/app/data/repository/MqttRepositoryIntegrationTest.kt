package com.androidtrack.app.data.repository

import app.cash.turbine.test
import com.androidtrack.app.data.model.MqttConfig
import com.androidtrack.app.data.model.MqttConnectionState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for [MqttRepository] using an embedded Moquette MQTT broker.
 *
 * Each test verifies actual MQTT protocol behaviour (connection lifecycle,
 * message delivery, error handling) without mocking the repository layer.
 */
class MqttRepositoryIntegrationTest {

    companion object {
        private const val TEST_PORT = 1884
        private const val BROKER_URL = "tcp://localhost:$TEST_PORT"

        private val broker = Server()

        @BeforeClass
        @JvmStatic
        fun startBroker() {
            val tempDir = Files.createTempDirectory("moquette-test").toAbsolutePath().toString()
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

    private lateinit var repository: MqttRepository

    /** Unique client ID avoids cross-test session collisions on the broker. */
    private fun testClientId(tag: String = "") =
        "test-${System.nanoTime()}${if (tag.isNotEmpty()) "-$tag" else ""}"

    private fun testConfig(tag: String = "") = MqttConfig(
        brokerUrl = BROKER_URL,
        clientId = testClientId(tag),
        topic = "test/topic"
    )

    @Before
    fun createRepository() {
        repository = MqttRepository()
    }

    @After
    fun cleanupRepository() {
        repository.disconnect()
    }

    // --- initial state -------------------------------------------------------

    @Test
    fun `initial connection state is Disconnected`() {
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)
    }

    // --- successful connection -----------------------------------------------

    @Test
    fun `connect to available broker transitions state through Connecting then Connected`() =
        runTest {
            val config = testConfig("connect")

            repository.connectionState.test {
                // StateFlow always replays the current value to a new collector.
                assertEquals(MqttConnectionState.Disconnected, awaitItem())

                // Run connect on the IO dispatcher so the blocking waitForCompletion()
                // inside the Paho library does not stall the test dispatcher.
                launch(Dispatchers.IO) { repository.connect(config) }

                assertEquals(MqttConnectionState.Connecting, awaitItem())

                val state = awaitItem()
                assertTrue(
                    "Expected Connected but was $state",
                    state is MqttConnectionState.Connected
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `connect to available broker stores correct broker URL in Connected state`() = runTest {
        val config = testConfig("url-check")

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(config) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val connected = awaitItem() as? MqttConnectionState.Connected
            assertNotNull("Expected Connected state but got something else", connected)
            assertEquals(BROKER_URL, connected!!.brokerUrl)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- error handling ------------------------------------------------------

    @Test
    fun `connect to unavailable broker transitions state to Error`() = runTest {
        val badConfig = MqttConfig(
            brokerUrl = "tcp://localhost:19999",  // nothing listening here
            clientId = testClientId("bad-broker"),
            topic = "test/topic"
        )

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(badConfig) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val state = awaitItem()
            assertTrue(
                "Expected Error but was $state",
                state is MqttConnectionState.Error
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- disconnection -------------------------------------------------------

    @Test
    fun `disconnect after connect transitions state to Disconnected`() = runTest {
        val config = testConfig("disconnect")

        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(config) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val connectedState = awaitItem()
            assertTrue(
                "Expected Connected before disconnecting, but was $connectedState",
                connectedState is MqttConnectionState.Connected
            )

            repository.disconnect()

            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- publish -------------------------------------------------------------

    @Test
    fun `publish message when connected delivers message to subscriber`() = runTest {
        val topic = "integration/publish-test"
        val payload = """{"sensor":"accel","values":[1.0,2.0,3.0]}"""

        // Set up a synchronous Paho subscriber before connecting the repository.
        val receivedPayload = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)

        val subscriber = MqttClient(BROKER_URL, testClientId("subscriber"), MemoryPersistence())
        subscriber.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            override fun messageArrived(t: String, message: MqttMessage) {
                receivedPayload.set(String(message.payload))
                latch.countDown()
            }
        })
        subscriber.connect(MqttConnectOptions().apply { isCleanSession = true })
        subscriber.subscribe(topic, 1)

        // Connect the repository and wait until Connected.
        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            // MqttConfig.topic is a convenience field; MqttRepository.publish() always
            // receives the target topic explicitly, so the two values are independent.
            launch(Dispatchers.IO) { repository.connect(testConfig("publisher")) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())

            val state = awaitItem()
            assertTrue(
                "Repository never reached Connected state, got $state",
                state is MqttConnectionState.Connected
            )

            cancelAndIgnoreRemainingEvents()
        }

        repository.publish(topic, payload, qos = 1)

        // Wait for the subscriber to receive the message (up to 3 s real time).
        assertTrue(
            "Subscriber did not receive the published message within 3 s",
            latch.await(3, TimeUnit.SECONDS)
        )
        assertEquals(payload, receivedPayload.get())

        subscriber.unsubscribe(topic)
        subscriber.disconnect()
    }

    @Test
    fun `publish when not connected does not throw`() = runTest {
        // Repository has never been connected – publish should silently do nothing.
        repository.publish("some/topic", "payload", qos = 1)
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)
    }

    // --- reconnect -----------------------------------------------------------

    @Test
    fun `disconnect and reconnect establishes a new connection`() = runTest {
        // First connection.
        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(testConfig("rc-first")) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())
            val firstState = awaitItem()
            assertTrue("First connect failed, got $firstState", firstState is MqttConnectionState.Connected)

            cancelAndIgnoreRemainingEvents()
        }

        repository.disconnect()
        assertEquals(MqttConnectionState.Disconnected, repository.connectionState.value)

        // Second connection with a different client ID.
        repository.connectionState.test {
            assertEquals(MqttConnectionState.Disconnected, awaitItem())

            launch(Dispatchers.IO) { repository.connect(testConfig("rc-second")) }

            assertEquals(MqttConnectionState.Connecting, awaitItem())
            val secondState = awaitItem()
            assertTrue("Reconnect failed, got $secondState", secondState is MqttConnectionState.Connected)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
