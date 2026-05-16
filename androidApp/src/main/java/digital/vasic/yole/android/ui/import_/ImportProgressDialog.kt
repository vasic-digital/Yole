/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 9: ImportProgressDialog — AlertDialog shown while a
 * document import conversion is in progress.
 *
 * Displays a CircularProgressIndicator, the file name being converted,
 * and a Cancel button.
 *
 * testTag: "import-progress-dialog"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Remove testTag("import-progress-dialog") →
 *      importProgressDialogHasCorrectTestTag FAILS.
 *   2. Remove CircularProgressIndicator call →
 *      importProgressDialogContainsProgressIndicator FAILS.
 *   3. Remove the fileName label text node →
 *      importProgressDialogShowsFileName FAILS.
 *   4. Remove onCancel from the Cancel button onClick →
 *      importProgressDialogCancelCallsCallback FAILS.
 *   Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: ships here.
 *   Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.import_

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Modal progress dialog displayed while a document is being converted.
 *
 * @param fileName the name of the file currently being imported.
 * @param onCancel invoked when the user taps Cancel.
 */
@Composable
fun ImportProgressDialog(
    fileName: String,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("import-progress-dialog"),
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = fileName)
            }
        },
    )
}
