/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform LaTeX Parser
 * Platform-agnostic LaTeX parsing and HTML conversion
 *
 *########################################################*/
package digital.vasic.yole.format.latex

import digital.vasic.yole.format.*

/**
 * LaTeX parser implementation.
 * Supports basic LaTeX document structure and mathematical expressions.
 */
class LatexParser : TextParser {
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_LATEX }

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val metadata = extractMetadata(content)
        val errors = validate(content)
        
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content, // Raw content for now, will be converted to HTML in toHtml
            metadata = metadata,
            errors = errors
        )
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return convertLatexToHtml(document.rawContent, lightMode)
    }

    override fun validate(content: String): List<String> {
        val errors = mutableListOf<String>()
        
        val lines = content.lines()
        var inMathMode = false
        var inEnvironment = false
        var currentEnvironment = ""
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check for unclosed math mode
            if (line.contains("\\[") || line.contains("\\begin{equation}")) {
                inMathMode = true
            }
            if (line.contains("\\]") || line.contains("\\end{equation}")) {
                inMathMode = false
            }
            
            // Check for environment boundaries
            val beginMatch = Regex("\\\\begin\\{([^}]+)\\}").find(line)
            if (beginMatch != null) {
                inEnvironment = true
                currentEnvironment = beginMatch.groupValues[1]
            }
            
            val endMatch = Regex("\\\\end\\{([^}]+)\\}").find(line)
            if (endMatch != null) {
                val endEnv = endMatch.groupValues[1]
                if (endEnv == currentEnvironment || currentEnvironment.isEmpty()) {
                    inEnvironment = false
                    currentEnvironment = ""
                } else {
                    errors.add("Line $lineNumber: Mismatched environment end: $endEnv (expected: $currentEnvironment)")
                }
            }
            
            // Check for malformed commands
            val malformedCommands = Regex("\\\\[^a-zA-Z@]+").findAll(line)
            malformedCommands.forEach { match ->
                errors.add("Line $lineNumber: Malformed LaTeX command: ${match.value}")
            }
            
            // Check for unescaped special characters
            if (line.contains("&") && !line.contains("\\\\&") && !inMathMode) {
                errors.add("Line $lineNumber: Unescaped ampersand (&)")
            }
            
            if (line.contains("#") && !line.contains("\\\\#") && !inMathMode) {
                errors.add("Line $lineNumber: Unescaped hash (#)")
            }
        }
        
        // Check for unclosed blocks
        if (inMathMode) {
            errors.add("Unclosed math mode")
        }
        if (inEnvironment) {
            errors.add("Unclosed environment: $currentEnvironment")
        }
        
        return errors
    }

    /**
     * Extract metadata from LaTeX content
     */
    private fun extractMetadata(content: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        val lines = content.lines()
        
        lines.take(50).forEach { line ->
            when {
                line.startsWith("\\title{") -> {
                    val title = extractBraceContent(line, "title")
                    metadata["title"] = title
                }
                line.startsWith("\\author{") -> {
                    val author = extractBraceContent(line, "author")
                    metadata["author"] = author
                }
                line.startsWith("\\date{") -> {
                    val date = extractBraceContent(line, "date")
                    metadata["date"] = date
                }
                line.startsWith("\\documentclass{") -> {
                    val docClass = extractBraceContent(line, "documentclass")
                    metadata["documentclass"] = docClass
                }
                line.startsWith("%") -> {
                    // Comment - could contain metadata
                    val comment = line.substring(1).trim()
                    if (comment.contains(":")) {
                        val parts = comment.split(":", limit = 2)
                        if (parts.size == 2) {
                            metadata[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
            }
        }
        
        return metadata
    }

    /**
     * Extract content from LaTeX command braces
     */
    private fun extractBraceContent(line: String, command: String): String {
        val pattern = Regex("\\\\$command\\{([^}]*)\\}")
        val match = pattern.find(line)
        return match?.groupValues?.get(1) ?: ""
    }

    /**
     * Convert LaTeX content to HTML
     */
    private fun convertLatexToHtml(content: String, lightMode: Boolean): String {
        val lines = content.lines()
        val html = StringBuilder()

        html.append("<div class='latex'>\n")
        html.append(StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode))
        
        var inDocument = false
        var inMathMode = false
        var inEnvironment = false
        var currentEnvironment = ""
        var inItemize = false
        var inEnumerate = false
        
        lines.forEach { line ->
            when {
                // Document structure
                line.contains("\\documentclass") -> {
                    inDocument = true
                    html.append("<div class='document-header'>\n")
                }
                
                line.contains("\\begin{document}") -> {
                    html.append("</div>\n<div class='document-body'>\n")
                }
                
                line.contains("\\end{document}") -> {
                    html.append("</div>\n")
                    inDocument = false
                }
                
                // Title, author, date
                line.startsWith("\\title{") -> {
                    val title = extractBraceContent(line, "title")
                    html.append("<div class='document-title'>${title.escapeHtml()}</div>\n")
                }
                
                line.startsWith("\\author{") -> {
                    val author = extractBraceContent(line, "author")
                    html.append("<div class='document-author'>${author.escapeHtml()}</div>\n")
                }
                
                line.startsWith("\\date{") -> {
                    val date = extractBraceContent(line, "date")
                    html.append("<div class='document-date'>${date.escapeHtml()}</div>\n")
                }
                
                // Sections
                line.startsWith("\\section{") -> {
                    val title = extractBraceContent(line, "section")
                    html.append("<div class='section'>${title.escapeHtml()}</div>\n")
                }
                
                line.startsWith("\\subsection{") -> {
                    val title = extractBraceContent(line, "subsection")
                    html.append("<div class='subsection'>${title.escapeHtml()}</div>\n")
                }
                
                line.startsWith("\\paragraph{") -> {
                    val title = extractBraceContent(line, "paragraph")
                    html.append("<div class='paragraph'>${title.escapeHtml()}</div>\n")
                }
                
                // Math mode
                line.contains("\\[") -> {
                    inMathMode = true
                    html.append("<div class='math math-display'>")
                }
                
                line.contains("\\]") -> {
                    inMathMode = false
                    html.append("</div>\n")
                }
                
                line.contains("\$") -> {
                    // Inline math - simplified handling
                    val processed = line.replace("\$", "<span class='math math-inline'>")
                    html.append("<p>${processed.escapeHtml()}</p>\n")
                }
                
                // Environments
                line.contains("\\begin{") -> {
                    val envMatch = Regex("\\\\begin\\{([^}]+)\\}").find(line)
                    if (envMatch != null) {
                        inEnvironment = true
                        currentEnvironment = envMatch.groupValues[1]
                        html.append("<div class='environment'>")
                        html.append("<div class='environment-title'>${currentEnvironment.escapeHtml()}</div>\n")
                    }
                }
                
                line.contains("\\end{") -> {
                    inEnvironment = false
                    currentEnvironment = ""
                    html.append("</div>\n")
                }
                
                // Lists
                line.contains("\\begin{itemize}") -> {
                    inItemize = true
                    html.append("<ul class='itemize'>\n")
                }
                
                line.contains("\\end{itemize}") -> {
                    inItemize = false
                    html.append("</ul>\n")
                }
                
                line.contains("\\begin{enumerate}") -> {
                    inEnumerate = true
                    html.append("<ol class='enumerate'>\n")
                }
                
                line.contains("\\end{enumerate}") -> {
                    inEnumerate = false
                    html.append("</ol>\n")
                }
                
                line.startsWith("\\item") -> {
                    val itemContent = line.substringAfter("\\item").trim()
                    html.append("<li class='item'>${itemContent.escapeHtml()}</li>\n")
                }
                
                // Text formatting
                line.contains("\\textbf{") -> {
                    val content = extractBraceContent(line, "textbf")
                    html.append("<span class='bold'>${content.escapeHtml()}</span>")
                }
                
                line.contains("\\textit{") -> {
                    val content = extractBraceContent(line, "textit")
                    html.append("<span class='italic'>${content.escapeHtml()}</span>")
                }
                
                line.contains("\\underline{") -> {
                    val content = extractBraceContent(line, "underline")
                    html.append("<span class='underline'>${content.escapeHtml()}</span>")
                }
                
                // Comments
                line.startsWith("%") -> {
                    // Skip comments
                }
                
                // Regular content
                line.isNotBlank() -> {
                    if (!inMathMode && !inEnvironment && !inItemize && !inEnumerate) {
                        // Skip document structure commands
                        if (!line.contains("\\") || line.contains("\\\n")) {
                            html.append("<p>${line.escapeHtml()}</p>\n")
                        }
                    }
                }
            }
        }
        
        html.append("</div>")
        
        return html.toString()
    }
}

/**
 * Register the LaTeX parser with the registry
 */
fun registerLatexParser() {
    ParserRegistry.register(LatexParser())
}