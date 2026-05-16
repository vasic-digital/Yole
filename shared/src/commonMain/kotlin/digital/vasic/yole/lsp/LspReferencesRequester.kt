/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspReferencesRequester — testability interface for find-references.
 *
 * [FindReferencesAction] (Phase 9) cannot depend directly on [LspServerHost]
 * in commonTest because LspServerHost is an expect class whose JVM actual is
 * not open/mockable without MockK (JVM-only). This thin interface provides a
 * stable seam so tests supply a simple fake implementation without needing
 * MockK or the full server lifecycle.
 *
 * Production wiring (Phase 9, FindReferencesAction):
 *   val requester = object : LspReferencesRequester {
 *       override suspend fun references(langId, uri, line, character, includeDeclaration) =
 *           host.references(langId, uri, line, character, includeDeclaration)
 *   }
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure interface, no platform APIs — runs on all targets.
 *   - Desktop/Android: JVM actual wires LspServerHost.references().
 *   - iOS/Wasm:        LspServerHost.references() returns emptyList().
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Thin interface over [LspServerHost.references] to enable testability of
 * [FindReferencesAction] without requiring a full JVM-actual LspServerHost.
 *
 * Mirrors the [LspDefinitionRequester] pattern introduced in iter-62 Phase 7.
 *
 * Note: [ReferenceLocation] is a typealias for [DefinitionLocation] (Phase 3),
 * so results integrate transparently with [EditorNavigationStack] and openFileAt.
 */
interface LspReferencesRequester {
    /**
     * Request all references to the symbol at [line] / [character] in the
     * document at [uri] for language [langId].
     *
     * @param includeDeclaration Whether to include the declaration site in results.
     *
     * Returns an empty list when the server reports no references, when the
     * server is unavailable, or when a timeout occurs.
     */
    suspend fun references(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean = true,
    ): List<ReferenceLocation>
}
