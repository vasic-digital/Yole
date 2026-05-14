/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: minimal token type produced by TokenizerEngine
 * implementations and consumed downstream by SyntaxHighlighter +
 * ScopeMapper (Phase 8). The shape mirrors the spec §4
 * "data class Token(range, scope, depth)" but normalises range to
 * explicit byte offsets so platform actuals can emit them directly
 * from native tree walks without first allocating an IntRange.
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * A single highlightable span produced by a [TokenizerEngine].
 *
 * @property startByte UTF-8 byte offset of the span start (inclusive).
 * @property endByte UTF-8 byte offset of the span end (exclusive).
 * @property scope Grammar-native scope name (e.g., Tree-Sitter node type
 *   like `atx_h1_marker`, `code_span`, `emphasis`). Downstream
 *   `ScopeMapper` (Phase 8) translates these to canonical VS Code
 *   TextMate scopes (`markup.heading`, `markup.inline.raw`, etc.) for
 *   theme lookup.
 */
data class Token(
    val startByte: Int,
    val endByte: Int,
    val scope: String,
)
