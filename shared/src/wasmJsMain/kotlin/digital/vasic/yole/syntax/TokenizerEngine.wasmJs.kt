/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 6: Wasm (web) actual for the platform-specific tokenizer
 * engine. Backed by vscode-textmate 9.x + vscode-oniguruma 2.x consumed
 * via Kotlin/Wasm JS interop. The TextMate Registry is constructed once
 * per [TokenizerEngine] instance; grammars are loaded lazily on first
 * use and cached for subsequent tokenize() calls.
 *
 * Data flow (per research-report.md §3 + spec data-flow §5.1):
 *   1. initialize()
 *      - fetch onig.wasm bytes
 *      - Oniguruma.loadWASM(bytes) → resolves the regex engine
 *      - new Registry({ onigLib: Promise.resolve(Oniguruma) })
 *   2. loadGrammar(lang)
 *      - fetch /grammars/<lang>.tmLanguage.json
 *      - JSON.parse → registry.addGrammar(raw) → IGrammar
 *      - cache by lang
 *   3. tokenize(text, lang)
 *      - split text into lines
 *      - for each line: grammar.tokenizeLine(line, prevState)
 *      - emit one Yole [Token] per TextMate token, using the
 *        most-specific scope (last element of `scopes`)
 *      - thread `ruleStack` into the next line's call for stateful
 *        markdown (fenced-code-block continuations, etc.)
 *
 * Anti-bluff anchor (CONST-035): the inner per-line emit loop
 * (`for (i in 0 until len)`) MUST run for the test to pass. Stubbing
 * it to a no-op causes [tokenize] to return an empty list and the
 * mutation-verification test FAILS.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlinx.coroutines.await

/**
 * Wasm (web) actual for [TokenizerEngine]. Real-tokenizes against
 * vscode-textmate via Kotlin/Wasm @JsModule interop — no fallbacks,
 * no stubs.
 *
 * Grammar resources are served from `/grammars/<lang>.tmLanguage.json`
 * relative to the page origin. Oniguruma's `onig.wasm` is loaded from
 * the published npm package path under `/node_modules/vscode-oniguruma/
 * release/onig.wasm` — webpack rewrites the asset URL at build time.
 *
 * Threading: Wasm runs single-threaded in browsers; coroutine context
 * is the caller's. The instance MUST NOT be shared across distinct
 * coroutines that may interleave [tokenize] calls (the underlying
 * [IGrammar] is stateless per call but the cached [loadedGrammars] map
 * is not synchronized — fine for the per-document UI model where one
 * engine = one editor surface).
 */
actual class TokenizerEngine actual constructor() {

    /** Lazily-set after [initialize] succeeds. */
    private var registry: Registry? = null

    /** Cached `IGrammar` instances keyed by Yole's `lang` string. */
    private val loadedGrammars: MutableMap<String, IGrammar> = mutableMapOf()

    /** Set true after [initialize] succeeds. */
    private var initialized: Boolean = false

    /**
     * One-shot startup. Fetches the Oniguruma .wasm binary, hands it to
     * `Oniguruma.loadWASM`, and constructs the vscode-textmate Registry
     * wired against the resolved Oniguruma module.
     *
     * Returns `Result.failure` (not throws) on any error so callers can
     * gracefully fall back to plain text per spec §4.
     */
    actual suspend fun initialize(): Result<Unit> {
        if (initialized) return Result.success(Unit)
        return try {
            // 1. Load the Oniguruma WebAssembly binary. The path is
            //    relative to the dev server root; webpack's static
            //    asset handling exposes node_modules/* under the
            //    standard module-resolution path.
            val onigWasmBytes = fetchArrayBuffer("vscode-oniguruma/release/onig.wasm").await<JsAny>()
            Oniguruma.loadWASM(onigWasmBytes).await<JsAny?>()

            // 2. Construct the Registry, handing it a Promise that
            //    resolves to the (already-WASM-initialized) Oniguruma
            //    module. The Registry uses this to compile grammar
            //    regexes lazily on first tokenize().
            val opts = createRegistryOptions(onigLibPromise())
            registry = Registry(opts)
            initialized = true
            Result.success(Unit)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Eagerly load the grammar named [lang]. Fetches the bundled
     * `grammars/<lang>.tmLanguage.json` file, JSON-parses it, and
     * registers it with the Registry. Cached by `lang` for subsequent
     * [tokenize] calls.
     *
     * Phase 6 only bundles `markdown`; other formats throw
     * [IllegalArgumentException].
     */
    actual suspend fun loadGrammar(lang: String) {
        EnabledFormatGate.requireEnabled(lang)
        check(initialized) { "TokenizerEngine.initialize() must be called first" }
        if (loadedGrammars.containsKey(lang)) return
        require(lang == "markdown") {
            "grammar `$lang` is not bundled in Phase 6 (markdown only)"
        }
        val r = registry ?: error("Registry was not constructed despite initialized=true")
        val json = fetchText("grammars/$lang.tmLanguage.json").await<JsString>().toString()
        val raw = jsonParse(json)
        val grammar = r.addGrammar(raw).await<IGrammar>()
        loadedGrammars[lang] = grammar
    }

    /**
     * Tokenize [text] using the previously-loaded grammar for [lang].
     * If the grammar is not yet loaded an implicit [loadGrammar] is
     * performed.
     *
     * Implementation: line-at-a-time tokenization, threading the
     * `ruleStack` from the previous line's [ITokenizeLineResult] into
     * the next call so multi-line markdown constructs (fenced code
     * blocks, block quotes spanning lines, etc.) tokenize correctly.
     *
     * Byte offsets here are 16-bit string offsets ("UTF-16 code unit
     * offsets") because JS strings are UTF-16. The downstream
     * SyntaxHighlighter (Phase 8) treats these as Yole's canonical
     * offsets for the Wasm target. Mixed JVM/Wasm callers should
     * normalize via [Token].
     */
    actual suspend fun tokenize(text: String, lang: String): List<Token> {
        EnabledFormatGate.requireEnabled(lang)
        check(initialized) { "TokenizerEngine.initialize() must be called first" }
        val grammar = loadedGrammars[lang] ?: run {
            loadGrammar(lang)
            loadedGrammars[lang]
                ?: error("grammar `$lang` failed to load")
        }
        val out = mutableListOf<Token>()
        var prevState: JsAny? = null
        var charOffset = 0
        // Use line-by-line iteration. Preserve \n separators in the offset.
        val lines = text.split('\n')
        for ((index, line) in lines.withIndex()) {
            val result = grammar.tokenizeLine(line, prevState)
            emitLineTokens(result, charOffset, out)
            prevState = result.ruleStack
            // Advance by line length + 1 for the consumed '\n', except
            // for the final line which has no trailing newline.
            charOffset += line.length + if (index < lines.size - 1) 1 else 0
        }
        return out
    }

    actual fun isGrammarLoaded(lang: String): Boolean = loadedGrammars.containsKey(lang)

    /**
     * Emits one Yole [Token] per scope-bearing TextMate token in
     * [result], using the most-specific scope (last element of the
     * token's `scopes` array). Tokens whose scope list is empty or
     * whose most-specific scope is `null` are silently dropped — this
     * happens for whitespace-only TextMate tokens that never matched
     * a grammar pattern.
     *
     * Anti-bluff anchor (CONST-035): this body is the mutation target.
     * Replacing it with `return` causes `tokenize()` to emit zero
     * tokens and the `tokenizesMarkdownSnippet` test MUST fail.
     */
    private fun emitLineTokens(
        result: ITokenizeLineResult,
        charOffset: Int,
        out: MutableList<Token>,
    ) {
        val tokens = result.tokens
        val tokenCount = tokens.length
        for (i in 0 until tokenCount) {
            val tok = tokens[i]
            val scope = tok?.let { resolveMostSpecificScope(it) }
            if (tok != null && scope != null) {
                out += Token(
                    startByte = charOffset + tok.startIndex,
                    endByte = charOffset + tok.endIndex,
                    scope = scope,
                )
            }
        }
    }

    /**
     * Returns the most-specific scope name (last element of the
     * token's `scopes` array) or `null` if the array is empty.
     */
    private fun resolveMostSpecificScope(tok: ITextMateToken): String? {
        val scopes = tok.scopes
        val scopeCount = scopes.length
        if (scopeCount == 0) return null
        return scopes[scopeCount - 1]?.toString()
    }
}

/**
 * Constructs a [RegistryOptions] JS object with the supplied
 * [onigLibPromise] property set. Implemented as a tiny @JsFun shim
 * because Kotlin/Wasm cannot directly synthesize JS object literals.
 */
@JsFun("(onigLib) => ({ onigLib: onigLib })")
private external fun createRegistryOptionsRaw(onigLib: kotlin.js.Promise<JsAny>): RegistryOptions

private fun createRegistryOptions(onigLib: kotlin.js.Promise<JsAny>): RegistryOptions =
    createRegistryOptionsRaw(onigLib)
