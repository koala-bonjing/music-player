package com.sonny.heartymusic.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = HeartyPrimary,
    onPrimary = HeartyWhite,
    primaryContainer = HeartyPrimaryContainer,
    onPrimaryContainer = HeartyOnPrimaryContainer,
    secondary = HeartySecondary,
    onSecondary = HeartyWhite,
    background = HeartyBackground,
    onBackground = HeartyText,
    surface = HeartySurface,
    onSurface = HeartyText,
    surfaceVariant = HeartySurfaceVariant,
    onSurfaceVariant = HeartyTextSecondary,
    outline = HeartyOutline
)

@Composable
fun HeartyMusicTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = HeartyBackground.toArgb()
            window.navigationBarColor = HeartyBackground.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HeartyTypography,
        shapes = HeartyShapes,
        content = content
    )
}
