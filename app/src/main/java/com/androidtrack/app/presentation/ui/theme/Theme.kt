package com.androidtrack.app.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Color palette per UI/UX Design spec:
 * - Primary: deep industrial blue
 * - Secondary: active/running green
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),          // Deep Blue 800
    onPrimary = Color.White,
    primaryContainer = Color(0xFF90CAF9), // Blue 200
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF2E7D32),        // Green 800
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA5D6A7),
    onSecondaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun AndroidTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

