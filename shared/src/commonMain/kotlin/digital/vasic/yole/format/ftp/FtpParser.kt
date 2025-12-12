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
 * Parser for FTP format files
 * 
 * This parser handles FTP-related file format parsing.
 * Currently implements basic text parsing functionality.
 */
class FtpParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "ftp",
        name = "FTP",
        defaultExtension = ".ftp",
        extensions = listOf(".ftp")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Basic implementation - treat as plain text for now
        // In a real implementation, this would parse FTP-specific format
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}