package com.androidtrack.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists application-level settings using [android.content.SharedPreferences].
 *
 * Currently manages:
 * - [showConsoleLog]: whether the Console Log section is visible on the Dashboard.
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _showConsoleLog = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_CONSOLE_LOG, true)
    )
    val showConsoleLog: StateFlow<Boolean> = _showConsoleLog.asStateFlow()

    fun setShowConsoleLog(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CONSOLE_LOG, value).apply()
        _showConsoleLog.value = value
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SHOW_CONSOLE_LOG = "show_console_log"
    }
}
