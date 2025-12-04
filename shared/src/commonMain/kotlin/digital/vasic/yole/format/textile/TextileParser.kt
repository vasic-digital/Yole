/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Textile Format Parser - Platform Agnostic
 * Handles Textile markup format
 *
 *########################################################*/
package digital.vasic.yole.format.textile

import digital.vasic.yole.format.*

/**
 * Textile format parser
 * Handles Textile markup language
 */
class TextileParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_TEXTILE) ?: FormatRegistry.formats.last()

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
     * Convert Textile to HTML
     */
    private fun convertToHtml(content: String): String {
        val lines = content.lines()
        val html = StringBuilder()

        html.append("<div class='textile'>")
        html.append("<style>")
        html.append(".textile { font-family: sans-serif; line-height: 1.6; }")
        html.append(".textile h1 { font-size: 2em; font-weight: bold; }")
        html.append(".textile h2 { font-size: 1.8em; font-weight: bold; }")
        html.append(".textile h3 { font-size: 1.6em; font-weight: bold; }")
        html.append(".textile h4 { font-size: 1.4em; font-weight: bold; }")
        html.append(".textile h5 { font-size: 1.2em; font-weight: bold; }")
        html.append(".textile h6 { font-size: 1em; font-weight: bold; }")
        html.append(".textile blockquote { border-left: 4px solid #ccc; padding-left: 16px; margin: 16px 0; color: #666; }")
        html.append(".textile pre { background-color: #f0f0f0; padding: 10px; overflow-x: auto; font-family: monospace; }")
        html.append(".textile code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; }")
        html.append(".textile a { color: #0066cc; text-decoration: none; }")
        html.append(".textile a:hover { text-decoration: underline; }")
        html.append("</style>")

        var inUnorderedList = false
        var inOrderedList = false
        var inPre = false

        for (line in lines) {
            val trimmed = line.trim()

            // Handle pre-formatted blocks
            if (trimmed.startsWith("pre.")) {
                if (!inPre) {
                    html.append("<pre>")
                    inPre = true
                }
                continue
            } else if (inPre && trimmed.isEmpty()) {
                html.append("</pre>")
                inPre = false
                continue
            } else if (inPre) {
                html.append(line.escapeHtml())
                html.append("\n")
                continue
            }

            // Detect line types
            val isUnorderedList = trimmed.startsWith("* ")
            val isOrderedList = trimmed.startsWith("# ")
            val isEmpty = trimmed.isEmpty()

            // Handle lists
            if (isUnorderedList) {
                if (!inUnorderedList) {
                    if (inOrderedList) { html.append("</ol>"); inOrderedList = false }
                    html.append("<ul>")
                    inUnorderedList = true
                }
                val text = trimmed.substring(2)
                html.append("<li>${convertInlineMarkup(text)}</li>")
            } else if (isOrderedList) {
                if (!inOrderedList) {
                    if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                    html.append("<ol>")
                    inOrderedList = true
                }
                val text = trimmed.substring(2)
                html.append("<li>${convertInlineMarkup(text)}</li>")
            } else {
                // Close any open lists
                if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                if (inOrderedList) { html.append("</ol>"); inOrderedList = false }

                if (!isEmpty) {
                    html.append(convertLine(line))
                }
            }
        }

        // Close any open lists
        if (inUnorderedList) html.append("</ul>")
        if (inOrderedList) html.append("</ol>")
        if (inPre) html.append("</pre>")

        html.append("</div>")
        return html.toString()
    }

    /**
     * Convert a single line of Textile to HTML
     */
    private fun convertLine(line: String): String {
        val trimmed = line.trim()

        // Headings: h1. Heading, h2. Heading, etc.
        val headingMatch = Regex("^h([1-6])\\.\\.?\\s+(.+)$").find(trimmed)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1]
            val text = headingMatch.groupValues[2]
            return "<h$level>${convertInlineMarkup(text)}</h$level>"
        }

        // Blockquote: bq. Text
        if (trimmed.startsWith("bq. ")) {
            val text = trimmed.substring(4)
            return "<blockquote>${convertInlineMarkup(text)}</blockquote>"
        }

        // Paragraph
        if (trimmed.isNotEmpty()) {
            return "<p>${convertInlineMarkup(trimmed)}</p>"
        }

        return ""
    }

    /**
     * Convert inline Textile markup to HTML
     */
    private fun convertInlineMarkup(text: String): String {
        var result = text

        // Process inline code and links BEFORE escaping
        result = result.replace(Regex("""@([^@]+)@""")) { match ->
            val code = match.groupValues[1].escapeHtml()
            "##CODE##$code##/CODE##"
        }

        // Links: "text":url
        result = result.replace(Regex(""""([^"]+)":(\S+)""")) { match ->
            val linkText = match.groupValues[1]
            val url = match.groupValues[2]
            "##LINK##$url##SEP##$linkText##/LINK##"
        }

        // Images: !imageurl!
        result = result.replace(Regex("""!([^!]+)!""")) { match ->
            val img = match.groupValues[1]
            "##IMG##$img##/IMG##"
        }

        // Escape HTML
        result = result.escapeHtml()

        // Strong emphasis: **text**
        result = result.replace(Regex("""\*\*([^*]+)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }

        // Bold: *text*
        result = result.replace(Regex("""\*([^*]+)\*""")) { "<b>${it.groupValues[1]}</b>" }

        // Double emphasis: __text__
        result = result.replace(Regex("""__([^_]+)__""")) { "<em><em>${it.groupValues[1]}</em></em>" }

        // Emphasis/Italics: _text_
        result = result.replace(Regex("""_([^_]+)_""")) { "<em>${it.groupValues[1]}</em>" }

        // Strikethrough: -text-
        result = result.replace(Regex("""-([^-]+)-""")) { "<s>${it.groupValues[1]}</s>" }

        // Superscript: ^text^
        result = result.replace(Regex("""\^([^^]+)\^""")) { "<sup>${it.groupValues[1]}</sup>" }

        // Subscript: ~text~
        result = result.replace(Regex("""~([^~]+)~""")) { "<sub>${it.groupValues[1]}</sub>" }

        // Restore inline code
        result = result.replace(Regex("""##CODE##(.+?)##/CODE##""")) { "<code>${it.groupValues[1]}</code>" }

        // Restore links
        result = result.replace(Regex("""##LINK##(.+?)##SEP##(.+?)##/LINK##""")) { match ->
            "<a href='${match.groupValues[1]}'>${match.groupValues[2]}</a>"
        }

        // Restore images
        result = result.replace(Regex("""##IMG##(.+?)##/IMG##""")) { match ->
            "<img src='${match.groupValues[1]}' alt='${match.groupValues[1]}'/>"
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
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Check for malformed headings
            val headingMatch = Regex("^h([0-9])\\.\\.?\\s+").find(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].toIntOrNull() ?: 0
                if (level < 1 || level > 6) {
                    errors.add("Line ${index + 1}: Invalid heading level $level (must be 1-6)")
                }
            }

            // Check for unclosed inline code
            val codeCount = trimmed.count { it == '@' }
            if (codeCount % 2 != 0) {
                errors.add("Line ${index + 1}: Unclosed inline code marker (@)")
            }

            // Check for unclosed images
            val imgCount = trimmed.count { it == '!' }
            if (imgCount % 2 != 0) {
                errors.add("Line ${index + 1}: Unclosed image marker (!)")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".textile", ".txt")
    }
}

/**
 * Register the Textile parser with the registry
 */
fun registerTextileParser() {
    ParserRegistry.register(TextileParser())
}
