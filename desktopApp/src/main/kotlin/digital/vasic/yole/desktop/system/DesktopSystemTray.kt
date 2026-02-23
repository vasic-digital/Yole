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
 * Provides native system tray integration with context menu, recent files,
 * show/hide window functionality, and desktop notifications.
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

    // AWT system tray icon reference
    private var trayIcon: java.awt.TrayIcon? = null

    companion object {
        private const val TOOLTIP = "Yole - Text Editor"
        private const val ICON_PATH = "icons/icon.png"
    }

    init {
        installSystemTray()
    }

    /**
     * Installs the system tray icon with context menu.
     */
    private fun installSystemTray() {
        if (!java.awt.SystemTray.isSupported()) return

        try {
            val systemTray = java.awt.SystemTray.getSystemTray()

            // Create a simple icon (16x16 solid color square as fallback)
            val image = try {
                val resource = javaClass.classLoader.getResource(ICON_PATH)
                if (resource != null) {
                    java.awt.Toolkit.getDefaultToolkit().getImage(resource)
                } else {
                    createDefaultIcon()
                }
            } catch (e: Exception) {
                createDefaultIcon()
            }

            val popup = buildPopupMenu()
            trayIcon = java.awt.TrayIcon(image, TOOLTIP, popup).apply {
                isImageAutoSize = true
                addActionListener { onShowWindow() }
            }

            systemTray.add(trayIcon)
        } catch (e: Exception) {
            // System tray not available on this platform
        }
    }

    /**
     * Creates a default tray icon as a fallback.
     */
    private fun createDefaultIcon(): java.awt.Image {
        val img = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(100, 100, 200)
        g.fillRoundRect(0, 0, 16, 16, 4, 4)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 12)
        g.drawString("Y", 3, 13)
        g.dispose()
        return img
    }

    /**
     * Builds the popup context menu for the system tray icon.
     */
    private fun buildPopupMenu(): java.awt.PopupMenu {
        val popup = java.awt.PopupMenu()

        val showItem = java.awt.MenuItem("Show Yole")
        showItem.addActionListener { onShowWindow() }
        popup.add(showItem)

        popup.addSeparator()

        val newFileItem = java.awt.MenuItem("New File")
        newFileItem.addActionListener { onNewFile() }
        popup.add(newFileItem)

        val openFileItem = java.awt.MenuItem("Open File...")
        openFileItem.addActionListener { onOpenFile() }
        popup.add(openFileItem)

        popup.addSeparator()

        // Recent files submenu
        val recentMenu = java.awt.Menu("Recent Files")
        val recentFiles = fileManager?.getRecentFiles() ?: emptyList()
        if (recentFiles.isEmpty()) {
            val emptyItem = java.awt.MenuItem("(No recent files)")
            emptyItem.isEnabled = false
            recentMenu.add(emptyItem)
        } else {
            recentFiles.take(5).forEach { file ->
                val item = java.awt.MenuItem(file.name)
                item.addActionListener {
                    onShowWindow()
                    // File opening would be handled through the callback chain
                }
                recentMenu.add(item)
            }
        }
        popup.add(recentMenu)

        popup.addSeparator()

        val exitItem = java.awt.MenuItem("Exit")
        exitItem.addActionListener { onExit() }
        popup.add(exitItem)

        return popup
    }

    /**
     * Updates the tray icon tooltip with current document info.
     */
    fun updateTooltip(fileName: String?) {
        trayIcon?.toolTip = if (fileName != null) "Yole - $fileName" else TOOLTIP
    }

    /**
     * Adds a listener to receive notification events.
     */
    fun addNotificationListener(listener: (String, String, NotificationType) -> Unit) {
        notificationListeners.add(listener)
    }

    /**
     * Shows a system notification using the tray icon if available,
     * and notifies all registered listeners.
     */
    fun showNotification(title: String, message: String, type: NotificationType = NotificationType.INFO) {
        // Show via AWT tray icon if available
        trayIcon?.let { icon ->
            val messageType = when (type) {
                NotificationType.INFO -> java.awt.TrayIcon.MessageType.INFO
                NotificationType.WARNING -> java.awt.TrayIcon.MessageType.WARNING
                NotificationType.ERROR -> java.awt.TrayIcon.MessageType.ERROR
            }
            try {
                icon.displayMessage(title, message, messageType)
            } catch (e: Exception) {
                // Tray notification failed - fall through to listeners
            }
        }

        // Notify all registered listeners (for UI integration)
        notificationListeners.forEach { listener ->
            try {
                listener(title, message, type)
            } catch (e: Exception) {
                // Listener error - continue with other listeners
            }
        }
    }

    /**
     * Shows the about dialog.
     */
    fun showAboutDialog() {
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

    /**
     * Removes the tray icon from the system tray.
     */
    fun dispose() {
        trayIcon?.let { icon ->
            try {
                java.awt.SystemTray.getSystemTray().remove(icon)
            } catch (e: Exception) {
                // Ignore
            }
        }
        trayIcon = null
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