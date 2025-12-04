/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Markdown Parser Performance Benchmarks
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.markdown.MarkdownParser
import kotlinx.benchmark.*

/**
 * Performance benchmarks for MarkdownParser.
 *
 * This benchmark suite measures the performance of Markdown parsing across
 * different document sizes and complexity levels to establish baselines and
 * track optimization improvements.
 *
 * Benchmark Scenarios:
 * - Small document (1KB) - Simple content
 * - Medium document (10KB) - Typical README.md
 * - Large document (100KB) - Comprehensive documentation
 * - Complex document - Heavy markup with tables, lists, code blocks
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class MarkdownParserBenchmark {

    private lateinit var parser: MarkdownParser

    // Test content of various sizes
    private lateinit var smallContent: String      // ~1KB
    private lateinit var mediumContent: String     // ~10KB
    private lateinit var largeContent: String      // ~100KB
    private lateinit var complexContent: String    // Complex markup

    @Setup
    fun setup() {
        parser = MarkdownParser()

        // Small content: ~1KB simple Markdown
        smallContent = """
            # Hello World

            This is a **simple** Markdown document for testing performance.

            ## Features

            - Item 1
            - Item 2
            - Item 3

            Here's some inline `code` and a [link](https://example.com).

            > This is a blockquote with some text.

            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
        """.trimIndent()

        // Medium content: ~10KB typical README.md
        mediumContent = buildString {
            append("# Project Title\n\n")
            append("A comprehensive project with multiple sections and features.\n\n")

            for (section in 1..10) {
                append("## Section $section\n\n")
                append("This section contains detailed information about ")
                append("feature $section of the project. ")
                append("It includes **bold text**, *italic text*, and `inline code`.\n\n")

                append("### Subsection $section.1\n\n")
                append("Here's a list of important items:\n\n")
                for (item in 1..5) {
                    append("- Item $item: Description with some **formatting** and [links](https://example.com)\n")
                }
                append("\n")

                append("### Subsection $section.2\n\n")
                append("```kotlin\n")
                append("// Code example for section $section\n")
                append("fun example$section() {\n")
                append("    val data = \"Sample data\"\n")
                append("    println(data)\n")
                append("}\n")
                append("```\n\n")

                append("> Note: This is an important note about section $section.\n\n")
            }
        }

        // Large content: ~100KB comprehensive documentation
        largeContent = buildString {
            append("# Comprehensive Documentation\n\n")
            append("This document contains extensive documentation for testing parser performance.\n\n")

            for (chapter in 1..20) {
                append("# Chapter $chapter\n\n")

                for (section in 1..5) {
                    append("## Section $chapter.$section\n\n")

                    append("This section provides detailed information. ")
                    repeat(10) {
                        append("More content with **bold**, *italic*, and `code` formatting. ")
                    }
                    append("\n\n")

                    append("### List Items\n\n")
                    for (item in 1..10) {
                        append("- List item $item with [link](https://example.com/$chapter-$section-$item)\n")
                    }
                    append("\n")

                    append("### Code Examples\n\n")
                    append("```kotlin\n")
                    append("// Example code for section $chapter.$section\n")
                    repeat(15) { line ->
                        append("val variable$line = \"value$line\" // Comment\n")
                    }
                    append("```\n\n")

                    append("### Tables\n\n")
                    append("| Column 1 | Column 2 | Column 3 |\n")
                    append("|----------|----------|----------|\n")
                    repeat(5) { row ->
                        append("| Cell ${row}A | Cell ${row}B | Cell ${row}C |\n")
                    }
                    append("\n")
                }
            }
        }

        // Complex content: Heavy markup features
        complexContent = """
            # Complex Markdown Document

            ## Tables with Complex Content

            | Feature | Status | Description | Priority |
            |---------|--------|-------------|----------|
            | **Parser** | ✅ Done | Full *Markdown* support | High |
            | **Tables** | ✅ Done | GFM tables with `code` | High |
            | **Lists** | ✅ Done | Nested lists support | Medium |
            | **Links** | ✅ Done | [External](https://example.com) | High |

            ## Nested Lists

            1. First level item
               - Nested bullet 1
               - Nested bullet 2
                 - Deep nested item
                   - Even deeper
            2. Second level item
               1. Nested numbered 1
               2. Nested numbered 2
            3. Third level item

            ## Task Lists

            - [x] Completed task with **bold**
            - [x] Another completed task with *italic*
            - [ ] Pending task with `code`
            - [ ] Another pending task with [link](https://example.com)

            ## Complex Code Blocks

            ```kotlin
            /**
             * Documentation with **markdown** inside code
             */
            class Example {
                fun complexMethod(param: String): Result<String> {
                    return when {
                        param.isEmpty() -> Result.failure(Exception("Empty"))
                        param.length > 100 -> Result.success(param.substring(0, 100))
                        else -> Result.success(param)
                    }
                }
            }
            ```

            ## Blockquotes with Formatting

            > This is a blockquote with **bold** and *italic* text.
            > It also contains `inline code` and [links](https://example.com).
            >
            > > Nested blockquotes are also supported
            > > with multiple lines

            ## Links and Images

            Here are various [link types](https://example.com):
            - [Regular link](https://example.com/regular)
            - [Link with **bold** text](https://example.com/bold)
            - ![Image alt text](https://example.com/image.png)

            ## Horizontal Rules

            ---

            ## Emphasis Combinations

            This text has ***bold and italic*** together.
            This has **bold with *italic* inside**.
            This has ~~strikethrough~~ text.

            ---
        """.trimIndent()
    }

    /**
     * Benchmark parsing small Markdown document (~1KB).
     *
     * Target: < 10ms for 1KB document
     */
    @Benchmark
    fun parseSmallDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallContent, mapOf("filename" to "small.md"))
    }

    /**
     * Benchmark parsing medium Markdown document (~10KB).
     *
     * Target: < 50ms for 10KB document
     */
    @Benchmark
    fun parseMediumDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumContent, mapOf("filename" to "medium.md"))
    }

    /**
     * Benchmark parsing large Markdown document (~100KB).
     *
     * Target: < 500ms for 100KB document
     */
    @Benchmark
    fun parseLargeDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeContent, mapOf("filename" to "large.md"))
    }

    /**
     * Benchmark parsing complex Markdown with heavy markup.
     *
     * Target: < 100ms for complex features
     */
    @Benchmark
    fun parseComplexDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexContent, mapOf("filename" to "complex.md"))
    }

    /**
     * Benchmark HTML conversion (after parsing).
     *
     * Target: < 5ms for HTML conversion
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(mediumContent, mapOf("filename" to "medium.md"))
        return parser.toHtml(document, lightMode = true)
    }

    /**
     * Benchmark validation (syntax checking).
     *
     * Target: < 20ms for validation
     */
    @Benchmark
    fun validateDocument(): List<String> {
        return parser.validate(mediumContent)
    }
}
