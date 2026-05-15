/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: ScmQuery + ScmCapture value types for Tree-Sitter
 * `.scm` query results. Capture coordinates carry the absolute byte
 * offsets of the captured node so downstream affordance code can
 * compute fold ranges, outline items, etc., without re-walking the tree.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * One match capture produced by running a Tree-Sitter `.scm` query
 * against a parsed source-code document. [name] is the capture
 * name (e.g., `fold`, `definition.section`); [startByte] / [endByte]
 * are the byte offsets of the captured node within the original
 * source text (UTF-8 on JVM, UTF-16 code units on Wasm — Phase 6
 * normalises to a single canonical encoding per spec §5.1).
 */
data class ScmCapture(
    val name: String,
    val startByte: Int,
    val endByte: Int,
    val startRow: Int = -1,
    val endRow: Int = -1,
)

/**
 * Compiled-or-source representation of a Tree-Sitter query.
 * [raw] is the canonical `.scm` source string as bundled (or
 * fetched from disk) — used for diagnostics and to cache-compile
 * on each platform's native query engine.
 * [source] identifies the origin (e.g., `nvim-treesitter/markdown/folds.scm`)
 * for licence attribution in error messages and audit logs.
 */
data class ScmQuery(
    val raw: String,
    val source: String,
)
