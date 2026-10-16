package com.lolipp.asthmadiary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF0D3B0F),
    secondary = Color(0xFF66BB6A),
    tertiary = Color(0xFF81C784),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D3B0F),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF66BB6A),
    tertiary = Color(0xFF4CAF50),
)

@Composable
fun AsthmaDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
