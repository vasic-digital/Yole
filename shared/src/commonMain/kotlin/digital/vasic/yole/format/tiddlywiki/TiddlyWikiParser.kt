/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TiddlyWiki Format Parser - Platform Agnostic
 * Handles TiddlyWiki wikitext format
 *
 *########################################################*/
package digital.vasic.yole.format.tiddlywiki

import digital.vasic.yole.format.*

/**
 * TiddlyWiki metadata fields
 */
data class TiddlerMetadata(
    val title: String? = null,
    val tags: List<String> = emptyList(),
    val created: String? = null,
    val modified: String? = null,
    val type: String? = null,
    val customFields: Map<String, String> = emptyMap()
)

/**
 * TiddlyWiki format parser
 * Handles TiddlyWiki wikitext markup
 */
class TiddlyWikiParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_TIDDLYWIKI) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Parse metadata and content
        val (metadata, textContent) = parseMetadataAndContent(content)

        // Convert to HTML
        val html = convertToHtml(textContent, metadata)

        val documentMetadata = buildMap {
            put("extension", extension)
            put("lines", content.lines().size.toString())
            metadata.title?.let { put("title", it) }
            if (metadata.tags.isNotEmpty()) {
                put("tags", metadata.tags.joinToString(", "))
            }
        }

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = html,
            metadata = documentMetadata
        )
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return document.parsedContent
    }

    /**
     * Parse TiddlyWiki metadata and content
     * TiddlyWiki files have metadata fields at the top:
     * title: My Tiddler
     * tags: tag1 tag2
     * created: timestamp
     *
     * Content starts after a blank line
     */
    private fun parseMetadataAndContent(content: String): Pair<TiddlerMetadata, String> {
        val lines = content.lines()
        val metadataFields = mutableMapOf<String, String>()
        var contentStartIndex = 0
        var inMetadata = true

        for ((index, line) in lines.withIndex()) {
            if (inMetadata) {
                if (line.trim().isEmpty()) {
                    // Empty line marks end of metadata
                    contentStartIndex = index + 1
                    inMetadata = false
                    break
                }

                // Parse metadata field: key: value
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim().lowercase()
                    val value = line.substring(colonIndex + 1).trim()
                    metadataFields[key] = value
                } else {
                    // Not a metadata field, content starts here
                    contentStartIndex = index
                    break
                }
            }
        }

        val textContent = lines.drop(contentStartIndex).joinToString("\n")

        val metadata = TiddlerMetadata(
            title = metadataFields["title"],
            tags = metadataFields["tags"]?.split(Regex("\\s+")) ?: emptyList(),
            created = metadataFields["created"],
            modified = metadataFields["modified"],
            type = metadataFields["type"],
            customFields = metadataFields.filterKeys {
                it !in setOf("title", "tags", "created", "modified", "type")
            }
        )

        return Pair(metadata, textContent)
    }

    /**
     * Convert TiddlyWiki wikitext to HTML
     */
    private fun convertToHtml(content: String, metadata: TiddlerMetadata): String {
        val html = StringBuilder()

        html.append("<div class='tiddlywiki'>")
        html.append("<style>")
        html.append(".tiddlywiki { font-family: sans-serif; line-height: 1.6; }")
        html.append(".tiddlywiki .metadata { background-color: #f5f5f5; border: 1px solid #ddd; padding: 10px; margin-bottom: 20px; }")
        html.append(".tiddlywiki .metadata .title { font-size: 1.2em; font-weight: bold; margin-bottom: 5px; }")
        html.append(".tiddlywiki .metadata .tags { color: #666; }")
        html.append(".tiddlywiki .metadata .tag { background-color: #e0e0e0; padding: 2px 6px; margin: 0 2px; border-radius: 3px; }")
        html.append(".tiddlywiki h1 { font-size: 2em; font-weight: bold; margin: 0.67em 0; }")
        html.append(".tiddlywiki h2 { font-size: 1.8em; font-weight: bold; margin: 0.75em 0; }")
        html.append(".tiddlywiki h3 { font-size: 1.6em; font-weight: bold; margin: 0.83em 0; }")
        html.append(".tiddlywiki h4 { font-size: 1.4em; font-weight: bold; margin: 1em 0; }")
        html.append(".tiddlywiki h5 { font-size: 1.2em; font-weight: bold; margin: 1.17em 0; }")
        html.append(".tiddlywiki h6 { font-size: 1em; font-weight: bold; margin: 1.33em 0; }")
        html.append(".tiddlywiki ul { list-style-type: disc; margin: 1em 0; padding-left: 40px; }")
        html.append(".tiddlywiki ol { list-style-type: decimal; margin: 1em 0; padding-left: 40px; }")
        html.append(".tiddlywiki blockquote { border-left: 4px solid #ccc; padding-left: 16px; margin: 16px 0; color: #666; }")
        html.append(".tiddlywiki hr { border: none; border-top: 1px solid #ccc; margin: 1em 0; }")
        html.append(".tiddlywiki pre { background-color: #f0f0f0; padding: 10px; overflow-x: auto; font-family: monospace; }")
        html.append(".tiddlywiki code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; }")
        html.append(".tiddlywiki a { color: #0066cc; text-decoration: none; }")
        html.append(".tiddlywiki a:hover { text-decoration: underline; }")
        html.append("</style>")

        // Add metadata display if present
        if (metadata.title != null || metadata.tags.isNotEmpty()) {
            html.append("<div class='metadata'>")
            metadata.title?.let {
                html.append("<div class='title'>${it.escapeHtml()}</div>")
            }
            if (metadata.tags.isNotEmpty()) {
                html.append("<div class='tags'>")
                metadata.tags.forEach { tag ->
                    html.append("<span class='tag'>${tag.escapeHtml()}</span>")
                }
                html.append("</div>")
            }
            html.append("</div>")
        }

        val lines = content.lines()
        var inUnorderedList = false
        var inOrderedList = false
        var inCodeBlock = false
        var inBlockQuote = false
        var currentListLevel = 0

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Handle code blocks (triple backticks)
            if (trimmed.startsWith("```")) {
                if (!inCodeBlock) {
                    html.append("<pre>")
                    inCodeBlock = true
                } else {
                    html.append("</pre>")
                    inCodeBlock = false
                }
                i++
                continue
            } else if (inCodeBlock) {
                html.append(line.escapeHtml())
                html.append("\n")
                i++
                continue
            }

            // Handle block quotes <<<...<<<
            if (trimmed == "<<<") {
                if (!inBlockQuote) {
                    html.append("<blockquote>")
                    inBlockQuote = true
                } else {
                    html.append("</blockquote>")
                    inBlockQuote = false
                }
                i++
                continue
            }

            // Detect line types
            val unorderedListMatch = Regex("^(\\*+)\\s+(.+)$").find(trimmed)
            val orderedListMatch = Regex("^(#+)\\s+(.+)$").find(trimmed)
            val isEmpty = trimmed.isEmpty()

            // Handle lists
            if (unorderedListMatch != null) {
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
                // Not a list - close any open lists
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

                if (!isEmpty) {
                    html.append(convertLine(trimmed))
                }
            }

            i++
        }

        // Close any open structures
        if (inUnorderedList) html.append("</ul>".repeat(currentListLevel))
        if (inOrderedList) html.append("</ol>".repeat(currentListLevel))
        if (inCodeBlock) html.append("</pre>")
        if (inBlockQuote) html.append("</blockquote>")

        html.append("</div>")
        return html.toString()
    }

    /**
     * Convert a single line of TiddlyWiki wikitext to HTML
     */
    private fun convertLine(line: String): String {
        val trimmed = line.trim()

        // Headings: ! H1, !! H2, !!! H3, etc.
        val headingMatch = Regex("^(!+)\\s+(.+)$").find(trimmed)
        if (headingMatch != null) {
            val level = minOf(headingMatch.groupValues[1].length, 6)
            val text = headingMatch.groupValues[2]
            return "<h$level>${convertInlineMarkup(text)}</h$level>"
        }

        // Horizontal rule: ---
        if (trimmed.matches(Regex("^-{3,}$"))) {
            return "<hr>"
        }

        // Block quote (single line): > text
        if (trimmed.startsWith("> ")) {
            val text = trimmed.substring(2)
            return "<blockquote>${convertInlineMarkup(text)}</blockquote>"
        }

        // Paragraph
        if (trimmed.isNotEmpty()) {
            return "<p>${convertInlineMarkup(trimmed)}</p>"
        }

        return ""
    }

    /**
     * Convert inline TiddlyWiki wikitext markup to HTML
     */
    private fun convertInlineMarkup(text: String): String {
        var result = text

        // Process ALL markup BEFORE escaping HTML (using placeholders)

        // Inline code: `code`
        result = result.replace(Regex("""`([^`]+)`""")) { match ->
            val code = match.groupValues[1].escapeHtml()
            "##CODE##$code##/CODE##"
        }

        // Internal links: [[Link]] or [[Link|Description]]
        result = result.replace(Regex("""\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")) { match ->
            val link = match.groupValues[1]
            val description = match.groupValues[2].ifEmpty { link }
            "\u0000LINK\u0000$link\u0000SEP\u0000$description\u0000/LINK\u0000"
        }

        // External links: [ext[URL]] or [ext[Description|URL]]
        result = result.replace(Regex("""\[ext\[([^|\]]+)(?:\|([^\]]+))?\]\]""")) { match ->
            val url = match.groupValues[1]
            val description = match.groupValues[2].ifEmpty { url }
            "\u0000EXTLINK\u0000$url\u0000SEP\u0000$description\u0000/EXTLINK\u0000"
        }

        // Images: [img[image.png]]
        result = result.replace(Regex("""\[img\[([^\]]+)\]\]""")) { match ->
            val img = match.groupValues[1]
            "\u0000IMG\u0000$img\u0000/IMG\u0000"
        }

        // Bold: ''text'' - but NOT inside placeholders
        result = result.replace(Regex("""''(.+?)''""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000BOLD\u0000${match.groupValues[1]}\u0000/BOLD\u0000"
        }

        // Italics: //text// - but NOT inside placeholders
        result = result.replace(Regex("""//(.+?)//""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000ITALIC\u0000${match.groupValues[1]}\u0000/ITALIC\u0000"
        }

        // Underline: __text__ - but NOT inside placeholders
        result = result.replace(Regex("""__(.+?)__""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000UNDERLINE\u0000${match.groupValues[1]}\u0000/UNDERLINE\u0000"
        }

        // Strikethrough: ~~text~~ - but NOT inside placeholders
        result = result.replace(Regex("""~~(.+?)~~""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000STRIKE\u0000${match.groupValues[1]}\u0000/STRIKE\u0000"
        }

        // Superscript: ^^text^^ - but NOT inside placeholders
        result = result.replace(Regex("""\^\^(.+?)\^\^""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000SUP\u0000${match.groupValues[1]}\u0000/SUP\u0000"
        }

        // Subscript: ,,text,, - but NOT inside placeholders
        result = result.replace(Regex(""",,(.+?),,""")) { match ->
            if (match.value.contains("\u0000")) match.value
            else "\u0000SUB\u0000${match.groupValues[1]}\u0000/SUB\u0000"
        }

        // NOW escape HTML in remaining text
        result = result.escapeHtml()

        // Restore all markup with proper HTML tags
        result = result.replace(Regex("""##CODE##(.+?)##/CODE##""")) { "<code>${it.groupValues[1]}</code>" }
        result = result.replace(Regex("""\u0000BOLD\u0000(.+?)\u0000/BOLD\u0000""")) { "<strong>${it.groupValues[1]}</strong>" }
        result = result.replace(Regex("""\u0000ITALIC\u0000(.+?)\u0000/ITALIC\u0000""")) { "<em>${it.groupValues[1]}</em>" }
        result = result.replace(Regex("""\u0000UNDERLINE\u0000(.+?)\u0000/UNDERLINE\u0000""")) { "<u>${it.groupValues[1]}</u>" }
        result = result.replace(Regex("""\u0000STRIKE\u0000(.+?)\u0000/STRIKE\u0000""")) { "<s>${it.groupValues[1]}</s>" }
        result = result.replace(Regex("""\u0000SUP\u0000(.+?)\u0000/SUP\u0000""")) { "<sup>${it.groupValues[1]}</sup>" }
        result = result.replace(Regex("""\u0000SUB\u0000(.+?)\u0000/SUB\u0000""")) { "<sub>${it.groupValues[1]}</sub>" }

        result = result.replace(Regex("""\u0000LINK\u0000(.+?)\u0000SEP\u0000(.+?)\u0000/LINK\u0000""")) { match ->
            "<a href='${match.groupValues[1]}'>${match.groupValues[2]}</a>"
        }

        result = result.replace(Regex("""\u0000EXTLINK\u0000(.+?)\u0000SEP\u0000(.+?)\u0000/EXTLINK\u0000""")) { match ->
            "<a href='${match.groupValues[1]}' target='_blank'>${match.groupValues[2]}</a>"
        }

        result = result.replace(Regex("""\u0000IMG\u0000(.+?)\u0000/IMG\u0000""")) { match ->
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
            val openBrackets = trimmed.count { it == '[' }
            val closeBrackets = trimmed.count { it == ']' }
            if (openBrackets != closeBrackets) {
                errors.add("Line ${index + 1}: Unclosed brackets in links")
            }

            // Check for unclosed quotes (for bold '')
            // Count occurrences of '' (two single quotes)
            val doubleQuoteCount = Regex("''").findAll(trimmed).count()
            if (doubleQuoteCount % 2 != 0) {
                errors.add("Line ${index + 1}: Unclosed quotes (for bold '')")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".tid", ".tiddler")
    }
}

/**
 * Register the TiddlyWiki parser with the registry
 */
fun registerTiddlyWikiParser() {
    ParserRegistry.register(TiddlyWikiParser())
}
