/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * AsciiDoc Parser Performance Benchmarks
 * Establishes baseline performance metrics for AsciiDoc parsing
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.asciidoc.AsciidocParser
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Performance benchmarks for AsciiDoc parser.
 *
 * Tests parser performance across different document sizes and complexity levels.
 *
 * Performance Targets:
 * - Small documents (~2KB): < 30ms
 * - Medium documents (~20KB): < 150ms
 * - Large documents (~200KB): < 1500ms
 * - Complex documents: < 80ms
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class AsciidocParserBenchmark {

    private val parser = AsciidocParser()

    // Test data
    private lateinit var smallDocument: String
    private lateinit var mediumDocument: String
    private lateinit var largeDocument: String
    private lateinit var complexDocument: String

    @Setup
    fun setup() {
        // Small AsciiDoc document (~2KB)
        smallDocument = generateSmallAsciidocDocument()

        // Medium AsciiDoc document (~20KB) - typical documentation page
        mediumDocument = generateMediumAsciidocDocument()

        // Large AsciiDoc document (~200KB) - comprehensive manual
        largeDocument = generateLargeAsciidocDocument()

        // Complex AsciiDoc document with advanced features
        complexDocument = generateComplexAsciidocDocument()
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

    private fun generateSmallAsciidocDocument(): String = buildString {
        appendLine("= Simple AsciiDoc Document")
        appendLine("Author Name <author@example.com>")
        appendLine("v1.0, 2025-11-19")
        appendLine()
        appendLine("== Introduction")
        appendLine()
        appendLine("This is a simple AsciiDoc document demonstrating basic formatting.")
        appendLine()
        appendLine("== Features")
        appendLine()
        appendLine("* First feature")
        appendLine("* Second feature")
        appendLine("* Third feature")
        appendLine()
        appendLine("== Code Example")
        appendLine()
        appendLine("[source,kotlin]")
        appendLine("----")
        appendLine("fun main() {")
        appendLine("    println(\"Hello, AsciiDoc!\")")
        appendLine("}")
        appendLine("----")
        appendLine()
        appendLine("== Conclusion")
        appendLine()
        appendLine("AsciiDoc is a lightweight markup language.")
    }

    private fun generateMediumAsciidocDocument(): String = buildString {
        appendLine("= AsciiDoc Documentation Guide")
        appendLine("Documentation Team <docs@example.com>")
        appendLine("v2.0, 2025-11-19")
        appendLine(":toc:")
        appendLine(":icons: font")
        appendLine(":source-highlighter: highlight.js")
        appendLine()

        // Generate 10 sections
        for (i in 1..10) {
            appendLine("== Section $i: Feature Documentation")
            appendLine()
            appendLine("This section covers feature $i in detail.")
            appendLine()
            appendLine("=== Overview")
            appendLine()
            appendLine("Feature $i provides the following capabilities:")
            appendLine()
            appendLine("* Capability ${i}.1 - Basic functionality")
            appendLine("* Capability ${i}.2 - Advanced options")
            appendLine("* Capability ${i}.3 - Integration points")
            appendLine("* Capability ${i}.4 - Performance considerations")
            appendLine()
            appendLine("=== Implementation")
            appendLine()
            appendLine("[source,kotlin]")
            appendLine("----")
            appendLine("class Feature$i {")
            appendLine("    fun execute() {")
            appendLine("        // Implementation details")
            appendLine("        println(\"Executing feature $i\")")
            appendLine("    }")
            appendLine("}")
            appendLine("----")
            appendLine()
            appendLine("=== Configuration")
            appendLine()
            appendLine("Configuration options for feature $i:")
            appendLine()
            appendLine("[cols=\"1,2,1\"]")
            appendLine("|===")
            appendLine("|Option |Description |Default")
            appendLine()
            appendLine("|enabled")
            appendLine("|Enable feature $i")
            appendLine("|true")
            appendLine()
            appendLine("|timeout")
            appendLine("|Execution timeout in seconds")
            appendLine("|30")
            appendLine()
            appendLine("|retries")
            appendLine("|Number of retry attempts")
            appendLine("|3")
            appendLine("|===")
            appendLine()
            appendLine("IMPORTANT: Always test configuration changes in a staging environment first.")
            appendLine()
            appendLine("=== Examples")
            appendLine()
            appendLine(".Example ${i}.1: Basic usage")
            appendLine("====")
            appendLine("[source,kotlin]")
            appendLine("----")
            appendLine("val feature = Feature$i()")
            appendLine("feature.execute()")
            appendLine("----")
            appendLine("====")
            appendLine()
        }

        appendLine("== Appendix A: Glossary")
        appendLine()
        appendLine("[glossary]")
        appendLine("AsciiDoc:: A lightweight markup language")
        appendLine("Parser:: Component that analyzes document structure")
        appendLine("Renderer:: Component that converts to HTML")
    }

    private fun generateLargeAsciidocDocument(): String = buildString {
        appendLine("= Comprehensive AsciiDoc Reference Manual")
        appendLine("Technical Writing Team <tech-writing@example.com>")
        appendLine("v3.0, 2025-11-19")
        appendLine(":doctype: book")
        appendLine(":toc: left")
        appendLine(":toclevels: 3")
        appendLine(":icons: font")
        appendLine(":source-highlighter: highlight.js")
        appendLine(":chapter-label:")
        appendLine()

        // Generate 20 chapters with 5 sections each
        for (chapter in 1..20) {
            appendLine("= Chapter $chapter: Advanced Topics")
            appendLine()
            appendLine("This chapter explores advanced AsciiDoc concepts and patterns.")
            appendLine()

            for (section in 1..5) {
                appendLine("== Section ${chapter}.$section: Topic Details")
                appendLine()
                appendLine("Detailed discussion of topic ${chapter}.$section.")
                appendLine()
                appendLine("=== Introduction to ${chapter}.$section")
                appendLine()
                appendLine("This topic covers important aspects of the system architecture.")
                appendLine()
                appendLine("Key points:")
                appendLine()
                appendLine("* Point ${chapter}.$section.1 - Architecture overview")
                appendLine("* Point ${chapter}.$section.2 - Design patterns")
                appendLine("* Point ${chapter}.$section.3 - Best practices")
                appendLine("* Point ${chapter}.$section.4 - Common pitfalls")
                appendLine("* Point ${chapter}.$section.5 - Performance optimization")
                appendLine()
                appendLine("=== Implementation Guide")
                appendLine()
                appendLine("[source,kotlin]")
                appendLine("----")
                appendLine("class Chapter${chapter}Section$section {")
                appendLine("    private val config = Configuration()")
                appendLine("    ")
                appendLine("    fun initialize() {")
                appendLine("        config.load(\"settings.properties\")")
                appendLine("        validateConfiguration()")
                appendLine("    }")
                appendLine("    ")
                appendLine("    fun process(input: String): Result {")
                appendLine("        // Process the input data")
                appendLine("        val transformed = transform(input)")
                appendLine("        return Result.success(transformed)")
                appendLine("    }")
                appendLine("}")
                appendLine("----")
                appendLine()
                appendLine("=== Configuration Reference")
                appendLine()
                appendLine("[cols=\"1,2,1,1\"]")
                appendLine("|===")
                appendLine("|Parameter |Description |Type |Default")
                appendLine()
                appendLine("|maxConnections")
                appendLine("|Maximum concurrent connections")
                appendLine("|Integer")
                appendLine("|100")
                appendLine()
                appendLine("|timeout")
                appendLine("|Request timeout in milliseconds")
                appendLine("|Long")
                appendLine("|5000")
                appendLine()
                appendLine("|retryPolicy")
                appendLine("|Retry strategy for failed requests")
                appendLine("|String")
                appendLine("|exponential")
                appendLine()
                appendLine("|enableCaching")
                appendLine("|Enable response caching")
                appendLine("|Boolean")
                appendLine("|true")
                appendLine("|===")
                appendLine()
                appendLine("TIP: Use environment-specific configuration files for better deployment management.")
                appendLine()
            }
        }
    }

    private fun generateComplexAsciidocDocument(): String = buildString {
        appendLine("= Complex AsciiDoc Features Demo")
        appendLine("Advanced User <advanced@example.com>")
        appendLine("v1.0, 2025-11-19")
        appendLine(":experimental:")
        appendLine(":icons: font")
        appendLine(":source-highlighter: highlight.js")
        appendLine()
        appendLine("== Document Attributes")
        appendLine()
        appendLine("This document demonstrates advanced AsciiDoc features:")
        appendLine()
        appendLine("* Admonitions (NOTE, TIP, IMPORTANT, WARNING, CAUTION)")
        appendLine("* Tables with complex formatting")
        appendLine("* Source code blocks with callouts")
        appendLine("* Include directives")
        appendLine("* Cross-references")
        appendLine("* Keyboard shortcuts and UI elements")
        appendLine()
        appendLine("== Admonitions")
        appendLine()
        appendLine("NOTE: This is a note admonition with important information.")
        appendLine()
        appendLine("TIP: Pro tips are highlighted with the TIP admonition.")
        appendLine()
        appendLine("IMPORTANT: Critical information uses IMPORTANT admonitions.")
        appendLine()
        appendLine("WARNING: Potential issues are marked with WARNING.")
        appendLine()
        appendLine("CAUTION: Dangerous operations require CAUTION admonitions.")
        appendLine()
        appendLine("== Complex Tables")
        appendLine()
        appendLine("[cols=\"1,2,1,1\",options=\"header\"]")
        appendLine("|===")
        appendLine("|ID |Feature |Status |Priority")
        appendLine()
        appendLine("|F001")
        appendLine("|User Authentication")
        appendLine("|Implemented")
        appendLine("|High")
        appendLine()
        appendLine("|F002")
        appendLine("|Data Encryption")
        appendLine("|In Progress")
        appendLine("|Critical")
        appendLine()
        appendLine("|F003")
        appendLine("|Export to PDF")
        appendLine("|Planned")
        appendLine("|Medium")
        appendLine("|===")
        appendLine()
        appendLine("== Source Code with Callouts")
        appendLine()
        appendLine("[source,kotlin]")
        appendLine("----")
        appendLine("class DataProcessor {")
        appendLine("    fun process(data: String): Result { // <1>")
        appendLine("        val validated = validate(data) // <2>")
        appendLine("        if (!validated) {")
        appendLine("            return Result.error(\"Invalid data\") // <3>")
        appendLine("        }")
        appendLine("        return Result.success(transform(data))")
        appendLine("    }")
        appendLine("}")
        appendLine("----")
        appendLine("<1> Entry point for data processing")
        appendLine("<2> Validate input before processing")
        appendLine("<3> Return error for invalid data")
        appendLine()
        appendLine("== Keyboard Shortcuts")
        appendLine()
        appendLine("Press kbd:[Ctrl+S] to save the document.")
        appendLine()
        appendLine("Use kbd:[Ctrl+Shift+P] to open the command palette.")
        appendLine()
        appendLine("== UI Elements")
        appendLine()
        appendLine("Click menu:File[Save As] to save with a new name.")
        appendLine()
        appendLine("Select menu:Edit[Preferences > Settings] to configure options.")
    }
}
