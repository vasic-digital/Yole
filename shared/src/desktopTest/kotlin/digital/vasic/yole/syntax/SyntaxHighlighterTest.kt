/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: end-to-end test for SyntaxHighlighter on Desktop.
 *   1. Real engine + real theme → AnnotatedString with > 0 SpanStyles
 *      (i.e., highlighting actually colored something).
 *   2. Disabled grammar → AnnotatedString with 0 SpanStyles (graceful
 *      degradation per spec §4 error table).
 *
 * Anti-bluff (CONST-035): if AnnotatedStringBuilder.build is mutated to
 *   return AnnotatedString(text) (no spans), test #1 FAILS.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.theme.VsCodeThemeParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyntaxHighlighterTest {

    // Theme keyed on a mix of:
    //   - VS Code TextMate scopes (markup.heading, punctuation) for tokens
    //     that map through ScopeMapper (e.g., "heading" → "markup.heading").
    //   - Tree-Sitter / vscode-textmate-native scopes that ScopeMapper
    //     passes through unchanged (atx_h1_marker, atx_heading, paragraph,
    //     inline, "#") — Theme.tokenColor will find these by identity.
    // The combination guarantees at least one Token's scope yields a
    // color regardless of whether the engine emits Markdown-grammar
    // node types directly or post-mapped TextMate scopes.
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
    fun highlightingProducesNonEmptyAnnotatedString() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val theme = VsCodeThemeParser.parse(testThemeJson)
        val highlighter = SyntaxHighlighter(engine) { theme }

        val input = "# Hello\n\nText.\n"
        val annotated = highlighter.highlight(input, "markdown")

        assertEquals(
            "AnnotatedString.text should match input",
            input,
            annotated.text,
        )
        // The Tree-Sitter Markdown grammar should produce at least one
        // token whose scope (after ScopeMapper + Theme hierarchical
        // fallback) matches one of the colors in the test theme. If
        // AnnotatedStringBuilder.build is stubbed to return
        // AnnotatedString(text) only, spanStyles.size == 0 here.
        assertTrue(
            "expected at least 1 span style for highlighted markdown, got ${annotated.spanStyles.size}",
            annotated.spanStyles.isNotEmpty(),
        )
    }

    @Test
    fun disabledLangReturnsUnstyledText() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(emptySet())
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val theme = VsCodeThemeParser.parse(testThemeJson)
        val highlighter = SyntaxHighlighter(engine) { theme }

        val input = "# Hello"
        val annotated = highlighter.highlight(input, "markdown")
        assertEquals(input, annotated.text)
        assertEquals(
            "expected zero span styles when grammar disabled",
            0,
            annotated.spanStyles.size,
        )
    }

    @Test
    fun tokensApi_returnsEmptyWhenDisabled() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(emptySet())
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val highlighter = SyntaxHighlighter(engine) {
            VsCodeThemeParser.parse(testThemeJson)
        }
        val toks = highlighter.tokens("# x", "markdown")
        assertTrue("disabled grammar should return empty tokens, got $toks", toks.isEmpty())
    }

    @Test
    fun tokensApi_returnsNonEmptyWhenEnabled() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        val highlighter = SyntaxHighlighter(engine) {
            VsCodeThemeParser.parse(testThemeJson)
        }
        val toks = highlighter.tokens("# Heading\n\nBody.\n", "markdown")
        // The Tree-Sitter Markdown leaf walk emits a small number of
        // tokens for this short snippet; we assert >= 1 (anti-bluff:
        // any empty-stub will fail this) and leave the exact-count
        // contract to TokenizerEngineJvmTest.tokenizesMarkdownSnippet.
        assertTrue(
            "expected non-empty tokens from Tree-Sitter, got ${toks.size}",
            toks.isNotEmpty(),
        )
    }
}
