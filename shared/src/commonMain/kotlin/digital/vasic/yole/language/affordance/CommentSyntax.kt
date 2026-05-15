/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: line-comment toggle with trim-tolerant detection.
 *#######################################################*/
package digital.vasic.yole.language.affordance

data class CommentSyntax(
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
) {
    /**
     * Toggle the line-comment prefix on a single line.
     *   - No-op if lineComment is null.
     *   - If the line (trimmed) starts with the prefix → remove it.
     *   - Otherwise → insert at the first-non-ws column.
     *
     * The check is tolerant of trailing-space-after-prefix variation:
     * "// foo" and "//foo" both detected as already-commented.
     */
    fun toggleLine(line: String): String {
        val prefix = lineComment ?: return line
        val trimmedPrefix = prefix.trimEnd()
        val firstNonWs = line.indexOfFirst { !it.isWhitespace() }
        if (firstNonWs < 0) return line // empty/whitespace line
        val content = line.substring(firstNonWs)
        return if (content.startsWith(trimmedPrefix)) {
            // Uncomment: drop the prefix + optional single trailing space
            val afterPrefix = content.substring(trimmedPrefix.length).let {
                if (it.startsWith(" ")) it.substring(1) else it
            }
            line.substring(0, firstNonWs) + afterPrefix
        } else {
            line.substring(0, firstNonWs) + prefix + content
        }
    }
}
