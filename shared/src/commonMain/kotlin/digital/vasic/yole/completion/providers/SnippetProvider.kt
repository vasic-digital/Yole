/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.2: SnippetProvider — surfaces bundled VS Code snippets
 * whose prefix matches the user's typed prefix.
 *
 * Delegates to SnippetRegistry.forLanguage(langId) then filters by
 * snippet.prefix.startsWith(ctx.prefix). Returns emptyList if langId
 * is null (plaintext documents have no language-specific snippets).
 *
 * Thread-safety: stateless — each call is independent. SnippetRegistry
 * is a process-global object that caches snippets after the first load.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider
import digital.vasic.yole.completion.snippet.SnippetRegistry

/**
 * Completion provider that serves bundled VS Code snippets for the
 * document's language.
 *
 * Returns an empty list when:
 * - [CompletionContext.langId] is null (plaintext — no snippets defined).
 * - No bundled snippet bundle exists for the language.
 * - No snippet's prefix starts with the current prefix.
 */
class SnippetProvider : CompletionProvider {

    override val id: String = "snippet"

    override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
        val langId = ctx.langId ?: return emptyList()
        return SnippetRegistry.forLanguage(langId)
            .filter { snippet -> snippet.prefix.startsWith(ctx.prefix) }
            .map { snippet ->
                CompletionItem(
                    label = snippet.prefix,
                    insertText = snippet.body,
                    kind = CompletionItem.Kind.Snippet,
                    score = 1.0,
                    range = ctx.prefixRange,
                )
            }
    }
}
