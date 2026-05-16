/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: TableConverter — row-list → GFM Markdown table.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

/**
 * Converts a list of rows (each a list of cell strings) into a
 * GitHub Flavored Markdown table.
 *
 * The **first row** is treated as the header row.
 * Pipe characters (`|`) inside cell text are escaped as `\|`.
 *
 * Output layout:
 * ```
 * | h1 | h2 |
 * | --- | --- |
 * | d1 | d2 |
 * ```
 */
object TableConverter {

    /**
     * Converts [rows] to a GFM Markdown table string.
     *
     * @param rows Outer list is rows, inner list is cells.
     *             The first row becomes the header.
     *             Returns an empty string for an empty [rows] list.
     */
    fun toMarkdownTable(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""

        val sb = StringBuilder()

        // Header row
        val header = rows[0]
        sb.appendRow(header)

        // Separator row — one `---` per column
        val separator = List(header.size) { "---" }
        sb.appendRow(separator)

        // Data rows
        for (i in 1 until rows.size) {
            sb.appendRow(rows[i])
        }

        // Remove trailing newline added by the last appendRow call
        return sb.trimEnd('\n').toString()
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        append("| ")
        append(cells.joinToString(" | ") { it.replace("|", "\\|") })
        append(" |\n")
    }
}
