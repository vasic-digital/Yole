/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 6: test for the Wasm tokenizer engine actual.
 * Exercises the full end-to-end path: initialize Oniguruma + Registry,
 * load the bundled markdown grammar, tokenize a small markdown snippet,
 * and assert that real (non-empty, scope-bearing) tokens come back.
 *
 * Anti-bluff anchor (CONST-035): this test MUST fail when the inner
 * per-line emit loop in [TokenizerEngine.tokenize] is stubbed to a
 * no-op. Mutation step in Phase 6.7 verifies that property.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Wasm test. kotlinx-coroutines-test has no WASM variant in 1.9.0, so
 * we expose a `Promise<JsAny?>` from each suspending test body using
 * the standard `GlobalScope.promise { ... }` bridge. Karma's Kotlin
 * test framework adapter recognizes a Promise return and awaits it
 * before reporting PASS/FAIL.
 */
class TokenizerEngineWasmTest {

    /**
     * End-to-end smoke test. Initializes the engine, loads the bundled
     * markdown grammar, tokenizes a 3-line markdown snippet, asserts
     * the result has at least one scope-bearing token at the heading
     * marker location.
     *
     * Anti-bluff: a stubbed [TokenizerEngine.tokenize] returning an
     * empty list MUST fail this test on the `tokens.isNotEmpty()`
     * assertion.
     */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun tokenizesMarkdownSnippet(): Promise<JsAny?> = GlobalScope.promise {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        val engine = TokenizerEngine()
        val initResult = engine.initialize()
        assertTrue(
            initResult.isSuccess,
            "TokenizerEngine.initialize() returned failure: $initResult"
        )
        engine.loadGrammar("markdown")
        assertTrue(
            engine.isGrammarLoaded("markdown"),
            "markdown grammar should report loaded after loadGrammar()"
        )

        val snippet = "# Heading\n\nA paragraph.\n"
        val tokens = engine.tokenize(snippet, "markdown")
        assertTrue(
            tokens.isNotEmpty(),
            "expected non-empty tokens for `$snippet`, got ${tokens.size}"
        )
        // At least one token MUST carry a non-blank scope (vscode-textmate
        // always emits a `source.gfm` or `text.html.markdown` root scope
        // for every span, so the most-specific scope is never blank for
        // markdown).
        assertNotNull(
            tokens.firstOrNull { it.scope.isNotBlank() },
            "expected at least one token with a non-blank scope, " +
                "got: ${tokens.map { it.scope }}"
        )
        null
    }
}
