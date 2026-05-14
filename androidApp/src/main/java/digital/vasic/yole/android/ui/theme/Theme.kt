/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android Theme Implementation for Yole
 *
 * Provides Material You dynamic colors, Android-specific theming, and
 * integration with the iter-57 VS Code theme system (ThemeProvider /
 * LocalTheme). Material3 ColorScheme is derived from the active VS Code
 * theme's colors.* keys via [LocalTheme.current.uiColor].
 *
 *########################################################*/

package digital.vasic.yole.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import digital.vasic.yole.android.ui.YoleSettings
import digital.vasic.yole.syntax.theme.LocalTheme
import digital.vasic.yole.syntax.theme.Theme
import digital.vasic.yole.ui.ThemeMode
import digital.vasic.yole.ui.ThemeUtils
import digital.vasic.yole.ui.YoleTypography

/**
 * Android-specific theme configuration with Material You support.
 *
 * iter-57 Phase 3b: semantic ColorScheme generation reads from the active
 * VS Code [Theme] (via [LocalTheme]) so Material3 surfaces and tints stay
 * consistent with editor/preview chrome.
 */
object YoleAndroidTheme {

    /**
     * Creates a Material3 color scheme with Material You dynamic colors when available.
     */
    @Composable
    fun createColorScheme(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        dynamicColor: Boolean = true,
        seedColor: Color? = null
    ): ColorScheme {
        val isDarkTheme = ThemeUtils.shouldUseDarkTheme(themeMode, isSystemInDarkTheme())
        val theme = LocalTheme.current

        return when {
            // Material You dynamic colors with custom seed (Android 12+)
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                createDynamicColorScheme(isDarkTheme, seedColor)
            }

            // Fallback: derive Material3 ColorScheme from active VS Code theme
            else -> {
                createSemanticColorScheme(isDarkTheme, theme)
            }
        }
    }

    /**
     * Creates dynamic color scheme with optional custom seed color.
     */
    @Composable
    private fun createDynamicColorScheme(isDarkTheme: Boolean, seedColor: Color?): ColorScheme {
        val context = LocalContext.current

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (seedColor != null) {
                if (isDarkTheme) {
                    dynamicDarkColorScheme(context).copy(primary = seedColor)
                } else {
                    dynamicLightColorScheme(context).copy(primary = seedColor)
                }
            } else {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
        } else {
            if (isDarkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    /**
     * Derives a Material3 ColorScheme from the active VS Code [Theme].
     * Every Material3 role is mapped to the closest VS Code colors.* key.
     */
    private fun createSemanticColorScheme(isDarkTheme: Boolean, theme: Theme): ColorScheme {
        val baseScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
        val accent = theme.uiColor("focusBorder")?.let { Color(it) } ?: baseScheme.primary
        val surface = theme.uiColor("sideBar.background")?.let { Color(it) } ?: baseScheme.surface
        val background = theme.uiColor("editor.background")?.let { Color(it) } ?: baseScheme.background
        val onBackground = theme.uiColor("editor.foreground")?.let { Color(it) } ?: baseScheme.onBackground
        val onSurface = theme.uiColor("editor.foreground")?.let { Color(it) } ?: baseScheme.onSurface
        val outline = theme.uiColor("editorWidget.border")?.let { Color(it) } ?: baseScheme.outline

        val customScheme = baseScheme.copy(
            primary = accent,
            secondary = accent,
            tertiary = accent,
            error = baseScheme.error,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            outline = outline
        )

        val violations = ThemeUtils.validateColorSchemeAccessibility(customScheme)
        if (violations.isNotEmpty()) {
            val tag = "YoleTheme"
            if (android.util.Log.isLoggable(tag, android.util.Log.WARN)) {
                android.util.Log.w(tag, "Color scheme accessibility violations: ${violations.joinToString(", ")}")
            }
        }

        return customScheme
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
     * Creates Material3 Shapes (using defaults for now, can be customized later).
     */
    fun createShapes(): Shapes {
        return Shapes() // Uses Material3 defaults
    }
}

/**
 * Main theme composable for Android with Material You support.
 */
@Composable
fun YoleAndroidTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDarkTheme = ThemeUtils.shouldUseDarkTheme(themeMode, isSystemInDarkTheme())
    val colorScheme = YoleAndroidTheme.createColorScheme(themeMode, dynamicColor, seedColor)
    val typography = YoleAndroidTheme.createTypography()
    val shapes = YoleAndroidTheme.createShapes()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

/**
 * Convenience function that reads settings and applies the theme.
 */
@Composable
fun YoleAndroidThemeWithSettings(
    settings: YoleSettings,
    content: @Composable () -> Unit
) {
    val themeMode = when (settings.getThemeMode()) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    val dynamicColor = settings.getDynamicColorsEnabled()
    val seedColorHex = settings.getCustomSeedColor()
    val seedColor = seedColorHex?.takeIf { it.isNotEmpty() }?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    YoleAndroidTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        seedColor = seedColor,
        content = content
    )
}
