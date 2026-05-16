/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 9: ImportMenuItem — Material3 DropdownMenuItem for the
 * app overflow / hamburger menu.
 *
 * testTag: "import-menu-item"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Remove testTag("import-menu-item") → structural check FAILS.
 *   2. Change text to any string other than "Import from..." →
 *      importMenuItemText assertion FAILS.
 *   3. Remove onImportRequest from onClick → callback assertion FAILS.
 *   Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: ships here.
 *   Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.import_

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Overflow-menu item that triggers the import flow.
 *
 * @param onImportRequest invoked when the user taps the item.
 * @param modifier optional modifier chain.
 */
@Composable
fun ImportMenuItem(
    onImportRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = { Text("Import from...") },
        onClick = onImportRequest,
        modifier = modifier.testTag("import-menu-item"),
    )
}
