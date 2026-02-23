/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Dropbox format parser
 *
 *########################################################*/

package digital.vasic.yole.format.dropbox

import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat

/**
 * Transport-layer adapter for Dropbox cloud storage, registered in the format system.
 *
 * **Important**: This is NOT a file format parser. Dropbox is a cloud storage
 * transport protocol, not a text format. This class exists as an adapter so that
 * the [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent
 * Dropbox as a storage backend alongside actual text format parsers (Markdown,
 * CSV, LaTeX, etc.).
 *
 * The actual Dropbox file operations (list, upload, download, etc.) are handled
 * by [DropboxService][digital.vasic.yole.network.protocols.dropbox.DropboxService].
 * This parser simply passes content through as plain text.
 *
 * The `.dbx` extension is a synthetic placeholder and does not correspond to a
 * real file format.
 */
class DropboxParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "dropbox",
        name = "Dropbox",
        defaultExtension = ".dbx",
        extensions = listOf(".dbx")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Pass-through: Dropbox is a transport protocol, not a text format.
        // Content is returned as-is. Actual file operations use DropboxService.
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}