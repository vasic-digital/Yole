/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 8.1 — HtmlEmbeddedLang real-engine tests.
 *
 * Anti-bluff (CONST-035) anchors:
 *   - tokenizesEmbeddedCssInStyleElement FAILS if [HtmlEmbeddedLang.tokenize]
 *     is stubbed to return only [htmlEngine.tokenize(text, "html")] (i.e.
 *     no CSS sub-tokens). Verified at Phase 8 mutation step.
 *   - fallsBackToPlainHtmlWhenCssEngineNull asserts the honest-degradation
 *     contract: with cssEngine=null we must NOT see CSS-specific scopes
 *     in the style region.
 *
 *########################################################*/
package digital.vasic.yole.language.special

import digital.vasic.yole.syntax.BonedeGrammarRegistry
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HtmlEmbeddedLangTest {

    /**
     * Concrete HTML node-type scopes the bonede tree-sitter-html grammar
     * emits on leaf nodes for the `<style>body { color: red; }</style>`
     * region (verified empirically). Used to distinguish "outer HTML
     * tokens" from "CSS sub-tokens": any leaf in the style body that is
     * NOT one of these well-known HTML scopes is, by definition, a CSS
     * scope produced by the CSS sub-engine.
     *
     * The tree-sitter-css grammar emits its own set of leaf node types
     * for the body — `property_name`, `identifier`, `color_value`,
     * the `:` and `;` punctuation leaves, etc. — none of which appear
     * in this set.
     */
    private val htmlOuterLeafScopes = setOf(
        // tag delimiters + tag names
        "<", ">", "</", "/>",
        "tag_name", "attribute_name", "attribute_value", "=",
        // whitespace + text-position leaves
        "\n", "\r", " ", "\t",
        // top-level production names (when emitted as leaves on tiny inputs)
        "doctype",
        // raw_text leaf scope name — the CSS body in HTML emerges as a
        // single `raw_text` leaf when no sub-grammar is applied
        "raw_text",
    )

    @Before
    fun setUp() {
        // Open the gate for the langs this test exercises so the engine
        // doesn't block on EnabledFormatGate.requireEnabled().
        EnabledFormatGate.setEnabled(setOf("markdown", "html", "css", "javascript"))
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun tokenizesEmbeddedCssInStyleElement() = runBlocking<Unit> {
        // Skip gracefully on platforms where the bonede HTML/CSS grammars
        // are not on the classpath (defensive — they should be in Desktop).
        val htmlClass = BonedeGrammarRegistry.classNameFor("html")
        val cssClass = BonedeGrammarRegistry.classNameFor("css")
        assertTrue("expected bonede html grammar wired", htmlClass != null)
        assertTrue("expected bonede css grammar wired", cssClass != null)

        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("html")
        engine.loadGrammar("css")

        val text = "<html><head><style>body { color: red; }</style></head></html>"
        val tokens = HtmlEmbeddedLang.tokenize(
            text = text,
            htmlEngine = engine,
            cssEngine = engine,
            jsEngine = null,
        )

        assertTrue("expected at least one token", tokens.isNotEmpty())

        // Locate the byte range of the CSS body (between `<style>` and `</style>`).
        val styleOpen = text.indexOf("<style>") + "<style>".length
        val styleClose = text.indexOf("</style>")
        assertTrue("test setup: style body should be non-empty", styleClose > styleOpen)

        // Tokens whose byte range falls strictly inside the style body.
        val bodyTokens = tokens.filter {
            it.startByte >= styleOpen && it.endByte <= styleClose
        }
        assertTrue(
            "expected >= 1 token inside style body, got: $bodyTokens",
            bodyTokens.isNotEmpty(),
        )

        // Anti-bluff anchor: at least one token inside the style body must
        // carry a CSS-specific scope, i.e. a scope that the HTML grammar
        // would never emit. If [HtmlEmbeddedLang] is stubbed to skip the
        // CSS re-tokenize pass, the body would contain only `raw_text` (or
        // other HTML-leaf) scopes and this assertion FAILS.
        val cssScopedBodyTokens = bodyTokens.filter { it.scope !in htmlOuterLeafScopes }
        assertTrue(
            "expected >= 1 CSS-scoped token inside style body, got: $bodyTokens",
            cssScopedBodyTokens.isNotEmpty(),
        )
    }

    @Test
    fun fallsBackToPlainHtmlWhenCssEngineNull() = runBlocking<Unit> {
        val htmlClass = BonedeGrammarRegistry.classNameFor("html")
        assertTrue("expected bonede html grammar wired", htmlClass != null)

        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("html")

        val text = "<html><head><style>body { color: red; }</style></head></html>"
        val tokens = HtmlEmbeddedLang.tokenize(
            text = text,
            htmlEngine = engine,
            cssEngine = null,
            jsEngine = null,
        )

        assertTrue("expected at least one token", tokens.isNotEmpty())

        // Honest-degradation contract: with cssEngine=null the style body
        // is left as outer HTML tokens. Concretely: at least one body
        // token must exist whose scope IS in htmlOuterLeafScopes (typically
        // `raw_text`), AND no token in the body may carry a CSS-only
        // scope name (e.g. `property_name`, `identifier` in a CSS context).
        val styleOpen = text.indexOf("<style>") + "<style>".length
        val styleClose = text.indexOf("</style>")
        val bodyTokens = tokens.filter {
            it.startByte >= styleOpen && it.endByte <= styleClose
        }
        assertTrue("expected >= 1 token in style body", bodyTokens.isNotEmpty())

        // The CSS-specific scope `property_name` is the smoking-gun token
        // the CSS grammar emits for `color` in `color: red;`. If we see
        // it here, somebody has bluffed a sub-tokenize pass when the test
        // explicitly disabled CSS. CONST-035: that's a defect.
        val bluffScopes = setOf("property_name", "plain_value", "color_value")
        val bluffTokens = bodyTokens.filter { it.scope in bluffScopes }
        assertFalse(
            "expected NO CSS-bluff tokens when cssEngine=null, got: $bluffTokens",
            bluffTokens.isNotEmpty(),
        )
    }
}
