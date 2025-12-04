/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Plaintext Parser Performance Benchmarks
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlinx.benchmark.*

/**
 * Performance benchmarks for PlaintextParser.
 *
 * This benchmark suite establishes a baseline for the simplest parser.
 * PlaintextParser should be the fastest parser as it requires minimal
 * processing. These benchmarks help identify parser overhead and provide
 * a baseline for comparing more complex parsers.
 *
 * Benchmark Scenarios:
 * - Small text (1KB) - Short note
 * - Medium text (10KB) - Typical text file
 * - Large text (100KB) - Large document
 * - Very large text (1MB) - Stress test
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class PlaintextParserBenchmark {

    private lateinit var parser: PlaintextParser

    private lateinit var smallText: String      // ~1KB
    private lateinit var mediumText: String     // ~10KB
    private lateinit var largeText: String      // ~100KB
    private lateinit var veryLargeText: String  // ~1MB

    @Setup
    fun setup() {
        parser = PlaintextParser()

        // Small text: ~1KB
        smallText = buildString {
            repeat(20) {
                appendLine("This is line $it of a simple plaintext document.")
            }
        }

        // Medium text: ~10KB
        mediumText = buildString {
            repeat(200) {
                appendLine("Line $it: Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
            }
        }

        // Large text: ~100KB
        largeText = buildString {
            repeat(2000) {
                appendLine("Line $it: This is a longer line with more text content to simulate a realistic document structure.")
            }
        }

        // Very large text: ~1MB
        veryLargeText = buildString {
            repeat(20000) {
                appendLine("Line $it: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
            }
        }
    }

    /**
     * Benchmark parsing small plaintext (~1KB).
     *
     * Target: < 1ms (baseline for minimal processing)
     */
    @Benchmark
    fun parseSmallText(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallText, mapOf("filename" to "small.txt"))
    }

    /**
     * Benchmark parsing medium plaintext (~10KB).
     *
     * Target: < 5ms
     */
    @Benchmark
    fun parseMediumText(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumText, mapOf("filename" to "medium.txt"))
    }

    /**
     * Benchmark parsing large plaintext (~100KB).
     *
     * Target: < 50ms
     */
    @Benchmark
    fun parseLargeText(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeText, mapOf("filename" to "large.txt"))
    }

    /**
     * Benchmark parsing very large plaintext (~1MB).
     *
     * Target: < 500ms (stress test)
     */
    @Benchmark
    fun parseVeryLargeText(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(veryLargeText, mapOf("filename" to "verylarge.txt"))
    }

    /**
     * Benchmark HTML conversion.
     *
     * Target: < 5ms (minimal conversion)
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumText, mapOf("filename" to "medium.txt"))
        return parser.toHtml(document, lightMode = true)
    }
}
