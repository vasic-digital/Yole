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
 * different table sizes and complexity levels to establish baselines and
 * track optimization improvements.
 *
 * Benchmark Scenarios:
 * - Small table (10 rows x 5 columns) - Simple CSV
 * - Medium table (100 rows x 10 columns) - Typical data export
 * - Large table (1000 rows x 20 columns) - Large dataset
 * - Complex table - Quoted fields, special characters, mixed data types
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class CsvParserBenchmark {

    private lateinit var parser: CsvParser

    // Test content of various sizes
    private lateinit var smallContent: String      // 10x5 table
    private lateinit var mediumContent: String     // 100x10 table
    private lateinit var largeContent: String      // 1000x20 table
    private lateinit var complexContent: String    // Complex CSV features

    @Setup
    fun setup() {
        parser = CsvParser()

        // Small content: 10 rows x 5 columns
        smallContent = buildString {
            append("ID,Name,Age,City,Country\n")
            for (i in 1..10) {
                append("$i,Person$i,${20 + i},City$i,Country$i\n")
            }
        }

        // Medium content: 100 rows x 10 columns
        mediumContent = buildString {
            append("ID,Name,Age,Email,Phone,City,Country,Occupation,Salary,Department\n")
            for (i in 1..100) {
                append("$i,Employee$i,${25 + (i % 40)},employee$i@example.com,")
                append("+1-555-${String.format("%04d", i)},City$i,Country${i % 10},")
                append("Job${i % 15},${50000 + i * 1000},Dept${i % 8}\n")
            }
        }

        // Large content: 1000 rows x 20 columns
        largeContent = buildString {
            append("ID,FirstName,LastName,Age,Email,Phone,Address,City,State,Country,")
            append("ZipCode,Occupation,Salary,Department,HireDate,Manager,Team,Status,Notes,Score\n")

            for (i in 1..1000) {
                append("$i,First$i,Last$i,${20 + (i % 50)},user$i@example.com,")
                append("+1-555-${String.format("%04d", i)},${i} Main St,City$i,State${i % 50},")
                append("Country${i % 20},${10000 + i},Job${i % 30},${40000 + i * 500},")
                append("Dept${i % 12},2025-01-${String.format("%02d", (i % 28) + 1)},")
                append("Manager${i % 25},Team${i % 10},Active,Note $i,${i % 100}\n")
            }
        }

        // Complex content: CSV with quoted fields, special characters, etc.
        complexContent = buildString {
            append("Name,Description,Price,Category,Stock,Available\n")
            append("\"Product 1\",\"Simple product\",19.99,Electronics,100,Yes\n")
            append("\"Product 2\",\"Product with \"\"quotes\"\"\",29.99,Home,50,Yes\n")
            append("\"Product 3\",\"Multi-line description here\",39.99,Garden,25,No\n")
            append("\"Product, with comma\",\"Description, also has, commas\",49.99,Kitchen,75,Yes\n")
            append("Simple Product,No quotes needed,59.99,Office,200,Yes\n")
            append("\"Special chars: é, ñ, 中文\",\"Unicode description\",69.99,International,30,Yes\n")
            append("\"Tabs and spaces\",\"Mixed whitespace\",79.99,Tools,15,No\n")
            append("\"Empty fields test\",,,,,\n")
            append("\"\"\"Last item\"\"\",\"Contains delimiter: , and quote: \"\"\",89.99,Misc,5,Yes")
        }
    }

    /**
     * Benchmark parsing small CSV table (10x5).
     *
     * Target: < 5ms for small table
     */
    @Benchmark
    fun parseSmallTable(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallContent, mapOf("filename" to "small.csv"))
    }

    /**
     * Benchmark parsing medium CSV table (100x10).
     *
     * Target: < 30ms for 100 rows
     */
    @Benchmark
    fun parseMediumTable(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumContent, mapOf("filename" to "medium.csv"))
    }

    /**
     * Benchmark parsing large CSV table (1000x20).
     *
     * Target: < 300ms for 1000 rows
     */
    @Benchmark
    fun parseLargeTable(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeContent, mapOf("filename" to "large.csv"))
    }

    /**
     * Benchmark parsing complex CSV with quoted fields and special chars.
     *
     * Target: < 10ms for complex parsing
     */
    @Benchmark
    fun parseComplexTable(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexContent, mapOf("filename" to "complex.csv"))
    }

    /**
     * Benchmark HTML table conversion.
     *
     * Target: < 20ms for HTML table generation
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumContent, mapOf("filename" to "medium.csv"))
        return parser.toHtml(document, lightMode = true)
    }

    /**
     * Benchmark CSV validation.
     *
     * Target: < 15ms for validation
     */
    @Benchmark
    fun validateDocument(): List<String> {
        return parser.validate(mediumContent)
    }

    /**
     * Benchmark parsing TSV (tab-separated values).
     *
     * Target: < 20ms for TSV parsing
     */
    @Benchmark
    fun parseTsvTable(): digital.vasic.yole.format.ParsedDocument {
        val tsvContent = smallContent.replace(',', '\t')
        return parser.parse(tsvContent, mapOf("filename" to "small.tsv"))
    }
}
