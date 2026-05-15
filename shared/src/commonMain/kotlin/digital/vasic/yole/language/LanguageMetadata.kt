/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: static language manifest.
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules

/**
 * Static manifest of every Yole-supported language.
 * Phase 1 ships markdown (the architectural anchor) plus kotlin so that the
 * Phase 1 anti-bluff test [detectByFilename_handlesKotlin] has a real entry
 * to find. Phase 6 fills the other 48+ from Phase 0 research-report.md §4.
 */
object LanguageMetadata {
    val markdown = LanguageFormat(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
        mimeTypes = listOf("text/markdown", "text/x-markdown"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — markdown's de-facto convention
    )

    val kotlin = LanguageFormat(
        id = "kotlin",
        displayName = "Kotlin",
        extensions = listOf(".kt", ".kts"),
        mimeTypes = listOf("text/x-kotlin"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Kotlin style guide
    )

    val all: List<LanguageFormat> = listOf(markdown, kotlin)
}
