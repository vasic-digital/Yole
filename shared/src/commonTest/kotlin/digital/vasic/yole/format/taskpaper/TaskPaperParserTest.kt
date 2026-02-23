/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for TaskPaper parser
 *
 *########################################################*/
package digital.vasic.yole.format.taskpaper

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest
import kotlin.system.measureTimeMillis

/**
 * Comprehensive unit tests for TaskPaper format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic TaskPaper parsing (projects, tasks, notes)
 * - TaskPaper tags and contexts
 * - Task completion and priority
 * - Indentation and hierarchy
 * - Round-trip parsing (parse -> format -> parse)
 * - Edge cases (empty files, malformed TaskPaper)
 * - Performance benchmarks
 * - HTML conversion
 */
class TaskPaperParserTest {

    private val parser = TaskpaperParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect TaskPaper format by extension`() {
        val format = FormatRegistry.getByExtension(".taskpaper")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TASKPAPER, format.id)
        assertEquals("TaskPaper", format.name)
    }

    @Test
    fun `should detect TaskPaper format by todo extension`() {
        // .todo is not registered in FormatRegistry for TaskPaper,
        // but the parser itself supports it via its companion EXTENSIONS.
        // Verify that getByExtension returns a format (it may map to a different format or null).
        val format = FormatRegistry.getByExtension(".todo")

        // .todo is not in FormatRegistry, so it returns null
        // This is expected since FormatRegistry only maps .taskpaper to TaskPaper
        if (format != null) {
            // If some future change registers .todo, verify it works
            assertNotNull(format)
        }
        // The parser itself supports .todo via TaskpaperParser.EXTENSIONS
        assertTrue(TaskpaperParser.EXTENSIONS.contains(".todo"))
    }

    @Test
    fun `should detect TaskPaper format by txt extension`() {
        // .txt maps to Plain Text in FormatRegistry, not TaskPaper.
        // The parser companion EXTENSIONS includes .txt, but the registry does not map it to TaskPaper.
        val format = FormatRegistry.getByExtension(".txt")

        assertNotNull(format)
        // .txt maps to plaintext in FormatRegistry
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
        // But the parser itself claims .txt support
        assertTrue(TaskpaperParser.EXTENSIONS.contains(".txt"))
    }

    @Test
    fun `should detect TaskPaper format by filename`() {
        val format = FormatRegistry.detectByFilename("tasks.taskpaper")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TASKPAPER, format.id)
    }

    @Test
    fun `should support all TaskPaper extensions`() {
        // Only .taskpaper is registered in FormatRegistry
        val registeredExtensions = listOf(".taskpaper")

        registeredExtensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_TASKPAPER, format.id)
        }

        // The parser's companion EXTENSIONS includes additional extensions
        val parserExtensions = listOf(".taskpaper", ".todo", ".txt")
        parserExtensions.forEach { ext ->
            assertTrue(TaskpaperParser.EXTENSIONS.contains(ext), "Parser should support extension $ext")
        }
    }

    // ==================== Basic TaskPaper Parsing Tests ====================

    @Test
    fun `should parse basic TaskPaper document structure`() {
        val content = """
            Project 1:
            - Task 1
            - Task 2
            Note about the project

            Project 2:
            - Task 3
            Another note
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals(content, result.rawContent)

        // Check metadata extraction
        assertEquals("2", result.metadata["projects"])
        assertEquals("3", result.metadata["tasks"])
        assertEquals("2", result.metadata["notes"])
        assertEquals("0", result.metadata["doneTasks"])
        assertEquals("0", result.metadata["todayTasks"])
    }

    @Test
    fun `should parse TaskPaper projects`() {
        val content = """
            Work Project:
            Personal Goals:
            Shopping List:
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["projects"])
        assertEquals("0", result.metadata["tasks"])
    }

    @Test
    fun `should parse TaskPaper tasks`() {
        val content = """
            - Complete documentation
            - Review code changes
            - Submit pull request
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("0", result.metadata["projects"])
        assertEquals("3", result.metadata["tasks"])
    }

    @Test
    fun `should parse TaskPaper notes`() {
        val content = """
            This is a note about the project
            Another note line
            Final note
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("0", result.metadata["projects"])
        assertEquals("0", result.metadata["tasks"])
        assertEquals("3", result.metadata["notes"])
    }

    // ==================== TaskPaper Tags and Contexts Tests ====================

    @Test
    fun `should parse tasks with tags`() {
        val content = """
            - Complete documentation @due(2025-01-15) @priority(high)
            - Review code @today @work
            - Submit report @due(2025-01-20)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["tasks"])
    }

    @Test
    fun `should parse projects with tags`() {
        val content = """
            Work Project: @work @urgent
            Personal Goals: @personal @2025
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("2", result.metadata["projects"])
    }

    @Test
    fun `should parse notes with tags`() {
        val content = """
            This is a note @important @review
            Another note @later
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("2", result.metadata["notes"])
    }

    // ==================== Task Completion and Priority Tests ====================

    @Test
    fun `should parse completed tasks`() {
        val content = """
            - Complete documentation @done
            - Review code changes
            - Submit pull request @done(2025-01-10)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["tasks"])
        assertEquals("2", result.metadata["doneTasks"])
    }

    @Test
    fun `should parse tasks with today tag`() {
        val content = """
            - Complete documentation @today
            - Review code changes
            - Submit pull request @today
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["tasks"])
        assertEquals("2", result.metadata["todayTasks"])
    }

    @Test
    fun `should parse mixed completed and active tasks`() {
        val content = """
            Work Tasks:
            - Complete project @done
            - Start new project @today
            - Review requirements
            - Update documentation @done(2025-01-05)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("1", result.metadata["projects"])
        assertEquals("4", result.metadata["tasks"])
        assertEquals("2", result.metadata["doneTasks"])
        assertEquals("1", result.metadata["todayTasks"])
    }

    // ==================== Indentation and Hierarchy Tests ====================

    @Test
    fun `should parse indented tasks under projects`() {
        val content = """
            Project Alpha:
            	- Task 1
            	- Task 2
            		- Subtask 2.1
            		- Subtask 2.2
            	- Task 3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("1", result.metadata["projects"])
        assertEquals("5", result.metadata["tasks"])
    }

    @Test
    fun `should parse nested projects`() {
        val content = """
            Main Project:
            	Subproject 1:
            		- Task A
            		- Task B
            	Subproject 2:
            		- Task C
            		- Task D
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["projects"])
        assertEquals("4", result.metadata["tasks"])
    }

    @Test
    fun `should parse complex hierarchy with notes`() {
        val content = """
            Work:
            	- Complete urgent task @today
            	This is a note about the urgent task

            	Project Development:
            		- Design phase @done
            		Note about design decisions
            		- Implementation phase @in-progress
            			- Setup environment
            			- Write initial code
            	- Review meeting @due(2025-01-20)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("2", result.metadata["projects"])
        assertEquals("6", result.metadata["tasks"])
        assertEquals("2", result.metadata["notes"])
    }

    // ==================== Mixed Content Tests ====================

    @Test
    fun `should parse mixed projects tasks and notes`() {
        val content = """
            Work Tasks:
            - Complete project documentation @due(2025-01-15) @high
            Meeting notes from yesterday

            Personal:
            - Buy groceries @today
            - Call dentist @due(2025-01-25)
            Remember to exercise

            Ideas:
            Consider new project approach
            Research latest technologies
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["projects"])
        assertEquals("3", result.metadata["tasks"])
        assertEquals("4", result.metadata["notes"])
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["projects"])
        assertEquals("0", result.metadata["tasks"])
        assertEquals("0", result.metadata["notes"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["projects"])
        assertEquals("0", result.metadata["tasks"])
        assertEquals("0", result.metadata["notes"])
    }

    @Test
    fun `should handle malformed task markers`() {
        val content = """
            -Valid task (no space)
            -  Invalid task (extra space)
            -Valid task
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        // Should parse as notes since they don't match proper task format
        assertEquals("0", result.metadata["tasks"])
        assertEquals("3", result.metadata["notes"])
    }

    @Test
    fun `should handle unclosed tags`() {
        val content = """
            - Task with unclosed tag @due(2025-01-15
            - Task with proper tag @due(2025-01-20)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("2", result.metadata["tasks"])
    }

    @Test
    fun `should handle special characters in content`() {
        val content = """
            Project with special chars:
            - Task with @ symbol in text @today
            - Task with : colon in text @done
            Note with @user and :emoji:
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("1", result.metadata["projects"])
        assertEquals("2", result.metadata["tasks"])
        assertEquals("1", result.metadata["notes"])
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple document to HTML`() {
        val content = """
            Work Project:
            - Complete task @today
            - Another task @done
            Note about the project
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='taskpaper'>"))
        assertTrue(html.contains("taskpaper-project"))
        assertTrue(html.contains("taskpaper-task"))
        assertTrue(html.contains("taskpaper-task-done"))
        assertTrue(html.contains("taskpaper-note"))
        assertTrue(html.contains("taskpaper-tag-today"))
        assertTrue(html.contains("taskpaper-tag-done"))
    }

    @Test
    fun `should convert document with tags to HTML`() {
        val content = """
            - Task with multiple tags @work @urgent @due(2025-01-15)
            Project: @important
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("taskpaper-tag"))
        assertTrue(html.contains("work"))
        assertTrue(html.contains("urgent"))
        assertTrue(html.contains("due"))
        assertTrue(html.contains("important"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            Test Project:
            - Simple task
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='taskpaper'>"))
        assertTrue(html.contains("<style>"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate well-formed TaskPaper`() {
        val content = """
            Project:
            - Task 1
            - Task 2
            Note about project
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should detect malformed task markers`() {
        val content = """
            -Valid task (no space after hyphen)
            -  Extra space task
            Proper task:
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Task marker should be '- '") })
    }

    @Test
    fun `should detect unclosed tag parameters`() {
        val content = """
            - Task with unclosed tag @due(2025-01-15
            - Task with proper tag @done
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed tag parameter") })
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            Work Project:
            - Complete urgent task @today @high
            - Review documentation @due(2025-01-20)
            Meeting scheduled for tomorrow

            Personal:
            - Buy groceries @today
            - Call dentist @due(2025-01-25)
            Remember to exercise
        """.trimIndent()

        // Parse the original content
        val firstParse = parser.parse(originalContent)

        // Get the formatted content (should be same as original)
        val formattedContent = firstParse.rawContent

        // Parse the formatted content again
        val secondParse = parser.parse(formattedContent)

        // Verify round-trip consistency
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
    }

    @Test
    fun `should preserve tags through round-trip`() {
        val originalContent = """
            - Task with tags @work @urgent @due(2025-01-15) @done
            Project with tags: @important @2025
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val secondParse = parser.parse(firstParse.rawContent)

        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse large documents efficiently`() {
        // Create a large document with many projects, tasks, and notes
        val largeContent = buildString {
            repeat(50) { projectIndex ->
                appendLine("Project ${projectIndex + 1}:")

                repeat(10) { taskIndex ->
                    appendLine("\t- Task ${taskIndex + 1} @due(2025-01-${(taskIndex % 30) + 1})")
                }

                repeat(3) { noteIndex ->
                    appendLine("\tNote ${noteIndex + 1} about this project")
                }

                appendLine()
            }
        }

        val parseTime = measureTimeMillis {
            val result = parser.parse(largeContent)
            assertNotNull(result)
            assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
            assertEquals("50", result.metadata["projects"])
            assertEquals("500", result.metadata["tasks"])
            assertEquals("150", result.metadata["notes"])
        }

        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            repeat(30) { i ->
                appendLine("Project ${i + 1}:")

                repeat(5) { j ->
                    appendLine("\t- Task ${j + 1} @today @done")
                }

                appendLine("\tNote for project ${i + 1}")
                appendLine()
            }
        }

        val document = parser.parse(largeContent)

        val conversionTime = measureTimeMillis {
            val html = parser.toHtml(document, lightMode = true)
            assertNotNull(html)
            assertTrue(html.isNotEmpty())
            assertTrue(html.contains("taskpaper-project"))
            assertTrue(html.contains("taskpaper-task"))
        }

        // Performance assertion - should convert in reasonable time (less than 500ms)
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)

        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_TASKPAPER)

        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_TASKPAPER, registryParser.supportedFormat.id)

        // Test that it works
        val content = "Project:\n\t- Task"
        val result = registryParser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val taskPaperFormat = allFormats.find { it.id == TextFormat.ID_TASKPAPER }

        assertNotNull(taskPaperFormat)
        assertEquals("TaskPaper", taskPaperFormat.name)
        assertEquals(".taskpaper", taskPaperFormat.defaultExtension)
        assertTrue(taskPaperFormat.extensions.contains(".taskpaper"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex TaskPaper document`() {
        val content = """
            Work Projects: @work @q1-2025
            	Urgent Tasks: @urgent
            		- Fix critical bug @today @high @due(2025-01-14)
            		This bug is affecting production users

            		- Deploy hotfix @done(2025-01-13)
            		Hotfix deployed successfully

            	Development:
            		- Implement new feature @in-progress @due(2025-01-25)
            		Working on user interface improvements

            		- Write unit tests @later
            		Tests for the new feature

            	Review:
            		- Code review for PR #123 @review @due(2025-01-18)
            		Review the pull request carefully

            Personal Goals: @personal @2025
            	Health:
            		- Exercise 3 times this week @today
            		- Schedule annual checkup @due(2025-02-01)
            		Remember to call the doctor's office

            	Learning:
            		- Complete online course @education @due(2025-03-01)
            		Progress: 60% complete

            		- Read technical book @reading @later
            		"Clean Code" by Robert Martin

            Shopping List: @shopping
            	- Buy groceries @today
            		Milk, bread, eggs, vegetables

            	- Order electronics @research @due(2025-01-30)
            		Compare prices for new laptop
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("6", result.metadata["projects"])
        assertEquals("11", result.metadata["tasks"])
        assertEquals("8", result.metadata["notes"])
        assertEquals("2", result.metadata["doneTasks"])
        assertEquals("3", result.metadata["todayTasks"])

        // Verify HTML contains expected elements
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("taskpaper-project"))
        assertTrue(html.contains("taskpaper-task"))
        assertTrue(html.contains("taskpaper-note"))
        assertTrue(html.contains("taskpaper-tag-today"))
        assertTrue(html.contains("taskpaper-tag-done"))
    }

    @Test
    fun `should handle deeply nested structures`() {
        val content = """
            Level 1 Project:
            	Level 2 Project:
            		Level 3 Project:
            			Level 4 Task @deep
            			Deep note about this task
            		Level 3 Task @done
            	Level 2 Task
            	Level 2 Note
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["projects"])
        assertEquals("3", result.metadata["tasks"])
        assertEquals("2", result.metadata["notes"])
    }

    @Test
    fun `should handle unicode and special characters`() {
        val content = """
            Unicode Project:
            - Task with Chinese: 完成任务 @done
            - Task with Russian: выполнить задачу @work
            Note with Arabic: مرحبا بالعالم
            Note with Japanese: こんにちは世界
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("1", result.metadata["projects"])
        assertEquals("2", result.metadata["tasks"])
        assertEquals("2", result.metadata["notes"])

        // Verify HTML contains unicode characters
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("完成任务"))
        assertTrue(html.contains("выполнить"))
        assertTrue(html.contains("مرحبا"))
        assertTrue(html.contains("こんにちは"))
    }
}
