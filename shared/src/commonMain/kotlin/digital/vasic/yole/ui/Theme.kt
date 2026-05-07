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

    /** Primary brand color (Material Red 700). */
    val BrandPrimary = Color(0xFFD32F2F)
    /** Secondary brand color (Material Pink A400). */
    val BrandSecondary = Color(0xFFDC004E)
    /** Tertiary brand color (Material Green 800). */
    val BrandTertiary = Color(0xFF2E7D32)

    /** Semantic color indicating success (Green 500). */
    val Success = Color(0xFF4CAF50)
    /** Semantic color indicating a warning (Orange 500). */
    val Warning = Color(0xFFFF9800)
    /** Semantic color indicating an error (Red 500). */
    val Error = Color(0xFFF44336)
    /** Semantic color for informational content (Red 600). */
    val Info = Color(0xFFE53935)

    /** Primary surface color for top-level backgrounds. */
    val SurfacePrimary = Color(0xFFFFFFFF)
    /** Secondary surface color for slightly elevated elements. */
    val SurfaceSecondary = Color(0xFFF5F5F5)
    /** Tertiary surface color for higher-elevation elements. */
    val SurfaceTertiary = Color(0xFFEEEEEE)

    /** Primary text color for body and heading text. */
    val TextPrimary = Color(0xFF212121)
    /** Secondary text color for captions and labels. */
    val TextSecondary = Color(0xFF757575)
    /** Tertiary text color for placeholder and hint text. */
    val TextTertiary = Color(0xFF9E9E9E)
    /** Text color for disabled or inactive elements. */
    val TextDisabled = Color(0xFFBDBDBD)

    /** Background color for hovered interactive elements. */
    val InteractiveHover = Color(0xFFFFEBEE)
    /** Background color for pressed interactive elements. */
    val InteractivePressed = Color(0xFFFFCDD2)
    /** Background color for focused interactive elements. */
    val InteractiveFocus = Color(0xFFEF9A9A)

    /** Light border for subtle separation. */
    val BorderLight = Color(0xFFE0E0E0)
    /** Medium border for moderate separation. */
    val BorderMedium = Color(0xFFBDBDBD)
    /** Strong border for prominent separation. */
    val BorderStrong = Color(0xFF9E9E9E)

    /**
     * IDE-specific surface and text tokens (VS Code inspired).
     * These are shared across web, desktop, and Android targets
     * so that all platform IDEs maintain visual consistency.
     */
    object Ide {
        val DarkBackground = Color(0xFF1E1E1E)
        val DarkSurface = Color(0xFF252526)
        val DarkSurfaceVariant = Color(0xFF2D2D30)
        val DarkBorder = Color(0xFF3C3C3C)
        val DarkPrimaryContainer = Color(0xFF5A1E1E)
        val DarkActivityBar = Color(0xFF333333)
        val DarkMenuHover = Color(0xFF2A2D2E)
        val DarkCurrentLine = Color(0xFF2A2D2E)
        val DarkSelection = Color(0xFF264F78)
        val DarkMutedText = Color(0xFF5A5A5A)

        val LightBackground = Color(0xFFFFFFFF)
        val LightSurface = Color(0xFFF3F3F3)
        val LightSurfaceVariant = Color(0xFFECECEC)
        val LightBorder = Color(0xFFD4D4D4)
        val LightActivityBar = Color(0xFFDDDDDD)
        val LightMenuHover = Color(0xFFE8E8E8)
        val LightCurrentLine = Color(0xFFF0F0F0)
        val LightMutedText = Color(0xFF6E6E6E)
    }

    /** Dark theme color variants optimized for dark backgrounds. */
    object Dark {
        /** Primary brand color for dark theme (Red 200). */
        val BrandPrimary = Color(0xFFEF9A9A)
        /** Secondary brand color for dark theme (Pink A200). */
        val BrandSecondary = Color(0xFFFF5983)
        /** Tertiary brand color for dark theme (Green 300). */
        val BrandTertiary = Color(0xFF81C784)

        /** Semantic success color for dark theme (Green 300). */
        val Success = Color(0xFF81C784)
        /** Semantic warning color for dark theme (Orange 300). */
        val Warning = Color(0xFFFFB74D)
        /** Semantic error color for dark theme (Red 400). */
        val Error = Color(0xFFEF5350)
        /** Semantic info color for dark theme (Red 300). */
        val Info = Color(0xFFE57373)

        /** Primary surface color for dark theme backgrounds. */
        val SurfacePrimary = Color(0xFF121212)
        /** Secondary surface color for dark theme elevated elements. */
        val SurfaceSecondary = Color(0xFF1E1E1E)
        /** Tertiary surface color for dark theme higher-elevation elements. */
        val SurfaceTertiary = Color(0xFF2A2A2A)

        /** Primary text color for dark theme. */
        val TextPrimary = Color(0xFFFFFFFF)
        /** Secondary text color for dark theme captions and labels. */
        val TextSecondary = Color(0xFFB0B0B0)
        /** Tertiary text color for dark theme hints and placeholders. */
        val TextTertiary = Color(0xFF808080)
        /** Disabled text color for dark theme. */
        val TextDisabled = Color(0xFF606060)

        /** Hover background for dark theme interactive elements. */
        val InteractiveHover = Color(0xFF5F1E1E)
        /** Pressed background for dark theme interactive elements. */
        val InteractivePressed = Color(0xFF6B2A2A)
        /** Focus background for dark theme interactive elements. */
        val InteractiveFocus = Color(0xFFC62828)

        /** Light border for dark theme. */
        val BorderLight = Color(0xFF404040)
        /** Medium border for dark theme. */
        val BorderMedium = Color(0xFF555555)
        /** Strong border for dark theme. */
        val BorderStrong = Color(0xFF707070)
    }
}

/**
 * Typography system with consistent text styles across platforms.
 * Based on Material Design 3 typography scale.
 */
object YoleTypography {

    /** Display large text style (57sp) for the most prominent text. */
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    )

    /** Display medium text style (45sp) for large prominent text. */
    val DisplayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    )

    /** Display small text style (36sp) for moderately prominent text. */
    val DisplaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )

    /** Headline large text style (32sp) for top-level headings. */
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    /** Headline medium text style (28sp) for section headings. */
    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    /** Headline small text style (24sp) for sub-section headings. */
    val HeadlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    /** Title large text style (22sp) for primary titles. */
    val TitleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )

    /** Title medium text style (16sp) for secondary titles. */
    val TitleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    /** Title small text style (14sp) for tertiary titles. */
    val TitleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /** Body large text style (16sp) for primary body text. */
    val BodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    /** Body medium text style (14sp) for secondary body text. */
    val BodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )

    /** Body small text style (12sp) for captions and annotations. */
    val BodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /** Label large text style (14sp) for prominent labels and buttons. */
    val LabelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /** Label medium text style (12sp) for standard labels. */
    val LabelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    /** Label small text style (11sp) for compact labels. */
    val LabelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    /** Monospace text style (14sp) for code and editor content. */
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
    /** Always use light theme. */
    LIGHT,
    /** Always use dark theme. */
    DARK,
    /** Follow the system-wide theme preference. */
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