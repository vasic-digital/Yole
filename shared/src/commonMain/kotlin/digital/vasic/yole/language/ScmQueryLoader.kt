/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: ScmQueryLoader — central authority for resolving
 * bundled Tree-Sitter `.scm` query files by (language, queryName).
 *
 * Resources live under `shared/src/commonMain/resources/grammars/<lang>/
 * <queryName>.scm` and are packaged into each consumer module's classpath
 * by the Kotlin/Compose Multiplatform Gradle pipeline. Per-platform
 * classpath/asset access is delegated to the expect fun
 * `readScmResource(path)` — JVM (Android + Desktop) actuals use the
 * standard ClassLoader; iOS + Wasm actuals throw [IllegalStateException]
 * pending Phase 6/7 asset bundling wiring (see KNOWN_DEFECTS.md entry
 * `#f2-phase-3-bonede-query-api-gap` for the cross-platform decoupling
 * rationale).
 *
 * Anti-bluff (CONST-035): the cache MUST be populated from a real
 * resource read — never from a hardcoded fallback string. If the
 * resource is missing, `load` throws so the caller surfaces an honest
 * "not bundled" error rather than emitting an empty-but-PASS result.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * Read a bundled `.scm` query resource from the platform's packaged
 * resources. [path] is the classpath-relative leaf path (e.g.,
 * `grammars/markdown/folds.scm`).
 *
 * @throws IllegalStateException if the resource is not bundled on this
 *   platform. JVM (Android + Desktop) actuals consult the ClassLoader;
 *   iOS + Wasm actuals throw until Phase 6/7 wires asset loading
 *   (see KNOWN_DEFECTS.md `#f2-phase-3-bonede-query-api-gap`).
 */
expect fun readScmResource(path: String): String

/**
 * Central authority for loading bundled Tree-Sitter `.scm` query files.
 *
 * Phase 3 v1 ships only `markdown/folds.scm` and `markdown/outline.scm`
 * — vendored from `nvim-treesitter/runtime/queries/markdown/folds.scm`
 * (Apache-2.0) and `helix-editor/helix/runtime/queries/markdown/tags.scm`
 * (MPL-2.0) per research-report.md §1 and §2 of the iter-58 plan.
 *
 * Cache: lazy, process-local, never invalidated (the bundled query
 * sources do not change at runtime). Concurrent callers may both read
 * the resource on first miss — that's idempotent and cheap, so no lock
 * is needed.
 */
object ScmQueryLoader {
    private val cache: MutableMap<String, String> = mutableMapOf()

    /**
     * Load the named query for the named language.
     *
     * @param lang Yole language id (e.g., `markdown`).
     * @param queryName one of `folds`, `outline`, `highlights`, `indents`,
     *   `injections`, `locals` per nvim-treesitter / helix conventions.
     * @return the raw `.scm` query source text.
     * @throws IllegalStateException if the resource is not bundled or
     *   not yet wired for this platform.
     */
    fun load(lang: String, queryName: String): String {
        val key = "$lang/$queryName"
        cache[key]?.let { return it }
        val path = "grammars/$lang/$queryName.scm"
        val content = readScmResource(path)
        cache[key] = content
        return content
    }

    /**
     * Test-only hook to flush the cache so a subsequent [load] call
     * re-reads the resource. Production code does not need this — the
     * bundled query strings are immutable.
     */
    internal fun clearCacheForTest() {
        cache.clear()
    }
}
