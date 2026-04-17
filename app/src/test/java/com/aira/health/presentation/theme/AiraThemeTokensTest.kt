package com.aira.health.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class AiraThemeTokensTest {

    @Test
    fun typographyExposesAllowedRolesAndConstraints() {
        assertEquals(16.sp, AiraTypography.Body.fontSize)
        assertEquals(FontWeight(400), AiraTypography.Body.fontWeight)
        
        assertEquals(14.sp, AiraTypography.Label.fontSize)
        assertEquals(FontWeight(600), AiraTypography.Label.fontWeight)

        assertEquals(20.sp, AiraTypography.Heading.fontSize)
        assertEquals(FontWeight(600), AiraTypography.Heading.fontWeight)

        assertEquals(28.sp, AiraTypography.Display.fontSize)
        assertEquals(FontWeight(600), AiraTypography.Display.fontWeight)
    }

    @Test
    fun spacingExposesAllowedRoles() {
        assertEquals(4.dp, AiraSpacing.xs)
        assertEquals(8.dp, AiraSpacing.sm)
        assertEquals(16.dp, AiraSpacing.md)
        assertEquals(24.dp, AiraSpacing.lg)
        assertEquals(32.dp, AiraSpacing.xl)
        assertEquals(48.dp, AiraSpacing.xxl) 
        assertEquals(64.dp, AiraSpacing.xxxl) 
    }
    
    @Test
    fun lightThemeColorsMatchSpec() {
        val lightColors = defaultLightAiraColors()
        assertEquals(Color(0xFFF6F8F7), lightColors.dominant)
        assertEquals(Color(0xFFE7ECEB), lightColors.secondary)
        assertEquals(Color(0xFF47EAED), lightColors.accent)
        assertEquals(Color(0xFFFFC9B7), lightColors.destructive)
        assertEquals(Color(0xFFFFE2AB), lightColors.caution)
    }

    @Test
    fun darkThemeColorsMatchSpec() {
        val darkColors = defaultDarkAiraColors()
        assertEquals(Color(0xFF131318), darkColors.dominant)
        assertEquals(Color(0xFF1F1F25), darkColors.secondary)
        assertEquals(Color(0xFF47EAED), darkColors.accent)
        assertEquals(Color(0xFFFFC9B7), darkColors.destructive)
        assertEquals(Color(0xFFFFE2AB), darkColors.caution)
    }
}
