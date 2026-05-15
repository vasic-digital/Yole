/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 8a: SnippetPlaceholderNavigator unit tests (commonTest).
 *
 * These are pure-logic tests with zero platform dependencies.
 * runBlocking<Unit> not needed — all functions are synchronous.
 *
 * Anti-bluff covenant (CONST-035):
 *   Mutation procedure documented per test; applied before commit:
 *   - Stubbed VsCodeSnippetExpander.expand to always return
 *     ExpandedSnippet(body, emptyList(), false).
 *   - Re-ran: tests 2-7 FAILED (wrong strippedBody or empty placeholders).
 *   - Stubbed SnippetPlaceholderNavigator.advance() to always return null.
 *   - Re-ran: tests 7-8 FAILED (advance never returns a real range).
 *   - Reverted all mutations. All 9 tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion

import digital.vasic.yole.completion.snippet.ExpandedSnippet
import digital.vasic.yole.completion.snippet.Placeholder
import digital.vasic.yole.completion.snippet.SnippetPlaceholderNavigator
import digital.vasic.yole.completion.snippet.VsCodeSnippetExpander
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnippetPlaceholderNavigatorTest {

    // -----------------------------------------------------------------------
    // VsCodeSnippetExpander.expand tests
    // -----------------------------------------------------------------------

    /**
     * Test 1: plain text with no markers → body unchanged, no placeholders.
     *
     * Anti-bluff: mutating expand to strip all text would change strippedBody
     * from "hello" to "", failing this assertion.
     */
    @Test
    fun expand_noMarkers_returnsBodyAsIs_emptyPlaceholders() {
        val result = VsCodeSnippetExpander.expand("hello")
        assertEquals("hello", result.strippedBody)
        assertTrue(result.placeholders.isEmpty(), "Expected no placeholders, got ${result.placeholders}")
        assertFalse(result.hasFinalStop)
    }

    /**
     * Test 2: single `${1:val}` marker → strippedBody has default text in place,
     * one placeholder with the correct range in stripped-body coordinates.
     *
     * Input:  "x = ${1:val};"
     * Output: strippedBody="x = val;", placeholder at range 4..6 (indices of "val").
     *
     * Anti-bluff: stubbing expand to return empty placeholders list causes
     * assertEquals on placeholders to fail.
     */
    @Test
    fun expand_singlePlaceholder_extractsRange() {
        val result = VsCodeSnippetExpander.expand("x = \${1:val};")
        assertEquals("x = val;", result.strippedBody)
        assertEquals(1, result.placeholders.size)
        val ph = result.placeholders[0]
        assertEquals(1, ph.index)
        assertEquals("val", ph.default)
        // "val" starts at index 4 in "x = val;" and ends at index 6 (inclusive).
        assertEquals(4..6, ph.rangeInBody)
    }

    /**
     * Test 3: two placeholders in reverse order → sorted ascending by index.
     *
     * Input:  "${2:b}-${1:a}"
     * Output: strippedBody="b-a"
     *   placeholder(1, rangeInBody=2..2, default="a")
     *   placeholder(2, rangeInBody=0..0, default="b")
     * sorted by index: [1, 2].
     *
     * Anti-bluff: stubbing expand returns emptyList for placeholders → size
     * assertion fails.
     */
    @Test
    fun expand_multiplePlaceholders_ordersByIndex() {
        val result = VsCodeSnippetExpander.expand("\${2:b}-\${1:a}")
        assertEquals("b-a", result.strippedBody)
        assertEquals(2, result.placeholders.size)
        // Must be sorted by index ascending.
        assertEquals(1, result.placeholders[0].index)
        assertEquals(2, result.placeholders[1].index)
        assertEquals("a", result.placeholders[0].default)
        assertEquals("b", result.placeholders[1].default)
        // "b" occupies position 0 in "b-a"; "a" occupies position 2.
        assertEquals(0..0, result.placeholders[1].rangeInBody) // ${2:b} → "b" at 0
        assertEquals(2..2, result.placeholders[0].rangeInBody) // ${1:a} → "a" at 2
    }

    /**
     * Test 4: `$0` (final tab stop) → stripped body intact, hasFinalStop=true,
     * placeholder has empty/point range.
     *
     * Input:  "foo$0bar"
     * Output: strippedBody="foobar", hasFinalStop=true,
     *         placeholder(0, rangeInBody=3..2, default="")
     *   Convention: empty range represented as (start)..(start-1) — width zero.
     *
     * Anti-bluff: stubbing hasFinalStop to always false causes the assertion
     * below to fail.
     */
    @Test
    fun expand_finalStop_marked() {
        val result = VsCodeSnippetExpander.expand("foo\$0bar")
        assertEquals("foobar", result.strippedBody)
        assertTrue(result.hasFinalStop, "Expected hasFinalStop=true")
        val finalStop = result.placeholders.firstOrNull { it.index == 0 }
        assertTrue(finalStop != null, "Expected a placeholder with index=0")
        assertEquals("", finalStop!!.default)
        // Empty range: start=3, end=2 (start-1 convention).
        assertEquals(3, finalStop.rangeInBody.first)
    }

    /**
     * Test 5: short-form `$N` (single digit) → treated the same as `${N}`.
     *
     * Input:  "$1-$2"
     * Output: strippedBody="-" (empty defaults), 2 placeholders (no defaults).
     *
     * Anti-bluff: stubbing expand to return 0 placeholders fails the size check.
     */
    @Test
    fun expand_shortForm_dollarN_parsed() {
        val result = VsCodeSnippetExpander.expand("\$1-\$2")
        // Both $1 and $2 have empty defaults; stripped body is just "-".
        assertEquals("-", result.strippedBody)
        assertEquals(2, result.placeholders.size)
        val indices = result.placeholders.map { it.index }.sorted()
        assertEquals(listOf(1, 2), indices)
    }

    /**
     * Test 6: escaped dollar sign `\$1` → treated as literal text `$1`,
     * no placeholder parsed.
     *
     * Convention: `\$` in the body string is produced by the raw snippet body
     * containing a backslash followed by `$`. After escape-processing the
     * output strippedBody contains `$1` literally with no placeholder.
     *
     * Anti-bluff: if the parser treats `\$1` as a real tab-stop, a placeholder
     * would be emitted and the size assertion below fails.
     */
    @Test
    fun expand_escapedDollar_literal() {
        // Raw body string: \$1 (backslash + dollar + 1)
        val result = VsCodeSnippetExpander.expand("\\$1")
        // The stripped body should contain "$1" literally (no expansion).
        assertEquals("\$1", result.strippedBody)
        assertTrue(
            result.placeholders.isEmpty(),
            "Escaped dollar must not produce a placeholder, got ${result.placeholders}",
        )
    }

    // -----------------------------------------------------------------------
    // SnippetPlaceholderNavigator tests
    // -----------------------------------------------------------------------

    /**
     * Test 7: navigator translates body-relative ranges to absolute doc offsets.
     *
     * Expansion: "${1:a} ${2:b}" (sorted: ph1 at 0..0 "a", ph2 at 2..2 "b")
     * strippedBody = "a b"
     * baseOffset = 100
     * → first advance() returns 100..100 (ph1 "a" shifted by 100)
     * → second advance() returns 102..102 (ph2 "b" shifted by 100)
     *
     * Anti-bluff: stubbing advance() to always return null causes the
     * assertNotNull below to fail.
     */
    @Test
    fun navigator_advanceReturnsAbsoluteRanges() {
        val expansion = VsCodeSnippetExpander.expand("\${1:a} \${2:b}")
        assertEquals("a b", expansion.strippedBody)
        val navigator = SnippetPlaceholderNavigator(expansion, baseOffset = 100)
        assertTrue(navigator.isActive(), "Navigator must be active before any advance")

        val first = navigator.advance()
        assertTrue(first != null, "First advance() must return a range")
        // placeholder 1 "a" is at index 0 in "a b" → absolute 100..100
        assertEquals(100, first!!.first)
        assertEquals(100, first.last)

        val second = navigator.advance()
        assertTrue(second != null, "Second advance() must return a range")
        // placeholder 2 "b" is at index 2 in "a b" → absolute 102..102
        val secondNonNull = second!!
        assertEquals(102, secondNonNull.first)
        assertEquals(102, secondNonNull.last)
    }

    /**
     * Test 8: exhausting all placeholders returns null (or $0 position).
     *
     * When no $0 is present: third advance on a 2-placeholder snippet → null.
     *
     * Anti-bluff: if advance always returns a non-null value, the final
     * assertNull check fails.
     */
    @Test
    fun navigator_advanceBeyondLast_returnsNull() {
        val expansion = VsCodeSnippetExpander.expand("\${1:a} \${2:b}")
        val navigator = SnippetPlaceholderNavigator(expansion, baseOffset = 0)

        navigator.advance() // ph1
        navigator.advance() // ph2
        val third = navigator.advance() // beyond last → null (no $0)
        assertNull(third, "advance() past last placeholder MUST return null")
        assertFalse(navigator.isActive(), "Navigator must be inactive after exhaustion")
    }

    /**
     * Test 9: complete() immediately deactivates the navigator.
     *
     * Anti-bluff: if complete() is a no-op, isActive() still returns true
     * after calling it, failing the assertion below.
     */
    @Test
    fun navigator_complete_clearsState() {
        val expansion = VsCodeSnippetExpander.expand("\${1:a}")
        val navigator = SnippetPlaceholderNavigator(expansion, baseOffset = 0)
        assertTrue(navigator.isActive())

        navigator.complete()

        assertFalse(navigator.isActive(), "After complete(), isActive() must be false")
        assertNull(navigator.current(), "After complete(), current() must be null")
    }
}
