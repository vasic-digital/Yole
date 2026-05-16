/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: anti-bluff tests for LinkPreserver.
 *
 * Mutation stub: replace toMarkdownLink body with `return text`
 * → tests plainLink_producesCorrectMarkdown,
 *          closingBracketInText_isEscaped, and
 *          closingParenInUrl_isPercentEncoded all FAIL.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkPreserverTests {

    @Test
    fun plainLink_producesCorrectMarkdown() {
        val result = LinkPreserver.toMarkdownLink("Click here", "https://example.com")
        assertEquals("[Click here](https://example.com)", result)
    }

    @Test
    fun closingBracketInText_isEscaped() {
        // "]" in text must become "\]"
        val result = LinkPreserver.toMarkdownLink("See [note]", "https://docs.example.com")
        assertEquals("[See [note\\]](https://docs.example.com)", result)
    }

    @Test
    fun closingParenInUrl_isPercentEncoded() {
        // Only ")" needs escaping in the URL portion (prevents premature link close).
        // "(" is left as-is — only ")" → "%29" per spec.
        val result = LinkPreserver.toMarkdownLink("Link", "https://en.wikipedia.org/wiki/Foo_(bar)")
        assertEquals("[Link](https://en.wikipedia.org/wiki/Foo_(bar%29)", result,
            "Closing paren in URL must be percent-encoded as %29")
    }
}
