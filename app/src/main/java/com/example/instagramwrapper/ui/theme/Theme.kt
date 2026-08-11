package com.example.instagramwrapper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82AFFF),
    secondary = Color(0xFF8B95A7),
    tertiary = Color(0xFFFFB36B),
    background = Color(0xFF0C1117),
    surface = Color(0xFF121923),
    surfaceVariant = Color(0xFF1A2330),
    onPrimary = Color(0xFF08101A),
    onSecondary = Color(0xFF0C1117),
    onTertiary = Color(0xFF0C1117),
    onBackground = Color(0xFFF2F6FA),
    onSurface = Color(0xFFF2F6FA),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    secondary = Color(0xFF506174),
    tertiary = Color(0xFFB45309),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
)

@Composable
fun InstagramWrapperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
