/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Org Mode Parser Performance Benchmarks
 * Establishes baseline performance metrics for Org Mode parsing
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.orgmode.OrgModeParser
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Performance benchmarks for Org Mode parser.
 *
 * Tests parser performance across different document sizes and complexity levels.
 *
 * Performance Targets:
 * - Small documents (~2KB): < 25ms
 * - Medium documents (~20KB): < 120ms
 * - Large documents (~200KB): < 1200ms
 * - Complex documents: < 60ms
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class OrgModeParserBenchmark {

    private val parser = OrgModeParser()

    // Test data
    private lateinit var smallDocument: String
    private lateinit var mediumDocument: String
    private lateinit var largeDocument: String
    private lateinit var complexDocument: String

    @Setup
    fun setup() {
        // Small Org Mode document (~2KB)
        smallDocument = generateSmallOrgDocument()

        // Medium Org Mode document (~20KB) - typical org file
        mediumDocument = generateMediumOrgDocument()

        // Large Org Mode document (~200KB) - comprehensive notes
        largeDocument = generateLargeOrgDocument()

        // Complex Org Mode document with advanced features
        complexDocument = generateComplexOrgDocument()
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

    private fun generateSmallOrgDocument(): String = buildString {
        appendLine("#+TITLE: Simple Org Document")
        appendLine("#+AUTHOR: User Name")
        appendLine("#+DATE: 2025-11-19")
        appendLine()
        appendLine("* Introduction")
        appendLine()
        appendLine("This is a simple Org Mode document.")
        appendLine()
        appendLine("** Features")
        appendLine()
        appendLine("- First feature")
        appendLine("- Second feature")
        appendLine("- Third feature")
        appendLine()
        appendLine("* Tasks")
        appendLine()
        appendLine("** TODO Write documentation")
        appendLine("   SCHEDULED: <2025-11-20 Wed>")
        appendLine()
        appendLine("** DONE Complete implementation")
        appendLine("   CLOSED: [2025-11-19 Tue 14:30]")
        appendLine()
        appendLine("* Code Example")
        appendLine()
        appendLine("#+BEGIN_SRC kotlin")
        appendLine("fun main() {")
        appendLine("    println(\"Hello, Org Mode!\")")
        appendLine("}")
        appendLine("#+END_SRC")
    }

    private fun generateMediumOrgDocument(): String = buildString {
        appendLine("#+TITLE: Project Management with Org Mode")
        appendLine("#+AUTHOR: Project Team")
        appendLine("#+DATE: 2025-11-19")
        appendLine("#+STARTUP: overview")
        appendLine("#+TAGS: IMPORTANT(i) URGENT(u) PROJECT(p)")
        appendLine()

        // Generate 10 major sections
        for (i in 1..10) {
            appendLine("* Section $i: Feature Development")
            appendLine()
            appendLine("This section tracks development of feature $i.")
            appendLine()
            appendLine("** Overview")
            appendLine()
            appendLine("Feature $i provides:")
            appendLine()
            appendLine("- Capability ${i}.1")
            appendLine("- Capability ${i}.2")
            appendLine("- Capability ${i}.3")
            appendLine("- Capability ${i}.4")
            appendLine()
            appendLine("** Tasks")
            appendLine()
            appendLine("*** TODO Design feature $i architecture")
            appendLine("    SCHEDULED: <2025-11-20 Wed>")
            appendLine("    :PROPERTIES:")
            appendLine("    :EFFORT: 4:00")
            appendLine("    :END:")
            appendLine()
            appendLine("*** IN-PROGRESS Implement core functionality")
            appendLine("    :LOGBOOK:")
            appendLine("    CLOCK: [2025-11-19 Tue 09:00]--[2025-11-19 Tue 12:00] =>  3:00")
            appendLine("    :END:")
            appendLine()
            appendLine("*** DONE Research requirements")
            appendLine("    CLOSED: [2025-11-18 Mon 16:00]")
            appendLine("    :PROPERTIES:")
            appendLine("    :EFFORT: 2:00")
            appendLine("    :ACTUAL: 2:30")
            appendLine("    :END:")
            appendLine()
            appendLine("** Implementation Notes")
            appendLine()
            appendLine("#+BEGIN_SRC kotlin")
            appendLine("class Feature$i {")
            appendLine("    fun execute() {")
            appendLine("        // Implementation")
            appendLine("        println(\"Feature $i executing\")")
            appendLine("    }")
            appendLine("}")
            appendLine("#+END_SRC")
            appendLine()
            appendLine("** Configuration")
            appendLine()
            appendLine("| Option   | Value | Description      |")
            appendLine("|----------+-------+------------------|")
            appendLine("| enabled  | true  | Feature enabled  |")
            appendLine("| timeout  | 30    | Timeout seconds  |")
            appendLine("| retries  | 3     | Retry attempts   |")
            appendLine()
            appendLine("** References")
            appendLine()
            appendLine("- [[https://example.com/feature-$i][Feature $i Spec]]")
            appendLine("- [[file:design-doc-$i.org][Design Document]]")
            appendLine()
        }

        appendLine("* Archive")
        appendLine()
        appendLine("Completed items are archived here.")
    }

    private fun generateLargeOrgDocument(): String = buildString {
        appendLine("#+TITLE: Comprehensive Project Documentation")
        appendLine("#+AUTHOR: Documentation Team")
        appendLine("#+DATE: 2025-11-19")
        appendLine("#+OPTIONS: toc:3 num:t")
        appendLine("#+STARTUP: showeverything")
        appendLine("#+TAGS: FEATURE(f) BUG(b) ENHANCEMENT(e) DOCUMENTATION(d)")
        appendLine()

        // Generate 20 chapters with 5 sections each
        for (chapter in 1..20) {
            appendLine("* Chapter $chapter: Advanced Topics")
            appendLine("  :PROPERTIES:")
            appendLine("  :CUSTOM_ID: chapter-$chapter")
            appendLine("  :END:")
            appendLine()

            for (section in 1..5) {
                appendLine("** Section ${chapter}.$section: Topic Details")
                appendLine()
                appendLine("*** Overview")
                appendLine()
                appendLine("Detailed discussion of topic ${chapter}.$section.")
                appendLine()
                appendLine("**** Key Points")
                appendLine()
                appendLine("- Point ${chapter}.$section.1 - Architecture")
                appendLine("- Point ${chapter}.$section.2 - Design patterns")
                appendLine("- Point ${chapter}.$section.3 - Best practices")
                appendLine("- Point ${chapter}.$section.4 - Common issues")
                appendLine("- Point ${chapter}.$section.5 - Optimization")
                appendLine()
                appendLine("*** Implementation")
                appendLine()
                appendLine("#+BEGIN_SRC kotlin")
                appendLine("class Chapter${chapter}Section$section {")
                appendLine("    private val config = Configuration()")
                appendLine("    ")
                appendLine("    fun initialize() {")
                appendLine("        config.load(\"settings.properties\")")
                appendLine("    }")
                appendLine("    ")
                appendLine("    fun process(input: String): Result {")
                appendLine("        val transformed = transform(input)")
                appendLine("        return Result.success(transformed)")
                appendLine("    }")
                appendLine("}")
                appendLine("#+END_SRC")
                appendLine()
                appendLine("*** Configuration Table")
                appendLine()
                appendLine("| Parameter       | Type    | Default     | Description               |")
                appendLine("|-----------------+---------+-------------+---------------------------|")
                appendLine("| maxConnections  | Integer | 100         | Max concurrent connections |")
                appendLine("| timeout         | Long    | 5000        | Request timeout (ms)       |")
                appendLine("| retryPolicy     | String  | exponential | Retry strategy            |")
                appendLine("| enableCaching   | Boolean | true        | Enable response cache     |")
                appendLine()
                appendLine("*** Tasks")
                appendLine()
                appendLine("**** TODO Implement feature ${chapter}.$section")
                appendLine("     SCHEDULED: <2025-11-20 Wed>")
                appendLine("     :PROPERTIES:")
                appendLine("     :EFFORT: 8:00")
                appendLine("     :PRIORITY: A")
                appendLine("     :END:")
                appendLine()
                appendLine("**** IN-PROGRESS Write tests")
                appendLine("     :LOGBOOK:")
                appendLine("     CLOCK: [2025-11-19 Tue 10:00]--[2025-11-19 Tue 12:00] =>  2:00")
                appendLine("     :END:")
                appendLine()
                appendLine("**** DONE Research approach")
                appendLine("     CLOSED: [2025-11-18 Mon 15:00]")
                appendLine()
            }
        }
    }

    private fun generateComplexOrgDocument(): String = buildString {
        appendLine("#+TITLE: Complex Org Mode Features")
        appendLine("#+AUTHOR: Advanced User")
        appendLine("#+DATE: 2025-11-19")
        appendLine("#+PROPERTY: header-args :results output")
        appendLine("#+STARTUP: indent")
        appendLine("#+TAGS: @WORK(w) @HOME(h) @COMPUTER(c)")
        appendLine()
        appendLine("* Org Mode Advanced Features Demo")
        appendLine()
        appendLine("This document demonstrates complex Org Mode features:")
        appendLine()
        appendLine("- Properties and drawers")
        appendLine("- Clocking and time tracking")
        appendLine("- Tags and priorities")
        appendLine("- Links and cross-references")
        appendLine("- Source code blocks with results")
        appendLine("- Complex tables with formulas")
        appendLine()
        appendLine("* Tasks with Properties")
        appendLine()
        appendLine("** TODO [#A] High priority task                           :IMPORTANT:URGENT:")
        appendLine("   SCHEDULED: <2025-11-20 Wed 09:00-10:00>")
        appendLine("   :PROPERTIES:")
        appendLine("   :EFFORT: 1:00")
        appendLine("   :CATEGORY: Development")
        appendLine("   :CREATED: [2025-11-19 Tue 10:00]")
        appendLine("   :ID: task-001")
        appendLine("   :END:")
        appendLine("   :LOGBOOK:")
        appendLine("   CLOCK: [2025-11-19 Tue 10:30]--[2025-11-19 Tue 11:30] =>  1:00")
        appendLine("   CLOCK: [2025-11-19 Tue 14:00]--[2025-11-19 Tue 15:30] =>  1:30")
        appendLine("   :END:")
        appendLine()
        appendLine("** DONE [#B] Completed task                                      :PROJECT:")
        appendLine("   CLOSED: [2025-11-19 Tue 16:00]")
        appendLine("   :PROPERTIES:")
        appendLine("   :EFFORT: 2:00")
        appendLine("   :ACTUAL: 1:45")
        appendLine("   :END:")
        appendLine()
        appendLine("* Code Blocks with Results")
        appendLine()
        appendLine("#+BEGIN_SRC kotlin :exports both")
        appendLine("fun fibonacci(n: Int): Int {")
        appendLine("    return when {")
        appendLine("        n <= 1 -> n")
        appendLine("        else -> fibonacci(n - 1) + fibonacci(n - 2)")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("println(\"Fibonacci(10) = \${fibonacci(10)}\")")
        appendLine("#+END_SRC")
        appendLine()
        appendLine("#+RESULTS:")
        appendLine(": Fibonacci(10) = 55")
        appendLine()
        appendLine("* Tables with Formulas")
        appendLine()
        appendLine("| Item       | Quantity | Price | Total |")
        appendLine("|------------+----------+-------+-------|")
        appendLine("| Widget A   |       10 | 15.50 |   155 |")
        appendLine("| Widget B   |       25 | 8.25  | 206.25 |")
        appendLine("| Widget C   |       15 | 12.00 |   180 |")
        appendLine("|------------+----------+-------+-------|")
        appendLine("| *Total*    |          |       | 541.25 |")
        appendLine("#+TBLFM: @2$4=$2*$3::@3$4=$2*$3::@4$4=$2*$3::@5$4=vsum(@2$4..@4$4)")
        appendLine()
        appendLine("* Links and References")
        appendLine()
        appendLine("- Internal link: [[*Tasks with Properties][Jump to tasks]]")
        appendLine("- External link: [[https://orgmode.org][Org Mode website]]")
        appendLine("- File link: [[file:~/documents/notes.org][My notes]]")
        appendLine("- ID link: [[#task-001][Task 001]]")
        appendLine()
        appendLine("* Agenda Views")
        appendLine()
        appendLine("** Scheduled Items")
        appendLine("   :PROPERTIES:")
        appendLine("   :CATEGORY: Agenda")
        appendLine("   :END:")
        appendLine()
        appendLine("Items scheduled for today and upcoming days.")
    }
}
