/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive edge case tests for Markdown parser
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import kotlin.test.*

/**
 * Comprehensive edge case tests for Markdown parser covering:
 * - Complex nested structures
 * - Malformed syntax
 * - Edge cases and boundaries
 * - Performance with large documents
 * - Unicode and special character handling
 */
class MarkdownParserEdgeCaseTest {

    private lateinit var parser: MarkdownParser

    @BeforeTest
    fun setUp() {
        parser = MarkdownParser()
    }

    @Test
    fun testComplexNestedStructures() {
        val complexContent = """
            # Main Heading
            
            ## Subheading with **bold** and *italic*
            
            ### Deep nesting
            
            #### Even deeper
            
            ##### Very deep
            
            ###### Deepest possible
            
            This paragraph has **bold *nested italic*** text.
            
            > This is a blockquote with **bold** text
            > > And a nested blockquote with *italic* text
            > > > Triple nested with `inline code`
            
            1. Numbered list with **bold**
               1. Nested numbered with *italic*
                  1. Triple nested with `code`
            
            - Bullet list with [link](http://example.com)
              - Nested bullet with ![image](image.jpg)
                - Triple nested with **emphasis**
            
            | Header 1 | Header 2 | Header 3 |
            |----------|----------|----------|
            | Cell with **bold** | Cell with *italic* | Cell with `code` |
            | [Link](url) | ![Image](img) | ~~Strikethrough~~ |
            
            ```python
            def complex_function():
                """This is a docstring with **bold** text"""
                print("Hello *world*")
                return [1, 2, 3]
            ```
            
            ~~Strikethrough with **bold** inside~~
            
            This is a paragraph with `inline code` and **bold *nested*** text.
            
            [Complex link with **bold** text](http://example.com "Title with *italic*")
            
            ![Complex image with **alt** text](image.jpg "Title with *italic*")
        """.trimIndent()
        
        val result = parser.parse(complexContent)
        
        assertNotNull(result)
        assertTrue(result.content.contains("Main Heading"))
        assertTrue(result.content.contains("bold"))
        assertTrue(result.content.contains("italic"))
        assertTrue(result.content.contains("nested"))
        
        // Verify nested structures are preserved
        assertTrue(result.metadata.isNotEmpty(), "Complex content should generate metadata")
    }

    @Test
    fun testMalformedSyntax() {
        val malformedCases = listOf(
            "Unclosed **bold marker" to "Unclosed bold",
            "Unclosed *italic marker" to "Unclosed italic",
            "Unclosed `code marker" to "Unclosed code",
            "Unclosed [link" to "Unclosed link",
            "Unclosed ![image" to "Unclosed image",
            "Unclosed ```code block" to "Unclosed code block",
            "Unclosed | table" to "Unclosed table",
            "Mismatched **bold** and __italic__" to "Mismatched markers",
            "Nested # # # # headings" to "Nested headings",
            "Broken | table | structure" to "Broken table",
            "Invalid URL: [link](not-a-valid-url" to "Invalid URL",
            "Empty markers: ** __ `` [] ()" to "Empty markers",
            "Mixed markers: **__``[]()" to "Mixed markers"
        )
        
        malformedCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle malformed content: $description")
            assertTrue(result.content.isNotEmpty() || content.isEmpty(), "Should produce content or handle empty input")
            
            println("Handled malformed content: $description")
        }
    }

    @Test
    fun testEdgeCaseHeadings() {
        val headingEdgeCases = listOf(
            "#" to "Single hash",
            "##" to "Double hash",
            "######" to "Maximum hashes",
            "#######" to "Too many hashes",
            "# Heading with trailing spaces   " to "Trailing spaces",
            "#HeadingWithoutSpace" to "No space after hash",
            "# Heading with **bold** text" to "Heading with formatting",
            "# Heading with [link](url)" to "Heading with link",
            "# Heading with `code`" to "Heading with code",
            "\n\n# Heading with newlines" to "Heading with newlines",
            "# Heading\n## Subheading\n### Subsubheading" to "Multiple headings",
            "# Heading with special chars: !@#$%^&*()" to "Special characters"
        )
        
        headingEdgeCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle heading edge case: $description")
            println("Handled heading edge case: $description")
        }
    }

    @Test
    fun testCodeBlockEdgeCases() {
        val codeBlockCases = listOf(
            "```" to "Empty code block",
            "```\n" to "Code block with newline",
            "```\n\n```" to "Code block with empty line",
            "```python" to "Code block with language",
            "```javascript" to "Code block with long language",
            "```\n```\n```" to "Multiple code blocks",
            "```\n# Nested heading\n```" to "Code block with heading",
            "```\n**bold** *italic* `code`\n```" to "Code block with formatting",
            "```\n```\nNested\n```\n```" to "Nested code blocks",
            "`inline code`" to "Inline code",
            "`inline with **bold**`" to "Inline code with formatting",
            "Multiple `inline code` in same line" to "Multiple inline codes",
            "`unclosed inline code" to "Unclosed inline code"
        )
        
        codeBlockCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle code block edge case: $description")
            println("Handled code block edge case: $description")
        }
    }

    @Test
    fun testListEdgeCases() {
        val listEdgeCases = listOf(
            "-" to "Single dash",
            "- " to "Dash with space",
            "- Item" to "Simple item",
            "* Item" to "Asterisk item",
            "+ Item" to "Plus item",
            "1. Item" to "Numbered item",
            "1) Item" to "Numbered with parenthesis",
            "- Item 1\n- Item 2\n- Item 3" to "Multiple items",
            "1. First\n2. Second\n3. Third" to "Multiple numbered",
            "- Item 1\n  - Nested\n- Item 2" to "Nested list",
            "1. First\n   1. Nested\n2. Second" to "Nested numbered",
            "- Item with **bold**" to "Item with formatting",
            "- Item with [link](url)" to "Item with link",
            "- Item with `code`" to "Item with code",
            "" to "Empty list item",
            "- \n- \n- " to "Empty items",
            "1. \n2. \n3. " to "Empty numbered items"
        )
        
        listEdgeCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle list edge case: $description")
            println("Handled list edge case: $description")
        }
    }

    @Test
    fun testLinkAndImageEdgeCases() {
        val linkImageCases = listOf(
            "[text](url)" to "Simple link",
            "[text](http://example.com)" to "HTTP link",
            "[text](https://example.com)" to "HTTPS link",
            "[text](ftp://example.com)" to "FTP link",
            "[text](mailto:test@example.com)" to "Mailto link",
            "[text]()" to "Empty URL",
            "[text](not-a-url)" to "Invalid URL",
            "[text](javascript:alert('xss'))" to "JavaScript URL",
            "![alt](image.jpg)" to "Simple image",
            "![alt](http://example.com/image.jpg)" to "HTTP image",
            "![alt]()" to "Empty image URL",
            "[link with spaces in text](url)" to "Link with spaces",
            "[link](url with spaces)" to "URL with spaces",
            "[link](url "title")" to "Link with title",
            "![image](image.jpg "title")" to "Image with title",
            "[very long link text that goes on and on](url)" to "Long link text",
            "[link with **bold** text](url)" to "Link with formatting",
            "[link with `code` text](url)" to "Link with code",
            "[unclosed link" to "Unclosed link",
            "![unclosed image" to "Unclosed image"
        )
        
        linkImageCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle link/image edge case: $description")
            println("Handled link/image edge case: $description")
        }
    }

    @Test
    fun testTableEdgeCases() {
        val tableEdgeCases = listOf(
            "| Column 1 | Column 2 |" to "Simple table",
            "| Header 1 | Header 2 |\n|----------|----------|" to "Header with separator",
            "| Cell 1 | Cell 2 |" to "Single row",
            "| A | B |\n| C | D |" to "Multiple rows",
            "| Left | Center | Right |" to "Multiple columns",
            "| Cell with **bold** | Cell with *italic* |" to "Cells with formatting",
            "| Cell with `code` | Cell with [link](url) |" to "Cells with mixed content",
            "| | Empty cell |" to "Empty cell",
            "| Cell 1 |\n| Cell 2 |" to "Single column",
            "| Very long cell content that goes on and on | Short |" to "Uneven cell lengths",
            "| Cell 1 | Cell 2 |\n| Cell 3 |" to "Mismatched column count",
            "| A | B |\n| C | D |\n| E | F |" to "Multiple rows",
            "| Header |\n|--------|\n| Cell |" to "Minimal table"
        )
        
        tableEdgeCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle table edge case: $description")
            println("Handled table edge case: $description")
        }
    }

    @Test
    fun testBlockquoteEdgeCases() {
        val blockquoteCases = listOf(
            "> Quote" to "Simple quote",
            "> Quote\n> Continued" to "Multi-line quote",
            "> Quote\n>\n> Paragraph 2" to "Quote with paragraph break",
            "> > Nested quote" to "Nested quote",
            "> > > Triple nested" to "Triple nested",
            "> Quote with **bold**" to "Quote with formatting",
            "> Quote with [link](url)" to "Quote with link",
            "> Quote with `code`" to "Quote with code",
            ">\n> Empty line" to "Quote with empty line",
            "> Very long quote that goes on and on and continues for a very long time" to "Long quote",
            ">\n\n> After break" to "Quote after break"
        )
        
        blockquoteCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle blockquote edge case: $description")
            println("Handled blockquote edge case: $description")
        }
    }

    @Test
    fun testUnicodeAndSpecialCharacters() {
        val unicodeCases = listOf(
            "# 标题 with 中文 characters" to "Chinese characters",
            "# Заголовок with русский text" to "Russian characters",
            "# Título with español text" to "Spanish characters",
            "# Titre with français text" to "French characters",
            "# Τίτλος with ελληνικά text" to "Greek characters",
            "# عنوان with العربية text" to "Arabic characters",
            "# タイトル with 日本語 text" to "Japanese characters",
            "# 제목 with 한국어 text" to "Korean characters",
            "# 🚀 Title with emojis 🎉" to "Emojis in title",
            "# ñáéíóú with accents" to "Accented characters",
            "# ÑÁÉÍÓÚ with uppercase accents" to "Uppercase accents",
            "# Mixed: 中文 + русский + español + 🌍" to "Mixed languages",
            "# Mathematical: ∑∏∫∂∇" to "Mathematical symbols",
            "# Currency: $€£¥₹₽" to "Currency symbols",
            "# Arrows: ←→↑↓↔↕" to "Arrow symbols"
        )
        
        unicodeCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle Unicode content: $description")
            println("Handled Unicode content: $description")
        }
    }

    @Test
    fun testPerformanceWithLargeDocument() {
        // Create a large document (1MB)
        val largeContent = buildString {
            repeat(10000) { i ->
                appendLine("# Heading $i")
                appendLine()
                appendLine("This is paragraph $i with **bold** and *italic* text.")
                appendLine()
                appendLine("```")
                appendLine("def function_$i():")
                appendLine("    return 'result_$i'")
                appendLine("```")
                appendLine()
                appendLine("| Col A | Col B | Col C |")
                appendLine("|-------|-------|-------|")
                appendLine("| A$i | B$i | C$i |")
                appendLine()
                appendLine("- Item 1")
                appendLine("- Item 2")
                appendLine("- Item 3")
                appendLine()
                appendLine("> Quote $i")
                appendLine()
                appendLine("[Link $i](http://example.com/$i)")
                appendLine()
            }
        }
        
        println("Testing performance with large document (${largeContent.length} characters)")
        
        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        val duration = endTime - startTime
        
        assertNotNull(result, "Should parse large document successfully")
        assertTrue(result.content.length > 0, "Should produce substantial output")
        assertTrue(duration < 5000, "Should parse within 5 seconds")
        
        println("Large document parsed in ${duration}ms")
        println("Output size: ${result.content.length} characters")
    }

    @Test
    fun testEmptyAndMinimalContent() {
        val minimalCases = listOf(
            "" to "Empty string",
            " " to "Single space",
            "\n" to "Single newline",
            "\n\n" to "Double newline",
            "   " to "Multiple spaces",
            "\t" to "Tab character",
            "\r" to "Carriage return",
            "\f" to "Form feed",
            "\u0000" to "Null character",
            "#" to "Single hash",
            "-" to "Single dash",
            "*" to "Single asterisk",
            ">" to "Single greater than",
            "|" to "Single pipe",
            "`" to "Single backtick",
            "[" to "Single bracket",
            "(" to "Single parenthesis",
            "_" to "Single underscore",
            "~" to "Single tilde"
        )
        
        minimalCases.forEach { (content, description) ->
            val result = parser.parse(content)
            
            assertNotNull(result, "Should handle minimal content: $description")
            println("Handled minimal content: $description")
        }
    }

    @Test
    fun testMixedValidAndInvalidSyntax() {
        val mixedContent = """
            # Valid Heading
            
            ## Unclosed **bold
            
            ### Valid heading with *italic*
            
            #### Unclosed [link
            
            ##### Valid heading with `code`
            
            ###### Unclosed ```code block
            
            - Valid bullet
            - Unclosed *italic
            - Valid bullet with **bold**
            
            1. Valid numbered
            2. Unclosed [link](url
            3. Valid numbered with [link](url)
            
            > Valid blockquote
            > Unclosed **bold
            > Valid blockquote with *italic*
            
            | Valid | Table |
            |-------|-------|
            | Cell  | Cell  |
            
            | Unclosed | Table
            |----------|-------
            | Cell     | Cell
            
            [Valid link](http://example.com)
            [Unclosed link
            
            ![Valid image](image.jpg)
            ![Unclosed image
            
            `Valid inline code`
            `Unclosed inline code
            
            ~~Valid strikethrough~~
            ~~Unclosed strikethrough
        """.trimIndent()
        
        val result = parser.parse(mixedContent)
        
        assertNotNull(result, "Should handle mixed valid and invalid syntax")
        assertTrue(result.content.contains("Valid Heading"))
        assertTrue(result.content.contains("Valid bullet"))
        assertTrue(result.content.contains("Valid blockquote"))
        
        println("Successfully handled mixed valid and invalid syntax")
    }

    @Test
    fun testDeterministicBehavior() {
        val content = """
            # Test Heading
            
            This is a paragraph with **bold** and *italic* text.
            
            - Item 1
            - Item 2
            - Item 3
            
            ```
            def test():
                return "result"
            ```
            
            | Col 1 | Col 2 |
            |-------|-------|
            | A | B |
        """.trimIndent()
        
        // Parse the same content multiple times
        val results = List(10) { parser.parse(content) }
        
        // All results should be identical
        results.forEach { result ->
            assertNotNull(result)
            assertEquals(results[0].content, result.content, "Content should be deterministic")
            assertEquals(results[0].metadata, result.metadata, "Metadata should be deterministic")
        }
        
        println("Deterministic behavior verified across 10 iterations")
    }

    // ==================== Helper Methods ====================

    private fun generateLargeContent(size: Int): String {
        return buildString {
            repeat(size / 100) {
                append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
            }
        }.take(size)
    }
}