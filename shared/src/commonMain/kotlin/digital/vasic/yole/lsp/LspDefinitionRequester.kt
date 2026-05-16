/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: LspDefinitionRequester — testability interface.
 *
 * [GoToDefinitionAction] cannot depend directly on [LspServerHost] in
 * commonTest because LspServerHost is an expect class whose JVM actual
 * is not open/mockable without MockK (JVM-only). This thin interface
 * provides a stable seam so tests supply a simple fake implementation
 * without needing MockK or the full server lifecycle.
 *
 * Production wiring (Phase 8, IdeEditorScreen):
 *   val requester = object : LspDefinitionRequester {
 *       override suspend fun definition(langId, uri, line, character) =
 *           host.definition(langId, uri, line, character)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs.
 *   - All targets: fully available. Phase 8 wires each platform's real host.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.definition] to enable testability of
 * [GoToDefinitionAction] without requiring a full JVM-actual LspServerHost.
 *
 * Plan deviation note (iter-62 Phase 7):
 *   The plan originally called for GoToDefinitionAction to accept an
 *   LspServerHost directly. That is infeasible in commonTest because
 *   LspServerHost is an expect class that cannot be subclassed or mocked
 *   in commonMain/commonTest. This interface is the documented clean
 *   deviation improving testability. See commit message for rationale.
 */
interface LspDefinitionRequester {
    /**
     * Request go-to-definition locations for the symbol at [line] / [character]
     * inside the document at [documentUri] for language [langId].
     *
     * Returns an empty list on any error (mirrors [LspServerHost.definition] contract).
     */
    suspend fun definition(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): List<DefinitionLocation>
}
