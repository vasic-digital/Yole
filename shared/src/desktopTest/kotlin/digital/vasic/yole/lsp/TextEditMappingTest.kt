/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 3: TextEditMappingTest — unit tests for
 * mapLspTextEditWithDoc and mapLspTextEdits internal helpers.
 *
 * Exercises the iter-62 LspRangeMapping integration: LSP (line, col) pairs
 * must be converted to absolute character offsets in the document text.
 *
 * Three cases per Phase 3 plan:
 *   1. Single edit on a multi-line document — real line/col → offset.
 *   2. Multi-edit batch — order is preserved.
 *   3. Out-of-bounds range — clamped to document length.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub mapLspTextEditWithDoc to always return TextEdit(0..0, "").
 *      → singleEdit_realRange FAILS (range != 0..0 expected).
 *      → multiEdit_preservesOrder FAILS (ranges differ from 0..0).
 *   2. Stub mapLspTextEdits to return emptyList().
 *      → outOfBounds_clamps FAILS (expected size 1, got 0).
 *   3. Revert; confirm all 3 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): JVM helpers under test; live here.
 *   - Android: mapLspTextEditWithDocAndroid mirrors logic; covered by androidUnitTest.
 *   - iOS/Wasm: formatting() returns emptyList() stubs; no mapping occurs.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit as LspTextEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [mapLspTextEditWithDoc] and [mapLspTextEdits].
 *
 * The document used across tests:
 * ```
 * line 0: "fun foo() {\n"   (12 chars, \n at index 11)
 * line 1: "    return 42\n" (14 chars, \n at index 25)
 * line 2: "}"               (1 char, index 26)
 * ```
 * Total: 27 chars.
 */
class TextEditMappingTest {

    private val docText = "fun foo() {\n    return 42\n}"

    /**
     * A single LSP TextEdit on line 1, col 11-13 (selecting "42") must be
     * mapped to the absolute offset range 23..25 in the document text.
     *
     * LspRangeMapping.lineColToOffset("fun foo() {\n    return 42\n}", 1, 11) = 23.
     * LspRangeMapping.lineColToOffset("fun foo() {\n    return 42\n}", 1, 13) = 25.
     *
     * Mutation: return 0..0 → assertEquals(23..25 , ...) FAILS.
     */
    @Test
    fun singleEdit_realRange() {
        // "    return 42\n"  — "42" starts at col 11 of line 1.
        // line 1 starts at absolute offset 12 (after the \n on line 0).
        // col 11 → offset 12 + 11 = 23. col 13 → offset 12 + 13 = 25.
        val lspEdit = LspTextEdit(
            Range(Position(1, 11), Position(1, 13)),
            "99",
        )
        val result = mapLspTextEditWithDoc(lspEdit, docText)
        assertEquals("99", result.newText, "newText must be preserved")
        assertEquals(23, result.range.first, "start offset must be 23 (line 1 col 11)")
        assertEquals(25, result.range.last, "end offset must be 25 (line 1 col 13)")
    }

    /**
     * A batch of two edits must be mapped in-order, both with real offsets.
     *
     * Edit 1: line 0 col 4-7 ("foo") → absolute 4..7.
     * Edit 2: line 2 col 0-1 ("}") → absolute 26..27.
     *
     * Mutation: return emptyList() or wrong offsets → FAILS.
     */
    @Test
    fun multiEdit_preservesOrder() {
        val edit1 = LspTextEdit(Range(Position(0, 4), Position(0, 7)), "bar")
        val edit2 = LspTextEdit(Range(Position(2, 0), Position(2, 1)), "end")

        val results = mapLspTextEdits(listOf(edit1, edit2), docText)

        assertEquals(2, results.size, "Expected 2 mapped edits")
        // edit1: line 0 starts at 0; col 4 → offset 4; col 7 → offset 7
        assertEquals("bar", results[0].newText)
        assertEquals(4, results[0].range.first, "edit1 start must be 4")
        assertEquals(7, results[0].range.last, "edit1 end must be 7")
        // edit2: line 2 starts at 26; col 0 → 26; col 1 → 27
        assertEquals("end", results[1].newText)
        assertEquals(26, results[1].range.first, "edit2 start must be 26")
        assertEquals(27, results[1].range.last, "edit2 end must be 27")
    }

    /**
     * An LSP edit with (line, col) beyond the document boundaries must be
     * clamped to [docText.length] — never throw and never produce a negative range.
     *
     * Mutation: throw instead of clamp → FAILS with exception.
     */
    @Test
    fun outOfBounds_clamps() {
        val lspEdit = LspTextEdit(
            Range(Position(999, 999), Position(999, 999)),
            "appended",
        )
        val results = mapLspTextEdits(listOf(lspEdit), docText)
        assertEquals(1, results.size, "Expected 1 clamped edit")
        val edit = results[0]
        // Both start and end must clamp to docText.length = 27.
        assertTrue(edit.range.first <= docText.length, "start must not exceed docText.length")
        assertTrue(edit.range.last <= docText.length, "end must not exceed docText.length")
        assertEquals("appended", edit.newText)
    }
}
