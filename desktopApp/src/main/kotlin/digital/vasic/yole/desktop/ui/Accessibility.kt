/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Accessibility Implementation for Yole
 *
 * Provides desktop-specific accessibility features including
 * keyboard navigation, focus management, and screen reader support.
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.vasic.yole.ui.AccessibilityConstants
import digital.vasic.yole.ui.AccessibilitySettings
import digital.vasic.yole.ui.AccessibilityState
import digital.vasic.yole.ui.ScreenReader
import digital.vasic.yole.ui.TouchTargets

/**
 * Desktop-specific accessibility utilities.
 */
object DesktopAccessibility {

    /**
     * Checks if screen reader is active (NVDA, JAWS, etc.).
     */
    fun isScreenReaderActive(): Boolean {
        // On desktop, we can't easily detect screen readers
        // This would require native code or accessibility APIs
        return false
    }

    /**
     * Gets desktop accessibility settings.
     */
    fun getAccessibilitySettings(): AccessibilitySettings {
        val settings = YoleDesktopSettings()

        return AccessibilitySettings(
            reduceMotion = settings.getReduceMotion(),
            highContrast = settings.getHighContrastEnabled(),
            largeText = false, // Desktop handles this via system settings
            screenReaderEnabled = isScreenReaderActive(),
            keyboardNavigation = true,
            focusIndicators = settings.getFocusIndicators(),
            announceChanges = settings.getAnnounceChanges()
        )
    }

    /**
     * Announces content to screen readers.
     */
    fun announceForAccessibility(message: String) {
        // Desktop screen reader announcement
        // This would use platform-specific APIs (Windows UIA, macOS NSAccessibility)
        println("Desktop accessibility announcement: $message")
    }
}

/**
 * Desktop-specific accessibility modifier implementations.
 */
object DesktopAccessibilityModifiers {

    /**
     * Ensures minimum touch target size for desktop accessibility.
     */
    fun Modifier.desktopAccessibleTouchTarget(
        currentSize: Dp = 44.dp,
        minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE
    ): Modifier {
        return if (currentSize < minSize) {
            val padding = (minSize - currentSize) / 2
            this.padding(padding)
        } else {
            this
        }
    }

    /**
     * Adds desktop-specific focus indicator and keyboard handling.
     */
    fun Modifier.desktopAccessibleFocusable(
        focusRequester: FocusRequester? = null
    ): Modifier {
        return this
            .focusable()
            .apply {
                focusRequester?.let { focusRequester(it) }
            }
    }

    /**
     * Adds keyboard shortcut handling for desktop.
     */
    fun Modifier.desktopKeyboardShortcut(
        key: Key,
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        onShortcut: () -> Unit
    ): Modifier {
        // For now, simplified implementation without modifier checking
        // Desktop modifier detection requires platform-specific code
        return onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == key) {
                onShortcut()
                true
            } else {
                false
            }
        }
    }
}

/**
 * Desktop screen reader implementation.
 */
object DesktopScreenReader {

    fun announce(message: String, role: AccessibilityConstants.SemanticRole) {
        DesktopAccessibility.announceForAccessibility(message)
    }

    fun Modifier.liveRegion(): Modifier {
        return this // Desktop handles live regions via ARIA attributes
    }
}

/**
 * Desktop accessibility state implementation.
 */
object DesktopAccessibilityState {

    fun announce(message: String) {
        DesktopAccessibility.announceForAccessibility(message)
    }

    fun isScreenReaderActive(): Boolean {
        return DesktopAccessibility.isScreenReaderActive()
    }

    fun getSettings(): AccessibilitySettings {
        return DesktopAccessibility.getAccessibilitySettings()
    }
}

/**
 * Desktop focus management implementation.
 */
object DesktopFocusManagement {

    private val focusRequesters = mutableMapOf<String, FocusRequester>()

    /**
     * Registers a focus requester for an element.
     */
    fun registerFocusRequester(id: String, requester: FocusRequester) {
        focusRequesters[id] = requester
    }

    /**
     * Moves focus to a specific element.
     */
    fun requestFocus(id: String) {
        focusRequesters[id]?.requestFocus()
    }

    /**
     * Gets a focus requester for an element.
     */
    fun getFocusRequester(id: String): FocusRequester {
        return focusRequesters.getOrPut(id) { FocusRequester() }
    }
}