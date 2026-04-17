package com.aira.health.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAiraColors = staticCompositionLocalOf { defaultLightAiraColors() }

@Composable
fun AiraTheme(
    oledDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val airaColors = if (oledDark) defaultDarkAiraColors() else defaultLightAiraColors()

    val materialColorScheme = if (oledDark) {
        darkColorScheme(
            background = airaColors.dominant,
            surface = airaColors.secondary,
            primary = airaColors.accent,
            error = airaColors.destructive,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            background = airaColors.dominant,
            surface = airaColors.secondary,
            primary = airaColors.accent,
            error = airaColors.destructive,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = airaColors.dominant.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !oledDark
        }
    }

    val typography = Typography(
        bodyLarge = AiraTypography.Body,
        labelLarge = AiraTypography.Label,
        headlineMedium = AiraTypography.Heading,
        displayMedium = AiraTypography.Display
    )

    CompositionLocalProvider(LocalAiraColors provides airaColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = typography,
            content = content
        )
    }
}

object Theme {
    val colors: AiraColors
        @Composable
        get() = LocalAiraColors.current
}
