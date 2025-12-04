/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for Format System
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.*

/**
 * Integration tests for the complete format parsing system.
 *
 * Tests cover:
 * - End-to-end parsing workflows
 * - Format detection and selection
 * - Parser integration with FormatRegistry
 * - Cross-format compatibility
 * - Real-world document handling
 */
class FormatIntegrationTest {

    // ==================== Format Detection Integration Tests ====================

    @Test
    fun `should detect Markdown format from extension and parse`() {
        val format = FormatRegistry.detectByFilename("README.md")
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)

        val parser = MarkdownParser()
        val content = "# Hello World"
        val document = parser.parse(content, mapOf("filename" to "README.md"))

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("<h1>"))
    }

    @Test
    fun `should detect TodoTxt format from extension and parse`() {
        // TodoTxt shares .txt extension with plaintext, so detection by content is needed
        val content = "(A) Important task +project @context"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_TODOTXT, format.id)

        val parser = TodoTxtParser()
        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals("1", document.metadata["totalTasks"])
    }

    @Test
    fun `should detect CSV format from extension and parse`() {
        val format = FormatRegistry.detectByFilename("data.csv")
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)

        val parser = CsvParser()
        val content = "Name,Age\nJohn,30\nJane,25"
        val document = parser.parse(content)

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("John") || document.parsedContent.contains("table"))
    }

    @Test
    fun `should detect format by content patterns for Markdown`() {
        val content = """
            # Header

            This is **bold** and *italic*.
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `should detect format by content patterns for CSV`() {
        val content = """
            Name,Age,Email
            Alice,28,alice@example.com
            Bob,32,bob@example.com
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    // ==================== End-to-End Parsing Tests ====================

    @Test
    fun `should parse Markdown document end-to-end`() {
        val content = """
            # Project Documentation

            ## Overview
            This is a **sample** project.

            ## Features
            - Feature 1
            - Feature 2

            ```kotlin
            fun main() {}
            ```
        """.trimIndent()

        val parser = MarkdownParser()
        val document = parser.parse(content, mapOf("filename" to "README.md"))
        val html = parser.toHtml(document)

        assertNotNull(document)
        assertNotNull(html)
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<pre>"))
    }

    @Test
    fun `should parse TodoTxt document end-to-end`() {
        val content = """
            (A) Write documentation +docs @computer
            x Buy groceries @home
            (B) Review PR #123 +github @work due:2025-12-31
        """.trimIndent()

        val parser = TodoTxtParser()
        val document = parser.parse(content)
        val html = parser.toHtml(document)

        assertNotNull(document)
        assertNotNull(html)
        assertEquals("3", document.metadata["totalTasks"])
        assertEquals("1", document.metadata["completedTasks"])
        assertEquals("2", document.metadata["pendingTasks"])
        assertTrue(html.contains("todotxt"))
    }

    @Test
    fun `should parse CSV document end-to-end`() {
        val content = """
            Product,Price,Quantity
            Apple,1.50,100
            Banana,0.75,200
            Orange,2.00,150
        """.trimIndent()

        val parser = CsvParser()
        val document = parser.parse(content)
        val html = parser.toHtml(document)

        assertNotNull(document)
        assertNotNull(html)
        assertTrue(html.contains("Apple"))
        assertTrue(html.contains("1.50"))
    }

    // ==================== Cross-Format Handling Tests ====================

    @Test
    fun `should handle Markdown content in different files`() {
        val parser = MarkdownParser()

        val readme = parser.parse("# README", mapOf("filename" to "README.md"))
        val changelog = parser.parse("# Changelog", mapOf("filename" to "CHANGELOG.md"))
        val docs = parser.parse("# Documentation", mapOf("filename" to "docs.markdown"))

        assertNotNull(readme)
        assertNotNull(changelog)
        assertNotNull(docs)

        assertTrue(readme.parsedContent.contains("README"))
        assertTrue(changelog.parsedContent.contains("Changelog"))
        assertTrue(docs.parsedContent.contains("Documentation"))
    }

    @Test
    fun `should handle same content with different parsers`() {
        val content = "Line 1\nLine 2\nLine 3"

        val markdownParser = MarkdownParser()
        val plaintextParser = PlaintextParser()

        val markdownDoc = markdownParser.parse(content)
        val plaintextDoc = plaintextParser.parse(content)

        assertNotNull(markdownDoc)
        assertNotNull(plaintextDoc)

        // Both should preserve the content
        assertEquals(content, markdownDoc.rawContent)
        assertEquals(content, plaintextDoc.rawContent)

        // But HTML output might differ
        assertNotEquals(markdownDoc.parsedContent, plaintextDoc.parsedContent)
    }

    @Test
    fun `should handle format switching in same session`() {
        val markdownParser = MarkdownParser()
        val csvParser = CsvParser()
        val todoParser = TodoTxtParser()

        val mdDoc = markdownParser.parse("# Header")
        val csvDoc = csvParser.parse("A,B\n1,2")
        val todoDoc = todoParser.parse("(A) Task")

        assertNotNull(mdDoc)
        assertNotNull(csvDoc)
        assertNotNull(todoDoc)

        // Each parser should produce appropriate output
        assertTrue(mdDoc.parsedContent.contains("<h1>"))
        assertTrue(csvDoc.parsedContent.contains("A") || csvDoc.parsedContent.contains("table"))
        assertTrue(todoDoc.parsedContent.contains("todotxt") || todoDoc.parsedContent.contains("Task"))
    }

    // ==================== Real-World Document Tests ====================

    @Test
    fun `should parse real-world GitHub README`() {
        val content = """
            # Awesome Project

            [![Build Status](https://travis-ci.org/user/project.svg)](https://travis-ci.org/user/project)
            [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

            ## Description

            This is an **awesome** project that does *amazing* things.

            ## Installation

            ```bash
            npm install awesome-project
            ```

            ## Usage

            ```javascript
            const awesome = require('awesome-project');
            awesome.doSomething();
            ```

            ## API

            ### `doSomething(options)`

            Does something amazing.

            | Parameter | Type | Description |
            |-----------|------|-------------|
            | options   | Object | Configuration |

            ## License

            MIT © 2025
        """.trimIndent()

        val parser = MarkdownParser()
        val document = parser.parse(content, mapOf("filename" to "README.md"))

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("<h1>"))
        assertTrue(document.parsedContent.contains("<h2>"))
        assertTrue(document.parsedContent.contains("<table>"))
        assertTrue(document.parsedContent.contains("<pre>"))
    }

    @Test
    fun `should parse real-world TodoTxt workflow`() {
        val content = """
            (A) 2025-01-15 Setup development environment +setup @computer due:2025-01-20
            (B) 2025-01-15 Write project documentation +docs @computer due:2025-01-25
            x 2025-01-16 (C) 2025-01-15 Install dependencies +setup @computer
            (A) 2025-01-16 Review architecture design +planning @meeting due:2025-01-18
            x 2025-01-17 Create GitHub repository +setup @computer
            (B) 2025-01-17 Write unit tests +testing @computer due:2025-01-30
        """.trimIndent()

        val parser = TodoTxtParser()
        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals("6", document.metadata["totalTasks"])
        assertEquals("2", document.metadata["completedTasks"])
        assertEquals("4", document.metadata["pendingTasks"])

        val tasks = parser.parseAllTasks(content)
        assertEquals(6, tasks.size)

        // Check priorities
        val priorityA = tasks.filter { it.priority == 'A' }
        assertEquals(2, priorityA.size)

        // Check projects
        val setupTasks = tasks.filter { it.projects.contains("setup") }
        assertTrue(setupTasks.size >= 3)
    }

    @Test
    fun `should parse real-world CSV data export`() {
        val content = """
            "Product ID","Name","Price","Stock","Category"
            "001","Laptop","999.99","50","Electronics"
            "002","Mouse","29.99","200","Accessories"
            "003","Keyboard","79.99","150","Accessories"
            "004","Monitor","299.99","75","Electronics"
            "005","Headphones","149.99","100","Audio"
        """.trimIndent()

        val parser = CsvParser()
        val document = parser.parse(content)

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("Laptop") || document.parsedContent.contains("Electronics"))
        assertTrue(document.parsedContent.contains("999.99") || document.parsedContent.contains("table"))
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    fun `should handle empty content gracefully across all parsers`() {
        val markdownParser = MarkdownParser()
        val csvParser = CsvParser()
        val todoParser = TodoTxtParser()
        val plaintextParser = PlaintextParser()

        val mdDoc = markdownParser.parse("")
        val csvDoc = csvParser.parse("")
        val todoDoc = todoParser.parse("")
        val plainDoc = plaintextParser.parse("")

        assertNotNull(mdDoc)
        assertNotNull(csvDoc)
        assertNotNull(todoDoc)
        assertNotNull(plainDoc)
    }

    @Test
    fun `should handle binary content detection`() {
        val binaryContent = "Binary\u0000Content\u0000Here"

        val markdownParser = MarkdownParser()
        val document = markdownParser.parse(binaryContent)

        assertNotNull(document)
        // Should not crash on binary content
    }

    @Test
    fun `should handle very large documents`() {
        val largeMd = "# Header\n\n" + "Paragraph\n\n".repeat(1000)
        val parser = MarkdownParser()
        val document = parser.parse(largeMd)

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("<h1>"))
    }

    @Test
    fun `should handle malformed Markdown gracefully`() {
        val malformed = """
            # Unclosed [bracket
            **Unclosed bold
            ![Broken image(url)
        """.trimIndent()

        val parser = MarkdownParser()
        val document = parser.parse(malformed)

        assertNotNull(document)
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle malformed CSV gracefully`() {
        val malformed = """
            "Unclosed quote
            Missing,commas here
            Normal,Row
        """.trimIndent()

        val parser = CsvParser()
        val document = parser.parse(malformed)

        assertNotNull(document)
        assertNotNull(document.parsedContent)
    }

    // ==================== Validation Integration Tests ====================

    @Test
    fun `should validate Markdown and report errors`() {
        val invalidContent = """
            # Valid Header

            This has [unclosed bracket

            And (unclosed paren
        """.trimIndent()

        val parser = MarkdownParser()
        val errors = parser.validate(invalidContent)

        assertNotNull(errors)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("bracket") || it.contains("parentheses") })
    }

    @Test
    fun `should validate correct Markdown without errors`() {
        val validContent = """
            # Header

            This is [a link](https://example.com) and ![image](test.png).
        """.trimIndent()

        val parser = MarkdownParser()
        val errors = parser.validate(validContent)

        assertTrue(errors.isEmpty())
    }

    // ==================== Multi-Document Handling Tests ====================

    @Test
    fun `should handle multiple documents sequentially`() {
        val parser = MarkdownParser()
        val documents = mutableListOf<ParsedDocument>()

        repeat(10) { i ->
            val content = "# Document $i\n\nContent for document $i"
            val doc = parser.parse(content, mapOf("filename" to "doc$i.md"))
            documents.add(doc)
        }

        assertEquals(10, documents.size)
        documents.forEachIndexed { index, doc ->
            assertTrue(doc.parsedContent.contains("Document $index"))
        }
    }

    @Test
    fun `should handle concurrent-like format detection`() {
        val files = listOf(
            "README.md" to FormatRegistry.ID_MARKDOWN,
            "data.csv" to FormatRegistry.ID_CSV,
            "notes.txt" to FormatRegistry.ID_PLAINTEXT,
            "document.markdown" to FormatRegistry.ID_MARKDOWN
        )

        files.forEach { (filename, expectedId) ->
            val format = FormatRegistry.detectByFilename(filename)
            assertNotNull(format, "Should detect format for $filename")
            assertEquals(expectedId, format.id)
        }
    }

    // ==================== Metadata Handling Tests ====================

    @Test
    fun `should preserve metadata through parsing pipeline`() {
        val parser = MarkdownParser()
        val options = mapOf(
            "filename" to "test.md",
            "custom" to "value"
        )

        val document = parser.parse("# Test", options)

        assertNotNull(document.metadata)
        assertEquals(".md", document.metadata["extension"])
        assertNotNull(document.metadata["lines"])
    }

    @Test
    fun `should extract TodoTxt metadata correctly`() {
        val content = """
            (A) Task 1
            x Task 2
            (B) Task 3 due:2020-01-01
        """.trimIndent()

        val parser = TodoTxtParser()
        val document = parser.parse(content)

        assertEquals("3", document.metadata["totalTasks"])
        assertEquals("1", document.metadata["completedTasks"])
        assertEquals("2", document.metadata["pendingTasks"])
        assertEquals("1", document.metadata["overdueTasks"])
    }

    // ==================== HTML Generation Integration Tests ====================

    @Test
    fun `should generate valid HTML structure from Markdown`() {
        val content = """
            # Main Header
            ## Sub Header

            Paragraph with **bold** and *italic*.
        """.trimIndent()

        val parser = MarkdownParser()
        val document = parser.parse(content)
        val html = parser.toHtml(document)

        assertNotNull(html)
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("<strong>"))
        assertTrue(html.contains("<em>"))
    }

    @Test
    fun `should generate themed HTML output`() {
        val parser = MarkdownParser()
        val document = parser.parse("# Test")

        val lightHtml = parser.toHtml(document, lightMode = true)
        val darkHtml = parser.toHtml(document, lightMode = false)

        assertNotNull(lightHtml)
        assertNotNull(darkHtml)
        // Both should contain the header
        assertTrue(lightHtml.contains("<h1>"))
        assertTrue(darkHtml.contains("<h1>"))
    }

    // ==================== Performance Integration Tests ====================

    @Test
    fun `should handle rapid format switching`() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser()
        )

        repeat(100) {
            val parser = parsers.random()
            val content = "Test content $it"
            val document = parser.parse(content)
            assertNotNull(document)
        }
    }

    @Test
    fun `should handle format detection for many files`() {
        val extensions = listOf(".md", ".txt", ".csv", ".markdown", ".mdown", ".mkd")

        repeat(100) { i ->
            val ext = extensions.random()
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
        }
    }
}
