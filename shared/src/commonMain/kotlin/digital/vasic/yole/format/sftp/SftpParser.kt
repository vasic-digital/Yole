/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * SFTP format parser
 *
 *########################################################*/

package digital.vasic.yole.format.sftp

import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat

/**
 * Transport-layer adapter for SFTP network protocol, registered in the format system.
 *
 * **Important**: This is NOT a file format parser. SFTP is a secure file transfer
 * protocol over SSH, not a text format. This class exists as an adapter so that
 * the [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent
 * SFTP as a storage backend alongside actual text format parsers (Markdown,
 * CSV, LaTeX, etc.).
 *
 * The actual SFTP file operations (list, upload, download, etc.) are handled by
 * [SftpService][digital.vasic.yole.network.protocols.sftp.SftpService].
 * This parser simply passes content through as plain text.
 *
 * The `.sftp` extension is a synthetic placeholder and does not correspond to a
 * real file format.
 */
class SftpParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "sftp",
        name = "SFTP",
        defaultExtension = ".sftp",
        extensions = listOf(".sftp")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Pass-through: SFTP is a transport protocol, not a text format.
        // Content is returned as-is. Actual file operations use SftpService.
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}