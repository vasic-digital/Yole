/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Parser Performance Measurement Tests
 *
 *########################################################*/
package digital.vasic.yole.format.performance

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.Test
import kotlin.time.measureTime
import kotlin.time.Duration

/**
 * Performance measurement tests for text format parsers.
 *
 * These tests measure parsing performance across different document sizes
 * to establish baseline metrics and identify optimization opportunities.
 *
 * Run with: `./gradlew :shared:desktopTest --tests "*ParserPerformanceTest*"`
 */
class ParserPerformanceTest {

    companion object {
        const val WARMUP_ITERATIONS = 3
        const val MEASUREMENT_ITERATIONS = 5
    }

    @Test
    fun measureMarkdownParserPerformance() {
        println("\n=== Markdown Parser Performance ===\n")

        val parser = MarkdownParser()

        // Small document (~1KB)
        val smallContent = """
            # Hello World
            This is a **simple** Markdown document.
            ## Features
            - Item 1
            - Item 2

            ```kotlin
            fun main() = println("Hello")
            ```
        """.trimIndent()

        measureAndReport("Markdown Small (1KB)", smallContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }

        // Medium document (~10KB)
        val mediumContent = buildString {
            append("# Project Documentation\n\n")
            repeat(20) { section ->
                append("## Section $section\n\n")
                append("This is detailed content for section $section. ")
                append("It includes **bold**, *italic*, and `code`.\n\n")
                append("- List item 1\n")
                append("- List item 2\n")
                append("- List item 3\n\n")
                append("```kotlin\nfun example$section() {}\n```\n\n")
            }
        }

        measureAndReport("Markdown Medium (10KB)", mediumContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }

        // Large document (~50KB)
        val largeContent = buildString {
            repeat(100) { section ->
                append("## Section $section\n\n")
                append("Content for section $section with **formatting**.\n\n")
                append("| Column 1 | Column 2 |\n")
                append("|----------|----------|\n")
                append("| Cell 1   | Cell 2   |\n\n")
            }
        }

        measureAndReport("Markdown Large (50KB)", largeContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }
    }

    @Test
    fun measureCsvParserPerformance() {
        println("\n=== CSV Parser Performance ===\n")

        val parser = CsvParser()

        // Small CSV (10 rows)
        val smallCsv = buildString {
            append("Name,Age,Email\n")
            repeat(10) { i ->
                append("User$i,$i,user$i@example.com\n")
            }
        }

        measureAndReport("CSV Small (10 rows)", smallCsv) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }

        // Medium CSV (100 rows)
        val mediumCsv = buildString {
            append("ID,Name,Email,Department\n")
            repeat(100) { i ->
                append("$i,Employee$i,emp$i@company.com,Dept${i % 5}\n")
            }
        }

        measureAndReport("CSV Medium (100 rows)", mediumCsv) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }

        // Large CSV (1000 rows)
        val largeCsv = buildString {
            append("ID,Name,Email,Phone,Address\n")
            repeat(1000) { i ->
                append("$i,User$i,user$i@domain.com,555-$i,${i} Main St\n")
            }
        }

        measureAndReport("CSV Large (1000 rows)", largeCsv) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }
    }

    @Test
    fun measureTodoTxtParserPerformance() {
        println("\n=== Todo.txt Parser Performance ===\n")

        val parser = TodoTxtParser()

        // Small list (10 tasks)
        val smallTodo = buildString {
            repeat(10) { i ->
                appendLine("(A) Task $i +project @context")
            }
        }

        measureAndReport("Todo.txt Small (10 tasks)", smallTodo) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Medium list (100 tasks)
        val mediumTodo = buildString {
            repeat(100) { i ->
                val priority = when (i % 3) {
                    0 -> "(A) "
                    1 -> "(B) "
                    else -> ""
                }
                appendLine("${priority}Task $i +project${i % 3} @context${i % 2}")
            }
        }

        measureAndReport("Todo.txt Medium (100 tasks)", mediumTodo) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Large list (1000 tasks)
        val largeTodo = buildString {
            repeat(1000) { i ->
                appendLine("Task $i with metadata +proj @ctx")
            }
        }

        measureAndReport("Todo.txt Large (1000 tasks)", largeTodo) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }
    }

    @Test
    fun measurePlaintextParserPerformance() {
        println("\n=== Plaintext Parser Performance (Baseline) ===\n")

        val parser = PlaintextParser()

        // Small text (1KB)
        val smallText = buildString {
            repeat(20) { i ->
                appendLine("Line $i of plaintext content.")
            }
        }

        measureAndReport("Plaintext Small (1KB)", smallText) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Medium text (10KB)
        val mediumText = buildString {
            repeat(200) { i ->
                appendLine("Line $i: Lorem ipsum dolor sit amet.")
            }
        }

        measureAndReport("Plaintext Medium (10KB)", mediumText) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Large text (100KB)
        val largeText = buildString {
            repeat(2000) { i ->
                appendLine("Line $i: This is a longer line with more content.")
            }
        }

        measureAndReport("Plaintext Large (100KB)", largeText) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }
    }

    /**
     * Measure execution time of a parser operation.
     *
     * Performs warmup iterations followed by measurement iterations,
     * then reports average, min, and max times.
     */
    private fun measureAndReport(
        testName: String,
        content: String,
        operation: (String) -> Any
    ) {
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            operation(content)
        }

        // Measurement
        val times = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                operation(content)
            }
            times.add(duration)
        }

        // Calculate statistics
        val avg = times.map { it.inWholeMilliseconds }.average()
        val min = times.minOf { it.inWholeMilliseconds }
        val max = times.maxOf { it.inWholeMilliseconds }
        val size = content.length

        // Report
        println("$testName:")
        println("  Size: ${size} chars")
        println("  Avg:  ${String.format("%.2f", avg)} ms")
        println("  Min:  $min ms")
        println("  Max:  $max ms")
        println("  Iterations: $MEASUREMENT_ITERATIONS (after $WARMUP_ITERATIONS warmup)")
        println()
    }
}
