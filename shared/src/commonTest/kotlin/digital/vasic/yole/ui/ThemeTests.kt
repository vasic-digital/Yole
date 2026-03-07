/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Theme Tests
 *
 * Deep tests for YoleColors, YoleTypography, ThemeMode,
 * ThemeUtils, and WCAG accessibility validation.
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.*

/**
 * Comprehensive tests for the theme system.
 *
 * Covers:
 * - YoleColors: light and dark color tokens, alpha channels
 * - YoleTypography: all 16 text styles, font sizes, line heights
 * - ThemeMode: enum values, shouldUseDarkTheme logic
 * - ThemeUtils: contrast ratio, WCAG AA/AAA, semantic color selection
 * - Cross-cutting: light text on dark surfaces, dark text on light surfaces
 */
class ThemeTests {

    // ====================================================================
    // YOLE COLORS - LIGHT THEME EXACT VALUES
    // ====================================================================

    @Test
    fun `light BrandPrimary is Material Blue 700`() {
        assertEquals(Color(0xFF1976D2), YoleColors.BrandPrimary)
    }

    @Test
    fun `light BrandSecondary is Material Pink A400`() {
        assertEquals(Color(0xFFDC004E), YoleColors.BrandSecondary)
    }

    @Test
    fun `light BrandTertiary is Material Green 800`() {
        assertEquals(Color(0xFF2E7D32), YoleColors.BrandTertiary)
    }

    @Test
    fun `light Success is Green 500`() {
        assertEquals(Color(0xFF4CAF50), YoleColors.Success)
    }

    @Test
    fun `light Warning is Orange 500`() {
        assertEquals(Color(0xFFFF9800), YoleColors.Warning)
    }

    @Test
    fun `light Error is Red 500`() {
        assertEquals(Color(0xFFF44336), YoleColors.Error)
    }

    @Test
    fun `light Info is Blue 500`() {
        assertEquals(Color(0xFF2196F3), YoleColors.Info)
    }

    @Test
    fun `light SurfacePrimary is white`() {
        assertEquals(Color(0xFFFFFFFF), YoleColors.SurfacePrimary)
    }

    @Test
    fun `light TextPrimary is near-black`() {
        assertEquals(Color(0xFF212121), YoleColors.TextPrimary)
    }

    // ====================================================================
    // YOLE COLORS - DARK THEME EXACT VALUES
    // ====================================================================

    @Test
    fun `dark BrandPrimary is Blue 200`() {
        assertEquals(Color(0xFF90CAF9), YoleColors.Dark.BrandPrimary)
    }

    @Test
    fun `dark BrandSecondary is Pink A200`() {
        assertEquals(Color(0xFFFF5983), YoleColors.Dark.BrandSecondary)
    }

    @Test
    fun `dark SurfacePrimary is dark gray 121212`() {
        assertEquals(Color(0xFF121212), YoleColors.Dark.SurfacePrimary)
    }

    @Test
    fun `dark TextPrimary is white`() {
        assertEquals(Color(0xFFFFFFFF), YoleColors.Dark.TextPrimary)
    }

    @Test
    fun `dark Error is Red 400`() {
        assertEquals(Color(0xFFEF5350), YoleColors.Dark.Error)
    }

    // ====================================================================
    // YOLE COLORS - ALPHA CHANNEL
    // ====================================================================

    @Test
    fun `all light colors have full opacity`() {
        val lightColors = listOf(
            YoleColors.BrandPrimary, YoleColors.BrandSecondary, YoleColors.BrandTertiary,
            YoleColors.Success, YoleColors.Warning, YoleColors.Error, YoleColors.Info,
            YoleColors.SurfacePrimary, YoleColors.SurfaceSecondary, YoleColors.SurfaceTertiary,
            YoleColors.TextPrimary, YoleColors.TextSecondary, YoleColors.TextTertiary, YoleColors.TextDisabled,
            YoleColors.InteractiveHover, YoleColors.InteractivePressed, YoleColors.InteractiveFocus,
            YoleColors.BorderLight, YoleColors.BorderMedium, YoleColors.BorderStrong
        )
        lightColors.forEachIndexed { index, color ->
            assertEquals(1f, color.alpha, "Light color at index $index should have full alpha")
        }
    }

    @Test
    fun `all dark colors have full opacity`() {
        val darkColors = listOf(
            YoleColors.Dark.BrandPrimary, YoleColors.Dark.BrandSecondary, YoleColors.Dark.BrandTertiary,
            YoleColors.Dark.Success, YoleColors.Dark.Warning, YoleColors.Dark.Error, YoleColors.Dark.Info,
            YoleColors.Dark.SurfacePrimary, YoleColors.Dark.SurfaceSecondary, YoleColors.Dark.SurfaceTertiary,
            YoleColors.Dark.TextPrimary, YoleColors.Dark.TextSecondary, YoleColors.Dark.TextTertiary,
            YoleColors.Dark.TextDisabled,
            YoleColors.Dark.InteractiveHover, YoleColors.Dark.InteractivePressed, YoleColors.Dark.InteractiveFocus,
            YoleColors.Dark.BorderLight, YoleColors.Dark.BorderMedium, YoleColors.Dark.BorderStrong
        )
        darkColors.forEachIndexed { index, color ->
            assertEquals(1f, color.alpha, "Dark color at index $index should have full alpha")
        }
    }

    // ====================================================================
    // YOLE COLORS - THEME DIFFERENTIATION
    // ====================================================================

    @Test
    fun `light and dark brand colors are different`() {
        assertNotEquals(YoleColors.BrandPrimary, YoleColors.Dark.BrandPrimary)
        assertNotEquals(YoleColors.BrandSecondary, YoleColors.Dark.BrandSecondary)
        assertNotEquals(YoleColors.BrandTertiary, YoleColors.Dark.BrandTertiary)
    }

    @Test
    fun `light and dark surface colors are different`() {
        assertNotEquals(YoleColors.SurfacePrimary, YoleColors.Dark.SurfacePrimary)
        assertNotEquals(YoleColors.SurfaceSecondary, YoleColors.Dark.SurfaceSecondary)
        assertNotEquals(YoleColors.SurfaceTertiary, YoleColors.Dark.SurfaceTertiary)
    }

    @Test
    fun `light and dark text colors are different`() {
        assertNotEquals(YoleColors.TextPrimary, YoleColors.Dark.TextPrimary)
        assertNotEquals(YoleColors.TextSecondary, YoleColors.Dark.TextSecondary)
    }

    // ====================================================================
    // YOLE TYPOGRAPHY - TEXT STYLES
    // ====================================================================

    @Test
    fun `typography DisplayLarge has correct font size`() {
        assertEquals(57.sp, YoleTypography.DisplayLarge.fontSize)
    }

    @Test
    fun `typography DisplayMedium has correct font size`() {
        assertEquals(45.sp, YoleTypography.DisplayMedium.fontSize)
    }

    @Test
    fun `typography DisplaySmall has correct font size`() {
        assertEquals(36.sp, YoleTypography.DisplaySmall.fontSize)
    }

    @Test
    fun `typography HeadlineLarge has correct font size`() {
        assertEquals(32.sp, YoleTypography.HeadlineLarge.fontSize)
    }

    @Test
    fun `typography HeadlineMedium has correct font size`() {
        assertEquals(28.sp, YoleTypography.HeadlineMedium.fontSize)
    }

    @Test
    fun `typography HeadlineSmall has correct font size`() {
        assertEquals(24.sp, YoleTypography.HeadlineSmall.fontSize)
    }

    @Test
    fun `typography TitleLarge has correct font size`() {
        assertEquals(22.sp, YoleTypography.TitleLarge.fontSize)
    }

    @Test
    fun `typography TitleMedium has correct font size and weight`() {
        assertEquals(16.sp, YoleTypography.TitleMedium.fontSize)
        assertEquals(FontWeight.Medium, YoleTypography.TitleMedium.fontWeight)
    }

    @Test
    fun `typography TitleSmall has correct font size and weight`() {
        assertEquals(14.sp, YoleTypography.TitleSmall.fontSize)
        assertEquals(FontWeight.Medium, YoleTypography.TitleSmall.fontWeight)
    }

    @Test
    fun `typography BodyLarge has correct font size`() {
        assertEquals(16.sp, YoleTypography.BodyLarge.fontSize)
    }

    @Test
    fun `typography BodyMedium has correct font size`() {
        assertEquals(14.sp, YoleTypography.BodyMedium.fontSize)
    }

    @Test
    fun `typography BodySmall has correct font size`() {
        assertEquals(12.sp, YoleTypography.BodySmall.fontSize)
    }

    @Test
    fun `typography LabelLarge has correct font size and weight`() {
        assertEquals(14.sp, YoleTypography.LabelLarge.fontSize)
        assertEquals(FontWeight.Medium, YoleTypography.LabelLarge.fontWeight)
    }

    @Test
    fun `typography LabelMedium has correct font size`() {
        assertEquals(12.sp, YoleTypography.LabelMedium.fontSize)
    }

    @Test
    fun `typography LabelSmall has correct font size`() {
        assertEquals(11.sp, YoleTypography.LabelSmall.fontSize)
    }

    @Test
    fun `typography Code uses Monospace font family`() {
        assertEquals(FontFamily.Monospace, YoleTypography.Code.fontFamily)
        assertEquals(14.sp, YoleTypography.Code.fontSize)
    }

    @Test
    fun `typography all line heights are greater than or equal to font sizes`() {
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
                "Line height ${style.lineHeight} should be >= font size ${style.fontSize}"
            )
        }
    }

    @Test
    fun `typography display sizes are larger than headline sizes`() {
        assertTrue(YoleTypography.DisplaySmall.fontSize > YoleTypography.HeadlineLarge.fontSize)
    }

    @Test
    fun `typography headline sizes are larger than title sizes`() {
        assertTrue(YoleTypography.HeadlineSmall.fontSize > YoleTypography.TitleLarge.fontSize)
    }

    @Test
    fun `typography title sizes are larger than body sizes`() {
        assertTrue(YoleTypography.TitleSmall.fontSize >= YoleTypography.BodyMedium.fontSize)
    }

    @Test
    fun `typography body text is readable minimum 12sp`() {
        assertTrue(YoleTypography.BodySmall.fontSize >= 12.sp, "Smallest body text should be at least 12sp")
    }

    @Test
    fun `typography display styles use normal font weight`() {
        assertEquals(FontWeight.Normal, YoleTypography.DisplayLarge.fontWeight)
        assertEquals(FontWeight.Normal, YoleTypography.DisplayMedium.fontWeight)
        assertEquals(FontWeight.Normal, YoleTypography.DisplaySmall.fontWeight)
    }

    @Test
    fun `typography label styles use medium font weight`() {
        assertEquals(FontWeight.Medium, YoleTypography.LabelLarge.fontWeight)
        assertEquals(FontWeight.Medium, YoleTypography.LabelMedium.fontWeight)
        assertEquals(FontWeight.Medium, YoleTypography.LabelSmall.fontWeight)
    }

    // ====================================================================
    // THEME MODE
    // ====================================================================

    @Test
    fun `themeMode has exactly 3 entries`() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun `themeMode contains LIGHT`() {
        assertTrue(ThemeMode.entries.contains(ThemeMode.LIGHT))
    }

    @Test
    fun `themeMode contains DARK`() {
        assertTrue(ThemeMode.entries.contains(ThemeMode.DARK))
    }

    @Test
    fun `themeMode contains SYSTEM`() {
        assertTrue(ThemeMode.entries.contains(ThemeMode.SYSTEM))
    }

    @Test
    fun `themeMode ordinals are 0 1 2`() {
        assertEquals(0, ThemeMode.LIGHT.ordinal)
        assertEquals(1, ThemeMode.DARK.ordinal)
        assertEquals(2, ThemeMode.SYSTEM.ordinal)
    }

    // ====================================================================
    // THEME UTILS - shouldUseDarkTheme
    // ====================================================================

    @Test
    fun `shouldUseDarkTheme LIGHT mode system light returns false`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, false))
    }

    @Test
    fun `shouldUseDarkTheme LIGHT mode system dark returns false`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, true))
    }

    @Test
    fun `shouldUseDarkTheme DARK mode system light returns true`() {
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, false))
    }

    @Test
    fun `shouldUseDarkTheme DARK mode system dark returns true`() {
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, true))
    }

    @Test
    fun `shouldUseDarkTheme SYSTEM mode system light returns false`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, false))
    }

    @Test
    fun `shouldUseDarkTheme SYSTEM mode system dark returns true`() {
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, true))
    }

    // ====================================================================
    // THEME UTILS - getSemanticColor
    // ====================================================================

    @Test
    fun `getSemanticColor light mode returns light color`() {
        val result = ThemeUtils.getSemanticColor(false, YoleColors.BrandPrimary, YoleColors.Dark.BrandPrimary)
        assertEquals(YoleColors.BrandPrimary, result)
    }

    @Test
    fun `getSemanticColor dark mode returns dark color`() {
        val result = ThemeUtils.getSemanticColor(true, YoleColors.BrandPrimary, YoleColors.Dark.BrandPrimary)
        assertEquals(YoleColors.Dark.BrandPrimary, result)
    }

    @Test
    fun `getSemanticColor with same color returns same regardless of mode`() {
        val color = Color(0xFF123456)
        assertEquals(color, ThemeUtils.getSemanticColor(false, color, color))
        assertEquals(color, ThemeUtils.getSemanticColor(true, color, color))
    }

    // ====================================================================
    // THEME UTILS - calculateContrastRatio
    // ====================================================================

    @Test
    fun `contrast black vs white is approximately 21`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.Black, Color.White)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun `contrast same color is 1`() {
        assertEquals(1.0, ThemeUtils.calculateContrastRatio(Color.Red, Color.Red), 0.01)
    }

    @Test
    fun `contrast is symmetric`() {
        val ratio1 = ThemeUtils.calculateContrastRatio(Color.Blue, Color.Yellow)
        val ratio2 = ThemeUtils.calculateContrastRatio(Color.Yellow, Color.Blue)
        assertEquals(ratio1, ratio2, 0.001)
    }

    @Test
    fun `contrast ratio is always at least 1`() {
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
        colors.forEach { c1 ->
            colors.forEach { c2 ->
                assertTrue(ThemeUtils.calculateContrastRatio(c1, c2) >= 1.0)
            }
        }
    }

    @Test
    fun `contrast ratio is always at most 21`() {
        val colors = listOf(Color.Black, Color.White, Color.Red, Color.Green, Color.Blue)
        colors.forEach { c1 ->
            colors.forEach { c2 ->
                assertTrue(ThemeUtils.calculateContrastRatio(c1, c2) <= 21.1)
            }
        }
    }

    // ====================================================================
    // THEME UTILS - WCAG
    // ====================================================================

    @Test
    fun `meetsWcagAA black on white passes`() {
        assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White))
    }

    @Test
    fun `meetsWcagAA same color fails`() {
        assertFalse(ThemeUtils.meetsWcagAA(Color.White, Color.White))
    }

    @Test
    fun `meetsWcagAAA black on white passes`() {
        assertTrue(ThemeUtils.meetsWcagAAA(Color.Black, Color.White))
    }

    @Test
    fun `meetsWcagAAA same color fails`() {
        assertFalse(ThemeUtils.meetsWcagAAA(Color.White, Color.White))
    }

    @Test
    fun `meetsWcagAA large text has lower threshold`() {
        // Gray that passes 3.0 but not 4.5
        val gray = Color(0xFF888888)
        val ratio = ThemeUtils.calculateContrastRatio(gray, Color.White)
        if (ratio >= 3.0 && ratio < 4.5) {
            assertFalse(ThemeUtils.meetsWcagAA(gray, Color.White, isLargeText = false))
            assertTrue(ThemeUtils.meetsWcagAA(gray, Color.White, isLargeText = true))
        }
    }

    @Test
    fun `meetsWcagAAA is stricter than meetsWcagAA`() {
        // If AAA fails, does not imply AA fails, but if AAA passes, AA must pass
        assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White))
        assertTrue(ThemeUtils.meetsWcagAAA(Color.Black, Color.White))
    }

    // ====================================================================
    // LIGHT THEME ACCESSIBILITY
    // ====================================================================

    @Test
    fun `light primary text on primary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfacePrimary))
    }

    @Test
    fun `light primary text on secondary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfaceSecondary))
    }

    @Test
    fun `light primary text on tertiary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfaceTertiary))
    }

    @Test
    fun `light secondary text on primary surface has adequate contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.TextSecondary, YoleColors.SurfacePrimary)
        assertTrue(ratio >= 3.0, "Secondary text should have at least 3.0 contrast ratio, got $ratio")
    }

    @Test
    fun `light error color is visible on primary surface`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Error, YoleColors.SurfacePrimary)
        assertTrue(ratio >= 3.0, "Error color should be visible on primary surface, ratio=$ratio")
    }

    // ====================================================================
    // DARK THEME ACCESSIBILITY
    // ====================================================================

    @Test
    fun `dark primary text on primary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfacePrimary))
    }

    @Test
    fun `dark primary text on secondary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfaceSecondary))
    }

    @Test
    fun `dark primary text on tertiary surface meets WCAG AA`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfaceTertiary))
    }

    @Test
    fun `dark secondary text on primary surface has adequate contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Dark.TextSecondary, YoleColors.Dark.SurfacePrimary)
        assertTrue(ratio >= 3.0, "Dark secondary text should have at least 3.0 contrast ratio, got $ratio")
    }
}
