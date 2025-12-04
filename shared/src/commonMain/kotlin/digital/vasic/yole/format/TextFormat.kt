/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform Text Format System
 * Platform-agnostic format definitions
 *
 *########################################################*/
package digital.vasic.yole.format

/**
 * Represents a text format supported by Yole.
 * 
 * This data class encapsulates all metadata about a supported text format,
 * including its identifier, human-readable name, file extensions, and detection
 * patterns. This is the KMP version of FormatRegistry.Format.
 *
 * @property id Unique format identifier (e.g., "markdown", "todotxt", "latex")
 * @property name Human-readable format name (e.g., "Markdown", "Todo.txt", "LaTeX")
 * @property defaultExtension Default file extension with dot (e.g., ".md", ".txt", ".tex")
 * @property extensions Supported file extensions for this format
 * @property detectionPatterns Format detection patterns (e.g., file content patterns)
 *
 * @example
 * ```kotlin
 * val markdownFormat = TextFormat(
 *     id = "markdown",
 *     name = "Markdown",
 *     defaultExtension = ".md",
 *     extensions = listOf(".md", ".markdown", ".mkd"),
 *     detectionPatterns = listOf("^#+ ", "^\\[.*\\]\\(.*\\)")
 * )
 * ```
 */
data class TextFormat(
    /**
     * Unique format identifier (e.g., "markdown", "todotxt", "latex")
     */
    val id: String,

    /**
     * Human-readable format name (e.g., "Markdown", "Todo.txt", "LaTeX")
     */
    val name: String,

    /**
     * Default file extension with dot (e.g., ".md", ".txt", ".tex")
     */
    val defaultExtension: String,

    /**
     * Supported file extensions for this format
     */
    val extensions: List<String> = listOf(defaultExtension),

    /**
     * Format detection patterns (e.g., file content patterns)
     */
    val detectionPatterns: List<String> = emptyList()
) {
    companion object {
        /**
         * Standard format identifiers used throughout the application.
         * 
         * These constants provide a centralized way to reference format IDs
         * and help avoid string literals throughout the codebase.
         */
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
}


