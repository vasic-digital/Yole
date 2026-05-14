/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: placeholder iOS actual for the platform-specific
 * tokenizer engine. The real implementation lands in Phase 7 via
 * Tree-Sitter through Kotlin/Native cinterop. Phase 5 ships only an
 * honest `Result.failure` from `initialize()` so the shared KMP module
 * compiles for the iOS targets without producing fake tokens
 * (CONST-035 anti-bluff covenant).
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * iOS placeholder actual for [TokenizerEngine]. All calls except
 * [initialize] throw `IllegalStateException` because the engine never
 * successfully initializes in Phase 5. [initialize] returns
 * `Result.failure` so callers gracefully fall back to plain text.
 *
 * Replaced in Phase 7 with a real Kotlin/Native cinterop binding.
 */
actual class TokenizerEngine actual constructor() {
    actual suspend fun initialize(): Result<Unit> =
        Result.failure(NotImplementedError("TokenizerEngine iOS actual lands in Phase 7"))

    actual suspend fun loadGrammar(lang: String): Unit =
        error("TokenizerEngine iOS actual lands in Phase 7")

    actual suspend fun tokenize(text: String, lang: String): List<Token> =
        error("TokenizerEngine iOS actual lands in Phase 7")

    actual fun isGrammarLoaded(lang: String): Boolean = false
}
