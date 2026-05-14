/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 6: Kotlin/Wasm external declarations for the
 * vscode-textmate + vscode-oniguruma npm packages. These are consumed
 * from Kotlin/Wasm via @JsModule, resolved by webpack at build time.
 *
 * Package versions are pinned in shared/build.gradle.kts:
 *   - vscode-textmate@9.2.0  (MIT, Microsoft)
 *   - vscode-oniguruma@2.0.1 (MIT, Microsoft)
 *
 * The API surface declared here covers ONLY what TokenizerEngine.wasmJs
 * needs to tokenize markdown line-by-line:
 *   - Registry construction with an onigLib promise
 *   - addGrammar() to register an inline grammar JSON
 *   - grammarForScopeName() to retrieve an IGrammar for tokenization
 *   - IGrammar.tokenizeLine() to actually tokenize a line
 *   - loadWASM()/createOnigScanner/createOnigString from vscode-oniguruma
 *
 * Anti-bluff note (CONST-035): these declarations are syntactic only —
 * Kotlin treats every method as `external` and the JS implementation
 * carries the behavior. Mutation-verification still applies at the
 * TokenizerEngine.tokenize() level.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlin.js.Promise

/* -------------------------------------------------------------------
 * vscode-textmate (https://github.com/microsoft/vscode-textmate)
 * ------------------------------------------------------------------- */

/**
 * Top-level Registry class from vscode-textmate. Constructed with a
 * [RegistryOptions] carrying the onigLib promise; subsequently used to
 * register grammars (via [addGrammar]) and retrieve them (via
 * [grammarForScopeName]).
 */
@JsModule("vscode-textmate")
@Suppress("UnusedPrivateProperty")
external class Registry(options: RegistryOptions) : JsAny {
    fun addGrammar(rawGrammar: JsAny): Promise<IGrammar>
    fun grammarForScopeName(scopeName: String): Promise<IGrammar?>
}

/**
 * Registry options passed to the [Registry] constructor. The required
 * property is `onigLib` — a promise resolving to an Oniguruma binding
 * (typically the vscode-oniguruma module after loadWASM() completes).
 */
external interface RegistryOptions : JsAny {
    var onigLib: Promise<JsAny>
}

/**
 * A tokenizable TextMate grammar. The relevant entry point is
 * [tokenizeLine] which returns a result containing tokens and the
 * rule-stack that should be threaded into the next call (for stateful
 * multi-line grammars).
 */
external interface IGrammar : JsAny {
    fun tokenizeLine(lineText: String, prevState: JsAny?): ITokenizeLineResult
}

/**
 * Result of tokenizing one line. Tokens are flat (no nesting); each
 * token spans `[startIndex, endIndex)` and carries the scope stack at
 * that point. [ruleStack] should be passed back as `prevState` to the
 * next line's [IGrammar.tokenizeLine] call.
 */
external interface ITokenizeLineResult : JsAny {
    val tokens: JsArray<ITextMateToken>
    val ruleStack: JsAny
}

/**
 * A single token within an [ITokenizeLineResult]. The [scopes] array
 * is ordered most-general-first; the most-specific scope is the last
 * element. Yole's downstream ScopeMapper (Phase 8) consumes the
 * most-specific scope.
 */
external interface ITextMateToken : JsAny {
    val startIndex: Int
    val endIndex: Int
    val scopes: JsArray<JsString>
}

/* -------------------------------------------------------------------
 * vscode-oniguruma (https://github.com/microsoft/vscode-oniguruma)
 * ------------------------------------------------------------------- */

/**
 * The vscode-oniguruma module surface. `loadWASM()` MUST be called
 * (and the returned Promise awaited) before [createOnigScanner] /
 * [createOnigString] are used — internally the regex engine is the
 * Oniguruma C library compiled to WebAssembly.
 */
@JsModule("vscode-oniguruma")
@Suppress("UnusedParameter")
external object Oniguruma : JsAny {
    fun loadWASM(data: JsAny): Promise<JsAny?>
    fun createOnigScanner(sources: JsArray<JsString>): JsAny
    fun createOnigString(str: String): JsAny
}

/* -------------------------------------------------------------------
 * Glue helpers (small JS shims) implemented in TextMateGlue.kt below
 * via @JsFun — these aren't part of the vscode-textmate API surface
 * but are required to bridge raw bytes / async JSON parsing.
 * ------------------------------------------------------------------- */

/**
 * Wraps the Oniguruma module itself into a Promise so it can be used as
 * the `onigLib` property of [RegistryOptions]. Uses a small @JsFun shim
 * because Kotlin/Wasm doesn't expose `Promise.resolve()` directly.
 */
internal fun onigLibPromise(): Promise<JsAny> =
    promiseResolveJs(Oniguruma)

@JsFun("(value) => Promise.resolve(value)")
internal external fun promiseResolveJs(value: JsAny): Promise<JsAny>

/**
 * `fetch(url).then(r => r.arrayBuffer())` — for loading the
 * Oniguruma .wasm binary at runtime.
 */
@JsFun("(url) => fetch(url).then(r => r.arrayBuffer())")
internal external fun fetchArrayBuffer(url: String): Promise<JsAny>

/**
 * `fetch(url).then(r => r.text())` — for loading the markdown
 * grammar JSON.
 */
@JsFun("(url) => fetch(url).then(r => r.text())")
internal external fun fetchText(url: String): Promise<JsString>

/**
 * `JSON.parse(text)` — parses a JSON string into a JS object suitable
 * for passing into [Registry.addGrammar].
 */
@JsFun("(text) => JSON.parse(text)")
internal external fun jsonParse(text: String): JsAny
