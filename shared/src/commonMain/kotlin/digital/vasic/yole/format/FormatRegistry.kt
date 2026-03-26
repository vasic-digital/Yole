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

import digital.vasic.yole.monitoring.PerformanceMetrics
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock

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
     * Lazy holder for the formats list. Exposed so tests can check
     * [isFormatsInitialized] without triggering initialization.
     */
    private val lazyFormats = lazy { createFormats() }

    /**
     * All supported text formats in order of detection priority.
     *
     * Order matters: more specific formats should come before more general ones
     * to ensure proper detection. For example, Markdown should be checked before
     * plain text since plain text would match almost any content.
     *
     * Lazily initialized: the format list is only constructed on first access,
     * reducing startup overhead when FormatRegistry is referenced but formats
     * are not immediately needed.
     */
    val formats: List<TextFormat> by lazyFormats

    /**
     * Whether the formats list has been initialized (for testing/monitoring).
     * Returns true if the lazy [formats] property has been accessed at least once.
     * Checking this property does NOT trigger initialization.
     */
    val isFormatsInitialized: Boolean get() = lazyFormats.isInitialized()

    /**
     * Creates the format list. Extracted to a private method so it can
     * be referenced from the [lazyFormats] delegate.
     */
    private fun createFormats(): List<TextFormat> = listOf(
        // Data science formats (before Markdown since R Markdown is more specific)
        TextFormat(
            id = ID_RMARKDOWN,
            name = "R Markdown",
            defaultExtension = ".rmd",
            extensions = listOf(".rmd", ".rmarkdown"),
            detectionPatterns = listOf("```\\{r")
        ),

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
            extensions = listOf(".txt", ".todotxt", ".todo.txt", ".todo"),
            detectionPatterns = listOf("^\\(([A-Z])\\) ", "^x \\d{4}-\\d{2}-\\d{2}")
        ),
        TextFormat(
            id = ID_CSV,
            name = "CSV",
            defaultExtension = ".csv",
            extensions = listOf(".csv", ".tsv"),
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
            extensions = listOf(".creole", ".txt"),
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
            extensions = listOf(".textile", ".txt"),
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

        // Data interchange formats (extension-only detection to avoid conflicts)
        TextFormat(
            id = ID_JSON,
            name = "JSON",
            defaultExtension = ".json",
            extensions = listOf(".json", ".geojson", ".jsonl")
        ),

        // Binary format
        TextFormat(
            id = ID_BINARY,
            name = "Binary",
            defaultExtension = ".bin",
            extensions = emptyList()
        ),

        // Network storage formats
        TextFormat(
            id = ID_DROPBOX,
            name = "Dropbox",
            defaultExtension = "",
            extensions = emptyList()
        ),
        TextFormat(
            id = ID_FTP,
            name = "FTP",
            defaultExtension = "",
            extensions = emptyList()
        ),
        TextFormat(
            id = ID_GOOGLEDRIVE,
            name = "Google Drive",
            defaultExtension = "",
            extensions = emptyList()
        ),
        TextFormat(
            id = ID_ONEDRIVE,
            name = "OneDrive",
            defaultExtension = "",
            extensions = emptyList()
        ),
        TextFormat(
            id = ID_SFTP,
            name = "SFTP",
            defaultExtension = "",
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
        val startMs = Clock.System.now().toEpochMilliseconds()

        val lines = content.lines().take(maxLines)
        val sampleText = lines.joinToString("\n")

        val result = formats.firstOrNull { format ->
            format.detectionPatterns.any { pattern ->
                Regex(pattern, RegexOption.MULTILINE).containsMatchIn(sampleText)
            }
        }
        val elapsed = Clock.System.now().toEpochMilliseconds() - startMs
        PerformanceMetrics.recordDetection(elapsed)
        return result
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
         val startMs = Clock.System.now().toEpochMilliseconds()
         val cleanExtension = extension.trim().lowercase().let { if (it.startsWith(".")) it else ".$it" }
         val result = formats.firstOrNull { format ->
             format.extensions.any { it.equals(cleanExtension, ignoreCase = true) }
         } ?: formats.first { it.id == ID_PLAINTEXT }
         val elapsed = Clock.System.now().toEpochMilliseconds() - startMs
         PerformanceMetrics.recordDetection(elapsed)
         return result
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
         // Try compound extensions first (e.g., ".todo.txt") for more specific matching,
         // then fall back to simple extension (e.g., ".txt")
         val dotIndex = filename.indexOf('.')
         if (dotIndex >= 0) {
             val compoundExt = filename.substring(dotIndex).lowercase()
             // Try progressively shorter compound extensions
             var idx = dotIndex
             while (idx < filename.length) {
                 val ext = filename.substring(idx).lowercase()
                 val format = formats.firstOrNull { fmt ->
                     fmt.extensions.any { it.equals(ext, ignoreCase = true) }
                 }
                 if (format != null) return format
                 // Move to next dot
                 val nextDot = filename.indexOf('.', idx + 1)
                 if (nextDot < 0) break
                 idx = nextDot
             }
         }
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

    /**
     * LRU cache for parsed documents, keyed by format ID and content hash.
     * Avoids re-parsing identical content for the same format.
     */
    val documentCache = DocumentCache()

    /**
     * Semaphore controlling concurrent parse operations.
     * Default permits = 4, which balances throughput and resource consumption.
     * Can be adjusted via [configureParseConcurrency] for platform-specific tuning.
     */
    @Volatile
    private var parseSemaphore = Semaphore(permits = DEFAULT_PARSE_CONCURRENCY)

    /**
     * Parse content with caching **and** concurrency control.
     *
     * Behaves identically to [parseWithCache] but acquires a permit from
     * [parseSemaphore] before executing. At most 4 parse operations can run
     * concurrently; additional callers suspend until a permit is released.
     *
     * Uses [withPermit] to guarantee permit release even on cancellation.
     *
     * Use this method when many coroutines may parse simultaneously (e.g.,
     * batch import, background re-parse on format change) to avoid CPU
     * saturation and memory pressure.
     *
     * @param content The raw text content to parse
     * @param format  The [TextFormat] to use for parsing
     * @param options Optional parsing options passed to the parser
     * @return The parsed document (possibly from cache)
     * @throws IllegalArgumentException if no parser exists for [format]
     *
     * @see parseWithCache
     */
    suspend fun parseWithCacheConcurrent(
        content: String,
        format: TextFormat,
        options: Map<String, Any> = emptyMap()
    ): ParsedDocument {
        val semaphoreStartMs = Clock.System.now().toEpochMilliseconds()
        return parseSemaphore.withPermit {
            val semaphoreElapsed = Clock.System.now().toEpochMilliseconds() - semaphoreStartMs
            PerformanceMetrics.recordSemaphoreWait(semaphoreElapsed)
            parseWithCache(content, format, options)
        }
    }

    /**
     * Parse content using the appropriate parser, with caching.
     *
     * If a [ParsedDocument] with the same format and content hash is already
     * cached, it is returned immediately. Otherwise, the content is parsed,
     * cached, and returned.
     *
     * @param content The raw text content to parse
     * @param format The [TextFormat] to use for parsing
     * @param options Optional parsing options passed to the parser
     * @return The parsed document (possibly from cache)
     * @throws IllegalArgumentException if no parser exists for [format]
     */
    suspend fun parseWithCache(content: String, format: TextFormat, options: Map<String, Any> = emptyMap()): ParsedDocument {
        val cacheKey = "${format.id}:${content.hashCode()}"
        documentCache.get(cacheKey)?.let { return it }

        val parseStartMs = Clock.System.now().toEpochMilliseconds()
        val parser = ParserRegistry.getParser(format)
            ?: throw IllegalArgumentException("No parser for format: ${format.id}")
        val result = parser.parse(content, options)
        val parseElapsed = Clock.System.now().toEpochMilliseconds() - parseStartMs
        PerformanceMetrics.recordParse(parseElapsed)
        documentCache.put(cacheKey, result)
        return result
    }

    /** Default concurrent parse operations limit */
    const val DEFAULT_PARSE_CONCURRENCY = 4

    /**
     * Configure the maximum number of concurrent parse operations.
     *
     * **Thread safety:** The [parseSemaphore] field is @Volatile, ensuring
     * visibility of the new Semaphore reference. However, in-flight
     * [parseWithCacheConcurrent] calls will complete with the old semaphore.
     * Call this method during initialization, before concurrent parsing begins.
     *
     * @param permits number of concurrent parse operations allowed (minimum 1, maximum 16)
     */
    fun configureParseConcurrency(permits: Int) {
        require(permits in 1..16) { "Parse concurrency must be between 1 and 16, got $permits" }
        parseSemaphore = Semaphore(permits = permits)
    }

    // Format ID constants
    const val ID_UNKNOWN = "unknown"
    const val ID_PLAINTEXT = "plaintext"
    const val ID_MARKDOWN = "markdown"
    const val ID_TODOTXT = "todotxt"
    const val ID_CSV = "csv"
    const val ID_JSON = "json"
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
    
    // Network storage format IDs
    const val ID_DROPBOX = "dropbox"
    const val ID_FTP = "ftp"
    const val ID_GOOGLEDRIVE = "googledrive"
    const val ID_ONEDRIVE = "onedrive"
    const val ID_SFTP = "sftp"
}