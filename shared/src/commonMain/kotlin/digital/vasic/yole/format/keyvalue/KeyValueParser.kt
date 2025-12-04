/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * KeyValue Format Parser - Platform Agnostic
 * Handles INI, YAML, JSON, TOML, VCF, ICS formats
 *
 *########################################################*/
package digital.vasic.yole.format.keyvalue

import digital.vasic.yole.format.*

/**
 * Key-value file type
 */
enum class KeyValueType {
    INI,        // INI files with [sections]
    YAML,       // YAML-like key: value
    JSON,       // JSON files
    TOML,       // TOML files
    PROPERTIES, // Java properties (key=value)
    VCARD,      // vCard format (.vcf)
    ICALENDAR,  // iCalendar format (.ics)
    ZIM,        // Zim Wiki config
    GENERIC     // Generic key-value
}

/**
 * Represents a key-value entry
 */
data class KeyValueEntry(
    val key: String,
    val value: String,
    val section: String? = null,
    val lineNumber: Int,
    val isComment: Boolean = false,
    val isSection: Boolean = false
)

/**
 * Key-value format parser
 * Handles various key-value formats: INI, YAML, JSON, TOML, properties, etc.
 */
class KeyValueParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_KEYVALUE) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Determine key-value type
        val type = detectType(extension)

        // Parse entries
        val entries = parseKeyValueEntries(content)

        val metadata = buildMap {
            put("type", type.name.lowercase())
            put("extension", extension)
            put("entries", entries.filter { !it.isComment && !it.isSection }.size.toString())
            put("lines", content.lines().size.toString())
            put("sections", entries.count { it.isSection }.toString())
        }

        // Convert to HTML
        val html = toHtml(type, entries, content)

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = html,
            metadata = metadata
        )
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return document.parsedContent
    }

    /**
     * Parse key-value entries from content
     */
    private fun parseKeyValueEntries(content: String): List<KeyValueEntry> {
        val entries = mutableListOf<KeyValueEntry>()
        val lines = content.lines()
        var currentSection: String? = null

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            when {
                // Empty line
                trimmed.isEmpty() -> { /* skip */ }

                // INI section: [section]
                trimmed.matches(Regex("^\\[.*\\]$")) -> {
                    val sectionName = trimmed.removeSurrounding("[", "]")
                    currentSection = sectionName
                    entries.add(
                        KeyValueEntry(
                            key = sectionName,
                            value = "",
                            section = null,
                            lineNumber = index + 1,
                            isSection = true
                        )
                    )
                }

                // Comment: # or ; or //
                trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.startsWith("//") -> {
                    entries.add(
                        KeyValueEntry(
                            key = "",
                            value = trimmed,
                            section = currentSection,
                            lineNumber = index + 1,
                            isComment = true
                        )
                    )
                }

                // Key-value pairs
                else -> {
                    parseKeyValueLine(trimmed)?.let { (key, value) ->
                        entries.add(
                            KeyValueEntry(
                                key = key,
                                value = value,
                                section = currentSection,
                                lineNumber = index + 1
                            )
                        )
                    }
                }
            }
        }

        return entries
    }

    /**
     * Parse a single key-value line
     * Supports formats: key=value, key:value, key: value, "key": value
     */
    private fun parseKeyValueLine(line: String): Pair<String, String>? {
        // Try different separators: =, :, -
        val separators = listOf("=", ":", "-")

        for (separator in separators) {
            if (separator in line) {
                val parts = line.split(separator, limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                    val value = parts[1].trim().removeSurrounding("\"", "\"")
                    if (key.isNotEmpty()) {
                        return Pair(key, value)
                    }
                }
            }
        }

        return null
    }

    /**
     * Convert to HTML with syntax highlighting
     */
    private fun toHtml(type: KeyValueType, entries: List<KeyValueEntry>, originalContent: String): String {
        return buildString {
            append("<div class='keyvalue'>")
            append("<pre style='white-space: pre-wrap; font-family: monospace;'>")

            val lines = originalContent.lines()
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()

                when {
                    // Empty line
                    trimmed.isEmpty() -> {
                        append("\n")
                    }

                    // Section header
                    trimmed.matches(Regex("^\\[.*\\]$")) -> {
                        append("<span style='color: #ef6d00; font-size: 1.25em; font-weight: bold;'>")
                        append(line.escapeHtml())
                        append("</span>\n")
                    }

                    // Comment
                    trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.startsWith("//") -> {
                        append("<span style='color: #88b04b;'>")
                        append(line.escapeHtml())
                        append("</span>\n")
                    }

                    // Key-value pair
                    else -> {
                        val highlighted = highlightKeyValue(line)
                        append(highlighted)
                        append("\n")
                    }
                }
            }

            append("</pre>")
            append("</div>")
        }
    }

    /**
     * Highlight key-value pair
     */
    private fun highlightKeyValue(line: String): String {
        // Try to split by separator
        val separators = listOf("=", ":", "-")

        for (separator in separators) {
            if (separator in line) {
                val index = line.indexOf(separator)
                val key = line.substring(0, index)
                val rest = line.substring(index)

                return buildString {
                    append("<span style='font-weight: bold;'>")
                    append(key.escapeHtml())
                    append("</span>")
                    append(rest.escapeHtml())
                }
            }
        }

        // No separator found, return as-is
        return line.escapeHtml()
    }

    /**
     * Detect key-value type from extension
     */
    private fun detectType(extension: String): KeyValueType {
        return when (extension.lowercase()) {
            ".ini" -> KeyValueType.INI
            ".yaml", ".yml" -> KeyValueType.YAML
            ".json" -> KeyValueType.JSON
            ".toml" -> KeyValueType.TOML
            ".properties" -> KeyValueType.PROPERTIES
            ".vcf" -> KeyValueType.VCARD
            ".ics" -> KeyValueType.ICALENDAR
            ".zim" -> KeyValueType.ZIM
            else -> KeyValueType.GENERIC
        }
    }

    /**
     * Extract file extension from filename
     */
    private fun getExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }

    override fun validate(content: String): List<String> {
        val errors = mutableListOf<String>()

        val lines = content.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.startsWith("//")) {
                return@forEachIndexed
            }

            // Skip section headers
            if (trimmed.matches(Regex("^\\[.*\\]$"))) {
                return@forEachIndexed
            }

            // Check if line contains a separator
            val hasSeparator = "=" in trimmed || ":" in trimmed || "-" in trimmed
            if (!hasSeparator) {
                errors.add("Line ${index + 1}: No key-value separator found")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".yml", ".yaml", ".toml", ".vcf", ".ics", ".ini", ".json", ".zim", ".properties")
    }
}

/**
 * Register the Key-Value parser with the registry
 */
fun registerKeyValueParser() {
    ParserRegistry.register(KeyValueParser())
}
