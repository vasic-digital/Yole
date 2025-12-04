/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * reStructuredText Parser for Kotlin Multiplatform
 * Supports parsing .rst and .rest files
 *
 *########################################################*/
package digital.vasic.yole.format.restructuredtext

import digital.vasic.yole.format.*

/**
 * Parser for reStructuredText (.rst, .rest) files
 */
class RestructuredTextParser : TextParser {
    
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_RESTRUCTUREDTEXT }
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val sections = extractSections(content)
        val directives = extractDirectives(content)
        
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = generateRstHtml(content, true),
            metadata = buildMap {
                put("sections", sections.size.toString())
                put("directives", directives.size.toString())
                put("max_level", sections.maxOfOrNull { it.level }?.toString() ?: "0")
            }
        )
    }
    
    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        val themeClass = if (lightMode) "light" else "dark"
        val styles = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, lightMode)

        return """
            |<div class="rst-document $themeClass">
            |${generateRstHtml(document.rawContent, lightMode)}
            |</div>
            |$styles
        """.trimMargin()
    }
    
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }
    
    override fun validate(content: String): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for invalid section underlines
        val sections = extractSections(content)
        sections.forEach { section ->
            if (section.underline.length < section.title.length) {
                issues.add("Section underline too short for '${section.title}'")
            }
        }
        
        return issues
    }
    
    private fun extractSections(content: String): List<RstSection> {
        val sections = mutableListOf<RstSection>()
        val lines = content.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            if (line.isNotEmpty() && i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                if (isUnderline(nextLine)) {
                    val level = getSectionLevel(nextLine)
                    sections.add(RstSection(
                        level = level,
                        title = line.trim(),
                        underline = nextLine.trim()
                    ))
                    i += 2
                    continue
                }
            }
            i++
        }
        
        return sections
    }
    
    private fun extractDirectives(content: String): List<RstDirective> {
        val directives = mutableListOf<RstDirective>()
        
        val lines = content.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith(".. ") && line.contains("::")) {
                val directiveName = line.substringAfter(".. ").substringBefore("::").trim()
                val content = mutableListOf<String>()
                i++
                
                // Collect directive content (indented lines)
                while (i < lines.size && lines[i].startsWith("   ")) {
                    content.add(lines[i].trimStart())
                    i++
                }
                
                directives.add(RstDirective(
                    name = directiveName,
                    content = content.joinToString("\n")
                ))
            } else {
                i++
            }
        }
        
        return directives
    }
    
    private fun isUnderline(line: String): Boolean {
        val firstChar = line.firstOrNull()
        return firstChar != null && line.all { it == firstChar } && line.length >= 2
    }
    
    private fun getSectionLevel(underline: String): Int {
        val char = underline.first()
        return when (char) {
            '=' -> 1
            '-' -> 2
            '~' -> 3
            '^' -> 4
            '"' -> 5
            else -> 6
        }
    }
    
    private fun generateRstHtml(content: String, lightMode: Boolean): String {
        val lines = content.lines()
        val htmlLines = mutableListOf<String>()
        var inDirective = false
        var currentDirective = ""
        var directiveContent = mutableListOf<String>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                line.startsWith(".. ") && line.contains("::") -> {
                    inDirective = true
                    currentDirective = line.substringAfter(".. ").substringBefore("::").trim()
                    directiveContent.clear()
                    htmlLines.add("<div class=\"rst-directive\">")
                    htmlLines.add("<div class=\"rst-directive-header\">$line</div>")
                    i++
                }
                inDirective && line.startsWith("   ") -> {
                    directiveContent.add(line.trimStart())
                    i++
                }
                inDirective -> {
                    // End of directive
                    inDirective = false
                    htmlLines.add("<div class=\"rst-directive-content\">${directiveContent.joinToString("\n")}</div>")
                    htmlLines.add("</div>")
                }
                isSectionTitle(line, i, lines) -> {
                    // Section heading
                    val title = line
                    val underline = lines[i + 1]
                    val level = getSectionLevel(underline)
                    htmlLines.add("<div class=\"rst-section rst-section-$level\">${escapeHtml(title)}</div>")
                    i += 2
                }
                line.isNotEmpty() -> {
                    // Regular paragraph
                    htmlLines.add("<p>${escapeHtml(line)}</p>")
                    i++
                }
                else -> {
                    // Empty line
                    htmlLines.add("<br>")
                    i++
                }
            }
        }
        
        // Close any open directive
        if (inDirective) {
            htmlLines.add("<div class=\"rst-directive-content\">${directiveContent.joinToString("\n")}</div>")
            htmlLines.add("</div>")
        }
        
        return htmlLines.joinToString("\n")
    }
    
    private fun isSectionTitle(line: String, index: Int, lines: List<String>): Boolean {
        if (index + 1 >= lines.size) return false
        val nextLine = lines[index + 1]
        return isUnderline(nextLine) && line.isNotEmpty()
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
 * Register the reStructuredText parser with the registry
 */
fun registerRestructuredTextParser() {
    ParserRegistry.register(RestructuredTextParser())
}

data class RstSection(
    val level: Int,
    val title: String,
    val underline: String
)

data class RstDirective(
    val name: String,
    val content: String
)