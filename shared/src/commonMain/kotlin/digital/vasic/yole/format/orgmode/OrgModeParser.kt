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
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        
        val headings = extractHeadings(content)
        val todos = extractTodos(content)
        val properties = extractProperties(content)
        
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = generateOrgHtml(content, true),
            metadata = buildMap {
                put("headings", headings.size.toString())
                put("todos", todos.size.toString())
                put("properties", properties.size.toString())
                put("max_level", headings.maxOfOrNull { it.level }?.toString() ?: "0")
            }
        )
    }
    
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
    
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }
    
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
                line.startsWith("*") -> {
                    // Heading
                    val level = line.takeWhile { it == '*' }.length
                    val title = line.substring(level).trim()
                    val todoState = extractTodoState(title)
                    val cleanTitle = if (todoState != null) title.substringAfter("$todoState ") else title
                    
                    val headingClass = "org-heading org-heading-$level"
                    val todoClass = if (todoState != null) "org-todo org-todo-${todoState.lowercase()}" else ""
                    
                    val todoHtml = if (todoState != null) "<span class=\"$todoClass\">$todoState</span> " else ""
                    val formattedTitle = formatInlineOrg(cleanTitle)
                    
                    htmlLines.add("<div class=\"$headingClass\">$todoHtml$formattedTitle</div>")
                }
                line.startsWith(":") && line.endsWith(":") -> {
                    // Property drawer
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
        return text
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
            .replace("=\"([^=\"]+)=\"".toRegex()) { match ->
                "<span class=\"org-verbatim\">${match.groupValues[1]}</span>"
            }
            .replace("~([^~]+)~".toRegex()) { match ->
                "<span class=\"org-code\">${match.groupValues[1]}</span>"
            }
            .replace("\\[\\[([^]]+)\\]\\[([^]]+)\\]\\]".toRegex()) { match ->
                val url = match.groupValues[1]
                val text = match.groupValues[2]
                "<a href=\"$url\" class=\"org-link\">$text</a>"
            }
            .replace("\\[\\[([^]]+)\\]\\]".toRegex()) { match ->
                val url = match.groupValues[1]
                "<a href=\"$url\" class=\"org-link\">$url</a>"
            }
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

data class OrgHeading(
    val level: Int,
    val title: String,
    val todoState: String? = null
)

data class OrgTodo(
    val state: String,
    val title: String
)