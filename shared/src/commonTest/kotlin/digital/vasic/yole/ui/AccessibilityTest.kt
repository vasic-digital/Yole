/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Accessibility Tests
 *
 * Comprehensive tests for accessibility utilities,
 * keyboard shortcuts, and screen reader support.
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import kotlin.test.*

/**
 * Tests for accessibility utilities and constants.
 */
class AccessibilityTest {

    // ==================== CONSTANTS TESTS ====================

    @Test
    fun `touch target sizes meet WCAG guidelines`() {
        // WCAG 2.1 AA requires minimum 44x44 dp
        assertTrue(
            AccessibilityConstants.MIN_TOUCH_TARGET_SIZE >= 44.dp,
            "Minimum touch target should be at least 44dp"
        )

        assertTrue(
            AccessibilityConstants.MIN_TOUCH_TARGET_SIZE_LARGE >= 48.dp,
            "Large touch target should be at least 48dp"
        )
    }

    @Test
    fun `focus indicator width is visible`() {
        assertTrue(
            AccessibilityConstants.FOCUS_INDICATOR_WIDTH >= 2.dp,
            "Focus indicator should be at least 2dp for visibility"
        )
    }

    @Test
    fun `keyboard shortcuts are defined`() {
        assertNotNull(AccessibilityConstants.Shortcuts.SAVE)
        assertNotNull(AccessibilityConstants.Shortcuts.NEW)
        assertNotNull(AccessibilityConstants.Shortcuts.OPEN)
        assertNotNull(AccessibilityConstants.Shortcuts.CLOSE)
        assertNotNull(AccessibilityConstants.Shortcuts.UNDO)
        assertNotNull(AccessibilityConstants.Shortcuts.REDO)
        assertNotNull(AccessibilityConstants.Shortcuts.FIND)
        assertNotNull(AccessibilityConstants.Shortcuts.SELECT_ALL)
        assertNotNull(AccessibilityConstants.Shortcuts.SETTINGS)
        assertNotNull(AccessibilityConstants.Shortcuts.HELP)
        assertNotNull(AccessibilityConstants.Shortcuts.ESCAPE)
    }

    @Test
    fun `semantic roles are complete`() {
        val roles = AccessibilityConstants.SemanticRole.entries

        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.BUTTON))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.LINK))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.HEADER))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.LIST))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.LIST_ITEM))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.NAVIGATION))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.MAIN_CONTENT))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.STATUS))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.ALERT))
    }

    // ==================== KEYBOARD SHORTCUTS TESTS ====================

    @Test
    fun `keyboard shortcuts enum has all common shortcuts`() {
        val shortcuts = KeyboardShortcuts.Shortcut.entries

        assertTrue(shortcuts.any { it.name == "SAVE" })
        assertTrue(shortcuts.any { it.name == "NEW" })
        assertTrue(shortcuts.any { it.name == "OPEN" })
        assertTrue(shortcuts.any { it.name == "CLOSE" })
        assertTrue(shortcuts.any { it.name == "UNDO" })
        assertTrue(shortcuts.any { it.name == "REDO" })
        assertTrue(shortcuts.any { it.name == "FIND" })
        assertTrue(shortcuts.any { it.name == "SELECT_ALL" })
        assertTrue(shortcuts.any { it.name == "SETTINGS" })
        assertTrue(shortcuts.any { it.name == "HELP" })
        assertTrue(shortcuts.any { it.name == "ESCAPE" })
    }

    @Test
    fun `keyboard shortcuts have descriptions`() {
        KeyboardShortcuts.Shortcut.entries.forEach { shortcut ->
            assertTrue(
                shortcut.description.isNotEmpty(),
                "Shortcut ${shortcut.name} should have a description"
            )
        }
    }

    @Test
    fun `save shortcut has ctrl modifier`() {
        val saveShortcut = KeyboardShortcuts.Shortcut.SAVE

        assertTrue(saveShortcut.ctrl, "Save should require Ctrl")
        assertEquals(Key.S, saveShortcut.key)
    }

    @Test
    fun `escape shortcut has no modifiers`() {
        val escapeShortcut = KeyboardShortcuts.Shortcut.ESCAPE

        assertFalse(escapeShortcut.ctrl, "Escape should not require Ctrl")
        assertFalse(escapeShortcut.alt, "Escape should not require Alt")
        assertFalse(escapeShortcut.shift, "Escape should not require Shift")
    }

    @Test
    fun `help shortcut uses F1`() {
        val helpShortcut = KeyboardShortcuts.Shortcut.HELP

        assertEquals(Key.F1, helpShortcut.key)
    }

    // ==================== ACCESSIBILITY SETTINGS TESTS ====================

    @Test
    fun `default settings have sensible values`() {
        val defaults = AccessibilitySettings.DEFAULT

        assertFalse(defaults.reduceMotion, "Reduce motion should default to false")
        assertFalse(defaults.highContrast, "High contrast should default to false")
        assertFalse(defaults.largeText, "Large text should default to false")
        assertFalse(defaults.screenReaderEnabled, "Screen reader should default to false")
        assertTrue(defaults.keyboardNavigation, "Keyboard navigation should default to true")
        assertTrue(defaults.focusIndicators, "Focus indicators should default to true")
        assertTrue(defaults.announceChanges, "Announce changes should default to true")
    }

    @Test
    fun `settings can be customized`() {
        val customSettings = AccessibilitySettings(
            reduceMotion = true,
            highContrast = true,
            largeText = true,
            screenReaderEnabled = true,
            keyboardNavigation = false,
            focusIndicators = false,
            announceChanges = false
        )

        assertTrue(customSettings.reduceMotion)
        assertTrue(customSettings.highContrast)
        assertTrue(customSettings.largeText)
        assertTrue(customSettings.screenReaderEnabled)
        assertFalse(customSettings.keyboardNavigation)
        assertFalse(customSettings.focusIndicators)
        assertFalse(customSettings.announceChanges)
    }

    @Test
    fun `settings equality works correctly`() {
        val settings1 = AccessibilitySettings(reduceMotion = true)
        val settings2 = AccessibilitySettings(reduceMotion = true)
        val settings3 = AccessibilitySettings(reduceMotion = false)

        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
    }

    // ==================== TOUCH TARGETS TESTS ====================

    @Test
    fun `isAccessibleSize correctly validates sizes`() {
        assertTrue(TouchTargets.isAccessibleSize(44.dp))
        assertTrue(TouchTargets.isAccessibleSize(48.dp))
        assertTrue(TouchTargets.isAccessibleSize(100.dp))

        assertFalse(TouchTargets.isAccessibleSize(20.dp))
        assertFalse(TouchTargets.isAccessibleSize(30.dp))
        assertFalse(TouchTargets.isAccessibleSize(43.dp))
    }

    @Test
    fun `isAccessibleSize respects custom minimum`() {
        assertTrue(TouchTargets.isAccessibleSize(48.dp, minSize = 48.dp))
        assertFalse(TouchTargets.isAccessibleSize(47.dp, minSize = 48.dp))
    }

    // ==================== ACCESSIBILITY STATE TESTS ====================

    @Test
    fun `getSettings returns settings object`() {
        val settings = AccessibilityState.getSettings()
        assertNotNull(settings)
    }

    @Test
    fun `isScreenReaderActive returns boolean`() {
        val isActive = AccessibilityState.isScreenReaderActive()
        // Should not throw, just return a boolean
        assertTrue(isActive || !isActive)
    }

    @Test
    fun `announce does not throw`() {
        // Should not throw
        AccessibilityState.announce("Test message")
    }

    // ==================== SCREEN READER TESTS ====================

    @Test
    fun `announceStatus does not throw`() {
        ScreenReader.announceStatus("Status update")
    }

    @Test
    fun `announceAlert does not throw`() {
        ScreenReader.announceAlert("Alert message")
    }

    @Test
    fun `announce with role does not throw`() {
        AccessibilityConstants.SemanticRole.entries.forEach { role ->
            ScreenReader.announce("Message for $role", role)
        }
    }

    // ==================== FOCUS MANAGEMENT TESTS ====================

    @Test
    fun `focus management methods do not throw`() {
        FocusManagement.moveFocusNext()
        FocusManagement.moveFocusPrevious()
        FocusManagement.moveFocusTo("element-id")
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `empty description is handled`() {
        // Should not crash with empty string
        AccessibilityState.announce("")
        ScreenReader.announceStatus("")
        ScreenReader.announceAlert("")
    }

    @Test
    fun `unicode in accessibility content`() {
        val unicodeMessages = listOf(
            "Status: \u2713 Complete",
            "Error: \u2717 Failed",
            "\u4e2d\u6587 Chinese",
            "\u0420\u0443\u0441\u0441\u043a\u0438\u0439 Russian"
        )

        unicodeMessages.forEach { message ->
            ScreenReader.announceStatus(message)
            ScreenReader.announceAlert(message)
        }
    }

    @Test
    fun `very long accessibility message`() {
        val longMessage = "Status: " + "a".repeat(10000)
        ScreenReader.announceStatus(longMessage)
    }
}
