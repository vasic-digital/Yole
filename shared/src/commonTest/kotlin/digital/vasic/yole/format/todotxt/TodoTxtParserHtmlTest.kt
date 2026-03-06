/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TodoTxtParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoTxtParserHtmlTest {

    private val parser = TodoTxtParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInTodotxtDiv() {
        val doc = parser.parse("(A) Buy milk")
        assertTrue(doc.parsedContent.contains("todotxt"))
    }

    @Test
    fun testTaskDivRendered() {
        val doc = parser.parse("(A) Buy milk")
        assertTrue(doc.parsedContent.contains("class='task"))
    }

    // ==================== Checkbox rendering ====================

    @Test
    fun testUncheckedCheckbox() {
        val doc = parser.parse("(A) Pending task")
        assertTrue(doc.parsedContent.contains("☐"))
    }

    @Test
    fun testCheckedCheckbox() {
        val doc = parser.parse("x 2026-01-01 Completed task")
        assertTrue(doc.parsedContent.contains("☑"))
    }

    // ==================== Priority rendering ====================

    @Test
    fun testPriorityARendered() {
        val doc = parser.parse("(A) High priority")
        assertTrue(doc.parsedContent.contains("priority"))
        assertTrue(doc.parsedContent.contains("(A)"))
    }

    @Test
    fun testPriorityBRendered() {
        val doc = parser.parse("(B) Medium priority")
        assertTrue(doc.parsedContent.contains("(B)"))
    }

    @Test
    fun testPriorityCssClass() {
        val doc = parser.parse("(A) Task")
        assertTrue(doc.parsedContent.contains("priority-a"))
    }

    @Test
    fun testPriorityBCssClass() {
        val doc = parser.parse("(B) Task")
        assertTrue(doc.parsedContent.contains("priority-b"))
    }

    // ==================== Done state ====================

    @Test
    fun testDoneTaskHasDoneClass() {
        val doc = parser.parse("x 2026-01-01 Done task")
        assertTrue(doc.parsedContent.contains("done"))
    }

    @Test
    fun testActiveTaskNoDoneClass() {
        val doc = parser.parse("(A) Active task")
        val classes = doc.parsedContent
        assertTrue(!classes.contains("class='task done"))
    }

    // ==================== Description ====================

    @Test
    fun testDescriptionRendered() {
        val doc = parser.parse("(A) Buy groceries")
        assertTrue(doc.parsedContent.contains("description"))
    }

    // ==================== Projects ====================

    @Test
    fun testProjectRendered() {
        val doc = parser.parse("(A) Task +ProjectA")
        assertTrue(doc.parsedContent.contains("project"))
        assertTrue(doc.parsedContent.contains("+ProjectA"))
    }

    @Test
    fun testMultipleProjects() {
        val doc = parser.parse("(A) Task +Work +Home")
        assertTrue(doc.parsedContent.contains("+Work"))
        assertTrue(doc.parsedContent.contains("+Home"))
    }

    @Test
    fun testProjectsContainerClass() {
        val doc = parser.parse("(A) Task +Work")
        assertTrue(doc.parsedContent.contains("projects"))
    }

    // ==================== Contexts ====================

    @Test
    fun testContextRendered() {
        val doc = parser.parse("(A) Call mom @phone")
        assertTrue(doc.parsedContent.contains("context"))
        assertTrue(doc.parsedContent.contains("@phone"))
    }

    @Test
    fun testMultipleContexts() {
        val doc = parser.parse("(A) Task @home @evening")
        assertTrue(doc.parsedContent.contains("@home"))
        assertTrue(doc.parsedContent.contains("@evening"))
    }

    @Test
    fun testContextsContainerClass() {
        val doc = parser.parse("(A) Task @phone")
        assertTrue(doc.parsedContent.contains("contexts"))
    }

    // ==================== Due dates ====================

    @Test
    fun testDueDateRendered() {
        val doc = parser.parse("(A) Task due:2026-03-15")
        assertTrue(doc.parsedContent.contains("due-date"))
        assertTrue(doc.parsedContent.contains("due:2026-03-15"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataTotalCount() {
        val doc = parser.parse("(A) Task 1\n(B) Task 2\nx Done task")
        assertEquals("3", doc.metadata["totalTasks"])
    }

    @Test
    fun testMetadataDoneCount() {
        val doc = parser.parse("(A) Task 1\nx Done task 1\nx Done task 2")
        assertEquals("2", doc.metadata["completedTasks"])
    }

    @Test
    fun testMetadataPendingCount() {
        val doc = parser.parse("(A) Task 1\n(B) Task 2\nx Done task")
        assertEquals("2", doc.metadata["pendingTasks"])
    }

    // ==================== Multiple tasks ====================

    @Test
    fun testMultipleTasksRendered() {
        val doc = parser.parse("(A) First\n(B) Second\n(C) Third")
        assertTrue(doc.parsedContent.contains("First"))
        assertTrue(doc.parsedContent.contains("Second"))
        assertTrue(doc.parsedContent.contains("Third"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("0", doc.metadata["totalTasks"])
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("(A) Task")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    // ==================== HTML escaping ====================

    @Test
    fun testDescriptionEscapesHtml() {
        val doc = parser.parse("(A) Use <script> tag")
        assertTrue(!doc.parsedContent.contains("<script> tag") || doc.parsedContent.contains("&lt;script&gt;"))
    }

    // ==================== Task parsing ====================

    @Test
    fun testParseTaskPriority() {
        val task = parser.parseTask("(A) Important task")
        assertEquals('A', task.priority)
    }

    @Test
    fun testParseTaskDone() {
        val task = parser.parseTask("x 2026-01-01 Done task")
        assertTrue(task.done)
    }

    @Test
    fun testParseTaskNotDone() {
        val task = parser.parseTask("(A) Active task")
        assertTrue(!task.done)
    }

    @Test
    fun testParseTaskProjects() {
        val task = parser.parseTask("(A) Task +ProjectA +ProjectB")
        assertTrue(task.projects.contains("ProjectA"))
        assertTrue(task.projects.contains("ProjectB"))
    }

    @Test
    fun testParseTaskContexts() {
        val task = parser.parseTask("(A) Task @home @evening")
        assertTrue(task.contexts.contains("home"))
        assertTrue(task.contexts.contains("evening"))
    }

    @Test
    fun testParseAllTasks() {
        val tasks = parser.parseAllTasks("(A) Task 1\n(B) Task 2\n\n(C) Task 3")
        assertEquals(3, tasks.size)
    }
}
