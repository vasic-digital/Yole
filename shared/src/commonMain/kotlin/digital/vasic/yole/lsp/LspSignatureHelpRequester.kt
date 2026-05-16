/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspSignatureHelpRequester — testability interface for signature help.
 *
 * [SignatureHelpTrigger] (Phase 7) cannot depend directly on [LspServerHost]
 * in commonTest because LspServerHost is an expect class whose JVM actual is
 * not open/mockable without MockK (JVM-only). This thin interface provides a
 * stable seam so tests supply a simple fake implementation without needing
 * MockK or the full server lifecycle.
 *
 * Production wiring (Phase 7, SignatureHelpTrigger):
 *   val requester = object : LspSignatureHelpRequester {
 *       override suspend fun signatureHelp(langId, uri, line, character) =
 *           host.signatureHelp(langId, uri, line, character)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs — runs on all targets.
 *   - Desktop/Android: JVM actual wires LspServerHost.signatureHelp().
 *   - iOS/Wasm:        LspServerHost.signatureHelp() returns null.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.signatureHelp] to enable testability of
 * [SignatureHelpTrigger] without requiring a full JVM-actual LspServerHost.
 *
 * Mirrors the [LspDefinitionRequester] pattern introduced in iter-62 Phase 7.
 */
interface LspSignatureHelpRequester {
    /**
     * Request signature help for the call site at [line] / [character] in
     * the document at [uri] for language [langId].
     *
     * Returns null when there is no applicable call site, when the server is
     * unavailable, or when a timeout occurs.
     */
    suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp?
}
