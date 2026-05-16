/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 3: LspRangeMappingTest — pure offset-calculation tests.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub lineColToOffset() to always return 0.
 *      → line1_col0_skipsFirstLine (expects 6) FAILS.
 *      → line1_col2_returnsAbsoluteOffset (expects 8) FAILS.
 *      → line0_col3_returns3 (expects 3) FAILS.
 *      → outOfBounds_clampsToEnd (expects 5) FAILS.
 *      (line0_col0 incidentally PASSes; ≥ 4 FAIL, satisfying the ≥ 2 threshold.)
 *   2. Revert stub → all 5 tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - commonTest: runs on all compilation targets (Desktop, Android, Wasm, iOS).
 *   - No coroutines-test dep; pure synchronous assertions (required for Wasm).
 *   - No MockK (JVM-only; forbidden in commonTest).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [LspRangeMapping.lineColToOffset].
 *
 * Each test calls the real production implementation and asserts an exact
 * offset — trivially-passing stubs would produce wrong numeric values and
 * cause test failures (anti-bluff property satisfied by design).
 */
class LspRangeMappingTest {

    /**
     * Line 0, col 0 on any non-empty text must return 0 (no skipping needed).
     *
     * Mutation: stub returns 0 → still PASS. (Other tests provide the failure
     * witnesses required by the procedure above.)
     */
    @Test
    fun line0_col0_returns0() {
        val offset = LspRangeMapping.lineColToOffset("hello", line = 0, col = 0)
        assertEquals(0, offset)
    }

    /**
     * Line 0, col 3 must return 3 (within the first line, no newline involved).
     *
     * Mutation: stub returns 0 → expected 3, FAIL.
     */
    @Test
    fun line0_col3_returns3() {
        val offset = LspRangeMapping.lineColToOffset("hello", line = 0, col = 3)
        assertEquals(3, offset)
    }

    /**
     * Line 1, col 0 must return 6 — skipping "hello\n" (6 chars).
     *
     * Mutation: stub returns 0 → expected 6, FAIL.
     */
    @Test
    fun line1_col0_skipsFirstLine() {
        // "hello\nworld" → line 0 = "hello\n" (indices 0..5), line 1 starts at 6.
        val offset = LspRangeMapping.lineColToOffset("hello\nworld", line = 1, col = 0)
        assertEquals(6, offset)
    }

    /**
     * Line 1, col 2 must return 8 — lineStart=6 + col=2.
     *
     * Mutation: stub returns 0 → expected 8, FAIL.
     */
    @Test
    fun line1_col2_returnsAbsoluteOffset() {
        val offset = LspRangeMapping.lineColToOffset("hello\nworld", line = 1, col = 2)
        assertEquals(8, offset)
    }

    /**
     * Line 999, col 999 on a 5-char single-line text must clamp to 5.
     *
     * Mutation: stub returns 0 → expected 5, FAIL.
     */
    @Test
    fun outOfBounds_clampsToEnd() {
        val offset = LspRangeMapping.lineColToOffset("hello", line = 999, col = 999)
        assertEquals(5, offset)
    }
}
