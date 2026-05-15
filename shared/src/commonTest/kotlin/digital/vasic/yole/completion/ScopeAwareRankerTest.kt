/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.1: anti-bluff ScopeAwareRanker tests (commonTest).
 *
 * Table-driven over the 8 scope × kind pairs specified in the phase-4 spec:
 *
 *   member_access + Identifier → +2.0
 *   member_access + Word       → +0.0
 *   member_access + Snippet    → +0.0
 *   type_annotation + Identifier → +1.5
 *   type_annotation + Word     → +0.0
 *   string_literal + Identifier → -3.0
 *   null scope + any           → +0.0
 *   unknown scope + any        → +0.0
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure —
 *   - Mutated: changed member_access Identifier branch to return 0.0.
 *   - Re-ran: memberAccess_identifier_boostsBy2 FAILED (got 0.0, expected 2.0).
 *   - Reverted mutation; all tests GREEN.
 *   This guarantees the boost table is actually exercised, not bypassed.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals

class ScopeAwareRankerTest {

    // Helpers to build minimal CompletionItems of a given kind.
    private fun item(kind: CompletionItem.Kind) = CompletionItem(
        label = "x",
        insertText = "x",
        kind = kind,
        score = 0.0,
        range = 0..1,
    )

    // -----------------------------------------------------------------------
    // member_access scope
    // -----------------------------------------------------------------------

    @Test
    fun memberAccess_identifier_boostsBy2() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Identifier), "member_access")
        assertEquals(
            2.0,
            boost,
            1e-9,
            "member_access + Identifier must boost by +2.0",
        )
    }

    @Test
    fun memberAccess_word_noBooost() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Word), "member_access")
        assertEquals(
            0.0,
            boost,
            1e-9,
            "member_access + Word must not boost (words aren't context-relevant after '.')",
        )
    }

    @Test
    fun memberAccess_snippet_noBoost() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Snippet), "member_access")
        assertEquals(
            0.0,
            boost,
            1e-9,
            "member_access + Snippet must not boost",
        )
    }

    // -----------------------------------------------------------------------
    // type_annotation scope
    // -----------------------------------------------------------------------

    @Test
    fun typeAnnotation_identifier_boostsBy1point5() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Identifier), "type_annotation")
        assertEquals(
            1.5,
            boost,
            1e-9,
            "type_annotation + Identifier must boost by +1.5",
        )
    }

    @Test
    fun typeAnnotation_word_noBoost() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Word), "type_annotation")
        assertEquals(
            0.0,
            boost,
            1e-9,
            "type_annotation + Word must not boost",
        )
    }

    // -----------------------------------------------------------------------
    // string_literal scope
    // -----------------------------------------------------------------------

    @Test
    fun stringLiteral_identifier_suppresses() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Identifier), "string_literal")
        assertEquals(
            -3.0,
            boost,
            1e-9,
            "string_literal + Identifier must suppress by -3.0",
        )
    }

    @Test
    fun stringLiteral_word_suppresses() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Word), "string_literal")
        assertEquals(
            -3.0,
            boost,
            1e-9,
            "string_literal + Word must suppress by -3.0 (all kinds suppressed in string literals)",
        )
    }

    // -----------------------------------------------------------------------
    // null and unknown scopes
    // -----------------------------------------------------------------------

    @Test
    fun nullScope_noBoost() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Identifier), null)
        assertEquals(
            0.0,
            boost,
            1e-9,
            "null scope (Tree-Sitter unavailable) must return 0.0 boost",
        )
    }

    @Test
    fun unknownScope_noBoost() {
        val boost = ScopeAwareRanker.boost(item(CompletionItem.Kind.Identifier), "some_unknown_scope")
        assertEquals(
            0.0,
            boost,
            1e-9,
            "Unknown scope must return 0.0 boost",
        )
    }
}
