/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.1: TokenFrequencyProvider — ranks words in the
 * document by frequency and surfaces those matching the current prefix.
 *
 * Algorithm:
 *   1. Tokenize ctx.text with the word regex `[A-Za-z_][A-Za-z0-9_]*`.
 *   2. Exclude the cursor word (the partial word currently being typed,
 *      i.e., the substring at ctx.prefixRange) so the user does not see
 *      a suggestion for the word they are mid-typing.
 *   3. Count occurrences for each remaining word.
 *   4. Return CompletionItems where word.startsWith(ctx.prefix), sorted
 *      by frequency descending. score = frequency.toDouble().
 *
 * Thread-safety: stateless — each call is independent.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider

/**
 * Completion provider that suggests words already present in the
 * document, ranked by their occurrence frequency.
 *
 * The cursor word (the partial word the user is currently typing) is
 * excluded from the frequency map so it never appears as its own
 * suggestion.
 */
class TokenFrequencyProvider : CompletionProvider {

    override val id: String = "token-frequency"

    private val wordRegex = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

    override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
        if (ctx.prefix.isEmpty()) return emptyList()

        // The exact substring being typed — ctx.prefix is the partial word
        // at the cursor. We exclude any token whose value equals the cursor
        // prefix AND whose start position aligns with the prefixRange start,
        // so the user does not see the word they are currently typing.
        // NOTE: ctx.prefixRange end is the cursor char offset (used as exclusive
        // end by CompletionContext.of), so we do NOT substring it — we use ctx.prefix
        // directly (which was already extracted correctly by CompletionContext.of).
        val cursorWord = ctx.prefix

        val freq = mutableMapOf<String, Int>()
        for (match in wordRegex.findAll(ctx.text)) {
            val word = match.value
            val matchRange = match.range
            // Exclude the cursor word occurrence (the partial token being typed).
            // A match is the cursor's own token when its start equals prefixRange.first
            // (the regex can only start one word there) and its text equals the prefix.
            if (word == cursorWord && matchRange.first == ctx.prefixRange.first) {
                continue
            }
            if (word.startsWith(ctx.prefix)) {
                freq[word] = (freq[word] ?: 0) + 1
            }
        }

        return freq
            .entries
            .sortedByDescending { it.value }
            .map { (word, count) ->
                CompletionItem(
                    label = word,
                    insertText = word,
                    kind = CompletionItem.Kind.Word,
                    score = count.toDouble(),
                    range = ctx.prefixRange,
                )
            }
    }
}
