/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: expect class for the platform-specific tokenizer
 * engine. Implementations:
 *   - Desktop (JVM 11+): Tree-Sitter via JNI (io.github.bonede tree-sitter-ng).
 *   - Android: same JNI binding API, but the upstream JAR does not
 *     publish an aarch64-linux-android .so today; initialize() therefore
 *     returns Result.failure on Android pending the operator producing
 *     an NDK-built libtree-sitter.so / libtree-sitter-markdown.so. The
 *     editor falls back to plain text per spec §4 "Engine load failed
 *     at startup" — no fake tokens are produced. See Phase 5 commit body
 *     and docs/CONTINUATION.md for the upgrade path.
 *   - iOS: Tree-Sitter via Kotlin/Native cinterop (Phase 7).
 *   - Wasm: vscode-textmate via JS interop (Phase 6).
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * Platform-specific tokenizer. Returns a list of [Token]s for the given
 * text and language identifier. Implementations MUST be thread-safe
 * (each call may run on a different coroutine dispatcher).
 *
 * Lifecycle:
 * 1. Construct.
 * 2. Call [initialize] once at app startup. Inspect the [Result]: on
 *    `Result.failure`, the platform cannot run the native engine and
 *    the caller MUST disable highlighting (no bluff fallback).
 * 3. For each language to be highlighted, call [loadGrammar].
 * 4. For each text to highlight, call [tokenize].
 *
 * @throws FormatDisabledException from [loadGrammar] / [tokenize] if
 *   the requested `lang` is gated off in [EnabledFormatGate].
 * @throws IllegalArgumentException from [loadGrammar] if the grammar
 *   identifier has no bundled implementation in this engine version.
 */
expect class TokenizerEngine() {
    /**
     * One-shot startup. Loads the native tree-sitter shared library for
     * the current host platform and arch. Idempotent — repeated calls
     * after a successful first call return [Result.success] immediately.
     *
     * @return `Result.failure` (NOT a thrown exception) if the platform
     *   has no compatible native binary available. Callers SHOULD inspect
     *   the result and disable highlighting on failure rather than retry.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Eagerly load the grammar named [lang]. Subsequent [tokenize] calls
     * for the same `lang` are guaranteed to find it in the engine's
     * grammar cache. Phase 5 only bundles `markdown`; later phases add
     * more grammars via lazy CDN fetch.
     *
     * @throws FormatDisabledException if [lang] is not enabled.
     * @throws IllegalArgumentException if [lang] is unknown.
     * @throws IllegalStateException if [initialize] has not been called
     *   successfully first.
     */
    suspend fun loadGrammar(lang: String)

    /**
     * Tokenize [text] using a grammar previously loaded via [loadGrammar].
     * If the grammar has not yet been loaded, an implementation MAY
     * load it implicitly; in either case the result is a list of [Token]s
     * sorted by `startByte` ascending, suitable for direct consumption
     * by `SyntaxHighlighter` (Phase 8).
     *
     * @throws FormatDisabledException if [lang] is not enabled.
     */
    suspend fun tokenize(text: String, lang: String): List<Token>

    /**
     * Returns true iff [lang]'s grammar is currently loaded in this engine
     * instance. Test/diagnostic hook; production code SHOULD use the
     * implicit-load path via [tokenize] rather than gate on this.
     */
    fun isGrammarLoaded(lang: String): Boolean
}
