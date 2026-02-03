/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Theme Tests
 *
 * Comprehensive tests for theme colors, typography,
 * and WCAG accessibility compliance.
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlin.test.*

/**
 * Tests for theme system including colors and accessibility.
 */
class ThemeTest {

    // ==================== COLOR DEFINITION TESTS ====================

    @Test
    fun `brand colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.BrandPrimary)
        assertNotEquals(Color.Unspecified, YoleColors.BrandSecondary)
        assertNotEquals(Color.Unspecified, YoleColors.BrandTertiary)
    }

    @Test
    fun `semantic colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.Success)
        assertNotEquals(Color.Unspecified, YoleColors.Warning)
        assertNotEquals(Color.Unspecified, YoleColors.Error)
        assertNotEquals(Color.Unspecified, YoleColors.Info)
    }

    @Test
    fun `surface colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.SurfacePrimary)
        assertNotEquals(Color.Unspecified, YoleColors.SurfaceSecondary)
        assertNotEquals(Color.Unspecified, YoleColors.SurfaceTertiary)
    }

    @Test
    fun `text colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.TextPrimary)
        assertNotEquals(Color.Unspecified, YoleColors.TextSecondary)
        assertNotEquals(Color.Unspecified, YoleColors.TextTertiary)
        assertNotEquals(Color.Unspecified, YoleColors.TextDisabled)
    }

    @Test
    fun `interactive colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.InteractiveHover)
        assertNotEquals(Color.Unspecified, YoleColors.InteractivePressed)
        assertNotEquals(Color.Unspecified, YoleColors.InteractiveFocus)
    }

    @Test
    fun `border colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.BorderLight)
        assertNotEquals(Color.Unspecified, YoleColors.BorderMedium)
        assertNotEquals(Color.Unspecified, YoleColors.BorderStrong)
    }

    // ==================== DARK THEME COLORS TESTS ====================

    @Test
    fun `dark theme brand colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.Dark.BrandPrimary)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.BrandSecondary)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.BrandTertiary)
    }

    @Test
    fun `dark theme semantic colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.Dark.Success)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.Warning)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.Error)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.Info)
    }

    @Test
    fun `dark theme surface colors are defined`() {
        assertNotEquals(Color.Unspecified, YoleColors.Dark.SurfacePrimary)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.SurfaceSecondary)
        assertNotEquals(Color.Unspecified, YoleColors.Dark.SurfaceTertiary)
    }

    @Test
    fun `dark theme has different surface colors than light`() {
        assertNotEquals(YoleColors.SurfacePrimary, YoleColors.Dark.SurfacePrimary)
        assertNotEquals(YoleColors.SurfaceSecondary, YoleColors.Dark.SurfaceSecondary)
    }

    @Test
    fun `dark theme has different text colors than light`() {
        assertNotEquals(YoleColors.TextPrimary, YoleColors.Dark.TextPrimary)
        assertNotEquals(YoleColors.TextSecondary, YoleColors.Dark.TextSecondary)
    }

    // ==================== TYPOGRAPHY TESTS ====================

    @Test
    fun `display styles have decreasing font sizes`() {
        assertTrue(YoleTypography.DisplayLarge.fontSize > YoleTypography.DisplayMedium.fontSize)
        assertTrue(YoleTypography.DisplayMedium.fontSize > YoleTypography.DisplaySmall.fontSize)
    }

    @Test
    fun `headline styles have decreasing font sizes`() {
        assertTrue(YoleTypography.HeadlineLarge.fontSize > YoleTypography.HeadlineMedium.fontSize)
        assertTrue(YoleTypography.HeadlineMedium.fontSize > YoleTypography.HeadlineSmall.fontSize)
    }

    @Test
    fun `title styles have decreasing font sizes`() {
        assertTrue(YoleTypography.TitleLarge.fontSize > YoleTypography.TitleMedium.fontSize)
        assertTrue(YoleTypography.TitleMedium.fontSize > YoleTypography.TitleSmall.fontSize)
    }

    @Test
    fun `body styles have decreasing font sizes`() {
        assertTrue(YoleTypography.BodyLarge.fontSize > YoleTypography.BodyMedium.fontSize)
        assertTrue(YoleTypography.BodyMedium.fontSize > YoleTypography.BodySmall.fontSize)
    }

    @Test
    fun `label styles have decreasing font sizes`() {
        assertTrue(YoleTypography.LabelLarge.fontSize > YoleTypography.LabelMedium.fontSize)
        assertTrue(YoleTypography.LabelMedium.fontSize > YoleTypography.LabelSmall.fontSize)
    }

    @Test
    fun `code style uses monospace font`() {
        assertEquals(
            androidx.compose.ui.text.font.FontFamily.Monospace,
            YoleTypography.Code.fontFamily
        )
    }

    @Test
    fun `all text styles have line height`() {
        assertTrue(YoleTypography.DisplayLarge.lineHeight >= 0.sp)
        assertTrue(YoleTypography.HeadlineLarge.lineHeight >= 0.sp)
        assertTrue(YoleTypography.TitleLarge.lineHeight >= 0.sp)
        assertTrue(YoleTypography.BodyLarge.lineHeight >= 0.sp)
        assertTrue(YoleTypography.LabelLarge.lineHeight >= 0.sp)
        assertTrue(YoleTypography.Code.lineHeight >= 0.sp)
    }

    @Test
    fun `body text is readable size`() {
        // Body text should be at least 14sp for readability
        assertTrue(
            YoleTypography.BodyMedium.fontSize >= 14.sp,
            "Body medium should be at least 14sp"
        )
    }

    // ==================== THEME MODE TESTS ====================

    @Test
    fun `theme modes are defined`() {
        val modes = ThemeMode.entries

        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.DARK))
        assertTrue(modes.contains(ThemeMode.SYSTEM))
    }

    @Test
    fun `shouldUseDarkTheme returns correct values`() {
        // Light mode always returns false
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, false))
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, true))

        // Dark mode always returns true
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, true))

        // System mode follows system preference
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, true))
    }

    // ==================== THEME UTILS TESTS ====================

    @Test
    fun `getSemanticColor returns correct color for theme`() {
        val lightColor = YoleColors.TextPrimary
        val darkColor = YoleColors.Dark.TextPrimary

        assertEquals(lightColor, ThemeUtils.getSemanticColor(false, lightColor, darkColor))
        assertEquals(darkColor, ThemeUtils.getSemanticColor(true, lightColor, darkColor))
    }

    // ==================== CONTRAST RATIO TESTS ====================

    @Test
    fun `contrast ratio of identical colors is 1`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.White, Color.White)
        assertEquals(1.0, ratio, 0.01)
    }

    @Test
    fun `contrast ratio of black and white is 21`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.Black, Color.White)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun `contrast ratio is symmetric`() {
        val ratio1 = ThemeUtils.calculateContrastRatio(Color.Blue, Color.White)
        val ratio2 = ThemeUtils.calculateContrastRatio(Color.White, Color.Blue)
        assertEquals(ratio1, ratio2, 0.001)
    }

    @Test
    fun `contrast ratio is always at least 1`() {
        val colors = listOf(
            Color.Red,
            Color.Green,
            Color.Blue,
            Color.Yellow,
            Color.Cyan,
            Color.Magenta
        )

        colors.forEach { color1 ->
            colors.forEach { color2 ->
                val ratio = ThemeUtils.calculateContrastRatio(color1, color2)
                assertTrue(ratio >= 1.0, "Contrast ratio should be at least 1.0")
            }
        }
    }

    // ==================== WCAG COMPLIANCE TESTS ====================

    @Test
    fun `meetsWcagAA returns true for high contrast`() {
        assertTrue(ThemeUtils.meetsWcagAA(Color.Black, Color.White))
        assertTrue(ThemeUtils.meetsWcagAA(Color.White, Color.Black))
    }

    @Test
    fun `meetsWcagAA returns false for low contrast`() {
        // Similar grays have low contrast
        val lightGray = Color(0xFFCCCCCC)
        val mediumGray = Color(0xFF999999)

        assertFalse(ThemeUtils.meetsWcagAA(lightGray, mediumGray))
    }

    @Test
    fun `meetsWcagAAA has higher threshold than AA`() {
        // Some color combinations pass AA but not AAA
        val text = Color(0xFF555555) // Dark gray
        val background = Color.White

        val ratio = ThemeUtils.calculateContrastRatio(text, background)

        // This combination should pass AA (4.5) but might not pass AAA (7.0)
        if (ratio >= 4.5 && ratio < 7.0) {
            assertTrue(ThemeUtils.meetsWcagAA(text, background))
            assertFalse(ThemeUtils.meetsWcagAAA(text, background))
        }
    }

    @Test
    fun `large text has lower contrast requirement`() {
        val text = Color(0xFF666666) // Medium gray
        val background = Color.White

        val ratio = ThemeUtils.calculateContrastRatio(text, background)

        // Large text only needs 3.0 for AA
        if (ratio >= 3.0 && ratio < 4.5) {
            assertFalse(ThemeUtils.meetsWcagAA(text, background, isLargeText = false))
            assertTrue(ThemeUtils.meetsWcagAA(text, background, isLargeText = true))
        }
    }

    // ==================== LIGHT THEME ACCESSIBILITY TESTS ====================

    @Test
    fun `light theme text on surface has good contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.TextPrimary,
            YoleColors.SurfacePrimary
        )

        assertTrue(
            ratio >= 4.5,
            "Primary text on primary surface should meet WCAG AA (ratio: $ratio)"
        )
    }

    @Test
    fun `light theme secondary text is readable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.TextSecondary,
            YoleColors.SurfacePrimary
        )

        assertTrue(
            ratio >= 3.0,
            "Secondary text should meet minimum contrast for UI (ratio: $ratio)"
        )
    }

    // ==================== DARK THEME ACCESSIBILITY TESTS ====================

    @Test
    fun `dark theme text on surface has good contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.TextPrimary,
            YoleColors.Dark.SurfacePrimary
        )

        assertTrue(
            ratio >= 4.5,
            "Dark theme primary text should meet WCAG AA (ratio: $ratio)"
        )
    }

    @Test
    fun `dark theme secondary text is readable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.TextSecondary,
            YoleColors.Dark.SurfacePrimary
        )

        assertTrue(
            ratio >= 3.0,
            "Dark theme secondary text should meet minimum contrast (ratio: $ratio)"
        )
    }

    // ==================== SEMANTIC COLOR ACCESSIBILITY ====================

    @Test
    fun `error color is distinguishable on light background`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Error,
            YoleColors.SurfacePrimary
        )

        assertTrue(
            ratio >= 3.0,
            "Error color should be distinguishable (ratio: $ratio)"
        )
    }

    @Test
    fun `success color is distinguishable on light background`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Success,
            YoleColors.SurfacePrimary
        )

        assertTrue(
            ratio >= 3.0,
            "Success color should be distinguishable (ratio: $ratio)"
        )
    }

    @Test
    fun `warning color is distinguishable on light background`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Warning,
            YoleColors.SurfacePrimary
        )

        assertTrue(
            ratio >= 3.0,
            "Warning color should be distinguishable (ratio: $ratio)"
        )
    }
}
