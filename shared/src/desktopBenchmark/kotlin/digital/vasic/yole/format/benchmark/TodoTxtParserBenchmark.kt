/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Todo.txt Parser Performance Benchmarks
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.todotxt.TodoTxtParser
import kotlinx.benchmark.*

/**
 * Performance benchmarks for TodoTxtParser.
 *
 * This benchmark suite measures the performance of Todo.txt parsing across
 * different task list sizes and complexity levels to establish baselines and
 * track optimization improvements.
 *
 * Benchmark Scenarios:
 * - Small list (10 tasks) - Simple task list
 * - Medium list (100 tasks) - Typical todo.txt file
 * - Large list (1000 tasks) - Comprehensive task management
 * - Complex list - Tasks with all metadata (priority, dates, tags)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class TodoTxtParserBenchmark {

    private lateinit var parser: TodoTxtParser

    // Test content of various sizes
    private lateinit var smallContent: String      // 10 tasks
    private lateinit var mediumContent: String     // 100 tasks
    private lateinit var largeContent: String      // 1000 tasks
    private lateinit var complexContent: String    // Complex metadata

    @Setup
    fun setup() {
        parser = TodoTxtParser()

        // Small content: 10 simple tasks
        smallContent = buildString {
            for (i in 1..10) {
                append("(${('A'.code + (i % 3)).toChar()}) Task $i description\n")
            }
        }

        // Medium content: 100 tasks with basic metadata
        mediumContent = buildString {
            for (i in 1..100) {
                val priority = ('A'.code + (i % 5)).toChar()
                val done = i % 3 == 0
                val prefix = if (done) "x " else ""
                val date = "2025-${String.format("%02d", (i % 12) + 1)}-${String.format("%02d", (i % 28) + 1)}"

                append("${prefix}($priority) $date Task $i description ")
                append("+Project${i % 5} @context${i % 3}\n")
            }
        }

        // Large content: 1000 tasks for stress testing
        largeContent = buildString {
            for (i in 1..1000) {
                val priority = ('A'.code + (i % 10)).toChar()
                val done = i % 4 == 0
                val prefix = if (done) "x " else ""
                val creationDate = "2025-${String.format("%02d", (i % 12) + 1)}-${String.format("%02d", (i % 28) + 1)}"
                val dueDate = "2025-${String.format("%02d", ((i + 1) % 12) + 1)}-${String.format("%02d", ((i + 7) % 28) + 1)}"

                append("${prefix}($priority) $creationDate Task $i description ")
                append("+Project${i % 10} @context${i % 5} due:$dueDate\n")
            }
        }

        // Complex content: Tasks with all possible metadata
        complexContent = """
            (A) 2025-01-15 High priority task +WorkProject @office due:2025-01-20 rec:+1w
            x 2025-01-14 2025-01-10 (B) Completed task with completion date +PersonalProject @home
            (C) 2025-01-12 Task with multiple projects +Project1 +Project2 +Project3 @context1 @context2
            Task without priority but with metadata +UrgentProject @email due:2025-01-18
            x (A) Completed high priority task +CompletedProject @done
            (B) 2025-01-11 Task with URL https://example.com/task @online +WebProject
            (D) 2025-01-13 Task with custom key-value pairs +CustomProject @office id:12345 category:bug
            x 2025-01-15 2025-01-14 (C) Completed task with full metadata +FinishedProject @work priority:high
            (A) Important task with note: Remember to call @phone +FamilyProject due:2025-01-16
            2025-01-10 Task with creation date only +NewProject @planning
            (E) Low priority task with long description and many words +Documentation @writing
            x Completed task without dates +ArchivedProject @completed
            (B) Task with special characters: tést, café, naïve +International @unicode
            (A) 2025-01-14 Urgent task with threshold t:2025-01-15 +ThresholdProject @calendar
            x 2025-01-16 2025-01-15 (A) Recently completed urgent task +RecentProject @finished
        """.trimIndent()
    }

    /**
     * Benchmark parsing small Todo.txt list (10 tasks).
     *
     * Target: < 5ms for 10 tasks
     */
    @Benchmark
    fun parseSmallList(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallContent, mapOf("filename" to "small-todo.txt"))
    }

    /**
     * Benchmark parsing medium Todo.txt list (100 tasks).
     *
     * Target: < 20ms for 100 tasks
     */
    @Benchmark
    fun parseMediumList(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumContent, mapOf("filename" to "medium-todo.txt"))
    }

    /**
     * Benchmark parsing large Todo.txt list (1000 tasks).
     *
     * Target: < 150ms for 1000 tasks
     */
    @Benchmark
    fun parseLargeList(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeContent, mapOf("filename" to "large-todo.txt"))
    }

    /**
     * Benchmark parsing complex Todo.txt with full metadata.
     *
     * Target: < 10ms for complex parsing
     */
    @Benchmark
    fun parseComplexList(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexContent, mapOf("filename" to "complex-todo.txt"))
    }

    /**
     * Benchmark HTML conversion for Todo.txt.
     *
     * Target: < 15ms for HTML conversion
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumContent, mapOf("filename" to "medium-todo.txt"))
        return parser.toHtml(document, lightMode = true)
    }

    /**
     * Benchmark validation of Todo.txt syntax.
     *
     * Target: < 10ms for validation
     */
    @Benchmark
    fun validateDocument(): List<String> {
        return parser.validate(mediumContent)
    }
}
