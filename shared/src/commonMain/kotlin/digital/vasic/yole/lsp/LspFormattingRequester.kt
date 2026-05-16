/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspFormattingRequester — testability interface for formatting.
 *
 * [FormattingTrigger] (Phase 8) cannot depend directly on [LspServerHost] in
 * commonTest because LspServerHost is an expect class whose JVM actual is not
 * open/mockable without MockK (JVM-only). This thin interface provides a
 * stable seam so tests supply a simple fake implementation without needing
 * MockK or the full server lifecycle.
 *
 * Production wiring (Phase 8, FormattingTrigger):
 *   val requester = object : LspFormattingRequester {
 *       override suspend fun formatting(langId, uri, indentSize, useSpaces) =
 *           host.formatting(langId, uri, indentSize, useSpaces)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs — runs on all targets.
 *   - Desktop/Android: JVM actual wires LspServerHost.formatting().
 *   - iOS/Wasm:        LspServerHost.formatting() returns emptyList().
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.formatting] to enable testability of
 * [FormattingTrigger] without requiring a full JVM-actual LspServerHost.
 *
 * Mirrors the [LspDefinitionRequester] pattern introduced in iter-62 Phase 7.
 */
interface LspFormattingRequester {
    /**
     * Request full-document formatting for the document at [uri] for language
     * [langId] using the given formatting options.
     *
     * Returns an empty list when no formatting changes are needed, when the
     * server is unavailable, or when a timeout occurs.
     *
     * @param indentSize Number of spaces (or tab stops) per indent level.
     * @param useSpaces  True to use space characters; false to use tab characters.
     */
    suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int = 4,
        useSpaces: Boolean = true,
    ): List<TextEdit>
}
