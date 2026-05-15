/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 1: anti-bluff prefix extraction tests.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionContextTest {
    @Test
    fun prefix_extractedFromCursorByte() {
        val ctx = CompletionContext.of(
            text = "fun foo() { println(\"hi\") }",
            cursorChar = 7, // just after "foo"
            langId = "kotlin",
        )
        assertEquals("foo", ctx.prefix)
        assertEquals(4..7, ctx.prefixRange)
    }

    @Test
    fun prefix_emptyWhenCursorOnWhitespace() {
        val ctx = CompletionContext.of(
            text = "fun foo()",
            cursorChar = 4, // insert-point after the space (between space and 'f' of "foo")
            langId = "kotlin",
        )
        assertEquals("", ctx.prefix)
    }
}
