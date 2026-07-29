package com.budgetnotes.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E6D5),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4A635B),
    onSecondary = Color.White,
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFF7F9F8),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF404944),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCFBD),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF1B5E4A),
    onPrimaryContainer = Color(0xFFB8E6D5),
    secondary = Color(0xFFB0CCC2),
    onSecondary = Color(0xFF1C3530),
    background = Color(0xFF111413),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF111413),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C4),
)

@Composable
fun BudgetNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
