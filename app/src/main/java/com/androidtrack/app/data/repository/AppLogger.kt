package com.androidtrack.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized in-memory log collector for MQTT and application events.
 *
 * Log entries are emitted via [logs] and displayed in the Dashboard Console Log section.
 * The most recent [MAX_ENTRIES] entries are kept; older ones are automatically discarded.
 */
@Singleton
class AppLogger @Inject constructor() {

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String
    ) {
        override fun toString(): String = "[$timestamp] $level: $message"
    }

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun info(message: String) = append("I", message)
    fun debug(message: String) = append("D", message)
    fun warn(message: String) = append("W", message)
    fun error(message: String) = append("E", message)

    fun clear() {
        _logs.value = emptyList()
    }

    private fun append(level: String, message: String) {
        val entry = LogEntry(
            timestamp = formatter.format(Instant.now()),
            level = level,
            message = message
        )
        _logs.update { list -> (list + entry).takeLast(MAX_ENTRIES) }
    }

    companion object {
        const val MAX_ENTRIES = 200
    }
}
