/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Window Manager for Yole
 * Multi-window support with native window management
 *
 *########################################################*/
package digital.vasic.yole.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import digital.vasic.yole.desktop.file.DesktopFileManager
import digital.vasic.yole.format.TextFormat
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages multiple windows for the Yole desktop application.
 * Provides native window management with state persistence.
 */
class DesktopWindowManager {
    private val windows = mutableStateListOf<DesktopWindow>()
    private val nextWindowId = AtomicInteger(1)
    
    companion object {
        private const val MAX_WINDOWS = 10
        private const val PREFERENCES_KEY = "window_states"
    }
    
    /**
     * Creates a new window with the specified file.
     */
    fun createWindow(file: File? = null, content: String = ""): DesktopWindow {
        val windowId = "window_${nextWindowId.getAndIncrement()}"
        val window = DesktopWindow(
            id = windowId,
            title = file?.name ?: "Untitled ${windows.size + 1}",
            file = file,
            content = content,
            isModified = false
        )
        
        synchronized(windows) {
            if (windows.size >= MAX_WINDOWS) {
                // Close the oldest window
                windows.removeAt(0)
            }
            windows.add(window)
        }
        
        return window
    }
    
    /**
     * Closes a window.
     */
    fun closeWindow(windowId: String): Boolean {
        synchronized(windows) {
            val window = windows.find { it.id == windowId }
            if (window != null) {
                // Check for unsaved changes
                if (window.isModified && window.file != null) {
                    // User should be prompted to save changes
                    return false
                }
                windows.remove(window)
                return true
            }
            return false
        }
    }
    
    /**
     * Gets all open windows.
     * Returns a copy to ensure thread safety.
     */
    fun getWindows(): List<DesktopWindow> {
        synchronized(windows) {
            return windows.toList()
        }
    }

    /**
     * Gets a specific window by ID.
     */
    fun getWindow(windowId: String): DesktopWindow? {
        synchronized(windows) {
            return windows.find { it.id == windowId }
        }
    }
    
    /**
     * Updates window content and modified state.
     */
    fun updateWindowContent(windowId: String, content: String, isModified: Boolean = true) {
        synchronized(windows) {
            val window = windows.find { it.id == windowId }
            window?.let {
                it.content = content
                it.isModified = isModified
            }
        }
    }
    
    /**
     * Updates window file association.
     */
    fun updateWindowFile(windowId: String, file: File?, title: String? = null) {
        synchronized(windows) {
            val window = windows.find { it.id == windowId }
            window?.let {
                it.file = file
                it.title = title ?: file?.name ?: it.title
                it.isModified = false
            }
        }
    }
    
    /**
     * Saves window state for persistence.
     */
    fun saveWindowState() {
        // Implementation for saving window states
        // This could include window positions, sizes, and open files
    }
    
    /**
     * Loads window state from persistence.
     */
    fun loadWindowState() {
        // Implementation for loading window states
    }
    
    /**
     * Checks if any window has unsaved changes.
     */
    fun hasUnsavedChanges(): Boolean {
        synchronized(windows) {
            return windows.any { it.isModified }
        }
    }

    /**
     * Gets windows with unsaved changes.
     * Returns a copy to ensure thread safety.
     */
    fun getWindowsWithUnsavedChanges(): List<DesktopWindow> {
        synchronized(windows) {
            return windows.filter { it.isModified }
        }
    }
}

/**
 * Represents a desktop window with its state and content.
 */
data class DesktopWindow(
    val id: String,
    var title: String,
    var file: File? = null,
    var content: String = "",
    var isModified: Boolean = false,
    val format: TextFormat? = null,
    val windowState: WindowState = WindowState()
) {
    /**
     * Gets the display title for the window.
     */
    fun getDisplayTitle(): String {
        return if (isModified) "* $title" else title
    }
    
    /**
     * Checks if this window represents a new unsaved document.
     */
    fun isNewDocument(): Boolean {
        return file == null
    }
}

/**
 * Window state configuration for persistence.
 */
data class WindowStateConfig(
    val placement: WindowPlacement = WindowPlacement.Floating,
    val isMinimized: Boolean = false,
    val position: WindowPosition = WindowPosition(),
    val size: WindowSize = WindowSize()
)

/**
 * Window position data class.
 */
data class WindowPosition(
    val x: Int = 100,
    val y: Int = 100
)

/**
 * Window size data class.
 */
data class WindowSize(
    val width: Int = 1200,
    val height: Int = 800
)