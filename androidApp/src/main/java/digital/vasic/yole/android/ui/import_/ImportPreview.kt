/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 9: ImportPreview — full-screen modal showing the
 * converted Markdown, an editable filename TextField, and Save/Cancel
 * buttons.  The ImportWarningsPanel is embedded as a collapsible panel
 * when the imported document carries warnings.
 *
 * testTag conventions:
 *   root:                   "import-preview"
 *   Save button:            "import-preview-save"
 *   Cancel button:          "import-preview-cancel"
 *   filename input:         "import-preview-filename-input"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Remove testTag("import-preview") → root-tag assertion FAILS.
 *   2. Remove testTag("import-preview-save") → save-tag assertion FAILS.
 *   3. Remove testTag("import-preview-cancel") → cancel-tag assertion FAILS.
 *   4. Remove testTag("import-preview-filename-input") → input-tag assertion FAILS.
 *   5. Stub onSave lambda to ignore filename (pass "" instead) →
 *      importPreviewSaveCallbackReceivesFilename FAILS.
 *   6. Remove onCancel from Cancel onClick → cancel-callback assertion FAILS.
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import digital.vasic.yole.import_.ImportedDocument

/**
 * Full-screen modal preview of a successfully converted document.
 *
 * Shows the converted Markdown text (scrollable), an optional collapsible
 * [ImportWarningsPanel], an editable filename [TextField], and Save/Cancel
 * action buttons.
 *
 * @param doc              the [ImportedDocument] produced by the importer.
 * @param suggestedFileName initial value for the filename field.
 * @param onSave           invoked with the (possibly edited) filename when the
 *                         user confirms the import.
 * @param onCancel         invoked when the user dismisses without saving.
 */
@Composable
fun ImportPreview(
    doc: ImportedDocument,
    suggestedFileName: String,
    onSave: (filename: String) -> Unit,
    onCancel: () -> Unit,
) {
    var fileName by remember { mutableStateOf(suggestedFileName) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("import-preview"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // Scrollable Markdown preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(text = doc.markdown)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Warnings panel (hidden when empty)
                if (doc.warnings.isNotEmpty()) {
                    ImportWarningsPanel(warnings = doc.warnings)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Editable filename
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import-preview-filename-input"),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("import-preview-cancel"),
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(fileName) },
                        modifier = Modifier.testTag("import-preview-save"),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
