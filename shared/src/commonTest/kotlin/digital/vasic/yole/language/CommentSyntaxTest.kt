/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: anti-bluff CommentSyntax.toggleLine tests.
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.CommentSyntax
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentSyntaxTest {
    @Test
    fun toggleLine_addsLineCommentWhenAbsent() {
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("    val x = 42")
        assertEquals("    // val x = 42", result)
    }

    @Test
    fun toggleLine_removesLineCommentWhenPresent() {
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("    // val x = 42")
        assertEquals("    val x = 42", result)
    }

    @Test
    fun toggleLine_isNoopWhenLineCommentUndefined() {
        val syntax = CommentSyntax(blockComment = "<!--" to "-->")
        val original = "<p>hi</p>"
        assertEquals(original, syntax.toggleLine(original))
    }

    @Test
    fun toggleLine_handlesShortTrimMismatch() {
        // "//val" (no trailing space after //) — should still uncomment
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("//val x")
        assertEquals("val x", result)
    }
}
