/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TaskPaper Format Parser - Platform Agnostic
 * Handles TaskPaper format for task management
 *
 *########################################################*/
package digital.vasic.yole.format.taskpaper

import digital.vasic.yole.format.*

/**
 * TaskPaper item type
 */
enum class TaskpaperItemType {
    PROJECT,    // Project: line ending with ":"
    TASK,       // Task: line starting with "- " or "\t- "
    NOTE,       // Note: any other line
    EMPTY       // Empty line
}

/**
 * Represents a TaskPaper item (project, task, or note)
 */
data class TaskpaperItem(
    val type: TaskpaperItemType,
    val content: String,
    val indentLevel: Int,
    val lineNumber: Int,
    val tags: Map<String, String> = emptyMap()
) {
    /**
     * Check if item has a specific tag
     */
    fun hasTag(tag: String): Boolean = tags.containsKey(tag)

    /**
     * Get tag value (for tags with parameters like @due(2025-10-27))
     */
    fun getTagValue(tag: String): String? = tags[tag]

    /**
     * Check if task is done
     */
    fun isDone(): Boolean = hasTag("done")

    /**
     * Check if task is for today
     */
    fun isToday(): Boolean = hasTag("today")

    /**
     * Get due date if present
     */
    fun getDueDate(): String? = getTagValue("due")
}

/**
 * TaskPaper format parser
 * Handles projects, tasks, notes, and tags
 */
class TaskpaperParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_TASKPAPER) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val extension = getExtension(filename)

        // Parse items
        val items = parseItems(content)

        val metadata = buildMap {
            put("extension", extension)
            put("lines", content.lines().size.toString())
            put("projects", items.count { it.type == TaskpaperItemType.PROJECT }.toString())
            put("tasks", items.count { it.type == TaskpaperItemType.TASK }.toString())
            put("notes", items.count { it.type == TaskpaperItemType.NOTE }.toString())
            put("doneTasks", items.count { it.type == TaskpaperItemType.TASK && it.isDone() }.toString())
            put("todayTasks", items.count { it.type == TaskpaperItemType.TASK && it.isToday() }.toString())
        }

        // Convert to HTML
        val html = itemsToHtml(items)

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
     * Parse TaskPaper items from content
     */
    private fun parseItems(content: String): List<TaskpaperItem> {
        val items = mutableListOf<TaskpaperItem>()
        val lines = content.lines()

        lines.forEachIndexed { index, line ->
            if (line.trim().isEmpty()) {
                items.add(
                    TaskpaperItem(
                        type = TaskpaperItemType.EMPTY,
                        content = "",
                        indentLevel = 0,
                        lineNumber = index + 1
                    )
                )
            } else {
                items.add(parseItem(line, index + 1))
            }
        }

        return items
    }

    /**
     * Parse a single TaskPaper item
     */
    private fun parseItem(line: String, lineNumber: Int): TaskpaperItem {
        val indentLevel = countIndent(line)
        val trimmed = line.trimStart()

        val type = when {
            trimmed.startsWith("- ") -> TaskpaperItemType.TASK
            trimmed.endsWith(":") -> TaskpaperItemType.PROJECT
            else -> TaskpaperItemType.NOTE
        }

        // Extract content (remove task marker if present)
        val content = if (type == TaskpaperItemType.TASK) {
            trimmed.substring(2) // Remove "- "
        } else {
            trimmed
        }

        // Extract tags
        val tags = extractTags(content)

        return TaskpaperItem(
            type = type,
            content = content,
            indentLevel = indentLevel,
            lineNumber = lineNumber,
            tags = tags
        )
    }

    /**
     * Count indentation level (number of tabs)
     */
    private fun countIndent(line: String): Int {
        var count = 0
        for (char in line) {
            if (char == '\t') {
                count++
            } else {
                break
            }
        }
        return count
    }

    /**
     * Extract tags from content
     * Tags format: @tagname or @tagname(value)
     */
    private fun extractTags(content: String): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        val tagRegex = Regex("""@(\w+)(?:\(([^)]*)\))?""")

        tagRegex.findAll(content).forEach { match ->
            val tagName = match.groupValues[1]
            val tagValue = match.groupValues[2].ifEmpty { "" }
            tags[tagName] = tagValue
        }

        return tags
    }

    /**
     * Convert items to HTML
     */
    private fun itemsToHtml(items: List<TaskpaperItem>): String {
        return buildString {
            append("<div class='taskpaper'>")
            append("<style>")
            append(".taskpaper { font-family: monospace; white-space: pre-wrap; }")
            append(".taskpaper-project { font-weight: bold; color: #ef6d00; font-size: 1.15em; }")
            append(".taskpaper-task { color: #333; }")
            append(".taskpaper-task-done { color: #888; text-decoration: line-through; }")
            append(".taskpaper-note { color: #666; font-style: italic; }")
            append(".taskpaper-tag { color: #0066cc; font-weight: bold; }")
            append(".taskpaper-tag-done { color: #00aa00; }")
            append(".taskpaper-tag-today { color: #cc0000; }")
            append("</style>")

            items.forEach { item ->
                when (item.type) {
                    TaskpaperItemType.EMPTY -> {
                        append("\n")
                    }

                    TaskpaperItemType.PROJECT -> {
                        val indent = "\t".repeat(item.indentLevel)
                        append(indent)
                        append("<span class='taskpaper-project'>")
                        append(highlightTags(item.content).escapeHtml())
                        append("</span>\n")
                    }

                    TaskpaperItemType.TASK -> {
                        val indent = "\t".repeat(item.indentLevel)
                        append(indent)
                        val taskClass = if (item.isDone()) "taskpaper-task-done" else "taskpaper-task"
                        append("<span class='$taskClass'>")
                        append("- ")
                        append(highlightTags(item.content))
                        append("</span>\n")
                    }

                    TaskpaperItemType.NOTE -> {
                        val indent = "\t".repeat(item.indentLevel)
                        append(indent)
                        append("<span class='taskpaper-note'>")
                        append(item.content.escapeHtml())
                        append("</span>\n")
                    }
                }
            }

            append("</div>")
        }
    }

    /**
     * Highlight tags in content
     */
    private fun highlightTags(content: String): String {
        var result = content.escapeHtml()
        val tagRegex = Regex("""@(\w+)(?:\(([^)]*)\))?""")

        tagRegex.findAll(content).forEach { match ->
            val fullTag = match.value
            val tagName = match.groupValues[1]

            val tagClass = when (tagName) {
                "done" -> "taskpaper-tag-done"
                "today" -> "taskpaper-tag-today"
                else -> "taskpaper-tag"
            }

            val escapedTag = fullTag.escapeHtml()
            val highlighted = "<span class='$tagClass'>$escapedTag</span>"
            result = result.replace(escapedTag, highlighted)
        }

        return result
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
            val trimmed = line.trimStart()

            // Check for malformed tasks
            if (trimmed.startsWith("-") && !trimmed.startsWith("- ")) {
                errors.add("Line ${index + 1}: Task marker should be '- ' (hyphen followed by space)")
            }

            // Check for malformed tags
            val tagRegex = Regex("""@(\w+)\([^)]*$""") // Tag with opening paren but no closing
            if (tagRegex.containsMatchIn(trimmed)) {
                errors.add("Line ${index + 1}: Unclosed tag parameter")
            }
        }

        return errors
    }

    companion object {
        // Supported extensions
        val EXTENSIONS = setOf(".taskpaper", ".todo", ".txt")
    }
}

/**
 * Register the TaskPaper parser with the registry
 */
fun registerTaskpaperParser() {
    ParserRegistry.register(TaskpaperParser())
}
