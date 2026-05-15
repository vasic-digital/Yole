/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 1: anti-bluff data-class tests.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionItemTest {
    @Test
    fun construct_carriesAllFields() {
        val item = CompletionItem(
            label = "println",
            insertText = "println($1)",
            kind = CompletionItem.Kind.Snippet,
            score = 0.75,
            range = 0..4,
        )
        assertEquals("println", item.label)
        assertEquals("println($1)", item.insertText)
        assertEquals(CompletionItem.Kind.Snippet, item.kind)
        assertEquals(0.75, item.score, 1e-9)
        assertEquals(0..4, item.range)
    }

    @Test
    fun kind_hasFourVariants() {
        val variants = CompletionItem.Kind.values().toSet()
        assertTrue(CompletionItem.Kind.Identifier in variants)
        assertTrue(CompletionItem.Kind.Snippet in variants)
        assertTrue(CompletionItem.Kind.Keyword in variants)
        assertTrue(CompletionItem.Kind.Word in variants)
        assertEquals(4, variants.size)
    }
}
