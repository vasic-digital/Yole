/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform Document Model
 * Platform-agnostic document representation
 *
 *########################################################*/
package digital.vasic.yole.model

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Platform-agnostic document model for Yole.
 * 
 * Represents a text document with associated metadata. This is the KMP version
 * of the Document class, containing only platform-independent properties.
 * Platform-specific operations are delegated to expect/actual functions.
 *
 * @property path Absolute path to the document file
 * @property title Document title (filename without extension)
 * @property extension File extension (e.g., "md", "txt", "tex")
 * @property format Format identifier (e.g., "markdown", "plaintext", "latex")
 * @property modTime Last modification time in milliseconds since epoch
 * @property touchTime Last touch time (when document was last accessed) in milliseconds since epoch
 *
 * @example
 * ```kotlin
 * val document = Document(
 *     path = "/path/to/README.md",
 *     title = "README",
 *     extension = "md",
 *     format = Document.FORMAT_MARKDOWN
 * )
 * 
 * document.detectFormatByExtension()
 * println(document.getTextFormat().name) // "Markdown"
 * ```
 */
@Serializable
data class Document(
    /**
     * Absolute path to the document file
     */
    val path: String,

    /**
     * Document title (filename without extension)
     */
    val title: String,

    /**
     * File extension (e.g., "md", "txt", "tex")
     */
    val extension: String,

    /**
     * Format identifier (e.g., "markdown", "plaintext", "latex")
     * Maps to FormatRegistry format constants
     */
    var format: String = FORMAT_UNKNOWN,

    /**
     * Last modification time in milliseconds since epoch
     */
    @Transient
    var modTime: Long = -1,

    /**
     * Last touch time (when document was last accessed) in milliseconds since epoch
     */
    @Transient
    var touchTime: Long = -1
) {

    companion object {
        /**
         * Format constants (delegated to TextFormat for consistency).
         * 
         * These constants provide backward compatibility and convenient access
         * to format identifiers throughout the codebase.
         */
        const val FORMAT_UNKNOWN = TextFormat.ID_UNKNOWN
        const val FORMAT_PLAINTEXT = TextFormat.ID_PLAINTEXT
        const val FORMAT_MARKDOWN = TextFormat.ID_MARKDOWN
        const val FORMAT_TODOTXT = TextFormat.ID_TODOTXT
        const val FORMAT_CSV = TextFormat.ID_CSV
        const val FORMAT_WIKITEXT = TextFormat.ID_WIKITEXT
        const val FORMAT_KEYVALUE = TextFormat.ID_KEYVALUE
        const val FORMAT_ASCIIDOC = TextFormat.ID_ASCIIDOC
        const val FORMAT_ORGMODE = TextFormat.ID_ORGMODE
        const val FORMAT_LATEX = TextFormat.ID_LATEX
        const val FORMAT_RESTRUCTUREDTEXT = TextFormat.ID_RESTRUCTUREDTEXT
        const val FORMAT_TASKPAPER = TextFormat.ID_TASKPAPER
        const val FORMAT_TEXTILE = TextFormat.ID_TEXTILE
        const val FORMAT_CREOLE = TextFormat.ID_CREOLE
        const val FORMAT_TIDDLYWIKI = TextFormat.ID_TIDDLYWIKI
        const val FORMAT_JUPYTER = TextFormat.ID_JUPYTER
        const val FORMAT_RMARKDOWN = TextFormat.ID_RMARKDOWN
        const val FORMAT_BINARY = TextFormat.ID_BINARY
    }

    /**
     * Get the filename with extension.
     * 
     * @return The complete filename including extension, or just the title if no extension
     *
     * @example
     * ```kotlin
     * val doc = Document("/path/to/README.md", "README", "md")
     * println(doc.filename) // "README.md"
     * ```
     */
    val filename: String
        get() = if (extension.isNotEmpty()) "$title.$extension" else title

    /**
     * Check if document has been modified since last tracking.
     * 
     * This method compares the stored modification time with the actual file
     * modification time to determine if the document has been changed externally.
     * 
     * @return true if the document has been modified, false otherwise
     *
     * @example
     * ```kotlin
     * if (document.hasChanged()) {
     *     // Reload document content
     * }
     * ```
     */
    fun hasChanged(): Boolean {
        return modTime < 0 || touchTime < 0 || getFileModTime() > modTime
    }

    /**
     * Reset change tracking timestamps.
     * 
     * This method resets the modification and touch time tracking, effectively
     * marking the document as "up to date". Call this after loading or saving
     * a document to reset change detection.
     *
     * @example
     * ```kotlin
     * document.resetChangeTracking()
     * assert(!document.hasChanged())
     * ```
     */
    fun resetChangeTracking() {
        modTime = -1
        touchTime = -1
    }

    /**
     * Update touch time to current time.
     * 
     * This method marks the document as recently accessed by updating the
     * touch time to the current timestamp. This is useful for tracking document
     * usage and implementing "recently used" functionality.
     *
     * @example
     * ```kotlin
     * document.touch()
     * // Document is now marked as recently accessed
     * ```
     */
    fun touch() {
        touchTime = currentTimeMillis()
    }

    /**
     * Get the TextFormat object for this document.
     * 
     * @return The TextFormat object, or the last format in registry if not found
     *
     * @example
     * ```kotlin
     * val format = document.getTextFormat()
     * println(format.name) // "Markdown"
     * println(format.extensions) // [".md", ".markdown", ...]
     * ```
     */
    fun getTextFormat(): TextFormat {
        return FormatRegistry.getById(format) ?: FormatRegistry.formats.last()
    }

    /**
     * Update format based on file extension.
     * 
     * This method automatically detects the document format based on its file
     * extension and updates the format property accordingly.
     *
     * @example
     * ```kotlin
     * val document = Document("/path/to/README.md", "README", "md")
     * document.detectFormatByExtension()
     * println(document.format) // "markdown"
     * ```
     */
    fun detectFormatByExtension() {
        val detectedFormat = FormatRegistry.detectByExtension(extension)
        format = detectedFormat.id
    }

    /**
     * Update format based on file content analysis.
     * 
     * This method analyzes the actual content to determine the most likely format
     * and updates the format property if a specific format is detected.
     * 
     * @param content The document content to analyze
     * @return true if format was detected and updated, false otherwise
     *
     * @example
     * ```kotlin
     * val content = "# Title\n\nThis is markdown content"
     * val detected = document.detectFormatByContent(content)
     * if (detected) {
     *     println(document.format) // "markdown"
     * }
     * ```
     */
    fun detectFormatByContent(content: String): Boolean {
        val detectedFormat = FormatRegistry.detectByContent(content)
        if (detectedFormat != null) {
            format = detectedFormat.id
            return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Document

        if (path != other.path) return false
        if (title != other.title) return false
        if (format != other.format) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + format.hashCode()
        return result
    }
}

/**
 * Get current time in milliseconds since epoch.
 * 
 * This is a platform-specific function implemented via expect/actual
 * to provide the current system time across different platforms (Android,
 * iOS, Desktop, Web).
 * 
 * @return Current time in milliseconds since Unix epoch
 */
expect fun currentTimeMillis(): Long

/**
 * Get file modification time for the document.
 * 
 * This is a platform-specific function implemented via expect/actual
 * to retrieve the last modification timestamp of the file at the document's path.
 * 
 * @return File modification time in milliseconds since epoch, or -1 if file doesn't exist
 */
expect fun Document.getFileModTime(): Long

/**
 * Get file size in bytes for the document.
 * 
 * This is a platform-specific function implemented via expect/actual
 * to retrieve the size of the file at the document's path.
 * 
 * @return File size in bytes, or -1 if file doesn't exist
 */
expect fun Document.getFileSize(): Long

/**
 * Check if the document file exists.
 * 
 * This is a platform-specific function implemented via expect/actual
 * to check if a file exists at the document's path.
 * 
 * @return true if the file exists, false otherwise
 */
expect fun Document.fileExists(): Boolean

/**
 * Create a Document from a file path.
 * 
 * This is a platform-specific factory function implemented via expect/actual
 * that creates a Document instance from a file path, automatically extracting
 * the title, extension, and detecting the format.
 * 
 * @param path The absolute path to the file
 * @return A Document instance if the file exists and is accessible, null otherwise
 *
 * @example
 * ```kotlin
 * val document = createDocument("/path/to/README.md")
 * document?.let {
 *     println(it.title) // "README"
 *     println(it.extension) // "md"
 * }
 * ```
 */
expect fun createDocument(path: String): Document?
