/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspRenameRequester — testability interface for rename.
 *
 * [RenameAction] (Phase 5) cannot depend directly on [LspServerHost] in
 * commonTest because LspServerHost is an expect class whose JVM actual
 * is not open/mockable without MockK (JVM-only). This thin interface
 * provides a stable seam so tests supply a simple fake implementation
 * without needing MockK or the full server lifecycle.
 *
 * Production wiring (Phase 5, RenameAction):
 *   val requester = object : LspRenameRequester {
 *       override suspend fun rename(langId, uri, line, character, newName) =
 *           host.rename(langId, uri, line, character, newName)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs — runs on all targets.
 *   - Desktop/Android: JVM actual wires LspServerHost.rename().
 *   - iOS/Wasm:        LspServerHost.rename() returns null; interface reuses stub.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.rename] to enable testability of
 * [RenameAction] without requiring a full JVM-actual LspServerHost.
 *
 * Mirrors the [LspDefinitionRequester] pattern introduced in iter-62 Phase 7.
 */
interface LspRenameRequester {
    /**
     * Request a rename refactoring for the symbol at [line] / [character]
     * in the document at [uri] for language [langId], replacing the symbol
     * with [newName].
     *
     * Returns null when the server reports no applicable rename, when the
     * server is unavailable, or when a timeout occurs.
     * Returns an empty [WorkspaceEdit] when the server indicates the rename
     * applies no changes.
     */
    suspend fun rename(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        newName: String,
    ): WorkspaceEdit?
}
