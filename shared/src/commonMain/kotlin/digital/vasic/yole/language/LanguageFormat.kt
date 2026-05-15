/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58: first-class language metadata. Extends iter-57's
 * Grammar abstraction with affordance data (filled in Phase 2).
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules

/**
 * First-class language metadata. Carries the iter-57 grammar id PLUS the
 * Feature 2 affordance data needed for comment-toggle, auto-indent,
 * bracket-pair, outline, and fold.
 */
data class LanguageFormat(
    val id: String,                 // matches iter-57 Grammar.id
    val displayName: String,
    val extensions: List<String>,
    val mimeTypes: List<String>,
    val commentSyntax: CommentSyntax,
    val indentRules: IndentRules,
    val bracketPairs: BracketPairs,
    val indentUnit: String = "    ", // 4 spaces default; per-lang overrides
)
