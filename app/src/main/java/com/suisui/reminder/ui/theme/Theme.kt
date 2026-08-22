package com.suisui.reminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF3B0908),
    secondary = Color(0xFF775652),
    tertiary = Color(0xFF6F5B2E),
    background = Cream,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5E9E6),
    onSurfaceVariant = Color(0xFF534340)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF561F1C),
    primaryContainer = Color(0xFF73342F),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB5),
    tertiary = Color(0xFFDCC693),
    background = Color(0xFF1F1210),
    surface = Color(0xFF291B18),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BC)
)

@Composable
fun SuiSuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
