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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.storage.DesktopSettingsStorage
import digital.vasic.yole.desktop.ui.EnhancedYoleApp
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.desktop.window.DesktopWindowManager
import java.awt.Dimension

fun main() = application {
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
    
    if (exitRequested && appSettings.value.window.confirmOnExit) {
        // Handle confirmation in the window close request
    }
    
    Window(
        onCloseRequest = {
            if (appSettings.value.window.confirmOnExit) {
                exitRequested = true
            } else {
                exitApplication()
            }
        },
        title = "Yole - Text Editor",
        state = windowState,
        icon = null // Would load from resources
    ) {
        // Set minimum window size
        window.minimumSize = Dimension(800, 600)
        
        // Apply theme based on settings
        val themeMode = when (appSettings.value.appearance.themeMode) {
            "light" -> digital.vasic.yole.ui.ThemeMode.LIGHT
            "dark" -> digital.vasic.yole.ui.ThemeMode.DARK
            else -> digital.vasic.yole.ui.ThemeMode.SYSTEM
        }
        
        YoleDesktopTheme(
            themeMode = themeMode,
            accentColor = appSettings.value.appearance.accentColor?.let { 
                digital.vasic.yole.desktop.ui.theme.parseHexColor(it)
            },
            highContrast = appSettings.value.appearance.highContrastEnabled
        ) {
            Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                EnhancedYoleApp()
            }
        }
    }
}

/**
 * Confirmation dialog for exit confirmation.
 */
