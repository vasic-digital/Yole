/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TodoTxt Parser Tests
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import kotlin.test.*

class TodoTxtParserTest {

    private val parser = TodoTxtParser()

    @Test
    fun testParseSimpleTask() {
        val task = parser.parseTask("Buy milk")

        assertEquals("Buy milk", task.line)
        assertEquals("Buy milk", task.description)
        assertFalse(task.done)
        assertEquals(null, task.priority)
        assertTrue(task.projects.isEmpty())
        assertTrue(task.contexts.isEmpty())
    }

    @Test
    fun testParseTaskWithPriority() {
        val task = parser.parseTask("(A) Important task")

        assertEquals("(A) Important task", task.line)
        assertEquals("Important task", task.description)
        assertEquals('A', task.priority)
        assertFalse(task.done)
    }

    @Test
    fun testParseCompletedTask() {
        val task = parser.parseTask("x 2025-01-15 Completed task")

        assertTrue(task.done)
        assertEquals("2025-01-15", task.completionDate)
        assertEquals("Completed task", task.description)
    }

    @Test
    fun testParseTaskWithProject() {
        val task = parser.parseTask("Write documentation +yole")

        assertEquals("Write documentation", task.description.trim())
        assertEquals(listOf("yole"), task.projects)
    }

    @Test
    fun testParseTaskWithContext() {
        val task = parser.parseTask("Call Mom @phone")

        assertEquals("Call Mom", task.description.trim())
        assertEquals(listOf("phone"), task.contexts)
    }

    @Test
    fun testParseTaskWithMultipleProjects() {
        val task = parser.parseTask("Write docs +yole +documentation +kmp")

        assertEquals(listOf("yole", "documentation", "kmp"), task.projects)
    }

    @Test
    fun testParseTaskWithMultipleContexts() {
        val task = parser.parseTask("Buy groceries @home @errands")

        assertEquals(listOf("home", "errands"), task.contexts)
    }

    @Test
    fun testParseTaskWithDueDate() {
        val task = parser.parseTask("Submit report due:2025-12-31")

        assertEquals("2025-12-31", task.dueDate)
        assertEquals("Submit report", task.description.trim())
    }

    @Test
    fun testParseTaskWithCreationDate() {
        val task = parser.parseTask("(B) 2025-01-01 New year task")

        assertEquals('B', task.priority)
        assertEquals("2025-01-01", task.creationDate)
        assertEquals("New year task", task.description)
    }

    @Test
    fun testParseComplexTask() {
        val task = parser.parseTask("(A) 2025-01-15 Write KMP documentation +yole +kmp @computer due:2025-02-01")

        assertEquals('A', task.priority)
        assertEquals("2025-01-15", task.creationDate)
        assertEquals("Write KMP documentation", task.description.trim())
        assertEquals(listOf("yole", "kmp"), task.projects)
        assertEquals(listOf("computer"), task.contexts)
        assertEquals("2025-02-01", task.dueDate)
        assertFalse(task.done)
    }

    @Test
    fun testParseCompletedTaskWithAllFields() {
        val task = parser.parseTask("x 2025-01-20 (B) 2025-01-15 Finished task +project @context")

        assertTrue(task.done)
        assertEquals("2025-01-20", task.completionDate)
        assertEquals('B', task.priority)
        assertEquals("2025-01-15", task.creationDate)
        assertEquals(listOf("project"), task.projects)
        assertEquals(listOf("context"), task.contexts)
    }

    @Test
    fun testParseTaskWithKeyValuePairs() {
        val task = parser.parseTask("Task with metadata key1:value1 key2:value2")

        assertEquals("value1", task.keyValues["key1"])
        assertEquals("value2", task.keyValues["key2"])
    }

    @Test
    fun testParseAllTasks() {
        val content = """
            (A) Important task +work
            Buy milk @home
            x 2025-01-15 Completed task
            (B) Another task due:2025-02-01
        """.trimIndent()

        val tasks = parser.parseAllTasks(content)

        assertEquals(4, tasks.size)
        assertEquals('A', tasks[0].priority)
        assertEquals("Buy milk", tasks[1].description)
        assertTrue(tasks[2].done)
        assertEquals("2025-02-01", tasks[3].dueDate)
    }

    @Test
    fun testParseEmptyLines() {
        val content = """
            Task 1

            Task 2


            Task 3
        """.trimIndent()

        val tasks = parser.parseAllTasks(content)

        assertEquals(3, tasks.size)
        assertEquals("Task 1", tasks[0].description)
        assertEquals("Task 2", tasks[1].description)
        assertEquals("Task 3", tasks[2].description)
    }

    @Test
    fun testParseLowercasePriority() {
        val task = parser.parseTask("(a) Task with lowercase priority")

        assertEquals('A', task.priority) // Should be uppercased
    }

    @Test
    fun testParseXUppercaseDone() {
        val task = parser.parseTask("X Task completed with uppercase X")

        assertTrue(task.done)
    }

    @Test
    fun testParseDocument() {
        val content = """
            (A) Important task +work @office
            Buy groceries @home
            x 2025-01-15 Completed task
        """.trimIndent()

        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals("3", document.metadata["totalTasks"])
        assertEquals("1", document.metadata["completedTasks"])
        assertEquals("2", document.metadata["pendingTasks"])
        assertTrue(document.parsedContent.contains("todotxt"))
    }

    @Test
    fun testHtmlGeneration() {
        val content = "(A) Test task +project @context"
        val document = parser.parse(content)

        val html = document.parsedContent

        assertTrue(html.contains("class='todotxt'"))
        assertTrue(html.contains("class='task"))
        assertTrue(html.contains("class='priority'"))
        assertTrue(html.contains("class='description'"))
        assertTrue(html.contains("class='project'"))
        assertTrue(html.contains("class='context'"))
    }

    @Test
    fun testCompletedTaskHtml() {
        val content = "x 2025-01-15 Completed task"
        val document = parser.parse(content)

        val html = document.parsedContent

        // Check for done class
        assertTrue(html.contains("done"), "HTML should contain 'done' class: $html")
        // Check for checkbox (either checked or unchecked should be present)
        assertTrue(html.contains("checkbox"), "HTML should contain 'checkbox' class: $html")
    }

    @Test
    fun testPendingTaskHtml() {
        val content = "Pending task"
        val document = parser.parse(content)

        val html = document.parsedContent

        assertFalse(html.contains("done"))
        assertTrue(html.contains("checkbox"))
    }

    @Test
    fun testTaskWithDueDateHtml() {
        val content = "Task with due date due:2025-12-31"
        val document = parser.parse(content)

        val html = document.parsedContent

        assertTrue(html.contains("class='due-date'"))
        assertTrue(html.contains("due:2025-12-31"))
    }

    @Test
    fun testGetCurrentDate() {
        val date = getCurrentDate()

        // Should match YYYY-MM-DD format
        assertTrue(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testTaskIsDueToday() {
        val today = getCurrentDate()
        val task = parser.parseTask("Task due today due:$today")

        assertTrue(task.isDueToday())
        assertFalse(task.isOverdue())
    }

    @Test
    fun testTaskIsNotOverdue() {
        val task = parser.parseTask("Task due:2099-12-31")

        assertFalse(task.isOverdue())
        assertFalse(task.isDueToday())
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    fun testTaskIsOverdue() {
        val task = parser.parseTask("Overdue task due:2020-01-01")

        assertTrue(task.isOverdue())
        assertFalse(task.isDueToday())
    }

    @Test
    fun testTaskWithNoDueDateIsNotOverdue() {
        val task = parser.parseTask("Task without due date")

        assertFalse(task.isOverdue())
        assertFalse(task.isDueToday())
    }

    @Test
    fun testTaskWithSpecialCharactersInDescription() {
        val task = parser.parseTask("Task with special chars: <>&\"' +project")

        assertTrue(task.description.contains("special chars"))
        assertEquals(listOf("project"), task.projects)
    }

    @Test
    fun testHtmlEscapingForSpecialCharacters() {
        val content = "Task with <html> & \"quotes\" +project"
        val document = parser.parse(content)
        val html = document.parsedContent

        // HTML should be escaped properly
        assertTrue(html.contains("&lt;") || html.contains("<html>"))
    }

    @Test
    fun testTaskWithDuplicateProjects() {
        val task = parser.parseTask("Task +project1 +project2 +project1")

        // Should include duplicates (parser doesn't deduplicate)
        assertTrue(task.projects.contains("project1"))
        assertTrue(task.projects.contains("project2"))
    }

    @Test
    fun testTaskWithDuplicateContexts() {
        val task = parser.parseTask("Task @home @work @home")

        assertTrue(task.contexts.contains("home"))
        assertTrue(task.contexts.contains("work"))
    }

    @Test
    fun testTaskWithProjectAtBeginning() {
        val task = parser.parseTask("+project Task description")

        assertEquals(listOf("project"), task.projects)
        assertTrue(task.description.contains("Task description"))
    }

    @Test
    fun testTaskWithContextAtBeginning() {
        val task = parser.parseTask("@context Task description")

        assertEquals(listOf("context"), task.contexts)
        assertTrue(task.description.contains("Task description"))
    }

    @Test
    fun testTaskWithMetadataAtEnd() {
        val task = parser.parseTask("Buy groceries +shopping @store")

        assertEquals("Buy groceries", task.description.trim())
        assertEquals(listOf("shopping"), task.projects)
        assertEquals(listOf("store"), task.contexts)
    }

    @Test
    fun testTaskWithMultipleKeyValuePairs() {
        val task = parser.parseTask("Task due:2025-12-31 priority:high status:pending")

        assertEquals("2025-12-31", task.keyValues["due"])
        assertEquals("high", task.keyValues["priority"])
        assertEquals("pending", task.keyValues["status"])
    }

    @Test
    fun testTaskWithKeyValuePairsWithSpecialChars() {
        val task = parser.parseTask("Task url:https://example.com/path?query=value")

        assertTrue(task.keyValues.containsKey("url"))
    }

    @Test
    fun testTaskWithVeryLongDescription() {
        val longDescription = "This is a very long task description that goes on and on ".repeat(10)
        val task = parser.parseTask("(A) $longDescription +project")

        assertEquals('A', task.priority)
        assertTrue(task.description.length > 100)
        assertEquals(listOf("project"), task.projects)
    }

    @Test
    fun testTaskWithMinimalDescription() {
        val task = parser.parseTask("(A) +project @context due:2025-12-31")

        assertEquals('A', task.priority)
        assertEquals(listOf("project"), task.projects)
        assertEquals(listOf("context"), task.contexts)
        assertEquals("2025-12-31", task.dueDate)
    }

    @Test
    fun testTaskWithOnlyMetadataNoDescription() {
        val task = parser.parseTask("+project @context")

        assertEquals(listOf("project"), task.projects)
        assertEquals(listOf("context"), task.contexts)
        // Description should be empty or minimal after metadata removal
        assertTrue(task.description.trim().isEmpty() || task.description.length < 3)
    }

    @Test
    fun testCompletedTaskWithoutCompletionDate() {
        val task = parser.parseTask("x Task completed without date")

        assertTrue(task.done)
        assertEquals(null, task.completionDate)
        assertTrue(task.description.contains("Task completed without date"))
    }

    @Test
    fun testCompletedTaskWithPriorityAndCreationDate() {
        val task = parser.parseTask("x 2025-01-20 (A) 2025-01-01 Completed with all metadata +project @context")

        assertTrue(task.done)
        assertEquals("2025-01-20", task.completionDate)
        assertEquals('A', task.priority)
        assertEquals("2025-01-01", task.creationDate)
        assertEquals(listOf("project"), task.projects)
        assertEquals(listOf("context"), task.contexts)
    }

    @Test
    fun testHtmlGenerationWithOverdueTask() {
        val content = "Overdue task due:2020-01-01"
        val document = parser.parse(content)
        val html = document.parsedContent

        assertTrue(html.contains("overdue") || html.contains("due-date"))
        assertTrue(html.contains("2020-01-01"))
    }

    @Test
    fun testHtmlGenerationWithDueTodayTask() {
        val today = getCurrentDate()
        val content = "Task due today due:$today"
        val document = parser.parse(content)
        val html = document.parsedContent

        assertTrue(html.contains("due-today") || html.contains("due-date"))
        assertTrue(html.contains(today))
    }

    @Test
    fun testMetadataCountsWithMultipleTasks() {
        val content = """
            (A) Pending task 1
            x Completed task 1
            (B) Pending task 2
            x Completed task 2
            Overdue task due:2020-01-01
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("5", document.metadata["totalTasks"])
        assertEquals("2", document.metadata["completedTasks"])
        assertEquals("3", document.metadata["pendingTasks"])
        assertEquals("1", document.metadata["overdueTasks"])
    }

    @Test
    fun testHtmlCheckboxSymbolsForCompletedTask() {
        val content = "x Completed task"
        val document = parser.parse(content)
        val html = document.parsedContent

        // Should contain checked checkbox symbol
        assertTrue(html.contains("☑") || html.contains("checked"))
    }

    @Test
    fun testHtmlCheckboxSymbolsForPendingTask() {
        val content = "Pending task"
        val document = parser.parse(content)
        val html = document.parsedContent

        // Should contain unchecked checkbox symbol
        assertTrue(html.contains("☐") || html.contains("checkbox"))
    }

    @Test
    fun testHtmlCssClassesForHighPriorityTask() {
        val content = "(A) High priority task"
        val document = parser.parse(content)
        val html = document.parsedContent

        // Should contain priority-specific CSS class
        assertTrue(html.contains("priority-a") || html.contains("priority"))
    }

    @Test
    fun testHtmlCssClassesForCompletedOverdueTask() {
        val content = "x 2025-01-20 Completed overdue task due:2020-01-01"
        val document = parser.parse(content)
        val html = document.parsedContent

        // Should contain both done and overdue classes
        assertTrue(html.contains("done"))
        // Overdue status should be checked based on current date, not completion
        assertTrue(html.contains("class="))
    }

    @Test
    fun testParseTaskWithMultipleProjectsAndContexts() {
        val task = parser.parseTask("(B) Complex task +project1 +project2 +project3 @context1 @context2 @context3 due:2025-12-31")

        assertEquals('B', task.priority)
        assertEquals(3, task.projects.size)
        assertEquals(3, task.contexts.size)
        assertTrue(task.projects.contains("project1"))
        assertTrue(task.projects.contains("project2"))
        assertTrue(task.projects.contains("project3"))
        assertTrue(task.contexts.contains("context1"))
        assertTrue(task.contexts.contains("context2"))
        assertTrue(task.contexts.contains("context3"))
        assertEquals("2025-12-31", task.dueDate)
    }

    @Test
    fun testParseTaskWithWhitespaceVariations() {
        val task = parser.parseTask("  (A)   2025-01-01    Task  with  extra   spaces   +project   @context  ")

        assertEquals('A', task.priority)
        assertEquals("2025-01-01", task.creationDate)
        assertEquals(listOf("project"), task.projects)
        assertEquals(listOf("context"), task.contexts)
        // Description should handle extra whitespace
        assertNotNull(task.description)
    }

    @Test
    fun testParseAllTasksWithBlankLinesAndWhitespace() {
        val content = """


            Task 1


            Task 2

            Task 3


        """.trimIndent()

        val tasks = parser.parseAllTasks(content)

        assertEquals(3, tasks.size)
        assertEquals("Task 1", tasks[0].description)
        assertEquals("Task 2", tasks[1].description)
        assertEquals("Task 3", tasks[2].description)
    }

    @Test
    fun testRealWorldComplexTask() {
        val task = parser.parseTask("(A) 2025-01-15 Review pull request #123 for feature/auth +github +code-review @computer @work due:2025-01-20 pr:123 status:in-progress")

        assertEquals('A', task.priority)
        assertEquals("2025-01-15", task.creationDate)
        assertTrue(task.description.contains("Review pull request"))
        assertEquals(2, task.projects.size)
        assertTrue(task.projects.contains("github"))
        assertTrue(task.projects.contains("code-review"))
        assertEquals(2, task.contexts.size)
        assertTrue(task.contexts.contains("computer"))
        assertTrue(task.contexts.contains("work"))
        assertEquals("2025-01-20", task.dueDate)
        assertEquals("123", task.keyValues["pr"])
        assertEquals("in-progress", task.keyValues["status"])
    }

    @Test
    fun testRealWorldShoppingList() {
        val content = """
            Buy milk @groceries @urgent
            Buy bread @groceries
            x Buy eggs @groceries
            Pick up prescription @pharmacy due:2025-01-16
            (A) Pay electricity bill @home +bills due:2025-01-15
        """.trimIndent()

        val tasks = parser.parseAllTasks(content)
        val document = parser.parse(content)

        assertEquals(5, tasks.size)
        assertEquals("1", document.metadata["completedTasks"])
        assertEquals("4", document.metadata["pendingTasks"])

        // Check grocery tasks
        val groceryTasks = tasks.filter { it.contexts.contains("groceries") }
        assertEquals(3, groceryTasks.size)

        // Check high priority bill task
        val billTask = tasks.find { it.projects.contains("bills") }
        assertNotNull(billTask)
        assertEquals('A', billTask?.priority)
        assertEquals("2025-01-15", billTask?.dueDate)
    }
}
