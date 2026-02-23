/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Google Drive format parser
 *
 *########################################################*/

package digital.vasic.yole.format.googledrive

import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat

/**
 * Transport-layer adapter for Google Drive cloud storage, registered in the format system.
 *
 * **Important**: This is NOT a file format parser. Google Drive is a cloud storage
 * service, not a text format. This class exists as an adapter so that the
 * [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent Google
 * Drive as a storage backend alongside actual text format parsers (Markdown, CSV,
 * LaTeX, etc.).
 *
 * The actual Google Drive file operations (list, upload, download, etc.) are
 * handled by [GoogleDriveService][digital.vasic.yole.network.protocols.googledrive.GoogleDriveService].
 * This parser simply passes content through as plain text.
 *
 * The `.gdoc`, `.gsheet`, `.gslides` extensions are Google Workspace link files
 * (JSON pointers to online documents), not parseable text content.
 */
class GoogleDriveParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "googledrive",
        name = "Google Drive",
        defaultExtension = ".gdoc",
        extensions = listOf(".gdoc", ".gsheet", ".gslides")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Pass-through: Google Drive is a cloud storage service, not a text format.
        // Content is returned as-is. Actual file operations use GoogleDriveService.
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}