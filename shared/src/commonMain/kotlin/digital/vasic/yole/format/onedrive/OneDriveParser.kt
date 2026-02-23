/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * OneDrive format parser
 *
 *########################################################*/

package digital.vasic.yole.format.onedrive

import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat

/**
 * Transport-layer adapter for Microsoft OneDrive cloud storage, registered in the format system.
 *
 * **Important**: This is NOT a file format parser. OneDrive is a cloud storage
 * service, not a text format. This class exists as an adapter so that the
 * [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent OneDrive
 * as a storage backend alongside actual text format parsers (Markdown, CSV,
 * LaTeX, etc.).
 *
 * The actual OneDrive file operations (list, upload, download, etc.) are handled
 * by [OneDriveService][digital.vasic.yole.network.protocols.onedrive.OneDriveService].
 * This parser simply passes content through as plain text.
 *
 * The `.one` extension is a synthetic placeholder and does not correspond to a
 * parseable text format (Microsoft OneNote .one files are binary).
 */
class OneDriveParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "onedrive",
        name = "OneDrive",
        defaultExtension = ".one",
        extensions = listOf(".one")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Pass-through: OneDrive is a cloud storage service, not a text format.
        // Content is returned as-is. Actual file operations use OneDriveService.
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}