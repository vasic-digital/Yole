/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform Format Registry
 * Central registry for all supported text formats
 *
 *########################################################*/
package digital.vasic.yole.format

/**
 * Registry for all supported text formats in Yole.
 * 
 * Provides format detection, lookup, and management functionality. This registry
 * contains metadata for all supported text formats and provides methods to detect
 * formats by file extension, content analysis, or MIME type.
 *
 * @example
 * ```kotlin
 * // Get format by ID
 * val markdownFormat = FormatRegistry.getById("markdown")
 * 
 * // Detect format by file extension
 * val format = FormatRegistry.detectByExtension("README.md")
 * 
 * // Detect format by content
 * val content = "# Title\n\nContent"
 * val detectedFormat = FormatRegistry.detectByContent(content)
 * ```
 */
object FormatRegistry {
    /**
     * All supported text formats in order of detection priority.
     * 
     * Order matters: more specific formats should come before more general ones
     * to ensure proper detection. For example, Markdown should be checked before
     * plain text since plain text would match almost any content.
     */
    val formats: List<TextFormat> = listOf(
        // Core formats
        TextFormat(
            id = ID_MARKDOWN,
            name = "Markdown",
            defaultExtension = ".md",
            extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
            detectionPatterns = listOf("^#+ ", "^\\[.*\\]\\(.*\\)", "^\\*\\*.*\\*\\*")
        ),
        TextFormat(
            id = ID_PLAINTEXT,
            name = "Plain Text",
            defaultExtension = ".txt",
            extensions = listOf(".txt", ".text", ".log")
        ),
        TextFormat(
            id = ID_TODOTXT,
            name = "Todo.txt",
            defaultExtension = ".txt",
            extensions = listOf(".txt"),
            detectionPatterns = listOf("^\\(([A-Z])\\) ", "^x \\d{4}-\\d{2}-\\d{2}")
        ),
        TextFormat(
            id = ID_CSV,
            name = "CSV",
            defaultExtension = ".csv",
            extensions = listOf(".csv"),
            detectionPatterns = listOf("^.*,.*,.*$")
        ),

        // Wiki formats
        TextFormat(
            id = ID_WIKITEXT,
            name = "WikiText",
            defaultExtension = ".wiki",
            extensions = listOf(".wiki", ".wikitext"),
            detectionPatterns = listOf("^==+ .* ==+$", "^\\[\\[.*\\]\\]")
        ),
        TextFormat(
            id = ID_ORGMODE,
            name = "Org Mode",
            defaultExtension = ".org",
            extensions = listOf(".org"),
            detectionPatterns = listOf("^\\* ", "^#\\+")
        ),
        TextFormat(
            id = ID_CREOLE,
            name = "Creole",
            defaultExtension = ".creole",
            extensions = listOf(".creole"),
            detectionPatterns = listOf("^=+ ", "^\\*\\* ")
        ),
        TextFormat(
            id = ID_TIDDLYWIKI,
            name = "TiddlyWiki",
            defaultExtension = ".tid",
            extensions = listOf(".tid", ".tiddly"),
            detectionPatterns = listOf("^!+ ", "^title: ")
        ),

        // Technical formats
        TextFormat(
            id = ID_LATEX,
            name = "LaTeX",
            defaultExtension = ".tex",
            extensions = listOf(".tex", ".latex"),
            detectionPatterns = listOf("\\\\documentclass", "\\\\begin\\{document\\}")
        ),
        TextFormat(
            id = ID_ASCIIDOC,
            name = "AsciiDoc",
            defaultExtension = ".adoc",
            extensions = listOf(".adoc", ".asciidoc"),
            detectionPatterns = listOf("^= ", "^== ")
        ),
        TextFormat(
            id = ID_RESTRUCTUREDTEXT,
            name = "reStructuredText",
            defaultExtension = ".rst",
            extensions = listOf(".rst", ".rest"),
            detectionPatterns = listOf("^=+$", "^-+$", "^\\.\\. ")
        ),

        // Specialized formats
        TextFormat(
            id = ID_KEYVALUE,
            name = "Key-Value",
            defaultExtension = ".ini",
            extensions = listOf(".keyvalue", ".properties", ".ini"),
            detectionPatterns = listOf("^[a-zA-Z_]+\\s*=", "^\\[.*\\]$")
        ),
        TextFormat(
            id = ID_TASKPAPER,
            name = "TaskPaper",
            defaultExtension = ".taskpaper",
            extensions = listOf(".taskpaper"),
            detectionPatterns = listOf("^\\t- ", "^.*:$")
        ),
        TextFormat(
            id = ID_TEXTILE,
            name = "Textile",
            defaultExtension = ".textile",
            extensions = listOf(".textile"),
            detectionPatterns = listOf("^h[1-6]\\. ", "^\\*+ ")
        ),

        // Data science formats
        TextFormat(
            id = ID_JUPYTER,
            name = "Jupyter Notebook",
            defaultExtension = ".ipynb",
            extensions = listOf(".ipynb"),
            detectionPatterns = listOf("\"nbformat\":", "\"cell_type\":")
        ),
        TextFormat(
            id = ID_RMARKDOWN,
            name = "R Markdown",
            defaultExtension = ".rmd",
            extensions = listOf(".rmd", ".rmarkdown"),
            detectionPatterns = listOf("```\\{r", "^---$")
        ),

        // Binary format
        TextFormat(
            id = ID_BINARY,
            name = "Binary",
            defaultExtension = ".bin",
            extensions = emptyList()
        )
    )

    /**
     * Get format by unique identifier.
     * 
     * @param id The format identifier to look up
     * @return The TextFormat if found, null otherwise
     *
     * @example
     * ```kotlin
     * val markdownFormat = FormatRegistry.getById("markdown")
     * println(markdownFormat?.name) // "Markdown"
     * ```
     */
    fun getById(id: String): TextFormat? {
        return formats.firstOrNull { it.id == id }
    }

    /**
     * Get format by file extension.
     *
     * @param extension The file extension (with or without dot)
     * @return The TextFormat if found, null otherwise
     *
     * @example
     * ```kotlin
     * val format = FormatRegistry.getByExtension("md")
     * val format2 = FormatRegistry.getByExtension(".markdown")
     * ```
     */
     fun getByExtension(extension: String): TextFormat? {
         val cleanExtension = extension.trim().lowercase().let { if (it.startsWith(".")) it else ".$it" }
         return formats.firstOrNull { format ->
             format.extensions.any { it.equals(cleanExtension, ignoreCase = true) }
         }
     }





    /**
     * Detect format by file content analysis.
     *
     * This method analyzes the actual content to determine the most likely format.
     * It checks for format-specific signatures and patterns in the text.
     *
     * @param content The content to analyze
     * @param maxLines Maximum number of lines to analyze (default: 10)
     * @return The detected TextFormat, or null if no specific format is detected
     *
     * @example
     * ```kotlin
     * val markdownContent = "# Title\n\nThis is **bold** text."
     * val format = FormatRegistry.detectByContent(markdownContent)
     * // Returns Markdown format
     * ```
     */
    fun detectByContent(content: String, maxLines: Int = 10): TextFormat? {
        if (content.isEmpty()) return null

        val lines = content.lines().take(maxLines)
        val sampleText = lines.joinToString("\n")

        return formats.firstOrNull { format ->
            format.detectionPatterns.any { pattern ->
                Regex(pattern, RegexOption.MULTILINE).containsMatchIn(sampleText)
            }
        }
    }

    /**
     * Get all formats that support a given extension.
     *
     * Unlike getByExtension(), this method returns all formats that claim to
     * support the extension, which can be useful when multiple formats share
     * the same extension (e.g., .txt for both plain text and Todo.txt).
     *
     * @param extension The file extension to check
     * @return List of all TextFormat objects that support the extension
     *
     * @example
     * ```kotlin
     * val txtFormats = FormatRegistry.getFormatsByExtension("txt")
     * // May return both plain text and Todo.txt formats
     * ```
     */
     fun getFormatsByExtension(extension: String): List<TextFormat> {
         val cleanExtension = extension.trim().lowercase().let { if (it.startsWith(".")) it else ".$it" }
         return formats.filter { format ->
             format.extensions.any { it.equals(cleanExtension, ignoreCase = true) }
         }
     }

    /**
     * Check if a format is supported by the registry.
     * 
     * @param formatId The format identifier to check
     * @return true if the format is supported, false otherwise
     *
     * @example
     * ```kotlin
     * val isMarkdownSupported = FormatRegistry.isSupported("markdown")
     * // Returns true
     * ```
     */
    fun isSupported(formatId: String): Boolean {
        return getById(formatId) != null
    }

    /**
     * Get all readable format names.
     * 
     * @return List of human-readable format names
     *
     * @example
     * ```kotlin
     * val names = FormatRegistry.getFormatNames()
     * // Returns ["Plain Text", "Markdown", "Todo.txt", ...]
     * ```
     */
    fun getFormatNames(): List<String> {
        return formats.map { it.name }
    }

    /**
     * Get all supported file extensions.
     *
     * @return List of all unique file extensions supported by any format
     *
     * @example
     * ```kotlin
     * val extensions = FormatRegistry.getAllExtensions()
     * // Returns ["txt", "md", "markdown", "csv", ...]
     * ```
     */
    fun getAllExtensions(): List<String> {
        return formats.flatMap { it.extensions }.distinct()
    }

    /**
     * Detect format by file extension with fallback to plain text.
     *
     * Unlike getByExtension(), this method never returns null and will fall back
     * to plain text format if the extension is not recognized.
     *
     * @param extension The file extension (with or without dot)
     * @return A TextFormat (never null, falls back to plain text)
     *
     * @example
     * ```kotlin
     * val format = FormatRegistry.detectByExtension("unknown")
     * // Returns plain text format
     * ```
     */
     fun detectByExtension(extension: String): TextFormat {
         val cleanExtension = extension.trim().lowercase().let { if (it.startsWith(".")) it else ".$it" }
         return formats.firstOrNull { format ->
             format.extensions.any { it.equals(cleanExtension, ignoreCase = true) }
         } ?: formats.first { it.id == ID_PLAINTEXT }
     }

    /**
     * Detect format by filename.
     *
     * @param filename The filename to analyze
     * @return The detected TextFormat
     *
     * @example
     * ```kotlin
     * val format = FormatRegistry.detectByFilename("document.md")
     * // Returns Markdown format
     * ```
     */
     fun detectByFilename(filename: String): TextFormat {
         val extension = filename.substringAfterLast('.', "")
         return if (extension.isNotEmpty()) {
             detectByExtension(extension)
         } else {
             formats.first { it.id == ID_PLAINTEXT }
         }
     }

    /**
     * Check if a file extension is supported.
     *
     * @param extension The file extension to check (with or without dot)
     * @return true if the extension is supported, false otherwise
     *
     * @example
     * ```kotlin
     * val isSupported = FormatRegistry.isExtensionSupported(".md")
     * // Returns true
     * ```
     */
     fun isExtensionSupported(extension: String): Boolean {
         val cleanExtension = extension.trim().lowercase().let { if (it.startsWith(".")) it else ".$it" }
         return formats.any { format ->
             format.extensions.any { it.equals(cleanExtension, ignoreCase = true) }
         }
     }

    // Format ID constants
    const val ID_UNKNOWN = "unknown"
    const val ID_PLAINTEXT = "plaintext"
    const val ID_MARKDOWN = "markdown"
    const val ID_TODOTXT = "todotxt"
    const val ID_CSV = "csv"
    const val ID_WIKITEXT = "wikitext"
    const val ID_KEYVALUE = "keyvalue"
    const val ID_ASCIIDOC = "asciidoc"
    const val ID_ORGMODE = "orgmode"
    const val ID_LATEX = "latex"
    const val ID_RESTRUCTUREDTEXT = "restructuredtext"
    const val ID_TASKPAPER = "taskpaper"
    const val ID_TEXTILE = "textile"
    const val ID_CREOLE = "creole"
    const val ID_TIDDLYWIKI = "tiddlywiki"
    const val ID_JUPYTER = "jupyter"
    const val ID_RMARKDOWN = "rmarkdown"
    const val ID_BINARY = "binary"
}