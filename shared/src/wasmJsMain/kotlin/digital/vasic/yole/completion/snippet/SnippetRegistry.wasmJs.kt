/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: Wasm/JS actual stub for `readSnippetResource`.
 *
 * The Wasm browser path needs an async `fetch()` against a static asset
 * URL for snippet JSON files. That plumbing is deferred to iter-60
 * Phase 7 (the same per-language asset-vendoring milestone as
 * Tree-Sitter .scm query files). Unlike ScmQueryLoader (which throws on
 * miss because .scm queries are required), snippets are optional —
 * returning null degrades gracefully to an empty completion list.
 *
 * See KNOWN_DEFECTS.md entry `#f2-phase-3-bonede-query-api-gap` (which
 * also covers snippet bundling for iOS + Wasm; iter-60 Phase 7 wires
 * fetch-based loading for both .scm queries and snippet JSON files).
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * Wasm/JS stub: returns `null` (no snippets bundled yet) until iter-60
 * Phase 7 wires fetch-based asset loading for snippet JSON files.
 * [SnippetRegistry] degrades gracefully — no crash, empty list.
 *
 * @suppress until iter-60 Phase 7.
 */
actual fun readSnippetResource(path: String): String? = null
