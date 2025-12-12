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
 * Parser for SFTP format files
 * 
 * This parser handles SFTP-specific file format parsing.
 * Currently implements basic text parsing functionality.
 */
class SftpParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "sftp",
        name = "SFTP",
        defaultExtension = ".sftp",
        extensions = listOf(".sftp")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Basic implementation - treat as plain text for now
        // In a real implementation, this would parse SFTP-specific format
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}