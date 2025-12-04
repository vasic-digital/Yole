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
 * different task list sizes and complexity levels. Todo.txt is a critical
 * format for the application, so performance is essential.
 *
 * Benchmark Scenarios:
 * - Small list (10 tasks) - Quick todo list
 * - Medium list (100 tasks) - Typical usage
 * - Large list (1000 tasks) - Power user scenario
 * - Complex tasks - With priorities, dates, projects, contexts
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class TodoTxtParserBenchmark {

    private lateinit var parser: TodoTxtParser

    private lateinit var smallTodo: String      // 10 tasks
    private lateinit var mediumTodo: String     // 100 tasks
    private lateinit var largeTodo: String      // 1000 tasks
    private lateinit var complexTodo: String    // Complex tasks

    @Setup
    fun setup() {
        parser = TodoTxtParser()

        // Small todo list: 10 tasks
        smallTodo = """
            (A) Call Mom
            (B) Complete project proposal +work
            x 2025-01-10 Submit tax documents
            (C) 2025-01-15 Schedule dentist appointment @phone
            Buy groceries +personal
            (A) Review PR #123 +work @computer
            x 2025-01-09 Update documentation
            Plan weekend trip +personal
            (B) Fix bug in parser +work @coding
            Read chapter 5 +learning
        """.trimIndent()

        // Medium todo list: 100 tasks
        mediumTodo = buildString {
            for (i in 1..100) {
                val priority = when (i % 5) {
                    0 -> "(A) "
                    1 -> "(B) "
                    2 -> "(C) "
                    else -> ""
                }
                val completed = if (i % 3 == 0) "x 2025-01-${(i % 28 + 1).toString().padStart(2, '0')} " else ""
                val date = if (i % 4 == 0) "2025-01-${(i % 28 + 1).toString().padStart(2, '0')} " else ""
                val project = "+project${i % 5}"
                val context = "@context${i % 3}"

                appendLine("$completed${priority}${date}Task $i description $project $context")
            }
        }

        // Large todo list: 1000 tasks
        largeTodo = buildString {
            for (i in 1..1000) {
                val priority = when {
                    i % 10 == 0 -> "(A) "
                    i % 10 == 1 -> "(B) "
                    i % 10 == 2 -> "(C) "
                    else -> ""
                }
                val completed = if (i % 5 == 0) {
                    "x 2025-01-${(i % 28 + 1).toString().padStart(2, '0')} "
                } else ""
                val date = if (i % 7 == 0) {
                    "2025-01-${(i % 28 + 1).toString().padStart(2, '0')} "
                } else ""

                val numProjects = i % 3 + 1
                val projects = (1..numProjects).joinToString(" ") { "+proj${it}" }

                val numContexts = i % 2 + 1
                val contexts = (1..numContexts).joinToString(" ") { "@ctx${it}" }

                appendLine("$completed${priority}${date}Task $i with multiple metadata $projects $contexts")
            }
        }

        // Complex todo list with rich metadata
        complexTodo = """
            (A) 2025-01-20 Complete critical bug fix +work +urgent @coding @review due:2025-01-22 t:2025-01-20
            x 2025-01-10 (B) 2025-01-05 Refactor authentication module +work +refactor @coding review:required
            (B) 2025-01-18 Write comprehensive tests for parser +work +testing @coding @documentation
            x 2025-01-12 (A) 2025-01-10 Deploy hotfix to production +work +deployment @devops @urgent
            (C) Schedule team meeting for Q1 planning +work +planning @meeting @management
            x 2025-01-11 Update project dependencies +work +maintenance @coding
            (A) 2025-01-19 Review and merge PR #456 +work +code-review @github priority:high
            Design new feature mockups +work +design @figma @ux
            x 2025-01-09 (C) 2025-01-08 Research performance optimization techniques +work +research @learning
            (B) 2025-01-21 Prepare presentation for stakeholders +work +presentation @powerpoint due:2025-01-25
            Call dentist for annual checkup +personal @phone @health
            x 2025-01-13 Buy birthday gift for Sarah +personal @shopping
            (A) Finish reading "Clean Code" chapter 7-10 +personal +learning @reading
            Plan vacation for summer +personal +travel @research
            x 2025-01-14 (B) 2025-01-12 Organize home office +personal @home
            (C) 2025-01-22 File tax documents +personal +admin @paperwork due:2025-04-15
            Start gym membership +personal +health @fitness
            x 2025-01-15 Clean garage +personal @home @weekend
            (B) 2025-01-20 Meal prep for the week +personal @cooking @health
            Research investment options +personal +finance @research @learning
        """.trimIndent()
    }

    /**
     * Benchmark parsing small todo list (10 tasks).
     *
     * Target: < 5ms for 10 tasks
     */
    @Benchmark
    fun parseSmallTodo(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallTodo, mapOf("filename" to "small.txt"))
    }

    /**
     * Benchmark parsing medium todo list (100 tasks).
     *
     * Target: < 20ms for 100 tasks
     */
    @Benchmark
    fun parseMediumTodo(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumTodo, mapOf("filename" to "medium.txt"))
    }

    /**
     * Benchmark parsing large todo list (1000 tasks).
     *
     * Target: < 200ms for 1000 tasks
     */
    @Benchmark
    fun parseLargeTodo(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeTodo, mapOf("filename" to "large.txt"))
    }

    /**
     * Benchmark parsing complex todo list with rich metadata.
     *
     * Target: < 15ms for complex parsing
     */
    @Benchmark
    fun parseComplexTodo(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexTodo, mapOf("filename" to "complex.txt"))
    }

    /**
     * Benchmark HTML conversion (task list rendering).
     *
     * Target: < 30ms for HTML conversion
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumTodo, mapOf("filename" to "medium.txt"))
        return parser.toHtml(document, lightMode = true)
    }

    /**
     * Benchmark validation (syntax checking).
     *
     * Target: < 10ms for validation
     */
    @Benchmark
    fun validateTodo(): List<String> {
        return parser.validate(mediumTodo)
    }
}
