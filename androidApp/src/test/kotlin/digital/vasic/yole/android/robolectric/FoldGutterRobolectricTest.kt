/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-58 Feature 2 Phase 5, CONST-035 anti-bluff):
 *   Verifies that the FoldGutter composable renders a chevron icon on
 *   lines that start a [FoldRange] returned by FoldQueryRunner, and
 *   that the toggle-fold helper mutates the editor's session fold-set
 *   correctly.
 *
 *   Test architecture (matches iter-57 EditorHighlightingRobolectricTest
 *   pattern): pure source-level + pure-function assertions, NOT
 *   createComposeRule. createComposeRule() needs an Activity which
 *   the project's `manifest = Config.NONE` runs do not provide. The
 *   source-level structural test is the load-bearing anti-bluff anchor
 *   for the runtime path (the Android NDK tree-sitter .so does load
 *   in the dedicated `robolectric-test` container; outside that
 *   container, the engine fails Result.failure and the editor falls
 *   back to plain text per spec §4).
 *
 *   Anti-bluff mutation guards:
 *     - Stubbing FoldQueryRunner.foldRangesFor to `return emptyList()`
 *       in rememberFoldRanges MUST fail `rememberFoldRangesCallsFoldQueryRunner`
 *       because the regex anchor would not match.
 *     - Removing the FoldGutter call from SyncedScrollEditor's gutter
 *       Column MUST fail `foldGutterWiredIntoGutter`.
 *     - Replacing the chevron testTag prefix
 *       (`foldGutter.chevron:line$lineNumber`) MUST fail
 *       `foldGutterEmitsPerLineChevronTestTag`.
 *     - Stubbing toggleFold to no-op MUST fail
 *       `toggleFoldAddsAndRemoves`.
 *     - Stubbing toggleFold to always add (never remove) MUST fail
 *       `toggleFoldAddsAndRemoves` (the "after re-toggle, a MUST be
 *       removed" assertion).
 *     - Removing the chevron-flip-on-folded branch (always rendering
 *       KeyboardArrowDown) MUST fail
 *       `foldGutterFlipsChevronWhenFolded`.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.runtime.mutableStateOf
import digital.vasic.yole.android.ui.editor.toggleFold
import digital.vasic.yole.language.affordance.FoldRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FoldGutterRobolectricTest {

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadFoldGutterSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/FoldGutter.kt"
    )

    private fun loadEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
    )

    /**
     * Layer 1: source-level structural guarantee — SyncedScrollEditor
     * wires FoldGutter + rememberFoldRanges + a foldedRanges
     * MutableState<Set<FoldRange>>. Mutation guard: deleting the
     * FoldGutter call defeats Phase 5 shipping; this assertion
     * catches it deterministically without depending on
     * tree-sitter-android runtime load.
     */
    @Test
    fun foldGutterWiredIntoGutter() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST call FoldGutter(",
            src.contains("FoldGutter("),
        )
        assertTrue(
            "SyncedScrollEditor MUST consume rememberFoldRanges",
            src.contains("rememberFoldRanges("),
        )
        assertTrue(
            "SyncedScrollEditor MUST maintain a foldedRanges MutableState",
            Regex("""foldedRanges\s*=\s*remember""").containsMatchIn(src),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept a tokenizerEngine parameter",
            Regex("""tokenizerEngine:\s*TokenizerEngine\?""").containsMatchIn(src),
        )
    }

    /**
     * Layer 2: rememberFoldRanges MUST call FoldQueryRunner.foldRangesFor.
     * Mutation guard: stubbing the call to `return emptyList()` makes
     * this fail — the test is the load-bearing anti-bluff anchor.
     */
    @Test
    fun rememberFoldRangesCallsFoldQueryRunner() {
        val src = loadFoldGutterSource()
        assertTrue(
            "FoldGutter.kt MUST call runner.foldRangesFor",
            src.contains("runner.foldRangesFor("),
        )
        assertTrue(
            "FoldGutter.kt MUST construct a FoldQueryRunner()",
            src.contains("FoldQueryRunner()"),
        )
        // The happy-path assignment must remain inside the try block so
        // the failure path produces emptyList() rather than crashing.
        assertTrue(
            "FoldGutter MUST assign runner.foldRangesFor result to state.value",
            Regex("""state\.value\s*=\s*try\s*\{[\s\S]*?runner\.foldRangesFor\(""")
                .containsMatchIn(src),
        )
    }

    /**
     * Layer 3: FoldGutter emits per-line chevron testTag
     * `foldGutter.chevron:line{lineNumber}`. Mutation guard: changing
     * the testTag prefix or removing the line-number suffix breaks
     * runtime UI-test selection of fold affordances.
     */
    @Test
    fun foldGutterEmitsPerLineChevronTestTag() {
        val src = loadFoldGutterSource()
        // The testTag is a Kotlin string template `foldGutter.chevron:line$lineNumber`.
        // We assert the literal prefix appears followed by the
        // `$lineNumber` interpolation marker.
        val expected = "foldGutter.chevron:line\$lineNumber"
        assertTrue(
            "FoldGutter MUST emit testTag with prefix `foldGutter.chevron:line\$lineNumber` (got source not containing it)",
            src.contains(expected),
        )
        assertTrue(
            "FoldGutter MUST invoke testTag() with the chevron tag",
            Regex("""testTag\(\s*["']foldGutter\.chevron:line""")
                .containsMatchIn(src),
        )
    }

    /**
     * Layer 4: the chevron MUST flip between KeyboardArrowDown
     * (expanded) and KeyboardArrowRight (collapsed). Removing the
     * fold-state branch and always rendering a single icon would
     * defeat the visible affordance of "I am collapsed / I am
     * expanded". The structural check ensures both icons appear in
     * the source.
     */
    @Test
    fun foldGutterFlipsChevronWhenFolded() {
        val src = loadFoldGutterSource()
        assertTrue(
            "FoldGutter MUST reference KeyboardArrowDown for the expanded state",
            src.contains("KeyboardArrowDown"),
        )
        assertTrue(
            "FoldGutter MUST reference KeyboardArrowRight for the collapsed state",
            src.contains("KeyboardArrowRight"),
        )
        // The branch decision MUST depend on whether the range is in
        // the foldedRanges set (per-fold-state, not a constant).
        assertTrue(
            "FoldGutter MUST select the chevron via `if (isFolded)` (or equivalent)",
            Regex("""if\s*\(\s*isFolded\s*\)""").containsMatchIn(src),
        )
        assertTrue(
            "FoldGutter MUST derive isFolded from `matching in foldedRanges`",
            Regex("""matching\s+in\s+foldedRanges""").containsMatchIn(src),
        )
    }

    /**
     * Layer 5: pure-function unit test for `toggleFold`. The helper
     * MUST add a range that is not yet folded and remove a range that
     * already is. Mutation guard: a no-op stub fails the first
     * assertion; an always-add stub fails the third.
     */
    @Test
    fun toggleFoldAddsAndRemoves() {
        val a = FoldRange(startLine = 0, endLine = 1, startByte = 0, endByte = 1)
        val b = FoldRange(startLine = 2, endLine = 3, startByte = 2, endByte = 3)
        val state = mutableStateOf<Set<FoldRange>>(emptySet())

        toggleFold(state, a)
        assertTrue("after first toggle, a MUST be present", a in state.value)
        assertEquals(1, state.value.size)

        toggleFold(state, b)
        assertTrue("after second toggle, b MUST be present", b in state.value)
        assertEquals(2, state.value.size)

        toggleFold(state, a)
        assertTrue("after re-toggle, a MUST be removed", a !in state.value)
        assertTrue("b MUST still be present", b in state.value)
        assertEquals(1, state.value.size)
    }

    /**
     * Layer 6: anti-bluff guard for the no-FoldRange-on-this-line
     * branch. The FoldGutter MUST short-circuit to a reserved-space
     * Box when no matching range exists, NOT render the chevron. A
     * stub that always renders the chevron regardless of the matching
     * lookup fails this structural check.
     */
    @Test
    fun foldGutterShortCircuitsWhenNoMatchingRange() {
        val src = loadFoldGutterSource()
        assertTrue(
            "FoldGutter MUST search `ranges.firstOrNull { it.startLine == lineNumber - 1 }`",
            Regex("""ranges\.firstOrNull\s*\{[\s\S]{0,60}startLine\s*==\s*lineNumber\s*-\s*1""")
                .containsMatchIn(src),
        )
        assertTrue(
            "FoldGutter MUST short-circuit (early return) on no-match",
            Regex("""if\s*\(\s*matching\s*==\s*null\s*\)""").containsMatchIn(src),
        )
    }
}
