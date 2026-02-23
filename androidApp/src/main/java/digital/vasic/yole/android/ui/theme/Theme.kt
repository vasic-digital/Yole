/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android Theme Implementation for Yole
 *
 * Provides Material You dynamic colors, Android-specific theming,
 * and integration with the shared theme system.
 *
 *########################################################*/

package digital.vasic.yole.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import digital.vasic.yole.android.ui.YoleSettings
import digital.vasic.yole.ui.ThemeMode
import digital.vasic.yole.ui.ThemeUtils
import digital.vasic.yole.ui.YoleColors
import digital.vasic.yole.ui.YoleTypography

/**
 * Android-specific theme configuration with Material You support.
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

        return when {
            // Material You dynamic colors with custom seed (Android 12+)
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                createDynamicColorScheme(isDarkTheme, seedColor)
            }

            // Fallback to custom semantic colors
            else -> {
                createSemanticColorScheme(isDarkTheme)
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
            // Dynamic color schemes are available on Android 12+
            if (seedColor != null) {
                // Use custom seed color for dynamic color generation
                if (isDarkTheme) {
                    dynamicDarkColorScheme(context).copy(
                        primary = seedColor,
                        // Let Material You generate the rest of the palette from the seed
                    )
                } else {
                    dynamicLightColorScheme(context).copy(
                        primary = seedColor,
                        // Let Material You generate the rest of the palette from the seed

                    )
                }
            } else {
                // Use default dynamic color schemes
                if (isDarkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }
        } else {
            // Fallback to static color schemes for older Android versions
            if (isDarkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
        }
    }

    /**
     * Creates a color scheme using semantic color tokens.
     */
    private fun createSemanticColorScheme(isDarkTheme: Boolean): ColorScheme {
        val baseScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

        // Customize the base scheme with our semantic colors
        val customScheme = baseScheme.copy(
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

        // Validate accessibility
        val violations = ThemeUtils.validateColorSchemeAccessibility(customScheme)
        if (violations.isNotEmpty()) {
            // Log accessibility violations using Android's Log class
            // This is only logged in debug builds and doesn't affect release performance
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
            // Safe cast to Activity - only set status bar if context is an Activity
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
    val seedColor = seedColorHex?.let { Color(android.graphics.Color.parseColor(it)) }

    YoleAndroidTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        seedColor = seedColor,
        content = content
    )
}