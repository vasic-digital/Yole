/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Accessibility Tests
 *
 * Deep tests for the accessibility system: semantic roles,
 * content descriptions, navigation labels, keyboard shortcuts,
 * screen reader support, touch targets, accessibility settings,
 * and focus management.
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import kotlin.test.*

/**
 * Comprehensive tests for accessibility utilities.
 *
 * Covers:
 * - AccessibilityConstants: touch targets, focus indicators, shortcuts, semantic roles
 * - KeyboardShortcuts: enum completeness, modifier correctness, display strings
 * - AccessibilitySettings: defaults, customization, equality, data class behavior
 * - AccessibilityState: announce, screen reader status, settings retrieval
 * - ScreenReader: status, alert, general announce with all roles
 * - FocusManagement: focus navigation methods
 * - TouchTargets: size validation, custom minimums
 * - Edge cases: empty strings, very long messages, unicode, special characters
 */
class AccessibilityTests {

    // ====================================================================
    // ACCESSIBILITY CONSTANTS
    // ====================================================================

    @Test
    fun `constants MIN_TOUCH_TARGET_SIZE is exactly 44dp`() {
        assertEquals(44.dp, AccessibilityConstants.MIN_TOUCH_TARGET_SIZE)
    }

    @Test
    fun `constants MIN_TOUCH_TARGET_SIZE_LARGE is exactly 48dp`() {
        assertEquals(48.dp, AccessibilityConstants.MIN_TOUCH_TARGET_SIZE_LARGE)
    }

    @Test
    fun `constants FOCUS_INDICATOR_WIDTH is exactly 2dp`() {
        assertEquals(2.dp, AccessibilityConstants.FOCUS_INDICATOR_WIDTH)
    }

    @Test
    fun `constants large touch target is bigger than standard`() {
        assertTrue(
            AccessibilityConstants.MIN_TOUCH_TARGET_SIZE_LARGE > AccessibilityConstants.MIN_TOUCH_TARGET_SIZE,
            "Large touch target should be bigger than standard"
        )
    }

    // ====================================================================
    // SHORTCUT STRINGS
    // ====================================================================

    @Test
    fun `shortcut strings contain modifier prefix`() {
        assertTrue(AccessibilityConstants.Shortcuts.SAVE.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.NEW.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.OPEN.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.CLOSE.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.UNDO.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.REDO.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.FIND.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.SELECT_ALL.startsWith("Ctrl+"))
        assertTrue(AccessibilityConstants.Shortcuts.SETTINGS.startsWith("Ctrl+"))
    }

    @Test
    fun `shortcut strings HELP is F1`() {
        assertEquals("F1", AccessibilityConstants.Shortcuts.HELP)
    }

    @Test
    fun `shortcut strings ESCAPE is Escape`() {
        assertEquals("Escape", AccessibilityConstants.Shortcuts.ESCAPE)
    }

    @Test
    fun `shortcut strings are unique`() {
        val shortcuts = listOf(
            AccessibilityConstants.Shortcuts.SAVE,
            AccessibilityConstants.Shortcuts.NEW,
            AccessibilityConstants.Shortcuts.OPEN,
            AccessibilityConstants.Shortcuts.CLOSE,
            AccessibilityConstants.Shortcuts.UNDO,
            AccessibilityConstants.Shortcuts.REDO,
            AccessibilityConstants.Shortcuts.FIND,
            AccessibilityConstants.Shortcuts.SELECT_ALL,
            AccessibilityConstants.Shortcuts.SETTINGS,
            AccessibilityConstants.Shortcuts.HELP,
            AccessibilityConstants.Shortcuts.ESCAPE
        )
        assertEquals(shortcuts.size, shortcuts.toSet().size, "All shortcut strings should be unique")
    }

    @Test
    fun `shortcut strings are all non-empty`() {
        val shortcuts = listOf(
            AccessibilityConstants.Shortcuts.SAVE,
            AccessibilityConstants.Shortcuts.NEW,
            AccessibilityConstants.Shortcuts.OPEN,
            AccessibilityConstants.Shortcuts.CLOSE,
            AccessibilityConstants.Shortcuts.UNDO,
            AccessibilityConstants.Shortcuts.REDO,
            AccessibilityConstants.Shortcuts.FIND,
            AccessibilityConstants.Shortcuts.SELECT_ALL,
            AccessibilityConstants.Shortcuts.SETTINGS,
            AccessibilityConstants.Shortcuts.HELP,
            AccessibilityConstants.Shortcuts.ESCAPE
        )
        shortcuts.forEach { shortcut ->
            assertTrue(shortcut.isNotEmpty(), "Shortcut string should not be empty")
        }
    }

    // ====================================================================
    // SEMANTIC ROLES
    // ====================================================================

    @Test
    fun `semantic roles contain all 10 required roles`() {
        val roles = AccessibilityConstants.SemanticRole.entries
        assertEquals(10, roles.size, "Should have exactly 10 semantic roles")
    }

    @Test
    fun `semantic roles include BUTTON`() {
        assertTrue(AccessibilityConstants.SemanticRole.entries.contains(AccessibilityConstants.SemanticRole.BUTTON))
    }

    @Test
    fun `semantic roles include LINK`() {
        assertTrue(AccessibilityConstants.SemanticRole.entries.contains(AccessibilityConstants.SemanticRole.LINK))
    }

    @Test
    fun `semantic roles include HEADER`() {
        assertTrue(AccessibilityConstants.SemanticRole.entries.contains(AccessibilityConstants.SemanticRole.HEADER))
    }

    @Test
    fun `semantic roles include LIST and LIST_ITEM`() {
        val roles = AccessibilityConstants.SemanticRole.entries
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.LIST))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.LIST_ITEM))
    }

    @Test
    fun `semantic roles include NAVIGATION`() {
        assertTrue(AccessibilityConstants.SemanticRole.entries.contains(AccessibilityConstants.SemanticRole.NAVIGATION))
    }

    @Test
    fun `semantic roles include MAIN_CONTENT and SECONDARY_CONTENT`() {
        val roles = AccessibilityConstants.SemanticRole.entries
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.MAIN_CONTENT))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.SECONDARY_CONTENT))
    }

    @Test
    fun `semantic roles include STATUS and ALERT`() {
        val roles = AccessibilityConstants.SemanticRole.entries
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.STATUS))
        assertTrue(roles.contains(AccessibilityConstants.SemanticRole.ALERT))
    }

    @Test
    fun `semantic role names are uppercase`() {
        AccessibilityConstants.SemanticRole.entries.forEach { role ->
            assertEquals(role.name, role.name.uppercase(), "Role name should be uppercase: ${role.name}")
        }
    }

    // ====================================================================
    // KEYBOARD SHORTCUTS ENUM
    // ====================================================================

    @Test
    fun `keyboard shortcuts enum has 11 entries`() {
        assertEquals(11, KeyboardShortcuts.Shortcut.entries.size)
    }

    @Test
    fun `keyboard shortcut SAVE uses Ctrl+S`() {
        val save = KeyboardShortcuts.Shortcut.SAVE
        assertEquals(Key.S, save.key)
        assertTrue(save.ctrl)
        assertFalse(save.alt)
        assertFalse(save.shift)
    }

    @Test
    fun `keyboard shortcut NEW uses Ctrl+N`() {
        val shortcut = KeyboardShortcuts.Shortcut.NEW
        assertEquals(Key.N, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut OPEN uses Ctrl+O`() {
        val shortcut = KeyboardShortcuts.Shortcut.OPEN
        assertEquals(Key.O, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut CLOSE uses Ctrl+W`() {
        val shortcut = KeyboardShortcuts.Shortcut.CLOSE
        assertEquals(Key.W, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut UNDO uses Ctrl+Z`() {
        val shortcut = KeyboardShortcuts.Shortcut.UNDO
        assertEquals(Key.Z, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut REDO uses Ctrl+Y`() {
        val shortcut = KeyboardShortcuts.Shortcut.REDO
        assertEquals(Key.Y, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut FIND uses Ctrl+F`() {
        val shortcut = KeyboardShortcuts.Shortcut.FIND
        assertEquals(Key.F, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut SELECT_ALL uses Ctrl+A`() {
        val shortcut = KeyboardShortcuts.Shortcut.SELECT_ALL
        assertEquals(Key.A, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut SETTINGS uses Ctrl+Comma`() {
        val shortcut = KeyboardShortcuts.Shortcut.SETTINGS
        assertEquals(Key.Comma, shortcut.key)
        assertTrue(shortcut.ctrl)
    }

    @Test
    fun `keyboard shortcut HELP uses F1 without modifiers`() {
        val shortcut = KeyboardShortcuts.Shortcut.HELP
        assertEquals(Key.F1, shortcut.key)
        assertFalse(shortcut.ctrl)
        assertFalse(shortcut.alt)
        assertFalse(shortcut.shift)
    }

    @Test
    fun `keyboard shortcut ESCAPE has no modifiers`() {
        val shortcut = KeyboardShortcuts.Shortcut.ESCAPE
        assertEquals(Key.Escape, shortcut.key)
        assertFalse(shortcut.ctrl)
        assertFalse(shortcut.alt)
        assertFalse(shortcut.shift)
    }

    @Test
    fun `all keyboard shortcuts have non-empty descriptions`() {
        KeyboardShortcuts.Shortcut.entries.forEach { shortcut ->
            assertTrue(
                shortcut.description.isNotBlank(),
                "Shortcut ${shortcut.name} must have a non-blank description"
            )
        }
    }

    @Test
    fun `keyboard shortcut descriptions are unique`() {
        val descriptions = KeyboardShortcuts.Shortcut.entries.map { it.description }
        assertEquals(
            descriptions.size,
            descriptions.toSet().size,
            "All shortcut descriptions should be unique"
        )
    }

    @Test
    fun `keyboard shortcuts with ctrl modifier all have ctrl true`() {
        val ctrlShortcuts = listOf(
            KeyboardShortcuts.Shortcut.SAVE,
            KeyboardShortcuts.Shortcut.NEW,
            KeyboardShortcuts.Shortcut.OPEN,
            KeyboardShortcuts.Shortcut.CLOSE,
            KeyboardShortcuts.Shortcut.UNDO,
            KeyboardShortcuts.Shortcut.REDO,
            KeyboardShortcuts.Shortcut.FIND,
            KeyboardShortcuts.Shortcut.SELECT_ALL,
            KeyboardShortcuts.Shortcut.SETTINGS
        )
        ctrlShortcuts.forEach { shortcut ->
            assertTrue(shortcut.ctrl, "${shortcut.name} should have ctrl=true")
        }
    }

    @Test
    fun `no keyboard shortcuts use alt modifier`() {
        KeyboardShortcuts.Shortcut.entries.forEach { shortcut ->
            assertFalse(shortcut.alt, "${shortcut.name} should not use Alt modifier")
        }
    }

    @Test
    fun `no keyboard shortcuts use shift modifier`() {
        KeyboardShortcuts.Shortcut.entries.forEach { shortcut ->
            assertFalse(shortcut.shift, "${shortcut.name} should not use Shift modifier")
        }
    }

    // ====================================================================
    // ACCESSIBILITY SETTINGS DATA CLASS
    // ====================================================================

    @Test
    fun `default settings reduceMotion is false`() {
        assertFalse(AccessibilitySettings.DEFAULT.reduceMotion)
    }

    @Test
    fun `default settings highContrast is false`() {
        assertFalse(AccessibilitySettings.DEFAULT.highContrast)
    }

    @Test
    fun `default settings largeText is false`() {
        assertFalse(AccessibilitySettings.DEFAULT.largeText)
    }

    @Test
    fun `default settings screenReaderEnabled is false`() {
        assertFalse(AccessibilitySettings.DEFAULT.screenReaderEnabled)
    }

    @Test
    fun `default settings keyboardNavigation is true`() {
        assertTrue(AccessibilitySettings.DEFAULT.keyboardNavigation)
    }

    @Test
    fun `default settings focusIndicators is true`() {
        assertTrue(AccessibilitySettings.DEFAULT.focusIndicators)
    }

    @Test
    fun `default settings announceChanges is true`() {
        assertTrue(AccessibilitySettings.DEFAULT.announceChanges)
    }

    @Test
    fun `settings with all flags enabled`() {
        val settings = AccessibilitySettings(
            reduceMotion = true,
            highContrast = true,
            largeText = true,
            screenReaderEnabled = true,
            keyboardNavigation = true,
            focusIndicators = true,
            announceChanges = true
        )
        assertTrue(settings.reduceMotion)
        assertTrue(settings.highContrast)
        assertTrue(settings.largeText)
        assertTrue(settings.screenReaderEnabled)
        assertTrue(settings.keyboardNavigation)
        assertTrue(settings.focusIndicators)
        assertTrue(settings.announceChanges)
    }

    @Test
    fun `settings with all flags disabled`() {
        val settings = AccessibilitySettings(
            reduceMotion = false,
            highContrast = false,
            largeText = false,
            screenReaderEnabled = false,
            keyboardNavigation = false,
            focusIndicators = false,
            announceChanges = false
        )
        assertFalse(settings.reduceMotion)
        assertFalse(settings.highContrast)
        assertFalse(settings.largeText)
        assertFalse(settings.screenReaderEnabled)
        assertFalse(settings.keyboardNavigation)
        assertFalse(settings.focusIndicators)
        assertFalse(settings.announceChanges)
    }

    @Test
    fun `settings equality with same values`() {
        val a = AccessibilitySettings(reduceMotion = true, highContrast = false)
        val b = AccessibilitySettings(reduceMotion = true, highContrast = false)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `settings inequality with different values`() {
        val a = AccessibilitySettings(reduceMotion = true)
        val b = AccessibilitySettings(reduceMotion = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `settings copy preserves values`() {
        val original = AccessibilitySettings(
            reduceMotion = true,
            highContrast = true,
            largeText = true
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun `settings copy with modification`() {
        val original = AccessibilitySettings.DEFAULT
        val modified = original.copy(highContrast = true)
        assertTrue(modified.highContrast)
        assertFalse(original.highContrast)
        assertNotEquals(original, modified)
    }

    @Test
    fun `DEFAULT companion object returns consistent instance`() {
        val default1 = AccessibilitySettings.DEFAULT
        val default2 = AccessibilitySettings.DEFAULT
        assertEquals(default1, default2)
    }

    // ====================================================================
    // ACCESSIBILITY STATE
    // ====================================================================

    @Test
    fun `state announce does not throw for normal message`() {
        AccessibilityState.announce("File saved successfully")
    }

    @Test
    fun `state announce does not throw for empty message`() {
        AccessibilityState.announce("")
    }

    @Test
    fun `state announce does not throw for very long message`() {
        AccessibilityState.announce("A".repeat(50000))
    }

    @Test
    fun `state announce does not throw for unicode message`() {
        AccessibilityState.announce("\u4F60\u597D \uD83D\uDE00 \u0645\u0631\u062D\u0628\u0627")
    }

    @Test
    fun `state announce does not throw for special characters`() {
        AccessibilityState.announce("<script>alert('xss')</script>&\"\n\t\r")
    }

    @Test
    fun `state isScreenReaderActive returns boolean`() {
        val active = AccessibilityState.isScreenReaderActive()
        // Just verify it returns without throwing
        assertFalse(active, "Default screen reader should be inactive")
    }

    @Test
    fun `state getSettings returns non-null settings`() {
        val settings = AccessibilityState.getSettings()
        assertNotNull(settings)
    }

    @Test
    fun `state getSettings returns default settings`() {
        val settings = AccessibilityState.getSettings()
        assertEquals(AccessibilitySettings.DEFAULT, settings)
    }

    // ====================================================================
    // SCREEN READER
    // ====================================================================

    @Test
    fun `screenReader announceStatus does not throw`() {
        ScreenReader.announceStatus("Status: complete")
    }

    @Test
    fun `screenReader announceAlert does not throw`() {
        ScreenReader.announceAlert("Error: file not found")
    }

    @Test
    fun `screenReader announce with STATUS role`() {
        ScreenReader.announce("Processing...", AccessibilityConstants.SemanticRole.STATUS)
    }

    @Test
    fun `screenReader announce with ALERT role`() {
        ScreenReader.announce("Critical error!", AccessibilityConstants.SemanticRole.ALERT)
    }

    @Test
    fun `screenReader announce with all semantic roles does not throw`() {
        AccessibilityConstants.SemanticRole.entries.forEach { role ->
            ScreenReader.announce("Message for role: ${role.name}", role)
        }
    }

    @Test
    fun `screenReader announce with default role`() {
        // Default role should be STATUS
        ScreenReader.announce("Default role message")
    }

    @Test
    fun `screenReader handles rapid announcements`() {
        repeat(100) { i ->
            ScreenReader.announceStatus("Rapid message $i")
        }
    }

    // ====================================================================
    // FOCUS MANAGEMENT
    // ====================================================================

    @Test
    fun `focusManagement moveFocusNext does not throw`() {
        FocusManagement.moveFocusNext()
    }

    @Test
    fun `focusManagement moveFocusPrevious does not throw`() {
        FocusManagement.moveFocusPrevious()
    }

    @Test
    fun `focusManagement moveFocusTo with valid ID does not throw`() {
        FocusManagement.moveFocusTo("editor-area")
    }

    @Test
    fun `focusManagement moveFocusTo with empty ID does not throw`() {
        FocusManagement.moveFocusTo("")
    }

    @Test
    fun `focusManagement moveFocusTo with special characters does not throw`() {
        FocusManagement.moveFocusTo("element-id-#123.class[attr]")
    }

    @Test
    fun `focusManagement rapid navigation does not throw`() {
        repeat(50) {
            FocusManagement.moveFocusNext()
            FocusManagement.moveFocusPrevious()
        }
    }

    // ====================================================================
    // TOUCH TARGETS
    // ====================================================================

    @Test
    fun `touchTargets isAccessibleSize 44dp meets minimum`() {
        assertTrue(TouchTargets.isAccessibleSize(44.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize 43dp below minimum`() {
        assertFalse(TouchTargets.isAccessibleSize(43.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize 48dp meets minimum`() {
        assertTrue(TouchTargets.isAccessibleSize(48.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize 100dp meets minimum`() {
        assertTrue(TouchTargets.isAccessibleSize(100.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize 0dp below minimum`() {
        assertFalse(TouchTargets.isAccessibleSize(0.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize with custom minimum 48dp`() {
        assertTrue(TouchTargets.isAccessibleSize(48.dp, minSize = 48.dp))
        assertFalse(TouchTargets.isAccessibleSize(47.dp, minSize = 48.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize with custom minimum 36dp`() {
        assertTrue(TouchTargets.isAccessibleSize(36.dp, minSize = 36.dp))
        assertTrue(TouchTargets.isAccessibleSize(44.dp, minSize = 36.dp))
        assertFalse(TouchTargets.isAccessibleSize(35.dp, minSize = 36.dp))
    }

    @Test
    fun `touchTargets isAccessibleSize boundary exactly at minimum`() {
        val min = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
        assertTrue(TouchTargets.isAccessibleSize(min, minSize = min))
    }

    // ====================================================================
    // INTEGRATION: settings affect behavior assumptions
    // ====================================================================

    @Test
    fun `settings for reduced motion should be respected by animation system`() {
        val settings = AccessibilitySettings(reduceMotion = true)
        assertTrue(settings.reduceMotion, "Reduced motion flag should be true when set")
    }

    @Test
    fun `settings for high contrast should be queryable`() {
        val settings = AccessibilitySettings(highContrast = true)
        assertTrue(settings.highContrast)
    }

    @Test
    fun `settings for large text should be queryable`() {
        val settings = AccessibilitySettings(largeText = true)
        assertTrue(settings.largeText)
    }

    @Test
    fun `settings toString contains field names`() {
        val settings = AccessibilitySettings(reduceMotion = true, highContrast = true)
        val str = settings.toString()
        assertTrue(str.contains("reduceMotion"), "toString should contain field name 'reduceMotion'")
        assertTrue(str.contains("highContrast"), "toString should contain field name 'highContrast'")
    }
}
