package com.androidtrack.app.data.model

/**
 * Domain model for the MQTT broker connection configuration.
 *
 * @param host      Broker IP address or domain name.
 * @param port      TCP port; defaults to 1883 (plain) or 8883 (SSL).
 * @param username  Optional username for broker authentication.
 * @param password  Optional password for broker authentication.
 * @param clientId  MQTT client identifier (populated from device MAC address at runtime).
 * @param secure    When true, the connection uses SSL/TLS (ssl:// scheme); defaults to false.
 */
data class BrokerConfig(
    val host: String = "broker.hivemq.com",
    val port: Int = 1883,
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val secure: Boolean = false
) {
    /** Builds the full broker URL used by the Paho MQTT client. */
    fun toUrl(): String {
        val scheme = if (secure) "ssl" else "tcp"
        return "$scheme://$host:$port"
    }
}
