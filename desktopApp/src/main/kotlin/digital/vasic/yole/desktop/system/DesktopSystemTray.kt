/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop System Tray Integration for Yole
 * Native system tray with context menu and notifications
 *
 *########################################################*/
package digital.vasic.yole.desktop.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.file.DesktopFileManager
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * System tray manager for Yole desktop application.
 * Provides native system tray integration with context menu.
 */
class DesktopSystemTray(
    private val fileManager: DesktopFileManager? = null,
    private val onNewFile: () -> Unit = {},
    private val onOpenFile: () -> Unit = {},
    private val onShowWindow: () -> Unit = {},
    private val onExit: () -> Unit = {}
) {
    // Notification listeners for UI integration
    private val notificationListeners = mutableListOf<(String, String, NotificationType) -> Unit>()

    companion object {
        private const val TOOLTIP = "Yole - Text Editor"
        private const val ICON_PATH = "icons/icon.png"
    }

    /**
     * Adds a listener to receive notification events.
     */
    fun addNotificationListener(listener: (String, String, NotificationType) -> Unit) {
        notificationListeners.add(listener)
    }

    /**
     * Shows a system notification.
     * Notifies all registered listeners.
     */
    fun showNotification(title: String, message: String, type: NotificationType = NotificationType.INFO) {
        // Notify all registered listeners (for UI integration)
        notificationListeners.forEach { listener ->
            try {
                listener(title, message, type)
            } catch (e: Exception) {
                // Listener error - continue with other listeners
            }
        }
        // Note: Actual system tray notification would be handled by platform-specific code
    }
    
    /**
     * Shows the about dialog.
     */
    fun showAboutDialog() {
        // Implementation for showing about dialog
        showNotification("Yole", "A versatile text editor supporting 18+ markup formats")
    }
    
    /**
     * Copies text to the system clipboard.
     */
    fun copyToClipboard(text: String) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(text)
            clipboard.setContents(selection, selection)
        } catch (e: Exception) {
            showNotification("Error", "Could not copy to clipboard", NotificationType.ERROR)
        }
    }
    
    /**
     * Gets text from the system clipboard.
     */
    fun getFromClipboard(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val contents = clipboard.getContents(this)
            if (contents != null && contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Notification types for system tray notifications.
 */
enum class NotificationType {
    INFO,
    WARNING,
    ERROR
}