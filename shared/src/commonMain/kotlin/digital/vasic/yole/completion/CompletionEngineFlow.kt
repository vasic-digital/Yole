/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-82: K2 FIR workaround — extracted top-level flow builder.
 *
 * KGP 2.3.21 K2 FirIncompatibleClassExpressionChecker crashes with
 * "source must not be null" when a flow { } or channelFlow { } call
 * that returns Flow<List<T>> is compiled inside a class method.
 * The checker visits FirRegularClass → FirSimpleFunction → visitBlock,
 * and encounters a synthetic FIR node with a null PSI source for the
 * parameterized flow builder call.
 *
 * Workaround: extract the flow { } call to a file-level function.
 * Top-level functions are visited via FirFile → FirSimpleFunction
 * (not nested under FirRegularClass), which avoids the checker visit
 * path that produces the NPE. Remove this file once the K2 bug is
 * fixed upstream and move the body back into CompletionEngine.complete().
 *
 * iter-82 STATUS: Temporarily stubbed with emptyFlow() + TODO.
 * The K2 FIR crash at SnippetPlaceholderNavigator.kt:239 (separate bug)
 * means the real flow { } body also fails to compile under K2.
 * Restore the real implementation (commented below) once both K2 bugs
 * are fixed upstream in a future KGP release.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Top-level flow builder extracted from [CompletionEngine.complete] as a K2 compiler
 * workaround (iter-82). Fans out [providers] in parallel on [Dispatchers.Default],
 * respects [providerTimeout] per provider, and emits progressively-merged lists.
 *
 * STUB: Currently returns emptyFlow() due to K2 FIR compiler crashes in KGP 2.3.21.
 * See file header for details. Remove TODO and restore real implementation below
 * once the K2 bugs are fixed.
 *
 * @see CompletionEngine.complete for full documentation.
 */
@Suppress("UnusedParameter")
internal fun completionEngineFlow(
    providers: List<CompletionProvider>,
    ctx: CompletionContext,
    providerTimeout: kotlin.time.Duration,
): Flow<CompletionList> {
    // iter-82: K2 FIR stub — real flow { } body fails to compile under KGP 2.3.21 K2.
    // TODO: restore real implementation below when K2 bugs are fixed upstream.
    return emptyFlow()
}

/*
 * Real implementation — restore once K2 bugs are fixed:
 *
 *    return flow {
 *        val providerResults: List<CompletionList> = coroutineScope {
 *            providers.map { provider ->
 *                async(Dispatchers.Default) {
 *                    val items: CompletionList
 *                    @Suppress("SwallowedException")
 *                    items = try {
 *                        withTimeout(providerTimeout) {
 *                            provider.complete(ctx)
 *                        }
 *                    } catch (e: TimeoutCancellationException) {
 *                        emptyList()
 *                    } catch (e: CancellationException) {
 *                        throw e
 *                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
 *                        emptyList()
 *                    }
 *                    items
 *                }
 *            }.awaitAll()
 *        }
 *
 *        val accumulated = mutableListOf<CompletionList>()
 *        for (result in providerResults) {
 *            accumulated.add(result)
 *            val merged = CompletionRanker.merge(accumulated, ctx.surroundingScope)
 *            if (merged.isNotEmpty()) {
 *                emit(merged)
 *            }
 *        }
 *    }
 */
