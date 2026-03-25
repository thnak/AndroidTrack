package com.androidtrack.app.data.model

/**
 * Operating mode for a virtual Digital Input pin.
 */
enum class PinMode {
    /** The user manually triggers the pin from the Dashboard. */
    MANUAL,

    /** The pin fires automatically on a fixed [DiPin.timerMs] interval. */
    AUTO
}

/**
 * Domain model representing a virtual Digital Input (DI) pin.
 *
 * @param id         Local database row identifier (0 = not yet persisted).
 * @param pinNumber  Human-readable pin identifier used in MQTT topics (e.g. "01").
 * @param mode       Whether the pin fires manually or automatically.
 * @param timerMs    Auto-mode interval in milliseconds (ignored in MANUAL mode).
 * @param shootCount Cumulative trigger counter included in every DI data payload.
 * @param pulseTime  Pulse duration in milliseconds included in every DI data payload.
 */
data class DiPin(
    val id: Int = 0,
    val pinNumber: String,
    val mode: PinMode,
    val timerMs: Long = 5000L,
    val shootCount: Long = 0L,
    val pulseTime: Long = 1000L
)
