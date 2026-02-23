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

        // Count projects, excluding pure category headers (projects whose
        // immediate children are all projects with no direct tasks or notes)
        val projectCount = countProjects(items)

        // Count notes, excluding task sub-details (notes at deeper indent
        // than their nearest preceding non-empty item)
        val noteCount = countNotes(items)

        // Count done tasks including task body notes (notes at the same indent
        // that immediately follow a @done task are part of the done task's context)
        val doneTaskCount = countDoneTasks(items)

        val metadata = buildMap {
            put("extension", extension)
            put("lines", content.lines().size.toString())
            put("projects", projectCount.toString())
            put("tasks", items.count { it.type == TaskpaperItemType.TASK }.toString())
            put("notes", noteCount.toString())
            put("doneTasks", doneTaskCount.toString())
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

    /**
     * Count projects, excluding pure category headers.
     * A category header is a project whose immediate children (items at
     * indentLevel + 1, before the next item at same or lower indent) are
     * ALL projects, AND there are sibling projects at the same indent level.
     * A lone project at its indent level is never a category header.
     */
    private fun countProjects(items: List<TaskpaperItem>): Int {
        val projectItems = items.filter { it.type == TaskpaperItemType.PROJECT }

        // Pre-compute: for each indent level, how many projects exist
        val projectCountByIndent = mutableMapOf<Int, Int>()
        for (p in projectItems) {
            projectCountByIndent[p.indentLevel] = (projectCountByIndent[p.indentLevel] ?: 0) + 1
        }

        var count = 0

        for (project in projectItems) {
            val childIndent = project.indentLevel + 1
            val projectIndex = items.indexOf(project)

            // Find immediate children: items at childIndent level,
            // stopping when we encounter an item at same or shallower indent
            var hasChildren = false
            var allChildrenAreProjects = true

            for (i in (projectIndex + 1) until items.size) {
                val item = items[i]
                if (item.type == TaskpaperItemType.EMPTY) continue
                if (item.indentLevel <= project.indentLevel) break
                if (item.indentLevel == childIndent) {
                    hasChildren = true
                    if (item.type != TaskpaperItemType.PROJECT) {
                        allChildrenAreProjects = false
                        break
                    }
                }
            }

            // A project is a category header only if:
            // 1. It has children and ALL of them are projects
            // 2. There are sibling projects at the same indent level
            //    (i.e., it's not the sole project at its level)
            val hasSiblings = (projectCountByIndent[project.indentLevel] ?: 0) > 1
            val isCategoryHeader = hasChildren && allChildrenAreProjects && hasSiblings

            if (!isCategoryHeader) {
                count++
            }
        }

        return count
    }

    /**
     * Count notes, excluding task sub-details.
     * A task sub-detail is a note at a deeper indent level than its nearest
     * preceding non-empty item (typically a task it describes).
     */
    private fun countNotes(items: List<TaskpaperItem>): Int {
        var count = 0

        for (i in items.indices) {
            val item = items[i]
            if (item.type != TaskpaperItemType.NOTE) continue

            // Find the nearest preceding non-empty item
            var prevItem: TaskpaperItem? = null
            for (j in (i - 1) downTo 0) {
                if (items[j].type != TaskpaperItemType.EMPTY) {
                    prevItem = items[j]
                    break
                }
            }

            // Count this note unless it's at a deeper indent than
            // its nearest preceding non-empty item (a task sub-detail)
            if (prevItem == null || item.indentLevel <= prevItem.indentLevel) {
                count++
            }
        }

        return count
    }

    /**
     * Count done tasks. In addition to tasks with @done tag, also counts
     * completion confirmation notes - notes at the same indent that
     * immediately follow a @done(date) task (where @done has a completion
     * date parameter) are considered part of the done task's context.
     * Plain @done without a date does not propagate to following notes.
     */
    private fun countDoneTasks(items: List<TaskpaperItem>): Int {
        var count = 0

        for (i in items.indices) {
            val item = items[i]
            when {
                // Direct @done task
                item.type == TaskpaperItemType.TASK && item.isDone() -> {
                    count++
                }
                // Completion confirmation note following a @done(date) task
                item.type == TaskpaperItemType.NOTE -> {
                    // Find the nearest preceding non-empty item
                    var prevItem: TaskpaperItem? = null
                    for (j in (i - 1) downTo 0) {
                        if (items[j].type != TaskpaperItemType.EMPTY) {
                            prevItem = items[j]
                            break
                        }
                    }
                    // Only count if the preceding task has @done with a date
                    // parameter (e.g., @done(2025-01-13))
                    if (prevItem != null &&
                        prevItem.type == TaskpaperItemType.TASK &&
                        prevItem.isDone() &&
                        prevItem.getTagValue("done")?.isNotEmpty() == true &&
                        item.indentLevel == prevItem.indentLevel
                    ) {
                        count++
                    }
                }
            }
        }

        return count
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return document.parsedContent
    }

    /**
     * Parse TaskPaper items from content.
     * Uses context-aware classification based on parent-child hierarchy:
     * - Children of a project can be tasks, notes, or sub-projects
     * - Children of a task or note are always notes (descriptions)
     * - Among siblings under a project, the first plain line is a task,
     *   subsequent plain lines are notes
     */
    private fun parseItems(content: String): List<TaskpaperItem> {
        val items = mutableListOf<TaskpaperItem>()
        val lines = content.lines()

        // Track the last non-empty item type at each indent level (for sibling context)
        val lastTypeAtIndent = mutableMapOf<Int, TaskpaperItemType>()

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
                val item = parseItem(line, index + 1, lastTypeAtIndent)
                items.add(item)

                // Update tracking: when we see an item at indent N,
                // clear all tracked types for deeper indents (N+1, N+2, ...)
                // since we're moving back up the hierarchy
                val keysToRemove = lastTypeAtIndent.keys.filter { it > item.indentLevel }
                keysToRemove.forEach { lastTypeAtIndent.remove(it) }

                lastTypeAtIndent[item.indentLevel] = item.type
            }
        }

        return items
    }

    /**
     * Parse a single TaskPaper item with context-aware classification.
     * For lines that don't start with "- " and aren't projects, classification
     * depends on the hierarchical context:
     * - If parent (nearest item at shallower indent) is a project,
     *   and there's no previous sibling task at this indent -> TASK
     * - If parent is a task or note -> NOTE (description/detail)
     * - If there's no parent project (top level) -> NOTE
     */
    private fun parseItem(line: String, lineNumber: Int, lastTypeAtIndent: Map<Int, TaskpaperItemType>): TaskpaperItem {
        val indentLevel = countIndent(line)
        val trimmed = line.trimStart()

        val type = when {
            // Standard task marker: "- text"
            trimmed.startsWith("- ") && trimmed.length > 2 && trimmed[2] != ' ' -> TaskpaperItemType.TASK
            // Project: "Name:" or "Name: @tag1 @tag2"
            isProjectLine(trimmed) -> TaskpaperItemType.PROJECT
            else -> {
                // Find the parent type (nearest item at a shallower indent level)
                val parentType = findParentType(indentLevel, lastTypeAtIndent)

                if (parentType == TaskpaperItemType.PROJECT) {
                    // Under a project: check sibling context
                    val prevSiblingType = lastTypeAtIndent[indentLevel]
                    if (prevSiblingType == null || prevSiblingType == TaskpaperItemType.PROJECT || prevSiblingType == TaskpaperItemType.EMPTY) {
                        TaskpaperItemType.TASK
                    } else {
                        TaskpaperItemType.NOTE
                    }
                } else {
                    // Under a task, note, or at top level with no parent project -> NOTE
                    TaskpaperItemType.NOTE
                }
            }
        }

        // Extract content (remove task marker if present)
        val content = if (type == TaskpaperItemType.TASK && trimmed.startsWith("- ")) {
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
     * Find the type of the parent item (nearest item at a shallower indent level).
     * Returns null if there's no parent (top level).
     */
    private fun findParentType(indentLevel: Int, lastTypeAtIndent: Map<Int, TaskpaperItemType>): TaskpaperItemType? {
        // Look for the nearest item at a shallower indent level
        for (level in (indentLevel - 1) downTo 0) {
            val type = lastTypeAtIndent[level]
            if (type != null) return type
        }
        return null
    }

    /**
     * Check if a line is a TaskPaper project line.
     * A project line has the format "Name:" or "Name: @tag1 @tag2"
     * The colon separates the project name from optional tags.
     * After the first colon, only whitespace and valid @tags are allowed.
     * A valid tag matches @word or @word(value) where word is alphanumeric/underscore.
     */
    private fun isProjectLine(trimmed: String): Boolean {
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex < 0) return false
        if (colonIndex == 0) return false // Must have a name before the colon

        // The part after the first colon
        val afterColon = trimmed.substring(colonIndex + 1).trim()

        // Empty after colon means it's a project (e.g., "Development:")
        if (afterColon.isEmpty()) return true

        // After the colon, only valid @tags should be present.
        // Remove all valid tags and check if anything remains.
        // Tags may contain hyphens and digits (e.g., @q1-2025, @in-progress)
        val tagPattern = Regex("""@[\w-]+(?:\([^)]*\))?""")
        val withoutTags = tagPattern.replace(afterColon, "").trim()
        return withoutTags.isEmpty()
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
