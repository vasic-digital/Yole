/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.2: CompletionRanker — merges per-provider result lists
 * into a single deduplicated, boosted, score-descending list.
 *
 * Merge pipeline (phase-4 spec §4.2):
 *   1. Flatten all per-provider lists.
 *   2. Dedupe by label: when two items share a label, keep the one with
 *      the highest score (highest wins, not first-seen).
 *   3. Apply ScopeAwareRanker.boost — add boost to each item's score.
 *      Items are replaced with copies carrying the updated score so the
 *      original data class remains immutable.
 *   4. Sort descending by final score.
 *   5. Return.
 *
 * Thread-safety: stateless object — each call is independent.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * Merges the result lists from multiple [CompletionProvider]s into a
 * single ranked list.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated: skipped the ScopeAwareRanker.boost step (boost = 0 always).
 *   - Re-ran CompletionRankerTest: boostIsApplied_memberAccessIdentifier FAILED.
 *   - Reverted mutation; all tests GREEN.
 */
object CompletionRanker {

    /**
     * Merge, dedupe, boost and sort [perProviderResults].
     *
     * @param perProviderResults one inner list per provider; any list may be empty.
     * @param scope passed through to [ScopeAwareRanker.boost]; null when
     *   Tree-Sitter is unavailable (boost is 0.0 for null scope).
     * @return a new list sorted by score descending, with at most one entry
     *   per unique label.
     */
    fun merge(
        perProviderResults: List<List<CompletionItem>>,
        scope: String?,
    ): List<CompletionItem> {
        // Step 1 + 2: flatten and dedupe by label (keep highest score).
        val byLabel = mutableMapOf<String, CompletionItem>()
        for (list in perProviderResults) {
            for (item in list) {
                val existing = byLabel[item.label]
                if (existing == null || item.score > existing.score) {
                    byLabel[item.label] = item
                }
            }
        }

        // Step 3: apply boost — replace item with a copy carrying the new score.
        val boosted = byLabel.values.map { item ->
            val b = ScopeAwareRanker.boost(item, scope)
            if (b == 0.0) item else item.copy(score = item.score + b)
        }

        // Step 4: sort descending by final score.
        return boosted.sortedByDescending { it.score }
    }
}
