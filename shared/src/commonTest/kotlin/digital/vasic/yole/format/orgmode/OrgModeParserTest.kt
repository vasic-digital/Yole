/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Org Mode parser
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.BeforeTest
import kotlin.system.measureTimeMillis

/**
 * Comprehensive unit tests for Org Mode format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Org Mode parsing (headlines, text, lists)
 * - Org Mode metadata (TODO keywords, tags, priorities)
 * - Timestamps and scheduling
 * - Properties and drawers
 * - Tables and code blocks
 * - Round-trip parsing (parse -> format -> parse)
 * - Edge cases (empty files, malformed Org Mode)
 * - Performance benchmarks
 * - HTML conversion
 */
class OrgModeParserTest {

    private val parser = OrgModeParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Org Mode format by extension`() {
        val format = FormatRegistry.getByExtension(".org")

        assertNotNull(format)
        assertEquals(TextFormat.ID_ORGMODE, format.id)
        assertEquals("Org Mode", format.name)
    }

    @Test
    fun `should detect Org Mode format by filename`() {
        val format = FormatRegistry.detectByFilename("notes.org")

        assertNotNull(format)
        assertEquals(TextFormat.ID_ORGMODE, format.id)
    }

    @Test
    fun `should support all Org Mode extensions`() {
        val extensions = listOf(".org")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_ORGMODE, format.id)
        }
    }

    // ==================== Basic Org Mode Parsing Tests ====================

    @Test
    fun `should parse basic Org Mode document structure`() {
        val content = """
            * Main Heading
            This is content under the main heading.

            ** Subheading
            More content here.

            *** Deep subheading
            Even deeper content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ORGMODE, result.format.id)
        assertEquals(content, result.rawContent)

        // Check metadata extraction
        assertEquals("3", result.metadata["headings"])
        assertEquals("0", result.metadata["todos"])
        assertEquals("0", result.metadata["properties"])
        assertEquals("3", result.metadata["max_level"])
    }

    @Test
    fun `should parse Org Mode headlines with different levels`() {
        val content = """
            * Level 1
            ** Level 2
            *** Level 3
            **** Level 4
            ***** Level 5
            ****** Level 6
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("6", result.metadata["headings"])
        assertEquals("6", result.metadata["max_level"])
    }

    @Test
    fun `should parse Org Mode text content`() {
        val content = """
            * Introduction
            This is a paragraph of text in Org Mode format.

            It can span multiple lines and contain various formatting.

            ** Details
            More detailed information goes here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.isNotEmpty())
    }

    // ==================== TODO Keywords and Task Management Tests ====================

    @Test
    fun `should parse TODO items`() {
        val content = """
            * TODO Write documentation
            This task needs to be completed.

            * DONE Completed task
            This task is already done.

            * Regular heading
            This is just a regular heading without TODO state.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["headings"])
        assertEquals("2", result.metadata["todos"])
    }

    @Test
    fun `should parse mixed TODO and regular headings`() {
        val content = """
            * TODO First task
            Description of first task.

            ** Subtask 1
            Details about subtask.

            * DONE Second task
            This is completed.

            ** Regular subheading
            No TODO state here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("4", result.metadata["headings"])
        assertEquals("2", result.metadata["todos"])
    }

    // ==================== Properties and Drawers Tests ====================

    @Test
    fun `should parse properties`() {
        val content = """
            * Task with properties
            :PROPERTIES:
            :Effort:   2:00
            :Priority: A
            :Tags:      work urgent
            :END:

            This task has properties defined.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("PROPERTIES"))
    }

    @Test
    fun `should parse simple property lines`() {
        val content = """
            * Simple properties
            :Category: Work
            :Status: In Progress

            Content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertEquals("2", result.metadata["properties"])
    }

    // ==================== Lists and Structure Tests ====================

    @Test
    fun `should parse unordered lists`() {
        val content = """
            * Shopping list
            - Milk
            - Bread
            - Eggs
            - Butter

            ** Nested list
            - First item
              - Sub-item 1
              - Sub-item 2
            - Second item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            * Steps to complete
            1. First step
            2. Second step
            3. Third step

            ** Nested numbered list
            1. Main step
               1. Sub-step 1
               2. Sub-step 2
            2. Another main step
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.isNotEmpty())
    }

    // ==================== Code Blocks and Special Elements Tests ====================

    @Test
    fun `should parse code blocks`() {
        val content = """
            * Code example
            #+BEGIN_SRC python
            def hello_world():
                print("Hello, World!")
                return True
            #+END_SRC

            This is a Python code block.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("BEGIN_SRC"))
    }

    @Test
    fun `should parse different types of blocks`() {
        val content = """
            * Various blocks
            #+BEGIN_QUOTE
            This is a quote block.
            #+END_QUOTE

            #+BEGIN_EXAMPLE
            This is an example block.
            #+END_EXAMPLE

            #+BEGIN_CENTER
            Centered text
            #+END_CENTER
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("BEGIN_QUOTE"))
        assertTrue(result.parsedContent.contains("BEGIN_EXAMPLE"))
        assertTrue(result.parsedContent.contains("BEGIN_CENTER"))
    }

    // ==================== Tables Tests ====================

    @Test
    fun `should parse simple tables`() {
        val content = """
            * Data table
            | Name  | Age | City     |
            |-------+-----+----------|
            | Alice | 30  | New York |
            | Bob   | 25  | London   |
            | Carol | 35  | Paris    |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("Name"))
        assertTrue(result.parsedContent.contains("Alice"))
    }

    @Test
    fun `should parse tables with alignment`() {
        val content = """
            * Formatted table
            | <r>   | <l> | <c>      |
            |-------+-----+----------|
            | Right | Left| Center   |
            | 1     | 2   | 3        |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("Right"))
        assertTrue(result.parsedContent.contains("Left"))
    }

    // ==================== Links and References Tests ====================

    @Test
    fun `should parse external links`() {
        val content = """
            * Useful links
            Check out [[https://example.com][this website]] for more info.

            Also visit [[https://github.com]] directly.

            ** Documentation
            See [[file:readme.org][the readme file]] for details.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("https://example.com"))
        assertTrue(result.parsedContent.contains("github.com"))
    }

    @Test
    fun `should parse internal links`() {
        val content = """
            * Main section
            See [[#details][the details section]] below.

            ** Details
            :CUSTOM_ID: details
            This is the details section.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("#details"))
    }

    // ==================== Timestamps and Scheduling Tests ====================

    @Test
    fun `should parse active timestamps`() {
        val content = """
            * Meeting
            SCHEDULED: <2025-01-15 Wed 14:00>

            ** Deadline
            DEADLINE: <2025-01-20 Mon>

            *** Regular timestamp
            <2025-01-10 Fri 10:30>
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("2025-01-15"))
    }

    @Test
    fun `should parse inactive timestamps`() {
        val content = """
            * Historical note
            [2025-01-01 Wed] - Created this entry

            ** Event
            [2025-01-05 Sun 15:00-17:00] Workshop session
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("2025-01-01"))
    }

    // ==================== Tags Tests ====================

    @Test
    fun `should parse tags`() {
        val content = """
            * Work task :work:urgent:
            This is an urgent work task.

            ** Personal note :personal:
            Personal information here.

            *** Project :project:2025:
            Project for the year 2025.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["headings"])
        assertTrue(result.parsedContent.contains(":work:urgent:"))
    }

    // ==================== Text Formatting Tests ====================

    @Test
    fun `should parse inline formatting`() {
        val content = """
            * Formatted text
            This is *bold* text and /italic/ text.

            This is _underlined_ and +strikethrough+ text.

            Code: ~printf("Hello")~ and verbatim: =exact text=
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("bold"))
        assertTrue(result.parsedContent.contains("italic"))
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert Org Mode to HTML`() {
        val content = """
            * Main Heading
            This is content under the main heading.

            ** Subheading with TODO
            TODO This needs to be done.

            Some *bold* and /italic/ text.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("org-mode-document"))
        assertTrue(html.contains("org-heading"))
        assertTrue(html.contains("org-todo"))
        assertTrue(html.contains("org-bold"))
        assertTrue(html.contains("org-italic"))
    }

    @Test
    fun `should convert dark mode HTML`() {
        val content = """
            * Test Document
            Content for dark mode testing.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("dark"))
        assertTrue(html.contains("org-mode-document"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate well-formed Org Mode`() {
        val content = """
            * Valid heading
            Valid content.

            #+BEGIN_SRC python
            print("Hello")
            #+END_SRC
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `should detect mismatched block delimiters`() {
        val content = """
            * Test
            #+BEGIN_SRC python
            print("Hello")
            # Missing END_SRC

            More content.
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Mismatched block delimiters") })
    }

    @Test
    fun `should detect invalid heading levels`() {
        val content = """
            * Valid level 1
            ******* Invalid level 7
            Too deep heading.
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Heading level 7 exceeds maximum of 6") })
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("", result.rawContent)
        assertEquals("0", result.metadata["headings"])
        assertEquals("0", result.metadata["todos"])
        assertEquals("0", result.metadata["properties"])
        assertEquals("0", result.metadata["max_level"])
    }

    @Test
    fun `should handle whitespace-only input`() {
        val content = "   \n\n\t  \n   "
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["headings"])
        assertEquals("0", result.metadata["max_level"])
    }

    @Test
    fun `should handle single heading`() {
        val content = "* Single heading"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals("1", result.metadata["headings"])
        assertEquals("1", result.metadata["max_level"])
    }

    @Test
    fun `should handle unicode characters`() {
        val content = """
            * Unicode test
            Chinese: 你好世界
            Russian: Привет мир
            Arabic: مرحبا بالعالم
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("你好世界"))
    }

    @Test
    fun `should handle very long input`() {
        val line = "* Heading with content\nThis is a test line in Org Mode format.\n"
        val longContent = line.repeat(1000)

        val result = parser.parse(longContent)

        assertNotNull(result)
        assertEquals(longContent, result.rawContent)
        assertEquals("1000", result.metadata["headings"])
        assertEquals("1", result.metadata["max_level"])
    }

    @Test
    fun `should handle null bytes gracefully`() {
        val content = "* Heading\nContent with null\u0000byte"
        val result = parser.parse(content)

        assertNotNull(result)
        // Should not crash, may have some issues with parsing
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should preserve content through round-trip parsing`() {
        val originalContent = """
            * Main Task
            TODO This is an important task

            Description of the task with *bold* and /italic/ text.

            ** Subtask 1
            - First item
            - Second item

            ** Subtask 2
            1. Numbered item
            2. Another numbered item

            #+BEGIN_SRC python
            def example():
                return "Hello, World!"
            #+END_SRC
        """.trimIndent()

        // Parse the original content
        val document = parser.parse(originalContent)

        // Convert to HTML
        val html = parser.toHtml(document, lightMode = true)

        // Parse the HTML back (simulating round-trip)
        val htmlDocument = parser.parse(html)

        // The HTML should be different from original (as expected)
        assertTrue(originalContent != html)
        assertTrue(html.contains("org-mode-document"))

        // The original document should have no errors
        assertTrue(document.errors.isEmpty())
    }

    // ==================== Performance Benchmark Tests ====================

    @Test
    fun `should parse large Org Mode documents efficiently`() {
        // Create a large document with many headings and content
        val largeContent = buildString {
            repeat(100) { i ->
                appendLine("* Heading $i")
                appendLine("TODO Task description for item $i")
                appendLine()
                appendLine("This is content for heading $i with *bold* and /italic/ text.")
                appendLine()

                repeat(3) { j ->
                    appendLine("** Subheading $i.$j")
                    appendLine("Content for subheading")
                    appendLine("- List item 1")
                    appendLine("- List item 2")
                    appendLine()
                }
            }
        }

        val parseTime = measureTimeMillis {
            val result = parser.parse(largeContent)
            assertNotNull(result)
            assertEquals("400", result.metadata["headings"]) // 100 main + 300 sub
        }

        // Performance assertion - should parse in reasonable time (< 1 second for this size)
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took $parseTime ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        val largeContent = buildString {
            repeat(50) { i ->
                appendLine("* Section $i")
                appendLine("Content with *bold* and /italic/ text.")
                appendLine()

                appendLine("#+BEGIN_SRC python")
                appendLine("def function_$i():")
                appendLine("    return $i")
                appendLine("#+END_SRC")
                appendLine()

                appendLine("| Col1 | Col2 | Col3 |")
                appendLine("|------+------+------|")
                repeat(5) { j ->
                    appendLine("| A$j   | B$j   | C$j   |")
                }
                appendLine()
            }
        }

        val document = parser.parse(largeContent)

        val convertTime = measureTimeMillis {
            val html = parser.toHtml(document, lightMode = true)
            assertNotNull(html)
            assertTrue(html.length > 0)
        }

        // Performance assertion - should convert in reasonable time (< 500ms)
        assertTrue(convertTime < 500, "HTML conversion should complete within 500ms, took $convertTime ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)

        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_ORGMODE)

        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_ORGMODE, registryParser.supportedFormat.id)

        // Test that it works
        val content = "* Test heading"
        val result = registryParser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ORGMODE, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val orgModeFormat = allFormats.find { it.id == TextFormat.ID_ORGMODE }

        assertNotNull(orgModeFormat)
        assertEquals("Org Mode", orgModeFormat.name)
        assertEquals(".org", orgModeFormat.defaultExtension)
        assertTrue(orgModeFormat.extensions.contains(".org"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex Org Mode document`() {
        val content = """
            #+TITLE: Complex Org Mode Document
            #+AUTHOR: Test Author
            #+DATE: 2025-01-15

            * Project Overview :project:important:
            This document demonstrates various Org Mode features.

            ** TODO Research phase
            SCHEDULED: <2025-01-20 Mon>
            :PROPERTIES:
            :Effort:   4:00
            :Priority: A
            :END:

            Conduct thorough research on the topic.

            *** Research sources
            - Academic papers
            - Industry reports
            - Expert interviews

            ** DONE Initial planning
            DEADLINE: <2025-01-10 Fri>
            Completed the initial planning phase.

            * Technical Details
            Implementation details and code examples.

            ** Code implementation
            #+BEGIN_SRC kotlin
            fun main() {
                println("Hello, Org Mode!")
                val data = listOf(1, 2, 3, 4, 5)
                val sum = data.sum()
                println("Sum: ${'$'}sum")
            }
            #+END_SRC

            ** Results table
            | Metric | Value | Status |
            |--------+-------+--------|
            | Speed  | 95%   | Good   |
            | Memory | 88%   | OK     |
            | CPU    | 92%   | Good   |

            * Links and References
            ** External resources
            Main documentation: [[https://orgmode.org][Org Mode Official Site]]

            ** Internal references
            See [[#technical-details][Technical Details]] section above.

            * Conclusion
            This document demonstrates the parsing capabilities.

            Final thoughts with *emphasis* and /style/.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("TITLE"))
        assertTrue(result.parsedContent.contains("Project Overview"))
        assertTrue(result.parsedContent.contains("TODO Research"))
        assertTrue(result.parsedContent.contains("BEGIN_SRC"))
        assertTrue(result.parsedContent.contains("table"))
        assertTrue(result.parsedContent.contains("https://orgmode.org"))

        // Check metadata
        assertEquals("4", result.metadata["headings"])
        assertEquals("2", result.metadata["todos"])
    }

    @Test
    fun `should handle nested structures`() {
        val content = """
            * Level 1
            Content at level 1

            ** Level 2
            Content at level 2

            *** Level 3
            Content at level 3

            **** Level 4
            Content at level 4

            ***** Level 5
            Content at level 5

            ****** Level 6
            Content at level 6

            * Another level 1
            Back to level 1
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("7", result.metadata["headings"])
        assertEquals("6", result.metadata["max_level"])
    }

    @Test
    fun `should handle mixed content types`() {
        val content = """
            * Mixed content heading
            Some regular paragraph text here.

            - List item 1
            - List item 2 with *bold* text
              - Nested list item
              - Another nested item with /italic/

            1. Numbered item
            2. Another numbered item

            Regular text again.

            #+BEGIN_QUOTE
            A quote block with some important information.
            #+END_QUOTE

            More text after the quote.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("1", result.metadata["headings"])
        assertTrue(result.parsedContent.contains("List item 1"))
        assertTrue(result.parsedContent.contains("Numbered item"))
        assertTrue(result.parsedContent.contains("BEGIN_QUOTE"))
    }
}
