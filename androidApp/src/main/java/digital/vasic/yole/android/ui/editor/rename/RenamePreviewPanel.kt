/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 5: RenamePreviewPanel — Material3 ModalBottomSheet diff preview.
 *
 * Displays a per-file expandable diff preview for a [WorkspaceEdit] produced
 * by an LSP rename refactoring. The user can review the proposed changes and
 * confirm (Apply) or reject (Cancel) them.
 *
 * Design:
 *   - Material3 ModalBottomSheet with a header title.
 *   - One collapsible row per URI in [WorkspaceEdit.changes].
 *   - Collapsed row: filename + "(N edits)".
 *   - Expanded row: monospace before→after preview for each [TextEdit].
 *   - Apply and Cancel buttons in a persistent footer.
 *   - testTag convention:
 *       root content column:  "rename-preview-panel"
 *       per-file header row:  "rename-file-<uri>"
 *       Apply button:         "rename-apply"
 *       Cancel button:        "rename-cancel"
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — stub Composable body to empty (remove all LazyColumn, testTag,
 *   button, and expansion logic). All four Robolectric source-structural tests
 *   FAIL because the structural markers they assert vanish. Reverted; GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation here (ModalBottomSheet).
 *   - Desktop:  Window-based diff dialog deferred to Phase 10 integration.
 *   - iOS:      deferred — UIKit sheet variant planned.
 *   - Web:      deferred — dialog variant planned.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.rename

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.TextEdit
import digital.vasic.yole.lsp.WorkspaceEdit

/**
 * ModalBottomSheet showing a per-file expandable diff preview of [edit].
 *
 * @param edit      The [WorkspaceEdit] to preview (must be non-empty before display).
 * @param onApply   Called with the [WorkspaceEdit] when the user taps Apply.
 * @param onDismiss Called when the user taps Cancel or swipes the sheet away.
 * @param modifier  Optional modifier for the sheet's root content column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePreviewPanel(
    edit: WorkspaceEdit,
    onApply: (WorkspaceEdit) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track expanded state per URI — false = collapsed (header only).
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = modifier
                .testTag("rename-preview-panel")
                .fillMaxWidth(),
        ) {
            // --- Header ---
            Text(
                text = "Rename Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider()

            // --- Per-file rows ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(edit.changes.entries.toList(), key = { it.key }) { (uri, edits) ->
                    val isExpanded = expanded[uri] == true
                    RenameFileRow(
                        uri = uri,
                        edits = edits,
                        isExpanded = isExpanded,
                        onToggle = { expanded[uri] = !isExpanded },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            HorizontalDivider()

            // --- Footer buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onApply(edit) },
                    modifier = Modifier.testTag("rename-apply"),
                ) {
                    Text("Apply")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("rename-cancel"),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal composables
// ---------------------------------------------------------------------------

/**
 * One collapsible row per file in the workspace edit.
 *
 * Collapsed: shows filename and edit count.
 * Expanded: also shows before→after for each [TextEdit].
 */
@Composable
private fun RenameFileRow(
    uri: String,
    edits: List<TextEdit>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val filename = uri.trimEnd('/').substringAfterLast('/').ifBlank { uri }
    val editCount = edits.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rename-file-$uri"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = filename,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "($editCount ${if (editCount == 1) "edit" else "edits"})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isExpanded) {
            edits.forEach { edit ->
                RenameEditDiffRow(textEdit = edit)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Shows offset range and new text for a single [TextEdit] in diff style.
 */
@Composable
private fun RenameEditDiffRow(textEdit: TextEdit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Text(
            text = "range: [${textEdit.range.first}, ${textEdit.range.last}]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "→ \"${textEdit.newText}\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
        )
    }
}
