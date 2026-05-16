/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 2: DefinitionLocation — forward-declared data class.
 *
 * Phase 3 finalizes LSP4J→DefinitionLocation mapping (Location vs
 * LocationLink) and true range computation via LspRangeMapping.
 * Phase 2 uses range = 0..0 as a placeholder so callers compile.
 *
 * Cross-platform (CONST-037):
 *   - Desktop/Android: populated by LspServerHost.definition() JVM actual.
 *   - iOS/Wasm:        definition() returns emptyList(); never instantiated.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * A single definition target returned by [LspServerHost.definition].
 *
 * @param uri   Document URI of the file containing the definition.
 * @param range Byte/character range within the document. Phase 2 sets
 *              this to [0..0] as a placeholder; Phase 3 LspRangeMapping
 *              resolves accurate line/column offsets.
 */
data class DefinitionLocation(val uri: String, val range: IntRange)
