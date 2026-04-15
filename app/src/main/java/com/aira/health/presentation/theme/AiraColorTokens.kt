package com.aira.health.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AiraColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val destructive: Color,
    val caution: Color,
    val isLight: Boolean
)

fun defaultLightAiraColors() = AiraColors(
    dominant = Color(0xFFF6F8F7),
    secondary = Color(0xFFE7ECEB),
    accent = Color(0xFF47EAED),
    destructive = Color(0xFFFFC9B7),
    caution = Color(0xFFFFE2AB),
    isLight = true
)

fun defaultDarkAiraColors() = AiraColors(
    dominant = Color(0xFF131318),
    secondary = Color(0xFF1F1F25),
    accent = Color(0xFF47EAED),
    destructive = Color(0xFFFFC9B7),
    caution = Color(0xFFFFE2AB),
    isLight = false
)
