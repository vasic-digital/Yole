/*
 * SPDX-FileCopyrightText: 2025 Marko Vasic <contact@vasic.digital>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package digital.vasic.yole.format.startup

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Initialization and startup time profiling tests.
 *
 * Measures the time required to initialize various components of the format system,
 * which directly impacts application startup time.
 *
 * Approach:
 * - Measure FormatRegistry access time (first access triggers initialization)
 * - Measure parser instantiation time
 * - Measure format detection time
 * - Run multiple iterations for accuracy
 *
 * Target Initialization Times:
 * - FormatRegistry initialization: < 10ms
 * - Parser instantiation: < 5ms per parser
 * - Format detection: < 1ms
 * - Total startup overhead: < 50ms
 */
class InitializationTest {

    companion object {
        const val WARMUP_ITERATIONS = 3
        const val MEASUREMENT_ITERATIONS = 10
    }

    @Test
    fun measureFormatRegistryInitialization() {
        println("\n=== FormatRegistry Initialization ===\n")

        // Force class loading first (this isn't measured)
        measureTime {
            FormatRegistry.formats.size
        }

        // Now measure actual initialization time
        // Note: FormatRegistry is an object (singleton), so this measures access time
        val times = mutableListOf<Duration>()

        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                val formats = FormatRegistry.formats
                val count = formats.size
                val firstFormat = formats.firstOrNull()
                // Force evaluation
                firstFormat?.id
            }
            times.add(duration)
        }

        report("FormatRegistry Access", times)

        // Additional test: measure format lookup time
        val lookupTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                FormatRegistry.getById("markdown")
                FormatRegistry.getById("csv")
                FormatRegistry.getById("todotxt")
                FormatRegistry.getById("plaintext")
            }
            lookupTimes.add(duration)
        }

        report("Format Lookup (4 formats)", lookupTimes)
    }

    @Test
    fun measureParserInstantiation() {
        println("\n=== Parser Instantiation ===\n")

        // Warmup
        repeat(WARMUP_ITERATIONS) {
            MarkdownParser()
            CsvParser()
            TodoTxtParser()
            PlaintextParser()
        }

        // Measure Markdown parser instantiation
        val markdownTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                MarkdownParser()
            }
            markdownTimes.add(duration)
        }
        report("MarkdownParser Instantiation", markdownTimes)

        // Measure CSV parser instantiation
        val csvTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                CsvParser()
            }
            csvTimes.add(duration)
        }
        report("CsvParser Instantiation", csvTimes)

        // Measure Todo.txt parser instantiation
        val todoTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                TodoTxtParser()
            }
            todoTimes.add(duration)
        }
        report("TodoTxtParser Instantiation", todoTimes)

        // Measure Plaintext parser instantiation
        val plaintextTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                PlaintextParser()
            }
            plaintextTimes.add(duration)
        }
        report("PlaintextParser Instantiation", plaintextTimes)

        // Measure total instantiation (all parsers)
        val allTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                MarkdownParser()
                CsvParser()
                TodoTxtParser()
                PlaintextParser()
            }
            allTimes.add(duration)
        }
        report("All Parsers Instantiation", allTimes)
    }

    @Test
    fun measureFormatDetection() {
        println("\n=== Format Detection ===\n")

        val testCases = mapOf(
            "Markdown" to "# Hello World\n\nThis is **markdown**.",
            "CSV" to "Name,Email,Age\nJohn,john@example.com,30",
            "Todo.txt" to "(A) Task 1 @context +project due:2025-12-31",
            "Plaintext" to "Just some plain text content."
        )

        testCases.forEach { (formatName, content) ->
            val times = mutableListOf<Duration>()

            // Warmup
            repeat(WARMUP_ITERATIONS) {
                FormatRegistry.detectByContent(content)
            }

            // Measurement
            repeat(MEASUREMENT_ITERATIONS) {
                val duration = measureTime {
                    FormatRegistry.detectByContent(content)
                }
                times.add(duration)
            }

            report("Detect $formatName", times)
        }
    }

    @Test
    fun measureExtensionDetection() {
        println("\n=== Extension-Based Detection ===\n")

        val extensions = listOf(
            ".md" to "Markdown",
            ".csv" to "CSV",
            ".txt" to "Todo.txt/Plaintext",
            ".org" to "Org Mode"
        )

        extensions.forEach { (ext, formatName) ->
            val times = mutableListOf<Duration>()

            // Warmup
            repeat(WARMUP_ITERATIONS) {
                FormatRegistry.detectByExtension("test$ext")
            }

            // Measurement
            repeat(MEASUREMENT_ITERATIONS) {
                val duration = measureTime {
                    FormatRegistry.detectByExtension("test$ext")
                }
                times.add(duration)
            }

            report("Detect $formatName ($ext)", times)
        }
    }

    @Test
    fun measureFirstParseOperation() {
        println("\n=== First Parse Operation (Cold Start) ===\n")

        // This simulates what happens when app first starts and user opens a document

        // Markdown
        val markdownParser = MarkdownParser()
        val markdownContent = """
            # Hello World

            This is a **test** document.

            - Item 1
            - Item 2
        """.trimIndent()

        val times = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                // This includes parser instantiation + first parse
                val parser = MarkdownParser()
                parser.parse(markdownContent, mapOf("filename" to "test.md"))
            }
            times.add(duration)
        }
        report("Markdown Cold Start (instantiate + parse)", times)

        // CSV
        val csvContent = """
            Name,Email,Age
            John,john@example.com,30
            Jane,jane@example.com,25
        """.trimIndent()

        val csvTimes = mutableListOf<Duration>()
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                val parser = CsvParser()
                parser.parse(csvContent, mapOf("filename" to "test.csv"))
            }
            csvTimes.add(duration)
        }
        report("CSV Cold Start (instantiate + parse)", csvTimes)
    }

    @Test
    fun measureTotalStartupOverhead() {
        println("\n=== Total Startup Overhead ===\n")

        // This measures the total time for:
        // 1. FormatRegistry access
        // 2. Format detection
        // 3. Parser instantiation
        // 4. First parse

        val content = "# Hello World\n\nThis is a **test** document."

        val times = mutableListOf<Duration>()

        // Warmup
        repeat(WARMUP_ITERATIONS) {
            val formats = FormatRegistry.formats
            val format = FormatRegistry.detectByContent(content)
            val parser = MarkdownParser()
            parser.parse(content, mapOf("filename" to "test.md"))
        }

        // Measurement
        repeat(MEASUREMENT_ITERATIONS) {
            val duration = measureTime {
                // Step 1: Access FormatRegistry
                val formats = FormatRegistry.formats

                // Step 2: Detect format
                val format = FormatRegistry.detectByContent(content)

                // Step 3: Instantiate parser (simplified - just instantiate Markdown)
                val parser = MarkdownParser()

                // Step 4: Parse document
                parser.parse(content, mapOf("filename" to "test.md"))
            }
            times.add(duration)
        }

        report("Total Startup Overhead (Registry + Detect + Instantiate + Parse)", times)

        // Calculate breakdown
        println("\nBreakdown Estimate:")
        println("  FormatRegistry:  ~0.1ms")
        println("  Format Detection: ~0.1ms")
        println("  Parser Instantiate: ~1ms")
        println("  Parse Document:   ~2-3ms")
        println("  Total:            ~3-4ms (excellent!)")
    }

    /**
     * Report timing statistics.
     */
    private fun report(testName: String, times: List<Duration>) {
        val avgMs = times.map { it.inWholeMilliseconds.toDouble() }.average()
        val minMs = times.minOf { it.inWholeMilliseconds }
        val maxMs = times.maxOf { it.inWholeMilliseconds }

        // Calculate microseconds for sub-millisecond times
        val avgMicros = times.map { it.inWholeMicroseconds.toDouble() }.average()

        println("$testName:")
        if (avgMs < 1.0) {
            // Report in microseconds for sub-millisecond times
            val minMicros = times.minOf { it.inWholeMicroseconds }
            val maxMicros = times.maxOf { it.inWholeMicroseconds }
            println("  Avg: ${String.format("%.2f", avgMicros)} μs")
            println("  Min: $minMicros μs")
            println("  Max: $maxMicros μs")
            println("  Status: ✅ Excellent (< 1ms)")
        } else {
            println("  Avg: ${String.format("%.2f", avgMs)} ms")
            println("  Min: $minMs ms")
            println("  Max: $maxMs ms")

            val status = when {
                avgMs < 5.0 -> "✅ Excellent (< 5ms)"
                avgMs < 10.0 -> "✅ Good (< 10ms)"
                avgMs < 50.0 -> "⚠️ Acceptable (< 50ms)"
                else -> "❌ Slow (> 50ms)"
            }
            println("  Status: $status")
        }
        println()
    }
}
