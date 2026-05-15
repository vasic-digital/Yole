/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60: universal completion provider contract.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * Universal completion provider contract. Phase 3 ships 3 impls
 * (TokenFrequencyProvider, SnippetProvider, IdentifierProvider).
 * Feature 4 (LSP integration) adds a 4th without touching this file.
 *
 * Implementations MUST be thread-safe and MUST NOT block — return
 * empty list on any failure (graceful degradation per CONST-035;
 * caller logs the failure).
 */
interface CompletionProvider {
    /** Human-readable id, used for diagnostics + Engine parity tests. */
    val id: String

    /**
     * Produce candidate items for the context. May return empty.
     * Implementations SHOULD respect a soft latency budget (~50ms);
     * the CompletionEngine wraps each call in withTimeout.
     */
    suspend fun complete(ctx: CompletionContext): List<CompletionItem>
}
