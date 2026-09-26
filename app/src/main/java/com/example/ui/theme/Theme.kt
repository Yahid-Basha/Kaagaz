package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = InkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = AmberDueSoon,
    onSecondary = Color.Black,
    tertiary = GreenFine,
    background = InkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = StampRed,
    errorContainer = Color(0xFF501210),
    onError = Color.White,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = InkBackground,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7ECEF),
    onPrimaryContainer = InkBackground,
    secondary = AmberDueSoon,
    onSecondary = Color.White,
    tertiary = GreenFine,
    background = PaperOffWhite,
    onBackground = InkBackground,
    surface = PaperSurface,
    onSurface = InkBackground,
    surfaceVariant = Color(0xFFEEF2F5),
    onSurfaceVariant = MutedText,
    error = StampRed,
    errorContainer = StampRedContainer,
    onError = Color.White,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally adhere to the Kaagaz paper & ink aesthetic rather than system dynamic wallpaper colors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
