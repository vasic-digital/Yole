/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Todo.txt Parser Tests - Comprehensive Coverage
 *
 *########################################################*/

package digital.vasic.yole.format

import digital.vasic.yole.format.todotxt.TodoTxtParser
import kotlin.test.*

/**
 * Comprehensive test suite for TodoTxtParser
 * Tests all aspects of todo.txt format parsing
 */
class TodoTxtParserTest {

    private lateinit var parser: TodoTxtParser

    @BeforeTest
    fun setup() {
        parser = TodoTxtParser()
    }

    @Test
    fun testBasicTodoTask() {
        val input = "(A) Call Mom +family @phone"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals("A", result.priority)
        assertEquals("Call Mom", result.text)
        assertEquals(1, result.contexts.size)
        assertEquals("phone", result.contexts[0])
        assertEquals(1, result.projects.size)
        assertEquals("family", result.projects[0])
        assertFalse(result.isCompleted)
    }

    @Test
    fun testCompletedTask() {
        val input = "x 2023-12-01 2023-11-30 Completed task +done @home"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertTrue(result.isCompleted)
        assertEquals("2023-12-01", result.completionDate)
        assertEquals("2023-11-30", result.creationDate)
        assertEquals("Completed task", result.text)
        assertEquals(1, result.projects.size)
        assertEquals("done", result.projects[0])
        assertEquals(1, result.contexts.size)
        assertEquals("home", result.contexts[0])
    }

    @Test
    fun testTaskWithDeadline() {
        val input = "(B) Submit report +work due:2023-12-15 @office"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals("B", result.priority)
        assertEquals("Submit report", result.text)
        assertEquals("2023-12-15", result.dueDate)
        assertEquals(1, result.projects.size)
        assertEquals("work", result.projects[0])
        assertEquals(1, result.contexts.size)
        assertEquals("office", result.contexts[0])
    }

    @Test
    fun testMultipleContextsAndProjects() {
        val input = "(C) Complex task +project1 +project2 @context1 @context2 @context3"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals("C", result.priority)
        assertEquals("Complex task", result.text)
        assertEquals(3, result.contexts.size)
        assertEquals(2, result.projects.size)
        assertTrue(result.contexts.containsAll(listOf("context1", "context2", "context3")))
        assertTrue(result.projects.containsAll(listOf("project1", "project2")))
    }

    @Test
    fun testTodoTxtEdgeCases() {
        // Empty task
        val emptyResult = parser.parse("")
        assertNotNull(emptyResult)
        assertEquals("", emptyResult.text)
        assertFalse(result.isCompleted)

        // Task with only whitespace
        val whitespaceResult = parser.parse("   \t\n   ")
        assertNotNull(whitespaceResult)
        assertEquals("", whitespaceResult.text)

        // Task with special characters
        val specialResult = parser.parse("(A) Task with special: !@#$%^&*() +project @context")
        assertNotNull(specialResult)
        assertEquals("Task with special: !@#$%^&*()", specialResult.text)

        // Task with unicode
        val unicodeResult = parser.parse("(A) Unicode task: 你好世界 🌍 +国际化 @中文")
        assertNotNull(unicodeResult)
        assertEquals("Unicode task: 你好世界 🌍", unicodeResult.text)
    }

    @Test
    fun testTodoTxtFormatting() {
        val input = "(A) Call Mom +family @phone"
        val result = parser.parse(input)
        
        val formatted = parser.toTodoTxt(result)
        assertEquals(input, formatted)
    }

    @Test
    fun testTodoTxtRoundTrip() {
        val originalInput = "(B) Submit annual report +work @office due:2023-12-31"
        val result = parser.parse(originalInput)
        
        val regenerated = parser.toTodoTxt(result)
        val reparsed = parser.parse(regenerated)
        
        assertEquals(result.priority, reparsed.priority)
        assertEquals(result.text, reparsed.text)
        assertEquals(result.projects, reparsed.projects)
        assertEquals(result.contexts, reparsed.contexts)
        assertEquals(result.dueDate, reparsed.dueDate)
    }

    @Test
    fun testTodoTxtPerformance() {
        val largeInput = buildString {
            repeat(1000) { i ->
                appendLine("(A) Task $i +project @context")
            }
        }

        val startTime = System.currentTimeMillis()
        val tasks = largeInput.lines().mapNotNull { line ->
            if (line.isNotBlank()) parser.parse(line) else null
        }
        val endTime = System.currentTimeMillis()
        
        assertEquals(1000, tasks.size)
        assertTrue(endTime - startTime < 500, "Parsing 1000 tasks should complete within 500ms")
    }

    @Test
    fun testTodoTxtErrorHandling() {
        // Malformed tasks should not crash
        val malformedInputs = listOf(
            "x", // Incomplete completed task
            "(Z) Invalid priority", // Invalid priority
            "Task without priority +project @context", // No priority
            "(A) Task with @ but no context", // Invalid context
            "(A) Task with + but no project", // Invalid project
            "x 2023-13-45 Invalid date" // Invalid date
        )

        malformedInputs.forEach { input ->
            val result = parser.parse(input)
            assertNotNull(result, "Should handle malformed input: $input")
        }
    }

    @Test
    fun testTodoTxtQuerySyntax() {
        val input = """
            (A) Urgent task +project1 @context1
            (B) Important task +project1 @context2
            (C) Normal task +project2 @context1
            x 2023-12-01 Completed task +project1 @context1
        """.trimIndent()

        val lines = input.lines().filter { it.isNotBlank() }
        val tasks = lines.map { parser.parse(it) }
        
        // Test priority filtering
        val priorityATasks = tasks.filter { it.priority == "A" }
        assertEquals(1, priorityATasks.size)
        
        // Test project filtering
        val project1Tasks = tasks.filter { it.projects.contains("project1") }
        assertEquals(3, project1Tasks.size)
        
        // Test context filtering
        val context1Tasks = tasks.filter { it.contexts.contains("context1") }
        assertEquals(3, context1Tasks.size)
        
        // Test completion filtering
        val completedTasks = tasks.filter { it.isCompleted }
        assertEquals(1, completedTasks.size)
    }
}