/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 8.2 — MarkdownCodeFences real-engine tests.
 *
 * Anti-bluff (CONST-035) anchors:
 *   - tokenizesKotlinFenceWithKotlinEngine FAILS if
 *     [MarkdownCodeFences.tokenize] is stubbed to return only
 *     [markdownEngine.tokenize(text, "markdown")] (no sub-tokens).
 *     Verified at Phase 8 mutation step.
 *   - fallsBackToPlainMarkdownWhenSubEngineMissing asserts the honest-
 *     degradation contract: with subEnginesByLang=emptyMap() we must
 *     NOT see kotlin-specific scopes in the fence body.
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

class MarkdownCodeFencesTest {

    /**
     * The set of leaf node-type scopes the bonede tree-sitter-markdown
     * grammar emits for the body of an unlabeled fenced code block. When
     * MarkdownCodeFences decides not to delegate (lang tag absent OR
     * no sub-engine for the lang), the fence body falls back to one of
     * these scopes. Any leaf scope inside the fence body that is NOT in
     * this set MUST have come from a sub-engine.
     */
    private val markdownFenceBodyLeafScopes = setOf(
        // Scopes the bundled tree-sitter-markdown 0.7.1 grammar emits for
        // a fenced code block — verified empirically by dumping engine
        // output for `# Title\n\n```kotlin\nfun foo() {}\n```\n`:
        //   - `19..31 text` for the fence body when no sub-engine is wired.
        //   - plus the fence delimiter / info-string scopes around it.
        "code_fence_content",
        "fenced_code_block_delimiter",
        "info_string",
        "language",
        "text",
        "\n", "\r",
        "raw_text",
        "block_continuation",
    )

    @Before
    fun setUp() {
        EnabledFormatGate.setEnabled(setOf("markdown", "kotlin"))
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun tokenizesKotlinFenceWithKotlinEngine() = runBlocking<Unit> {
        val kotlinClass = BonedeGrammarRegistry.classNameFor("kotlin")
        assertTrue("expected bonede kotlin grammar wired", kotlinClass != null)

        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")
        engine.loadGrammar("kotlin")

        val text = "# Title\n\n```kotlin\nfun foo() {}\n```\n"
        val tokens = MarkdownCodeFences.tokenize(
            text = text,
            markdownEngine = engine,
            subEnginesByLang = mapOf("kotlin" to engine),
        )
        assertTrue("expected >= 1 token", tokens.isNotEmpty())

        // Compute the byte range of the fence body (between the newline
        // after "```kotlin" and the newline before "```\n").
        val bodyStart = text.indexOf("```kotlin\n") + "```kotlin\n".length
        val bodyEnd = text.lastIndexOf("\n```")
        assertTrue("test setup: fence body non-empty", bodyEnd > bodyStart)
        // "fun foo() {}" — the textual body
        assertTrue(
            "test setup: body is the kotlin snippet",
            text.substring(bodyStart, bodyEnd) == "fun foo() {}",
        )

        val bodyTokens = tokens.filter {
            it.startByte >= bodyStart && it.endByte <= bodyEnd
        }
        assertTrue(
            "expected >= 1 token inside fence body, got: $bodyTokens",
            bodyTokens.isNotEmpty(),
        )

        // Anti-bluff anchor: at least one body token must carry a kotlin-
        // specific scope, i.e. one the markdown grammar would never emit.
        // We accept any leaf scope NOT in markdownFenceBodyLeafScopes —
        // the bonede tree-sitter-kotlin grammar emits scopes like `fun`,
        // `simple_identifier`, `(`, `)`, `{`, `}`, etc. — none of which
        // overlap with markdownFenceBodyLeafScopes.
        val nonMarkdownBodyTokens = bodyTokens.filter {
            it.scope !in markdownFenceBodyLeafScopes
        }
        assertTrue(
            "expected >= 1 kotlin-scoped token inside fence body, got: $bodyTokens",
            nonMarkdownBodyTokens.isNotEmpty(),
        )
    }

    @Test
    fun fallsBackToPlainMarkdownWhenSubEngineMissing() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val text = "# Title\n\n```kotlin\nfun foo() {}\n```\n"
        val tokens = MarkdownCodeFences.tokenize(
            text = text,
            markdownEngine = engine,
            subEnginesByLang = emptyMap(),
        )
        assertTrue("expected >= 1 token", tokens.isNotEmpty())

        val bodyStart = text.indexOf("```kotlin\n") + "```kotlin\n".length
        val bodyEnd = text.lastIndexOf("\n```")
        val bodyTokens = tokens.filter {
            it.startByte >= bodyStart && it.endByte <= bodyEnd
        }
        assertTrue("expected >= 1 token in fence body", bodyTokens.isNotEmpty())

        // Honest-degradation: NO kotlin-specific tokens may appear. The
        // smoking-gun is `simple_identifier` — a kotlin grammar leaf the
        // markdown grammar never produces. If it leaks through here,
        // somebody bluffed a sub-tokenize when no sub-engine was wired.
        val bluffScopes = setOf("simple_identifier", "fun", "function_declaration")
        val bluffTokens = bodyTokens.filter { it.scope in bluffScopes }
        assertFalse(
            "expected NO kotlin-bluff tokens with empty subEnginesByLang, got: $bluffTokens",
            bluffTokens.isNotEmpty(),
        )
    }
}
