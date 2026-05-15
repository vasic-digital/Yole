/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: SnippetRegistry — per-language snippet store, backed
 * by bundled VS Code snippets.json resources.
 *
 * Cross-platform resource access is delegated to the platform-specific
 * `expect fun readSnippetResource(path: String): String?` — JVM
 * (Android + Desktop) actuals use the standard ClassLoader and return
 * null on miss; iOS + Wasm actuals return null for now (snippet asset
 * bundling lands in iter-60 Phase 7, the same per-language vendoring
 * milestone as .scm query files — see KNOWN_DEFECTS.md
 * #f2-phase-3-bonede-query-api-gap). Returning null is benign: missing
 * snippets degrade to an empty completion list, never a crash.
 *
 * Plan deviation (vs. plan lines 692-717): the plan's draft used
 * `SnippetRegistry::class.java.classLoader` directly. That does not
 * compile in commonMain (no JVM types). This implementation mirrors the
 * iter-58 ScmQueryLoader expect/actual pattern precisely, with the one
 * semantic difference that a missing resource returns null rather than
 * throwing, because snippets are optional.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * Read a bundled snippet resource from the platform's packaged
 * resources. [path] is the classpath-relative path
 * (e.g., `snippets/markdown/snippets.json`).
 *
 * Returns `null` if the resource is not found on this platform —
 * snippet bundles are optional; a missing file degrades gracefully to
 * an empty list. JVM actuals (Android + Desktop) consult the ClassLoader;
 * iOS + Wasm actuals return null until iter-60 Phase 7 wires asset
 * loading (see KNOWN_DEFECTS.md `#f2-phase-3-bonede-query-api-gap`).
 */
expect fun readSnippetResource(path: String): String?

/**
 * Per-language snippet store.
 *
 * Loads snippets lazily on first access and caches them for the
 * lifetime of the process. The cache is process-local and never
 * invalidated (bundled snippet files are immutable at runtime).
 * Concurrent callers may race on first miss — that is idempotent and
 * cheap, so no lock is needed.
 *
 * Usage:
 * ```kotlin
 * val snippets = SnippetRegistry.forLanguage("markdown")
 * ```
 */
object SnippetRegistry {
    private val cache = mutableMapOf<String, List<Snippet>>()

    /**
     * Return all snippets for [langId].
     *
     * Returns an empty list if no snippet bundle is bundled for this
     * language, or if the bundle cannot be parsed.
     */
    fun forLanguage(langId: String): List<Snippet> = cache.getOrPut(langId) {
        val raw = readSnippetResource("snippets/$langId/snippets.json")
            ?: return@getOrPut emptyList()
        try {
            VsCodeSnippetParser.parse(raw)
        } catch (e: SnippetParseException) {
            emptyList()
        }
    }

    /**
     * Flush the snippet cache.
     *
     * Test-only — production code does not need to call this.
     */
    fun clear() = cache.clear()
}
