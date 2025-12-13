/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple unit tests for TaskPaper parser
 *
 *########################################################*/
package digital.vasic.yole.format.taskpaper

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple unit tests for TaskPaper format parser.
 */
class TaskPaperParserSimpleTest {

    private val parser = TaskpaperParser()

    @Test
    fun `should parse basic TaskPaper document`() {
        val content = """
            Work Project:
            - Complete documentation
            - Review code changes
            Note about the project
            
            Personal Tasks:
            - Buy groceries
            - Call dentist
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("2", result.metadata["projects"])
        assertEquals("4", result.metadata["tasks"])
        assertEquals("1", result.metadata["notes"])
    }

    @Test
    fun `should detect TaskPaper format by extension`() {
        val format = FormatRegistry.getByExtension(".taskpaper")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TASKPAPER, format.id)
        assertEquals("TaskPaper", format.name)
    }

    @Test
    fun `should parse tasks with tags`() {
        val content = """
            - Complete task @today @high
            - Another task @done(2025-01-10)
            - Task with due date @due(2025-01-15)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("3", result.metadata["tasks"])
        assertEquals("1", result.metadata["todayTasks"])
        assertEquals("1", result.metadata["doneTasks"])
    }

    @Test
    fun `should convert to HTML`() {
        val content = """
            Project:
            - Task @today @done
            Note about project
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='taskpaper'>"))
        assertTrue(html.contains("taskpaper-project"))
        assertTrue(html.contains("taskpaper-task"))
        assertTrue(html.contains("taskpaper-note"))
        assertTrue(html.contains("taskpaper-tag-today"))
        assertTrue(html.contains("taskpaper-tag-done"))
    }

    @Test
    fun `should validate content`() {
        // Test valid content
        val validContent = """
            Project:
            - Valid task
            Note
        """.trimIndent()

        val validErrors = parser.validate(validContent)
        assertTrue(validErrors.isEmpty())

        // Test malformed task marker
        val invalidContent = """
            -Invalid task (no space)
        """.trimIndent()

        val invalidErrors = parser.validate(invalidContent)
        assertTrue(invalidErrors.isNotEmpty())
    }

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
    fun `should parse indented hierarchy`() {
        val content = """
            Main Project:
            	Subproject:
            		- Deep task @done
            		Deep note
            	- Shallow task
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TASKPAPER, result.format.id)
        assertEquals("2", result.metadata["projects"])
        assertEquals("2", result.metadata["tasks"])
        assertEquals("1", result.metadata["notes"])
        assertEquals("1", result.metadata["doneTasks"])
    }
}