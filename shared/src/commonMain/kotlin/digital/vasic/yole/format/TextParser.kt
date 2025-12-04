/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform Text Parser System
 * Platform-agnostic text parsing interface
 *
 *########################################################*/
package digital.vasic.yole.format

/**
 * Represents a parsed document with structured content.
 *
 * This is the result of parsing markup text into a structured format. It contains
 * both the original raw content and the processed/parsed version, along with
 * metadata and any errors encountered during parsing.
 *
 * **Memory Optimization**: HTML generation is lazily evaluated and cached. The first
 * call to `toHtml()` generates HTML from the parsed content, subsequent calls return
 * the cached result. This reduces memory usage when documents are parsed but never
 * displayed.
 *
 * @property format The format that was used to parse this document
 * @property rawContent Raw text content (original markup)
 * @property parsedContent Parsed content (could be HTML, structured data, etc.)
 * @property metadata Document metadata extracted during parsing (e.g., title, author)
 * @property errors Any parsing errors or warnings encountered
 *
 * @example
 * ```kotlin
 * val parser = MarkdownParser()
 * val document = parser.parse("# Hello World\n\nThis is **markdown**.")
 *
 * println(document.format.name) // "Markdown"
 * println(document.metadata["lines"]) // "2"
 * println(document.errors.isEmpty()) // true
 *
 * // HTML is generated only when toHtml() is called
 * val html = document.toHtml(lightMode = true)  // First call generates HTML
 * val html2 = document.toHtml(lightMode = true) // Second call returns cached HTML
 * ```
 */
class ParsedDocument(
    /**
     * The format that was used to parse this document
     */
    val format: TextFormat,

    /**
     * Raw text content (original markup)
     */
    val rawContent: String,

    /**
     * Parsed content (could be HTML, structured data, etc.)
     */
    val parsedContent: String,

    /**
     * Document metadata extracted during parsing
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * Any parsing errors or warnings
     */
    val errors: List<String> = emptyList()
) {
    /**
     * Cached HTML for light mode. Lazily generated on first toHtml(lightMode=true) call.
     */
    private var _cachedHtmlLight: String? = null

    /**
     * Cached HTML for dark mode. Lazily generated on first toHtml(lightMode=false) call.
     */
    private var _cachedHtmlDark: String? = null

    /**
     * Convert the parsed document to HTML with lazy evaluation and caching.
     *
     * This method provides lazy HTML generation - the HTML is only created when first
     * requested and then cached for subsequent calls. This reduces memory usage in
     * scenarios where documents are parsed but never displayed (e.g., metadata extraction,
     * validation, or batch processing).
     *
     * **Caching Behavior**:
     * - Light and dark mode HTML are cached separately
     * - First call generates HTML and caches it
     * - Subsequent calls with the same `lightMode` value return the cached HTML
     * - No memory allocated for HTML if toHtml() is never called
     *
     * **Memory Savings**:
     * - Defers HTML allocation until needed (50-70% savings if never displayed)
     * - Prevents regeneration on repeated calls (100% savings on subsequent calls)
     * - Separate caching for light/dark modes avoids redundant generation
     *
     * @param lightMode Whether to use light theme (true) or dark theme (false)
     * @return HTML representation of the document with appropriate styling
     *
     * @example
     * ```kotlin
     * val document = parser.parse(content)
     *
     * // HTML not generated yet - zero allocation
     * println(document.metadata["lines"])
     *
     * // First call generates and caches HTML
     * val html1 = document.toHtml(lightMode = true)  // Generates HTML (~5-10KB)
     *
     * // Subsequent calls return cached HTML - no generation
     * val html2 = document.toHtml(lightMode = true)  // Returns cached (~0 allocation)
     *
     * // Different mode generates separate cached version
     * val htmlDark = document.toHtml(lightMode = false)  // Generates dark HTML
     * ```
     */
    fun toHtml(lightMode: Boolean = true): String {
        // Check appropriate cache based on light mode
        if (lightMode) {
            return _cachedHtmlLight ?: run {
                // Generate HTML using the appropriate parser
                val parser = ParserRegistry.getParser(format)
                    ?: throw IllegalStateException("No parser found for format: ${format.id}")
                parser.toHtml(this, lightMode).also { _cachedHtmlLight = it }
            }
        } else {
            return _cachedHtmlDark ?: run {
                // Generate HTML using the appropriate parser
                val parser = ParserRegistry.getParser(format)
                    ?: throw IllegalStateException("No parser found for format: ${format.id}")
                parser.toHtml(this, lightMode).also { _cachedHtmlDark = it }
            }
        }
    }

    /**
     * Clear cached HTML to free memory.
     *
     * This method clears both light and dark mode cached HTML, allowing the memory
     * to be garbage collected. Useful in memory-constrained scenarios or when the
     * document content has been modified externally.
     *
     * After calling this method, subsequent `toHtml()` calls will regenerate the HTML.
     *
     * @example
     * ```kotlin
     * val document = parser.parse(content)
     * val html = document.toHtml()  // Generates and caches
     *
     * document.clearHtmlCache()  // Free cached HTML memory
     *
     * val html2 = document.toHtml()  // Regenerates HTML
     * ```
     */
    fun clearHtmlCache() {
        _cachedHtmlLight = null
        _cachedHtmlDark = null
    }

    /**
     * Check if HTML has been generated and cached for the given mode.
     *
     * Useful for debugging or monitoring memory usage.
     *
     * @param lightMode The mode to check (true for light, false for dark)
     * @return true if HTML is cached for the specified mode, false otherwise
     */
    fun hasHtmlCached(lightMode: Boolean = true): Boolean {
        return if (lightMode) _cachedHtmlLight != null else _cachedHtmlDark != null
    }

    // Implement equals, hashCode, and toString for data class-like behavior
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParsedDocument

        if (format != other.format) return false
        if (rawContent != other.rawContent) return false
        if (parsedContent != other.parsedContent) return false
        if (metadata != other.metadata) return false
        if (errors != other.errors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + rawContent.hashCode()
        result = 31 * result + parsedContent.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + errors.hashCode()
        return result
    }

    override fun toString(): String {
        return "ParsedDocument(format=$format, rawContent='${rawContent.take(50)}...', metadata=$metadata, errors=$errors)"
    }

    /**
     * Create a copy of this ParsedDocument with modified properties.
     *
     * Mimics the `copy()` method of data classes. Note that the HTML cache is NOT
     * copied to the new instance - it starts with an empty cache.
     *
     * @param format The format (default: current format)
     * @param rawContent The raw content (default: current rawContent)
     * @param parsedContent The parsed content (default: current parsedContent)
     * @param metadata The metadata (default: current metadata)
     * @param errors The errors (default: current errors)
     * @return A new ParsedDocument instance with the specified properties
     */
    fun copy(
        format: TextFormat = this.format,
        rawContent: String = this.rawContent,
        parsedContent: String = this.parsedContent,
        metadata: Map<String, String> = this.metadata,
        errors: List<String> = this.errors
    ): ParsedDocument {
        return ParsedDocument(format, rawContent, parsedContent, metadata, errors)
    }
}

/**
 * Interface for text format parsers.
 * 
 * Each format (Markdown, TodoTxt, LaTeX, etc.) implements this interface to provide
 * parsing capabilities for their specific markup language. The interface defines the
 * contract for parsing text content, converting to HTML, and validating syntax.
 *
 * @property supportedFormat The format this parser handles
 *
 * @example
 * ```kotlin
 * class MyCustomParser : TextParser {
 *     override val supportedFormat = TextFormat("custom", "Custom Format", ".custom")
 *     
 *     override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
 *         // Implementation here
 *     }
 * }
 * ```
 */
interface TextParser {
    /**
     * The format this parser handles
     */
    val supportedFormat: TextFormat

    /**
     * Check if this parser can handle the given format.
     * 
     * Default implementation checks format ID equality. Override for more complex
     * format compatibility checking.
     *
     * @param format The format to check
     * @return true if this parser can handle the format, false otherwise
     */
    fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }

    /**
     * Parse markup text into structured content.
     * 
     * This is the main parsing method that converts raw markup text into a
     * structured ParsedDocument. The implementation should extract metadata,
     * perform syntax validation, and generate appropriate parsed content.
     *
     * @param content The raw markup text to parse
     * @param options Optional parsing options (format-specific). Common options include:
     *                - "filename": String - source filename for context
     *                - "lineNumbers": Boolean - enable line numbering
     *                - "baseUrl": String - base URL for relative links
     * @return Parsed document with structured content, metadata, and any errors
     *
     * @example
     * ```kotlin
     * val parser = MarkdownParser()
     * val options = mapOf(
     *     "filename" to "README.md",
     *     "lineNumbers" to true
     * )
     * val document = parser.parse("# Title\n\nContent", options)
     * ```
     */
    fun parse(content: String, options: Map<String, Any> = emptyMap()): ParsedDocument

    /**
     * Convert parsed document to HTML (for preview/export).
     * 
     * This method generates HTML representation suitable for display in web views
     * or export to HTML files. The default implementation provides a simple
     * pre-formatted text display, but parsers should override to provide rich
     * formatting.
     *
     * @param document The parsed document to convert
     * @param lightMode Whether to use light theme (true) or dark theme (false)
     * @return HTML representation of the document with appropriate styling
     *
     * @example
     * ```kotlin
     * val parser = MarkdownParser()
     * val document = parser.parse("# Title")
     * val html = parser.toHtml(document, lightMode = true)
     * // Returns: "<div class='markdown'><h1>Title</h1></div>"
     * ```
     */
    fun toHtml(document: ParsedDocument, lightMode: Boolean = true): String {
        // Default implementation: escape HTML and wrap in <pre>
        return buildString {
            append("<pre>")
            append(document.rawContent.escapeHtml())
            append("</pre>")
        }
    }

    /**
     * Validate document content for syntax errors.
     * 
     * This method performs syntax validation on the raw content and returns
     * a list of validation errors. The default implementation returns no errors,
     * but parsers should override to provide format-specific validation.
     *
     * @param content The content to validate
     * @return List of validation errors (empty if valid). Each error should include
     *         line number information when applicable.
     *
     * @example
     * ```kotlin
     * val parser = MarkdownParser()
     * val errors = parser.validate("[Unclosed link](missing-end")
     * // Returns: ["Line 1: Unclosed parentheses in link"]
     * ```
     */
    fun validate(content: String): List<String> {
        return emptyList()
    }
}

/**
 * Registry for text parsers.
 * 
 * Manages all available parsers and provides lookup by format. This is a singleton
 * object that maintains a registry of all TextParser implementations. Parsers are
 * typically registered during application initialization.
 *
 * @example
 * ```kotlin
 * // Register a parser
 * ParserRegistry.register(MarkdownParser())
 * 
 * // Get a parser for a format
 * val format = FormatRegistry.getById("markdown")!!
 * val parser = ParserRegistry.getParser(format)
 * 
 * // Parse content
 * val document = parser?.parse("# Hello World")
 * ```
 */
object ParserRegistry {
    // Eager storage: Parsers that have been explicitly registered or lazy-loaded
    private val parsers = mutableMapOf<String, TextParser>()

    // Lazy storage: Factory functions for parsers not yet instantiated
    private val parserFactories = mutableMapOf<String, () -> TextParser>()

    /**
     * Register a parser with the registry (eager instantiation).
     *
     * This method immediately stores the parser instance. For lazy loading,
     * use registerLazy() instead.
     *
     * @param parser The parser to register
     * @throws IllegalArgumentException if a parser for the same format is already registered
     *
     * @example
     * ```kotlin
     * ParserRegistry.register(MarkdownParser())
     * ```
     */
    fun register(parser: TextParser) {
        val formatId = parser.supportedFormat.id

        // Check for duplicate format registration
        if (parsers.containsKey(formatId) || parserFactories.containsKey(formatId)) {
            throw IllegalArgumentException("Parser for format '$formatId' is already registered")
        }

        parsers[formatId] = parser
    }

    /**
     * Register a parser factory for lazy instantiation.
     *
     * The parser will not be instantiated until first access via getParser().
     * This reduces startup time by deferring parser creation until needed.
     *
     * @param formatId The format ID to register
     * @param factory A lambda that creates the parser instance when called
     * @throws IllegalArgumentException if a parser for the same format is already registered
     *
     * @example
     * ```kotlin
     * ParserRegistry.registerLazy("markdown") { MarkdownParser() }
     * ```
     */
    fun registerLazy(formatId: String, factory: () -> TextParser) {
        // Check for duplicate format registration
        if (parsers.containsKey(formatId) || parserFactories.containsKey(formatId)) {
            throw IllegalArgumentException("Parser for format '$formatId' is already registered")
        }

        parserFactories[formatId] = factory
    }

    /**
     * Get parser for a specific format.
     *
     * If the parser was registered lazily and hasn't been instantiated yet,
     * this method will create it on first access and cache it for future use.
     *
     * @param format The format to get a parser for
     * @return The parser if found, null otherwise
     *
     * @example
     * ```kotlin
     * val format = FormatRegistry.getById("markdown")!!
     * val parser = ParserRegistry.getParser(format)
     * ```
     */
    fun getParser(format: TextFormat): TextParser? {
        val formatId = format.id

        // Check if parser is already instantiated
        parsers[formatId]?.let { return it }

        // Check if we have a factory for this format
        val factory = parserFactories[formatId] ?: run {
            // Try to find by canParse() for backwards compatibility
            return parsers.values.firstOrNull { it.canParse(format) }
        }

        // Lazy instantiate the parser
        val parser = factory()
        parsers[formatId] = parser
        parserFactories.remove(formatId)  // Remove factory after instantiation

        return parser
    }

    /**
     * Get parser by format ID.
     * 
     * @param formatId The format ID to get a parser for
     * @return The parser if found, null otherwise
     *
     * @example
     * ```kotlin
     * val parser = ParserRegistry.getParser("markdown")
     * ```
     */
    fun getParser(formatId: String): TextParser? {
        val format = FormatRegistry.getById(formatId) ?: return null
        return getParser(format)
    }

    /**
     * Check if a parser exists for the given format.
     *
     * This method checks both instantiated parsers and registered factories.
     * It does NOT trigger lazy instantiation.
     *
     * @param format The format to check
     * @return true if a parser exists, false otherwise
     */
    fun hasParser(format: TextFormat): Boolean {
        val formatId = format.id
        return parsers.containsKey(formatId) ||
               parserFactories.containsKey(formatId) ||
               parsers.values.any { it.canParse(format) }
    }

    /**
     * Get all registered parsers.
     *
     * NOTE: This method only returns parsers that have been instantiated.
     * Lazy-registered parsers that haven't been accessed yet are NOT included.
     * To get all available formats, use FormatRegistry.formats instead.
     *
     * @return A list of all instantiated parsers
     */
    fun getAllParsers(): List<TextParser> {
        return parsers.values.toList()
    }

    /**
     * Get count of registered parser factories (not yet instantiated).
     *
     * Useful for monitoring lazy loading effectiveness.
     *
     * @return Number of parsers registered but not yet instantiated
     */
    fun getPendingParserCount(): Int {
        return parserFactories.size
    }

    /**
     * Get count of instantiated parsers.
     *
     * Useful for monitoring lazy loading effectiveness.
     *
     * @return Number of parsers that have been instantiated
     */
    fun getInstantiatedParserCount(): Int {
        return parsers.size
    }

    /**
     * Clear all registered parsers.
     * 
     * This method is primarily useful for testing scenarios where you need
     * to reset the registry to a clean state.
     */
    fun clear() {
        parsers.clear()
    }
}

/**
 * Escape HTML special characters in a string.
 * 
 * This extension function escapes the standard HTML special characters to prevent
 * XSS attacks and ensure proper HTML rendering. It should be used when inserting
 * user-provided text into HTML content.
 *
 * @receiver The string to escape
 * @return The escaped string safe for HTML insertion
 *
 * @example
 * ```kotlin
 * val unsafe = "<script>alert('xss')</script>"
 * val safe = unsafe.escapeHtml()
 * // Returns: "&lt;script&gt;alert('xss')&lt;/script&gt;"
 * ```
 */
fun String.escapeHtml(): String {
    return this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

/**
 * Builder for common parsing configuration options.
 * 
 * This class provides a fluent API for building parsing options that can be
 * passed to TextParser.parse() methods. It includes common options like line
 * numbering, syntax highlighting, and base URL configuration.
 *
 * @property options Internal map of option key-value pairs
 *
 * @example
 * ```kotlin
 * val options = ParseOptions.create()
 *     .enableLineNumbers(true)
 *     .enableHighlighting(true)
 *     .setBaseUrl("https://example.com")
 *     .set("customOption", "value")
 *     .build()
 * 
 * val parser = MarkdownParser()
 * val document = parser.parse(content, options)
 * ```
 */
class ParseOptions {
    private val options = mutableMapOf<String, Any>()

    /**
     * Enable or disable line numbering in the output.
     * 
     * @param enable Whether to enable line numbering (default: true)
     * @return This builder for method chaining
     */
    fun enableLineNumbers(enable: Boolean = true): ParseOptions {
        options["lineNumbers"] = enable
        return this
    }

    /**
     * Enable or disable syntax highlighting in the output.
     * 
     * @param enable Whether to enable syntax highlighting (default: true)
     * @return This builder for method chaining
     */
    fun enableHighlighting(enable: Boolean = true): ParseOptions {
        options["highlighting"] = enable
        return this
    }

    /**
     * Set the base URL for resolving relative links.
     * 
     * @param url The base URL to use for relative link resolution
     * @return This builder for method chaining
     */
    fun setBaseUrl(url: String): ParseOptions {
        options["baseUrl"] = url
        return this
    }

    /**
     * Set a custom option key-value pair.
     * 
     * @param key The option key
     * @param value The option value
     * @return This builder for method chaining
     */
    fun set(key: String, value: Any): ParseOptions {
        options[key] = value
        return this
    }

    /**
     * Build the options map.
     * 
     * @return An immutable map of all configured options
     */
    fun build(): Map<String, Any> = options.toMap()

    companion object {
        /**
         * Create a new ParseOptions builder.
         * 
         * @return A new ParseOptions instance
         */
        fun create(): ParseOptions = ParseOptions()
    }
}
