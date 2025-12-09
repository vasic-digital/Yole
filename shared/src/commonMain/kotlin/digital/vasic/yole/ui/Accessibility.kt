/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared Accessibility System for Yole
 *
 * Provides cross-platform accessibility utilities, keyboard navigation,
 * screen reader support, and touch target management.
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility constants and minimum requirements.
 */
object AccessibilityConstants {

    // Touch target sizes (WCAG 2.1 AA)
    val MIN_TOUCH_TARGET_SIZE = 44.dp  // iOS/Android minimum
    val MIN_TOUCH_TARGET_SIZE_LARGE = 48.dp  // Android recommended

    // Focus indicator minimum width
    val FOCUS_INDICATOR_WIDTH = 2.dp

    // Keyboard shortcuts
    object Shortcuts {
        const val SAVE = "Ctrl+S"
        const val NEW = "Ctrl+N"
        const val OPEN = "Ctrl+O"
        const val CLOSE = "Ctrl+W"
        const val UNDO = "Ctrl+Z"
        const val REDO = "Ctrl+Y"
        const val FIND = "Ctrl+F"
        const val SELECT_ALL = "Ctrl+A"
        const val SETTINGS = "Ctrl+,"
        const val HELP = "F1"
        const val ESCAPE = "Escape"
    }

    // Semantic roles for screen readers
    enum class SemanticRole {
        BUTTON,
        LINK,
        HEADER,
        LIST_ITEM,
        LIST,
        NAVIGATION,
        MAIN_CONTENT,
        SECONDARY_CONTENT,
        STATUS,
        ALERT
    }
}

/**
 * Accessibility modifier extensions for Compose.
 */
object AccessibilityModifiers {

    /**
     * Adds semantic content description for screen readers.
     */
    fun Modifier.accessibleContentDescription(description: String): Modifier {
        return semantics { contentDescription = description }
    }

    /**
     * Adds semantic role for screen readers.
     */
    fun Modifier.accessibleRole(role: Role): Modifier {
        return semantics { this.role = role }
    }

    /**
     * Adds custom accessibility label combining multiple descriptions.
     */
    fun Modifier.accessibleLabel(
        primary: String,
        secondary: String? = null,
        state: String? = null
    ): Modifier {
        val description = buildString {
            append(primary)
            secondary?.let { append(", $it") }
            state?.let { append(". $it") }
        }
        return accessibleContentDescription(description)
    }

    /**
     * Ensures minimum touch target size for accessibility.
     */
    fun Modifier.accessibleTouchTarget(
        minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
    ): Modifier {
        return this // Implementation will be platform-specific
    }

    /**
     * Adds keyboard shortcut handling.
     * Platform-specific implementation required.
     */
    fun Modifier.accessibleShortcut(
        key: Key,
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        onShortcut: () -> Unit
    ): Modifier {
        // Platform-specific implementation
        return this
    }

    /**
     * Adds escape key handling for dialogs/modals.
     */
    fun Modifier.accessibleEscape(onEscape: () -> Unit): Modifier {
        return accessibleShortcut(Key.Escape, onShortcut = onEscape)
    }

    /**
     * Makes element focusable with proper focus indicators.
     */
    fun Modifier.accessibleFocusable(): Modifier {
        return this // Implementation will be platform-specific
    }
}

/**
 * Keyboard shortcut definitions and utilities.
 */
object KeyboardShortcuts {

    /**
     * Common keyboard shortcuts used across the app.
     */
    enum class Shortcut(
        val key: Key,
        val ctrl: Boolean = false,
        val alt: Boolean = false,
        val shift: Boolean = false,
        val description: String
    ) {
        SAVE(Key.S, ctrl = true, description = "Save current file"),
        NEW(Key.N, ctrl = true, description = "Create new file"),
        OPEN(Key.O, ctrl = true, description = "Open file"),
        CLOSE(Key.W, ctrl = true, description = "Close current tab"),
        UNDO(Key.Z, ctrl = true, description = "Undo last action"),
        REDO(Key.Y, ctrl = true, description = "Redo last action"),
        FIND(Key.F, ctrl = true, description = "Find in current file"),
        SELECT_ALL(Key.A, ctrl = true, description = "Select all"),
        SETTINGS(Key.Comma, ctrl = true, description = "Open settings"),
        HELP(Key.F1, description = "Show help"),
        ESCAPE(Key.Escape, description = "Close dialog or cancel action")
    }

    /**
     * Gets the display string for a shortcut.
     */
    fun Shortcut.toDisplayString(): String {
        return buildString {
            if (ctrl) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            append(key.keyCode.toString()) // This will need platform-specific implementation
        }
    }
}

/**
 * Accessibility settings and preferences.
 */
data class AccessibilitySettings(
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val screenReaderEnabled: Boolean = false,
    val keyboardNavigation: Boolean = true,
    val focusIndicators: Boolean = true,
    val announceChanges: Boolean = true
) {
    companion object {
        val DEFAULT = AccessibilitySettings()
    }
}

/**
 * Accessibility state management.
 */
object AccessibilityState {

    /**
     * Announces content changes to screen readers.
     */
    fun announce(message: String) {
        // Platform-specific implementation
    }

    /**
     * Checks if screen reader is active.
     */
    fun isScreenReaderActive(): Boolean {
        // Platform-specific implementation
        return false
    }

    /**
     * Gets current accessibility settings.
     */
    fun getSettings(): AccessibilitySettings {
        // Platform-specific implementation
        return AccessibilitySettings.DEFAULT
    }
}

/**
 * Focus management utilities.
 */
object FocusManagement {

    /**
     * Moves focus to the next element in tab order.
     */
    fun moveFocusNext() {
        // Platform-specific implementation
    }

    /**
     * Moves focus to the previous element in tab order.
     */
    fun moveFocusPrevious() {
        // Platform-specific implementation
    }

    /**
     * Moves focus to a specific element.
     */
    fun moveFocusTo(elementId: String) {
        // Platform-specific implementation
    }
}

/**
 * Screen reader utilities for dynamic content.
 */
object ScreenReader {

    /**
     * Announces a status update.
     */
    fun announceStatus(message: String) {
        announce(message, AccessibilityConstants.SemanticRole.STATUS)
    }

    /**
     * Announces an alert or error.
     */
    fun announceAlert(message: String) {
        announce(message, AccessibilityConstants.SemanticRole.ALERT)
    }

    /**
     * Announces a general message.
     */
    fun announce(message: String, role: AccessibilityConstants.SemanticRole = AccessibilityConstants.SemanticRole.STATUS) {
        // Platform-specific implementation
        AccessibilityState.announce(message)
    }

    /**
     * Creates a live region for dynamic content updates.
     */
    fun Modifier.liveRegion(): Modifier {
        return this // Platform-specific implementation
    }
}

/**
 * Touch target utilities for ensuring accessibility.
 */
object TouchTargets {

    /**
     * Ensures a composable meets minimum touch target size.
     */
    fun Modifier.ensureMinTouchTarget(
        currentSize: Dp,
        minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
    ): Modifier {
        return if (currentSize < minSize) {
            // Add padding to meet minimum size
            val padding = (minSize - currentSize) / 2
            this // Implementation will add padding
        } else {
            this
        }
    }

    /**
     * Checks if a size meets accessibility requirements.
     */
    fun isAccessibleSize(size: Dp, minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE): Boolean {
        return size >= minSize
    }
}