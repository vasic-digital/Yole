/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * reStructuredText Parser Performance Benchmarks
 * Establishes baseline performance metrics for reStructuredText parsing
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Performance benchmarks for reStructuredText (reST) parser.
 *
 * Tests parser performance across different document sizes and complexity levels.
 *
 * Performance Targets:
 * - Small documents (~2KB): < 35ms
 * - Medium documents (~20KB): < 180ms
 * - Large documents (~200KB): < 1800ms
 * - Complex documents: < 90ms
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class RestructuredTextParserBenchmark {

    private val parser = RestructuredTextParser()

    // Test data
    private lateinit var smallDocument: String
    private lateinit var mediumDocument: String
    private lateinit var largeDocument: String
    private lateinit var complexDocument: String

    @Setup
    fun setup() {
        // Small reST document (~2KB)
        smallDocument = generateSmallRestDocument()

        // Medium reST document (~20KB) - typical Python documentation
        mediumDocument = generateMediumRestDocument()

        // Large reST document (~200KB) - comprehensive Sphinx docs
        largeDocument = generateLargeRestDocument()

        // Complex reST document with advanced features
        complexDocument = generateComplexRestDocument()
    }

    @Benchmark
    fun parseSmallDocument() {
        parser.parse(smallDocument, emptyMap())
    }

    @Benchmark
    fun parseMediumDocument() {
        parser.parse(mediumDocument, emptyMap())
    }

    @Benchmark
    fun parseLargeDocument() {
        parser.parse(largeDocument, emptyMap())
    }

    @Benchmark
    fun parseComplexDocument() {
        parser.parse(complexDocument, emptyMap())
    }

    @Benchmark
    fun convertToHtml() {
        val document = parser.parse(mediumDocument, emptyMap())
        parser.toHtml(document, lightMode = false)
    }

    @Benchmark
    fun validateDocument() {
        parser.validate(mediumDocument)
    }

    // Test data generators

    private fun generateSmallRestDocument(): String = buildString {
        appendLine("Simple reStructuredText Document")
        appendLine("==================================")
        appendLine()
        appendLine(":Author: Documentation Team")
        appendLine(":Date: 2025-11-19")
        appendLine(":Version: 1.0")
        appendLine()
        appendLine("Introduction")
        appendLine("------------")
        appendLine()
        appendLine("This is a simple reStructuredText document.")
        appendLine()
        appendLine("Features")
        appendLine("--------")
        appendLine()
        appendLine("* First feature")
        appendLine("* Second feature")
        appendLine("* Third feature")
        appendLine()
        appendLine("Code Example")
        appendLine("------------")
        appendLine()
        appendLine(".. code-block:: kotlin")
        appendLine()
        appendLine("   fun main() {")
        appendLine("       println(\"Hello, reStructuredText!\")")
        appendLine("   }")
        appendLine()
        appendLine("Conclusion")
        appendLine("----------")
        appendLine()
        appendLine("reStructuredText is widely used for Python documentation.")
    }

    private fun generateMediumRestDocument(): String = buildString {
        appendLine("Python Project Documentation")
        appendLine("=============================")
        appendLine()
        appendLine(":Author: Development Team")
        appendLine(":Date: 2025-11-19")
        appendLine(":Version: 2.0")
        appendLine()
        appendLine(".. contents:: Table of Contents")
        appendLine("   :depth: 3")
        appendLine()

        // Generate 10 sections
        for (i in 1..10) {
            appendLine("Section $i: Module Documentation")
            appendLine("=" .repeat(35 + i.toString().length))
            appendLine()
            appendLine("This section documents module $i of the project.")
            appendLine()
            appendLine("Overview")
            appendLine("--------")
            appendLine()
            appendLine("Module $i provides the following capabilities:")
            appendLine()
            appendLine("* Capability ${i}.1 - Core functionality")
            appendLine("* Capability ${i}.2 - Advanced features")
            appendLine("* Capability ${i}.3 - Integration support")
            appendLine("* Capability ${i}.4 - Performance optimization")
            appendLine()
            appendLine("API Reference")
            appendLine("-------------")
            appendLine()
            appendLine(".. code-block:: kotlin")
            appendLine()
            appendLine("   class Module$i {")
            appendLine("       /**")
            appendLine("        * Execute the module functionality.")
            appendLine("        *")
            appendLine("        * :param input: Input data to process")
            appendLine("        * :returns: Processed result")
            appendLine("        */")
            appendLine("       fun execute(input: String): Result {")
            appendLine("           // Implementation")
            appendLine("           return Result.success(process(input))")
            appendLine("       }")
            appendLine("   }")
            appendLine()
            appendLine("Configuration")
            appendLine("-------------")
            appendLine()
            appendLine(".. list-table:: Configuration Options")
            appendLine("   :widths: 20 40 20")
            appendLine("   :header-rows: 1")
            appendLine()
            appendLine("   * - Option")
            appendLine("     - Description")
            appendLine("     - Default")
            appendLine("   * - enabled")
            appendLine("     - Enable module $i")
            appendLine("     - true")
            appendLine("   * - timeout")
            appendLine("     - Execution timeout in seconds")
            appendLine("     - 30")
            appendLine("   * - retries")
            appendLine("     - Number of retry attempts")
            appendLine("     - 3")
            appendLine()
            appendLine(".. note::")
            appendLine()
            appendLine("   Always test configuration changes in a staging environment.")
            appendLine()
            appendLine("Examples")
            appendLine("--------")
            appendLine()
            appendLine("Basic Usage")
            appendLine("~~~~~~~~~~~")
            appendLine()
            appendLine(".. code-block:: kotlin")
            appendLine()
            appendLine("   val module = Module$i()")
            appendLine("   val result = module.execute(\"test data\")")
            appendLine("   println(result)")
            appendLine()
            appendLine("Advanced Usage")
            appendLine("~~~~~~~~~~~~~~")
            appendLine()
            appendLine(".. code-block:: kotlin")
            appendLine()
            appendLine("   val module = Module$i(")
            appendLine("       config = Configuration(")
            appendLine("           timeout = 60,")
            appendLine("           retries = 5")
            appendLine("       )")
            appendLine("   )")
            appendLine("   val result = module.execute(\"complex data\")")
            appendLine()
        }

        appendLine("Glossary")
        appendLine("========")
        appendLine()
        appendLine(".. glossary::")
        appendLine()
        appendLine("   reStructuredText")
        appendLine("      A plaintext markup language")
        appendLine()
        appendLine("   Sphinx")
        appendLine("      Documentation generator")
    }

    private fun generateLargeRestDocument(): String = buildString {
        appendLine("Comprehensive API Reference")
        appendLine("============================")
        appendLine()
        appendLine(":Author: API Documentation Team")
        appendLine(":Date: 2025-11-19")
        appendLine(":Version: 3.0")
        appendLine()
        appendLine(".. contents:: Contents")
        appendLine("   :depth: 4")
        appendLine("   :local:")
        appendLine()

        // Generate 20 chapters with 5 sections each
        for (chapter in 1..20) {
            appendLine("Chapter $chapter: Advanced Topics")
            appendLine("=" .repeat(35 + chapter.toString().length))
            appendLine()

            for (section in 1..5) {
                appendLine("Section ${chapter}.$section: Topic Details")
                appendLine("-" .repeat(44 + "${chapter}.$section".length))
                appendLine()
                appendLine("Introduction to ${chapter}.$section")
                appendLine("~" .repeat(35 + "${chapter}.$section".length))
                appendLine()
                appendLine("Detailed discussion of topic ${chapter}.$section.")
                appendLine()
                appendLine("Key Concepts")
                appendLine("^^^^^^^^^^^^")
                appendLine()
                appendLine("* Concept ${chapter}.$section.1 - Architecture patterns")
                appendLine("* Concept ${chapter}.$section.2 - Design principles")
                appendLine("* Concept ${chapter}.$section.3 - Implementation strategies")
                appendLine("* Concept ${chapter}.$section.4 - Common pitfalls")
                appendLine("* Concept ${chapter}.$section.5 - Best practices")
                appendLine()
                appendLine("Implementation")
                appendLine("^^^^^^^^^^^^^^")
                appendLine()
                appendLine(".. code-block:: kotlin")
                appendLine("   :linenos:")
                appendLine("   :emphasize-lines: 3,7-9")
                appendLine()
                appendLine("   class Chapter${chapter}Section$section {")
                appendLine("       private val config = Configuration()")
                appendLine("       ")
                appendLine("       fun initialize() {")
                appendLine("           config.load(\"settings.properties\")")
                appendLine("           validateConfiguration()")
                appendLine("       }")
                appendLine("       ")
                appendLine("       fun process(input: String): Result {")
                appendLine("           val validated = validate(input)")
                appendLine("           if (!validated) {")
                appendLine("               return Result.error(\"Invalid input\")")
                appendLine("           }")
                appendLine("           val transformed = transform(input)")
                appendLine("           return Result.success(transformed)")
                appendLine("       }")
                appendLine("   }")
                appendLine()
                appendLine("Configuration Reference")
                appendLine("^^^^^^^^^^^^^^^^^^^^^^^")
                appendLine()
                appendLine(".. list-table::")
                appendLine("   :widths: 20 30 15 15")
                appendLine("   :header-rows: 1")
                appendLine()
                appendLine("   * - Parameter")
                appendLine("     - Description")
                appendLine("     - Type")
                appendLine("     - Default")
                appendLine("   * - maxConnections")
                appendLine("     - Maximum concurrent connections")
                appendLine("     - Integer")
                appendLine("     - 100")
                appendLine("   * - timeout")
                appendLine("     - Request timeout in milliseconds")
                appendLine("     - Long")
                appendLine("     - 5000")
                appendLine("   * - retryPolicy")
                appendLine("     - Retry strategy for failed requests")
                appendLine("     - String")
                appendLine("     - exponential")
                appendLine()
                appendLine(".. warning::")
                appendLine()
                appendLine("   Ensure all configuration values are validated before use.")
                appendLine()
            }
        }
    }

    private fun generateComplexRestDocument(): String = buildString {
        appendLine("Advanced reStructuredText Features")
        appendLine("====================================")
        appendLine()
        appendLine(":Author: Advanced User")
        appendLine(":Date: 2025-11-19")
        appendLine(":Version: 1.0")
        appendLine()
        appendLine(".. contents:: Contents")
        appendLine("   :depth: 2")
        appendLine()
        appendLine("Admonitions")
        appendLine("-----------")
        appendLine()
        appendLine(".. note::")
        appendLine()
        appendLine("   This is a note admonition.")
        appendLine()
        appendLine(".. tip::")
        appendLine()
        appendLine("   Pro tips are highlighted here.")
        appendLine()
        appendLine(".. important::")
        appendLine()
        appendLine("   Critical information is marked important.")
        appendLine()
        appendLine(".. warning::")
        appendLine()
        appendLine("   Potential issues are flagged with warnings.")
        appendLine()
        appendLine(".. danger::")
        appendLine()
        appendLine("   Dangerous operations require special attention.")
        appendLine()
        appendLine("Complex Tables")
        appendLine("--------------")
        appendLine()
        appendLine(".. list-table:: Feature Matrix")
        appendLine("   :widths: 15 30 20 15")
        appendLine("   :header-rows: 1")
        appendLine("   :stub-columns: 1")
        appendLine()
        appendLine("   * - ID")
        appendLine("     - Feature")
        appendLine("     - Status")
        appendLine("     - Priority")
        appendLine("   * - F001")
        appendLine("     - User Authentication")
        appendLine("     - Implemented")
        appendLine("     - High")
        appendLine("   * - F002")
        appendLine("     - Data Encryption")
        appendLine("     - In Progress")
        appendLine("     - Critical")
        appendLine("   * - F003")
        appendLine("     - Export to PDF")
        appendLine("     - Planned")
        appendLine("     - Medium")
        appendLine()
        appendLine("Directives")
        appendLine("----------")
        appendLine()
        appendLine(".. code-block:: kotlin")
        appendLine("   :linenos:")
        appendLine("   :emphasize-lines: 2,5-7")
        appendLine("   :caption: Data Processor Example")
        appendLine()
        appendLine("   class DataProcessor {")
        appendLine("       fun process(data: String): Result {  // Entry point")
        appendLine("           val validated = validate(data)   // Validation")
        appendLine("           if (!validated) {")
        appendLine("               return Result.error(\"Invalid data\")  // Error handling")
        appendLine("           }")
        appendLine("           return Result.success(transform(data))")
        appendLine("       }")
        appendLine("   }")
        appendLine()
        appendLine("Substitutions")
        appendLine("-------------")
        appendLine()
        appendLine(".. |date| date::")
        appendLine(".. |version| replace:: 1.0.0")
        appendLine(".. |project| replace:: reStructuredText Parser")
        appendLine()
        appendLine("This is |project| version |version| as of |date|.")
        appendLine()
        appendLine("Cross-References")
        appendLine("----------------")
        appendLine()
        appendLine("See :ref:`admonitions` for more information.")
        appendLine()
        appendLine(".. _admonitions:")
        appendLine()
        appendLine("The admonitions section is referenced above.")
        appendLine()
        appendLine("Inline Markup")
        appendLine("-------------")
        appendLine()
        appendLine("Use *emphasis* for italics and **strong** for bold text.")
        appendLine()
        appendLine("Code can be marked as ``inline code`` within text.")
        appendLine()
        appendLine("Math Expressions")
        appendLine("----------------")
        appendLine()
        appendLine(".. math::")
        appendLine()
        appendLine("   \\int_0^\\infty e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}")
    }
}
