/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parsing Stress Tests
 *
 * Comprehensive stress tests for format parsers covering
 * concurrent parsing, large documents, and edge cases.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Stress tests for format parsers.
 * Tests concurrent parsing, large documents, and edge cases.
 */
class FormatParsingStressTest {

    // ==================== MARKDOWN STRESS TESTS ====================

    @Test
    fun `Markdown concurrent parsing`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = """
            # Heading 1

            This is a paragraph with **bold** and *italic* text.

            ## Heading 2

            - List item 1
            - List item 2
            - List item 3

            ```kotlin
            fun hello() = println("Hello")
            ```
        """.trimIndent()

        // Concurrent parsing of the same document
        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        assertTrue(results.all { it.format.id == FormatRegistry.ID_MARKDOWN })
        assertTrue(results.all { it.rawContent == content })
    }

    @Test
    fun `Markdown large document parsing`() = runBlocking<Unit> {
        val parser = MarkdownParser()

        // Generate a large document
        val largeContent = buildString {
            repeat(1000) { i ->
                appendLine("# Section $i")
                appendLine()
                appendLine("This is paragraph $i with some content.")
                appendLine()
                appendLine("- Item 1")
                appendLine("- Item 2")
                appendLine("- Item 3")
                appendLine()
            }
        }

        // Parse should complete without issues
        val result = parser.parse(largeContent)
        assertEquals(FormatRegistry.ID_MARKDOWN, result.format.id)
        assertEquals(largeContent, result.rawContent)
    }

    @Test
    fun `Markdown rapid HTML conversion`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Hello **World**"

        // Rapid HTML conversions
        val results = (1..200).map {
            async {
                val parseResult = parser.parse(content)
                parser.toHtml(parseResult)
            }
        }.awaitAll()

        assertTrue(results.all { it.contains("Hello") })
        assertTrue(results.all { it.contains("World") })
    }

    // ==================== CSV STRESS TESTS ====================

    @Test
    fun `CSV concurrent parsing`() = runBlocking<Unit> {
        val parser = CsvParser()
        val content = """
            name,age,city
            Alice,30,New York
            Bob,25,Los Angeles
            Charlie,35,Chicago
        """.trimIndent()

        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        assertTrue(results.all { it.format.id == FormatRegistry.ID_CSV })
    }

    @Test
    fun `CSV large table parsing`() = runBlocking<Unit> {
        val parser = CsvParser()

        // Generate a large CSV
        val largeContent = buildString {
            appendLine("id,name,value,description")
            repeat(10000) { i ->
                appendLine("$i,Item $i,${i * 100},Description for item $i")
            }
        }

        val result = parser.parse(largeContent)
        assertEquals(FormatRegistry.ID_CSV, result.format.id)
    }

    @Test
    fun `CSV wide table parsing`() = runBlocking<Unit> {
        val parser = CsvParser()

        // Generate a wide CSV (many columns)
        val headers = (1..100).joinToString(",") { "col$it" }
        val rows = (1..100).map { row ->
            (1..100).joinToString(",") { col -> "r${row}c$col" }
        }
        val content = listOf(headers, *rows.toTypedArray()).joinToString("\n")

        val result = parser.parse(content)
        assertEquals(FormatRegistry.ID_CSV, result.format.id)
    }

    // ==================== TODO.TXT STRESS TESTS ====================

    @Test
    fun `TodoTxt concurrent parsing`() = runBlocking<Unit> {
        val parser = TodoTxtParser()
        val content = """
            (A) Important task @work +project1
            (B) Less important task @home +project2
            x 2025-01-15 Completed task
            Regular task without priority
        """.trimIndent()

        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        assertTrue(results.all { it.format.id == FormatRegistry.ID_TODOTXT })
    }

    @Test
    fun `TodoTxt large task list parsing`() = runBlocking<Unit> {
        val parser = TodoTxtParser()

        // Generate a large todo list
        val priorities = listOf("A", "B", "C", "D")
        val contexts = listOf("@work", "@home", "@phone", "@computer")
        val projects = listOf("+project1", "+project2", "+project3")

        val largeContent = buildString {
            repeat(5000) { i ->
                val priority = priorities[i % priorities.size]
                val context = contexts[i % contexts.size]
                val project = projects[i % projects.size]
                appendLine("($priority) Task $i $context $project")
            }
        }

        val result = parser.parse(largeContent)
        assertEquals(FormatRegistry.ID_TODOTXT, result.format.id)
    }

    // ==================== LATEX STRESS TESTS ====================

    @Test
    fun `LaTeX concurrent parsing`() = runBlocking<Unit> {
        val parser = LatexParser()
        val content = """
            \documentclass{article}
            \begin{document}
            \section{Introduction}
            This is a simple LaTeX document.
            \end{document}
        """.trimIndent()

        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        assertTrue(results.all { it.format.id == FormatRegistry.ID_LATEX })
    }

    @Test
    fun `LaTeX complex document parsing`() = runBlocking<Unit> {
        val parser = LatexParser()

        val complexContent = buildString {
            appendLine("\\documentclass{article}")
            appendLine("\\usepackage{amsmath}")
            appendLine("\\begin{document}")
            repeat(500) { i ->
                appendLine("\\section{Section $i}")
                appendLine("Content for section $i.")
                appendLine("\\begin{equation}")
                appendLine("x_{$i} = \\frac{a_{$i}}{b_{$i}}")
                appendLine("\\end{equation}")
            }
            appendLine("\\end{document}")
        }

        val result = parser.parse(complexContent)
        assertEquals(FormatRegistry.ID_LATEX, result.format.id)
    }

    // ==================== ORG MODE STRESS TESTS ====================

    @Test
    fun `OrgMode concurrent parsing`() = runBlocking<Unit> {
        val parser = OrgModeParser()
        val content = """
            * Heading 1
            ** Subheading 1.1
            Some content here.
            *** Subheading 1.1.1
            More content.
            * Heading 2
            ** Subheading 2.1
        """.trimIndent()

        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        assertTrue(results.all { it.format.id == FormatRegistry.ID_ORGMODE })
    }

    @Test
    fun `OrgMode deep hierarchy parsing`() = runBlocking<Unit> {
        val parser = OrgModeParser()

        // Generate deep hierarchy
        val deepContent = buildString {
            repeat(100) { section ->
                appendLine("* Section $section")
                repeat(10) { sub1 ->
                    appendLine("** Subsection $section.$sub1")
                    repeat(5) { sub2 ->
                        appendLine("*** Subsubsection $section.$sub1.$sub2")
                        appendLine("Content at level 3")
                    }
                }
            }
        }

        val result = parser.parse(deepContent)
        assertEquals(FormatRegistry.ID_ORGMODE, result.format.id)
    }

    // ==================== FORMAT DETECTION STRESS TESTS ====================

    @Test
    fun `Rapid format detection`() = runBlocking<Unit> {
        val contents = listOf(
            "# Markdown" to "md",
            "name,age\nAlice,30" to "csv",
            "(A) Task @work" to "txt",
            "\\documentclass{article}" to "tex",
            "* Org heading" to "org"
        )

        val results = (1..200).flatMap { _ ->
            contents.map { (content, ext) ->
                async {
                    FormatRegistry.detectByContent(content) to ext
                }
            }
        }.awaitAll()

        // All detections should complete
        assertEquals(1000, results.size)
    }

    @Test
    fun `Extension detection stress`() = runBlocking<Unit> {
        val extensions = listOf(
            ".md", ".markdown", ".txt", ".csv", ".tex",
            ".org", ".adoc", ".rst", ".wiki", ".json"
        )

        val results = (1..100).flatMap { _ ->
            extensions.map { ext ->
                async {
                    FormatRegistry.getByExtension(ext)
                }
            }
        }.awaitAll()

        // All should complete without exceptions
        assertEquals(1000, results.size)
    }

    // ==================== EDGE CASE STRESS TESTS ====================

    @Test
    fun `Empty content parsing stress`() = runBlocking<Unit> {
        val parsers = listOf(
            MarkdownParser(),
            CsvParser(),
            TodoTxtParser(),
            LatexParser(),
            OrgModeParser()
        )

        val results = (1..100).flatMap { _ ->
            parsers.map { parser ->
                async {
                    parser.parse("")
                }
            }
        }.awaitAll()

        // All parsers should handle empty content
        assertEquals(500, results.size)
    }

    @Test
    fun `Single character content stress`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        val results = chars.flatMap { char ->
            (1..10).map {
                async {
                    parser.parse(char.toString())
                }
            }
        }.awaitAll()

        assertTrue(results.all { it.rawContent.length == 1 })
    }

    @Test
    fun `Unicode content stress`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val unicodeContents = listOf(
            "# 中文标题",
            "# Заголовок на русском",
            "# العنوان بالعربية",
            "# 日本語見出し",
            "# Κεφαλίδα στα ελληνικά",
            "# शीर्षक हिंदी में"
        )

        val results = (1..50).flatMap { _ ->
            unicodeContents.map { content ->
                async {
                    parser.parse(content)
                }
            }
        }.awaitAll()

        assertEquals(300, results.size)
        assertTrue(results.all { it.format.id == FormatRegistry.ID_MARKDOWN })
    }

    @Test
    fun `Special characters stress`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val specialContent = """
            # Special Characters Test

            Symbols: !@#$%^&*()_+-=[]{}|;':",.<>?/`~

            Escaped: \* \_ \# \[ \] \( \)

            HTML entities: &amp; &lt; &gt; &quot;
        """.trimIndent()

        val results = (1..100).map {
            async {
                parser.parse(specialContent)
            }
        }.awaitAll()

        assertTrue(results.all { it.rawContent.contains("Special Characters") })
    }

    // ==================== MEMORY STRESS TESTS ====================

    @Test
    fun `Repeated parsing without memory leak`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Test\n\nParagraph with content."

        // Parse many times - should not cause memory issues
        repeat(1000) {
            val result = parser.parse(content)
            assertEquals(FormatRegistry.ID_MARKDOWN, result.format.id)
        }
    }

    @Test
    fun `Large content repeated parsing`() = runBlocking<Unit> {
        val parser = CsvParser()

        val largeContent = buildString {
            appendLine("a,b,c")
            repeat(1000) { i ->
                appendLine("$i,${i*2},${i*3}")
            }
        }

        // Parse large content multiple times
        repeat(50) {
            val result = parser.parse(largeContent)
            assertEquals(FormatRegistry.ID_CSV, result.format.id)
        }
    }
}
