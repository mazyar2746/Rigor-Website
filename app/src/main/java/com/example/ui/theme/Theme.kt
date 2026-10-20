package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RigourCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = RigourTeal,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF73F8E4),
    tertiary = RigourGold,
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5F4100),
    onTertiaryContainer = Color(0xFFFFDEA3),
    background = RigourBackground,
    onBackground = RigourTextPrimary,
    surface = RigourSurface,
    onSurface = RigourTextPrimary,
    surfaceVariant = RigourSurfaceVariant,
    onSurfaceVariant = RigourTextSecondary,
    outline = RigourCardBorder,
    outlineVariant = Color(0xFF475569),
    error = RigourBearish,
    onError = Color.White
)

@Composable
fun RigourTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Professional crypto apps are optimized for dark trading terminal UI
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
