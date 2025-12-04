/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TodoTxt Format Parser - Platform Agnostic
 * Based on todo.txt format specification: http://todotxt.org/
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import digital.vasic.yole.format.*

/**
 * Represents a single TodoTxt task.
 * 
 * This data class encapsulates all components of a Todo.txt task according to the
 * official Todo.txt format specification (http://todotxt.org/). It includes priority,
 * completion status, dates, projects, contexts, and custom key-value metadata.
 *
 * @property line Original line text from the file
 * @property priority Task priority (A-Z), null if no priority
 * @property description Task description (text without metadata)
 * @property done Whether task is completed (starts with "x " or "X ")
 * @property creationDate Creation date (YYYY-MM-DD format)
 * @property completionDate Completion date (YYYY-MM-DD format)
 * @property dueDate Due date (due:YYYY-MM-DD format)
 * @property projects Project tags (prefixed with +)
 * @property contexts Context tags (prefixed with @)
 * @property keyValues Key-value pairs (key:value format)
 *
 * @example
 * ```kotlin
 * val task = TodoTxtTask(
 *     line = "(A) 2023-01-01 Call mom +Family @phone due:2023-01-15",
 *     priority = 'A',
 *     description = "Call mom",
 *     done = false,
 *     creationDate = "2023-01-01",
 *     dueDate = "2023-01-15",
 *     projects = listOf("Family"),
 *     contexts = listOf("phone"),
 *     keyValues = mapOf("due" to "2023-01-15")
 * )
 * ```
 */
data class TodoTxtTask(
    /**
     * Original line text
     */
    val line: String,

    /**
     * Task priority (A-Z), null if no priority
     */
    val priority: Char? = null,

    /**
     * Task description (text without metadata)
     */
    val description: String = "",

    /**
     * Whether task is completed (starts with "x " or "X ")
     */
    val done: Boolean = false,

    /**
     * Creation date (YYYY-MM-DD format)
     */
    val creationDate: String? = null,

    /**
     * Completion date (YYYY-MM-DD format)
     */
    val completionDate: String? = null,

    /**
     * Due date (due:YYYY-MM-DD format)
     */
    val dueDate: String? = null,

    /**
     * Project tags (prefixed with +)
     */
    val projects: List<String> = emptyList(),

    /**
     * Context tags (prefixed with @)
     */
    val contexts: List<String> = emptyList(),

    /**
     * Key-value pairs (key:value format)
     */
    val keyValues: Map<String, String> = emptyMap()
) {
    /**
     * Check if task is overdue.
     * 
     * @return true if the task has a due date and it's before today, false otherwise
     */
    fun isOverdue(): Boolean {
        val due = dueDate ?: return false
        val today = getCurrentDate()
        return due < today
    }

    /**
     * Check if task is due today.
     * 
     * @return true if the task has a due date and it's today, false otherwise
     */
    fun isDueToday(): Boolean {
        val due = dueDate ?: return false
        val today = getCurrentDate()
        return due == today
    }
}

/**
 * TodoTxt format parser.
 * 
 * Implements the Todo.txt format specification (http://todotxt.org/) for parsing
 * task lists. Supports priorities, completion status, dates, projects, contexts,
 * and custom key-value metadata. Converts tasks to HTML with appropriate styling
 * and provides syntax validation.
 *
 * @constructor Creates a new TodoTxtParser instance
 *
 * @example
 * ```kotlin
 * val parser = TodoTxtParser()
 * val content = """
 *     (A) Call Mom +Family @phone due:2023-01-15
 *     x 2023-01-10 Buy groceries
 *     (B) 2023-01-01 Finish project +work @office
 * """.trimIndent()
 * 
 * val document = parser.parse(content)
 * val tasks = parser.parseAllTasks(content)
 * println("Total tasks: ${tasks.size}")
 * println("Completed: ${tasks.count { it.done }}")
 * ```
 */
class TodoTxtParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(TextFormat.ID_TODOTXT) ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val tasks = parseAllTasks(content)
        val metadata = buildMap {
            put("totalTasks", tasks.size.toString())
            put("completedTasks", tasks.count { it.done }.toString())
            put("pendingTasks", tasks.count { !it.done }.toString())
            put("overdueTasks", tasks.count { it.isOverdue() }.toString())
        }

        // Convert to HTML
        val html = tasksToHtml(tasks, options)

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
     * Parse all tasks from content.
     * 
     * @param content The Todo.txt content to parse
     * @return List of parsed TodoTxtTask objects
     *
     * @example
     * ```kotlin
     * val content = "(A) Task 1\nx Task 2\n(B) Task 3"
     * val tasks = parser.parseAllTasks(content)
     * println(tasks.size) // 3
     * ```
     */
    fun parseAllTasks(content: String): List<TodoTxtTask> {
        return content.lines()
            .filter { it.isNotBlank() }
            .map { parseTask(it) }
    }

    /**
     * Parse a single task line according to Todo.txt format specification.
     * 
     * The parsing follows this order:
     * 1. Completion status (x or X)
     * 2. Completion date (if completed)
     * 3. Priority (A-Z in parentheses)
     * 4. Creation date
     * 5. Description and metadata
     * 
     * @param line The task line to parse
     * @return Parsed TodoTxtTask object
     *
     * @example
     * ```kotlin
     * val task = parser.parseTask("(A) 2023-01-01 Call mom +Family @phone")
     * println(task.priority) // 'A'
     * println(task.description) // "Call mom"
     * println(task.projects) // ["Family"]
     * println(task.contexts) // ["phone"]
     * ```
     */
    fun parseTask(line: String): TodoTxtTask {
        var remaining = line.trim()

        // Check if done
        val done = remaining.startsWith("x ", ignoreCase = true) ||
                   remaining.startsWith("X ")
        if (done) {
            remaining = remaining.substring(2).trim()
        }

        // Extract completion date (only if done)
        var completionDate: String? = null
        if (done) {
            val completionMatch = Regex("^($DATE_PATTERN)\\s+").find(remaining)
            if (completionMatch != null) {
                completionDate = completionMatch.groupValues[1]
                remaining = remaining.substring(completionMatch.value.length)
            }
        }

        // Extract priority
        var priority: Char? = null
        val priorityMatch = Regex("^\\(([A-Za-z])\\)\\s+").find(remaining)
        if (priorityMatch != null) {
            priority = priorityMatch.groupValues[1].uppercase()[0]
            remaining = remaining.substring(priorityMatch.value.length)
        }

        // Extract creation date
        var creationDate: String? = null
        val creationMatch = Regex("^($DATE_PATTERN)\\s+").find(remaining)
        if (creationMatch != null) {
            creationDate = creationMatch.groupValues[1]
            remaining = remaining.substring(creationMatch.value.length)
        }

        // Extract projects, contexts, key-values from full line
        val projects = extractProjects(line)
        val contexts = extractContexts(line)
        val keyValues = extractKeyValues(line)
        val dueDate = keyValues["due"]

        // Description is what's left after removing metadata
        val description = remaining
            .replace(Regex("\\+\\S+"), "")  // Remove projects
            .replace(Regex("@\\S+"), "")   // Remove contexts
            .replace(Regex("\\w+:\\S+"), "")  // Remove key:value pairs
            .trim()

        return TodoTxtTask(
            line = line,
            priority = priority,
            description = description,
            done = done,
            creationDate = creationDate,
            completionDate = completionDate,
            dueDate = dueDate,
            projects = projects,
            contexts = contexts,
            keyValues = keyValues
        )
    }

    /**
     * Extract project tags (+project) from a task line.
     * 
     * @param line The task line to extract projects from
     * @return List of project names without the + prefix
     */
    private fun extractProjects(line: String): List<String> {
        return Regex("(?:^|\\s)\\+(\\S+)").findAll(line)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Extract context tags (@context) from a task line.
     * 
     * @param line The task line to extract contexts from
     * @return List of context names without the @ prefix
     */
    private fun extractContexts(line: String): List<String> {
        return Regex("(?:^|\\s)@(\\S+)").findAll(line)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Extract key-value pairs (key:value) from a task line.
     * 
     * @param line The task line to extract key-value pairs from
     * @return Map of key-value pairs
     */
    private fun extractKeyValues(line: String): Map<String, String> {
        return Regex("(\\w+):(\\S+)").findAll(line)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    /**
     * Convert tasks to HTML with appropriate styling.
     * 
     * This method generates HTML with CSS classes for different task states
     * and components, making it easy to style tasks with CSS.
     * 
     * @param tasks List of tasks to convert
     * @param options Additional options (currently unused)
     * @return HTML representation of all tasks
     */
    private fun tasksToHtml(tasks: List<TodoTxtTask>, options: Map<String, Any>): String {
        return buildString {
            append("<div class='todotxt'>")

            for (task in tasks) {
                val classes = buildList {
                    add("task")
                    if (task.done) add("done")
                    if (task.isOverdue()) add("overdue")
                    if (task.isDueToday()) add("due-today")
                    task.priority?.let { add("priority-${it.lowercase()}") }
                }

                append("<div class='${classes.joinToString(" ")}'>")

                // Checkbox
                append("<span class='checkbox'>${if (task.done) "☑" else "☐"}</span> ")

                // Priority
                task.priority?.let {
                    append("<span class='priority'>($it)</span> ")
                }

                // Description
                append("<span class='description'>")
                append(task.description.escapeHtml())
                append("</span>")

                // Projects
                if (task.projects.isNotEmpty()) {
                    append(" <span class='projects'>")
                    task.projects.forEach {
                        append("<span class='project'>+$it</span> ")
                    }
                    append("</span>")
                }

                // Contexts
                if (task.contexts.isNotEmpty()) {
                    append(" <span class='contexts'>")
                    task.contexts.forEach {
                        append("<span class='context'>@$it</span> ")
                    }
                    append("</span>")
                }

                // Due date
                task.dueDate?.let {
                    append(" <span class='due-date'>due:$it</span>")
                }

                append("</div>")
            }

            append("</div>")
        }
    }

    companion object {
        /**
         * Regular expression pattern for YYYY-MM-DD date format.
         */
        const val DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}"
    }
}

/**
 * Register the Todo.txt parser with the registry
 */
fun registerTodoTxtParser() {
    ParserRegistry.register(TodoTxtParser())
}

/**
 * Get current date in YYYY-MM-DD format.
 * 
 * This is a platform-specific function implemented via expect/actual
 * to provide the current date across different platforms.
 * 
 * @return Current date in YYYY-MM-DD format
 */
expect fun getCurrentDate(): String
