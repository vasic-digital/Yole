/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TaskpaperParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.taskpaper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskpaperParserHtmlTest {

    private val parser = TaskpaperParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInTaskpaperDiv() {
        val doc = parser.parse("- Task one")
        assertTrue(doc.parsedContent.contains("class='taskpaper'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("- Task one")
        assertTrue(doc.parsedContent.contains("<style>"))
    }

    // ==================== Projects ====================

    @Test
    fun testProjectRendered() {
        val doc = parser.parse("My Project:")
        assertTrue(doc.parsedContent.contains("taskpaper-project"))
    }

    @Test
    fun testProjectContent() {
        val doc = parser.parse("Development:")
        assertTrue(doc.parsedContent.contains("Development"))
    }

    // ==================== Tasks ====================

    @Test
    fun testTaskRendered() {
        val doc = parser.parse("- Buy groceries")
        assertTrue(doc.parsedContent.contains("taskpaper-task"))
    }

    @Test
    fun testTaskMarker() {
        val doc = parser.parse("- Do something")
        assertTrue(doc.parsedContent.contains("- "))
    }

    @Test
    fun testDoneTaskClass() {
        val doc = parser.parse("- Completed task @done")
        assertTrue(doc.parsedContent.contains("taskpaper-task-done"))
    }

    @Test
    fun testDoneTaskStrikethrough() {
        val doc = parser.parse("- Completed task @done")
        assertTrue(doc.parsedContent.contains("line-through"))
    }

    // ==================== Notes ====================

    @Test
    fun testNoteRendered() {
        val doc = parser.parse("Some note text")
        assertTrue(doc.parsedContent.contains("taskpaper-note"))
    }

    @Test
    fun testNoteItalic() {
        val doc = parser.parse("Some note text")
        assertTrue(doc.parsedContent.contains("font-style: italic"))
    }

    // ==================== Tags ====================

    @Test
    fun testTagHighlighted() {
        val doc = parser.parse("- Task @priority")
        assertTrue(doc.parsedContent.contains("taskpaper-tag"))
    }

    @Test
    fun testDoneTagClass() {
        val doc = parser.parse("- Task @done")
        assertTrue(doc.parsedContent.contains("taskpaper-tag-done"))
    }

    @Test
    fun testTodayTagClass() {
        val doc = parser.parse("- Task @today")
        assertTrue(doc.parsedContent.contains("taskpaper-tag-today"))
    }

    @Test
    fun testTagWithValue() {
        val doc = parser.parse("- Task @due(2025-12-31)")
        assertTrue(doc.parsedContent.contains("@due(2025-12-31)"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataTaskCount() {
        val doc = parser.parse("- Task 1\n- Task 2\n- Task 3")
        assertEquals("3", doc.metadata["tasks"])
    }

    @Test
    fun testMetadataProjectCount() {
        val doc = parser.parse("Project A:\n- Task 1\nProject B:\n- Task 2")
        assertEquals("2", doc.metadata["projects"])
    }

    @Test
    fun testMetadataDoneTasks() {
        val doc = parser.parse("- Done task @done\n- Pending task")
        assertEquals("1", doc.metadata["doneTasks"])
    }

    @Test
    fun testMetadataTodayTasks() {
        val doc = parser.parse("- Today task @today\n- Other task")
        assertEquals("1", doc.metadata["todayTasks"])
    }

    @Test
    fun testMetadataLineCount() {
        val doc = parser.parse("- Task 1\n- Task 2")
        assertEquals("2", doc.metadata["lines"])
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("Project:\n- Task @done")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testMalformedTaskMarker() {
        val errors = parser.validate("-missing space")
        assertTrue(errors.any { it.contains("Task marker") })
    }

    @Test
    fun testUnclosedTagParameter() {
        val errors = parser.validate("- Task @due(2025-01-01")
        assertTrue(errors.any { it.contains("Unclosed tag parameter") })
    }

    // ==================== TaskpaperItem ====================

    @Test
    fun testTaskpaperItemHasTag() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task @done",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("done" to "")
        )
        assertTrue(item.hasTag("done"))
        assertTrue(item.isDone())
    }

    @Test
    fun testTaskpaperItemGetTagValue() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task @due(2025-12-31)",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("due" to "2025-12-31")
        )
        assertEquals("2025-12-31", item.getDueDate())
    }

    @Test
    fun testTaskpaperItemIsToday() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task @today",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("today" to "")
        )
        assertTrue(item.isToday())
    }

    // ==================== TaskpaperItemType enum ====================

    @Test
    fun testTaskpaperItemTypeEntries() {
        assertEquals(4, TaskpaperItemType.entries.size)
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("- Task")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }
}
