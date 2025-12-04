/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * CSV Parser Performance Benchmarks
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.csv.CsvParser
import kotlinx.benchmark.*

/**
 * Performance benchmarks for CsvParser.
 *
 * This benchmark suite measures the performance of CSV parsing across
 * different table sizes and complexity levels.
 *
 * Benchmark Scenarios:
 * - Small table (10 rows) - Basic CSV
 * - Medium table (100 rows) - Typical data file
 * - Large table (1000 rows) - Large dataset
 * - Complex data - Special characters, quotes, escaping
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class CsvParserBenchmark {

    private lateinit var parser: CsvParser

    private lateinit var smallCsv: String       // 10 rows
    private lateinit var mediumCsv: String      // 100 rows
    private lateinit var largeCsv: String       // 1000 rows
    private lateinit var complexCsv: String     // Special characters

    @Setup
    fun setup() {
        parser = CsvParser()

        // Small CSV: 10 rows
        smallCsv = buildString {
            append("Name,Age,Email,Department\n")
            for (i in 1..10) {
                append("User$i,$i,user$i@example.com,Dept$i\n")
            }
        }

        // Medium CSV: 100 rows
        mediumCsv = buildString {
            append("ID,Name,Email,Phone,Department,Salary,Date\n")
            for (i in 1..100) {
                append("$i,Employee$i,employee$i@company.com,555-000$i,Department${i % 5},${50000 + i * 1000},2025-01-${"${i % 28 + 1}".padStart(2, '0')}\n")
            }
        }

        // Large CSV: 1000 rows
        largeCsv = buildString {
            append("ID,FirstName,LastName,Email,Phone,Address,City,State,Zip,Country\n")
            for (i in 1..1000) {
                append("$i,First$i,Last$i,user$i@domain.com,555-${"$i".padStart(4, '0')},")
                append("${i} Main St,City$i,State${i % 50},${10000 + i},Country${i % 10}\n")
            }
        }

        // Complex CSV: Special characters, quotes, escaping
        complexCsv = """
            Name,Description,Price,Tags
            "Product, with comma","Description with ""quotes"" inside",29.99,"tag1,tag2,tag3"
            "Product
            with
            newlines","Multi-line description",49.99,"special,chars"
            "Simple Product","Normal description",19.99,"basic"
            "Product with 'quotes'","Description with, commas and ""quotes""",39.99,"complex,data"
            "Emoji Product 😀","Description with special chars: €, ¥, £",59.99,"international"
        """.trimIndent()
    }

    /**
     * Benchmark parsing small CSV (10 rows).
     *
     * Target: < 5ms for 10 rows
     */
    @Benchmark
    fun parseSmallCsv(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallCsv, mapOf("filename" to "small.csv"))
    }

    /**
     * Benchmark parsing medium CSV (100 rows).
     *
     * Target: < 30ms for 100 rows
     */
    @Benchmark
    fun parseMediumCsv(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumCsv, mapOf("filename" to "medium.csv"))
    }

    /**
     * Benchmark parsing large CSV (1000 rows).
     *
     * Target: < 300ms for 1000 rows
     */
    @Benchmark
    fun parseLargeCsv(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeCsv, mapOf("filename" to "large.csv"))
    }

    /**
     * Benchmark parsing complex CSV with special characters.
     *
     * Target: < 10ms for complex parsing
     */
    @Benchmark
    fun parseComplexCsv(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexCsv, mapOf("filename" to "complex.csv"))
    }

    /**
     * Benchmark HTML table conversion.
     *
     * Target: < 50ms for HTML conversion
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumCsv, mapOf("filename" to "medium.csv"))
        return parser.toHtml(document, lightMode = true)
    }
}
