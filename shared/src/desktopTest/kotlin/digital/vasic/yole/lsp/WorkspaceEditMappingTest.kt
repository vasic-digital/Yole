/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 3: WorkspaceEditMappingTest — unit tests for
 * the mapLspWorkspaceEditToYole internal helper.
 *
 * Constructs real LSP4J WorkspaceEdit objects (no mock server) and
 * asserts the exact Yole WorkspaceEdit produced by the mapper. Covers
 * the three specification cases from Phase 0 §2 research:
 *   1. documentChanges only (modern LSP 3.18 path).
 *   2. legacy changes only (old-style map path).
 *   3. both fields populated → documentChanges wins.
 *
 * Mutation procedure (CONST-035):
 *   1. Swap preference in mapLspWorkspaceEditToYole: check `changes` first.
 *      → documentChanges_preferredOver_legacyChanges FAILS.
 *   2. Remove the documentChanges branch (always use legacy path).
 *      → documentChanges_only_mapsEdits FAILS.
 *   3. Revert; confirm all 3 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): JVM actual helper under test; lives here.
 *   - Android: mapLspWorkspaceEditToYoleAndroid mirrors logic; covered by androidUnitTest.
 *   - iOS/Wasm: WorkspaceEdit stubs return null/empty; no mapping occurs.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import org.eclipse.lsp4j.ResourceOperation
import org.eclipse.lsp4j.SnippetTextEdit
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.TextEdit as LspTextEdit
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [mapLspWorkspaceEditToYole].
 *
 * Each test constructs a real LSP4J [org.eclipse.lsp4j.WorkspaceEdit] and
 * asserts the URI-keyed [WorkspaceEdit.changes] produced by the mapper.
 * Structural assertions on URIs, edit counts, and newText strings ensure
 * a stub returning an empty or wrong WorkspaceEdit causes failures.
 */
class WorkspaceEditMappingTest {

    /**
     * When LSP4J WorkspaceEdit carries only `documentChanges` (modern path),
     * the mapper MUST produce a [WorkspaceEdit] whose `changes` map contains
     * every URI from the [TextDocumentEdit] entries, with correctly mapped edits.
     *
     * Mutation: strip documentChanges branch → empty changes map → FAILS.
     */
    @Test
    fun documentChanges_only_mapsEdits() {
        val lspEdit = buildLspWorkspaceEditWithDocChanges(
            uri = "file:///project/Foo.kt",
            edits = listOf(lspTextEdit(line = 0, col = 0, endLine = 0, endCol = 3, newText = "bar")),
        )
        val result = mapLspWorkspaceEditToYole(lspEdit)
        assertEquals(1, result.changes.size, "Expected 1 URI in changes")
        assertTrue(result.changes.containsKey("file:///project/Foo.kt"), "URI must be preserved")
        val edits = result.changes["file:///project/Foo.kt"]!!
        assertEquals(1, edits.size, "Expected 1 edit")
        assertEquals("bar", edits[0].newText, "newText must be 'bar'")
    }

    /**
     * When LSP4J WorkspaceEdit carries only the legacy `changes` map (no
     * `documentChanges`), the mapper MUST use that map and preserve all URIs
     * and edit texts.
     *
     * Mutation: remove legacy-changes path → empty changes → FAILS.
     */
    @Test
    fun legacyChanges_only_mapsEdits() {
        val lspEdit = org.eclipse.lsp4j.WorkspaceEdit()
        // documentChanges intentionally left null; only legacy changes set.
        lspEdit.changes = mapOf(
            "file:///project/Bar.kt" to listOf(
                lspTextEdit(0, 0, 0, 5, "newName"),
                lspTextEdit(2, 4, 2, 9, "newName"),
            ),
        )
        val result = mapLspWorkspaceEditToYole(lspEdit)
        assertEquals(1, result.changes.size, "Expected 1 URI in legacy changes")
        val edits = result.changes["file:///project/Bar.kt"]!!
        assertEquals(2, edits.size, "Expected 2 edits from legacy map")
        assertEquals("newName", edits[0].newText)
        assertEquals("newName", edits[1].newText)
    }

    /**
     * When both `documentChanges` and `changes` are non-empty, the mapper MUST
     * prefer `documentChanges` per LSP 3.18 §3.17.11 ("if documentChanges are
     * present they are preferred over changes").
     *
     * Mutation: swap preference (check changes first) → result contains "legacy-uri"
     *           instead of "modern-uri" → FAILS.
     */
    @Test
    fun documentChanges_preferredOver_legacyChanges() {
        val lspEdit = buildLspWorkspaceEditWithDocChanges(
            uri = "file:///modern-uri.kt",
            edits = listOf(lspTextEdit(0, 0, 0, 1, "MODERN")),
        )
        // Also populate legacy changes with a different URI.
        lspEdit.changes = mapOf(
            "file:///legacy-uri.kt" to listOf(lspTextEdit(0, 0, 0, 1, "LEGACY")),
        )
        val result = mapLspWorkspaceEditToYole(lspEdit)
        // documentChanges MUST win.
        assertTrue(
            result.changes.containsKey("file:///modern-uri.kt"),
            "documentChanges URI must be present; got: ${result.changes.keys}",
        )
        assertTrue(
            !result.changes.containsKey("file:///legacy-uri.kt"),
            "legacy URI must NOT be present when documentChanges is non-empty",
        )
        assertEquals("MODERN", result.changes["file:///modern-uri.kt"]!![0].newText)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun lspTextEdit(line: Int, col: Int, endLine: Int, endCol: Int, newText: String): LspTextEdit =
        LspTextEdit(Range(Position(line, col), Position(endLine, endCol)), newText)

    /**
     * Build an LSP4J [org.eclipse.lsp4j.WorkspaceEdit] with a single
     * [TextDocumentEdit] entry in `documentChanges`.
     *
     * LSP4J 1.0.0: TextDocumentEdit.edits is List<Either<TextEdit, SnippetTextEdit>>.
     */
    private fun buildLspWorkspaceEditWithDocChanges(
        uri: String,
        edits: List<LspTextEdit>,
    ): org.eclipse.lsp4j.WorkspaceEdit {
        val versionedId = VersionedTextDocumentIdentifier(uri, 1)
        // Wrap each plain TextEdit in Either.forLeft for LSP4J 1.0.0 compatibility.
        val wrappedEdits: List<Either<LspTextEdit, SnippetTextEdit>> =
            edits.map { Either.forLeft(it) }
        val tde = TextDocumentEdit(versionedId, wrappedEdits)
        val either: Either<TextDocumentEdit, ResourceOperation> = Either.forLeft(tde)
        return org.eclipse.lsp4j.WorkspaceEdit(listOf(either))
    }
}
