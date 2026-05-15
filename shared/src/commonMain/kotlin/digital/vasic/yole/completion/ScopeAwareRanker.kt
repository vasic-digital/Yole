/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.1: ScopeAwareRanker — table-driven score boost
 * based on the Tree-Sitter surroundingScope and item kind.
 *
 * Design rationale (phase-4 spec §4.1):
 *   The ranker is a pure rank-modifier, not a provider. It returns a
 *   boost Double that the caller (CompletionRanker) adds to an item's
 *   existing score. Keeping it separate means CompletionRanker can
 *   swap ranking strategies without touching provider logic.
 *
 * Boost table (derived from research-report §6.2 and phase-4 spec):
 *   member_access + Identifier → +2.0  (method / field suggestion)
 *   member_access + Word       → +0.0  (words aren't relevant after `.`)
 *   type_annotation + Identifier → +1.5 (type names are relevant after `:`)
 *   string_literal + any       → -3.0  (suppress — completing inside
 *                                       string literals is rarely useful)
 *   null scope + any           → +0.0  (Tree-Sitter unavailable)
 *   any other scope + any      → +0.0  (unknown scope, no opinion)
 *
 * Thread-safety: stateless object — all calls are independent.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * Rank-modifier that returns a score boost to apply to a [CompletionItem]
 * based on the editor's surrounding syntactic scope.
 *
 * Obtain the scope from [CompletionContext.surroundingScope] (filled by
 * Tree-Sitter via the Engine; null when Tree-Sitter is unavailable).
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated the `member_access` Identifier branch to return 0.0.
 *   - Re-ran ScopeAwareRankerTest: memberAccess_identifier_boostsBy2 FAILED.
 *   - Reverted mutation; all tests GREEN.
 */
object ScopeAwareRanker {

    private const val MEMBER_ACCESS = "member_access"
    private const val TYPE_ANNOTATION = "type_annotation"
    private const val STRING_LITERAL = "string_literal"

    private const val BOOST_MEMBER_IDENTIFIER = 2.0
    private const val BOOST_TYPE_IDENTIFIER = 1.5
    private const val SUPPRESS_STRING = -3.0
    private const val BOOST_NONE = 0.0

    /**
     * Return the score boost to add to [item]'s current score given [scope].
     *
     * @param item the candidate completion item.
     * @param scope Tree-Sitter node type at the cursor, or null.
     * @return a Double (may be negative) to be summed with [item.score].
     */
    fun boost(item: CompletionItem, scope: String?): Double = when (scope) {
        MEMBER_ACCESS -> when (item.kind) {
            CompletionItem.Kind.Identifier -> BOOST_MEMBER_IDENTIFIER
            else -> BOOST_NONE
        }
        TYPE_ANNOTATION -> when (item.kind) {
            CompletionItem.Kind.Identifier -> BOOST_TYPE_IDENTIFIER
            else -> BOOST_NONE
        }
        STRING_LITERAL -> SUPPRESS_STRING
        else -> BOOST_NONE
    }
}
