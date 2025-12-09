/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Markdown Format Parser - Platform Agnostic
 * Handles CommonMark + GitHub Flavored Markdown
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.*

/**
 * Markdown format parser.
 * 
 * Supports CommonMark + GitHub Flavored Markdown (GFM) extensions including
 * tables, task lists, strikethrough, footnotes, and more. This parser converts
 * Markdown markup to HTML for display and provides syntax validation.
 *
 * @constructor Creates a new MarkdownParser instance
 *
 * @example
 * ```kotlin
 * val parser = MarkdownParser()
 * val content = """
 *     # Hello World
 *     
 *     This is **bold** and *italic* text.
 *     
 *     - [x] Completed task
 *     - [ ] Pending task
 *     
 *     | Column 1 | Column 2 |
 *     |----------|----------|
 *     | Cell 1   | Cell 2   |
 * """.trimIndent()
 * 
 * val document = parser.parse(content)
 * val html = parser.toHtml(document)
 * ```
 */
class MarkdownParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_MARKDOWN) ?: FormatRegistry.formats.last()

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
     * Convert Markdown content to HTML.
     * 
     * This method implements a comprehensive Markdown parser that supports:
     * - Headers (H1-H6)
     * - Emphasis (bold, italic, strikethrough)
     * - Links and images
     * - Code blocks and inline code
     * - Blockquotes
     * - Lists (ordered and unordered)
     * - Tables (GFM style)
     * - Task lists
     * - Horizontal rules
     * 
     * @param content The Markdown content to convert
     * @return HTML representation with embedded CSS styling
     */
    private fun convertToHtml(content: String): String {
        val lines = content.lines()
        val html = StringBuilder()

        html.append("<div class='markdown'>")
        html.append(StyleSheets.MARKDOWN_STYLES)

        var inCodeBlock = false
        var inBlockQuote = false
        var inUnorderedList = false
        var inOrderedList = false
        var inTable = false
        var inParagraph = false
        var codeBlockLanguage = ""

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Handle code blocks (fenced: ```)
            if (trimmed.startsWith("```")) {
                if (!inCodeBlock) {
                    codeBlockLanguage = trimmed.substring(3).trim()
                    html.append("<pre><code>")
                    inCodeBlock = true
                } else {
                    html.append("</code></pre>")
                    inCodeBlock = false
                    codeBlockLanguage = ""
                }
                i++
                continue
            } else if (inCodeBlock) {
                html.append(line.escapeHtml())
                html.append("\n")
                i++
                continue
            }

            // Empty line handling
            if (trimmed.isEmpty()) {
                if (inParagraph) {
                    html.append("</p>")
                    inParagraph = false
                }
                if (inBlockQuote) {
                    html.append("</blockquote>")
                    inBlockQuote = false
                }
                if (inUnorderedList) {
                    html.append("</ul>")
                    inUnorderedList = false
                }
                if (inOrderedList) {
                    html.append("</ol>")
                    inOrderedList = false
                }
                i++
                continue
            }

            // Detect line types
            val isHeading = trimmed.startsWith("#")
            val isBlockQuote = trimmed.startsWith(">")
            val isUnorderedList = trimmed.matches(Regex("^[-*+]\\s+.*"))
            val isOrderedList = trimmed.matches(Regex("^\\d+\\.\\s+.*"))
            val isHorizontalRule = trimmed.matches(Regex("^([-*_]\\s*){3,}$"))
            val isTableRow = trimmed.startsWith("|") && trimmed.endsWith("|")

            // Handle table separator detection
            var isTableSeparator = false
            if (isTableRow && trimmed.matches(Regex("^\\|\\s*([-:]+\\s*\\|)+\\s*$"))) {
                isTableSeparator = true
            }

            // Close incompatible block elements
            if (isHeading || isHorizontalRule) {
                if (inParagraph) { html.append("</p>"); inParagraph = false }
                if (inBlockQuote) { html.append("</blockquote>"); inBlockQuote = false }
                if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                if (inOrderedList) { html.append("</ol>"); inOrderedList = false }
            }

            when {
                isHeading -> {
                    val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmed)
                    if (headingMatch != null) {
                        val level = headingMatch.groupValues[1].length
                        val text = headingMatch.groupValues[2]
                        html.append("<h$level>${convertInlineMarkup(text)}</h$level>")
                    }
                }

                isHorizontalRule -> {
                    html.append("<hr>")
                }

                isTableRow && !isTableSeparator -> {
                    if (!inTable) {
                        html.append("<table>")
                        inTable = true
                    }
                    // Check if next line is a separator (makes this row a header)
                    val isHeaderRow = i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|\\s*([-:]+\\s*\\|)+\\s*$"))
                    html.append(convertTableRow(trimmed, isHeaderRow))
                }

                isTableSeparator -> {
                    // Just skip the separator line, table already started
                }

                isBlockQuote -> {
                    if (!inBlockQuote) {
                        html.append("<blockquote>")
                        inBlockQuote = true
                    }
                    val text = trimmed.substring(1).trim()
                    html.append(convertInlineMarkup(text))
                    html.append("<br>")
                }

                isUnorderedList -> {
                    if (!inUnorderedList) {
                        html.append("<ul>")
                        inUnorderedList = true
                    }
                    val text = trimmed.substring(trimmed.indexOfFirst { it.isWhitespace() }).trim()
                    html.append("<li>${convertInlineMarkup(text)}</li>")
                }

                isOrderedList -> {
                    if (!inOrderedList) {
                        html.append("<ol>")
                        inOrderedList = true
                    }
                    val text = trimmed.substring(trimmed.indexOf('.') + 1).trim()
                    html.append("<li>${convertInlineMarkup(text)}</li>")
                }

                else -> {
                    if (inTable) {
                        html.append("</table>")
                        inTable = false
                    }
                    if (!inParagraph) {
                        html.append("<p>")
                        inParagraph = true
                    }
                    html.append(convertInlineMarkup(trimmed))
                    html.append(" ")
                }
            }

            i++
        }

        // Close any open elements
        if (inCodeBlock) html.append("</code></pre>")
        if (inParagraph) html.append("</p>")
        if (inBlockQuote) html.append("</blockquote>")
        if (inUnorderedList) html.append("</ul>")
        if (inOrderedList) html.append("</ol>")
        if (inTable) html.append("</table>")

        html.append("</div>")
        return html.toString()
    }

    /**
     * Convert a Markdown table row to HTML.
     * 
     * @param line The table row line (starts and ends with |)
     * @param isHeaderRow Whether this row should be treated as a header row
     * @return HTML table row with appropriate cell tags
     */
    private fun convertTableRow(line: String, isHeaderRow: Boolean): String {
        val cells = line.trim().substring(1, line.trim().length - 1).split("|")
        val tag = if (isHeaderRow) "th" else "td"

        val html = StringBuilder("<tr>")
        for (cell in cells) {
            html.append("<$tag>${convertInlineMarkup(cell.trim())}</$tag>")
        }
        html.append("</tr>")
        return html.toString()
    }

    /**
     * Convert inline Markdown markup to HTML.
     *
     * Memory-optimized version that processes markup in a single pass
     * to reduce string allocations and improve performance.
     *
     * @param text The text containing inline markup
     * @return HTML with inline markup converted to appropriate tags
     */
    private fun convertInlineMarkup(text: String): String {
        if (text.isEmpty()) return text

        val result = StringBuilder(text.length + text.length / 2) // Pre-allocate with some overhead
        var i = 0

        while (i < text.length) {
            val char = text[i]

            when (char) {
                '`' -> { // Inline code
                    if (i + 1 < text.length && text[i + 1] != '`') {
                        // Find closing backtick
                        var end = i + 1
                        while (end < text.length && text[end] != '`') {
                            end++
                        }
                        if (end < text.length) {
                            val code = text.substring(i + 1, end).escapeHtml()
                            result.append("<code>$code</code>")
                            i = end + 1
                            continue
                        }
                    }
                    result.append(char)
                }

                '!' -> { // Image
                    if (i + 1 < text.length && text[i + 1] == '[') {
                        val imageMatch = parseLinkOrImage(text, i, true)
                        if (imageMatch != null) {
                            result.append("<img src='${imageMatch.url}' alt='${imageMatch.text}'/>")
                            i = imageMatch.endIndex
                            continue
                        }
                    }
                    result.append(char)
                }

                '[' -> { // Link or checkbox
                    if (i + 2 < text.length && text[i + 1] == ' ' && text[i + 2] == ']') {
                        // Unchecked checkbox
                        result.append("<input type='checkbox' disabled>")
                        i += 3
                        continue
                    } else if (i + 2 < text.length && text[i + 1] == 'x' && text[i + 2] == ']') {
                        // Checked checkbox
                        result.append("<input type='checkbox' disabled checked>")
                        i += 3
                        continue
                    } else {
                        // Link
                        val linkMatch = parseLinkOrImage(text, i, false)
                        if (linkMatch != null) {
                            result.append("<a href='${linkMatch.url}'>${linkMatch.text}</a>")
                            i = linkMatch.endIndex
                            continue
                        }
                    }
                    result.append(char)
                }

                '*' -> { // Bold or italic
                    val boldMatch = parseBoldOrItalic(text, i, true)
                    if (boldMatch != null) {
                        result.append("<strong>${boldMatch.content}</strong>")
                        i = boldMatch.endIndex
                        continue
                    }
                    val italicMatch = parseBoldOrItalic(text, i, false)
                    if (italicMatch != null) {
                        result.append("<em>${italicMatch.content}</em>")
                        i = italicMatch.endIndex
                        continue
                    }
                    result.append(char)
                }

                '_' -> { // Bold or italic (underscore)
                    val boldMatch = parseBoldOrItalic(text, i, true)
                    if (boldMatch != null) {
                        result.append("<strong>${boldMatch.content}</strong>")
                        i = boldMatch.endIndex
                        continue
                    }
                    val italicMatch = parseBoldOrItalic(text, i, false)
                    if (italicMatch != null) {
                        result.append("<em>${italicMatch.content}</em>")
                        i = italicMatch.endIndex
                        continue
                    }
                    result.append(char)
                }

                '~' -> { // Strikethrough
                    if (i + 1 < text.length && text[i + 1] == '~') {
                        var end = i + 2
                        while (end < text.length && !(text[end] == '~' && end + 1 < text.length && text[end + 1] == '~')) {
                            end++
                        }
                        if (end + 1 < text.length) {
                            val content = convertInlineMarkup(text.substring(i + 2, end)) // Recursively process nested markup
                            result.append("<s>$content</s>")
                            i = end + 2
                            continue
                        }
                    }
                    result.append(char)
                }

                '&', '<', '>' -> { // HTML entities
                    result.append(char.toString().escapeHtml())
                }

                else -> {
                    result.append(char)
                }
            }
            i++
        }

        return result.toString()
    }

    private data class LinkMatch(val text: String, val url: String, val endIndex: Int)

    private fun parseLinkOrImage(text: String, startIndex: Int, isImage: Boolean): LinkMatch? {
        val bracketStart = if (isImage) startIndex + 2 else startIndex + 1 // Skip ![ or [

        // Find closing ]
        var bracketEnd = bracketStart
        while (bracketEnd < text.length && text[bracketEnd] != ']') {
            bracketEnd++
        }
        if (bracketEnd >= text.length || bracketEnd + 1 >= text.length || text[bracketEnd + 1] != '(') {
            return null
        }

        val textContent = text.substring(bracketStart, bracketEnd)

        // Find closing )
        var parenEnd = bracketEnd + 2
        while (parenEnd < text.length && text[parenEnd] != ')') {
            parenEnd++
        }
        if (parenEnd >= text.length) {
            return null
        }

        val url = text.substring(bracketEnd + 2, parenEnd)
        return LinkMatch(textContent, url, parenEnd + 1)
    }

    private data class BoldItalicMatch(val content: String, val endIndex: Int)

    private fun parseBoldOrItalic(text: String, startIndex: Int, isBold: Boolean): BoldItalicMatch? {
        val marker = text[startIndex]
        val requiredLength = if (isBold) 2 else 1

        // Check if we have enough markers
        for (j in 0 until requiredLength) {
            if (startIndex + j >= text.length || text[startIndex + j] != marker) {
                return null
            }
        }

        // Find closing markers
        var end = startIndex + requiredLength
        var found = false
        while (end + requiredLength <= text.length) {
            if (text[end] == marker) {
                found = true
                for (j in 1 until requiredLength) {
                    if (text[end + j] != marker) {
                        found = false
                        break
                    }
                }
                if (found) break
            }
            end++
        }

        if (!found) return null

        val content = text.substring(startIndex + requiredLength, end)
        return BoldItalicMatch(convertInlineMarkup(content), end + requiredLength) // Recursively process nested markup
    }

    /**
     * Extract file extension from filename.
     * 
     * @param filename The filename to extract extension from
     * @return The file extension with dot (e.g., ".md") or empty string if no extension
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
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
            }

            // Skip validation inside code blocks
            if (inCodeBlock) return@forEachIndexed

            // Check for unclosed brackets in links
            val linkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
            val imageLinkPattern = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

            // Remove valid links/images, then check for orphaned brackets
            var testLine = trimmed
            testLine = testLine.replace(linkPattern, "")
            testLine = testLine.replace(imageLinkPattern, "")

            val openBrackets = testLine.count { it == '[' }
            val closeBrackets = testLine.count { it == ']' }
            if (openBrackets != closeBrackets) {
                errors.add("Line ${index + 1}: Unclosed brackets")
            }

            val openParens = testLine.count { it == '(' }
            val closeParens = testLine.count { it == ')' }
            if (openParens != closeParens) {
                errors.add("Line ${index + 1}: Unclosed parentheses")
            }
        }

        return errors
    }

    companion object {
        /**
         * Supported file extensions for Markdown format.
         */
        val EXTENSIONS = setOf(".md", ".markdown", ".mdown", ".mkd")
    }
}

/**
 * Register the Markdown parser with the registry
 */
fun registerMarkdownParser() {
    ParserRegistry.register(MarkdownParser())
}
