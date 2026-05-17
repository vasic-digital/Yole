/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-75 (#iter-62-desktop-editor-lsp-wiring):
 * Unit tests for diagnosticOffsetToLine — the shared pure-logic helper
 * that backs both Android DiagnosticsGutter and Desktop DesktopLspSurfaces.
 *
 * The function converts a character offset to a 0-based line number and is
 * the structural invariant that connects the LSP Diagnostic range offset to
 * the gutter dot position on both platforms.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub diagnosticOffsetToLine to always return 0.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.DesktopLspSurfacesLogicTest"
 *   3. Expect FAIL: offsetToLine_multiLine (line 2 expected, got 0).
 *   4. Revert; confirm all 5 GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop/Android: helper is commonMain; tests run on desktop JVM runner.
 *   - iOS/Web: helper available; not yet called (no gutter surface).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [diagnosticOffsetToLine] — character offset → 0-based line number.
 */
class DesktopLspSurfacesLogicTest {

    /**
     * An offset of 0 in a single-line document must map to line 0.
     *
     * Mutation: return 1 always → FAIL (expected 0 but got 1).
     */
    @Test
    fun offsetToLine_firstLine_isZero() {
        val text = "hello world"
        assertEquals(0, diagnosticOffsetToLine(text, offset = 0), "offset 0 must be on line 0")
        assertEquals(0, diagnosticOffsetToLine(text, offset = 5), "mid-line offset must still be 0")
        assertEquals(0, diagnosticOffsetToLine(text, offset = text.length), "end-of-doc offset must be line 0 for single-line")
    }

    /**
     * An offset after the first newline must map to line 1.
     *
     * Mutation: return 0 always → FAIL (expected 1 but got 0).
     */
    @Test
    fun offsetToLine_secondLine_isOne() {
        val text = "line0\nline1"
        val offsetOnLine1 = 7 // 'l' in "line1"
        assertEquals(1, diagnosticOffsetToLine(text, offset = offsetOnLine1), "offset 7 must be on line 1")
    }

    /**
     * Multi-line document with varying line lengths.
     *
     * Mutation: count newlines from the wrong direction →
     * line 2 returns wrong value → FAIL.
     */
    @Test
    fun offsetToLine_multiLine() {
        // "abc\nde\nfg" — line 0: 0-2, \n at 3, line 1: 4-5, \n at 6, line 2: 7-8
        val text = "abc\nde\nfg"
        assertEquals(0, diagnosticOffsetToLine(text, 0))
        assertEquals(0, diagnosticOffsetToLine(text, 3))  // newline itself is at index 3; loop runs 0..2
        assertEquals(1, diagnosticOffsetToLine(text, 4))  // 'd'
        assertEquals(1, diagnosticOffsetToLine(text, 5))  // 'e'
        assertEquals(2, diagnosticOffsetToLine(text, 7))  // 'f'
        assertEquals(2, diagnosticOffsetToLine(text, 8))  // 'g'
    }

    /**
     * An offset beyond the document length must be clamped to text.length.
     *
     * Mutation: remove coerceIn clamping → IndexOutOfBoundsException or
     * wrong count → FAIL.
     */
    @Test
    fun offsetToLine_beyondEnd_clampsToLastLine() {
        val text = "ab\ncd"
        // text.length = 5; clamped → line 1.
        assertEquals(1, diagnosticOffsetToLine(text, offset = 999), "out-of-bounds offset must clamp to last line")
    }

    /**
     * An empty string always returns line 0 regardless of offset.
     *
     * Mutation: return -1 for empty string → FAIL.
     */
    @Test
    fun offsetToLine_emptyString_isZero() {
        assertEquals(0, diagnosticOffsetToLine("", offset = 0), "empty string: line 0")
        assertEquals(0, diagnosticOffsetToLine("", offset = 100), "empty string with large offset: still line 0")
    }
}
