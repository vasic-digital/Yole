/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android Accessibility Implementation for Yole
 *
 * Provides Android-specific accessibility features including
 * TalkBack support, touch targets, and accessibility settings.
 *
 *########################################################*/

package digital.vasic.yole.android.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.vasic.yole.ui.AccessibilityConstants
import digital.vasic.yole.ui.AccessibilitySettings
import digital.vasic.yole.ui.AccessibilityState as SharedAccessibilityState
import digital.vasic.yole.ui.ScreenReader as SharedScreenReader
import digital.vasic.yole.ui.TouchTargets

/**
 * Android-specific accessibility utilities.
 */
object AndroidAccessibility {

    /**
     * Checks if TalkBack or other screen reader is active.
     */
    fun isScreenReaderActive(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Announces content to TalkBack.
     */
    fun announceForAccessibility(context: Context, message: String) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (accessibilityManager.isEnabled) {
            // Use AccessibilityManager to announce
            // Note: In a real implementation, you'd use AccessibilityEvent
            println("Accessibility announcement: $message")
        }
    }

    /**
     * Gets Android accessibility settings.
     */
    fun getAccessibilitySettings(context: Context): AccessibilitySettings {
        val settings = YoleSettings(context)

        return AccessibilitySettings(
            reduceMotion = settings.getReduceMotion(),
            highContrast = settings.getHighContrast(),
            largeText = isLargeTextEnabled(context),
            screenReaderEnabled = isScreenReaderActive(context),
            keyboardNavigation = true, // Android always supports keyboard navigation
            focusIndicators = true, // Android shows focus indicators by default
            announceChanges = settings.getAnnounceChanges()
        )
    }

    /**
     * Checks if large text is enabled in system settings.
     */
    private fun isLargeTextEnabled(context: Context): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale > 1.0f
    }
}

/**
 * Android-specific accessibility modifier implementations.
 */
object AndroidAccessibilityModifiers {

    /**
     * Ensures minimum touch target size for Android accessibility.
     */
    fun Modifier.androidAccessibleTouchTarget(
        currentSize: Dp = 44.dp, // Default to minimum size
        minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE_LARGE
    ): Modifier {
        return if (currentSize < minSize) {
            val padding = (minSize - currentSize) / 2
            this.padding(padding)
        } else {
            this
        }
    }

    /**
     * Adds Android-specific focus indicator.
     */
    fun Modifier.androidAccessibleFocusable(): Modifier {
        return this // Android handles focus indicators automatically
    }
}

/**
 * Android screen reader implementation.
 */
object AndroidScreenReader {

    fun announce(message: String, role: AccessibilityConstants.SemanticRole) {
        // Get context from composition local
        // This would need to be called from a composable context
        SharedScreenReader.announce(message, role)
    }

    fun Modifier.liveRegion(): Modifier {
        return this.semantics { liveRegion = LiveRegionMode.Polite }
    }
}

/**
 * Android accessibility state implementation.
 */
object AndroidAccessibilityState {

    fun announce(message: String) {
        // Implementation would use Android's accessibility framework
        SharedAccessibilityState.announce(message)
    }

    fun isScreenReaderActive(): Boolean {
        // This would need context, so it's handled in AndroidAccessibility
        return false
    }

    fun getSettings(): AccessibilitySettings {
        // This would need context, so it's handled in AndroidAccessibility
        return AccessibilitySettings.DEFAULT
    }
}