/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.1: anti-bluff TokenFrequencyProvider tests.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated TokenFrequencyProvider.complete to always return emptyList().
 *   - Re-ran: fooAppears3Times_scoreIs3 FAILED (emptyList returned).
 *   - cursorWordFo_fooReturnedNotCursorPartial also FAILED.
 *   - Reverted mutation; all tests GREEN.
 *
 * Tests:
 *   1. fooAppears3Times_scoreIs3 — text with "foo" 3 times; prefix "f"
 *      at cursor position → "foo" returned with score ≥ 3.
 *   2. cursorWordExcluded_noFaInResults — "foo foo fa" cursor at end,
 *      prefix "fa" → cursor word "fa" is excluded, no match returned.
 *   3. cursorWordFo_fooReturnedNotCursorPartial — "foo foo fo" cursor
 *      at end, prefix "fo" → "foo" returned, "fo" excluded.
 *   4. emptyPrefix_returnsEmpty — no prefix → empty list.
 *   5. noMatch_returnsEmpty — prefix "zzz" → empty list.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenFrequencyProviderTest {

    private val provider = TokenFrequencyProvider()

    @Test
    fun fooAppears3Times_scoreIs3() = runBlocking<Unit> {
        // text = "foo bar foo baz foo " (trailing space so cursor lands with empty prefix)
        // We place the cursor at position 1 (just after first 'f') so prefix = "f".
        // All three "foo" occurrences are NOT the cursor word (cursor word = "f"),
        // so all three should be counted.
        val text = "foo bar foo baz foo "
        val ctx = CompletionContext.of(text = text, cursorChar = 1, langId = null)
        assertEquals("f", ctx.prefix)

        val items = provider.complete(ctx)

        assertTrue(
            items.any { it.label == "foo" },
            "Expected 'foo' in suggestions but got: ${items.map { it.label }}",
        )
        val fooItem = items.first { it.label == "foo" }
        assertTrue(
            fooItem.score >= 3.0,
            "Expected score ≥ 3.0 for 'foo' (appears 3 times), got ${fooItem.score}",
        )
    }

    @Test
    fun cursorWordExcluded_noFaInResults() = runBlocking<Unit> {
        // "foo foo fa" — cursor at end (position 10), prefix = "fa"
        // "fa" is the cursor word and should NOT appear as a suggestion.
        val text = "foo foo fa"
        val ctx = CompletionContext.of(text = text, cursorChar = text.length, langId = null)
        assertEquals("fa", ctx.prefix)

        val items = provider.complete(ctx)

        // "foo".startsWith("fa") = false, "fa" excluded → no results.
        assertTrue(
            items.none { it.label == "fa" },
            "Cursor word 'fa' must be excluded, got: ${items.map { it.label }}",
        )
    }

    @Test
    fun cursorWordFo_fooReturnedNotCursorPartial() = runBlocking<Unit> {
        // "foo foo fo" — cursor at end, prefix = "fo"
        // "fo" is the cursor word (excluded). "foo" starts with "fo" and is NOT excluded.
        val text = "foo foo fo"
        val ctx = CompletionContext.of(text = text, cursorChar = text.length, langId = null)
        assertEquals("fo", ctx.prefix)

        val items = provider.complete(ctx)

        assertTrue(
            items.any { it.label == "foo" },
            "Expected 'foo' in suggestions, got: ${items.map { it.label }}",
        )
        assertTrue(
            items.none { it.label == "fo" },
            "Cursor word 'fo' should be excluded, got: ${items.map { it.label }}",
        )
    }

    @Test
    fun emptyPrefix_returnsEmpty() = runBlocking<Unit> {
        // cursor after trailing space → prefix ""
        val text = "foo bar baz "
        val ctx = CompletionContext.of(text = text, cursorChar = text.length, langId = null)
        assertEquals("", ctx.prefix)

        val items = provider.complete(ctx)
        assertTrue(items.isEmpty(), "Empty prefix should return empty list, got: $items")
    }

    @Test
    fun noMatch_returnsEmpty() = runBlocking<Unit> {
        val text = "foo bar baz"
        val ctx = CompletionContext(
            text = text,
            cursorChar = text.length,
            langId = null,
            prefix = "zzz",
            prefixRange = text.length..text.length,
        )

        val items = provider.complete(ctx)
        assertTrue(items.isEmpty(), "No words match prefix 'zzz', expected empty, got: $items")
    }
}
