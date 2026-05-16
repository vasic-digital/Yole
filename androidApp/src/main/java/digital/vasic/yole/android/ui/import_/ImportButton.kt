/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 9: ImportButton — toolbar icon button that fires the
 * import-file-picker request.
 *
 * testTag: "import-button"
 *
 * Note: Uses Icons.Filled.Add (material-icons-core) as the upload affordance.
 * Icons.Filled.Upload lives in material-icons-extended which is not a
 * dependency of this module; Add is the closest core equivalent.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Remove testTag("import-button") → importButtonClickCallsCallback
 *      FAILS because "import-button" is absent from the source.
 *   2. Replace Icons.Filled.Add with any other icon →
 *      importButtonUsesAddIcon FAILS.
 *   3. Remove onImportRequest from onClick → importButtonClickCallsCallback
 *      FAILS because "onClick = onImportRequest" is absent from the source.
 *   Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: ships here.
 *   Desktop: toolbar import button deferred; tracked in CONTINUATION.md.
 *   iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.import_

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Toolbar icon button that triggers the import flow.
 *
 * @param onImportRequest invoked when the user taps the button.
 * @param modifier optional modifier chain.
 */
@Composable
fun ImportButton(
    onImportRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onImportRequest,
        modifier = modifier.testTag("import-button"),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Import",
        )
    }
}
