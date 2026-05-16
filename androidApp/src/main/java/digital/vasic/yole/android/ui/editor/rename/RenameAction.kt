/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 5: RenameAction — orchestrator for LSP rename refactoring.
 *
 * Presents a name-prompt AlertDialog, invokes [LspRenameRequester.rename],
 * and routes the result:
 *   - Non-null, non-empty [WorkspaceEdit] → display [RenamePreviewPanel].
 *   - Null or empty edit → show a Toast ("No rename changes available.").
 *
 * The Composable is stateful: it manages its own AlertDialog and
 * RenamePreviewPanel visibility via local remembered state.
 *
 * Production wiring (Phase 10 IdeEditorScreen integration):
 *   RenameAction(
 *       langId = currentLangId,
 *       uri = currentUri,
 *       line = cursorLine,
 *       character = cursorCharacter,
 *       requester = object : LspRenameRequester {
 *           override suspend fun rename(l, u, ln, ch, n) =
 *               lspServerHost.rename(l, u, ln, ch, n)
 *       },
 *       onApplyEdit = { edit -> WorkspaceEditApplier.apply(edit, openSources) },
 *       onDismiss = { /* hide trigger */ },
 *   )
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — stub Composable body to empty (no AlertDialog, no requester
 *   call, no RenamePreviewPanel). All structural Robolectric tests FAIL.
 *   Reverted; GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation here (AlertDialog + ModalBottomSheet).
 *   - Desktop:  dialog is Compose-based — same pattern; integration in Phase 10.
 *   - iOS:      deferred — UIAlertController variant planned.
 *   - Web:      deferred — dialog variant planned.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.rename

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import digital.vasic.yole.lsp.LspRenameRequester
import digital.vasic.yole.lsp.WorkspaceEdit
import kotlinx.coroutines.launch

/**
 * Orchestrates an LSP rename refactoring:
 *  1. Prompts the user for a new name via an [AlertDialog].
 *  2. On confirm, calls [requester.rename] to obtain a [WorkspaceEdit].
 *  3. Routes to [RenamePreviewPanel] on success or Toast on no-op.
 *
 * @param langId      Language identifier for the LSP server.
 * @param uri         Document URI of the symbol being renamed.
 * @param line        0-based line of the cursor.
 * @param character   0-based character offset of the cursor.
 * @param requester   [LspRenameRequester] used to fetch the edit.
 * @param onApplyEdit Called with the confirmed [WorkspaceEdit] when the user
 *                    taps Apply in the preview panel.
 * @param onDismiss   Called when the dialog or preview panel is dismissed
 *                    without applying changes.
 */
@Composable
fun RenameAction(
    langId: String,
    uri: String,
    line: Int,
    character: Int,
    requester: LspRenameRequester,
    onApplyEdit: (WorkspaceEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newName by remember { mutableStateOf("") }
    var pendingEdit by remember { mutableStateOf<WorkspaceEdit?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    // --- Name-prompt AlertDialog ---
    if (!showPreview) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Rename Symbol") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) return@TextButton
                        scope.launch {
                            val edit = requester.rename(langId, uri, line, character, name)
                            if (edit == null || edit.isEmpty) {
                                Toast.makeText(
                                    context,
                                    "No rename changes available.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onDismiss()
                            } else {
                                pendingEdit = edit
                                showPreview = true
                            }
                        }
                    },
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        )
    }

    // --- Preview panel shown after a successful rename request ---
    val editToPreview = pendingEdit
    if (showPreview && editToPreview != null) {
        RenamePreviewPanel(
            edit = editToPreview,
            onApply = { confirmedEdit ->
                onApplyEdit(confirmedEdit)
                showPreview = false
                onDismiss()
            },
            onDismiss = {
                showPreview = false
                onDismiss()
            },
        )
    }
}
