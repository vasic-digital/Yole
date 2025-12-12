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
 * Parser for Dropbox format files (.dbx)
 * 
 * This parser handles Dropbox-specific file format parsing.
 * Currently implements basic text parsing functionality.
 */
class DropboxParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "dropbox",
        name = "Dropbox",
        defaultExtension = ".dbx",
        extensions = listOf(".dbx")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Basic implementation - treat as plain text for now
        // In a real implementation, this would parse Dropbox-specific format
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}