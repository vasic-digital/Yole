/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * CSV Format Parser - Platform Agnostic
 * Simple CSV parser without external dependencies
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import digital.vasic.yole.format.*

/**
 * Configuration for CSV parsing.
 * 
 * This data class defines how CSV files should be parsed, including the delimiter
 * character, quote character, and whether the first row contains headers.
 *
 * @property delimiter Character used to separate fields (default: ',')
 * @property quote Character used to quote fields containing delimiters (default: '"')
 * @property hasHeader Whether the first row contains column headers (default: true)
 *
 * @example
 * ```kotlin
 * val config = CsvConfig(
 *     delimiter = ';',
 *     quote = '"',
 *     hasHeader = true
 * )
 * 
 * val parser = CsvParser()
 * val table = parser.parseCsv(content, config)
 * ```
 */
data class CsvConfig(
    val delimiter: Char = ',',
    val quote: Char = '"',
    val hasHeader: Boolean = true
) {
    companion object {
        /**
         * Infer CSV configuration from the first line of content.
         * 
         * This method analyzes the first line to determine the most likely
         * delimiter and quote character based on common patterns.
         * 
         * @param firstLine The first line of CSV content
         * @return Inferred CsvConfig object
         *
         * @example
         * ```kotlin
         * val config = CsvConfig.infer("name;age;city")
         * println(config.delimiter) // ';'
         * ```
         */
        fun infer(firstLine: String): CsvConfig {
            // Try common delimiters
            val delimiter = when {
                '\t' in firstLine -> '\t'  // TSV
                ';' in firstLine -> ';'    // Semicolon-separated
                '|' in firstLine -> '|'    // Pipe-separated
                ',' in firstLine -> ','    // CSV
                else -> ','                // Default to comma
            }

            // Detect quote character
            val quote = when {
                '\'' in firstLine -> '\''
                else -> '"'
            }

            return CsvConfig(delimiter, quote, hasHeader = true)
        }
    }
}

/**
 * Represents a parsed CSV table.
 * 
 * This data class contains the parsed CSV data along with optional headers
 * and the configuration used for parsing. It provides convenient accessors
 * for row and column counts.
 *
 * @property rows List of rows, where each row is a list of string values
 * @property headers Optional list of column headers (null if no header row)
 * @property config The CsvConfig used to parse this table
 *
 * @example
 * ```kotlin
 * val table = CsvTable(
 *     rows = listOf(listOf("John", "30"), listOf("Jane", "25")),
 *     headers = listOf("Name", "Age")
 * )
 * 
 * println(table.rowCount) // 2
 * println(table.columnCount) // 2
 * println(table.headers?.first()) // "Name"
 * ```
 */
data class CsvTable(
    val rows: List<List<String>>,
    val headers: List<String>? = null,
    val config: CsvConfig = CsvConfig()
) {
    /**
     * Number of data rows (excluding header row if present).
     */
    val rowCount: Int get() = rows.size
    
    /**
     * Number of columns in the table.
     */
    val columnCount: Int get() = headers?.size ?: rows.firstOrNull()?.size ?: 0
}

/**
 * CSV format parser.
 * 
 * Implements CSV (Comma-Separated Values) parsing with support for custom delimiters,
 * quote characters, and optional header rows. The parser handles escaped quotes,
 * embedded delimiters, and empty fields. Converts CSV data to HTML tables and
 * provides validation for malformed CSV structure.
 *
 * @constructor Creates a new CsvParser instance
 *
 * @example
 * ```kotlin
 * val parser = CsvParser()
 * val content = """
 *     Name,Age,City
 *     John,30,New York
 *     Jane,25,Los Angeles
 * """.trimIndent()
 * 
 * val document = parser.parse(content)
 * val table = parser.parseCsv(content)
 * println("Rows: ${table.rowCount}")
 * println("Columns: ${table.columnCount}")
 * ```
 */
class CsvParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_CSV) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Infer configuration from first non-empty line
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: ""
        val config = CsvConfig.infer(firstLine)

        // Parse CSV
        val table = parseCsv(content, config)

        val metadata = buildMap {
            put("rows", table.rowCount.toString())
            put("columns", table.columnCount.toString())
            put("delimiter", config.delimiter.toString())
            put("hasHeader", config.hasHeader.toString())
        }

        // Convert to HTML
        val html = tableToHtml(table)

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = html,
            metadata = metadata
        )
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return document.parsedContent
    }

    /**
     * Parse CSV content into a structured table.
     * 
     * This method parses CSV content according to the provided configuration,
     * handling quoted fields, escaped quotes, and custom delimiters.
     * 
     * @param content The CSV content to parse
     * @param config The CSV configuration to use (default: standard CSV)
     * @return A CsvTable containing the parsed data and optional headers
     *
     * @example
     * ```kotlin
     * val config = CsvConfig(delimiter = ';', hasHeader = true)
     * val table = parser.parseCsv(csvContent, config)
     * println(table.headers) // ["Name", "Age", "City"]
     * ```
     */
    fun parseCsv(content: String, config: CsvConfig = CsvConfig()): CsvTable {
        val lines = content.lines().filter { it.isNotBlank() && !it.trim().startsWith("#") }
        if (lines.isEmpty()) {
            return CsvTable(emptyList(), null, config)
        }

        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            val row = parseLine(line, config.delimiter, config.quote)
            if (row.isNotEmpty()) {
                rows.add(row)
            }
        }

        // Extract headers if configured
        val headers = if (config.hasHeader && rows.isNotEmpty()) {
            rows.removeAt(0)
        } else {
            null
        }

        return CsvTable(rows, headers, config)
    }

    /**
     * Parse a single CSV line into individual fields.
     * 
     * This method handles the complexities of CSV parsing including:
     * - Quoted fields containing delimiters
     * - Escaped quotes (double quotes within quoted fields)
     * - Empty fields
     * - Fields with leading/trailing whitespace
     * 
     * @param line The CSV line to parse
     * @param delimiter The delimiter character (default: ',')
     * @param quote The quote character (default: '"')
     * @return List of field values as strings
     *
     * @example
     * ```kotlin
     * val fields = parser.parseLine("John,\"Doe, Jr.\",30")
     * println(fields) // ["John", "Doe, Jr.", "30"]
     * ```
     */
    fun parseLine(line: String, delimiter: Char = ',', quote: Char = '"'): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]

            when {
                // Quote character
                char == quote -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == quote) {
                        // Escaped quote (two quotes in a row)
                        currentField.append(quote)
                        i++ // Skip next quote
                    } else {
                        // Toggle quote state
                        inQuotes = !inQuotes
    }
}

                // Delimiter (when not in quotes)
                char == delimiter && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }

                // Regular character
                else -> {
                    currentField.append(char)
    }
}

/**
 * Register the CSV parser with the registry
 */
fun registerCsvParser() {
    ParserRegistry.register(CsvParser())
}

            i++
        }

        // Add last field
        fields.add(currentField.toString())

        return fields
    }

    /**
     * Convert CSV table to HTML with styling.
     * 
     * This method generates an HTML table with appropriate CSS classes for
     * styling. Headers are rendered in <thead> with <th> tags, while data
     * rows are in <tbody> with <td> tags. Empty cells are rendered with
     * non-breaking spaces to maintain table structure.
     * 
     * @param table The CsvTable to convert
     * @return HTML representation of the table with embedded CSS
     */
    private fun tableToHtml(table: CsvTable): String {
        return buildString {
            append("<div class='csv-table'>")
            append("<table>")

            // Headers
            if (table.headers != null) {
                append("<thead><tr>")
                for (header in table.headers) {
                    append("<th>")
                    append(header.trim().escapeHtml())
                    append("</th>")
                }
                append("</tr></thead>")
            }

            // Rows
            append("<tbody>")
            for (row in table.rows) {
                append("<tr>")
                for (cell in row) {
                    append("<td>")
                    val trimmed = cell.trim()
                    if (trimmed.isEmpty()) {
                        append("&nbsp;")
                    } else {
                        // Handle newlines in cells
                        append(trimmed.replace("\n", "<br/>").escapeHtml())
                    }
                    append("</td>")
                }
                append("</tr>")
            }
            append("</tbody>")

            append("</table>")
            append("</div>")
        }
    }

    /**
     * Convert CSV table to Markdown table format.
     * 
     * This method converts the parsed CSV data into GitHub Flavored Markdown
     * table format, which is widely supported for documentation and README files.
     * 
     * @param table The CsvTable to convert
     * @return Markdown table representation
     *
     * @example
     * ```kotlin
     * val markdown = parser.toMarkdownTable(table)
     * println(markdown)
     * // Output:
     * // | Name | Age |
     * // |------|-----|
     * // | John | 30  |
     * ```
     */
    fun toMarkdownTable(table: CsvTable): String {
        return buildString {
            // Headers
            if (table.headers != null) {
                append("| ")
                append(table.headers.joinToString(" | ") { it.trim() })
                append(" |\n")

                // Separator line
                append("|")
                append(table.headers.joinToString("|") { " --- " })
                append("|\n")
            }

            // Rows
            for (row in table.rows) {
                append("| ")
                append(row.joinToString(" | ") { it.trim().ifEmpty { "&nbsp;" } })
                append(" |\n")
            }
        }
    }
}
