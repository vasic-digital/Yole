/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 1: tests for TextEdit (3), WorkspaceEdit (2),
 * WorkspaceEditApplier (4) — 9 total.
 *
 * Mutation evidence (CONST-035):
 *
 *   TextEdit — stub apply() to return text unchanged:
 *     applyEdit_replacesMiddle       → FAIL (result unchanged, expected replaced)
 *     applyEdit_clampsBeyondEnd      → FAIL (no newText appended)
 *     applyEdit_emptyRange_inserts   → FAIL (insertion not performed)
 *
 *   WorkspaceEdit — change isEmpty to always return false:
 *     isEmpty_trueWhen_noEdits       → FAIL (asserts true, gets false)
 *     isEmpty_falseWhen_editsPresent → PASS (false == false; unaffected)
 *
 *   WorkspaceEditApplier — stub apply() to return sources unchanged:
 *     singleFile_singleEdit          → FAIL (text unchanged)
 *     multiFile_editsBothFiles       → FAIL (both files unchanged)
 *     conflict_throwsApplyConflict   → FAIL (no exception thrown)
 *     nonExistentUri_skipped         → PASS (sources returned either way)
 *
 * JUnit4 + plain assertions (no runBlocking — all logic is synchronous).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// TextEdit tests (3)
// ---------------------------------------------------------------------------

class TextEditTests {

    @Test
    fun applyEdit_replacesMiddle() {
        // "Hello, World!" → replace "World" (7..11) with "Kotlin"
        val edit = TextEdit(range = 7..11, newText = "Kotlin")
        val result = edit.apply("Hello, World!")
        assertEquals("Hello, Kotlin!", result)
    }

    @Test
    fun applyEdit_clampsBeyondEnd() {
        // Range extends past the end of the string — must be clamped.
        val text = "abc"
        val edit = TextEdit(range = 2..999, newText = "XYZ")
        val result = edit.apply(text)
        assertEquals("abXYZ", result)
    }

    @Test
    fun applyEdit_emptyRange_inserts() {
        // An empty range (start == last + 1 after +1) acts as insertion.
        // range = 3..2 → start=3, end=3 (no chars consumed) → insert at 3.
        val edit = TextEdit(range = 3..2, newText = "---")
        val result = edit.apply("abcdef")
        assertEquals("abc---def", result)
    }
}

// ---------------------------------------------------------------------------
// WorkspaceEdit tests (2)
// ---------------------------------------------------------------------------

class WorkspaceEditTests {

    @Test
    fun isEmpty_trueWhen_noEdits() {
        val emptyByMap = WorkspaceEdit(emptyMap())
        assertTrue(emptyByMap.isEmpty)

        val emptyByLists = WorkspaceEdit(mapOf("file:///a.kt" to emptyList()))
        assertTrue(emptyByLists.isEmpty)
    }

    @Test
    fun isEmpty_falseWhen_editsPresent() {
        val edit = WorkspaceEdit(
            mapOf("file:///a.kt" to listOf(TextEdit(0..2, "new")))
        )
        assertFalse(edit.isEmpty)
    }
}

// ---------------------------------------------------------------------------
// WorkspaceEditApplier tests (4)
// ---------------------------------------------------------------------------

class WorkspaceEditApplierTests {

    @Test
    fun singleFile_singleEdit() {
        val uri = "file:///main.kt"
        val sources = mapOf(uri to "val x = 1")
        val edit = WorkspaceEdit(mapOf(uri to listOf(TextEdit(4..4, "y"))))
        val result = WorkspaceEditApplier.apply(edit, sources)
        assertEquals("val y = 1", result[uri])
    }

    @Test
    fun multiFile_editsBothFiles() {
        val uriA = "file:///a.kt"
        val uriB = "file:///b.kt"
        val sources = mapOf(uriA to "foo", uriB to "bar")
        val edit = WorkspaceEdit(
            mapOf(
                uriA to listOf(TextEdit(0..2, "FOO")),
                uriB to listOf(TextEdit(0..2, "BAR"))
            )
        )
        val result = WorkspaceEditApplier.apply(edit, sources)
        assertEquals("FOO", result[uriA])
        assertEquals("BAR", result[uriB])
    }

    @Test
    fun conflict_throwsApplyConflict() {
        val uri = "file:///conflict.kt"
        val sources = mapOf(uri to "Hello World")
        // Ranges 0..5 and 3..7 overlap.
        val edit = WorkspaceEdit(
            mapOf(
                uri to listOf(
                    TextEdit(0..5, "A"),
                    TextEdit(3..7, "B")
                )
            )
        )
        assertFailsWith<WorkspaceEditApplier.ApplyConflict> {
            WorkspaceEditApplier.apply(edit, sources)
        }
    }

    @Test
    fun nonExistentUri_skipped() {
        val knownUri = "file:///known.kt"
        val unknownUri = "file:///unknown.kt"
        val sources = mapOf(knownUri to "original")
        val edit = WorkspaceEdit(
            mapOf(unknownUri to listOf(TextEdit(0..0, "X")))
        )
        val result = WorkspaceEditApplier.apply(edit, sources)
        // knownUri preserved, unknownUri NOT added to result.
        assertEquals("original", result[knownUri])
        assertFalse(result.containsKey(unknownUri))
    }
}
