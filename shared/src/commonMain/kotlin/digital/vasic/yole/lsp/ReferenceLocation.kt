/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 3: ReferenceLocation — typealias for DefinitionLocation.
 *
 * LSP "textDocument/references" returns the same Location type as
 * "textDocument/definition". We model references as DefinitionLocation
 * so that EditorNavigationStack and openFileAt work identically for
 * both go-to-definition and find-references without duplicating code.
 *
 * Cross-platform (CONST-037):
 *   - commonMain: typealias is pure Kotlin; runs on all targets unchanged.
 *   - Desktop/Android: populated by LspServerHost.references() JVM actual.
 *   - iOS/Wasm:        references() returns emptyList(); never instantiated.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * A single reference location returned by [LspServerHost.references].
 *
 * Alias of [DefinitionLocation] because LSP `textDocument/references`
 * returns plain [org.eclipse.lsp4j.Location] objects — the same structure
 * as `textDocument/definition` targets.  Using a typealias means callers
 * work with [ReferenceLocation] in domain contexts while the navigation
 * infrastructure ([EditorNavigationStack], `openFileAt`) accepts the
 * canonical [DefinitionLocation] type transparently.
 */
typealias ReferenceLocation = DefinitionLocation
