/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Theme Implementation for Yole
 *
 * Provides desktop-native theming with system integration, custom accent
 * colors, and high contrast support. iter-57 Phase 3b: Material3
 * ColorScheme is derived from the active VS Code [Theme] via LocalTheme.
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import digital.vasic.yole.desktop.ui.YoleDesktopSettings
import digital.vasic.yole.syntax.theme.LocalTheme
import digital.vasic.yole.syntax.theme.Theme
import digital.vasic.yole.ui.ThemeMode
import digital.vasic.yole.ui.ThemeUtils
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
        val theme = LocalTheme.current

        val baseScheme = createSemanticColorScheme(isDarkTheme, theme)

        return if (accentColor != null) {
            applyAccentColor(baseScheme, accentColor, isDarkTheme)
        } else {
            baseScheme
        }
    }

    /**
     * Derives a Material3 ColorScheme from the active VS Code [Theme].
     * Mirrors the legacy `IDE Dark/Light` mapping, but pulls every color
     * out of `theme.uiColor(...)` rather than hardcoded legacy palette tokens.
     */
    private fun createSemanticColorScheme(isDarkTheme: Boolean, theme: Theme): ColorScheme {
        val baseScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

        val background = theme.uiColor("editor.background")?.let { Color(it) } ?: baseScheme.background
        val surface = theme.uiColor("sideBar.background")?.let { Color(it) } ?: baseScheme.surface
        val surfaceVariant =
            theme.uiColor("tab.inactiveBackground")?.let { Color(it) } ?: baseScheme.surfaceVariant
        val onBackground = theme.uiColor("editor.foreground")?.let { Color(it) } ?: baseScheme.onBackground
        val onSurface = theme.uiColor("editor.foreground")?.let { Color(it) } ?: baseScheme.onSurface
        val onSurfaceVariant =
            theme.uiColor("editorLineNumber.foreground")?.let { Color(it) } ?: baseScheme.onSurfaceVariant
        val accent = theme.uiColor("focusBorder")?.let { Color(it) } ?: baseScheme.primary
        val outline = theme.uiColor("editorWidget.border")?.let { Color(it) } ?: baseScheme.outline
        val outlineVariant =
            theme.uiColor("editorLineNumber.foreground")?.let { Color(it) } ?: baseScheme.outlineVariant

        return baseScheme.copy(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = if (isDarkTheme) 0.3f else 0.15f),
            onPrimaryContainer = onBackground,
            secondary = accent,
            tertiary = accent,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            background = background,
            onBackground = onBackground,
            outline = outline,
            outlineVariant = outlineVariant
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
        val accentContainer = if (isDarkTheme) {
            accentColor.copy(alpha = 0.2f)
        } else {
            accentColor.copy(alpha = 0.1f)
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
        return Shapes()
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
