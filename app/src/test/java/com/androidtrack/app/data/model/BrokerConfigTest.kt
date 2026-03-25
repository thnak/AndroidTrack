package com.androidtrack.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerConfigTest {

    // --- toUrl() ------------------------------------------------------------------

    @Test
    fun `toUrl returns tcp scheme when secure is false`() {
        val config = BrokerConfig(host = "mqtt.example.com", port = 1883, secure = false)
        assertEquals("tcp://mqtt.example.com:1883", config.toUrl())
    }

    @Test
    fun `toUrl returns ssl scheme when secure is true`() {
        val config = BrokerConfig(host = "mqtt.example.com", port = 8883, secure = true)
        assertEquals("ssl://mqtt.example.com:8883", config.toUrl())
    }

    @Test
    fun `toUrl embeds custom port correctly`() {
        val config = BrokerConfig(host = "192.168.1.100", port = 1234)
        assertEquals("tcp://192.168.1.100:1234", config.toUrl())
    }

    @Test
    fun `toUrl works with IP address host`() {
        val config = BrokerConfig(host = "10.0.0.1", port = 1883, secure = false)
        assertEquals("tcp://10.0.0.1:1883", config.toUrl())
    }

    // --- defaults ------------------------------------------------------------------

    @Test
    fun `default BrokerConfig has expected field values`() {
        val config = BrokerConfig()
        assertEquals("broker.hivemq.com", config.host)
        assertEquals(1883, config.port)
        assertEquals("", config.username)
        assertEquals("", config.password)
        assertEquals("", config.clientId)
        assertFalse(config.secure)
    }

    // --- equality ------------------------------------------------------------------

    @Test
    fun `two BrokerConfigs with same values are equal`() {
        val a = BrokerConfig(host = "host", port = 1883, username = "user", password = "pass", clientId = "id", secure = false)
        val b = BrokerConfig(host = "host", port = 1883, username = "user", password = "pass", clientId = "id", secure = false)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `two BrokerConfigs with different hosts are not equal`() {
        val a = BrokerConfig(host = "host-a")
        val b = BrokerConfig(host = "host-b")
        assertNotEquals(a, b)
    }

    @Test
    fun `two BrokerConfigs differing only in secure flag are not equal`() {
        val a = BrokerConfig(secure = false)
        val b = BrokerConfig(secure = true)
        assertNotEquals(a, b)
    }

    // --- copy ---------------------------------------------------------------------

    @Test
    fun `copy with changed host produces correct URL`() {
        val original = BrokerConfig(host = "original.com", port = 1883)
        val copied = original.copy(host = "new.com", secure = true)
        assertEquals("ssl://new.com:1883", copied.toUrl())
    }
}
