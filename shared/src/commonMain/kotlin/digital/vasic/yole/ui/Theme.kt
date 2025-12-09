/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared Theme System for Yole
 *
 * Provides semantic color tokens, typography constants, and theme utilities
 * that work across all platforms (Android, Desktop, Web).
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.pow

/**
 * Semantic color tokens for consistent theming across platforms.
 * These provide meaningful names for colors rather than generic "primary/secondary".
 */
object YoleColors {

    // Brand colors
    val BrandPrimary = Color(0xFF1976D2)      // Material Blue 700
    val BrandSecondary = Color(0xFFDC004E)    // Material Pink A400
    val BrandTertiary = Color(0xFF2E7D32)     // Material Green 800

    // Semantic colors (context-aware)
    val Success = Color(0xFF4CAF50)           // Green 500
    val Warning = Color(0xFFFF9800)           // Orange 500
    val Error = Color(0xFFF44336)             // Red 500
    val Info = Color(0xFF2196F3)              // Blue 500

    // Surface colors (for different elevations)
    val SurfacePrimary = Color(0xFFFFFFFF)    // White
    val SurfaceSecondary = Color(0xFFF5F5F5)  // Gray 100
    val SurfaceTertiary = Color(0xFFEEEEEE)   // Gray 200

    // Text colors
    val TextPrimary = Color(0xFF212121)       // Gray 900
    val TextSecondary = Color(0xFF757575)     // Gray 600
    val TextTertiary = Color(0xFF9E9E9E)      // Gray 500
    val TextDisabled = Color(0xFFBDBDBD)      // Gray 400

    // Interactive colors
    val InteractiveHover = Color(0xFFE3F2FD)  // Blue 50
    val InteractivePressed = Color(0xFFBBDEFB) // Blue 100
    val InteractiveFocus = Color(0xFF90CAF9)  // Blue 200

    // Border colors
    val BorderLight = Color(0xFFE0E0E0)       // Gray 300
    val BorderMedium = Color(0xFFBDBDBD)      // Gray 400
    val BorderStrong = Color(0xFF9E9E9E)      // Gray 500

    // Dark theme variants
    object Dark {
        val BrandPrimary = Color(0xFF90CAF9)      // Blue 200
        val BrandSecondary = Color(0xFFFF5983)    // Pink A200
        val BrandTertiary = Color(0xFF81C784)     // Green 300

        val Success = Color(0xFF81C784)           // Green 300
        val Warning = Color(0xFFFFB74D)           // Orange 300
        val Error = Color(0xFFEF5350)             // Red 400
        val Info = Color(0xFF64B5F6)              // Blue 300

        val SurfacePrimary = Color(0xFF121212)    // Gray 900
        val SurfaceSecondary = Color(0xFF1E1E1E)  // Gray 800
        val SurfaceTertiary = Color(0xFF2A2A2A)   // Gray 750

        val TextPrimary = Color(0xFFFFFFFF)       // White
        val TextSecondary = Color(0xFFB0B0B0)     // Gray 300
        val TextTertiary = Color(0xFF808080)      // Gray 500
        val TextDisabled = Color(0xFF606060)      // Gray 600

        val InteractiveHover = Color(0xFF1E3A5F)  // Dark blue
        val InteractivePressed = Color(0xFF2A4A6B) // Darker blue
        val InteractiveFocus = Color(0xFF1565C0)  // Blue 800

        val BorderLight = Color(0xFF404040)       // Gray 600
        val BorderMedium = Color(0xFF555555)      // Gray 550
        val BorderStrong = Color(0xFF707070)      // Gray 500
    }
}

/**
 * Typography system with consistent text styles across platforms.
 * Based on Material Design 3 typography scale.
 */
object YoleTypography {

    // Display styles (largest)
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    )

    val DisplayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    )

    val DisplaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )

    // Headline styles
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val HeadlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    // Title styles
    val TitleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )

    val TitleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    val TitleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    // Body styles
    val BodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    val BodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )

    val BodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    // Label styles
    val LabelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val LabelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    val LabelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    // Monospace for code/editor
    val Code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
}

/**
 * Theme mode enumeration for consistent theme handling.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Utility functions for theme operations.
 */
object ThemeUtils {

    /**
     * Determines if dark theme should be used based on theme mode and system preference.
     */
    fun shouldUseDarkTheme(themeMode: ThemeMode, systemInDarkTheme: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
    }

    /**
     * Gets the appropriate color from semantic tokens based on theme.
     */
    fun getSemanticColor(
        isDarkTheme: Boolean,
        lightColor: Color,
        darkColor: Color
    ): Color {
        return if (isDarkTheme) darkColor else lightColor
    }

    /**
     * Calculates contrast ratio between two colors (for accessibility).
     * Returns a value between 1.0 and 21.0, where:
     * - 1.0 = no contrast (same color)
     * - 4.5 = WCAG AA minimum for normal text
     * - 7.0 = WCAG AAA minimum for normal text
     * - 3.0 = WCAG AA minimum for large text
     */
    fun calculateContrastRatio(color1: Color, color2: Color): Double {
        // Convert to relative luminance
        fun relativeLuminance(color: Color): Double {
            val r = color.red.toDouble()
            val g = color.green.toDouble()
            val b = color.blue.toDouble()

            val toLinear = { value: Double ->
                if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }

            val rLinear = toLinear(r)
            val gLinear = toLinear(g)
            val bLinear = toLinear(b)

            return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
        }

        val lum1 = relativeLuminance(color1)
        val lum2 = relativeLuminance(color2)

        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Checks if contrast ratio meets WCAG AA requirements.
     */
    fun meetsWcagAA(textColor: Color, backgroundColor: Color, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(textColor, backgroundColor)
        return if (isLargeText) ratio >= 3.0 else ratio >= 4.5
    }

    /**
     * Checks if contrast ratio meets WCAG AAA requirements.
     */
    fun meetsWcagAAA(textColor: Color, backgroundColor: Color, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(textColor, backgroundColor)
        return if (isLargeText) ratio >= 4.5 else ratio >= 7.0
    }

    /**
     * Validates that a color scheme meets WCAG AA accessibility standards.
     * Returns a list of violations if any.
     */
    fun validateColorSchemeAccessibility(colorScheme: ColorScheme): List<String> {
        val violations = mutableListOf<String>()

        // Primary text on background
        if (!meetsWcagAA(colorScheme.onBackground, colorScheme.background)) {
            violations.add("Primary text on background: ${calculateContrastRatio(colorScheme.onBackground, colorScheme.background)} < 4.5")
        }

        // Primary text on surface
        if (!meetsWcagAA(colorScheme.onSurface, colorScheme.surface)) {
            violations.add("Primary text on surface: ${calculateContrastRatio(colorScheme.onSurface, colorScheme.surface)} < 4.5")
        }

        // Secondary text on background (can be lower contrast)
        val secondaryBgRatio = calculateContrastRatio(colorScheme.onSurfaceVariant, colorScheme.background)
        if (secondaryBgRatio < 3.0) { // Minimum for UI elements
            violations.add("Secondary text on background: $secondaryBgRatio < 3.0")
        }

        // Error text combinations
        if (!meetsWcagAA(colorScheme.onError, colorScheme.error)) {
            violations.add("Error text on error background: ${calculateContrastRatio(colorScheme.onError, colorScheme.error)} < 4.5")
        }

        // Interactive elements
        if (!meetsWcagAA(colorScheme.onPrimary, colorScheme.primary)) {
            violations.add("Text on primary button: ${calculateContrastRatio(colorScheme.onPrimary, colorScheme.primary)} < 4.5")
        }

        return violations
    }
}