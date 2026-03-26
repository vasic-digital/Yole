/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for TaskpaperParser
 *
 *########################################################*/
package digital.vasic.yole.format.taskpaper

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserInitializer
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TaskpaperParser].
 *
 * Covers: projects (ending with :), tasks (starting with -), notes,
 * tags (@tag(value)), @done/@today tags, nested items, empty input,
 * validation, deeply nested structures, Unicode.
 *
 * Total: ~45 tests
 */
class TaskpaperParserUnitTest {

    private val parser = TaskpaperParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    // ==================== Supported format ====================

    @Test
    fun SupportedFormatIsTaskpaper() {
        assertEquals(TextFormat.ID_TASKPAPER, parser.supportedFormat.id)
    }

    @Test
    fun SupportedFormatExtensionsIncludeTaskpaper() {
        assertTrue(
            parser.supportedFormat.extensions.contains(".taskpaper") ||
            TaskpaperParser.EXTENSIONS.contains(".taskpaper")
        )
    }

    @Test
    fun CanParseReturnsTrueForTaskpaper() {
        val format = FormatRegistry.getById(TextFormat.ID_TASKPAPER)!!
        assertTrue(parser.canParse(format))
    }

    @Test
    fun CanParseReturnsFalseForMarkdown() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        assertFalse(parser.canParse(format))
    }

    // ==================== Empty / minimal input ====================

    @Test
    fun ParseEmptyStringReturnsDocument() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("", doc.rawContent)
    }

    @Test
    fun ParseEmptyStringMetadataTasksZero() {
        val doc = parser.parse("")
        assertEquals("0", doc.metadata["tasks"])
    }

    @Test
    fun ParseEmptyStringMetadataProjectsZero() {
        val doc = parser.parse("")
        assertEquals("0", doc.metadata["projects"])
    }

    // ==================== Projects ====================

    @Test
    fun ParseSimpleProjectLine() {
        val doc = parser.parse("My Project:")
        assertEquals("1", doc.metadata["projects"])
    }

    @Test
    fun ParseProjectWithTags() {
        val doc = parser.parse("Work Project: @active")
        assertEquals("1", doc.metadata["projects"])
    }

    @Test
    fun ParseMultipleProjects() {
        val content = "Project A:\n\nProject B:\n\nProject C:"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["projects"])
    }

    @Test
    fun ParseProjectHtmlContainsProjectClass() {
        val doc = parser.parse("My Project:")
        assertTrue(doc.parsedContent.contains("taskpaper-project"))
    }

    // ==================== Tasks ====================

    @Test
    fun ParseSimpleTask() {
        val doc = parser.parse("Project:\n\t- Do something")
        assertEquals("1", doc.metadata["tasks"])
    }

    @Test
    fun ParseMultipleTasks() {
        val content = "Project:\n\t- Task one\n\t- Task two\n\t- Task three"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["tasks"])
    }

    @Test
    fun ParseTaskHtmlContainsTaskClass() {
        val doc = parser.parse("Project:\n\t- Do something")
        assertTrue(doc.parsedContent.contains("taskpaper-task"))
    }

    @Test
    fun ParseTaskWithDoneTagIsMarkedDone() {
        val doc = parser.parse("Project:\n\t- Finished task @done")
        assertEquals("1", doc.metadata["doneTasks"])
    }

    @Test
    fun ParseTaskWithDoneTagAndDateIsMarkedDone() {
        val doc = parser.parse("Project:\n\t- Completed @done(2025-01-15)")
        assertEquals("1", doc.metadata["doneTasks"])
    }

    @Test
    fun ParseDoneTaskHtmlContainsDoneClass() {
        val doc = parser.parse("Project:\n\t- Done task @done")
        assertTrue(doc.parsedContent.contains("taskpaper-task-done"))
    }

    @Test
    fun ParseTaskWithTodayTag() {
        val doc = parser.parse("Project:\n\t- Today's task @today")
        assertEquals("1", doc.metadata["todayTasks"])
    }

    // ==================== Notes ====================

    @Test
    fun ParseNoteLineReturnsNoteCount() {
        val doc = parser.parse("A standalone note line.")
        assertEquals("1", doc.metadata["notes"])
    }

    @Test
    fun ParseNoteHtmlContainsNoteClass() {
        val doc = parser.parse("This is a note.")
        assertTrue(doc.parsedContent.contains("taskpaper-note"))
    }

    // ==================== Tags ====================

    @Test
    fun ParseTagWithNoValue() {
        val content = "Project:\n\t- Task @important"
        val doc = parser.parse(content)
        // Task metadata should reflect tag presence implicitly
        assertEquals("1", doc.metadata["tasks"])
    }

    @Test
    fun ParseTagWithValue() {
        val content = "Project:\n\t- Task @due(2025-12-31)"
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["tasks"])
    }

    @Test
    fun TaskpaperItemHasTagMethod() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task text @done",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("done" to "")
        )
        assertTrue(item.hasTag("done"))
        assertFalse(item.hasTag("today"))
    }

    @Test
    fun TaskpaperItemGetTagValue() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task @due(2025-01-01)",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("due" to "2025-01-01")
        )
        assertEquals("2025-01-01", item.getDueDate())
    }

    @Test
    fun TaskpaperItemIsDone() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Done task @done",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("done" to "")
        )
        assertTrue(item.isDone())
    }

    @Test
    fun TaskpaperItemIsToday() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Today task @today",
            indentLevel = 0,
            lineNumber = 1,
            tags = mapOf("today" to "")
        )
        assertTrue(item.isToday())
    }

    @Test
    fun TaskpaperItemGetTagValueReturnsNullForMissingTag() {
        val item = TaskpaperItem(
            type = TaskpaperItemType.TASK,
            content = "Task",
            indentLevel = 0,
            lineNumber = 1,
            tags = emptyMap()
        )
        assertNull(item.getDueDate())
    }

    // ==================== Nested items ====================

    @Test
    fun ParseNestedTaskUnderProject() {
        val content = "Project:\n\t- Task\n\t\t- Sub-task"
        val doc = parser.parse(content)
        assertTrue(doc.metadata["tasks"]!!.toInt() >= 1)
    }

    @Test
    fun ParseDeeplyNestedStructure() {
        val content = buildString {
            appendLine("Top Project:")
            appendLine("\tSub Project A:")
            appendLine("\t\t- Task 1")
            appendLine("\t\t- Task 2")
            appendLine("\tSub Project B:")
            appendLine("\t\t- Task 3")
        }
        val doc = parser.parse(content)
        assertNotNull(doc)
        assertTrue(doc.metadata["tasks"]!!.toInt() >= 3)
    }

    // ==================== Item types ====================

    @Test
    fun ParseEmptyLineIsEmptyType() {
        // An empty line in a document should be recognized (no crash)
        val doc = parser.parse("Project:\n\n\t- Task")
        assertNotNull(doc)
    }

    @Test
    fun ParseItemTypeEnumValues() {
        // Ensure enum values exist
        assertEquals(TaskpaperItemType.PROJECT, TaskpaperItemType.valueOf("PROJECT"))
        assertEquals(TaskpaperItemType.TASK, TaskpaperItemType.valueOf("TASK"))
        assertEquals(TaskpaperItemType.NOTE, TaskpaperItemType.valueOf("NOTE"))
        assertEquals(TaskpaperItemType.EMPTY, TaskpaperItemType.valueOf("EMPTY"))
    }

    // ==================== Validation ====================

    @Test
    fun ValidateWellFormedContentNoErrors() {
        val content = "Project:\n\t- Task one\n\t- Task two @done"
        val errors = parser.validate(content)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun ValidateMalformedTaskMarkerReturnsError() {
        val content = "Project:\n\t-Task without space"
        val errors = parser.validate(content)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Task marker") })
    }

    @Test
    fun ValidateUnclosedTagParameterReturnsError() {
        val content = "Project:\n\t- Task @due(2025-01-01"
        val errors = parser.validate(content)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed") })
    }

    @Test
    fun ValidateEmptyContentNoErrors() {
        val errors = parser.validate("")
        assertTrue(errors.isEmpty())
    }

    // ==================== Metadata ====================

    @Test
    fun ParseMetadataContainsLineCount() {
        val doc = parser.parse("Line 1\nLine 2\nLine 3")
        assertEquals("3", doc.metadata["lines"])
    }

    @Test
    fun ParseMetadataContainsExtensionFromOptions() {
        val doc = parser.parse("Project:", mapOf("filename" to "tasks.taskpaper"))
        assertEquals(".taskpaper", doc.metadata["extension"])
    }

    @Test
    fun ParsedDocumentFormatMatchesParser() {
        val doc = parser.parse("Test")
        assertEquals(TextFormat.ID_TASKPAPER, doc.format.id)
    }

    @Test
    fun RawContentIsPreserved() {
        val content = "Project:\n\t- Task one\n\t- Task two"
        val doc = parser.parse(content)
        assertEquals(content, doc.rawContent)
    }

    // ==================== HTML output ====================

    @Test
    fun ToHtmlReturnsParsedContent() {
        val doc = parser.parse("Project:\n\t- Task")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("taskpaper"))
    }

    @Test
    fun ToHtmlContainsStyleBlock() {
        val doc = parser.parse("Project:\n\t- Task")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    // ==================== Unicode ====================

    @Test
    fun ParseUnicodeProjectName() {
        val doc = parser.parse("Proyecto en Español:")
        assertEquals("1", doc.metadata["projects"])
    }

    @Test
    fun ParseUnicodeTaskContent() {
        val doc = parser.parse("Projet:\n\t- Tâche avec accents éàü")
        assertEquals("1", doc.metadata["tasks"])
    }

    @Test
    fun ParseChineseCharactersInContent() {
        val content = "工作项目:\n\t- 完成任务"
        val doc = parser.parse(content)
        assertTrue(doc.rawContent.contains("工作项目"))
    }

    // ==================== Large documents ====================

    @Test
    fun ParseDocumentWith100Tasks() {
        val sb = StringBuilder()
        sb.appendLine("Big Project:")
        repeat(100) { i ->
            sb.appendLine("\t- Task number $i")
        }
        val doc = parser.parse(sb.toString())
        assertEquals("100", doc.metadata["tasks"])
    }

    @Test
    fun ParseDocumentWith10Projects() {
        val sb = StringBuilder()
        repeat(10) { i ->
            sb.appendLine("Project $i:")
            sb.appendLine("\t- Task for project $i")
            sb.appendLine()
        }
        val doc = parser.parse(sb.toString())
        assertEquals("10", doc.metadata["projects"])
    }
}
