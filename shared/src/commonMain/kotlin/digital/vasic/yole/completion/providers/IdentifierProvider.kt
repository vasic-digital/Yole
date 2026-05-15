/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.3: IdentifierProvider — surfaces document symbols
 * (headings, functions, classes, etc.) extracted by OutlineExtractor.
 *
 * Delegates to OutlineExtractor.outlineFor() and maps each OutlineItem
 * whose name starts with ctx.prefix to a CompletionItem of kind Identifier.
 * Dedupes by name (multiple headings with identical text → one item).
 *
 * The OutlineExtractor and TokenizerEngine are injected by the caller so
 * the provider is testable without constructing a full editor stack.
 *
 * Thread-safety: the provider itself is stateless. OutlineExtractor is
 * constructed once per provider instance — callers MUST call
 * engine.initialize() and engine.loadGrammar(langId) before passing the
 * engine to complete(). Failure to do so will produce an empty result
 * (OutlineExtractor returns emptyList() on an uninitialised engine).
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Completion provider that extracts identifiers (headings, functions,
 * classes, etc.) from the document outline via Tree-Sitter.
 *
 * @param extractor pre-constructed OutlineExtractor (one per editor surface).
 * @param engine pre-initialised TokenizerEngine with the document's grammar loaded.
 */
class IdentifierProvider(
    private val extractor: OutlineExtractor,
    private val engine: TokenizerEngine,
) : CompletionProvider {

    override val id: String = "identifier"

    override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
        val langId = ctx.langId ?: return emptyList()

        val items = extractor.outlineFor(ctx.text, langId, engine)

        val seen = mutableSetOf<String>()
        val result = mutableListOf<CompletionItem>()
        for (item in items) {
            if (!item.name.startsWith(ctx.prefix)) continue
            if (!seen.add(item.name)) continue  // dedupe
            result += CompletionItem(
                label = item.name,
                insertText = item.name,
                kind = CompletionItem.Kind.Identifier,
                score = 1.0,
                range = ctx.prefixRange,
            )
        }
        return result
    }
}
