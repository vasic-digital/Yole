/*
 * SPDX-FileCopyrightText: 2025 Marko Vasic <contact@vasic.digital>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package digital.vasic.yole.format.memory

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.Test

/**
 * Memory profiling tests for parser implementations.
 *
 * Measures heap memory usage for various document sizes to establish
 * baseline memory consumption and identify potential memory issues.
 *
 * Approach:
 * - Force GC before measurement for accurate baseline
 * - Measure memory before and after parsing
 * - Calculate memory delta and per-byte overhead
 * - Test with small, medium, and large documents
 *
 * Target Memory Usage:
 * - Small documents (< 10KB): < 1MB heap usage
 * - Medium documents (10-100KB): < 5MB heap usage
 * - Large documents (100KB-1MB): < 20MB heap usage
 * - Memory efficiency: < 10x document size
 */
class ParserMemoryTest {

    companion object {
        const val WARMUP_ITERATIONS = 3
        const val MEASUREMENT_ITERATIONS = 5
    }

    /**
     * Measure memory usage for a parsing operation.
     *
     * @param testName Name of the test for reporting
     * @param content Document content to parse
     * @param operation Parsing operation to measure
     * @return Memory usage statistics
     */
    private fun measureMemory(
        testName: String,
        content: String,
        operation: (String) -> Any
    ): MemoryStats {
        val runtime = Runtime.getRuntime()
        val measurements = mutableListOf<Long>()

        // Warmup iterations
        repeat(WARMUP_ITERATIONS) {
            operation(content)
        }

        // Force GC before measurement
        System.gc()
        Thread.sleep(100)

        // Measurement iterations
        repeat(MEASUREMENT_ITERATIONS) {
            // Force GC to get clean baseline
            System.gc()
            Thread.sleep(50)

            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

            // Parse document
            val result = operation(content)

            // Keep result in scope to prevent premature GC
            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()

            // Calculate delta
            val memoryUsed = memoryAfter - memoryBefore
            measurements.add(memoryUsed)

            // Keep result referenced
            result.toString()
        }

        // Calculate statistics
        val avgBytes = measurements.average().toLong()
        val minBytes = measurements.minOrNull() ?: 0L
        val maxBytes = measurements.maxOrNull() ?: 0L

        val stats = MemoryStats(
            testName = testName,
            contentSize = content.length,
            avgMemoryBytes = avgBytes,
            minMemoryBytes = minBytes,
            maxMemoryBytes = maxBytes
        )

        // Report
        stats.report()

        return stats
    }

    @Test
    fun measureMarkdownParserMemory() {
        println("\n=== Markdown Parser Memory Usage ===\n")
        val parser = MarkdownParser()

        // Small document (~1KB)
        val smallContent = """
            # Hello World

            This is a **simple** Markdown document with some basic formatting.

            ## Features

            - Bullet points
            - **Bold text**
            - *Italic text*
            - [Links](https://example.com)

            ```kotlin
            fun example() {
                println("Code block")
            }
            ```
        """.trimIndent()

        measureMemory("Markdown Small (1KB)", smallContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }

        // Medium document (~10KB)
        val mediumContent = buildString {
            append("# Large Markdown Document\n\n")
            repeat(50) { section ->
                append("## Section $section\n\n")
                append("This is paragraph text with **bold** and *italic* formatting. ")
                append("Here's a [link](https://example.com/$section) to something.\n\n")
                append("- List item 1\n")
                append("- List item 2\n")
                append("- List item 3\n\n")
                append("```kotlin\n")
                append("fun section$section() {\n")
                append("    println(\"Section $section\")\n")
                append("}\n")
                append("```\n\n")
            }
        }

        measureMemory("Markdown Medium (10KB)", mediumContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }

        // Large document (~50KB)
        val largeContent = buildString {
            append("# Very Large Markdown Document\n\n")
            repeat(200) { section ->
                append("## Section $section\n\n")
                append("This is paragraph text with **bold** and *italic* formatting. ")
                append("Here's a [link](https://example.com/$section) to something. ")
                append("More text to make this section larger and test memory usage.\n\n")
                append("- List item 1\n")
                append("- List item 2\n")
                append("- List item 3\n")
                append("- List item 4\n")
                append("- List item 5\n\n")
                append("```kotlin\n")
                append("fun section$section() {\n")
                append("    val data = listOf(1, 2, 3, 4, 5)\n")
                append("    data.forEach { println(it) }\n")
                append("}\n")
                append("```\n\n")
            }
        }

        measureMemory("Markdown Large (50KB)", largeContent) { content ->
            parser.parse(content, mapOf("filename" to "test.md"))
        }
    }

    @Test
    fun measureCsvParserMemory() {
        println("\n=== CSV Parser Memory Usage ===\n")
        val parser = CsvParser()

        // Small CSV (10 rows)
        val smallContent = buildString {
            append("Name,Email,Age,City,Country\n")
            repeat(10) { i ->
                append("User$i,user$i@example.com,${20 + i},City$i,Country$i\n")
            }
        }

        measureMemory("CSV Small (10 rows)", smallContent) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }

        // Medium CSV (100 rows)
        val mediumContent = buildString {
            append("Name,Email,Age,City,Country\n")
            repeat(100) { i ->
                append("User$i,user$i@example.com,${20 + i},City$i,Country$i\n")
            }
        }

        measureMemory("CSV Medium (100 rows)", mediumContent) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }

        // Large CSV (1000 rows)
        val largeContent = buildString {
            append("Name,Email,Age,City,Country,Address,Phone,Department,Salary,StartDate\n")
            repeat(1000) { i ->
                append("User$i,user$i@example.com,${20 + i},City$i,Country$i,")
                append("Address $i Street,+1-555-000$i,Department${i % 10},${50000 + i * 100},2020-01-01\n")
            }
        }

        measureMemory("CSV Large (1000 rows)", largeContent) { content ->
            parser.parse(content, mapOf("filename" to "test.csv"))
        }
    }

    @Test
    fun measureTodoTxtParserMemory() {
        println("\n=== Todo.txt Parser Memory Usage ===\n")
        val parser = TodoTxtParser()

        // Small todo list (10 tasks)
        val smallContent = buildString {
            repeat(10) { i ->
                val priority = ('A'..'C').random()
                append("($priority) Task $i @context${i % 3} +project${i % 2} due:2025-12-31\n")
            }
        }

        measureMemory("Todo.txt Small (10 tasks)", smallContent) { content ->
            parser.parse(content, mapOf("filename" to "todo.txt"))
        }

        // Medium todo list (100 tasks)
        val mediumContent = buildString {
            repeat(100) { i ->
                val priority = ('A'..'E').random()
                append("($priority) Task $i with longer description @context${i % 5} +project${i % 3} due:2025-12-31\n")
            }
        }

        measureMemory("Todo.txt Medium (100 tasks)", mediumContent) { content ->
            parser.parse(content, mapOf("filename" to "todo.txt"))
        }

        // Large todo list (1000 tasks)
        val largeContent = buildString {
            repeat(1000) { i ->
                val priority = if (i % 3 == 0) "(${'A' + (i % 5)}) " else ""
                val contexts = (1..3).joinToString(" ") { "@context${i % 10}_$it" }
                val projects = (1..2).joinToString(" ") { "+project${i % 5}_$it" }
                append("${priority}Task $i with detailed description $contexts $projects due:2025-12-31\n")
            }
        }

        measureMemory("Todo.txt Large (1000 tasks)", largeContent) { content ->
            parser.parse(content, mapOf("filename" to "todo.txt"))
        }
    }

    @Test
    fun measurePlaintextParserMemory() {
        println("\n=== Plaintext Parser Memory Usage (Baseline) ===\n")
        val parser = PlaintextParser()

        // Small plaintext (~1KB)
        val smallContent = buildString {
            repeat(20) { i ->
                append("This is line $i of plaintext content.\n")
            }
        }

        measureMemory("Plaintext Small (1KB)", smallContent) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Medium plaintext (~10KB)
        val mediumContent = buildString {
            repeat(200) { i ->
                append("This is line $i of plaintext content with some additional text to make it larger.\n")
            }
        }

        measureMemory("Plaintext Medium (10KB)", mediumContent) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }

        // Large plaintext (~100KB)
        val largeContent = buildString {
            repeat(2000) { i ->
                append("This is line $i of plaintext content with some additional text to make it larger and test memory usage.\n")
            }
        }

        measureMemory("Plaintext Large (100KB)", largeContent) { content ->
            parser.parse(content, mapOf("filename" to "test.txt"))
        }
    }

    /**
     * Memory usage statistics for a test run.
     */
    data class MemoryStats(
        val testName: String,
        val contentSize: Int,
        val avgMemoryBytes: Long,
        val minMemoryBytes: Long,
        val maxMemoryBytes: Long
    ) {
        val avgMemoryKB: Double get() = avgMemoryBytes / 1024.0
        val avgMemoryMB: Double get() = avgMemoryBytes / (1024.0 * 1024.0)
        val contentKB: Double get() = contentSize / 1024.0
        val memoryOverhead: Double get() = avgMemoryBytes.toDouble() / contentSize.toDouble()

        fun report() {
            println("$testName:")
            println("  Content size: ${formatBytes(contentSize.toLong())}")
            println("  Avg memory:   ${formatBytes(avgMemoryBytes)}")
            println("  Min memory:   ${formatBytes(minMemoryBytes)}")
            println("  Max memory:   ${formatBytes(maxMemoryBytes)}")
            println("  Overhead:     ${String.format("%.2f", memoryOverhead)}x")
            println()
        }

        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes bytes"
                bytes < 1024 * 1024 -> "${String.format("%.2f", bytes / 1024.0)} KB"
                else -> "${String.format("%.2f", bytes / (1024.0 * 1024.0))} MB"
            }
        }
    }
}
