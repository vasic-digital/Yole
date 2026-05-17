/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-75 (#iter-63-desktop-signature-help-popup-deferred):
 * Unit tests for resolveActiveParamSpan + splitSignatureParams — the shared
 * pure-logic helpers in SignatureHelpSpanResolver.kt (commonMain).
 *
 * These helpers back both Android SignatureHelpPill/SignatureHelpPopup and
 * the new Desktop DesktopSignatureHelpPopup, ensuring behavioral parity.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub resolveActiveParamSpan to always return null.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.SignatureHelpSpanResolverTest"
 *   3. Expect FAIL: singleParam_firstIsActive (expected range, got null).
 *                   multiParam_secondIsActive (expected range, got null).
 *   4. Remove the nesting depth guard in splitSignatureParams.
 *   5. Expect FAIL: genericParam_notSplitOnInnerComma (wrong span returned).
 *   6. Revert; confirm all 7 GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: tests run on JVM (desktopTest source set).
 *   - Android: identical logic path; covered by androidUnitTest.
 *   - iOS/Web:  helper is commonMain; logic unchanged on those targets.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [resolveActiveParamSpan] and [splitSignatureParams].
 */
class SignatureHelpSpanResolverTest {

    // -----------------------------------------------------------------------
    // resolveActiveParamSpan
    // -----------------------------------------------------------------------

    /**
     * Label with no parentheses must return null — nothing to highlight.
     *
     * Mutation: return 0..0 always → FAIL (expected null but got 0..0).
     */
    @Test
    fun noParens_returnsNull() {
        assertNull(resolveActiveParamSpan("someFunc", activeParam = 0), "no parens → must be null")
    }

    /**
     * Single-param label, param 0 active — span must cover exactly the param
     * token inside the parens.
     *
     * Mutation: stub to return null → FAIL (expected non-null span).
     */
    @Test
    fun singleParam_firstIsActive() {
        // "foo(a: Int)" — inner = "a: Int", token at positions 4..10
        val label = "foo(a: Int)"
        val span = resolveActiveParamSpan(label, activeParam = 0)
        assertEquals(4, span?.first, "token must start right after '('")
        assertEquals(10, span?.last,  "token must end before ')'")
        // Verify by substring
        assertEquals("a: Int", label.substring(span!!.first, span.last))
    }

    /**
     * Two-param label, second param active — span must cover the second token.
     *
     * Mutation: always return span for first param → FAIL (substring mismatch).
     */
    @Test
    fun multiParam_secondIsActive() {
        // "bar(x: Int, y: String)" — params: ["x: Int", " y: String"]
        val label = "bar(x: Int, y: String)"
        val span = resolveActiveParamSpan(label, activeParam = 1)
        val token = label.substring(span!!.first, span.last)
        assertEquals("y: String", token, "second active param token must be y: String (trimmed)")
    }

    /**
     * Generic type param — the inner comma in Map<K, V> must NOT split the
     * parameter; the whole generic is one token.
     *
     * Mutation: remove depth guard in splitSignatureParams → Map<K, V> gets
     * split at its inner comma → wrong span returned → FAIL.
     */
    @Test
    fun genericParam_notSplitOnInnerComma() {
        // "put(key: K, value: Map<K, V>)" — params: ["key: K", " value: Map<K, V>"]
        val label = "put(key: K, value: Map<K, V>)"
        val span = resolveActiveParamSpan(label, activeParam = 1)
        val token = label.substring(span!!.first, span.last)
        assertEquals("value: Map<K, V>", token, "generic param must be a single token")
    }

    /**
     * activeParam index out of range — must return null gracefully.
     *
     * Mutation: throw IndexOutOfBoundsException → FAIL (crash, not null).
     */
    @Test
    fun outOfRange_returnsNull() {
        val label = "f(a: Int)"
        assertNull(resolveActiveParamSpan(label, activeParam = 5), "out-of-range activeParam → null")
        assertNull(resolveActiveParamSpan(label, activeParam = -1), "negative activeParam → null")
    }

    // -----------------------------------------------------------------------
    // splitSignatureParams
    // -----------------------------------------------------------------------

    /**
     * Single param with no commas — list of one element.
     *
     * Mutation: return emptyList() → FAIL (expected size 1, got 0).
     */
    @Test
    fun splitSingleParam() {
        val result = splitSignatureParams("a: Int")
        assertEquals(1, result.size, "single param → one element")
        assertEquals("a: Int", result[0])
    }

    /**
     * Multiple params — each comma at depth 0 splits a new token.
     *
     * Mutation: always split on every comma (ignore depth) → generic test FAILS.
     */
    @Test
    fun splitMultipleParams_respectsNesting() {
        // "k: K, v: Map<K, V>" — must produce exactly 2 elements
        val result = splitSignatureParams("k: K, v: Map<K, V>")
        assertEquals(2, result.size, "two params, depth-guarded split → 2 elements")
        assertEquals("k: K", result[0])
        assertEquals(" v: Map<K, V>", result[1])
    }
}
