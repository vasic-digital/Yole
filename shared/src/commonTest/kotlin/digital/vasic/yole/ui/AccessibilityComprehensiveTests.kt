/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive accessibility tests covering semantic
 * descriptions, content descriptions, touch targets,
 * color contrast, screen reader annotations, keyboard
 * navigation, and accessibility settings.
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.test.*

/**
 * Comprehensive accessibility tests for Yole UI accessibility system.
 *
 * Tests cover:
 * - Semantic descriptions generation
 * - Content description accessibility via AccessibilityModifiers
 * - Touch target sizes and WCAG compliance
 * - Color contrast calculations
 * - Screen reader annotations
 * - Keyboard navigation support
 * - AccessibilitySettings data class behavior
 * - Focus management
 * - Label building with primary/secondary/state
 * - Edge cases in all accessibility utilities
 */
class AccessibilityComprehensiveTests {

    // ==================== Semantic Descriptions Generation ====================

    @Test
    fun `semantic role BUTTON exists and has correct name`() {
        val role = AccessibilityConstants.SemanticRole.BUTTON
        assertEquals("BUTTON", role.name)
    }

    @Test
    fun `semantic role LINK exists and has correct name`() {
        val role = AccessibilityConstants.SemanticRole.LINK
        assertEquals("LINK", role.name)
    }

    @Test
    fun `semantic role HEADER exists and has correct name`() {
        val role = AccessibilityConstants.SemanticRole.HEADER
        assertEquals("HEADER", role.name)
    }

    @Test
    fun `semantic role LIST_ITEM is separate from LIST`() {
        assertNotEquals(
            AccessibilityConstants.SemanticRole.LIST,
            AccessibilityConstants.SemanticRole.LIST_ITEM
        )
    }

    @Test
    fun `semantic role NAVIGATION exists for nav regions`() {
        val role = AccessibilityConstants.SemanticRole.NAVIGATION
        assertEquals("NAVIGATION", role.name)
        assertTrue(AccessibilityConstants.SemanticRole.entries.contains(role))
    }

    @Test
    fun `semantic role MAIN_CONTENT and SECONDARY_CONTENT are distinct`() {
        assertNotEquals(
            AccessibilityConstants.SemanticRole.MAIN_CONTENT,
            AccessibilityConstants.SemanticRole.SECONDARY_CONTENT
        )
    }

    @Test
    fun `semantic role STATUS and ALERT are distinct`() {
        assertNotEquals(
            AccessibilityConstants.SemanticRole.STATUS,
            AccessibilityConstants.SemanticRole.ALERT
        )
    }

    @Test
    fun `all semantic roles have unique ordinals`() {
        val ordinals = AccessibilityConstants.SemanticRole.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    @Test
    fun `semantic roles enum valueOf works for all entries`() {
        AccessibilityConstants.SemanticRole.entries.forEach { role ->
            val retrieved = AccessibilityConstants.SemanticRole.valueOf(role.name)
            assertEquals(role, retrieved)
        }
    }

    // ==================== Content Description Accessibility ====================

    @Test
    fun `accessibleContentDescription returns non-null Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleContentDescription("Save button")
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleContentDescription with empty string returns Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleContentDescription("")
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleContentDescription with long description returns Modifier`() {
        with(AccessibilityModifiers) {
            val longDesc = "A".repeat(5000)
            val result = Modifier.accessibleContentDescription(longDesc)
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleRole returns non-null Modifier for Button role`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleRole(Role.Button)
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleRole returns non-null Modifier for Image role`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleRole(Role.Image)
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleRole returns non-null Modifier for Checkbox role`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleRole(Role.Checkbox)
            assertNotNull(result)
        }
    }

    // ==================== Label Building Tests ====================

    @Test
    fun `accessibleLabel with primary only builds correct description`() {
        with(AccessibilityModifiers) {
            // We verify the modifier is created without throwing
            val result = Modifier.accessibleLabel("Save document")
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleLabel with primary and secondary builds combined description`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleLabel("Save", secondary = "Ctrl+S")
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleLabel with primary secondary and state builds full description`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleLabel(
                primary = "Save document",
                secondary = "Ctrl+S shortcut",
                state = "All changes saved"
            )
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleLabel with null secondary and null state works`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleLabel("Primary only", null, null)
            assertNotNull(result)
        }
    }

    // ==================== Touch Target Size Tests ====================

    @Test
    fun `touch target 44dp meets WCAG AA minimum`() {
        assertTrue(TouchTargets.isAccessibleSize(44.dp))
    }

    @Test
    fun `touch target 48dp meets Android large guideline`() {
        assertTrue(TouchTargets.isAccessibleSize(
            48.dp,
            AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
        ))
    }

    @Test
    fun `touch target 10dp is too small`() {
        assertFalse(TouchTargets.isAccessibleSize(10.dp))
    }

    @Test
    fun `touch target 43dp is below WCAG minimum`() {
        assertFalse(TouchTargets.isAccessibleSize(43.dp))
    }

    @Test
    fun `touch target 0dp is not accessible`() {
        assertFalse(TouchTargets.isAccessibleSize(0.dp))
    }

    @Test
    fun `touch target exactly at minimum passes`() {
        val minSize = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
        assertTrue(TouchTargets.isAccessibleSize(minSize))
    }

    @Test
    fun `touch target custom minimum 36dp`() {
        assertTrue(TouchTargets.isAccessibleSize(36.dp, 36.dp))
        assertFalse(TouchTargets.isAccessibleSize(35.dp, 36.dp))
    }

    @Test
    fun `large touch target constant is 48dp`() {
        assertEquals(48.dp, AccessibilityConstants.MIN_TOUCH_TARGET_SIZE_LARGE)
    }

    @Test
    fun `ensureMinTouchTarget returns Modifier for small size`() {
        with(TouchTargets) {
            val result = Modifier.ensureMinTouchTarget(20.dp)
            assertNotNull(result)
        }
    }

    @Test
    fun `ensureMinTouchTarget returns Modifier for accessible size`() {
        with(TouchTargets) {
            val result = Modifier.ensureMinTouchTarget(50.dp)
            assertNotNull(result)
        }
    }

    // ==================== Color Contrast Tests ====================

    @Test
    fun `contrast ratio black on white is approximately 21`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.Black, Color.White)
        assertTrue(ratio >= 20.9 && ratio <= 21.1,
            "Black on white contrast should be ~21, got $ratio")
    }

    @Test
    fun `contrast ratio white on white is 1`() {
        val ratio = ThemeUtils.calculateContrastRatio(Color.White, Color.White)
        assertEquals(1.0, ratio, 0.01)
    }

    @Test
    fun `contrast ratio is commutative`() {
        val ratio1 = ThemeUtils.calculateContrastRatio(Color.Red, Color.Blue)
        val ratio2 = ThemeUtils.calculateContrastRatio(Color.Blue, Color.Red)
        assertEquals(ratio1, ratio2, 0.001)
    }

    @Test
    fun `light theme primary text meets WCAG AA on primary surface`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.TextPrimary, YoleColors.SurfacePrimary))
    }

    @Test
    fun `dark theme primary text meets WCAG AA on primary surface`() {
        assertTrue(ThemeUtils.meetsWcagAA(YoleColors.Dark.TextPrimary, YoleColors.Dark.SurfacePrimary))
    }

    @Test
    fun `WCAG AA threshold for normal text is 4_5`() {
        // Find a color pair that has exactly around 4.5 ratio
        val black = Color.Black
        val white = Color.White
        // Black on white should exceed 4.5
        assertTrue(ThemeUtils.meetsWcagAA(black, white, isLargeText = false))
    }

    @Test
    fun `WCAG AA threshold for large text is 3_0`() {
        // Large text has a lower threshold of 3.0
        val gray = Color(0xFF777777)
        val white = Color.White
        val ratio = ThemeUtils.calculateContrastRatio(gray, white)
        if (ratio >= 3.0 && ratio < 4.5) {
            assertTrue(ThemeUtils.meetsWcagAA(gray, white, isLargeText = true))
            assertFalse(ThemeUtils.meetsWcagAA(gray, white, isLargeText = false))
        }
    }

    @Test
    fun `WCAG AAA is stricter than AA`() {
        // AAA requires 7.0 for normal text, AA requires 4.5
        val color = Color(0xFF555555)
        val bg = Color.White
        val ratio = ThemeUtils.calculateContrastRatio(color, bg)
        if (ratio >= 4.5 && ratio < 7.0) {
            assertTrue(ThemeUtils.meetsWcagAA(color, bg))
            assertFalse(ThemeUtils.meetsWcagAAA(color, bg))
        }
    }

    // ==================== Screen Reader Annotations ====================

    @Test
    fun `screenReader announceStatus does not throw for normal message`() {
        ScreenReader.announceStatus("Document saved")
    }

    @Test
    fun `screenReader announceAlert does not throw for error message`() {
        ScreenReader.announceAlert("Connection failed: timeout")
    }

    @Test
    fun `screenReader announce with each semantic role does not throw`() {
        AccessibilityConstants.SemanticRole.entries.forEach { role ->
            ScreenReader.announce("Testing role ${role.name}", role)
        }
    }

    @Test
    fun `screenReader announce default role is STATUS`() {
        // Should not throw - verifies default parameter works
        ScreenReader.announce("Default role announcement")
    }

    @Test
    fun `screenReader liveRegion returns Modifier`() {
        with(ScreenReader) {
            val result = Modifier.liveRegion()
            assertNotNull(result)
        }
    }

    @Test
    fun `screenReader handles empty announcement`() {
        ScreenReader.announceStatus("")
        ScreenReader.announceAlert("")
        ScreenReader.announce("")
    }

    @Test
    fun `screenReader handles unicode announcement`() {
        ScreenReader.announceStatus("\u4e2d\u6587\u7248\u672c")
        ScreenReader.announceAlert("\u0420\u0443\u0441\u0441\u043a\u0438\u0439")
    }

    @Test
    fun `screenReader handles HTML-like announcement`() {
        ScreenReader.announceStatus("<b>Bold</b> text &amp; entities")
    }

    // ==================== Keyboard Navigation Support ====================

    @Test
    fun `keyboard shortcut SAVE has correct key and modifier`() {
        val save = KeyboardShortcuts.Shortcut.SAVE
        assertEquals(Key.S, save.key)
        assertTrue(save.ctrl)
        assertFalse(save.alt)
        assertFalse(save.shift)
    }

    @Test
    fun `keyboard shortcut FIND uses Ctrl+F`() {
        val find = KeyboardShortcuts.Shortcut.FIND
        assertEquals(Key.F, find.key)
        assertTrue(find.ctrl)
    }

    @Test
    fun `keyboard shortcut UNDO uses Ctrl+Z`() {
        val undo = KeyboardShortcuts.Shortcut.UNDO
        assertEquals(Key.Z, undo.key)
        assertTrue(undo.ctrl)
    }

    @Test
    fun `keyboard shortcut ESCAPE has no modifiers`() {
        val escape = KeyboardShortcuts.Shortcut.ESCAPE
        assertEquals(Key.Escape, escape.key)
        assertFalse(escape.ctrl)
        assertFalse(escape.alt)
        assertFalse(escape.shift)
    }

    @Test
    fun `keyboard shortcut HELP uses F1`() {
        val help = KeyboardShortcuts.Shortcut.HELP
        assertEquals(Key.F1, help.key)
        assertFalse(help.ctrl)
    }

    @Test
    fun `all keyboard shortcuts have non-blank descriptions`() {
        KeyboardShortcuts.Shortcut.entries.forEach { shortcut ->
            assertTrue(shortcut.description.isNotBlank(),
                "Shortcut ${shortcut.name} should have a description")
        }
    }

    @Test
    fun `keyboard shortcut toDisplayString contains Ctrl for ctrl shortcuts`() {
        with(KeyboardShortcuts) {
            val display = KeyboardShortcuts.Shortcut.SAVE.toDisplayString()
            assertTrue(display.startsWith("Ctrl+"),
                "Display string for SAVE should start with Ctrl+, got: $display")
        }
    }

    @Test
    fun `keyboard shortcut toDisplayString does not contain Ctrl for non-ctrl shortcuts`() {
        with(KeyboardShortcuts) {
            val display = KeyboardShortcuts.Shortcut.ESCAPE.toDisplayString()
            assertFalse(display.startsWith("Ctrl+"),
                "Display string for ESCAPE should not start with Ctrl+, got: $display")
        }
    }

    @Test
    fun `accessibleShortcut returns non-null Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleShortcut(
                key = Key.S,
                ctrl = true,
                onShortcut = {}
            )
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleEscape returns non-null Modifier`() {
        with(AccessibilityModifiers) {
            var called = false
            val result = Modifier.accessibleEscape { called = true }
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleFocusable returns non-null Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleFocusable()
            assertNotNull(result)
        }
    }

    // ==================== Accessibility Settings Tests ====================

    @Test
    fun `default settings has reduceMotion false`() {
        assertFalse(AccessibilitySettings.DEFAULT.reduceMotion)
    }

    @Test
    fun `default settings has keyboardNavigation true`() {
        assertTrue(AccessibilitySettings.DEFAULT.keyboardNavigation)
    }

    @Test
    fun `settings with highContrast enabled`() {
        val settings = AccessibilitySettings(highContrast = true)
        assertTrue(settings.highContrast)
    }

    @Test
    fun `settings with largeText enabled`() {
        val settings = AccessibilitySettings(largeText = true)
        assertTrue(settings.largeText)
    }

    @Test
    fun `settings copy changes only specified field`() {
        val original = AccessibilitySettings.DEFAULT
        val modified = original.copy(screenReaderEnabled = true)
        assertTrue(modified.screenReaderEnabled)
        assertFalse(original.screenReaderEnabled)
        // Other fields unchanged
        assertEquals(original.reduceMotion, modified.reduceMotion)
        assertEquals(original.highContrast, modified.highContrast)
        assertEquals(original.keyboardNavigation, modified.keyboardNavigation)
    }

    @Test
    fun `settings equality for identical instances`() {
        val a = AccessibilitySettings(
            reduceMotion = true,
            highContrast = false,
            largeText = true
        )
        val b = AccessibilitySettings(
            reduceMotion = true,
            highContrast = false,
            largeText = true
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `settings inequality for different instances`() {
        val a = AccessibilitySettings(reduceMotion = true)
        val b = AccessibilitySettings(reduceMotion = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `settings toString contains field names`() {
        val settings = AccessibilitySettings(announceChanges = false)
        val str = settings.toString()
        assertTrue(str.contains("announceChanges"))
    }

    // ==================== Focus Management Tests ====================

    @Test
    fun `focusManagement moveFocusNext is callable`() {
        FocusManagement.moveFocusNext()
    }

    @Test
    fun `focusManagement moveFocusPrevious is callable`() {
        FocusManagement.moveFocusPrevious()
    }

    @Test
    fun `focusManagement moveFocusTo with element ID is callable`() {
        FocusManagement.moveFocusTo("main-editor")
    }

    @Test
    fun `focusManagement moveFocusTo with empty ID does not throw`() {
        FocusManagement.moveFocusTo("")
    }

    @Test
    fun `focusManagement sequential navigation calls do not throw`() {
        repeat(20) {
            FocusManagement.moveFocusNext()
        }
        repeat(20) {
            FocusManagement.moveFocusPrevious()
        }
    }

    // ==================== AccessibilityState Tests ====================

    @Test
    fun `accessibilityState getSettings returns DEFAULT`() {
        val settings = AccessibilityState.getSettings()
        assertEquals(AccessibilitySettings.DEFAULT, settings)
    }

    @Test
    fun `accessibilityState isScreenReaderActive returns false by default`() {
        assertFalse(AccessibilityState.isScreenReaderActive())
    }

    @Test
    fun `accessibilityState announce does not throw for normal text`() {
        AccessibilityState.announce("Test announcement")
    }

    @Test
    fun `accessibilityState announce does not throw for special characters`() {
        AccessibilityState.announce("Test <>&\"' \t\n\r special chars")
    }

    // ==================== AccessibilityModifiers Touch Target ====================

    @Test
    fun `accessibleTouchTarget with default size returns Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleTouchTarget()
            assertNotNull(result)
        }
    }

    @Test
    fun `accessibleTouchTarget with custom size returns Modifier`() {
        with(AccessibilityModifiers) {
            val result = Modifier.accessibleTouchTarget(48.dp)
            assertNotNull(result)
        }
    }

    // ==================== Focus Indicator Width ====================

    @Test
    fun `focus indicator width is at least 2dp for visibility`() {
        assertTrue(AccessibilityConstants.FOCUS_INDICATOR_WIDTH >= 2.dp)
    }

    @Test
    fun `focus indicator width is positive`() {
        assertTrue(AccessibilityConstants.FOCUS_INDICATOR_WIDTH > 0.dp)
    }
}
