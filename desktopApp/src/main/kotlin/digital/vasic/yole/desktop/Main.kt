/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main entry point for Yole Desktop App
 * Cross-platform desktop applications
 *
 *########################################################*/

package digital.vasic.yole.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import digital.vasic.yole.desktop.ui.YoleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Yole - Text Editor"
    ) {
        MaterialTheme {
            Surface {
                YoleApp()
            }
        }
    }
}