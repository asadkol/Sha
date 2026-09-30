package com.shahpharmacy.app.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = NavyDark,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0F4FA),
    onSurfaceVariant = TextMuted,
    outline = CardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF88B3F8),
    onPrimary = NavyDark,
    primaryContainer = NavyDark,
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D254C),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF182544),
    onSurfaceVariant = DarkTextMuted,
    outline = DarkCardBorder
)

@Composable
fun ShahPharmacyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyPrimary.toArgb()
            window.navigationBarColor = NavyDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
