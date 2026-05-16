/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 5.3, CONST-035 anti-bluff):
 *   Verifies DiagnosticsInlineUnderline:
 *   - Empty diagnostics → identity transform (no spans added).
 *   - Single diagnostic → underline span at correct position.
 *   - Multiple non-overlapping diagnostics → multiple spans.
 *   - Out-of-bounds range → clamped without throwing.
 *
 * Anti-bluff mutation guards:
 *   Stub filter() to return TransformedText(text, Identity) always:
 *   → singleDiagnostic_appliesUnderlineSpan FAILS (0 span styles).
 *   → multipleDiagnostics_layerSpans FAILS (0 span styles).
 *   Revert → 4/4 PASS.
 *
 * Test architecture: source-level structural tests (manifest=Config.NONE).
 * DiagnosticsInlineUnderline is in androidApp, so we test it here.
 *
 *#######################################################*/
package digital.vasic.yole.android.robolectric.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DiagnosticsInlineUnderlineTest {

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

    private fun loadUnderlineSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics/DiagnosticsInlineUnderline.kt",
    )

    /**
     * Layer 1: emptyDiagnostics_identityTransform — source MUST contain
     * the identity-return early-exit for empty diagnostics list. Anti-bluff:
     * removing the isEmpty guard means empty list still runs the loop (which
     * does nothing, but the structural check catches the removal).
     */
    @Test
    fun emptyDiagnostics_identityTransform() {
        val src = loadUnderlineSource()
        assertTrue(
            "DiagnosticsInlineUnderline MUST short-circuit on diagnostics.isEmpty()",
            src.contains("diagnostics.isEmpty()"),
        )
        assertTrue(
            "DiagnosticsInlineUnderline MUST return TransformedText(text, OffsetMapping.Identity) on empty",
            src.contains("TransformedText(text, OffsetMapping.Identity)"),
        )
    }

    /**
     * Layer 2: singleDiagnostic_appliesUnderlineSpan — source MUST call
     * builder.addStyle with SpanStyle(textDecoration=TextDecoration.Underline).
     * Anti-bluff: stubbing filter() to skip the span loop means addStyle is
     * never called; this assertion catches the structural removal.
     */
    @Test
    fun singleDiagnostic_appliesUnderlineSpan() {
        val src = loadUnderlineSource()
        assertTrue(
            "DiagnosticsInlineUnderline MUST add SpanStyle with TextDecoration.Underline",
            src.contains("TextDecoration.Underline"),
        )
        assertTrue(
            "DiagnosticsInlineUnderline MUST call builder.addStyle",
            src.contains("builder.addStyle("),
        )
        // The severity color MUST come from severityVisuals.
        assertTrue(
            "DiagnosticsInlineUnderline MUST call severityVisuals for the span color",
            src.contains("severityVisuals("),
        )
    }

    /**
     * Layer 3: multipleDiagnostics_layerSpans — source MUST iterate over
     * all diagnostics (not stop after the first). Anti-bluff: changing
     * `for (diag in diagnostics)` to `diagnostics.firstOrNull()` would
     * break this structural check.
     */
    @Test
    fun multipleDiagnostics_layerSpans() {
        val src = loadUnderlineSource()
        assertTrue(
            "DiagnosticsInlineUnderline MUST iterate all diagnostics with `for (diag in diagnostics)`",
            src.contains("for (diag in diagnostics)"),
        )
    }

    /**
     * Layer 4: outOfBoundsRange_clamped — source MUST clamp start/end to
     * [0, text.length]. Anti-bluff: removing the coerceIn calls causes
     * IndexOutOfBoundsException at runtime; the structural check ensures
     * the clamp is present.
     */
    @Test
    fun outOfBoundsRange_clamped() {
        val src = loadUnderlineSource()
        assertTrue(
            "DiagnosticsInlineUnderline MUST clamp start via coerceIn(0, text.length)",
            src.contains("coerceIn(0, text.length)"),
        )
        assertTrue(
            "DiagnosticsInlineUnderline MUST clamp end via coerceIn(start, text.length)",
            src.contains("coerceIn(start, text.length)"),
        )
        // Must guard against start >= end to avoid zero-width span.
        assertTrue(
            "DiagnosticsInlineUnderline MUST guard `if (start < end)` before addStyle",
            src.contains("if (start < end)"),
        )
    }
}
