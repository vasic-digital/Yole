/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: naive last-char indent computation.
 *#######################################################*/
package digital.vasic.yole.language.affordance

data class IndentRules(
    val indentTokens: Set<String> = setOf("{", "(", "["),
    val dedentTokens: Set<String> = setOf("}", ")", "]"),
) {
    /**
     * Given the line being broken at Enter, return the indent for the
     * next line. Naive but correct for v1: looks at the last non-ws
     * character. AST-aware indent is a Phase 6 enhancement using
     * Tree-Sitter trees.
     */
    fun computeIndent(line: String, indentUnit: String): String {
        if (line.isEmpty()) return ""
        val currentIndent = line.takeWhile { it == ' ' || it == '\t' }
        val trimmed = line.trimEnd()
        if (trimmed.isEmpty()) return currentIndent
        val lastChar = trimmed.last().toString()
        return if (lastChar in indentTokens) currentIndent + indentUnit else currentIndent
    }
}
