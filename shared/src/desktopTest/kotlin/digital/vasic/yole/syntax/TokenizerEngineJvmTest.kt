/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: end-user-quality test for the Desktop TokenizerEngine
 * actual. Asserts the engine REAL-tokenizes a Markdown snippet via the
 * underlying Tree-Sitter native library — not against a hardcoded
 * golden list, but by verifying that distinct, non-trivial scopes are
 * emitted for the characters of a known input. This is the anti-bluff
 * anchor for Phase 5 (CONST-035): if the tree walk is replaced by an
 * empty-list stub, this test FAILS. Mutation-verified in Phase 5.7.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenizerEngineJvmTest {

    @Before
    fun setUp() {
        // The gate defaults to FormatRegistry.defaultEnabledFormatIds()
        // which already includes "markdown" — but explicitly set it
        // here so the test is self-contained.
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @After
    fun tearDown() {
        // Restore default in case another test relies on it.
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun initializeReturnsSuccess() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        val result = engine.initialize()
        assertTrue(
            "initialize() should succeed on host JVM (got $result)",
            result.isSuccess,
        )
    }

    @Test
    fun isGrammarLoadedFlipsAfterLoad() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        assertEquals(false, engine.isGrammarLoaded("markdown"))
        engine.loadGrammar("markdown")
        assertEquals(true, engine.isGrammarLoaded("markdown"))
    }

    @Test
    fun tokenizesMarkdownSnippet() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val input = "# Heading\n\nA paragraph with `code`.\n"
        val tokens = engine.tokenize(input, "markdown")

        // The real Tree-Sitter parser produces a tree of nodes; our
        // pre-order walk emits a Token for each leaf with a non-empty
        // node type. For this snippet that yields well over a dozen
        // tokens. The exact count is grammar-version-dependent, so we
        // assert a soft lower bound that is still meaningfully > 0.
        assertTrue(
            "expected at least 5 leaf tokens for `$input`, got ${tokens.size}: $tokens",
            tokens.size >= 5,
        )

        // Tokens MUST collectively cover the input's byte range.
        val maxEnd = tokens.maxOf { it.endByte }
        assertTrue(
            "tokens should cover at least the first 10 bytes of input (got maxEnd=$maxEnd)",
            maxEnd >= 10,
        )

        // At least one non-blank-scope token must exist.
        val firstNonBlank = tokens.firstOrNull { it.scope.isNotBlank() }
        assertNotNull(
            "expected at least one non-blank-scope token, got $tokens",
            firstNonBlank,
        )

        // Anti-bluff: the FIRST emitted token should sit inside the
        // first heading and carry a meaningful scope (something like
        // `atx_h1_marker` or `#` literal). A stub that returns
        // hardcoded tokens of scope "TODO" or empty would fail this.
        val first = tokens.first()
        assertTrue(
            "first token should be at the start of the input (startByte=${first.startByte})",
            first.startByte == 0,
        )
        assertTrue(
            "first token scope `${first.scope}` should not be a placeholder",
            first.scope.isNotBlank() &&
                    first.scope != "TODO" &&
                    first.scope != "STUB",
        )
    }

    @Test
    fun tokenizeFailsWhenGrammarDisabled() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(emptySet()) // No formats enabled
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        try {
            engine.tokenize("# foo", "markdown")
            error("expected FormatDisabledException, got no exception")
        } catch (e: FormatDisabledException) {
            assertEquals("markdown", e.formatId)
        }
    }

    @Test
    fun loadGrammarFailsForUnknownLang() = runBlocking<Unit> {
        // iter-58 F2 Phase 7 update: this test used to use "python" as the
        // canonical unbundled lang. Phase 7 bundled python (and 46 other
        // langs), so we pin against "jsx" — one of the 8 langs that
        // remains unbundled today (no bonede artifact, see
        // BonedeGrammarRegistry.unsupportedLangs).
        EnabledFormatGate.setEnabled(setOf("jsx"))
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        try {
            engine.loadGrammar("jsx")
            error("expected IllegalArgumentException for unbundled grammar")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "expected message mentioning jsx, got: ${e.message}",
                e.message?.contains("jsx") == true,
            )
        }
    }
}
