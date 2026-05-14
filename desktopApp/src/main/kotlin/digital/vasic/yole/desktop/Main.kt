/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Main entry point for Yole Desktop App
 * Comprehensive desktop application with all features
 *
 *########################################################*/

package digital.vasic.yole.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.storage.DesktopSettingsStorage
import digital.vasic.yole.desktop.ui.EnhancedYoleApp
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.syntax.theme.ThemeProvider
import digital.vasic.yole.syntax.theme.ThemeRegistry
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File

/**
 * Global reference to the application exit callback for use by EnhancedYoleApp.
 */
internal var applicationExitCallback: (() -> Unit)? = null

/**
 * Global reference to the window visibility toggle for system tray.
 */
internal var windowVisibilityToggle: ((Boolean) -> Unit)? = null

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize parsers before launching the UI
    digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()

    application {
    // Load application settings
    val settingsStorage = DesktopSettingsStorage()
    val appSettings = remember { mutableStateOf(settingsStorage.loadSettings()) }

    // Window state management
    val windowState = rememberWindowState(
        width = appSettings.value.window.defaultWidth.dp,
        height = appSettings.value.window.defaultHeight.dp,
        position = WindowPosition(
            x = appSettings.value.window.defaultX.dp,
            y = appSettings.value.window.defaultY.dp
        )
    )

    // Handle window events
    var exitRequested by remember { mutableStateOf(false) }
    var isWindowVisible by remember { mutableStateOf(true) }

    // Dropped files state for drag-and-drop support
    var droppedFile by remember { mutableStateOf<File?>(null) }

    // Set up application exit callback
    applicationExitCallback = { exitApplication() }
    windowVisibilityToggle = { visible -> isWindowVisible = visible }

    // Exit confirmation dialog
    if (exitRequested) {
        if (appSettings.value.window.confirmOnExit) {
            Dialog(
                onCloseRequest = { exitRequested = false },
                title = "Confirm Exit",
                state = rememberDialogState(
                    width = 350.dp,
                    height = 180.dp,
                    position = WindowPosition(Alignment.Center)
                )
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Are you sure you want to exit?",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { exitRequested = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    // Save window state before exiting
                                    saveWindowState(settingsStorage, appSettings.value, windowState)
                                    exitApplication()
                                }) {
                                    Text("Exit")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Save window state and exit immediately
            saveWindowState(settingsStorage, appSettings.value, windowState)
            exitApplication()
        }
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (appSettings.value.window.minimizeToTray) {
                    isWindowVisible = false
                } else {
                    exitRequested = true
                }
            },
            title = "Yole - Text Editor",
            state = windowState,
            icon = null // Would load from resources
        ) {
            // Set minimum window size
            window.minimumSize = Dimension(800, 600)

            // Set up drag-and-drop support on the AWT window
            LaunchedEffect(Unit) {
                setupDragAndDrop(window) { file ->
                    droppedFile = file
                }
            }

            // Apply theme based on settings
            val themeMode = when (appSettings.value.appearance.themeMode) {
                "light" -> digital.vasic.yole.ui.ThemeMode.LIGHT
                "dark" -> digital.vasic.yole.ui.ThemeMode.DARK
                else -> digital.vasic.yole.ui.ThemeMode.SYSTEM
            }

            // iter-57 Phase 3: ThemeProvider publishes the active VS Code
            // theme to every Composable under it via LocalTheme. The
            // LaunchedEffect seeds the registry with the default ("Yole Dark"
            // or "Yole Light" depending on user themeMode). Failures are
            // swallowed: ThemeRegistry's bootstrap default keeps colors usable.
            ThemeProvider {
                val initialTheme = if (themeMode == digital.vasic.yole.ui.ThemeMode.LIGHT) {
                    "Yole Light"
                } else {
                    "Yole Dark"
                }
                LaunchedEffect(initialTheme) {
                    try {
                        ThemeRegistry.setActive(initialTheme)
                    } catch (t: Throwable) {
                        println("ThemeRegistry init skipped: ${t.message}")
                    }
                }
                YoleDesktopTheme(
                    themeMode = themeMode,
                    accentColor = appSettings.value.appearance.accentColor?.let {
                        digital.vasic.yole.desktop.ui.theme.parseHexColor(it)
                    },
                    highContrast = appSettings.value.appearance.highContrastEnabled
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        EnhancedYoleApp(
                            droppedFile = droppedFile,
                            onDroppedFileConsumed = { droppedFile = null },
                            onExit = { exitRequested = true }
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * Saves the current window state (size and position) to settings storage.
 */
private fun saveWindowState(
    settingsStorage: DesktopSettingsStorage,
    currentSettings: DesktopSettingsStorage.AppSettings,
    windowState: WindowState
) {
    try {
        val pos = windowState.position
        val posX = if (pos.isSpecified) pos.x.value.toInt() else currentSettings.window.defaultX
        val posY = if (pos.isSpecified) pos.y.value.toInt() else currentSettings.window.defaultY

        settingsStorage.saveSettings(
            currentSettings.copy(
                window = currentSettings.window.copy(
                    defaultWidth = windowState.size.width.value.toInt(),
                    defaultHeight = windowState.size.height.value.toInt(),
                    defaultX = posX,
                    defaultY = posY
                )
            )
        )
    } catch (e: Exception) {
        // Ignore errors saving window state
    }
}

/**
 * Sets up drag-and-drop file support on the AWT window.
 * When a file is dropped onto the window, the callback is invoked with the file.
 */
private fun setupDragAndDrop(window: java.awt.Window, onFileDrop: (File) -> Unit) {
    val dropTarget = DropTarget(window, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
        override fun drop(event: DropTargetDropEvent) {
            try {
                event.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = event.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    files.firstOrNull()?.let { file ->
                        if (file.isFile && file.canRead()) {
                            onFileDrop(file)
                        }
                    }
                }
                event.dropComplete(true)
            } catch (e: Exception) {
                event.dropComplete(false)
            }
        }
    })
    window.dropTarget = dropTarget
}
