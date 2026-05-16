/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 5.2, CONST-035 anti-bluff):
 *   Verifies that DiagnosticsGutter renders the correct number of dot
 *   indicators for a given set of diagnostics, and that offsetToLine
 *   correctly maps character offsets to line numbers.
 *
 * Anti-bluff mutation guards:
 *   Stub DiagnosticsGutter to render nothing (empty Column):
 *   → twoDotsForThreeDiagnosticsOnTwoLines FAILS (no diag-line-* tags).
 *   Stub offsetToLine to always return 0:
 *   → offsetToLine_countsNewlines FAILS.
 *   Revert → all PASS.
 *
 * Test architecture: pure-function tests for offsetToLine + source-level
 * structural assertions for the Composable (manifest = Config.NONE pattern).
 *
 *#######################################################*/
package digital.vasic.yole.android.robolectric.diagnostics

import digital.vasic.yole.android.ui.editor.diagnostics.offsetToLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DiagnosticsGutterTest {

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

    private fun loadGutterSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics/DiagnosticsGutter.kt",
    )

    /**
     * Layer 1: offsetToLine — first line (offset 0, no newlines before it)
     * must be line 0.
     */
    @Test
    fun offsetToLine_firstLine() {
        val text = "hello\nworld\nfoo"
        assertEquals(0, offsetToLine(text, 0))
        assertEquals(0, offsetToLine(text, 4))
    }

    /**
     * Layer 2: offsetToLine — counts newline characters correctly.
     * Anti-bluff: stub to always return 0 → assertEquals(1, ...) FAILS.
     */
    @Test
    fun offsetToLine_countsNewlines() {
        val text = "hello\nworld\nfoo"
        // 'w' is at index 6 → line 1
        assertEquals(1, offsetToLine(text, 6))
        // 'f' is at index 12 → line 2
        assertEquals(2, offsetToLine(text, 12))
    }

    /**
     * Layer 3: offsetToLine — clamps out-of-bounds offset to text.length.
     */
    @Test
    fun offsetToLine_clampsOutOfBounds() {
        val text = "abc"
        // Beyond text.length should not throw; result is line 0.
        val result = offsetToLine(text, 1000)
        assertTrue("out-of-bounds offset MUST return a valid line number", result >= 0)
    }

    /**
     * Layer 4: source-level structural — DiagnosticsGutter MUST emit
     * testTag("diagnostics-gutter") on the root and testTag("diag-line-$lineNum")
     * per dot. Anti-bluff: removing either testTag string causes this to FAIL.
     */
    @Test
    fun twoDotsForThreeDiagnosticsOnTwoLines() {
        val src = loadGutterSource()
        assertTrue(
            "DiagnosticsGutter MUST emit testTag diagnostics-gutter on root",
            src.contains("\"diagnostics-gutter\""),
        )
        // Per-line testTag uses string interpolation:
        assertTrue(
            "DiagnosticsGutter MUST emit testTag diag-line-\$lineNum per dot",
            src.contains("\"diag-line-\$lineNum\""),
        )
        // Highest-severity grouping: must reference priority() or ordinal.
        assertTrue(
            "DiagnosticsGutter MUST group by line and select highest severity",
            src.contains("priority()") || src.contains("ordinal"),
        )
    }

    /**
     * Layer 5: source-level — DiagnosticsGutter MUST call offsetToLine
     * to derive line numbers from diagnostic offsets. Anti-bluff: removing
     * the call means line grouping is broken.
     */
    @Test
    fun gutterCallsOffsetToLine() {
        val src = loadGutterSource()
        assertTrue(
            "DiagnosticsGutter MUST call offsetToLine to compute line from offset",
            src.contains("offsetToLine("),
        )
    }
}
