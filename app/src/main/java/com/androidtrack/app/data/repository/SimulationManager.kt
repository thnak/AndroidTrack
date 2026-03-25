package com.androidtrack.app.data.repository

import android.util.Log
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.MqttConnectionState
import com.androidtrack.app.data.model.PinMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the simulation lifecycle: heartbeat scheduler and per-pin auto-send schedulers.
 *
 * It owns a [ScheduledExecutorService] for timer-based tasks and a coroutine scope on
 * [Dispatchers.IO] for MQTT publish calls.
 *
 * Usage:
 * 1. Call [startSimulation] with the resolved device ID and the list of pins to simulate.
 * 2. Call [triggerManualPin] to immediately fire a manual-mode pin.
 * 3. Call [stopSimulation] to cancel all schedulers.
 */
@Singleton
class SimulationManager @Inject constructor(
    private val edgeMqttRepository: EdgeMqttRepository,
    private val diPinRepository: DiPinRepository,
    private val wifiInfoProvider: WifiInfoProvider,
    private val appLogger: AppLogger
) {
    private val tag = "SimulationManager"

    companion object {
        /** Heartbeat interval in milliseconds. */
        private const val HEARTBEAT_INTERVAL_MS = 30_000L

        /**
         * Minimum thread-pool size: 1 dedicated thread for the heartbeat +
         * 1 buffer thread to avoid starvation when many tasks fire simultaneously.
         */
        private const val MIN_POOL_THREADS = 2
    }

    /** Heartbeat attribute names cycled round-robin. */
    private val heartbeatAttributes = listOf("counter", "status", "temp", "humidity")
    private var heartbeatAttributeIndex = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var scheduler: ScheduledExecutorService
    private var heartbeatFuture: ScheduledFuture<*>? = null
    private val pinFutures = ConcurrentHashMap<Int, ScheduledFuture<*>>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // --- Lifecycle -----------------------------------------------------------

    /**
     * Starts the heartbeat and AUTO-mode pin schedulers.
     *
     * [deviceId] is the device identifier included in MQTT topics/payloads.
     * [pins] is the full list of configured DI pins; only AUTO-mode pins are scheduled here.
     *
     * This method is a no-op if the simulation is already running.
     */
    fun startSimulation(deviceId: String, pins: List<DiPin>) {
        if (_isRunning.value) return

        wifiInfoProvider.startObserving()

        scheduler = Executors.newScheduledThreadPool(
            maxOf(MIN_POOL_THREADS, pins.count { it.mode == PinMode.AUTO } + 1)
        )

        startHeartbeat(deviceId)
        pins.filter { it.mode == PinMode.AUTO }.forEach { startAutoPinScheduler(it) }

        _isRunning.value = true
        val msg = "Simulation started – device=$deviceId, pins=${pins.size}"
        Log.i(tag, msg)
        appLogger.info(msg)
    }

    /**
     * Cancels all active schedulers and releases the thread pool.
     */
    fun stopSimulation() {
        heartbeatFuture?.cancel(false)
        heartbeatFuture = null

        pinFutures.values.forEach { it.cancel(false) }
        pinFutures.clear()

        if (::scheduler.isInitialized) {
            scheduler.shutdownNow()
        }

        wifiInfoProvider.stopObserving()

        _isRunning.value = false
        Log.i(tag, "Simulation stopped")
        appLogger.info("Simulation stopped")
    }

    // --- Manual trigger ------------------------------------------------------

    /**
     * Increments [pin]'s shoot-count in the database, then publishes its DI data payload.
     * Returns the updated [DiPin] with the new shoot-count.
     *
     * Only intended for MANUAL-mode pins; calling this on an AUTO pin is allowed but unusual.
     */
    suspend fun triggerManualPin(pin: DiPin): DiPin {
        val updated = diPinRepository.incrementShootCount(pin)
        if (edgeMqttRepository.connectionState.value is MqttConnectionState.Connected) {
            edgeMqttRepository.publishDiData(updated)
            appLogger.info("Manual pin ${pin.pinNumber} triggered (count=${updated.shootCount})")
        }
        return updated
    }

    // --- Private schedulers --------------------------------------------------

    private fun startHeartbeat(deviceId: String) {
        heartbeatFuture = scheduler.scheduleAtFixedRate(
            {
                scope.launch {
                    try {
                        if (edgeMqttRepository.connectionState.value is MqttConnectionState.Connected) {
                            val attribute = heartbeatAttributes[heartbeatAttributeIndex]
                            heartbeatAttributeIndex =
                                (heartbeatAttributeIndex + 1) % heartbeatAttributes.size
                            edgeMqttRepository.publishHeartbeat(
                                deviceId = deviceId,
                                attributeName = attribute,
                                rssi = wifiInfoProvider.getRssi()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Heartbeat error", e)
                    }
                }
            },
            0L,
            HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun startAutoPinScheduler(pin: DiPin) {
        val pinId = pin.id
        val pinNumber = pin.pinNumber
        val future = scheduler.scheduleAtFixedRate(
            {
                scope.launch {
                    try {
                        if (edgeMqttRepository.connectionState.value is MqttConnectionState.Connected) {
                            // Fetch the current pin state from the DB so shoot_count is always
                            // up-to-date regardless of other concurrent increments.
                            val current = diPinRepository.getAll().find { it.id == pinId }
                            if (current != null) {
                                val updated = diPinRepository.incrementShootCount(current)
                                edgeMqttRepository.publishDiData(updated)
                                val msg = "Auto pin $pinNumber fired (count=${updated.shootCount})"
                                Log.d(tag, msg)
                                appLogger.debug(msg)
                            }
                        }
                    } catch (e: Exception) {
                        val msg = "Auto pin $pinNumber scheduler error: ${e.message}"
                        Log.e(tag, msg, e)
                        appLogger.error(msg)
                    }
                }
            },
            pin.timerMs,
            pin.timerMs,
            TimeUnit.MILLISECONDS
        )
        pinFutures[pinId] = future
    }
}
