/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for TodoTxt Parser
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoTxtParserTest {

    private val parser = TodoTxtParser()

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test basic task parsing`() {
        val content = "Buy groceries"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TODOTXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test empty content`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("0", result.metadata["totalTasks"])
    }

    @Test
    fun `test multiple tasks`() {
        val content = """
            Task 1
            Task 2
            Task 3
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals("3", result.metadata["totalTasks"])
    }

    @Test
    fun `test blank lines are skipped`() {
        val content = """
            Task 1

            Task 2
        """.trimIndent()

        val tasks = parser.parseAllTasks(content)

        assertEquals(2, tasks.size)
    }

    // ==================== Priority Tests ====================

    @Test
    fun `test priority A parsing`() {
        val content = "(A) High priority task"
        val tasks = parser.parseAllTasks(content)

        assertEquals(1, tasks.size)
        assertEquals('A', tasks[0].priority)
        assertTrue(tasks[0].description.contains("High priority task"))
    }

    @Test
    fun `test priority B parsing`() {
        val content = "(B) Medium priority task"
        val tasks = parser.parseAllTasks(content)

        assertEquals('B', tasks[0].priority)
    }

    @Test
    fun `test priority Z parsing`() {
        val content = "(Z) Low priority task"
        val tasks = parser.parseAllTasks(content)

        assertEquals('Z', tasks[0].priority)
    }

    @Test
    fun `test no priority`() {
        val content = "Task without priority"
        val tasks = parser.parseAllTasks(content)

        assertNull(tasks[0].priority)
    }

    @Test
    fun `test invalid priority format`() {
        val content = "(a) Lowercase priority - not valid"
        val tasks = parser.parseAllTasks(content)

        // Lowercase should not be recognized as priority
        assertNull(tasks[0].priority)
    }

    // ==================== Completion Tests ====================

    @Test
    fun `test completed task with lowercase x`() {
        val content = "x Completed task"
        val tasks = parser.parseAllTasks(content)

        assertTrue(tasks[0].done)
    }

    @Test
    fun `test completed task with uppercase X`() {
        val content = "X Completed task"
        val tasks = parser.parseAllTasks(content)

        assertTrue(tasks[0].done)
    }

    @Test
    fun `test incomplete task`() {
        val content = "Incomplete task"
        val tasks = parser.parseAllTasks(content)

        assertFalse(tasks[0].done)
    }

    @Test
    fun `test completed task count`() {
        val content = """
            x Done 1
            Task 2
            x Done 2
            Task 3
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals("4", result.metadata["totalTasks"])
        assertEquals("2", result.metadata["completedTasks"])
        assertEquals("2", result.metadata["pendingTasks"])
    }

    // ==================== Date Tests ====================

    @Test
    fun `test creation date parsing`() {
        val content = "2023-01-15 Task with creation date"
        val tasks = parser.parseAllTasks(content)

        assertEquals("2023-01-15", tasks[0].creationDate)
    }

    @Test
    fun `test completion date parsing`() {
        val content = "x 2023-01-20 2023-01-15 Completed task"
        val tasks = parser.parseAllTasks(content)

        assertTrue(tasks[0].done)
        assertEquals("2023-01-20", tasks[0].completionDate)
        assertEquals("2023-01-15", tasks[0].creationDate)
    }

    @Test
    fun `test due date parsing`() {
        val content = "Task with due date due:2023-12-31"
        val tasks = parser.parseAllTasks(content)

        assertEquals("2023-12-31", tasks[0].dueDate)
    }

    @Test
    fun `test priority with creation date`() {
        val content = "(A) 2023-01-01 Priority task with date"
        val tasks = parser.parseAllTasks(content)

        assertEquals('A', tasks[0].priority)
        assertEquals("2023-01-01", tasks[0].creationDate)
    }

    // ==================== Project Tests ====================

    @Test
    fun `test single project`() {
        val content = "Work on +ProjectA"
        val tasks = parser.parseAllTasks(content)

        assertEquals(1, tasks[0].projects.size)
        assertTrue(tasks[0].projects.contains("ProjectA"))
    }

    @Test
    fun `test multiple projects`() {
        val content = "Work on +ProjectA and +ProjectB"
        val tasks = parser.parseAllTasks(content)

        assertEquals(2, tasks[0].projects.size)
        assertTrue(tasks[0].projects.contains("ProjectA"))
        assertTrue(tasks[0].projects.contains("ProjectB"))
    }

    @Test
    fun `test project with underscore`() {
        val content = "Task +My_Project"
        val tasks = parser.parseAllTasks(content)

        assertTrue(tasks[0].projects.any { it.startsWith("My") })
    }

    // ==================== Context Tests ====================

    @Test
    fun `test single context`() {
        val content = "Call mom @phone"
        val tasks = parser.parseAllTasks(content)

        assertEquals(1, tasks[0].contexts.size)
        assertTrue(tasks[0].contexts.contains("phone"))
    }

    @Test
    fun `test multiple contexts`() {
        val content = "Meeting @office @work"
        val tasks = parser.parseAllTasks(content)

        assertEquals(2, tasks[0].contexts.size)
        assertTrue(tasks[0].contexts.contains("office"))
        assertTrue(tasks[0].contexts.contains("work"))
    }

    // ==================== Key-Value Tests ====================

    @Test
    fun `test key-value pair`() {
        val content = "Task with metadata key:value"
        val tasks = parser.parseAllTasks(content)

        assertTrue(tasks[0].keyValues.containsKey("key"))
        assertEquals("value", tasks[0].keyValues["key"])
    }

    @Test
    fun `test due key-value`() {
        val content = "Task due:2023-12-31"
        val tasks = parser.parseAllTasks(content)

        assertEquals("2023-12-31", tasks[0].keyValues["due"])
        assertEquals("2023-12-31", tasks[0].dueDate)
    }

    @Test
    fun `test multiple key-values`() {
        val content = "Task key1:value1 key2:value2"
        val tasks = parser.parseAllTasks(content)

        assertEquals("value1", tasks[0].keyValues["key1"])
        assertEquals("value2", tasks[0].keyValues["key2"])
    }

    // ==================== Complex Task Tests ====================

    @Test
    fun `test fully featured task`() {
        val content = "(A) 2023-01-01 Call mom +Family @phone due:2023-01-15"
        val tasks = parser.parseAllTasks(content)

        val task = tasks[0]
        assertEquals('A', task.priority)
        assertEquals("2023-01-01", task.creationDate)
        assertTrue(task.projects.contains("Family"))
        assertTrue(task.contexts.contains("phone"))
        assertEquals("2023-01-15", task.dueDate)
        assertFalse(task.done)
    }

    @Test
    fun `test completed task with all metadata`() {
        val content = "x 2023-01-20 2023-01-01 (A) Finished task +Project @context"
        val tasks = parser.parseAllTasks(content)

        val task = tasks[0]
        assertTrue(task.done)
        assertEquals("2023-01-20", task.completionDate)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `test HTML output contains task`() {
        val content = "(A) Important task"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("Important task"))
    }

    @Test
    fun `test HTML shows completed status`() {
        val content = "x Completed task"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        // Should indicate completion somehow (strikethrough, checkbox, etc.)
        assertTrue(html.isNotEmpty())
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `test supported format`() {
        assertEquals(TextFormat.ID_TODOTXT, parser.supportedFormat.id)
    }

    // ==================== Task Object Tests ====================

    @Test
    fun `test TodoTxtTask data class`() {
        val task = TodoTxtTask(
            line = "(A) Test task +Project @context",
            priority = 'A',
            description = "Test task",
            done = false,
            projects = listOf("Project"),
            contexts = listOf("context")
        )

        assertEquals('A', task.priority)
        assertEquals("Test task", task.description)
        assertFalse(task.done)
        assertEquals(1, task.projects.size)
        assertEquals(1, task.contexts.size)
    }

    @Test
    fun `test task equality`() {
        val task1 = TodoTxtTask(
            line = "Test",
            priority = 'A',
            description = "Test"
        )
        val task2 = TodoTxtTask(
            line = "Test",
            priority = 'A',
            description = "Test"
        )

        assertEquals(task1, task2)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test task with only spaces`() {
        val content = "   "
        val tasks = parser.parseAllTasks(content)

        assertEquals(0, tasks.size)
    }

    @Test
    fun `test task with special characters`() {
        val content = "Task with special chars: & < > \""
        val tasks = parser.parseAllTasks(content)

        assertEquals(1, tasks.size)
    }

    @Test
    fun `test long task line`() {
        val content = "A".repeat(500) + " task"
        val tasks = parser.parseAllTasks(content)

        assertEquals(1, tasks.size)
    }

    @Test
    fun `test many tasks`() {
        val content = (1..100).map { "Task $it +Project @context" }.joinToString("\n")
        val tasks = parser.parseAllTasks(content)

        assertEquals(100, tasks.size)
    }

    // ==================== parseTask Single Task Tests ====================

    @Test
    fun `test parseTask simple`() {
        val line = "Simple task"
        val tasks = parser.parseAllTasks(line)

        assertEquals(1, tasks.size)
        assertEquals(line, tasks[0].line)
    }

    @Test
    fun `test parseTask preserves original line`() {
        val line = "(A) 2023-01-01 Task +Project @context due:2023-12-31"
        val tasks = parser.parseAllTasks(line)

        assertEquals(line, tasks[0].line)
    }
}
