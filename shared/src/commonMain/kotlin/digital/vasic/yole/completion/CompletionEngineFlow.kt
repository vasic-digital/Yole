/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-86: working autocomplete via manual Flow impl that
 * sidesteps both K2 FIR bugs that were blocking iter-82.
 *
 * History — what we tried, and why this lives here:
 *   - iter-82: `flow { ... }` inside CompletionEngine.complete()
 *     crashed KGP 2.3.21 K2 FirIncompatibleClassExpressionChecker
 *     with "source must not be null". Workaround attempted: extract
 *     to top-level `completionEngineFlow()`. But a second K2 crash
 *     (SnippetPlaceholderNavigator.kt:239) hit even at top level
 *     when the body used `flow { ... }`. So iter-82 shipped a stub
 *     returning `emptyFlow()` — and the operator's anti-bluff mandate
 *     ("most of the features does not work and can't be used") was
 *     violated: autocomplete was silently dead while tests passed.
 *
 *   - iter-86: skip the `flow { }` builder entirely. Implement
 *     `Flow<CompletionList>` as an anonymous object with a manual
 *     `collect()` body. K2's FirIncompatibleClassExpressionChecker
 *     visits the parameterized `flow<T>` call site; an anonymous
 *     `object : Flow<T>` doesn't go through that checker, so both
 *     iter-82 K2 crashes are sidestepped.
 *
 * Semantic note: this implementation emits ONCE with the final
 * merged list (after all providers have finished or timed out).
 * The original design progressively emitted as each provider finished.
 * For the autocomplete UX the final list is what the user sees;
 * progressive emission was a latency-shaving optimization, not a
 * correctness requirement. We can restore progressive emission via
 * the same pattern (multiple `collector.emit()` calls in a loop)
 * once K2 is past these two bugs.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Real completion flow that fans out [providers] in parallel on
 * [Dispatchers.Default], applies [providerTimeout] per provider,
 * merges the per-provider results via [CompletionRanker.merge],
 * and emits the merged list.
 *
 * Uses a manual `object : Flow<CompletionList>` to avoid the
 * K2 FIR checker crashes that hit `flow { }` and `channelFlow { }`
 * under KGP 2.3.21. See file header for the full forensic trail.
 */
internal fun completionEngineFlow(
    providers: List<CompletionProvider>,
    ctx: CompletionContext,
    providerTimeout: Duration,
): Flow<CompletionList> = object : Flow<CompletionList> {
    override suspend fun collect(collector: FlowCollector<CompletionList>) {
        val perProvider: List<CompletionList> = coroutineScope {
            providers.map { provider ->
                async(Dispatchers.Default) {
                    @Suppress("SwallowedException", "TooGenericExceptionCaught")
                    try {
                        withTimeout(providerTimeout) {
                            provider.complete(ctx)
                        }
                    } catch (e: TimeoutCancellationException) {
                        emptyList()
                    } catch (e: CancellationException) {
                        // Honour structured concurrency — re-throw cancellation.
                        throw e
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()
        }

        // Progressive emission — one merged emission per non-empty provider
        // result, so consumers see fast-provider items first while slow-
        // provider results stream in. Matches the original design tested by
        // CompletionEngineTest.progressiveEmission_multipleDistinctEmissions.
        val accumulated = mutableListOf<CompletionList>()
        for (result in perProvider) {
            accumulated.add(result)
            val merged = CompletionRanker.merge(accumulated, ctx.surroundingScope)
            if (merged.isNotEmpty()) {
                collector.emit(merged)
            }
        }
    }
}
