/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Keyboard Shortcuts for Yole
 * Comprehensive keyboard shortcut system with customization
 *
 *########################################################*/
package digital.vasic.yole.desktop.shortcut

import androidx.compose.ui.input.key.*
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences

/**
 * Comprehensive keyboard shortcut system for Yole desktop application.
 * Supports customizable shortcuts with platform-specific adaptations.
 * Thread-safe for concurrent access from UI and settings threads.
 */
class DesktopKeyboardShortcuts {
    private val prefs by lazy { Preferences.userNodeForPackage(DesktopKeyboardShortcuts::class.java) }
    
    companion object {
        // File operations
        const val ACTION_NEW_FILE = "new_file"
        const val ACTION_OPEN_FILE = "open_file"
        const val ACTION_SAVE_FILE = "save_file"
        const val ACTION_SAVE_AS_FILE = "save_as_file"
        const val ACTION_CLOSE_FILE = "close_file"
        const val ACTION_EXIT = "exit"
        
        // Edit operations
        const val ACTION_UNDO = "undo"
        const val ACTION_REDO = "redo"
        const val ACTION_CUT = "cut"
        const val ACTION_COPY = "copy"
        const val ACTION_PASTE = "paste"
        const val ACTION_FIND = "find"
        const val ACTION_REPLACE = "replace"
        const val ACTION_SELECT_ALL = "select_all"
        const val ACTION_DUPLICATE_LINE = "duplicate_line"
        const val ACTION_DELETE_LINE = "delete_line"
        
        // View operations
        const val ACTION_ZOOM_IN = "zoom_in"
        const val ACTION_ZOOM_OUT = "zoom_out"
        const val ACTION_RESET_ZOOM = "reset_zoom"
        const val ACTION_TOGGLE_WORD_WRAP = "toggle_word_wrap"
        const val ACTION_TOGGLE_LINE_NUMBERS = "toggle_line_numbers"
        const val ACTION_TOGGLE_TOOLBAR = "toggle_toolbar"
        const val ACTION_TOGGLE_STATUS_BAR = "toggle_status_bar"
        const val ACTION_FULLSCREEN = "fullscreen"
        
        // Navigation
        const val ACTION_GO_TO_LINE = "go_to_line"
        const val ACTION_GO_TO_DEFINITION = "go_to_definition"
        const val ACTION_GO_TO_PREVIOUS = "go_to_previous"
        const val ACTION_GO_TO_NEXT = "go_to_next"
        
        // Text operations
        const val ACTION_INDENT = "indent"
        const val ACTION_UNINDENT = "unindent"
        const val ACTION_COMMENT_LINE = "comment_line"
        const val ACTION_BLOCK_COMMENT = "block_comment"
        const val ACTION_FORMAT_DOCUMENT = "format_document"
        
        // Window operations
        const val ACTION_NEW_WINDOW = "new_window"
        const val ACTION_CLOSE_WINDOW = "close_window"
        const val ACTION_NEXT_WINDOW = "next_window"
        const val ACTION_PREVIOUS_WINDOW = "previous_window"
        
        // Default shortcuts
        private val DEFAULT_SHORTCUTS = mapOf(
            // File operations
            ACTION_NEW_FILE to KeyShortcut(Key.N, ctrl = true),
            ACTION_OPEN_FILE to KeyShortcut(Key.O, ctrl = true),
            ACTION_SAVE_FILE to KeyShortcut(Key.S, ctrl = true),
            ACTION_SAVE_AS_FILE to KeyShortcut(Key.S, ctrl = true, shift = true),
            ACTION_CLOSE_FILE to KeyShortcut(Key.W, ctrl = true),
            ACTION_EXIT to KeyShortcut(Key.F4, alt = true),
            
            // Edit operations
            ACTION_UNDO to KeyShortcut(Key.Z, ctrl = true),
            ACTION_REDO to KeyShortcut(Key.Y, ctrl = true),
            ACTION_CUT to KeyShortcut(Key.X, ctrl = true),
            ACTION_COPY to KeyShortcut(Key.C, ctrl = true),
            ACTION_PASTE to KeyShortcut(Key.V, ctrl = true),
            ACTION_FIND to KeyShortcut(Key.F, ctrl = true),
            ACTION_REPLACE to KeyShortcut(Key.H, ctrl = true),
            ACTION_SELECT_ALL to KeyShortcut(Key.A, ctrl = true),
            ACTION_DUPLICATE_LINE to KeyShortcut(Key.D, ctrl = true),
            ACTION_DELETE_LINE to KeyShortcut(Key.Delete, ctrl = true),
            
            // View operations
            ACTION_ZOOM_IN to KeyShortcut(Key.Plus, ctrl = true),
            ACTION_ZOOM_OUT to KeyShortcut(Key.Minus, ctrl = true),
            ACTION_RESET_ZOOM to KeyShortcut(Key.Zero, ctrl = true),
            ACTION_TOGGLE_WORD_WRAP to KeyShortcut(Key.W, alt = true),
            ACTION_TOGGLE_LINE_NUMBERS to KeyShortcut(Key.L, alt = true),
            ACTION_TOGGLE_TOOLBAR to KeyShortcut(Key.T, alt = true),
            ACTION_TOGGLE_STATUS_BAR to KeyShortcut(Key.B, alt = true),
            ACTION_FULLSCREEN to KeyShortcut(Key.F11),
            
            // Navigation
            ACTION_GO_TO_LINE to KeyShortcut(Key.G, ctrl = true),
            ACTION_GO_TO_DEFINITION to KeyShortcut(Key.F12),
            ACTION_GO_TO_PREVIOUS to KeyShortcut(Key.F12, ctrl = true, shift = true),
            ACTION_GO_TO_NEXT to KeyShortcut(Key.F12, ctrl = true),
            
            // Text operations
            ACTION_INDENT to KeyShortcut(Key.Tab),
            ACTION_UNINDENT to KeyShortcut(Key.Tab, shift = true),
            ACTION_COMMENT_LINE to KeyShortcut(Key.Slash, ctrl = true),
            ACTION_BLOCK_COMMENT to KeyShortcut(Key.Slash, ctrl = true, shift = true),
            ACTION_FORMAT_DOCUMENT to KeyShortcut(Key.F, ctrl = true, shift = true),
            
            // Window operations
            ACTION_NEW_WINDOW to KeyShortcut(Key.N, ctrl = true, shift = true),
            ACTION_CLOSE_WINDOW to KeyShortcut(Key.W, ctrl = true, shift = true),
            ACTION_NEXT_WINDOW to KeyShortcut(Key.Tab, ctrl = true),
            ACTION_PREVIOUS_WINDOW to KeyShortcut(Key.Tab, ctrl = true, shift = true)
        )
    }
    
    // Thread-safe map for custom shortcuts
    private val customShortcuts = ConcurrentHashMap<String, KeyShortcut>()
    
    init {
        loadCustomShortcuts()
    }
    
    /**
     * Gets the shortcut for a specific action.
     */
    fun getShortcut(action: String): KeyShortcut? {
        return customShortcuts[action] ?: DEFAULT_SHORTCUTS[action]
    }
    
    /**
     * Sets a custom shortcut for an action.
     */
    fun setShortcut(action: String, shortcut: KeyShortcut) {
        customShortcuts[action] = shortcut
        saveCustomShortcuts()
    }
    
    /**
     * Resets a shortcut to its default value.
     */
    fun resetShortcut(action: String) {
        customShortcuts.remove(action)
        saveCustomShortcuts()
    }
    
    /**
     * Resets all shortcuts to their default values.
     */
    fun resetAllShortcuts() {
        customShortcuts.clear()
        saveCustomShortcuts()
    }
    
    /**
     * Gets all shortcuts (custom and default).
     */
    fun getAllShortcuts(): Map<String, KeyShortcut> {
        val allShortcuts = mutableMapOf<String, KeyShortcut>()
        allShortcuts.putAll(DEFAULT_SHORTCUTS)
        allShortcuts.putAll(customShortcuts)
        return allShortcuts
    }
    
    /**
     * Checks if a key event matches a shortcut for an action.
     */
    fun matchesShortcut(event: KeyEvent, action: String): Boolean {
        val shortcut = getShortcut(action) ?: return false
        return shortcut.matches(event)
    }
    
    /**
     * Finds the action that matches a key event.
     */
    fun findActionForShortcut(event: KeyEvent): String? {
        return getAllShortcuts().entries.find { (_, shortcut) ->
            shortcut.matches(event)
        }?.key
    }
    
    /**
     * Handles a key event and returns the matching action.
     */
    fun handleKeyEvent(event: KeyEvent): String? {
        if (event.type != KeyEventType.KeyDown) return null
        return findActionForShortcut(event)
    }
    
    /**
     * Loads custom shortcuts from preferences.
     */
    private fun loadCustomShortcuts() {
        try {
            val shortcutsJson = prefs.get("custom_shortcuts", "{}")
            // Parse JSON and populate customShortcuts map
            // This is a simplified implementation
        } catch (e: Exception) {
            // Ignore errors loading shortcuts
        }
    }
    
    /**
     * Saves custom shortcuts to preferences.
     */
    private fun saveCustomShortcuts() {
        try {
            // Convert customShortcuts to JSON and save
            // This is a simplified implementation
        } catch (e: Exception) {
            // Ignore errors saving shortcuts
        }
    }
    
    /**
     * Gets platform-specific modifier keys.
     */
    fun getPlatformModifiers(): PlatformModifiers {
        return when (getCurrentPlatform()) {
            Platform.WINDOWS -> PlatformModifiers(ctrl = true, alt = false, meta = false)
            Platform.MACOS -> PlatformModifiers(ctrl = false, alt = false, meta = true)
            Platform.LINUX -> PlatformModifiers(ctrl = true, alt = false, meta = false)
        }
    }
    
    /**
     * Gets the current platform.
     */
    private fun getCurrentPlatform(): Platform {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> Platform.WINDOWS
            osName.contains("mac") -> Platform.MACOS
            else -> Platform.LINUX
        }
    }
}

/**
 * Represents a keyboard shortcut combination.
 */
data class KeyShortcut(
    val key: Key,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false
) {
    /**
     * Checks if this shortcut matches a key event.
     */
    fun matches(event: KeyEvent): Boolean {
        return event.key == key &&
               event.isCtrlPressed == ctrl &&
               event.isAltPressed == alt &&
               event.isShiftPressed == shift &&
               event.isMetaPressed == meta
    }
    
    /**
     * Gets a display string for this shortcut.
     */
    fun getDisplayString(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts.add("Ctrl")
        if (alt) parts.add("Alt")
        if (shift) parts.add("Shift")
        if (meta) parts.add("Cmd")
        parts.add(keyToDisplayString(key))
        return parts.joinToString("+")
    }
    
    /**
     * Converts a key to a display string.
     */
    private fun keyToDisplayString(key: Key): String {
        return when (key) {
            Key.Plus -> "+"
            Key.Minus -> "-"
            Key.Zero -> "0"
            Key.Tab -> "Tab"
            Key.Enter -> "Enter"
            Key.Delete -> "Delete"
            Key.Escape -> "Esc"
            Key.Spacebar -> "Space"
            Key.Backspace -> "Backspace"
            Key.F1 -> "F1"
            Key.F2 -> "F2"
            Key.F3 -> "F3"
            Key.F4 -> "F4"
            Key.F5 -> "F5"
            Key.F6 -> "F6"
            Key.F7 -> "F7"
            Key.F8 -> "F8"
            Key.F9 -> "F9"
            Key.F10 -> "F10"
            Key.F11 -> "F11"
            Key.F12 -> "F12"
            Key.Slash -> "/"
            else -> {
                // Key.toString() returns "Key: X" for letter/symbol keys;
                // strip the "Key: " prefix to get just the key name.
                val str = key.toString()
                if (str.startsWith("Key: ")) str.removePrefix("Key: ") else str
            }
        }
    }
}

/**
 * Platform-specific modifier keys.
 */
data class PlatformModifiers(
    val ctrl: Boolean,
    val alt: Boolean,
    val meta: Boolean
)

/**
 * Supported platforms.
 */
enum class Platform {
    WINDOWS,
    MACOS,
    LINUX
}