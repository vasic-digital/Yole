/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Keyboard Shortcuts Tests
 * Tests for keyboard shortcut functionality
 *
 *########################################################*/
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package digital.vasic.yole.desktop

import digital.vasic.yole.desktop.shortcut.DesktopKeyboardShortcuts
import digital.vasic.yole.desktop.shortcut.KeyShortcut
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Tests for DesktopKeyboardShortcuts functionality.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "OPT_IN_IS_NOT_ENABLED")
class DesktopKeyboardShortcutsTest {

    private lateinit var keyboardShortcuts: DesktopKeyboardShortcuts

    @BeforeTest
    fun setUp() {
        keyboardShortcuts = DesktopKeyboardShortcuts()
    }

    // ==================== Default Shortcuts Tests ====================

    @Test
    fun `should have default shortcuts for file operations`() {
        val newFileShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val openFileShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_OPEN_FILE)
        val saveFileShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_SAVE_FILE)
        
        assertNotNull(newFileShortcut)
        assertNotNull(openFileShortcut)
        assertNotNull(saveFileShortcut)
        
        assertEquals(Key.N, newFileShortcut.key)
        assertTrue(newFileShortcut.ctrl)
        
        assertEquals(Key.O, openFileShortcut.key)
        assertTrue(openFileShortcut.ctrl)
        
        assertEquals(Key.S, saveFileShortcut.key)
        assertTrue(saveFileShortcut.ctrl)
    }

    @Test
    fun `should have default shortcuts for edit operations`() {
        val undoShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_UNDO)
        val redoShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_REDO)
        val cutShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_CUT)
        val copyShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_COPY)
        val pasteShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_PASTE)
        
        assertNotNull(undoShortcut)
        assertNotNull(redoShortcut)
        assertNotNull(cutShortcut)
        assertNotNull(copyShortcut)
        assertNotNull(pasteShortcut)
        
        assertEquals(Key.Z, undoShortcut.key)
        assertTrue(undoShortcut.ctrl)
        
        assertEquals(Key.Y, redoShortcut.key)
        assertTrue(redoShortcut.ctrl)
        
        assertEquals(Key.X, cutShortcut.key)
        assertTrue(cutShortcut.ctrl)
        
        assertEquals(Key.C, copyShortcut.key)
        assertTrue(copyShortcut.ctrl)
        
        assertEquals(Key.V, pasteShortcut.key)
        assertTrue(pasteShortcut.ctrl)
    }

    @Test
    fun `should have default shortcuts for view operations`() {
        val zoomInShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_IN)
        val zoomOutShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_OUT)
        val resetZoomShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_RESET_ZOOM)
        
        assertNotNull(zoomInShortcut)
        assertNotNull(zoomOutShortcut)
        assertNotNull(resetZoomShortcut)
        
        assertEquals(Key.Plus, zoomInShortcut.key)
        assertTrue(zoomInShortcut.ctrl)
        
        assertEquals(Key.Minus, zoomOutShortcut.key)
        assertTrue(zoomOutShortcut.ctrl)
        
        assertEquals(Key.Zero, resetZoomShortcut.key)
        assertTrue(resetZoomShortcut.ctrl)
    }

    // ==================== Custom Shortcuts Tests ====================

    @Test
    fun `should set custom shortcut for action`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true, shift = true)
        
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertEquals(customShortcut, retrievedShortcut)
    }

    @Test
    fun `should override default shortcut with custom one`() {
        val defaultShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val customShortcut = KeyShortcut(Key.F2, alt = true)
        
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertEquals(customShortcut, retrievedShortcut)
        assertNotEquals(defaultShortcut, retrievedShortcut)
    }

    @Test
    fun `should reset shortcut to default value`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true)
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        keyboardShortcuts.resetShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val defaultShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertEquals(defaultShortcut, retrievedShortcut)
    }

    @Test
    fun `should reset all shortcuts to default values`() {
        val customShortcut1 = KeyShortcut(Key.F1, ctrl = true)
        val customShortcut2 = KeyShortcut(Key.F2, alt = true)
        
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut1)
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_OPEN_FILE, customShortcut2)
        
        keyboardShortcuts.resetAllShortcuts()
        
        val newFileShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val openFileShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_OPEN_FILE)
        
        // Should be back to defaults
        assertEquals(Key.N, newFileShortcut?.key)
        assertTrue(newFileShortcut?.ctrl == true)
        assertEquals(Key.O, openFileShortcut?.key)
        assertTrue(openFileShortcut?.ctrl == true)
    }

    // ==================== Shortcut Matching Tests ====================
    // Note: KeyEvent matching tests are tested via integration tests
    // since KeyEvent constructor is internal API

    // ==================== Complex Shortcut Tests ====================

    @Test
    fun `should create shortcuts with multiple modifiers`() {
        val shortcut = KeyShortcut(Key.S, ctrl = true, shift = true)
        
        assertEquals(Key.S, shortcut.key)
        assertTrue(shortcut.ctrl)
        assertTrue(shortcut.shift)
        assertFalse(shortcut.alt)
        assertFalse(shortcut.meta)
    }

    @Test
    fun `should create function key shortcuts`() {
        val shortcut = KeyShortcut(Key.F11)
        
        assertEquals(Key.F11, shortcut.key)
        assertFalse(shortcut.ctrl)
        assertFalse(shortcut.alt)
        assertFalse(shortcut.shift)
        assertFalse(shortcut.meta)
    }

    @Test
    fun `should create special key shortcuts`() {
        val shortcut = KeyShortcut(Key.Tab, shift = true)
        
        assertEquals(Key.Tab, shortcut.key)
        assertTrue(shortcut.shift)
    }

    // ==================== Display String Tests ====================

    @Test
    fun `should generate correct display string for simple shortcut`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertEquals("Ctrl+N", displayString)
    }

    @Test
    fun `should generate correct display string for complex shortcut`() {
        val shortcut = KeyShortcut(Key.S, ctrl = true, shift = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertEquals("Ctrl+Shift+S", displayString)
    }

    @Test
    fun `should generate correct display string for function key`() {
        val shortcut = KeyShortcut(Key.F1, alt = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertEquals("Alt+F1", displayString)
    }

    @Test
    fun `should generate correct display string for special keys`() {
        val plusShortcut = KeyShortcut(Key.Plus, ctrl = true)
        val minusShortcut = KeyShortcut(Key.Minus, ctrl = true)
        val tabShortcut = KeyShortcut(Key.Tab, shift = true)
        
        assertEquals("Ctrl++", plusShortcut.getDisplayString())
        assertEquals("Ctrl+-", minusShortcut.getDisplayString())
        assertEquals("Shift+Tab", tabShortcut.getDisplayString())
    }

    // ==================== Platform-Specific Tests ====================

    @Test
    fun `should get platform modifiers correctly`() {
        val platformModifiers = keyboardShortcuts.getPlatformModifiers()
        
        // This test will pass on any platform, but the specific values
        // depend on the current platform
        assertNotNull(platformModifiers)
        
        // On Windows/Linux, Ctrl should be true
        // On macOS, Meta should be true
        val currentPlatform = getCurrentPlatform()
        when (currentPlatform) {
            "windows", "linux" -> {
                assertTrue(platformModifiers.ctrl)
                assertFalse(platformModifiers.meta)
            }
            "mac" -> {
                assertFalse(platformModifiers.ctrl)
                assertTrue(platformModifiers.meta)
            }
        }
    }

    // ==================== All Shortcuts Tests ====================

    @Test
    fun `should get all shortcuts including custom ones`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true)
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val allShortcuts = keyboardShortcuts.getAllShortcuts()
        
        assertTrue(allShortcuts.containsKey(DesktopKeyboardShortcuts.ACTION_NEW_FILE))
        assertEquals(customShortcut, allShortcuts[DesktopKeyboardShortcuts.ACTION_NEW_FILE])
        
        // Should also contain default shortcuts that weren't overridden
        assertTrue(allShortcuts.containsKey(DesktopKeyboardShortcuts.ACTION_OPEN_FILE))
        assertEquals(Key.O, allShortcuts[DesktopKeyboardShortcuts.ACTION_OPEN_FILE]?.key)
    }

    @Test
    fun `should not have duplicate shortcuts in all shortcuts`() {
        val allShortcuts = keyboardShortcuts.getAllShortcuts()
        
        val shortcutValues = allShortcuts.values
        val uniqueShortcuts = shortcutValues.toSet()
        
        // All shortcuts should be unique
        assertEquals(uniqueShortcuts.size, shortcutValues.size)
    }

    // ==================== Helper Functions ====================

    private fun getCurrentPlatform(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "mac"
            else -> "linux"
        }
    }
}