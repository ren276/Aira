package com.aira.health.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Manrope via Downloadable Fonts ──────────────────────────────────────────
// Uses Google Fonts provider — no TTF bundling needed.
// The manifest must declare the fonts-provider meta-data for offline caching.
// Declared in ManropeFontProvider.kt (companion object).
// If the provider is unavailable at first launch, falls back to SansSerif gracefully.

val ManropeFontFamily: FontFamily = try {
    // This is resolved at build time via Google Fonts Compose API
    // Falls back to system sans-serif on devices without Play Services
    FontFamily(
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.ExtraLight
        ),
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.Normal
        ),
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.Medium
        ),
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.SemiBold
        ),
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.Bold
        ),
        androidx.compose.ui.text.googlefonts.Font(
            googleFont = androidx.compose.ui.text.googlefonts.GoogleFont("Manrope"),
            fontProvider = ManropeFontProvider.provider,
            weight = FontWeight.ExtraBold
        ),
    )
} catch (e: Exception) {
    FontFamily.SansSerif
}

// ── Type Scale ──────────────────────────────────────────────────────────────
// Matches design spec exactly (designs/aira_intelligence/DESIGN.md §3 Typography)
object AiraTypography {

    /** 3.5rem / 800 / -0.04em — Impactful data points, hero states */
    val DisplayLg = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize   = 56.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 60.sp,
        letterSpacing = (-2.24).sp
    )

    /** 1.75rem / 600 / -0.02em — Section starts */
    val HeadlineMd = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize   = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 33.6.sp,
        letterSpacing = (-0.56).sp
    )

    /** 1.0rem / 600 / +0.01em — Card headers, small labels */
    val TitleSm = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.16.sp
    )

    /** 1.0rem / 400 / 0 — Long-form predictive insights */
    val BodyLg = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    )

    /** 0.75rem / 700 / +0.05em — Micro-data, all-caps metadata */
    val LabelMd = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )

    // Contract roles used by tests and legacy composables.
    val Body = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    )

    val Label = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 19.6.sp
    )

    val Heading = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    )

    val Display = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 33.6.sp
    )
}
