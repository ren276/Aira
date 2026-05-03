package com.aira.health.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ── Aira design token set ────────────────────────────────────────────────────
// Derived directly from the "Clinical Ghost" design system (designs/aira_intelligence/DESIGN.md)
// Colors are used across all 18 screen designs.

// Core palette
val Primary         = Color(0xFF47EAED)   // Teal — healthy / optimistic
val PrimaryContainer= Color(0xFF00CED1)
val OnPrimary       = Color(0xFF003738)
val Secondary       = Color(0xFFFFE2AB)   // Amber — cautionary / neutral
val SecondaryContainer = Color(0xFFFFBF00)
val OnSecondary     = Color(0xFF402D00)
val Tertiary        = Color(0xFFFFC9B7)   // Coral — critical / action
val TertiaryContainer = Color(0xFFFFA282)
val OnTertiary      = Color(0xFF5C1A00)
val Error           = Color(0xFFFFB4AB)
val ErrorContainer  = Color(0xFF93000A)

// Surface tones
val Background      = Color(0xFF131318)
val BackgroundOled  = Color(0xFF000000)
val Surface         = Color(0xFF131318)
val SurfaceDim      = Color(0xFF131318)
val SurfaceBright   = Color(0xFF39383E)
val SurfaceContainerLowest = Color(0xFF0E0E13)
val SurfaceContainerLow    = Color(0xFF1B1B20)
val SurfaceContainer       = Color(0xFF1F1F25)
val SurfaceContainerHigh   = Color(0xFF2A292F)
val SurfaceContainerHighest= Color(0xFF35343A)
val SurfaceVariant  = Color(0xFF35343A)

// On-surface
val OnSurface       = Color(0xFFE4E1E9)
val OnSurfaceVariant= Color(0xFFBAC9C9)
val OnBackground    = Color(0xFFE4E1E9)

// Outline
val Outline         = Color(0xFF859493)
val OutlineVariant  = Color(0xFF3B4949)

// Tint / misc
val SurfaceTint     = Color(0xFF2DDBDE)
val InversePrimary  = Color(0xFF00696B)
val InverseSurface  = Color(0xFFE4E1E9)
val InverseOnSurface= Color(0xFF303036)

// Light-mode variants (for onboarding / settings light screens)
val BackgroundLight  = Color(0xFFF6F8F7)
val SurfaceLight     = Color(0xFFFFFFFF)
val OnSurfaceLight   = Color(0xFF1A1C1E)
val PrimaryLight     = Color(0xFF47EAED)

// ── AiraColors model ─────────────────────────────────────────────────────────
@Immutable
data class AiraColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val destructive: Color,
    val caution: Color,
    val isLight: Boolean,
    // Extended tokens for design-faithful surfaces
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outlineVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
)

fun defaultLightAiraColors() = AiraColors(
    dominant               = BackgroundLight,
    secondary              = Color(0xFFE7ECEB),
    accent                 = Primary,
    destructive            = Tertiary,
    caution                = Secondary,
    isLight                = true,
    surfaceContainerLow    = Color(0xFFF0F4F3),
    surfaceContainer       = Color(0xFFEAEEED),
    surfaceContainerHigh   = Color(0xFFE4E9E8),
    surfaceContainerHighest= Color(0xFFDEE3E2),
    outlineVariant         = Color(0xFFBFC9C8),
    onSurface              = OnSurfaceLight,
    onSurfaceVariant       = Color(0xFF3F4948),
    primaryContainer       = PrimaryContainer,
    secondaryColor         = Secondary,
    tertiaryColor          = Tertiary,
)

fun defaultDarkAiraColors() = AiraColors(
    dominant               = Background,
    secondary              = SurfaceContainer,
    accent                 = Primary,
    destructive            = Tertiary,
    caution                = Secondary,
    isLight                = false,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest= SurfaceContainerHighest,
    outlineVariant         = OutlineVariant,
    onSurface              = OnSurface,
    onSurfaceVariant       = OnSurfaceVariant,
    primaryContainer       = PrimaryContainer,
    secondaryColor         = Secondary,
    tertiaryColor          = Tertiary,
)

fun defaultOledAiraColors() = defaultDarkAiraColors().copy(
    dominant = BackgroundOled
)
