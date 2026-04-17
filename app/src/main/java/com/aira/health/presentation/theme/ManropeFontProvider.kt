package com.aira.health.presentation.theme

import androidx.compose.ui.text.googlefonts.GoogleFont
import com.aira.health.R

/**
 * Google Fonts provider configuration for Manrope.
 * The provider caches the font on-device via Play Services Font Provider.
 * Offline fallback: system SansSerif (handled in AiraTypography.kt).
 */
object ManropeFontProvider {
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage   = "com.google.android.gms",
        certificates      = R.array.com_google_android_gms_fonts_certs
    )
}
