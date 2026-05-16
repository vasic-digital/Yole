/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 6.3: CodeActionInvoker — dispatches a CodeAction to
 * either a workspace-edit applier or a server-side command callback.
 *
 * Dispatch logic:
 *   1. If action.edit != null → apply via WorkspaceEditApplier and
 *      deliver the result to onEdit.
 *   2. Else if action.command != null → pass the command string to
 *      onCommand (caller routes to LspServerHost.executeCommand or a
 *      stub in Phase 8).
 *   3. Otherwise → no-op (server emitted a CodeAction with neither edit
 *      nor command — logged as a warning; we don't bluff a fake result).
 *
 * Anti-bluff (CONST-035):
 *   The Robolectric test confirms that the invoke() call-site passes
 *   action.edit to WorkspaceEditApplier.apply and that onEdit is called
 *   with the result. Stubbing the apply call to return sources unchanged
 *   does NOT cause our structural Robolectric tests to fail (those tests
 *   verify source-level wiring, not runtime output), but the
 *   WorkspaceEditApplierTest in commonTest catches that mutation.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here.
 *   - Desktop: CodeActionInvoker can be shared via commonMain in a future
 *     phase; for now the logic lives only in androidApp.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.codeaction

import digital.vasic.yole.lsp.CodeAction
import digital.vasic.yole.lsp.WorkspaceEditApplier

/**
 * Dispatches a [CodeAction] invocation to the appropriate handler.
 *
 * This object is intentionally free of Compose dependencies so it can be
 * unit-tested and, later, promoted to `commonMain` without restructuring.
 */
object CodeActionInvoker {

    /**
     * Invokes [action] against [sources], routing to [onEdit] or [onCommand].
     *
     * @param action   The [CodeAction] selected by the user.
     * @param sources  Current snapshot of all open file texts, keyed by URI.
     * @param onEdit   Receives the modified sources map after a workspace edit
     *                 is applied.  Only called when [action.edit] is non-null.
     * @param onCommand Receives the command string when [action.command] is
     *                  non-null and [action.edit] is null.
     */
    suspend fun invoke(
        action: CodeAction,
        sources: Map<String, String>,
        onEdit: (Map<String, String>) -> Unit,
        onCommand: (String) -> Unit,
    ) {
        val edit = action.edit
        val command = action.command
        when {
            edit != null -> {
                val result = WorkspaceEditApplier.apply(edit, sources)
                onEdit(result)
            }
            command != null -> {
                onCommand(command)
            }
            else -> {
                // No-op: server emitted a CodeAction with neither edit nor command.
                // This is a valid (if unusual) LSP response — we log and do nothing
                // rather than fabricating a result (CONST-035 anti-bluff).
                @Suppress("NON_EXHAUSTIVE_WHEN_STATEMENT")
                println("CodeActionInvoker: action '${action.title}' has neither edit nor command — ignoring.")
            }
        }
    }
}
