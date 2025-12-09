/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Theme Implementation for Yole
 *
 * Provides desktop-native theming with system integration,
 * custom accent colors, and high contrast support.
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import digital.vasic.yole.desktop.ui.YoleDesktopSettings
import digital.vasic.yole.ui.ThemeMode
import digital.vasic.yole.ui.ThemeUtils
import digital.vasic.yole.ui.YoleColors
import digital.vasic.yole.ui.YoleTypography
import kotlin.text.toIntOrNull

/**
 * Parse hex color string to Color.
 */
fun parseHexColor(hex: String): Color? {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> {
                val r = cleanHex.substring(0, 2).toIntOrNull(16) ?: return null
                val g = cleanHex.substring(2, 4).toIntOrNull(16) ?: return null
                val b = cleanHex.substring(4, 6).toIntOrNull(16) ?: return null
                Color(r / 255f, g / 255f, b / 255f)
            }
            8 -> {
                val a = cleanHex.substring(0, 2).toIntOrNull(16) ?: return null
                val r = cleanHex.substring(2, 4).toIntOrNull(16) ?: return null
                val g = cleanHex.substring(4, 6).toIntOrNull(16) ?: return null
                val b = cleanHex.substring(6, 8).toIntOrNull(16) ?: return null
                Color(r / 255f, g / 255f, b / 255f, a / 255f)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Desktop-specific theme configuration with system integration.
 */
object YoleDesktopTheme {

    /**
     * Creates a Material3 color scheme optimized for desktop.
     * Includes system theme detection and custom accent colors.
     */
    @Composable
    fun createColorScheme(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        accentColor: Color? = null
    ): ColorScheme {
        val isDarkTheme = ThemeUtils.shouldUseDarkTheme(themeMode, isSystemInDarkTheme())

        // Start with semantic color scheme
        val baseScheme = createSemanticColorScheme(isDarkTheme)

        // Apply custom accent color if provided
        return if (accentColor != null) {
            applyAccentColor(baseScheme, accentColor, isDarkTheme)
        } else {
            baseScheme
        }
    }

    /**
     * Creates a color scheme using semantic color tokens optimized for desktop.
     */
    private fun createSemanticColorScheme(isDarkTheme: Boolean): ColorScheme {
        val baseScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

        // Customize the base scheme with our semantic colors
        return baseScheme.copy(
            primary = YoleColors.BrandPrimary,
            secondary = YoleColors.BrandSecondary,
            tertiary = YoleColors.BrandTertiary,
            error = YoleColors.Error,
            surface = YoleColors.SurfacePrimary,
            onSurface = YoleColors.TextPrimary,
            background = YoleColors.SurfacePrimary,
            onBackground = YoleColors.TextPrimary,
            outline = YoleColors.BorderMedium
        )
    }

    /**
     * Applies a custom accent color to the color scheme.
     */
    private fun applyAccentColor(
        baseScheme: ColorScheme,
        accentColor: Color,
        isDarkTheme: Boolean
    ): ColorScheme {
        // Calculate derived colors from accent
        val accentContainer = if (isDarkTheme) {
            accentColor.copy(alpha = 0.2f) // Darker container
        } else {
            accentColor.copy(alpha = 0.1f) // Lighter container
        }

        val onAccent = if (ThemeUtils.calculateContrastRatio(accentColor, Color.White) > 4.5) {
            Color.White
        } else {
            Color.Black
        }

        val onAccentContainer = if (ThemeUtils.calculateContrastRatio(accentContainer, Color.White) > 4.5) {
            Color.White
        } else {
            Color.Black
        }

        return baseScheme.copy(
            primary = accentColor,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            surfaceTint = accentColor
        )
    }

    /**
     * Creates high contrast color scheme for accessibility.
     */
    fun createHighContrastColorScheme(isDarkTheme: Boolean): ColorScheme {
        val baseScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

        return if (isDarkTheme) {
            // High contrast dark theme
            baseScheme.copy(
                primary = Color.White,
                onPrimary = Color.Black,
                surface = Color.Black,
                onSurface = Color.White,
                background = Color.Black,
                onBackground = Color.White,
                outline = Color.White
            )
        } else {
            // High contrast light theme
            baseScheme.copy(
                primary = Color.Black,
                onPrimary = Color.White,
                surface = Color.White,
                onSurface = Color.Black,
                background = Color.White,
                onBackground = Color.Black,
                outline = Color.Black
            )
        }
    }

    /**
     * Creates Material3 Typography using shared typography system.
     */
    fun createTypography(): Typography {
        return Typography(
            displayLarge = YoleTypography.DisplayLarge,
            displayMedium = YoleTypography.DisplayMedium,
            displaySmall = YoleTypography.DisplaySmall,
            headlineLarge = YoleTypography.HeadlineLarge,
            headlineMedium = YoleTypography.HeadlineMedium,
            headlineSmall = YoleTypography.HeadlineSmall,
            titleLarge = YoleTypography.TitleLarge,
            titleMedium = YoleTypography.TitleMedium,
            titleSmall = YoleTypography.TitleSmall,
            bodyLarge = YoleTypography.BodyLarge,
            bodyMedium = YoleTypography.BodyMedium,
            bodySmall = YoleTypography.BodySmall,
            labelLarge = YoleTypography.LabelLarge,
            labelMedium = YoleTypography.LabelMedium,
            labelSmall = YoleTypography.LabelSmall
        )
    }

    /**
     * Creates Material3 Shapes optimized for desktop.
     */
    fun createShapes(): Shapes {
        return Shapes() // Uses Material3 defaults, can be customized for desktop
    }
}

/**
 * Main theme composable for Desktop with system integration.
 */
@Composable
fun YoleDesktopTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: Color? = null,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (highContrast) {
        YoleDesktopTheme.createHighContrastColorScheme(
            ThemeUtils.shouldUseDarkTheme(themeMode, isSystemInDarkTheme())
        )
    } else {
        YoleDesktopTheme.createColorScheme(themeMode, accentColor)
    }

    val typography = YoleDesktopTheme.createTypography()
    val shapes = YoleDesktopTheme.createShapes()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

/**
 * Convenience function that reads desktop settings and applies the theme.
 */
@Composable
fun YoleDesktopThemeWithSettings(
    settings: YoleDesktopSettings,
    content: @Composable () -> Unit
) {
    val themeMode = when (settings.getThemeMode()) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    val accentColorHex = settings.getAccentColor()
    val accentColor = accentColorHex?.let { parseHexColor(it) }
    val highContrast = settings.getHighContrastEnabled()

    YoleDesktopTheme(
        themeMode = themeMode,
        accentColor = accentColor,
        highContrast = highContrast,
        content = content
    )
}