/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.3: anti-bluff CompletionEngine tests (desktopTest).
 *
 * Must run in desktopTest because delay() requires the JVM coroutines
 * dispatcher to be exercised in real time (no kotlinx-coroutines-test
 * available in commonTest per the JUnit4 / WASM constraints).
 *
 * Tests:
 *   1. fastProvider_itemsInFirstEmission — fast provider results appear
 *      in the first Flow emission.
 *   2. slowProvider_itemsInLaterEmission — slow provider (delay 50ms)
 *      contributes items; final emission contains its items.
 *   3. throwingProvider_doesNotCrashFlow — provider that throws must not
 *      propagate exception; other providers' items still arrive.
 *   4. progressiveEmission_multipleDistinctEmissions — at least 2 distinct
 *      emission events when one provider is fast and one is slow.
 *   5. hangingProvider_doesNotHangFlow — provider that delays 10 s is
 *      killed by withTimeout(500ms); flow completes within 1 s total.
 *   6. finalEmission_containsUnionOfFastAndSlow — after both a fast
 *      and a slow provider finish, the last emission contains both sets.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure —
 *   - Mutated: removed withTimeout wrapper from CompletionEngine.complete.
 *   - Re-ran hangingProvider_doesNotHangFlow: test timed out (> 1 s actual
 *     vs 1 s assertion timeout), proving the timeout is load-bearing.
 *   - Reverted mutation; all tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class CompletionEngineTest {

    // -----------------------------------------------------------------------
    // Test helpers — fake providers
    // -----------------------------------------------------------------------

    private fun ctx() = CompletionContext(
        text = "hello world hello",
        cursorChar = 5,
        langId = null,
        prefix = "hello",
        prefixRange = 0..5,
        surroundingScope = null,
    )

    private fun item(label: String, score: Double = 1.0, kind: CompletionItem.Kind = CompletionItem.Kind.Word) =
        CompletionItem(label = label, insertText = label, kind = kind, score = score, range = 0..5)

    /** Returns items immediately. */
    private fun fastProvider(vararg items: CompletionItem): CompletionProvider =
        object : CompletionProvider {
            override val id = "fast"
            override suspend fun complete(ctx: CompletionContext) = items.toList()
        }

    /** Returns items after a delay. */
    private fun slowProvider(delayMs: Long, vararg items: CompletionItem): CompletionProvider =
        object : CompletionProvider {
            override val id = "slow"
            override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
                delay(delayMs)
                return items.toList()
            }
        }

    /** Always throws a non-cancellation exception. */
    private fun throwingProvider(): CompletionProvider =
        object : CompletionProvider {
            override val id = "throwing"
            override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
                throw IllegalStateException("simulated provider crash")
            }
        }

    /** Hangs indefinitely (long delay to trigger timeout). */
    private fun hangingProvider(): CompletionProvider =
        object : CompletionProvider {
            override val id = "hanging"
            override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
                delay(30_000L)  // 30 s — far beyond the 500 ms engine timeout
                return emptyList()
            }
        }

    // -----------------------------------------------------------------------
    // Test 1: fast provider appears in first emission
    // -----------------------------------------------------------------------

    @Test
    fun fastProvider_itemsInFirstEmission() = runBlocking<Unit> {
        val engine = CompletionEngine(listOf(fastProvider(item("alpha"))))
        val emissions = engine.complete(ctx()).toList()

        assertTrue("Expected at least one emission", emissions.isNotEmpty())
        val first = emissions.first()
        assertTrue(
            "First emission must contain 'alpha', got: ${first.map { it.label }}",
            first.any { it.label == "alpha" },
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: slow provider items appear in a later emission
    // -----------------------------------------------------------------------

    @Test
    fun slowProvider_itemsInLaterEmission() = runBlocking<Unit> {
        val engine = CompletionEngine(
            listOf(
                fastProvider(item("fast-item")),
                slowProvider(80L, item("slow-item")),
            ),
        )
        val emissions = engine.complete(ctx()).toList()

        // The last emission must contain slow-item.
        val last = emissions.last()
        assertTrue(
            "Last emission must contain 'slow-item', got: ${last.map { it.label }}",
            last.any { it.label == "slow-item" },
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: throwing provider does not crash the flow
    // -----------------------------------------------------------------------

    @Test
    fun throwingProvider_doesNotCrashFlow() = runBlocking<Unit> {
        val engine = CompletionEngine(
            listOf(
                throwingProvider(),
                fastProvider(item("safe-item")),
            ),
        )
        // Must not throw; toList() completes normally.
        val emissions = engine.complete(ctx()).toList()

        val allLabels = emissions.flatten().map { it.label }
        assertTrue(
            "safe-item from the non-throwing provider must be present. Got labels: $allLabels",
            allLabels.contains("safe-item"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: progressive emission — at least 2 distinct events
    // -----------------------------------------------------------------------

    @Test
    fun progressiveEmission_multipleDistinctEmissions() = runBlocking<Unit> {
        val engine = CompletionEngine(
            listOf(
                fastProvider(item("fast-item")),
                slowProvider(80L, item("slow-item")),
            ),
        )
        val emissions = engine.complete(ctx()).toList()

        assertTrue(
            "Expected at least 2 emissions (one per non-empty provider), got ${emissions.size}",
            emissions.size >= 2,
        )
        // Each emission must be distinct (growing list).
        val firstLabels = emissions.first().map { it.label }.toSet()
        val lastLabels = emissions.last().map { it.label }.toSet()
        assertTrue(
            "Last emission must contain more labels than the first (progressive), " +
                "first=$firstLabels last=$lastLabels",
            lastLabels.size > firstLabels.size,
        )
    }

    // -----------------------------------------------------------------------
    // Test 5: hanging provider is cancelled by timeout; flow finishes < 1 s
    // -----------------------------------------------------------------------

    @Test
    fun hangingProvider_doesNotHangFlow() = runBlocking<Unit> {
        val engine = CompletionEngine(listOf(hangingProvider()))

        val elapsedMs = measureTimeMillis {
            engine.complete(ctx()).toList()
        }

        assertTrue(
            "Flow with a hanging provider must complete within 1000 ms (timeout=500ms). " +
                "Actual: ${elapsedMs}ms",
            elapsedMs < 1_000L,
        )
    }

    // -----------------------------------------------------------------------
    // Test 6: final emission contains union of fast + slow items
    // -----------------------------------------------------------------------

    @Test
    fun finalEmission_containsUnionOfFastAndSlow() = runBlocking<Unit> {
        val engine = CompletionEngine(
            listOf(
                fastProvider(item("fast-only")),
                slowProvider(80L, item("slow-only")),
            ),
        )
        val emissions = engine.complete(ctx()).toList()
        val lastLabels = emissions.last().map { it.label }.toSet()

        assertTrue(
            "Final emission must contain 'fast-only'. Got: $lastLabels",
            "fast-only" in lastLabels,
        )
        assertTrue(
            "Final emission must contain 'slow-only'. Got: $lastLabels",
            "slow-only" in lastLabels,
        )
        assertEquals(
            "Final emission must have exactly 2 items (deduped union of fast + slow). Got: $lastLabels",
            2,
            lastLabels.size,
        )
    }
}
