/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Org Mode Parser for Kotlin Multiplatform
 * Supports parsing .org files
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import digital.vasic.yole.format.*

/**
 * Parser for Org Mode (.org) files
 */
class OrgModeParser : TextParser {
    
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_ORGMODE }
    
    /** Parse Org Mode [content] into a [ParsedDocument] with heading, TODO, and property metadata. */
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""

        val headings = extractHeadings(content)
        val todos = extractTodos(content)
        val properties = extractProperties(content)
        val hasDocumentHeader = hasOrgMetadata(content)

        // For structured documents (those with #+TITLE or other document-level
        // metadata), count only top-level (level-1) headings as "sections."
        // For simple documents without metadata headers, count all headings.
        val headingCount = if (hasDocumentHeader && headings.any { it.level == 1 }) {
            headings.count { it.level == 1 }
        } else {
            headings.size
        }

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = generateOrgHtml(content, true),
            metadata = buildMap {
                put("headings", headingCount.toString())
                put("todos", todos.size.toString())
                put("properties", properties.size.toString())
                put("max_level", headings.maxOfOrNull { it.level }?.toString() ?: "0")
            }
        )
    }

    /**
     * Check if the content has Org Mode document-level metadata headers.
     * These are lines starting with #+KEYWORD: that define document properties
     * like #+TITLE:, #+AUTHOR:, #+DATE:, etc.
     *
     * Documents with metadata headers are structured documents where level-1
     * headings define top-level sections. Documents without metadata are
     * simpler and each heading is counted individually.
     */
    private fun hasOrgMetadata(content: String): Boolean {
        val metadataRegex = "^#\\+[A-Z_]+:".toRegex(RegexOption.MULTILINE)
        return metadataRegex.containsMatchIn(content)
    }
    
    /** Convert a parsed Org Mode [document] to themed HTML, respecting [lightMode] for styling. */
    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        val themeClass = if (lightMode) "light" else "dark"
        val styles = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode)

        return """
            |<div class="org-mode-document $themeClass">
            |${generateOrgHtml(document.rawContent, lightMode)}
            |</div>
            |$styles
        """.trimMargin()
    }
    
    /** Return true if this parser supports the given [format]. */
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }
    
    /** Validate Org Mode [content] and return a list of issues such as mismatched block delimiters or excessive heading levels. */
    override fun validate(content: String): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for unclosed blocks
        val blockStarts = content.lines().count { it.startsWith("#+BEGIN_") }
        val blockEnds = content.lines().count { it.startsWith("#+END_") }
        
        if (blockStarts != blockEnds) {
            issues.add("Mismatched block delimiters")
        }
        
        // Check for invalid heading levels
        val headings = extractHeadings(content)
        headings.forEach { heading ->
            if (heading.level > 6) {
                issues.add("Heading level ${heading.level} exceeds maximum of 6")
            }
        }
        
        return issues
    }
    
    private fun extractHeadings(content: String): List<OrgHeading> {
        val headingRegex = "^(\\*+)\\s+(.*)$".toRegex(RegexOption.MULTILINE)
        return headingRegex.findAll(content).map { matchResult ->
            val stars = matchResult.groupValues[1]
            val title = matchResult.groupValues[2]
            OrgHeading(
                level = stars.length,
                title = title.trim(),
                todoState = extractTodoState(title)
            )
        }.toList()
    }
    
    private fun extractTodos(content: String): List<OrgTodo> {
        val todoRegex = "^\\*+\\s+(TODO|DONE)\\s+(.*)$".toRegex(RegexOption.MULTILINE)
        return todoRegex.findAll(content).map { matchResult ->
            val state = matchResult.groupValues[1]
            val title = matchResult.groupValues[2]
            OrgTodo(
                state = state,
                title = title.trim()
            )
        }.toList()
    }
    
    private fun extractProperties(content: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        val propertyRegex = "^:([^:]+):\\s+(.*)$".toRegex(RegexOption.MULTILINE)
        
        propertyRegex.findAll(content).forEach { matchResult ->
            val key = matchResult.groupValues[1].trim()
            val value = matchResult.groupValues[2].trim()
            properties[key] = value
        }
        
        return properties
    }
    
    private fun extractTodoState(title: String): String? {
        val todoRegex = "^(TODO|DONE)\\s+".toRegex()
        return todoRegex.find(title)?.groupValues?.get(1)
    }
    
    private fun generateOrgHtml(content: String, lightMode: Boolean): String {
        val lines = content.lines()
        val htmlLines = mutableListOf<String>()
        var inBlock = false
        var currentBlockType = ""
        var blockContent = mutableListOf<String>()
        var inPropertyDrawer = false

        for (line in lines) {
            when {
                line.startsWith("#+BEGIN_") -> {
                    inBlock = true
                    currentBlockType = line.substringAfter("#+BEGIN_").trim()
                    blockContent.clear()
                    htmlLines.add("<div class=\"org-block\">")
                    htmlLines.add("<div class=\"org-block-header\">$line</div>")
                }
                line.startsWith("#+END_") -> {
                    inBlock = false
                    htmlLines.add("<div class=\"org-block-content\">${blockContent.joinToString("\n")}</div>")
                    htmlLines.add("</div>")
                }
                inBlock -> {
                    blockContent.add(escapeHtml(line))
                }
                line.trim() == ":PROPERTIES:" -> {
                    // Start of property drawer
                    inPropertyDrawer = true
                    htmlLines.add("<div class=\"org-properties\">")
                    htmlLines.add("<div class=\"org-property-header\">PROPERTIES</div>")
                }
                line.trim() == ":END:" -> {
                    // End of property drawer
                    inPropertyDrawer = false
                    htmlLines.add("</div>")
                }
                inPropertyDrawer -> {
                    // Property lines inside drawer
                    val propertyMatch = "^:([^:]+):\\s+(.*)$".toRegex().find(line.trim())
                    if (propertyMatch != null) {
                        val key = propertyMatch.groupValues[1].trim()
                        val value = propertyMatch.groupValues[2].trim()
                        htmlLines.add("<div class=\"org-property\"><span class=\"org-property-key\">$key:</span> <span class=\"org-property-value\">$value</span></div>")
                    } else {
                        htmlLines.add("<p>${escapeHtml(line)}</p>")
                    }
                }
                line.startsWith("*") && line.length > 1 && line.trimStart('*').startsWith(" ") -> {
                    // Heading
                    val level = line.takeWhile { it == '*' }.length
                    val title = line.substring(level).trim()
                    val todoState = extractTodoState(title)

                    val headingClass = "org-heading org-heading-$level"
                    val todoClass = if (todoState != null) " org-todo org-todo-${todoState.lowercase()}" else ""
                    val formattedTitle = formatInlineOrg(title)

                    htmlLines.add("<div class=\"$headingClass$todoClass\">$formattedTitle</div>")
                }
                line.startsWith(":") && line.endsWith(":") && line.length > 2 -> {
                    // Single-line property drawer marker or standalone property reference
                    htmlLines.add("<div class=\"org-properties\">")
                    val properties = extractProperties(line)
                    properties.forEach { (key, value) ->
                        htmlLines.add("<div class=\"org-property\"><span class=\"org-property-key\">$key:</span> <span class=\"org-property-value\">$value</span></div>")
                    }
                    htmlLines.add("</div>")
                }
                line.isNotEmpty() -> {
                    // Regular paragraph
                    htmlLines.add("<p>${formatInlineOrg(line)}</p>")
                }
                else -> {
                    // Empty line
                    htmlLines.add("<br>")
                }
            }
        }

        return htmlLines.joinToString("\n")
    }
    
    private fun formatInlineOrg(text: String): String {
        // Escape HTML entities BEFORE applying inline Org markup. The input is
        // attacker-controlled document text; without this a heading or paragraph
        // containing a raw <script> tag would flow straight into the rendered
        // HTML as a live stored-XSS vector. Org markup tokens (* / _ + ~ [[ ]])
        // are unaffected by entity-escaping, so formatting still works.
        // See P5-FIX-001 / CONST-039.
        return text.escapeHtml()
            .replace("\\*\\*([^*]+)\\*\\*".toRegex()) { match ->
                "<span class=\"org-bold\">${match.groupValues[1]}</span>"
            }
            .replace("\\/([^/]+)\\/".toRegex()) { match ->
                "<span class=\"org-italic\">${match.groupValues[1]}</span>"
            }
            .replace("_([^_]+)_".toRegex()) { match ->
                "<span class=\"org-underline\">${match.groupValues[1]}</span>"
            }
            .replace("\\+([^+]+)\\+".toRegex()) { match ->
                "<span class=\"org-strikethrough\">${match.groupValues[1]}</span>"
            }
            // The verbatim delimiter `="` contains a literal double-quote, which
            // the upstream escapeHtml() has already turned into the &quot; entity
            // — so the delimiter to match here is the escaped form `=&quot;`.
            .replace("=&quot;([^=]+?)=&quot;".toRegex()) { match ->
                "<span class=\"org-verbatim\">${match.groupValues[1]}</span>"
            }
            .replace("~([^~]+)~".toRegex()) { match ->
                "<span class=\"org-code\">${match.groupValues[1]}</span>"
            }
            .replace("\\[\\[([^]]+)\\]\\[([^]]+)\\]\\]".toRegex()) { match ->
                val url = sanitizeUri(match.groupValues[1])
                val text = match.groupValues[2]
                "<a href=\"$url\" class=\"org-link\">$text</a>"
            }
            .replace("\\[\\[([^]]+)\\]\\]".toRegex()) { match ->
                val url = sanitizeUri(match.groupValues[1])
                "<a href=\"$url\" class=\"org-link\">$url</a>"
            }
    }

    /**
     * Neutralize dangerous URI schemes in link targets. A `javascript:` (or
     * `data:` / `vbscript:`) href executes attacker code on click, so such
     * targets are replaced with an inert anchor. The input here has already
     * been HTML-entity-escaped by [formatInlineOrg]. See P5-FIX-001 / CONST-039.
     */
    private fun sanitizeUri(url: String): String {
        val scheme = url.trim().lowercase()
        val dangerous = scheme.startsWith("javascript:") ||
            scheme.startsWith("vbscript:") ||
            scheme.startsWith("data:")
        return if (dangerous) "#" else url
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * Register the Org Mode parser with the registry
 */
fun registerOrgModeParser() {
    ParserRegistry.register(OrgModeParser())
}

/** Represents an Org Mode heading with its nesting [level], display [title], and optional [todoState]. */
data class OrgHeading(
    val level: Int,
    val title: String,
    val todoState: String? = null
)

/** Represents an Org Mode TODO item with its current [state] (e.g., TODO, DONE) and [title]. */
data class OrgTodo(
    val state: String,
    val title: String
)