/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: Wasm/JS actual stub for `readScmResource`.
 *
 * The Wasm browser path needs an async `fetch()` against a static asset
 * URL served from `/grammars/<lang>/<queryName>.scm`. That plumbing
 * lands as part of iter-58 Phase 6 (web-tree-sitter Wasm affordance
 * engine + Wasm asset bundling) per the plan. Until then this stub
 * throws — Wasm callers on the affordance path get an honest "not
 * bundled yet" error rather than a faked-empty result per CONST-035
 * anti-bluff covenant.
 *
 * See KNOWN_DEFECTS.md entry `#f2-phase-3-bonede-query-api-gap`.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * Wasm/JS stub: throws [IllegalStateException] until iter-58 Phase 6
 * wires fetch-based asset loading for vendored `.scm` query files.
 * Documented in the iter-58 plan Phase 6 + KNOWN_DEFECTS.md.
 *
 * @suppress until iter-58 Phase 6.
 */
actual fun readScmResource(path: String): String {
    error(
        "readScmResource is not yet wired on Wasm/JS (path=`$path`). " +
            "iter-58 Phase 6 (web-tree-sitter affordance engine + Wasm " +
            "asset bundling) implements fetch-based loading. " +
            "See KNOWN_DEFECTS.md #f2-phase-3-bonede-query-api-gap.",
    )
}
