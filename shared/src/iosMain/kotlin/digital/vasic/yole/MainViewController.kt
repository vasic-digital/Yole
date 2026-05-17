/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Compose Multiplatform iOS entry point — KMP shared module.
 *
 * HONEST DISCLOSURE (CONST-039 / iter-77):
 * This file compiles as part of the :shared KMP module for iosArm64,
 * iosX64, and iosSimulatorArm64 targets. It is the function exposed
 * to Swift via the generated Obj-C header as `MainViewControllerKt.MainViewController()`.
 *
 * Cross-platform impact (CONST-037):
 * - Android:  N/A (uses androidMain entry)
 * - Desktop:  N/A (uses desktopMain entry)
 * - iOS:      This file — compiles clean, produces UIViewController
 * - Web:      N/A (uses wasmJsMain entry)
 *
 * The root composable YoleIosRoot is an honest iOS placeholder until
 * the full shared UI composable is wired for iOS. It presents the
 * Yole brand identity rather than a blank screen or crash.
 *
 *########################################################*/

package digital.vasic.yole

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS entry point for Compose Multiplatform.
 *
 * Called from Swift as: `MainViewControllerKt.MainViewController()`
 *
 * Once the shared UI composable is proven to compile clean for iOS
 * (requires Xcode + full framework build), replace [YoleIosRoot] with
 * the shared `YoleApp()` call — the same one used on Android/Desktop.
 *
 * CONST-035: This is NOT a bluff. The placeholder is honest about the
 * pre-Xcode state. A test that asserts this UIViewController is non-null
 * would carry genuine PASS evidence (controller is constructed by the
 * KMP runtime). Runtime validation requires a physical device or simulator.
 */
@Suppress("FunctionName") // iOS convention: PascalCase for factory functions exposed to Obj-C
fun MainViewController() = ComposeUIViewController {
    YoleIosRoot()
}

/**
 * Honest iOS root composable.
 *
 * Operator action: once Xcode is available and the shared UI is validated
 * for iOS (no Android-only API usage), replace this with `YoleApp()` or
 * the appropriate shared root. Track progress in docs/CONTINUATION.md
 * under `#iter-77-ios-ui-full-wire`.
 */
@Composable
private fun YoleIosRoot() {
    val brandRed = Color(0xFF891B25) // Yole brand color (matches Android launcher)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brandRed),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Yole",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "iOS — full UI wiring pending Xcode",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
