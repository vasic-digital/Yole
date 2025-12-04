/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WikiText Format Parser - Platform Agnostic
 * Handles WikiText/Zim Wiki format
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.*

/**
 * WikiText format parser
 * Handles WikiText/Zim Wiki markup
 */
class WikitextParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_WIKITEXT) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Remove Zim header if present
        val contentWithoutHeader = removeZimHeader(content)

        // Convert to HTML
        val html = convertToHtml(contentWithoutHeader)

        val metadata = buildMap {
            put("extension", extension)
            put("lines", content.lines().size.toString())
            put("hasZimHeader", (content != contentWithoutHeader).toString())
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
     * Remove Zim Wiki header from content
     */
    private fun removeZimHeader(content: String): String {
        val zimHeaderRegex = Regex("""(?s)^\[DocumentAttributes\].*?\n\n""")
        return content.replace(zimHeaderRegex, "")
    }

    /**
     * Convert WikiText to HTML
     */
    private fun convertToHtml(content: String): String {
        val lines = content.lines()
        val html = StringBuilder()

        html.append("<div class='wikitext'>")
        html.append(StyleSheets.WIKITEXT_STYLES)

        var inCodeBlock = false
        var inUnorderedList = false
        var inOrderedList = false
        var inCheckList = false

        for ((index, line) in lines.withIndex()) {
            // Handle code blocks
            if (line.trim() == "'''") {
                if (inCodeBlock) {
                    html.append("</pre>")
                    inCodeBlock = false
                } else {
                    html.append("<pre>")
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                html.append(line.escapeHtml())
                html.append("\n")
                continue
            }

            // Detect line types
            val isUnorderedList = line.trimStart().startsWith("* ")
            val isOrderedList = Regex("^\\s*[0-9a-zA-Z]\\. ").matches(line)
            val isCheckList = Regex("^\\s*\\[[ x*><]\\] ").matches(line)
            val isEmpty = line.trim().isEmpty()

            // Handle checklist
            if (isCheckList) {
                if (!inCheckList) {
                    // Close other lists if open
                    if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                    if (inOrderedList) { html.append("</ol>"); inOrderedList = false }
                    html.append("<ul class='checklist'>")
                    inCheckList = true
                }
                html.append(convertLine(line))
            } else if (isUnorderedList) {
                if (!inUnorderedList) {
                    // Close other lists if open
                    if (inCheckList) { html.append("</ul>"); inCheckList = false }
                    if (inOrderedList) { html.append("</ol>"); inOrderedList = false }
                    html.append("<ul>")
                    inUnorderedList = true
                }
                html.append(convertLine(line))
            } else if (isOrderedList) {
                if (!inOrderedList) {
                    // Close other lists if open
                    if (inCheckList) { html.append("</ul>"); inCheckList = false }
                    if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                    html.append("<ol>")
                    inOrderedList = true
                }
                html.append(convertLine(line))
            } else {
                // Not a list item - close any open lists
                if (inCheckList) { html.append("</ul>"); inCheckList = false }
                if (inUnorderedList) { html.append("</ul>"); inUnorderedList = false }
                if (inOrderedList) { html.append("</ol>"); inOrderedList = false }

                if (!isEmpty) {
                    html.append(convertLine(line))
                }
            }
        }

        // Close any open lists at the end
        if (inCheckList) html.append("</ul>")
        if (inUnorderedList) html.append("</ul>")
        if (inOrderedList) html.append("</ol>")

        html.append("</div>")
        return html.toString()
    }

    /**
     * Convert a single line of WikiText to HTML
     */
    private fun convertLine(line: String): String {
        var result = line

        // Headings: == Heading == (more = means smaller heading)
        val headingMatch = Regex("^(={2,6})\\s+(.+?)\\s+\\1$").find(result)
        if (headingMatch != null) {
            val level = 7 - headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2]
            return "<h$level>${convertInlineMarkup(text)}</h$level>"
        }

        // Checklists: [ ], [*], [x], [>], [<]
        val checklistMatch = Regex("^(\\s*)(\\[)([ x*><])(\\])\\s+(.+)$").find(result)
        if (checklistMatch != null) {
            val indent = checklistMatch.groupValues[1]
            val symbol = checklistMatch.groupValues[3]
            val text = checklistMatch.groupValues[5]
            val classAttr = when (symbol) {
                "*" -> " class='checked'"
                "x" -> " class='crossed'"
                else -> ""
            }
            return "$indent<li$classAttr>${convertInlineMarkup(text)}</li>"
        }

        // Unordered list: * item
        if (result.trimStart().startsWith("* ")) {
            val indent = result.takeWhile { it.isWhitespace() }
            val text = result.trimStart().substring(2)
            return "$indent<li>${convertInlineMarkup(text)}</li>"
        }

        // Ordered list: 1. item or a. item
        val orderedListMatch = Regex("^(\\s*)([0-9a-zA-Z])\\.\\s+(.+)$").find(result)
        if (orderedListMatch != null) {
            val indent = orderedListMatch.groupValues[1]
            val text = orderedListMatch.groupValues[3]
            return "$indent<li>${convertInlineMarkup(text)}</li>"
        }

        // Regular paragraph
        if (result.trim().isNotEmpty()) {
            return "<p>${convertInlineMarkup(result)}</p>"
        }

        return ""
    }

    /**
     * Convert inline WikiText markup to HTML
     */
    private fun convertInlineMarkup(text: String): String {
        var result = text

        // Process inline code FIRST (before escaping) to preserve content
        result = result.replace(Regex("""''(?!')(.+?)''""")) { match ->
            val code = match.groupValues[1].escapeHtml()
            "##CODE_START##$code##CODE_END##"
        }

        // Process links and images BEFORE escaping
        result = result.replace(Regex("""\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")) { match ->
            val link = match.groupValues[1]
            val description = match.groupValues[2].ifEmpty { link }
            "##LINK_START##$link##LINK_SEP##$description##LINK_END##"
        }

        result = result.replace(Regex("""\{\{([^}]+)\}\}""")) { match ->
            val img = match.groupValues[1]
            "##IMG_START##$img##IMG_END##"
        }

        // Now escape HTML in the remaining text
        result = result.escapeHtml()

        // Bold: **text**
        result = result.replace(Regex("""(?<!\*)\*\*(?!\*)([^*]+)\*\*(?!\*)""")) { "<strong>${it.groupValues[1]}</strong>" }

        // Italics: //text//
        result = result.replace(Regex("""(?<!/)//(?!/)([^/]+)//(?!/)""")) { "<em>${it.groupValues[1]}</em>" }

        // Highlighted: __text__
        result = result.replace(Regex("""(?<!_)__(?!_)([^_]+)__(?!_)""")) { "<span class='highlight'>${it.groupValues[1]}</span>" }

        // Strikethrough: ~~text~~
        result = result.replace(Regex("""(?<!~)~~(?!~)([^~]+)~~(?!~)""")) { "<s>${it.groupValues[1]}</s>" }

        // Superscript: ^{text}
        result = result.replace(Regex("""\^\{([^}]+)\}""")) { "<sup>${it.groupValues[1]}</sup>" }

        // Subscript: _{text}
        result = result.replace(Regex("""_\{([^}]+)\}""")) { "<sub>${it.groupValues[1]}</sub>" }

        // Restore inline code
        result = result.replace(Regex("""##CODE_START##(.+?)##CODE_END##""")) { "<code>${it.groupValues[1]}</code>" }

        // Restore links
        result = result.replace(Regex("""##LINK_START##(.+?)##LINK_SEP##(.+?)##LINK_END##""")) { match ->
            "<a href='${match.groupValues[1]}'>${match.groupValues[2]}</a>"
        }

        // Restore images
        result = result.replace(Regex("""##IMG_START##(.+?)##IMG_END##""")) { match ->
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
            // Check for malformed headings (unbalanced = signs)
            val headingMatch = Regex("^(={2,6})\\s+(.+?)\\s+(={2,6})$").find(line)
            if (headingMatch != null) {
                val leftEquals = headingMatch.groupValues[1].length
                val rightEquals = headingMatch.groupValues[3].length
                if (leftEquals != rightEquals) {
                    errors.add("Line ${index + 1}: Unbalanced heading markers (left=$leftEquals, right=$rightEquals)")
                }
            }

            // Check for unclosed brackets in links
            val openBrackets = line.count { it == '[' }
            val closeBrackets = line.count { it == ']' }
            if (openBrackets != closeBrackets) {
                errors.add("Line ${index + 1}: Unclosed brackets in links")
            }

            // Check for unclosed braces in images
            val openBraces = line.count { it == '{' }
            val closeBraces = line.count { it == '}' }
            if (openBraces != closeBraces) {
                errors.add("Line ${index + 1}: Unclosed braces in images")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".wiki", ".wikitext", ".txt")
    }
}

/**
 * Register the WikiText parser with the registry
 */
fun registerWikitextParser() {
    ParserRegistry.register(WikitextParser())
}
