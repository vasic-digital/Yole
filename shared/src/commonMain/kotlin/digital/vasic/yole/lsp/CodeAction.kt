/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 2: CodeAction — forward-declared placeholder data class.
 *
 * Phase 3 finalizes LSP4J Either<Command, CodeAction> mapping via
 * mapLspCodeAction(). Phase 2 defines the type so LspServerHost.codeActions()
 * compiles on all targets.
 *
 * Cross-platform (CONST-037):
 *   - Desktop/Android: populated by LspServerHost.codeActions() JVM actual.
 *   - iOS/Wasm:        codeActions() returns emptyList(); never instantiated.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * A single code-action entry returned by [LspServerHost.codeActions].
 *
 * @param title   Human-readable label shown in the lightbulb menu.
 * @param kind    LSP action kind string (e.g. "quickfix", "refactor.rename").
 *                Null when the server omits it.
 * @param edit    Workspace edit to apply when the action is invoked.
 *                Null when the action is command-based.
 * @param command Server-side command identifier. Null when edit-based.
 */
data class CodeAction(
    val title: String,
    val kind: String?,
    val edit: WorkspaceEdit?,
    val command: String?,
)
