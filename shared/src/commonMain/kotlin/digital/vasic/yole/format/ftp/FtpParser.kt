/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * FTP format parser
 *
 *########################################################*/

package digital.vasic.yole.format.ftp

import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat

/**
 * Transport-layer adapter for FTP network protocol, registered in the format system.
 *
 * **Important**: This is NOT a file format parser. FTP is a network file transfer
 * protocol (RFC 959), not a text format. This class exists as an adapter so that
 * the [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent
 * FTP as a storage backend alongside actual text format parsers (Markdown,
 * CSV, LaTeX, etc.).
 *
 * The actual FTP file operations (list, upload, download, etc.) are handled by
 * [FtpService][digital.vasic.yole.network.protocols.ftp.FtpService].
 * This parser simply passes content through as plain text.
 *
 * The `.ftp` extension is a synthetic placeholder and does not correspond to a
 * real file format.
 */
class FtpParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "ftp",
        name = "FTP",
        defaultExtension = ".ftp",
        extensions = listOf(".ftp")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Pass-through: FTP is a transport protocol, not a text format.
        // Content is returned as-is. Actual file operations use FtpService.
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}