/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60: auto-complete data classes.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * A single completion suggestion shown in the popup.
 *
 * @property label what the user sees in the popup row.
 * @property insertText what gets inserted on commit. For snippets,
 *   may contain `${N:placeholder}` markers parsed by Phase 8 navigation.
 * @property kind disambiguates the icon + ranker boost rules.
 * @property score [0.0, 1.0] — higher wins on dedup.
 * @property range char-index range of the partial word being completed
 *   (the user's already-typed prefix); insertText replaces this range.
 */
data class CompletionItem(
    val label: String,
    val insertText: String,
    val kind: Kind,
    val score: Double,
    val range: IntRange,
) {
    enum class Kind { Identifier, Snippet, Keyword, Word }
}
