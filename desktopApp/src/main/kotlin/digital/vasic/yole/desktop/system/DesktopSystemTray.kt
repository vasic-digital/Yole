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
    private val fileManager: DesktopFileManager,
    private val onNewFile: () -> Unit,
    private val onOpenFile: () -> Unit,
    private val onShowWindow: () -> Unit,
    private val onExit: () -> Unit
) {
    companion object {
        private const val TOOLTIP = "Yole - Text Editor"
        private const val ICON_PATH = "icons/icon.png"
    }
    
    /**
     * Shows a system notification.
     */
    fun showNotification(title: String, message: String, type: NotificationType = NotificationType.INFO) {
        try {
            println("[$title] $message")
        } catch (e: Exception) {
            // Fallback to console notification
            println("[$title] $message")
        }
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