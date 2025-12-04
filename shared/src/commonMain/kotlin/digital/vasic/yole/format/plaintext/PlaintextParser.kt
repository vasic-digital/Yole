/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Plaintext Format Parser - Platform Agnostic
 * Handles plain text, HTML, and source code files
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.*

/**
 * Plaintext file type
 */
enum class PlaintextType {
    PLAIN,      // Plain text (.txt)
    HTML,       // HTML files (.html, .htm)
    CODE,       // Source code with syntax highlighting
    JSON,       // JSON files (with pretty-printing)
    XML,        // XML files
    MARKDOWN    // Markdown in plaintext mode
}

/**
 * Plaintext format parser
 * Handles plain text, HTML, source code, and other text-based formats
 */
class PlaintextParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_PLAINTEXT) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Determine plaintext type
        val type = detectType(extension, content)

        // Process content based on type
        val processedContent = when (type) {
            PlaintextType.JSON -> prettyPrintJson(content)
            PlaintextType.XML -> content // Could add XML formatting
            else -> content
        }

        val metadata = buildMap {
            put("type", type.name.lowercase())
            put("extension", extension)
            put("lines", processedContent.lines().size.toString())
            put("characters", processedContent.length.toString())
        }

        // Convert to HTML
        val html = toHtml(type, extension, processedContent)

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
     * Convert plaintext to HTML based on type
     */
    private fun toHtml(type: PlaintextType, extension: String, content: String): String {
        return when (type) {
            PlaintextType.HTML -> {
                // HTML: display as-is
                content
            }

            PlaintextType.CODE -> {
                // Source code: wrap in code block with language
                val language = mapExtensionToLanguage(extension)
                buildString {
                    append("<div class='plaintext code-block'>")
                    append("<pre><code class='language-$language'>")
                    append(content.escapeHtml())
                    append("</code></pre>")
                    append("</div>")
                }
            }

            else -> {
                // Plain text: wrap in pre block
                buildString {
                    append("<div class='plaintext'>")
                    append("<pre style='white-space: pre-wrap; font-family: monospace;'>")
                    append(content.escapeHtml())
                    append("</pre>")
                    append("</div>")
                }
            }
        }
    }

    /**
     * Detect plaintext type from extension and content
     */
    private fun detectType(extension: String, content: String): PlaintextType {
        return when {
            extension in HTML_EXTENSIONS -> PlaintextType.HTML
            extension in CODE_EXTENSIONS -> PlaintextType.CODE
            extension == ".json" -> PlaintextType.JSON
            extension == ".xml" || extension == ".xlf" -> PlaintextType.XML
            extension in listOf(".md", ".markdown") -> PlaintextType.MARKDOWN
            else -> PlaintextType.PLAIN
        }
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

    /**
     * Map file extension to syntax highlighting language
     */
    private fun mapExtensionToLanguage(extension: String): String {
        return when (extension) {
            ".py" -> "python"
            ".js", ".mjs" -> "javascript"
            ".ts" -> "typescript"
            ".java" -> "java"
            ".kt" -> "kotlin"
            ".cpp", ".cc", ".cxx" -> "cpp"
            ".c" -> "c"
            ".h", ".hpp" -> "cpp"
            ".cs" -> "csharp"
            ".rb" -> "ruby"
            ".php" -> "php"
            ".swift" -> "swift"
            ".rs" -> "rust"
            ".go" -> "go"
            ".sh", ".bash" -> "bash"
            ".xml", ".xlf" -> "xml"
            ".json" -> "json"
            ".css" -> "css"
            ".html", ".htm" -> "html"
            ".sql" -> "sql"
            ".yaml", ".yml" -> "yaml"
            ".r" -> "r"
            ".lua" -> "lua"
            ".perl", ".pl" -> "perl"
            ".diff", ".patch" -> "diff"
            else -> "plaintext"
        }
    }

    /**
     * Pretty-print JSON content
     */
    private fun prettyPrintJson(content: String): String {
        return try {
            // Simple JSON pretty-printer
            var result = content.trim()
            var indent = 0
            val output = StringBuilder()
            var inString = false
            var escaped = false

            for (i in result.indices) {
                val char = result[i]

                when {
                    escaped -> {
                        output.append(char)
                        escaped = false
                    }

                    char == '\\' && inString -> {
                        output.append(char)
                        escaped = true
                    }

                    char == '"' -> {
                        output.append(char)
                        inString = !inString
                    }

                    !inString && (char == '{' || char == '[') -> {
                        output.append(char)
                        indent++
                        output.append("\n")
                        output.append("  ".repeat(indent))
                    }

                    !inString && (char == '}' || char == ']') -> {
                        indent--
                        output.append("\n")
                        output.append("  ".repeat(indent))
                        output.append(char)
                    }

                    !inString && char == ',' -> {
                        output.append(char)
                        output.append("\n")
                        output.append("  ".repeat(indent))
                    }

                    !inString && char == ':' -> {
                        output.append(char)
                        output.append(" ")
                    }

                    !inString && char.isWhitespace() -> {
                        // Skip whitespace outside strings
                    }

                    else -> {
                        output.append(char)
                    }
                }
            }

            output.toString()
        } catch (e: Exception) {
            // If pretty-print fails, return original
            content
        }
    }

    companion object {
        // HTML file extensions
        private val HTML_EXTENSIONS = setOf(".html", ".htm")

        // Source code file extensions
        private val CODE_EXTENSIONS = setOf(
            ".py", ".cpp", ".h", ".c", ".js", ".mjs", ".css", ".cs",
            ".kt", ".lua", ".perl", ".pl", ".java", ".qml", ".diff", ".php",
            ".r", ".patch", ".rs", ".swift", ".ts", ".mm", ".go",
            ".sh", ".bash", ".rb", ".xml", ".xlf", ".json", ".yaml", ".yml",
            ".sql", ".groovy", ".scala", ".dart"
        )

        // Plain text extensions
        private val TEXT_EXTENSIONS = setOf(
            ".txt", ".text", ".log", ".taskpaper", ".org", ".ldg", ".ledger",
            ".m3u", ".m3u8", ".svg", ".lrc", ".fen"
        )
    }
}

/**
 * Register the Plaintext parser with the registry
 */
fun registerPlaintextParser() {
    ParserRegistry.register(PlaintextParser())
}
