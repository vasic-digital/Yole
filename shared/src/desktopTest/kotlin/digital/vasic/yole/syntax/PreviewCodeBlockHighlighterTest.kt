/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 10: contract tests for PreviewCodeBlockHighlighter.
 *
 * Anti-bluff (CONST-035): the rewritesFencedMarkdownBlock test FAILS if
 *   PreviewCodeBlockHighlighter.buildTokenSpans is stubbed to return the
 *   original `text` unchanged — see Phase 10.5 mutation step.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.render.PreviewCodeBlockHighlighter
import digital.vasic.yole.syntax.theme.VsCodeThemeParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreviewCodeBlockHighlighterTest {

    // Theme covers both the post-mapped TextMate scopes (markup.heading,
    // punctuation, string, comment) AND the Tree-Sitter native scopes
    // (atx_h1_marker, atx_heading, paragraph, inline, "#") so that
    // whichever scope the real engine emits, Theme.tokenColor still
    // returns a non-null color. Mirrors SyntaxHighlighterTest.testThemeJson
    // so the two tests share a known-good fixture.
    private val testThemeJson = """
        {
          "name": "Test",
          "type": "dark",
          "colors": {},
          "tokenColors": [
            { "scope": "markup.heading", "settings": { "foreground": "#ff0000" } },
            { "scope": "punctuation", "settings": { "foreground": "#00ff00" } },
            { "scope": "markup.inline.raw", "settings": { "foreground": "#0000ff" } },
            { "scope": "string", "settings": { "foreground": "#ffff00" } },
            { "scope": "comment", "settings": { "foreground": "#00ffff" } },
            { "scope": "atx_h1_marker", "settings": { "foreground": "#ff00ff" } },
            { "scope": "atx_heading", "settings": { "foreground": "#ff00ff" } },
            { "scope": "atx_h1", "settings": { "foreground": "#ff00ff" } },
            { "scope": "#", "settings": { "foreground": "#ff00ff" } },
            { "scope": "inline", "settings": { "foreground": "#aa00aa" } },
            { "scope": "paragraph", "settings": { "foreground": "#aa00aa" } }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun rewritesFencedMarkdownBlock() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val highlighter = SyntaxHighlighter(engine) { VsCodeThemeParser.parse(testThemeJson) }
        val input = """<p>before</p>
<pre><code class="language-markdown"># Heading
content</code></pre>
<p>after</p>"""

        val output = PreviewCodeBlockHighlighter.rewrite(input, highlighter)

        // The fenced block's inner text should be wrapped in tok- spans.
        // If buildTokenSpans is stubbed to return `text` verbatim (the
        // mutation in Phase 10.5), no `tok-` class will appear and this
        // assertion FAILS — the anti-bluff guarantee.
        assertTrue(
            "expected at least one tok-* span in rewritten output, got: $output",
            output.contains("<span class=\"tok-"),
        )
        // Surrounding HTML is preserved byte-identically.
        assertTrue("expected <p>before</p> preserved", output.contains("<p>before</p>"))
        assertTrue("expected <p>after</p> preserved", output.contains("<p>after</p>"))
    }

    @Test
    fun preservesBlockWhenFormatDisabled() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(emptySet())  // markdown disabled
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val highlighter = SyntaxHighlighter(engine) { VsCodeThemeParser.parse(testThemeJson) }
        val input = """<pre><code class="language-markdown">x</code></pre>"""

        val output = PreviewCodeBlockHighlighter.rewrite(input, highlighter)

        assertFalse(
            "disabled format should not produce tok-* spans, got: $output",
            output.contains("tok-"),
        )
        assertTrue(
            "disabled-format block should be preserved verbatim, got: $output",
            output.contains("<pre><code class=\"language-markdown\">x</code></pre>"),
        )
    }

    @Test
    fun preservesBlockWithoutLangClass() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val highlighter = SyntaxHighlighter(engine) { VsCodeThemeParser.parse(testThemeJson) }
        val input = """<pre><code>plain code</code></pre>"""

        val output = PreviewCodeBlockHighlighter.rewrite(input, highlighter)

        assertFalse(
            "no-lang block should not produce tok-* spans, got: $output",
            output.contains("tok-"),
        )
        assertTrue(
            "no-lang block should be preserved verbatim, got: $output",
            output.contains("<pre><code>plain code</code></pre>"),
        )
    }
}
