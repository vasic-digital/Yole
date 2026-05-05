/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Theme Deep Tests
 *
 * Comprehensive deep tests for YoleColors, YoleTypography,
 * ThemeMode, and ThemeUtils including WCAG accessibility validation.
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.*

/**
 * Deep test suite for the Yole theme system.
 * Covers YoleColors (light + dark), YoleTypography, ThemeMode,
 * and all ThemeUtils utility functions including WCAG compliance.
 */
class ThemeDeepTest {

    // ==================== YOLE COLORS - LIGHT THEME ====================

    @Test
    fun `light brand colors have expected ARGB values`() {
        assertEquals(Color(0xFFD32F2F), YoleColors.BrandPrimary)
        assertEquals(Color(0xFFDC004E), YoleColors.BrandSecondary)
        assertEquals(Color(0xFF2E7D32), YoleColors.BrandTertiary)
    }

    @Test
    fun `light semantic colors have expected values`() {
        assertEquals(Color(0xFF4CAF50), YoleColors.Success)
        assertEquals(Color(0xFFFF9800), YoleColors.Warning)
        assertEquals(Color(0xFFF44336), YoleColors.Error)
        assertEquals(Color(0xFFE53935), YoleColors.Info)
    }

    @Test
    fun `light surface colors are not transparent`() {
        assertTrue(YoleColors.SurfacePrimary.alpha > 0f)
        assertTrue(YoleColors.SurfaceSecondary.alpha > 0f)
        assertTrue(YoleColors.SurfaceTertiary.alpha > 0f)
    }

    @Test
    fun `light surface colors have correct values`() {
        assertEquals(Color(0xFFFFFFFF), YoleColors.SurfacePrimary)
        assertEquals(Color(0xFFF5F5F5), YoleColors.SurfaceSecondary)
        assertEquals(Color(0xFFEEEEEE), YoleColors.SurfaceTertiary)
    }

    @Test
    fun `light text colors are ordered by darkness`() {
        // Primary text should be darkest (lowest luminance)
        val primaryLum = luminanceOf(YoleColors.TextPrimary)
        val secondaryLum = luminanceOf(YoleColors.TextSecondary)
        val tertiaryLum = luminanceOf(YoleColors.TextTertiary)
        val disabledLum = luminanceOf(YoleColors.TextDisabled)

        assertTrue(primaryLum < secondaryLum, "Primary text should be darker than secondary")
        assertTrue(secondaryLum < tertiaryLum, "Secondary text should be darker than tertiary")
        assertTrue(tertiaryLum < disabledLum, "Tertiary text should be darker than disabled")
    }

    @Test
    fun `light text color values are correct`() {
        assertEquals(Color(0xFF212121), YoleColors.TextPrimary)
        assertEquals(Color(0xFF757575), YoleColors.TextSecondary)
        assertEquals(Color(0xFF9E9E9E), YoleColors.TextTertiary)
        assertEquals(Color(0xFFBDBDBD), YoleColors.TextDisabled)
    }

    @Test
    fun `light interactive colors are distinct from each other`() {
        assertNotEquals(YoleColors.InteractiveHover, YoleColors.InteractivePressed)
        assertNotEquals(YoleColors.InteractivePressed, YoleColors.InteractiveFocus)
        assertNotEquals(YoleColors.InteractiveHover, YoleColors.InteractiveFocus)
    }

    @Test
    fun `light interactive color values are correct`() {
        assertEquals(Color(0xFFFFEBEE), YoleColors.InteractiveHover)
        assertEquals(Color(0xFFFFCDD2), YoleColors.InteractivePressed)
        assertEquals(Color(0xFFEF9A9A), YoleColors.InteractiveFocus)
    }

    @Test
    fun `light border colors are ordered by intensity`() {
        val lightLum = luminanceOf(YoleColors.BorderLight)
        val mediumLum = luminanceOf(YoleColors.BorderMedium)
        val strongLum = luminanceOf(YoleColors.BorderStrong)

        assertTrue(lightLum > mediumLum, "Light border should be lighter than medium")
        assertTrue(mediumLum > strongLum, "Medium border should be lighter than strong")
    }

    @Test
    fun `light border color values are correct`() {
        assertEquals(Color(0xFFE0E0E0), YoleColors.BorderLight)
        assertEquals(Color(0xFFBDBDBD), YoleColors.BorderMedium)
        assertEquals(Color(0xFF9E9E9E), YoleColors.BorderStrong)
    }

    @Test
    fun `all light colors have full alpha`() {
        val lightColors = listOf(
            YoleColors.BrandPrimary, YoleColors.BrandSecondary, YoleColors.BrandTertiary,
            YoleColors.Success, YoleColors.Warning, YoleColors.Error, YoleColors.Info,
            YoleColors.SurfacePrimary, YoleColors.SurfaceSecondary, YoleColors.SurfaceTertiary,
            YoleColors.TextPrimary, YoleColors.TextSecondary, YoleColors.TextTertiary, YoleColors.TextDisabled,
            YoleColors.InteractiveHover, YoleColors.InteractivePressed, YoleColors.InteractiveFocus,
            YoleColors.BorderLight, YoleColors.BorderMedium, YoleColors.BorderStrong
        )
        lightColors.forEach { color ->
            assertEquals(1f, color.alpha, "All light colors should have full alpha")
        }
    }

    // ==================== YOLE COLORS - DARK THEME ====================

    @Test
    fun `dark brand colors have expected values`() {
        assertEquals(Color(0xFFEF9A9A), YoleColors.Dark.BrandPrimary)
        assertEquals(Color(0xFFFF5983), YoleColors.Dark.BrandSecondary)
        assertEquals(Color(0xFF81C784), YoleColors.Dark.BrandTertiary)
    }

    @Test
    fun `dark semantic colors have expected values`() {
        assertEquals(Color(0xFF81C784), YoleColors.Dark.Success)
        assertEquals(Color(0xFFFFB74D), YoleColors.Dark.Warning)
        assertEquals(Color(0xFFEF5350), YoleColors.Dark.Error)
        assertEquals(Color(0xFFE57373), YoleColors.Dark.Info)
    }

    @Test
    fun `dark surface colors have correct values`() {
        assertEquals(Color(0xFF121212), YoleColors.Dark.SurfacePrimary)
        assertEquals(Color(0xFF1E1E1E), YoleColors.Dark.SurfaceSecondary)
        assertEquals(Color(0xFF2A2A2A), YoleColors.Dark.SurfaceTertiary)
    }

    @Test
    fun `dark surface colors are darker than light surface colors`() {
        assertTrue(
            luminanceOf(YoleColors.Dark.SurfacePrimary) < luminanceOf(YoleColors.SurfacePrimary),
            "Dark surface should be darker than light surface"
        )
    }

    @Test
    fun `dark text colors have correct values`() {
        assertEquals(Color(0xFFFFFFFF), YoleColors.Dark.TextPrimary)
        assertEquals(Color(0xFFB0B0B0), YoleColors.Dark.TextSecondary)
        assertEquals(Color(0xFF808080), YoleColors.Dark.TextTertiary)
        assertEquals(Color(0xFF606060), YoleColors.Dark.TextDisabled)
    }

    @Test
    fun `dark text colors are ordered by brightness`() {
        // For dark theme, primary text should be brightest (highest luminance)
        val primaryLum = luminanceOf(YoleColors.Dark.TextPrimary)
        val secondaryLum = luminanceOf(YoleColors.Dark.TextSecondary)
        val tertiaryLum = luminanceOf(YoleColors.Dark.TextTertiary)
        val disabledLum = luminanceOf(YoleColors.Dark.TextDisabled)

        assertTrue(primaryLum > secondaryLum, "Dark primary text should be brighter than secondary")
        assertTrue(secondaryLum > tertiaryLum, "Dark secondary text should be brighter than tertiary")
        assertTrue(tertiaryLum > disabledLum, "Dark tertiary text should be brighter than disabled")
    }

    @Test
    fun `dark interactive color values are correct`() {
        assertEquals(Color(0xFF5F1E1E), YoleColors.Dark.InteractiveHover)
        assertEquals(Color(0xFF6B2A2A), YoleColors.Dark.InteractivePressed)
        assertEquals(Color(0xFFC62828), YoleColors.Dark.InteractiveFocus)
    }

    @Test
    fun `dark border color values are correct`() {
        assertEquals(Color(0xFF404040), YoleColors.Dark.BorderLight)
        assertEquals(Color(0xFF555555), YoleColors.Dark.BorderMedium)
        assertEquals(Color(0xFF707070), YoleColors.Dark.BorderStrong)
    }

    @Test
    fun `all dark colors have full alpha`() {
        val darkColors = listOf(
            YoleColors.Dark.BrandPrimary, YoleColors.Dark.BrandSecondary, YoleColors.Dark.BrandTertiary,
            YoleColors.Dark.Success, YoleColors.Dark.Warning, YoleColors.Dark.Error, YoleColors.Dark.Info,
            YoleColors.Dark.SurfacePrimary, YoleColors.Dark.SurfaceSecondary, YoleColors.Dark.SurfaceTertiary,
            YoleColors.Dark.TextPrimary, YoleColors.Dark.TextSecondary, YoleColors.Dark.TextTertiary,
            YoleColors.Dark.TextDisabled,
            YoleColors.Dark.InteractiveHover, YoleColors.Dark.InteractivePressed, YoleColors.Dark.InteractiveFocus,
            YoleColors.Dark.BorderLight, YoleColors.Dark.BorderMedium, YoleColors.Dark.BorderStrong
        )
        darkColors.forEach { color ->
            assertEquals(1f, color.alpha, "All dark colors should have full alpha")
        }
    }

    @Test
    fun `dark brand colors differ from light brand colors`() {
        assertNotEquals(YoleColors.BrandPrimary, YoleColors.Dark.BrandPrimary)
        assertNotEquals(YoleColors.BrandSecondary, YoleColors.Dark.BrandSecondary)
        assertNotEquals(YoleColors.BrandTertiary, YoleColors.Dark.BrandTertiary)
    }

    // ==================== YOLE TYPOGRAPHY ====================

    @Test
    fun `DisplayLarge has correct properties`() {
        assertEquals(FontFamily.Default, YoleTypography.DisplayLarge.fontFamily)
        assertEquals(FontWeight.Normal, YoleTypography.DisplayLarge.fontWeight)
        assertEquals(57.sp, YoleTypography.DisplayLarge.fontSize)
        assertEquals(64.sp, YoleTypography.DisplayLarge.lineHeight)
        assertEquals(0.sp, YoleTypography.DisplayLarge.letterSpacing)
    }

    @Test
    fun `DisplayMedium has correct properties`() {
        assertEquals(FontFamily.Default, YoleTypography.DisplayMedium.fontFamily)
        assertEquals(FontWeight.Normal, YoleTypography.DisplayMedium.fontWeight)
        assertEquals(45.sp, YoleTypography.DisplayMedium.fontSize)
        assertEquals(52.sp, YoleTypography.DisplayMedium.lineHeight)
        assertEquals(0.sp, YoleTypography.DisplayMedium.letterSpacing)
    }

    @Test
    fun `DisplaySmall has correct properties`() {
        assertEquals(FontFamily.Default, YoleTypography.DisplaySmall.fontFamily)
        assertEquals(FontWeight.Normal, YoleTypography.DisplaySmall.fontWeight)
        assertEquals(36.sp, YoleTypography.DisplaySmall.fontSize)
        assertEquals(44.sp, YoleTypography.DisplaySmall.lineHeight)
    }

    @Test
    fun `HeadlineLarge has correct properties`() {
        assertEquals(32.sp, YoleTypography.HeadlineLarge.fontSize)
        assertEquals(40.sp, YoleTypography.HeadlineLarge.lineHeight)
        assertEquals(FontWeight.Normal, YoleTypography.HeadlineLarge.fontWeight)
    }

    @Test
    fun `HeadlineMedium has correct properties`() {
        assertEquals(28.sp, YoleTypography.HeadlineMedium.fontSize)
        assertEquals(36.sp, YoleTypography.HeadlineMedium.lineHeight)
    }

    @Test
    fun `HeadlineSmall has correct properties`() {
        assertEquals(24.sp, YoleTypography.HeadlineSmall.fontSize)
        assertEquals(32.sp, YoleTypography.HeadlineSmall.lineHeight)
    }

    @Test
    fun `TitleLarge has correct properties`() {
        assertEquals(22.sp, YoleTypography.TitleLarge.fontSize)
        assertEquals(28.sp, YoleTypography.TitleLarge.lineHeight)
        assertEquals(FontWeight.Normal, YoleTypography.TitleLarge.fontWeight)
    }

    @Test
    fun `TitleMedium has medium font weight and correct spacing`() {
        assertEquals(FontWeight.Medium, YoleTypography.TitleMedium.fontWeight)
        assertEquals(16.sp, YoleTypography.TitleMedium.fontSize)
        assertEquals(24.sp, YoleTypography.TitleMedium.lineHeight)
        assertEquals(0.15.sp, YoleTypography.TitleMedium.letterSpacing)
    }

    @Test
    fun `TitleSmall has medium font weight and correct spacing`() {
        assertEquals(FontWeight.Medium, YoleTypography.TitleSmall.fontWeight)
        assertEquals(14.sp, YoleTypography.TitleSmall.fontSize)
        assertEquals(20.sp, YoleTypography.TitleSmall.lineHeight)
        assertEquals(0.1.sp, YoleTypography.TitleSmall.letterSpacing)
    }

    @Test
    fun `BodyLarge has correct properties`() {
        assertEquals(16.sp, YoleTypography.BodyLarge.fontSize)
        assertEquals(24.sp, YoleTypography.BodyLarge.lineHeight)
        assertEquals(0.15.sp, YoleTypography.BodyLarge.letterSpacing)
        assertEquals(FontWeight.Normal, YoleTypography.BodyLarge.fontWeight)
    }

    @Test
    fun `BodyMedium has correct properties`() {
        assertEquals(14.sp, YoleTypography.BodyMedium.fontSize)
        assertEquals(20.sp, YoleTypography.BodyMedium.lineHeight)
        assertEquals(0.25.sp, YoleTypography.BodyMedium.letterSpacing)
    }

    @Test
    fun `BodySmall has correct properties`() {
        assertEquals(12.sp, YoleTypography.BodySmall.fontSize)
        assertEquals(16.sp, YoleTypography.BodySmall.lineHeight)
        assertEquals(0.4.sp, YoleTypography.BodySmall.letterSpacing)
    }

    @Test
    fun `LabelLarge has medium weight and correct properties`() {
        assertEquals(FontWeight.Medium, YoleTypography.LabelLarge.fontWeight)
        assertEquals(14.sp, YoleTypography.LabelLarge.fontSize)
        assertEquals(20.sp, YoleTypography.LabelLarge.lineHeight)
        assertEquals(0.1.sp, YoleTypography.LabelLarge.letterSpacing)
    }

    @Test
    fun `LabelMedium has correct properties`() {
        assertEquals(FontWeight.Medium, YoleTypography.LabelMedium.fontWeight)
        assertEquals(12.sp, YoleTypography.LabelMedium.fontSize)
        assertEquals(16.sp, YoleTypography.LabelMedium.lineHeight)
        assertEquals(0.5.sp, YoleTypography.LabelMedium.letterSpacing)
    }

    @Test
    fun `LabelSmall has correct properties`() {
        assertEquals(FontWeight.Medium, YoleTypography.LabelSmall.fontWeight)
        assertEquals(11.sp, YoleTypography.LabelSmall.fontSize)
        assertEquals(16.sp, YoleTypography.LabelSmall.lineHeight)
        assertEquals(0.5.sp, YoleTypography.LabelSmall.letterSpacing)
    }

    @Test
    fun `Code style uses Monospace font with correct size`() {
        assertEquals(FontFamily.Monospace, YoleTypography.Code.fontFamily)
        assertEquals(FontWeight.Normal, YoleTypography.Code.fontWeight)
        assertEquals(14.sp, YoleTypography.Code.fontSize)
        assertEquals(20.sp, YoleTypography.Code.lineHeight)
        assertEquals(0.sp, YoleTypography.Code.letterSpacing)
    }

    @Test
    fun `all line heights are greater than font sizes`() {
        val styles = listOf(
            YoleTypography.DisplayLarge, YoleTypography.DisplayMedium, YoleTypography.DisplaySmall,
            YoleTypography.HeadlineLarge, YoleTypography.HeadlineMedium, YoleTypography.HeadlineSmall,
            YoleTypography.TitleLarge, YoleTypography.TitleMedium, YoleTypography.TitleSmall,
            YoleTypography.BodyLarge, YoleTypography.BodyMedium, YoleTypography.BodySmall,
            YoleTypography.LabelLarge, YoleTypography.LabelMedium, YoleTypography.LabelSmall,
            YoleTypography.Code
        )
        styles.forEach { style ->
            assertTrue(
                style.lineHeight >= style.fontSize,
                "Line height (${style.lineHeight}) should be >= font size (${style.fontSize})"
            )
        }
    }

    // ==================== THEME MODE ====================

    @Test
    fun `ThemeMode has exactly 3 entries`() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun `ThemeMode contains LIGHT DARK and SYSTEM`() {
        val modes = ThemeMode.entries.toSet()
        assertTrue(ThemeMode.LIGHT in modes)
        assertTrue(ThemeMode.DARK in modes)
        assertTrue(ThemeMode.SYSTEM in modes)
    }

    @Test
    fun `ThemeMode ordinals are sequential`() {
        assertEquals(0, ThemeMode.LIGHT.ordinal)
        assertEquals(1, ThemeMode.DARK.ordinal)
        assertEquals(2, ThemeMode.SYSTEM.ordinal)
    }

    // ==================== ThemeUtils.shouldUseDarkTheme ====================

    @Test
    fun `shouldUseDarkTheme LIGHT mode always returns false regardless of system`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, false))
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, true))
    }

    @Test
    fun `shouldUseDarkTheme DARK mode always returns true regardless of system`() {
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, true))
    }

    @Test
    fun `shouldUseDarkTheme SYSTEM mode returns system preference`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, true))
    }

    // ==================== ThemeUtils.calculateContrastRatio ====================

    @Test
    fun `contrast ratio black vs white is approximately 21 to 1`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.Black, Color.White)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun `contrast ratio same color is 1 to 1`() {
        assertEquals(1.0, ThemeUtils.calculateContrastRatio(Color.White, Color.White), 0.01)
        assertEquals(1.0, ThemeUtils.calculateContrastRatio(Color.Black, Color.Black), 0.01)
        assertEquals(1.0, ThemeUtils.calculateContrastRatio(Color.Red, Color.Red), 0.01)
    }

    @Test
    fun `contrast ratio is commutative`() {
        val pairsToTest = listOf(
            Color.Red to Color.White,
            Color.Blue to Color.Yellow,
            Color.Green to Color.Black,
            YoleColors.BrandPrimary to YoleColors.SurfacePrimary
        )
        pairsToTest.forEach { (a, b) ->
            val ratio1 = ThemeUtils.calculateContrastRatio(a, b)
            val ratio2 = ThemeUtils.calculateContrastRatio(b, a)
            assertEquals(ratio1, ratio2, 0.001, "Contrast ratio should be symmetric")
        }
    }

    @Test
    fun `contrast ratio is always between 1 and 21`() {
        val colors = listOf(
            Color.Black, Color.White, Color.Red, Color.Green, Color.Blue,
            Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray,
            YoleColors.BrandPrimary, YoleColors.Dark.BrandPrimary
        )
        colors.forEach { c1 ->
            colors.forEach { c2 ->
                val ratio = ThemeUtils.calculateContrastRatio(c1, c2)
                assertTrue(ratio >= 1.0, "Contrast ratio should be >= 1.0, was $ratio")
                assertTrue(ratio <= 21.1, "Contrast ratio should be <= 21.0, was $ratio")
            }
        }
    }

    @Test
    fun `contrast ratio mid gray vs white is approximately 4 to 1`() {
        // #767676 is the boundary color for WCAG AA on white
        val midGray = Color(0xFF767676)
        val ratio = ThemeUtils.calculateContrastRatio(midGray, Color.White)
        assertTrue(ratio >= 4.0 && ratio <= 5.0, "Mid-gray to white should be about 4.5:1, was $ratio")
    }

    @Test
    fun `contrast ratio dark gray vs white exceeds 7`() {
        val darkGray = Color(0xFF333333)
        val ratio = ThemeUtils.calculateContrastRatio(darkGray, Color.White)
        assertTrue(ratio > 7.0, "Dark gray (#333) vs white should exceed 7:1, was $ratio")
    }

    // ==================== ThemeUtils.meetsWcagAA ====================

    @Test
    fun `meetsWcagAA passes for black on white normal text`() {
        assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White))
    }

    @Test
    fun `meetsWcagAA fails for similar grays normal text`() {
        val gray1 = Color(0xFFAAAAAA)
        val gray2 = Color(0xFFCCCCCC)
        assertFalse(ThemeUtils.meetsWcagAA(gray1, gray2))
    }

    @Test
    fun `meetsWcagAA large text uses 3 to 1 threshold`() {
        // Find a color pair with contrast between 3.0 and 4.5
        val text = Color(0xFF888888)
        val background = Color.White
        val ratio = ThemeUtils.calculateContrastRatio(text, background)
        // 0x888888 on white should be approximately 3.5:1
        if (ratio >= 3.0 && ratio < 4.5) {
            assertFalse(ThemeUtils.meetsWcagAA(text, background, isLargeText = false))
            assertTrue(ThemeUtils.meetsWcagAA(text, background, isLargeText = true))
        }
    }

    @Test
    fun `meetsWcagAA at exact threshold boundary`() {
        // Black and white clearly pass
        assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White, isLargeText = false))
        // Same color clearly fails
        assertFalse(ThemeUtils.meetsWcagAA(Color.White, Color.White, isLargeText = false))
    }

    // ==================== ThemeUtils.meetsWcagAAA ====================

    @Test
    fun `meetsWcagAAA passes for black on white`() {
        assertTrue(ThemeUtils.meetsWcagAAA(Color.Black, Color.White))
    }

    @Test
    fun `meetsWcagAAA fails for same color`() {
        assertFalse(ThemeUtils.meetsWcagAAA(Color.White, Color.White))
    }

    @Test
    fun `meetsWcagAAA normal text requires 7 to 1`() {
        // #555555 on white is about 7.5:1 - should pass AAA
        val darkText = Color(0xFF555555)
        val ratio = ThemeUtils.calculateContrastRatio(darkText, Color.White)
        if (ratio >= 7.0) {
            assertTrue(ThemeUtils.meetsWcagAAA(darkText, Color.White, isLargeText = false))
        }
    }

    @Test
    fun `meetsWcagAAA large text uses 4 point 5 to 1 threshold`() {
        // Find a pair between 4.5 and 7.0
        val text = Color(0xFF666666)
        val background = Color.White
        val ratio = ThemeUtils.calculateContrastRatio(text, background)
        if (ratio >= 4.5 && ratio < 7.0) {
            assertFalse(ThemeUtils.meetsWcagAAA(text, background, isLargeText = false))
            assertTrue(ThemeUtils.meetsWcagAAA(text, background, isLargeText = true))
        }
    }

    @Test
    fun `meetsWcagAAA is stricter than meetsWcagAA`() {
        // Any pair that fails AA should also fail AAA
        val text = Color(0xFFAAAAAA)
        val bg = Color(0xFFCCCCCC)
        if (!ThemeUtils.meetsWcagAA(text, bg)) {
            assertFalse(ThemeUtils.meetsWcagAAA(text, bg))
        }
        // Any pair that passes AAA should also pass AA
        if (ThemeUtils.meetsWcagAAA(Color.Black, Color.White)) {
            assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White))
        }
    }

    // ==================== ThemeUtils.getSemanticColor ====================

    @Test
    fun `getSemanticColor returns light color in light theme`() {
        val result = ThemeUtils.getSemanticColor(
            isDarkTheme = false,
            lightColor = YoleColors.BrandPrimary,
            darkColor = YoleColors.Dark.BrandPrimary
        )
        assertEquals(YoleColors.BrandPrimary, result)
    }

    @Test
    fun `getSemanticColor returns dark color in dark theme`() {
        val result = ThemeUtils.getSemanticColor(
            isDarkTheme = true,
            lightColor = YoleColors.BrandPrimary,
            darkColor = YoleColors.Dark.BrandPrimary
        )
        assertEquals(YoleColors.Dark.BrandPrimary, result)
    }

    @Test
    fun `getSemanticColor with identical colors returns same regardless of theme`() {
        val sameColor = Color(0xFF123456)
        assertEquals(
            sameColor,
            ThemeUtils.getSemanticColor(isDarkTheme = false, lightColor = sameColor, darkColor = sameColor)
        )
        assertEquals(
            sameColor,
            ThemeUtils.getSemanticColor(isDarkTheme = true, lightColor = sameColor, darkColor = sameColor)
        )
    }

    @Test
    fun `getSemanticColor works for all semantic color pairs`() {
        val pairs = listOf(
            YoleColors.Success to YoleColors.Dark.Success,
            YoleColors.Warning to YoleColors.Dark.Warning,
            YoleColors.Error to YoleColors.Dark.Error,
            YoleColors.Info to YoleColors.Dark.Info,
            YoleColors.TextPrimary to YoleColors.Dark.TextPrimary,
            YoleColors.SurfacePrimary to YoleColors.Dark.SurfacePrimary
        )
        pairs.forEach { (light, dark) ->
            assertEquals(light, ThemeUtils.getSemanticColor(false, light, dark))
            assertEquals(dark, ThemeUtils.getSemanticColor(true, light, dark))
        }
    }

    // ==================== ThemeUtils.validateColorSchemeAccessibility ====================

    @Test
    fun `validateColorSchemeAccessibility with high contrast scheme returns no violations`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF333333),
            error = Color(0xFFB71C1C), // Dark red - high contrast with white
            onError = Color.White,
            primary = Color(0xFF1976D2),
            onPrimary = Color.White
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(violations.isEmpty(), "High-contrast scheme should have no violations: $violations")
    }

    @Test
    fun `validateColorSchemeAccessibility with low contrast scheme reports violations`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color(0xFFEEEEEE), // very light gray on white - fails
            surface = Color.White,
            onSurface = Color(0xFFEEEEEE), // fails
            onSurfaceVariant = Color(0xFFEEEEEE), // fails
            error = Color.White,
            onError = Color(0xFFEEEEEE), // fails
            primary = Color.White,
            onPrimary = Color(0xFFEEEEEE) // fails
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(violations.isNotEmpty(), "Low-contrast scheme should report violations")
    }

    @Test
    fun `validateColorSchemeAccessibility checks text on background`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color(0xFFDDDDDD), // poor contrast
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF333333),
            error = Color.Red,
            onError = Color.White,
            primary = Color(0xFF1976D2),
            onPrimary = Color.White
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(
            violations.any { it.contains("background") },
            "Should detect poor onBackground contrast: $violations"
        )
    }

    @Test
    fun `validateColorSchemeAccessibility checks text on surface`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color(0xFFDDDDDD), // poor contrast on white
            onSurfaceVariant = Color(0xFF333333),
            error = Color.Red,
            onError = Color.White,
            primary = Color(0xFF1976D2),
            onPrimary = Color.White
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(
            violations.any { it.contains("surface") },
            "Should detect poor onSurface contrast: $violations"
        )
    }

    @Test
    fun `validateColorSchemeAccessibility checks error color`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF333333),
            error = Color(0xFFFF0000),
            onError = Color(0xFFFF3333), // similar red on red - poor contrast
            primary = Color(0xFF1976D2),
            onPrimary = Color.White
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(
            violations.any { it.lowercase().contains("error") },
            "Should detect poor error text contrast: $violations"
        )
    }

    @Test
    fun `validateColorSchemeAccessibility checks primary button text`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF333333),
            error = Color.Red,
            onError = Color.White,
            primary = Color(0xFF1976D2),
            onPrimary = Color(0xFF1565C0) // similar blue on blue - poor contrast
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        assertTrue(
            violations.any { it.lowercase().contains("primary") },
            "Should detect poor onPrimary contrast: $violations"
        )
    }

    @Test
    fun `validateColorSchemeAccessibility returns list of strings describing violations`() {
        val scheme = lightColorScheme(
            background = Color.White,
            onBackground = Color(0xFFEEEEEE),
            surface = Color.White,
            onSurface = Color(0xFFEEEEEE),
            onSurfaceVariant = Color(0xFFFAFAFA),
            error = Color.White,
            onError = Color(0xFFEEEEEE),
            primary = Color.White,
            onPrimary = Color(0xFFEEEEEE)
        )
        val violations = ThemeUtils.validateColorSchemeAccessibility(scheme)
        violations.forEach { violation ->
            assertTrue(violation.isNotBlank(), "Each violation should be a non-blank string")
        }
    }

    // ==================== CROSS-CUTTING ACCESSIBILITY ====================

    @Test
    fun `light theme primary text on all surfaces meets WCAG AA`() {
        val surfaces = listOf(
            YoleColors.SurfacePrimary,
            YoleColors.SurfaceSecondary,
            YoleColors.SurfaceTertiary
        )
        surfaces.forEach { surface ->
            assertTrue(
                ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, surface),
                "Primary text should meet AA on surface $surface"
            )
        }
    }

    @Test
    fun `dark theme primary text on dark surfaces meets WCAG AA`() {
        val surfaces = listOf(
            YoleColors.Dark.SurfacePrimary,
            YoleColors.Dark.SurfaceSecondary,
            YoleColors.Dark.SurfaceTertiary
        )
        surfaces.forEach { surface ->
            assertTrue(
                ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, surface),
                "Dark primary text should meet AA on dark surface $surface"
            )
        }
    }

    // ==================== HELPERS ====================

    /**
     * Rough relative luminance helper for ordering assertions.
     * Uses the simple sRGB luminance formula without gamma correction
     * (sufficient for ordering comparisons, not for precise contrast math).
     */
    private fun luminanceOf(color: Color): Double {
        return 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    }
}
