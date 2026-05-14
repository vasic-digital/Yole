/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: placeholder Wasm actual for the platform-specific
 * tokenizer engine. The real implementation lands in Phase 6 via
 * vscode-textmate through JS interop. Phase 5 ships only an honest
 * `Result.failure` from `initialize()` so the shared KMP module compiles
 * for the wasmJs target without producing fake tokens (CONST-035).
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * Wasm (web) placeholder actual for [TokenizerEngine]. All calls except
 * [initialize] throw `IllegalStateException` because the engine never
 * successfully initializes in Phase 5. [initialize] returns
 * `Result.failure` so callers gracefully fall back to plain text.
 *
 * Replaced in Phase 6 with a real vscode-textmate JS interop binding.
 */
actual class TokenizerEngine actual constructor() {
    actual suspend fun initialize(): Result<Unit> =
        Result.failure(NotImplementedError("TokenizerEngine Wasm actual lands in Phase 6"))

    actual suspend fun loadGrammar(lang: String): Unit =
        error("TokenizerEngine Wasm actual lands in Phase 6")

    actual suspend fun tokenize(text: String, lang: String): List<Token> =
        error("TokenizerEngine Wasm actual lands in Phase 6")

    actual fun isGrammarLoaded(lang: String): Boolean = false
}
