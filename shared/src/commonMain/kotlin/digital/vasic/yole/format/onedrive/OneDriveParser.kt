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
 * Parser for OneDrive format files
 * 
 * This parser handles OneDrive-specific file format parsing.
 * Currently implements basic text parsing functionality.
 */
class OneDriveParser : TextParser {
    
    override val supportedFormat = TextFormat(
        id = "onedrive",
        name = "OneDrive",
        defaultExtension = ".one",
        extensions = listOf(".one")
    )
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Basic implementation - treat as plain text for now
        // In a real implementation, this would parse OneDrive-specific format
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = content,
            metadata = emptyMap(),
            errors = emptyList()
        )
    }
}