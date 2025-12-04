/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Creole Format Parser - Platform Agnostic
 * Handles Creole wiki markup format (Creole 1.0)
 *
 *########################################################*/
package digital.vasic.yole.format.creole

import digital.vasic.yole.format.*

/**
 * Creole format parser
 * Handles Creole 1.0 wiki markup language
 */
class CreoleParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_CREOLE) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Convert to HTML
        val html = convertToHtml(content)

        val metadata = buildMap {
            put("extension", extension)
            put("lines", content.lines().size.toString())
        }

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
     * Convert Creole to HTML
     */
    private fun convertToHtml(content: String): String {
        val lines = content.lines()
        val html = StringBuilder()

        html.append("<div class='creole'>")
        html.append("<style>")
        html.append(".creole { font-family: sans-serif; line-height: 1.6; }")
        html.append(".creole h1 { font-size: 2em; font-weight: bold; margin: 0.67em 0; }")
        html.append(".creole h2 { font-size: 1.8em; font-weight: bold; margin: 0.75em 0; }")
        html.append(".creole h3 { font-size: 1.6em; font-weight: bold; margin: 0.83em 0; }")
        html.append(".creole h4 { font-size: 1.4em; font-weight: bold; margin: 1em 0; }")
        html.append(".creole h5 { font-size: 1.2em; font-weight: bold; margin: 1.17em 0; }")
        html.append(".creole h6 { font-size: 1em; font-weight: bold; margin: 1.33em 0; }")
        html.append(".creole ul { list-style-type: disc; margin: 1em 0; padding-left: 40px; }")
        html.append(".creole ol { list-style-type: decimal; margin: 1em 0; padding-left: 40px; }")
        html.append(".creole ul ul { list-style-type: circle; }")
        html.append(".creole ul ul ul { list-style-type: square; }")
        html.append(".creole table { border-collapse: collapse; margin: 1em 0; }")
        html.append(".creole th { border: 1px solid #ccc; padding: 8px; background-color: #f0f0f0; font-weight: bold; }")
        html.append(".creole td { border: 1px solid #ccc; padding: 8px; }")
        html.append(".creole hr { border: none; border-top: 1px solid #ccc; margin: 1em 0; }")
        html.append(".creole pre { background-color: #f0f0f0; padding: 10px; overflow-x: auto; font-family: monospace; }")
        html.append(".creole code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; }")
        html.append(".creole a { color: #0066cc; text-decoration: none; }")
        html.append(".creole a:hover { text-decoration: underline; }")
        html.append("</style>")

        var inUnorderedList = false
        var inOrderedList = false
        var inCodeBlock = false
        var inTable = false
        var currentListLevel = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Handle code blocks
            if (trimmed == "{{{") {
                if (!inCodeBlock) {
                    html.append("<pre>")
                    inCodeBlock = true
                }
                i++
                continue
            } else if (trimmed == "}}}" && inCodeBlock) {
                html.append("</pre>")
                inCodeBlock = false
                i++
                continue
            } else if (inCodeBlock) {
                html.append(line.escapeHtml())
                html.append("\n")
                i++
                continue
            }

            // Detect line types
            val unorderedListMatch = Regex("^(\\*+) (.+)$").find(trimmed)
            val orderedListMatch = Regex("^(#+) (.+)$").find(trimmed)
            val isTableRow = trimmed.startsWith("|") && trimmed.endsWith("|")
            val isEmpty = trimmed.isEmpty()

            // Handle tables
            if (isTableRow) {
                if (!inTable) {
                    // Close any open lists
                    if (inUnorderedList) {
                        html.append("</ul>".repeat(currentListLevel))
                        inUnorderedList = false
                        currentListLevel = 0
                    }
                    if (inOrderedList) {
                        html.append("</ol>".repeat(currentListLevel))
                        inOrderedList = false
                        currentListLevel = 0
                    }
                    html.append("<table>")
                    inTable = true
                }
                html.append(convertTableRow(trimmed))
            } else if (unorderedListMatch != null) {
                // Close table if open
                if (inTable) {
                    html.append("</table>")
                    inTable = false
                }
                // Close ordered list if open
                if (inOrderedList) {
                    html.append("</ol>".repeat(currentListLevel))
                    inOrderedList = false
                    currentListLevel = 0
                }

                val level = unorderedListMatch.groupValues[1].length
                val text = unorderedListMatch.groupValues[2]

                // Adjust list nesting
                while (currentListLevel < level) {
                    html.append("<ul>")
                    currentListLevel++
                    inUnorderedList = true
                }
                while (currentListLevel > level) {
                    html.append("</ul>")
                    currentListLevel--
                }

                html.append("<li>${convertInlineMarkup(text)}</li>")
            } else if (orderedListMatch != null) {
                // Close table if open
                if (inTable) {
                    html.append("</table>")
                    inTable = false
                }
                // Close unordered list if open
                if (inUnorderedList) {
                    html.append("</ul>".repeat(currentListLevel))
                    inUnorderedList = false
                    currentListLevel = 0
                }

                val level = orderedListMatch.groupValues[1].length
                val text = orderedListMatch.groupValues[2]

                // Adjust list nesting
                while (currentListLevel < level) {
                    html.append("<ol>")
                    currentListLevel++
                    inOrderedList = true
                }
                while (currentListLevel > level) {
                    html.append("</ol>")
                    currentListLevel--
                }

                html.append("<li>${convertInlineMarkup(text)}</li>")
            } else {
                // Not a list or table - close any open structures
                if (inUnorderedList) {
                    html.append("</ul>".repeat(currentListLevel))
                    inUnorderedList = false
                    currentListLevel = 0
                }
                if (inOrderedList) {
                    html.append("</ol>".repeat(currentListLevel))
                    inOrderedList = false
                    currentListLevel = 0
                }
                if (inTable) {
                    html.append("</table>")
                    inTable = false
                }

                if (!isEmpty) {
                    html.append(convertLine(trimmed))
                }
            }

            i++
        }

        // Close any open structures at the end
        if (inUnorderedList) html.append("</ul>".repeat(currentListLevel))
        if (inOrderedList) html.append("</ol>".repeat(currentListLevel))
        if (inTable) html.append("</table>")
        if (inCodeBlock) html.append("</pre>")

        html.append("</div>")
        return html.toString()
    }

    /**
     * Convert a single line of Creole to HTML
     */
    private fun convertLine(line: String): String {
        val trimmed = line.trim()

        // Headings: = H1 =, == H2 ==, etc.
        val headingMatch = Regex("^(=+)\\s+(.+?)\\s*=*$").find(trimmed)
        if (headingMatch != null) {
            val level = minOf(headingMatch.groupValues[1].length, 6)
            val text = headingMatch.groupValues[2]
            return "<h$level>${convertInlineMarkup(text)}</h$level>"
        }

        // Horizontal rule: ----
        if (trimmed.matches(Regex("^-{4,}$"))) {
            return "<hr>"
        }

        // Paragraph
        if (trimmed.isNotEmpty()) {
            return "<p>${convertInlineMarkup(trimmed)}</p>"
        }

        return ""
    }

    /**
     * Convert a table row
     */
    private fun convertTableRow(line: String): String {
        val trimmed = line.trim()
        val cells = trimmed.substring(1, trimmed.length - 1).split("|")

        val html = StringBuilder("<tr>")
        for (cell in cells) {
            val cellTrimmed = cell.trim()
            if (cellTrimmed.startsWith("=")) {
                // Header cell
                val content = cellTrimmed.substring(1).trim()
                html.append("<th>${convertInlineMarkup(content)}</th>")
            } else {
                // Regular cell
                html.append("<td>${convertInlineMarkup(cellTrimmed)}</td>")
            }
        }
        html.append("</tr>")
        return html.toString()
    }

    /**
     * Convert inline Creole markup to HTML
     */
    private fun convertInlineMarkup(text: String): String {
        var result = text

        // Process inline code FIRST (before escaping)
        result = result.replace(Regex("""\{\{\{([^}]+)\}\}\}""")) { match ->
            val code = match.groupValues[1].escapeHtml()
            "##CODE##$code##/CODE##"
        }

        // Process links and images BEFORE escaping
        result = result.replace(Regex("""\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")) { match ->
            val link = match.groupValues[1]
            val description = match.groupValues[2].ifEmpty { link }
            "##LINK##$link##SEP##$description##/LINK##"
        }

        result = result.replace(Regex("""\{\{([^|}]+)(?:\|([^}]+))?\}\}""")) { match ->
            val img = match.groupValues[1]
            val alt = match.groupValues[2].ifEmpty { img }
            "##IMG##$img##ALT##$alt##/IMG##"
        }

        // Escape HTML
        result = result.escapeHtml()

        // Line breaks: \\ -> <br>
        result = result.replace("\\\\", "<br>")

        // Bold: **text**
        result = result.replace(Regex("""\*\*([^*]+)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }

        // Italics: //text//
        result = result.replace(Regex("""//([^/]+)//""")) { "<em>${it.groupValues[1]}</em>" }

        // Restore inline code
        result = result.replace(Regex("""##CODE##(.+?)##/CODE##""")) { "<code>${it.groupValues[1]}</code>" }

        // Restore links
        result = result.replace(Regex("""##LINK##(.+?)##SEP##(.+?)##/LINK##""")) { match ->
            "<a href='${match.groupValues[1]}'>${match.groupValues[2]}</a>"
        }

        // Restore images
        result = result.replace(Regex("""##IMG##(.+?)##ALT##(.+?)##/IMG##""")) { match ->
            "<img src='${match.groupValues[1]}' alt='${match.groupValues[2]}'/>"
        }

        return result
    }

    /**
     * Extract file extension from filename
     */
    private fun getExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }

    override fun validate(content: String): List<String> {
        val errors = mutableListOf<String>()

        val lines = content.lines()
        var inCodeBlock = false

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Track code blocks
            if (trimmed == "{{{") {
                inCodeBlock = true
            } else if (trimmed == "}}}") {
                inCodeBlock = false
            }

            // Skip validation inside code blocks
            if (inCodeBlock) return@forEachIndexed

            // Check for malformed table rows
            if (trimmed.startsWith("|") && !trimmed.endsWith("|")) {
                errors.add("Line ${index + 1}: Malformed table row (should end with |)")
            }

            // Check for unclosed brackets in links
            val openBrackets = trimmed.count { it == '[' }
            val closeBrackets = trimmed.count { it == ']' }
            if (openBrackets != closeBrackets) {
                errors.add("Line ${index + 1}: Unclosed brackets in links")
            }

            // Check for unclosed braces
            val openBraces = trimmed.count { it == '{' }
            val closeBraces = trimmed.count { it == '}' }
            if (openBraces != closeBraces) {
                errors.add("Line ${index + 1}: Unclosed braces")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".creole", ".txt")
    }
}

/**
 * Register the Creole parser with the registry
 */
fun registerCreoleParser() {
    ParserRegistry.register(CreoleParser())
}
