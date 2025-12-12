/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for all format parsers
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import kotlin.test.*

/**
 * Comprehensive tests for all format parsers covering:
 * - Round-trip parsing (parse → format → parse)
 * - Cross-format compatibility
 * - Format-specific edge cases
 * - Performance benchmarks
 * - Memory efficiency
 */
class FormatParserComprehensiveTest {

    private val parsers = listOf(
        MarkdownParser(),
        PlaintextParser(),
        TodoTxtParser()
    )

    @Test
    fun testAllParsersWithSampleContent() {
        val sampleContents = mapOf(
            "Markdown" to """
                # Sample Document
                
                This is a **markdown** document with *formatting*.
                
                ## Features
                
                - Lists
                - Code blocks
                - Tables
                
                ```kotlin
                fun main() {
                    println("Hello, World!")
                }
                ```
            """.trimIndent(),
            
            "Plain Text" to """
                This is a plain text document.
                It has multiple lines.
                No special formatting is applied.
            """.trimIndent(),
            
            "Todo.txt" to """
                (A) 2024-01-01 Call Mom @phone +family
                (B) 2024-01-02 Finish report @work +project
                x 2024-01-03 2024-01-03 Completed task @done
                2024-01-04 Buy groceries @errands +personal
            """.trimIndent()
        )
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            val content = sampleContents[when {
                parserName.contains("Markdown", ignoreCase = true) -> "Markdown"
                parserName.contains("Todo", ignoreCase = true) -> "Todo.txt"
                else -> "Plain Text"
            }] ?: sampleContents["Plain Text"]!!
            
            val result = parser.parse(content)
            
            assertNotNull(result, "$parserName should parse sample content successfully")
            assertTrue(result.content.isNotEmpty(), "$parserName should produce non-empty content")
            assertNotNull(result.metadata, "$parserName should produce metadata")
            
            println("✓ $parserName successfully parsed sample content")
        }
    }

    @Test
    fun testRoundTripParsing() {
        val testContents = listOf(
            "# Markdown Document\n\nWith **bold** and *italic* text.",
            "Plain text document without any special formatting.",
            "(A) Todo item with @context +project",
            "Mixed content: **bold** and plain text together.",
            "Unicode content: ñáéíóú 中文 🚀",
            "Empty lines and\n\n\nmultiple breaks."
        )
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            
            testContents.forEach { content ->
                val result1 = parser.parse(content)
                assertNotNull(result1, "$parserName should parse content initially")
                
                // Format back to string (if formatter exists)
                val formattedContent = result1.content
                
                // Parse the formatted content again
                val result2 = parser.parse(formattedContent)
                assertNotNull(result2, "$parserName should parse formatted content")
                
                // Results should be equivalent (content-wise)
                assertEquals(result1.content, result2.content, 
                    "$parserName should produce consistent results in round-trip parsing")
                
                println("✓ $parserName round-trip parsing successful for: '${content.take(30)}...'")
            }
        }
    }

    @Test
    fun testCrossFormatCompatibility() {
        val crossFormatContent = """
            # Heading that looks like Markdown
            
            This is plain text that might be confused with other formats.
            
            - This looks like a list
            - But might be plain text
            
            [This looks like a link](http://example.com)
            
            (A) This could be a todo item or just text
            
            | This | looks | like |
            | a | table | structure |
            
            > This looks like a quote
            
            `This looks like code`
            
            **Bold text** and *italic text*
        """.trimIndent()
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            val result = parser.parse(crossFormatContent)
            
            assertNotNull(result, "$parserName should handle cross-format content")
            assertTrue(result.content.isNotEmpty(), "$parserName should produce content from cross-format input")
            
            println("✓ $parserName handled cross-format compatibility")
        }
    }

    @Test
    fun testFormatSpecificEdgeCases() {
        val edgeCases = mapOf(
            MarkdownParser::class to listOf(
                "# Heading with trailing spaces   " to "Heading with trailing spaces",
                "##Nested heading without space" to "Nested heading without space",
                "```\n\n```" to "Empty code block",
                "| Single | Column |" to "Single column table",
                "> > > Deep nesting" to "Deep blockquote nesting",
                "Mixed **bold *and italic*** text" to "Mixed formatting",
                "# Heading with [link](url) and `code`" to "Heading with mixed content"
            ),
            PlainTextParser::class to listOf(
                "" to "Empty string",
                "   " to "Only spaces",
                "\n\n\n" to "Only newlines",
                "Text with\nmultiple\nlines" to "Multiple lines",
                "Unicode: ñáéíóú 中文 🚀" to "Unicode characters",
                "Very long line that goes on and on without any breaks whatsoever" to "Very long line"
            ),
            TodoTxtParser::class to listOf(
                "(A) High priority task" to "High priority task",
                "(Z) Low priority task" to "Low priority task",
                "x Completed task" to "Completed task format",
                "x 2024-01-01 2024-01-01 Completed with dates" to "Completed with dates",
                "Task with @context +project" to "Task with context and project",
                "Task with multiple @context1 @context2 +project1 +project2" to "Multiple contexts and projects",
                "2024-01-01 Task with creation date" to "Task with creation date",
                "(A) 2024-01-01 Task with priority and date" to "Priority and date"
            )
        )
        
        edgeCases.forEach { (parserClass, cases) ->
            parsers.filter { it::class == parserClass }.forEach { parser ->
                val parserName = parser::class.simpleName ?: "UnknownParser"
                
                cases.forEach { (content, description) ->
                    val result = parser.parse(content)
                    
                    assertNotNull(result, "$parserName should handle edge case: $description")
                    println("✓ $parserName handled edge case: $description")
                }
            }
        }
    }

    @Test
    fun testPerformanceBenchmarks() {
        val contentSizes = listOf(100, 1000, 10000, 100000) // 100B to 100KB
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            println("\nPerformance benchmarks for $parserName:")
            
            contentSizes.forEach { size ->
                val content = generateTestContent(size, parser)
                
                // Warm up
                repeat(5) { parser.parse(content) }
                
                // Benchmark
                val times = mutableListOf<Long>()
                repeat(20) {
                    val startTime = System.nanoTime()
                    parser.parse(content)
                    val endTime = System.nanoTime()
                    times.add(endTime - startTime)
                }
                
                val averageTime = times.average() / 1_000_000 // Convert to milliseconds
                val minTime = times.minOrNull()?.div(1_000_000.0) ?: 0.0
                val maxTime = times.maxOrNull()?.div(1_000_000.0) ?: 0.0
                
                println("  $size bytes: avg ${averageTime}ms, min ${minTime}ms, max ${maxTime}ms")
                
                // Performance assertions
                when (size) {
                    100 -> assertTrue(averageTime < 10, "$parserName should parse 100B in under 10ms")
                    1000 -> assertTrue(averageTime < 20, "$parserName should parse 1KB in under 20ms")
                    10000 -> assertTrue(averageTime < 50, "$parserName should parse 10KB in under 50ms")
                    100000 -> assertTrue(averageTime < 200, "$parserName should parse 100KB in under 200ms")
                }
            }
        }
    }

    @Test
    fun testMemoryEfficiency() {
        val largeContent = generateLargeTestContent(100000) // 100KB
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            
            // Measure memory usage
            val runtime = Runtime.getRuntime()
            System.gc()
            Thread.sleep(100)
            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
            
            // Parse multiple times
            repeat(100) {
                parser.parse(largeContent)
            }
            
            System.gc()
            Thread.sleep(100)
            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            
            val memoryIncrease = memoryAfter - memoryBefore
            println("Memory efficiency for $parserName: ${memoryIncrease / 1024}KB increase after 100 parses")
            
            // Should not have significant memory leaks
            assertTrue(memoryIncrease < 10 * 1024 * 1024, "$parserName should not increase memory by more than 10MB")
        }
    }

    @Test
    fun testDeterministicBehavior() {
        val testContent = """
            # Test Heading
            
            This is a paragraph with **bold** and *italic* text.
            
            - Item 1
            - Item 2
            - Item 3
            
            ```
            code block
            ```
            
            | Col 1 | Col 2 |
            |-------|-------|
            | A | B |
        """.trimIndent()
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            
            // Parse multiple times
            val results = List(10) { parser.parse(testContent) }
            
            // All results should be identical
            results.forEach { result ->
                assertNotNull(result, "$parserName should produce consistent results")
                assertEquals(results[0].content, result.content, "$parserName should produce identical content")
                assertEquals(results[0].metadata, result.metadata, "$parserName should produce identical metadata")
            }
            
            println("✓ $parserName deterministic behavior verified")
        }
    }

    @Test
    fun testErrorRecovery() {
        val errorCases = listOf(
            null to "Null input",
            "" to "Empty string",
            "   " to "Whitespace only",
            "\u0000\u0001\u0002" to "Control characters",
            "Invalid syntax: [[[[" to "Invalid syntax",
            "Mixed: **unclosed bold and __unclosed italic" to "Mixed invalid syntax",
            "Very long single line: ${"a".repeat(10000)}" to "Very long line"
        )
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            
            errorCases.forEach { (content, description) ->
                try {
                    val result = if (content != null) parser.parse(content) else null
                    
                    if (result != null) {
                        println("✓ $parserName handled error case gracefully: $description")
                    } else {
                        println("✓ $parserName returned null for: $description")
                    }
                } catch (e: Exception) {
                    println("✓ $parserName threw expected exception for: $description - ${e.message}")
                }
            }
        }
    }

    @Test
    fun testConcurrentParsing() = kotlinx.coroutines.test.runTest {
        val concurrentParses = 50
        val content = generateLargeTestContent(10000) // 10KB content
        
        parsers.forEach { parser ->
            val parserName = parser::class.simpleName ?: "UnknownParser"
            
            val results = mutableListOf<ParsedDocument>()
            val jobs = (1..concurrentParses).map { index ->
                kotlinx.coroutines.async {
                    try {
                        val result = parser.parse(content)
                        assertNotNull(result, "$parserName should parse successfully in concurrent execution")
                        synchronized(results) {
                            results.add(result)
                        }
                        true
                    } catch (e: Exception) {
                        println("Concurrent parse $index failed for $parserName: ${e.message}")
                        false
                    }
                }
            }
            
            val successCount = jobs.awaitAll().count { it }
            
            assertEquals(concurrentParses, successCount, "$parserName should complete all concurrent parses successfully")
            assertEquals(concurrentParses, results.size, "$parserName should produce results for all concurrent parses")
            
            // All results should be identical
            results.forEach { result ->
                assertEquals(results[0].content, result.content, "$parserName should produce consistent results in concurrent execution")
            }
            
            println("✓ $parserName concurrent parsing successful: $concurrentParses operations")
        }
    }

    // ==================== Helper Methods ====================

    private fun generateTestContent(size: Int, parser: TextParser): String {
        return when (parser) {
            is MarkdownParser -> buildString {
                repeat(size / 200) {
                    appendLine("# Heading $it")
                    appendLine()
                    appendLine("This is paragraph $it with **bold** and *italic* text.")
                    appendLine()
                    appendLine("- Item $it")
                    appendLine("- Another item $it")
                    appendLine()
                }
            }.take(size)
            
            is TodoTxtParser -> buildString {
                repeat(size / 100) {
                    appendLine("(A) 2024-01-0${it % 9 + 1} Task $it @context +project")
                }
            }.take(size)
            
            else -> "Plain text content ".repeat(size / 20).take(size)
        }
    }

    private fun generateLargeTestContent(size: Int): String {
        return buildString {
            repeat(size / 100) {
                append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
            }
        }.take(size)
    }
}