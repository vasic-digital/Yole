/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Keyboard Shortcuts Tests
 * Tests for keyboard shortcut functionality
 *
 *########################################################*/
package digital.vasic.yole.desktop

import digital.vasic.yole.desktop.shortcut.DesktopKeyboardShortcuts
import digital.vasic.yole.desktop.shortcut.KeyShortcut
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for DesktopKeyboardShortcuts functionality.
 */

class DesktopKeyboardShortcutsTest {

    private lateinit var keyboardShortcuts: DesktopKeyboardShortcuts

    @Before
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
        
        assertThat(newFileShortcut.key).isEqualTo(Key.N)
        assertThat(newFileShortcut.ctrl).isTrue()
        
        assertThat(openFileShortcut.key).isEqualTo(Key.O)
        assertThat(openFileShortcut.ctrl).isTrue()
        
        assertThat(saveFileShortcut.key).isEqualTo(Key.S)
        assertThat(saveFileShortcut.ctrl).isTrue()
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
        
        assertThat(undoShortcut.key).isEqualTo(Key.Z)
        assertThat(undoShortcut.ctrl).isTrue()
        
        assertThat(redoShortcut.key).isEqualTo(Key.Y)
        assertThat(redoShortcut.ctrl).isTrue()
        
        assertThat(cutShortcut.key).isEqualTo(Key.X)
        assertThat(cutShortcut.ctrl).isTrue()
        
        assertThat(copyShortcut.key).isEqualTo(Key.C)
        assertThat(copyShortcut.ctrl).isTrue()
        
        assertThat(pasteShortcut.key).isEqualTo(Key.V)
        assertThat(pasteShortcut.ctrl).isTrue()
    }

    @Test
    fun `should have default shortcuts for view operations`() {
        val zoomInShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_IN)
        val zoomOutShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_OUT)
        val resetZoomShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_RESET_ZOOM)
        
        assertNotNull(zoomInShortcut)
        assertNotNull(zoomOutShortcut)
        assertNotNull(resetZoomShortcut)
        
        assertThat(zoomInShortcut.key).isEqualTo(Key.Plus)
        assertThat(zoomInShortcut.ctrl).isTrue()
        
        assertThat(zoomOutShortcut.key).isEqualTo(Key.Minus)
        assertThat(zoomOutShortcut.ctrl).isTrue()
        
        assertThat(resetZoomShortcut.key).isEqualTo(Key.Zero)
        assertThat(resetZoomShortcut.ctrl).isTrue()
    }

    // ==================== Custom Shortcuts Tests ====================

    @Test
    fun `should set custom shortcut for action`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true, shift = true)
        
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertThat(retrievedShortcut).isEqualTo(customShortcut)
    }

    @Test
    fun `should override default shortcut with custom one`() {
        val defaultShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val customShortcut = KeyShortcut(Key.F2, alt = true)
        
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertThat(retrievedShortcut).isEqualTo(customShortcut)
        assertThat(retrievedShortcut).isNotEqualTo(defaultShortcut)
    }

    @Test
    fun `should reset shortcut to default value`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true)
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        keyboardShortcuts.resetShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        
        val retrievedShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        val defaultShortcut = keyboardShortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertThat(retrievedShortcut).isEqualTo(defaultShortcut)
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
        assertThat(newFileShortcut?.key).isEqualTo(Key.N)
        assertThat(newFileShortcut?.ctrl).isTrue()
        assertThat(openFileShortcut?.key).isEqualTo(Key.O)
        assertThat(openFileShortcut?.ctrl).isTrue()
    }

    // ==================== Shortcut Matching Tests ====================

    @Test
    fun `should match key event to shortcut correctly`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        val keyEvent = createKeyEvent(Key.N, ctrl = true, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isTrue()
    }

    @Test
    fun `should not match key event with wrong key`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        val keyEvent = createKeyEvent(Key.O, ctrl = true, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isFalse()
    }

    @Test
    fun `should not match key event with wrong modifiers`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        val keyEvent = createKeyEvent(Key.N, alt = true, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isFalse()
    }

    @Test
    fun `should not match key event on key up`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        val keyEvent = createKeyEvent(Key.N, ctrl = true, type = KeyEventType.KeyUp)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isFalse()
    }

    @Test
    fun `should find action for matching key event`() {
        val keyEvent = createKeyEvent(Key.N, ctrl = true, type = KeyEventType.KeyDown)
        
        val action = keyboardShortcuts.findActionForShortcut(keyEvent)
        
        assertThat(action).isEqualTo(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
    }

    @Test
    fun `should return null for non-matching key event`() {
        val keyEvent = createKeyEvent(Key.A, ctrl = true, type = KeyEventType.KeyDown)
        
        val action = keyboardShortcuts.findActionForShortcut(keyEvent)
        
        assertThat(action).isNull()
    }

    @Test
    fun `should handle key event and return matching action`() {
        val keyEvent = createKeyEvent(Key.S, ctrl = true, type = KeyEventType.KeyDown)
        
        val action = keyboardShortcuts.handleKeyEvent(keyEvent)
        
        assertThat(action).isEqualTo(DesktopKeyboardShortcuts.ACTION_SAVE_FILE)
    }

    @Test
    fun `should return null for non-key-down events`() {
        val keyEvent = createKeyEvent(Key.S, ctrl = true, type = KeyEventType.KeyUp)
        
        val action = keyboardShortcuts.handleKeyEvent(keyEvent)
        
        assertThat(action).isNull()
    }

    // ==================== Complex Shortcut Tests ====================

    @Test
    fun `should handle shortcuts with multiple modifiers`() {
        val shortcut = KeyShortcut(Key.S, ctrl = true, shift = true)
        val keyEvent = createKeyEvent(Key.S, ctrl = true, shift = true, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isTrue()
    }

    @Test
    fun `should handle function key shortcuts`() {
        val shortcut = KeyShortcut(Key.F11)
        val keyEvent = createKeyEvent(Key.F11, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isTrue()
    }

    @Test
    fun `should handle special key shortcuts`() {
        val shortcut = KeyShortcut(Key.Tab, shift = true)
        val keyEvent = createKeyEvent(Key.Tab, shift = true, type = KeyEventType.KeyDown)
        
        val matches = shortcut.matches(keyEvent)
        
        assertThat(matches).isTrue()
    }

    // ==================== Display String Tests ====================

    @Test
    fun `should generate correct display string for simple shortcut`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertThat(displayString).isEqualTo("Ctrl+N")
    }

    @Test
    fun `should generate correct display string for complex shortcut`() {
        val shortcut = KeyShortcut(Key.S, ctrl = true, shift = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertThat(displayString).isEqualTo("Ctrl+Shift+S")
    }

    @Test
    fun `should generate correct display string for function key`() {
        val shortcut = KeyShortcut(Key.F1, alt = true)
        
        val displayString = shortcut.getDisplayString()
        
        assertThat(displayString).isEqualTo("Alt+F1")
    }

    @Test
    fun `should generate correct display string for special keys`() {
        val plusShortcut = KeyShortcut(Key.Plus, ctrl = true)
        val minusShortcut = KeyShortcut(Key.Minus, ctrl = true)
        val tabShortcut = KeyShortcut(Key.Tab, shift = true)
        
        assertThat(plusShortcut.getDisplayString()).isEqualTo("Ctrl++")
        assertThat(minusShortcut.getDisplayString()).isEqualTo("Ctrl+-")
        assertThat(tabShortcut.getDisplayString()).isEqualTo("Shift+Tab")
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
                assertThat(platformModifiers.ctrl).isTrue()
                assertThat(platformModifiers.meta).isFalse()
            }
            "mac" -> {
                assertThat(platformModifiers.ctrl).isFalse()
                assertThat(platformModifiers.meta).isTrue()
            }
        }
    }

    // ==================== All Shortcuts Tests ====================

    @Test
    fun `should get all shortcuts including custom ones`() {
        val customShortcut = KeyShortcut(Key.F1, ctrl = true)
        keyboardShortcuts.setShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE, customShortcut)
        
        val allShortcuts = keyboardShortcuts.getAllShortcuts()
        
        assertThat(allShortcuts).containsKey(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertThat(allShortcuts[DesktopKeyboardShortcuts.ACTION_NEW_FILE]).isEqualTo(customShortcut)
        
        // Should also contain default shortcuts that weren't overridden
        assertThat(allShortcuts).containsKey(DesktopKeyboardShortcuts.ACTION_OPEN_FILE)
        assertThat(allShortcuts[DesktopKeyboardShortcuts.ACTION_OPEN_FILE]?.key).isEqualTo(Key.O)
    }

    @Test
    fun `should not have duplicate shortcuts in all shortcuts`() {
        val allShortcuts = keyboardShortcuts.getAllShortcuts()
        
        val shortcutValues = allShortcuts.values
        val uniqueShortcuts = shortcutValues.toSet()
        
        // All shortcuts should be unique
        assertThat(shortcutValues.size).isEqualTo(uniqueShortcuts.size)
    }

    // ==================== Helper Functions ====================

    private fun createKeyEvent(
        key: Key,
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        meta: Boolean = false,
        type: KeyEventType = KeyEventType.KeyDown
    ): KeyEvent {
        return KeyEvent(
            key = key,
            type = type,
            isCtrlPressed = ctrl,
            isAltPressed = alt,
            isShiftPressed = shift,
            isMetaPressed = meta
        )
    }

    private fun getCurrentPlatform(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "mac"
            else -> "linux"
        }
    }
}