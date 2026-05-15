/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.2: anti-bluff CompletionRanker tests (commonTest).
 *
 * Tests:
 *   1. dedupe_keepHighestScore — overlapping label from two providers;
 *      deduped entry must carry the MAX score, not the min.
 *   2. sortedDescendingByScore — 3 items with distinct scores verify order.
 *   3. boostIsApplied_memberAccessIdentifier — member_access scope boosts
 *      an Identifier item; final score = original + 2.0.
 *   4. dedupe_thenBoost_correctOrder — overlapping items + boost → correct
 *      final ordering after dedup + boost.
 *   5. emptyInput_returnsEmpty — no providers → empty result.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure —
 *   - Mutated: CompletionRanker.merge skips ScopeAwareRanker.boost step.
 *   - Re-ran: boostIsApplied_memberAccessIdentifier FAILED (score didn't change).
 *   - Reverted mutation; all tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionRankerTest {

    private fun wordItem(label: String, score: Double) = CompletionItem(
        label = label,
        insertText = label,
        kind = CompletionItem.Kind.Word,
        score = score,
        range = 0..1,
    )

    private fun identifierItem(label: String, score: Double) = CompletionItem(
        label = label,
        insertText = label,
        kind = CompletionItem.Kind.Identifier,
        score = score,
        range = 0..1,
    )

    // -----------------------------------------------------------------------
    // Test 1: deduplication keeps highest score
    // -----------------------------------------------------------------------

    @Test
    fun dedupe_keepHighestScore() {
        // Provider A: "foo" with score 0.5
        // Provider B: "foo" with score 0.9  ← must win
        val providerA = listOf(wordItem("foo", 0.5), wordItem("bar", 0.3))
        val providerB = listOf(wordItem("foo", 0.9), wordItem("baz", 0.1))

        val merged = CompletionRanker.merge(listOf(providerA, providerB), scope = null)

        val foos = merged.filter { it.label == "foo" }
        assertEquals(1, foos.size, "label 'foo' must appear exactly once after dedup")
        assertEquals(
            0.9,
            foos.first().score,
            1e-9,
            "deduped 'foo' must carry the MAX score (0.9), not 0.5",
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: sort descending by score
    // -----------------------------------------------------------------------

    @Test
    fun sortedDescendingByScore() {
        val provider1 = listOf(
            wordItem("alpha", 1.0),
            wordItem("beta", 3.0),
            wordItem("gamma", 2.0),
        )

        val merged = CompletionRanker.merge(listOf(provider1), scope = null)

        assertEquals("beta", merged[0].label, "highest score (3.0) must be first")
        assertEquals("gamma", merged[1].label, "second score (2.0) must be second")
        assertEquals("alpha", merged[2].label, "lowest score (1.0) must be last")
    }

    // -----------------------------------------------------------------------
    // Test 3: boost is applied — member_access + Identifier → +2.0
    // -----------------------------------------------------------------------

    @Test
    fun boostIsApplied_memberAccessIdentifier() {
        val item = identifierItem("MyClass", 1.0)
        val merged = CompletionRanker.merge(listOf(listOf(item)), scope = "member_access")

        assertEquals(1, merged.size)
        val finalScore = merged.first().score
        assertEquals(
            3.0,  // 1.0 original + 2.0 boost
            finalScore,
            1e-9,
            "member_access scope must boost Identifier score by +2.0; expected 3.0, got $finalScore",
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: dedup then boost, correct final ordering
    // -----------------------------------------------------------------------

    @Test
    fun dedupe_thenBoost_correctOrder() {
        // Two providers each returning "method" (Identifier) and "word" (Word).
        // Highest score for "method" = 1.0 (provider B), "word" = 2.0.
        // With member_access scope: "method" gets +2.0 → final 3.0, "word" stays 2.0.
        // Expected order: method (3.0), word (2.0).
        val providerA = listOf(identifierItem("method", 0.5), wordItem("word", 2.0))
        val providerB = listOf(identifierItem("method", 1.0))

        val merged = CompletionRanker.merge(listOf(providerA, providerB), scope = "member_access")

        val methodItem = merged.find { it.label == "method" }
        val wordItem = merged.find { it.label == "word" }

        assertEquals(1, merged.filter { it.label == "method" }.size, "method deduped to one entry")
        assertEquals(3.0, methodItem?.score ?: -1.0, 1e-9, "method: 1.0 + 2.0 boost = 3.0")
        assertEquals(2.0, wordItem?.score ?: -1.0, 1e-9, "word: 2.0, no boost (Word in member_access)")
        assertEquals("method", merged[0].label, "method (3.0) must rank first")
        assertEquals("word", merged[1].label, "word (2.0) must rank second")
    }

    // -----------------------------------------------------------------------
    // Test 5: empty input
    // -----------------------------------------------------------------------

    @Test
    fun emptyInput_returnsEmpty() {
        val merged = CompletionRanker.merge(emptyList(), scope = null)
        assertTrue(merged.isEmpty(), "empty provider list must produce empty result")
    }
}
