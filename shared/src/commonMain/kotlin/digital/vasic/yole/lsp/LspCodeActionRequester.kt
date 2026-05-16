/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspCodeActionRequester — testability interface for code actions.
 *
 * [CodeActionLightbulb] and [CodeActionInvoker] (Phase 6) cannot depend
 * directly on [LspServerHost] in commonTest because LspServerHost is an
 * expect class whose JVM actual is not open/mockable without MockK (JVM-only).
 * This thin interface provides a stable seam so tests supply a simple fake
 * implementation without needing MockK or the full server lifecycle.
 *
 * Production wiring (Phase 6, CodeActionInvoker):
 *   val requester = object : LspCodeActionRequester {
 *       override suspend fun codeActions(langId, uri, range) =
 *           host.codeActions(langId, uri, range)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs — runs on all targets.
 *   - Desktop/Android: JVM actual wires LspServerHost.codeActions().
 *   - iOS/Wasm:        LspServerHost.codeActions() returns emptyList().
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.codeActions] to enable testability of
 * [CodeActionLightbulb] and [CodeActionInvoker] without requiring a full
 * JVM-actual LspServerHost.
 *
 * Mirrors the [LspDefinitionRequester] pattern introduced in iter-62 Phase 7.
 */
interface LspCodeActionRequester {
    /**
     * Request available code actions for the given [range] in the document
     * at [uri] for language [langId].
     *
     * Returns an empty list when the server reports no applicable actions,
     * when the server is unavailable, or when a timeout occurs.
     */
    suspend fun codeActions(
        langId: String,
        uri: String,
        range: IntRange,
    ): List<CodeAction>
}
