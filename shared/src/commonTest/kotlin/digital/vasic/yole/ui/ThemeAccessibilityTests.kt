/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Theme accessibility tests focusing on WCAG compliance,
 * contrast ratios, high contrast mode, font size scaling,
 * and color blindness compatibility.
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.*

/**
 * Theme accessibility tests for Yole.
 *
 * Tests cover:
 * - Light theme contrast ratios for WCAG AA and AAA
 * - Dark theme contrast ratios for WCAG AA and AAA
 * - High contrast mode requirements
 * - Font size scaling and readability
 * - Color blindness compatibility
 * - Semantic color accessibility
 * - Interactive element contrast
 * - Border visibility
 * - Typography hierarchy accessibility
 */
class ThemeAccessibilityTests {

    // ==================== Light Theme Contrast Ratios ====================

    @Test
    fun `light theme TextPrimary on SurfacePrimary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfacePrimary),
            "Primary text on primary surface must meet WCAG AA (4.5:1)"
        )
    }

    @Test
    fun `light theme TextPrimary on SurfaceSecondary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfaceSecondary),
            "Primary text on secondary surface must meet WCAG AA"
        )
    }

    @Test
    fun `light theme TextPrimary on SurfaceTertiary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfaceTertiary),
            "Primary text on tertiary surface must meet WCAG AA"
        )
    }

    @Test
    fun `light theme TextSecondary on SurfacePrimary meets minimum 3_0 contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.TextSecondary, YoleColors.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Secondary text on primary surface should have at least 3.0 contrast, got $ratio")
    }

    @Test
    fun `light theme TextPrimary on SurfacePrimary contrast is above 7`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.TextPrimary, YoleColors.SurfacePrimary
        )
        assertTrue(ratio >= 7.0,
            "Primary text (near-black) on white surface should exceed AAA (7.0), got $ratio")
    }

    @Test
    fun `light theme BrandPrimary on SurfacePrimary has adequate contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.BrandPrimary, YoleColors.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Brand primary on white should have at least 3.0 contrast for UI elements, got $ratio")
    }

    @Test
    fun `light theme Error color on SurfacePrimary is distinguishable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Error, YoleColors.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Error color must be distinguishable on primary surface, got $ratio")
    }

    // ==================== Dark Theme Contrast Ratios ====================

    @Test
    fun `dark theme TextPrimary on SurfacePrimary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfacePrimary),
            "Dark primary text on dark primary surface must meet WCAG AA"
        )
    }

    @Test
    fun `dark theme TextPrimary on SurfaceSecondary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfaceSecondary),
            "Dark primary text on secondary surface must meet WCAG AA"
        )
    }

    @Test
    fun `dark theme TextPrimary on SurfaceTertiary meets WCAG AA`() {
        assertTrue(
            ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfaceTertiary),
            "Dark primary text on tertiary surface must meet WCAG AA"
        )
    }

    @Test
    fun `dark theme TextSecondary on SurfacePrimary meets minimum 3_0 contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.TextSecondary, YoleColors.Dark.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Dark secondary text on dark surface should have at least 3.0 contrast, got $ratio")
    }

    @Test
    fun `dark theme BrandPrimary on SurfacePrimary has adequate contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.BrandPrimary, YoleColors.Dark.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Dark brand primary on dark surface should have at least 3.0 contrast, got $ratio")
    }

    @Test
    fun `dark theme Error color on SurfacePrimary is distinguishable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.Error, YoleColors.Dark.SurfacePrimary
        )
        assertTrue(ratio >= 3.0,
            "Dark error color must be distinguishable, got $ratio")
    }

    @Test
    fun `dark theme TextPrimary on SurfacePrimary contrast exceeds 7 for AAA`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfacePrimary
        )
        assertTrue(ratio >= 7.0,
            "White text on near-black surface should meet AAA (7.0), got $ratio")
    }

    // ==================== High Contrast Mode Tests ====================

    @Test
    fun `high contrast settings can be enabled`() {
        val settings = AccessibilitySettings(highContrast = true)
        assertTrue(settings.highContrast)
    }

    @Test
    fun `high contrast mode is false by default`() {
        assertFalse(AccessibilitySettings.DEFAULT.highContrast)
    }

    @Test
    fun `high contrast setting is independent of other settings`() {
        val settings = AccessibilitySettings(
            highContrast = true,
            reduceMotion = false,
            largeText = false
        )
        assertTrue(settings.highContrast)
        assertFalse(settings.reduceMotion)
        assertFalse(settings.largeText)
    }

    @Test
    fun `primary text on primary surface exceeds WCAG AAA in both themes`() {
        // Light theme
        assertTrue(
            ThemeUtils.meetsWcagAAA(YoleColors.TextPrimary, YoleColors.SurfacePrimary),
            "Light theme primary text should meet AAA"
        )
        // Dark theme
        assertTrue(
            ThemeUtils.meetsWcagAAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfacePrimary),
            "Dark theme primary text should meet AAA"
        )
    }

    @Test
    fun `contrast ratio of pure black and white is maximum`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.Black, Color.White)
        assertTrue(ratio >= 20.9, "Black/white contrast should be ~21")
    }

    // ==================== Font Size Scaling Tests ====================

    @Test
    fun `body text is at least 12sp for readability`() {
        assertTrue(YoleTypography.BodySmall.fontSize >= 12.sp,
            "Smallest body text should be at least 12sp")
    }

    @Test
    fun `body medium text is at least 14sp`() {
        assertTrue(YoleTypography.BodyMedium.fontSize >= 14.sp,
            "Body medium should be at least 14sp")
    }

    @Test
    fun `body large text is at least 16sp`() {
        assertTrue(YoleTypography.BodyLarge.fontSize >= 16.sp,
            "Body large should be at least 16sp")
    }

    @Test
    fun `label text is at least 11sp`() {
        assertTrue(YoleTypography.LabelSmall.fontSize >= 11.sp,
            "Label small should be at least 11sp")
    }

    @Test
    fun `heading sizes scale up from body`() {
        assertTrue(YoleTypography.HeadlineSmall.fontSize > YoleTypography.BodyLarge.fontSize,
            "Headings should be larger than body text")
    }

    @Test
    fun `display sizes are the largest`() {
        assertTrue(YoleTypography.DisplayLarge.fontSize > YoleTypography.HeadlineLarge.fontSize,
            "Display should be larger than headline")
    }

    @Test
    fun `line heights are proportional to font sizes`() {
        val styles = listOf(
            YoleTypography.BodySmall, YoleTypography.BodyMedium, YoleTypography.BodyLarge,
            YoleTypography.LabelSmall, YoleTypography.LabelMedium, YoleTypography.LabelLarge,
            YoleTypography.TitleSmall, YoleTypography.TitleMedium, YoleTypography.TitleLarge,
            YoleTypography.HeadlineSmall, YoleTypography.HeadlineMedium, YoleTypography.HeadlineLarge,
            YoleTypography.DisplaySmall, YoleTypography.DisplayMedium, YoleTypography.DisplayLarge,
            YoleTypography.Code
        )
        styles.forEach { style ->
            assertTrue(style.lineHeight >= style.fontSize,
                "Line height ${style.lineHeight} should be >= font size ${style.fontSize}")
        }
    }

    @Test
    fun `code text uses monospace font for readability`() {
        assertEquals(FontFamily.Monospace, YoleTypography.Code.fontFamily)
    }

    @Test
    fun `code text is at least 14sp`() {
        assertTrue(YoleTypography.Code.fontSize >= 14.sp,
            "Code text should be at least 14sp for readability")
    }

    @Test
    fun `largeText accessibility setting can be enabled`() {
        val settings = AccessibilitySettings(largeText = true)
        assertTrue(settings.largeText)
    }

    // ==================== Color Blindness Compatibility Tests ====================

    @Test
    fun `success and error colors are distinguishable by contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Success, YoleColors.Error)
        assertTrue(ratio > 1.0,
            "Success and error colors should be distinguishable, ratio: $ratio")
    }

    @Test
    fun `warning and error colors are distinguishable by contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Warning, YoleColors.Error)
        assertTrue(ratio > 1.0,
            "Warning and error colors should be distinguishable, ratio: $ratio")
    }

    @Test
    fun `success and warning colors are distinguishable by contrast`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Success, YoleColors.Warning)
        assertTrue(ratio > 1.0,
            "Success and warning colors should be distinguishable, ratio: $ratio")
    }

    @Test
    fun `info and error colors are distinguishable`() {
        val ratio = ThemeUtils.calculateContrastRatio(YoleColors.Info, YoleColors.Error)
        assertTrue(ratio > 1.0,
            "Info and error colors should be distinguishable, ratio: $ratio")
    }

    @Test
    fun `dark theme success and error are distinguishable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.Success, YoleColors.Dark.Error
        )
        assertTrue(ratio > 1.0)
    }

    @Test
    fun `dark theme warning and error are distinguishable`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.Warning, YoleColors.Dark.Error
        )
        assertTrue(ratio > 1.0)
    }

    @Test
    fun `all semantic colors differ from surfaces in both themes`() {
        // Light theme
        val lightSemanticColors = listOf(
            YoleColors.Success, YoleColors.Warning, YoleColors.Error, YoleColors.Info
        )
        lightSemanticColors.forEach { color ->
            assertNotEquals(color, YoleColors.SurfacePrimary)
            assertNotEquals(color, YoleColors.SurfaceSecondary)
        }

        // Dark theme
        val darkSemanticColors = listOf(
            YoleColors.Dark.Success, YoleColors.Dark.Warning,
            YoleColors.Dark.Error, YoleColors.Dark.Info
        )
        darkSemanticColors.forEach { color ->
            assertNotEquals(color, YoleColors.Dark.SurfacePrimary)
            assertNotEquals(color, YoleColors.Dark.SurfaceSecondary)
        }
    }

    // ==================== Interactive Element Contrast ====================

    @Test
    fun `light interactive hover is distinct from surface`() {
        assertNotEquals(YoleColors.InteractiveHover, YoleColors.SurfacePrimary)
    }

    @Test
    fun `light interactive pressed is distinct from hover`() {
        assertNotEquals(YoleColors.InteractivePressed, YoleColors.InteractiveHover)
    }

    @Test
    fun `light interactive focus is distinct from hover and pressed`() {
        assertNotEquals(YoleColors.InteractiveFocus, YoleColors.InteractiveHover)
        assertNotEquals(YoleColors.InteractiveFocus, YoleColors.InteractivePressed)
    }

    @Test
    fun `dark interactive states are distinct from each other`() {
        assertNotEquals(YoleColors.Dark.InteractiveHover, YoleColors.Dark.InteractivePressed)
        assertNotEquals(YoleColors.Dark.InteractivePressed, YoleColors.Dark.InteractiveFocus)
        assertNotEquals(YoleColors.Dark.InteractiveHover, YoleColors.Dark.InteractiveFocus)
    }

    // ==================== Border Visibility Tests ====================

    @Test
    fun `light borders have increasing contrast from light to strong`() {
        val lightRatio = ThemeUtils.calculateContrastRatio(
            YoleColors.BorderLight, YoleColors.SurfacePrimary
        )
        val mediumRatio = ThemeUtils.calculateContrastRatio(
            YoleColors.BorderMedium, YoleColors.SurfacePrimary
        )
        val strongRatio = ThemeUtils.calculateContrastRatio(
            YoleColors.BorderStrong, YoleColors.SurfacePrimary
        )
        assertTrue(strongRatio >= mediumRatio,
            "Strong border should have more contrast than medium ($strongRatio vs $mediumRatio)")
        assertTrue(mediumRatio >= lightRatio,
            "Medium border should have more contrast than light ($mediumRatio vs $lightRatio)")
    }

    @Test
    fun `dark borders are visible on dark surfaces`() {
        val ratio = ThemeUtils.calculateContrastRatio(
            YoleColors.Dark.BorderStrong, YoleColors.Dark.SurfacePrimary
        )
        assertTrue(ratio > 1.0,
            "Dark strong border should be visible on dark surface, ratio: $ratio")
    }

    // ==================== ThemeUtils Helper Tests ====================

    @Test
    fun `getSemanticColor returns light color in light mode`() {
        val result = ThemeUtils.getSemanticColor(
            isDarkTheme = false,
            lightColor = YoleColors.TextPrimary,
            darkColor = YoleColors.Dark.TextPrimary
        )
        assertEquals(YoleColors.TextPrimary, result)
    }

    @Test
    fun `getSemanticColor returns dark color in dark mode`() {
        val result = ThemeUtils.getSemanticColor(
            isDarkTheme = true,
            lightColor = YoleColors.TextPrimary,
            darkColor = YoleColors.Dark.TextPrimary
        )
        assertEquals(YoleColors.Dark.TextPrimary, result)
    }

    @Test
    fun `shouldUseDarkTheme LIGHT mode always returns false`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, false))
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, true))
    }

    @Test
    fun `shouldUseDarkTheme DARK mode always returns true`() {
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, true))
    }

    @Test
    fun `shouldUseDarkTheme SYSTEM mode follows system preference`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, false))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, true))
    }
}
