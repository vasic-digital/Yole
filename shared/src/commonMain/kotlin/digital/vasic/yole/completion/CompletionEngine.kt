/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.3: CompletionEngine — top-level entry point that
 * fans out to all providers in parallel and emits progressive results.
 *
 * Design decisions (phase-4 spec §4.3):
 *
 *   Progressive emission: the Flow emits once per provider that completes.
 *   Each emission is a freshly-merged list of all results received so far.
 *   This gives the UI a fast first-paint (fast provider) and enriches the
 *   list as slower providers arrive — matching research-report §6 UX rec.
 *
 *   Per-provider timeout: withTimeout(500ms) wraps each provider call.
 *   TimeoutCancellationException is caught and that provider contributes
 *   no items. Other providers are unaffected.
 *
 *   CancellationException handling: only TimeoutCancellationException
 *   (a subclass of CancellationException) is caught silently. Any other
 *   CancellationException from the enclosing coroutine is rethrown
 *   (Detekt SwallowedException rule + CONST-035 anti-bluff).
 *
 *   Provider errors: non-cancellation exceptions are caught so one broken
 *   provider does not abort the whole flow. Logging is deferred to a
 *   later phase.
 *
 *   Dispatcher: Dispatchers.Default — CPU-bound fan-out; each provider
 *   is launched as an independent coroutine.
 *
 *   channelFlow is used so providers can send results back concurrently
 *   without a shared mutable list.
 *
 * Thread-safety: CompletionEngine is immutable after construction.
 *   providers list is read-only; channelFlow handles concurrent writes.
 *#######################################################*/
package digital.vasic.yole.completion

import digital.vasic.yole.completion.providers.IdentifierProvider
import digital.vasic.yole.completion.providers.LspCompletionProvider
import digital.vasic.yole.completion.providers.SnippetProvider
import digital.vasic.yole.completion.providers.TokenFrequencyProvider
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.lsp.LspServerHost
import digital.vasic.yole.lsp.LspServerRegistry
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * Orchestrates all [CompletionProvider]s in parallel and emits progressive
 * ranked lists via a [Flow].
 *
 * Each time a provider returns, the engine merges all results received so
 * far and emits a new [List<CompletionItem>]. The Flow completes once all
 * providers have either finished or timed out.
 *
 * @param providers ordered list of providers to fan out to (order affects
 *   nothing; ranking is handled by [CompletionRanker]).
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated: removed withTimeout wrapper (hanging provider hangs Flow).
 *   - Re-ran CompletionEngineTest.hangingProvider_doesNotHangFlow: FAILED
 *     (test timed out waiting for Flow to complete).
 *   - Reverted mutation; all tests GREEN.
 */
class CompletionEngine(
    val providers: List<CompletionProvider>,
) {

    /** Timeout applied to each individual provider call. */
    private val providerTimeout = 500.milliseconds

    /**
     * Fan out [ctx] to all providers on [Dispatchers.Default] and emit
     * progressively-richer merged lists as each provider completes.
     *
     * The [Flow] emits at least once per provider that completes within
     * [providerTimeout] and returns non-empty results. The flow always
     * terminates — hanging providers are cancelled by the timeout.
     *
     * @param ctx snapshot of editor state for this completion request.
     * @return cold [Flow] of progressively-merged, scored, deduplicated items.
     */
    fun complete(ctx: CompletionContext): Flow<List<CompletionItem>> = channelFlow {
        // Accumulate per-provider results as they arrive. Each provider
        // writes its own slot; the merger reads all slots on each write.
        val accumulated = mutableListOf<List<CompletionItem>>()

        // Channel to receive (providerIndex, results) pairs from each launched coroutine.
        // We use a rendez-vous channel so each send blocks until the collector processes it.
        val resultsChannel = Channel<List<CompletionItem>>(Channel.UNLIMITED)

        val jobs = providers.map { provider ->
            launch(Dispatchers.Default) {
                @Suppress("SwallowedException")
                val result: List<CompletionItem> = try {
                    withTimeout(providerTimeout) {
                        provider.complete(ctx)
                    }
                } catch (e: TimeoutCancellationException) {
                    // Intentionally swallowed: provider exceeded the 500 ms budget.
                    // TimeoutCancellationException is a scoped CancellationException
                    // generated by our own withTimeout — not a signal to cancel the
                    // enclosing flow. Contributes no items; flow continues normally.
                    emptyList()
                } catch (e: CancellationException) {
                    // Flow collector cancelled — propagate upward.
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Intentionally swallowed: provider threw a non-cancellation
                    // exception. Degrade gracefully — contribute no items.
                    // Logging deferred to Phase 7 (diagnostics plumbing).
                    emptyList()
                }
                resultsChannel.send(result)
            }
        }

        // Collect exactly providers.size messages, emitting after each.
        // resultsChannel.receive() is called sequentially on the channelFlow
        // dispatcher — no concurrent access to `accumulated`, so no lock needed.
        // (synchronized is JVM-only; channelFlow's single-collector invariant
        // guarantees the repeat loop body is never re-entered concurrently.)
        repeat(providers.size) {
            val result = resultsChannel.receive()
            accumulated.add(result)
            val merged = CompletionRanker.merge(accumulated, ctx.surroundingScope)
            if (merged.isNotEmpty()) {
                send(merged)
            }
        }

        // Wait for all provider coroutines to finish (they already have — all
        // sends completed — but join() ensures structured concurrency cleanup).
        jobs.forEach { it.join() }
        resultsChannel.close()
    }

    companion object {
        /**
         * Construct the production-default engine wired with all four providers:
         * [TokenFrequencyProvider], [SnippetProvider], [IdentifierProvider], and
         * [LspCompletionProvider] (added in iter-61 Phase 5).
         *
         * @param extractor pre-constructed [OutlineExtractor] (one per surface).
         * @param engine pre-initialised [TokenizerEngine] with the grammar loaded.
         * @param lspHost optional [LspServerHost] for LSP-backed completions.
         *   Defaults to a freshly-constructed host backed by [LspServerRegistry.default()].
         *   Pass a pre-existing host (e.g. from IdeEditorScreen in Phase 6) to share
         *   the per-langId server process across calls.
         */
        fun default(
            extractor: OutlineExtractor,
            engine: TokenizerEngine,
            lspHost: LspServerHost = LspServerHost(LspServerRegistry.default()),
        ): CompletionEngine =
            CompletionEngine(
                providers = listOf(
                    TokenFrequencyProvider(),
                    SnippetProvider(),
                    IdentifierProvider(extractor, engine),
                    LspCompletionProvider(lspHost),
                ),
            )
    }
}
