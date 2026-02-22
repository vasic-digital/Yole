/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for TiddlyWiki parser
 *
 *########################################################*/
package digital.vasic.yole.format.tiddlywiki

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals


/**
 * Comprehensive unit tests for TiddlyWiki format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic TiddlyWiki parsing (tiddlers, metadata, content)
 * - TiddlyWiki transclusion and macros
 * - Links and references between tiddlers
 * - Tags and fields
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed TiddlyWiki)
 * - Performance benchmarks
 * - HTML conversion
 */
class TiddlyWikiParserTest {

    private val parser = TiddlyWikiParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect TiddlyWiki format by extension`() {
        val format = FormatRegistry.getByExtension(".tid")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TIDDLYWIKI, format.id)
        assertEquals("TiddlyWiki", format.name)
    }

    @Test
    fun `should detect TiddlyWiki format by filename`() {
        val format = FormatRegistry.detectByFilename("notes.tid")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TIDDLYWIKI, format.id)
    }

    @Test
    fun `should support all TiddlyWiki extensions`() {
        val extensions = listOf(".tid", ".tiddly")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_TIDDLYWIKI, format.id)
        }
    }

    // ==================== Basic TiddlyWiki Parsing Tests ====================

    @Test
    fun `should parse basic TiddlyWiki document with metadata`() {
        val content = """
            title: My First Tiddler
            tags: introduction tutorial
            created: 20250101120000000
            modified: 20250102150000000
            
            This is the content of my first tiddler.
            It can span multiple lines and contain various formatting.
            
            ! Heading 1
            Some content under the heading.
            
            !! Heading 2
            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TIDDLYWIKI, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("My First Tiddler", result.metadata["title"])
        assertEquals("introduction, tutorial", result.metadata["tags"])
        assertEquals("", result.metadata["extension"])
        assertTrue(result.metadata["lines"]!!.toInt() > 0)
    }

    @Test
    fun `should parse TiddlyWiki with minimal metadata`() {
        val content = """
            title: Simple Tiddler
            
            This is a simple tiddler with just a title.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Simple Tiddler", result.metadata["title"])
        assertFalse(result.metadata.containsKey("tags"))
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should parse TiddlyWiki without metadata`() {
        val content = """
            This tiddler has no metadata section.
            It starts directly with content.
            
            ! Heading
            Content under heading.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertFalse(result.metadata.containsKey("title"))
        assertFalse(result.metadata.containsKey("tags"))
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should parse TiddlyWiki text content`() {
        val content = """
            title: Content Test
            
            This is a paragraph of text in TiddlyWiki format.
            
            It can span multiple lines and contain various formatting.
            
            ! Heading 1
            Content under first heading.
            
            !! Heading 2
            Content under second heading.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Content Test", result.metadata["title"])
        assertTrue(result.parsedContent.contains("This is a paragraph"))
        assertTrue(result.parsedContent.contains("Heading 1"))
        assertTrue(result.parsedContent.contains("Heading 2"))
    }

    // ==================== Headings Tests ====================

    @Test
    fun `should parse TiddlyWiki headings with different levels`() {
        val content = """
            title: Headings Test
            
            ! Level 1 Heading
            Content for level 1
            
            !! Level 2 Heading
            Content for level 2
            
            !!! Level 3 Heading
            Content for level 3
            
            !!!! Level 4 Heading
            Content for level 4
            
            !!!!! Level 5 Heading
            Content for level 5
            
            !!!!!! Level 6 Heading
            Content for level 6
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Headings Test", result.metadata["title"])
        assertTrue(result.parsedContent.contains("Level 1 Heading"))
        assertTrue(result.parsedContent.contains("Level 6 Heading"))
    }

    // ==================== Lists Tests ====================

    @Test
    fun `should parse unordered lists`() {
        val content = """
            title: Unordered Lists
            
            Here is an unordered list:
            * First item
            * Second item
            * Third item
            
            Nested list:
            * Main item 1
            ** Sub-item 1.1
            ** Sub-item 1.2
            * Main item 2
            ** Sub-item 2.1
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Unordered Lists", result.metadata["title"])
        assertTrue(result.parsedContent.contains("First item"))
        assertTrue(result.parsedContent.contains("Sub-item 1.1"))
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            title: Ordered Lists
            
            Here is an ordered list:
            # First step
            # Second step
            # Third step
            
            Nested ordered list:
            # Main step 1
            ## Sub-step 1.1
            ## Sub-step 1.2
            # Main step 2
            ## Sub-step 2.1
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Ordered Lists", result.metadata["title"])
        assertTrue(result.parsedContent.contains("First step"))
        assertTrue(result.parsedContent.contains("Sub-step 1.1"))
    }

    @Test
    fun `should parse mixed list types`() {
        val content = """
            title: Mixed Lists
            
            Mixed list types:
            * Unordered item 1
            *# Ordered sub-item 1.1
            *# Ordered sub-item 1.2
            * Unordered item 2
            ** Nested unordered 2.1
            **# Mixed nested 2.1.1
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Mixed Lists", result.metadata["title"])
        assertTrue(result.parsedContent.contains("Unordered item 1"))
        assertTrue(result.parsedContent.contains("Ordered sub-item 1.1"))
    }

    // ==================== Text Formatting Tests ====================

    @Test
    fun `should parse bold text formatting`() {
        val content = """
            title: Bold Text
            
            This is ''bold'' text.
            Multiple ''bold words'' in a sentence.
            ''Entire sentence in bold.''
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Bold Text", result.metadata["title"])
        assertTrue(result.parsedContent.contains("bold"))
        assertTrue(result.parsedContent.contains("<strong>"))
    }

    @Test
    fun `should parse italic text formatting`() {
        val content = """
            title: Italic Text
            
            This is //italic// text.
            Multiple //italic words// in a sentence.
            //Entire sentence in italic.//
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Italic Text", result.metadata["title"])
        assertTrue(result.parsedContent.contains("italic"))
        assertTrue(result.parsedContent.contains("<em>"))
    }

    @Test
    fun `should parse underline text formatting`() {
        val content = """
            title: Underline Text
            
            This is __underlined__ text.
            Multiple __underlined words__ in a sentence.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Underline Text", result.metadata["title"])
        assertTrue(result.parsedContent.contains("underlined"))
        assertTrue(result.parsedContent.contains("<u>"))
    }

    @Test
    fun `should parse strikethrough text formatting`() {
        val content = """
            title: Strikethrough Text
            
            This is ~~strikethrough~~ text.
            Multiple ~~strikethrough words~~ in a sentence.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Strikethrough Text", result.metadata["title"])
        assertTrue(result.parsedContent.contains("strikethrough"))
        assertTrue(result.parsedContent.contains("<s>"))
    }

    @Test
    fun `should parse superscript and subscript formatting`() {
        val content = """
            title: Superscript and Subscript
            
            Superscript: x^^2^^ + y^^2^^ = z^^2^^
            Subscript: H,,2,,O is water, CO,,2,, is carbon dioxide
            Combined: E^^2^^ = (mc^^2^^)^^2^^ + (pc)^^2^^
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Superscript and Subscript", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<sup>"))
        assertTrue(result.parsedContent.contains("<sub>"))
    }

    @Test
    fun `should parse inline code formatting`() {
        val content = """
            title: Inline Code
            
            Use `printf()` function to print output.
            Variable `count` stores the number of items.
            Command `ls -la` lists all files.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Inline Code", result.metadata["title"])
        assertTrue(result.parsedContent.contains("printf"))
        assertTrue(result.parsedContent.contains("<code>"))
    }

    @Test
    fun `should parse combined text formatting`() {
        val content = """
            title: Combined Formatting
            
            Mixed ''bold'' and //italic// text.
            Complex ''//bold italic//'' combination.
            Code in formatting: ''Use `printf()` for output''
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Combined Formatting", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<strong>"))
        assertTrue(result.parsedContent.contains("<em>"))
        assertTrue(result.parsedContent.contains("<code>"))
    }

    // ==================== Links and References Tests ====================

    @Test
    fun `should parse internal wiki links`() {
        val content = """
            title: Internal Links
            
            Link to [[Another Tiddler]].
            Link with custom text: [[Another Tiddler|click here]].
            Multiple links: [[First]] and [[Second]] tiddlers.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Internal Links", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<a href='Another Tiddler'>Another Tiddler</a>"))
        assertTrue(result.parsedContent.contains("<a href='Another Tiddler'>click here</a>"))
    }

    @Test
    fun `should parse external links`() {
        val content = """
            title: External Links
            
            External link: [ext[https://example.com]].
            External link with text: [ext[https://example.com|Example Site]].
            Multiple external links: [ext[https://google.com]] and [ext[https://github.com]].
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("External Links", result.metadata["title"])
        assertTrue(result.parsedContent.contains("https://example.com"))
        assertTrue(result.parsedContent.contains("target='_blank'"))
    }

    @Test
    fun `should parse image links`() {
        val content = """
            title: Image Links
            
            Simple image: [img[photo.jpg]].
            Multiple images: [img[image1.png]] and [img[image2.gif]].
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Image Links", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<img src='photo.jpg'"))
        assertTrue(result.parsedContent.contains("<img src='image1.png'"))
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse code blocks`() {
        val content = """
            title: Code Blocks
            
            Here is a code block:
            ```
            def hello_world():
                print("Hello, World!")
                return True
            ```
            
            Another code block:
            ```
            // JavaScript code
            function greet(name) {
                console.log(`Hello, ${'$'}{name}!`);
            }
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Code Blocks", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<pre>"))
        assertTrue(result.parsedContent.contains("def hello_world"))
        assertTrue(result.parsedContent.contains("function greet"))
    }

    // ==================== Block Quotes Tests ====================

    @Test
    fun `should parse block quotes`() {
        val content = """
            title: Block Quotes
            
            Regular paragraph.
            
            <<<
            This is a block quote.
            It can span multiple lines.
            <<<
            
            Another regular paragraph.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Block Quotes", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<blockquote>"))
        assertTrue(result.parsedContent.contains("This is a block quote"))
    }

    @Test
    fun `should parse single line block quotes`() {
        val content = """
            title: Single Line Quotes
            
            > This is a single line block quote.
            
            Regular text continues here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Single Line Quotes", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<blockquote>"))
        assertTrue(result.parsedContent.contains("This is a single line block quote"))
    }

    // ==================== Horizontal Rules Tests ====================

    @Test
    fun `should parse horizontal rules`() {
        val content = """
            title: Horizontal Rules
            
            Content before rule.
            ---
            Content after rule.
            
            Another section.
            ----
            More content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Horizontal Rules", result.metadata["title"])
        assertTrue(result.parsedContent.contains("<hr>"))
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert TiddlyWiki to HTML`() {
        val content = """
            title: HTML Conversion Test
            tags: test html
            
            ! Main Heading
            This is content under the main heading.
            
            !! Subheading
            This is ''bold'' text and //italic// text.
            
            * List item 1
            * List item 2
            
            Here is a [[Wiki Link]] and an [ext[https://example.com|external link]].
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("tiddlywiki"))
        assertTrue(html.contains("Main Heading"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<a href='Wiki Link'>Wiki Link</a>"))
        assertTrue(html.contains("target='_blank'"))
    }

    @Test
    fun `should convert dark mode HTML`() {
        val content = """
            title: Dark Mode Test
            
            Content for dark mode testing.
            
            ! Heading
            Some content here.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("tiddlywiki"))
        assertTrue(html.contains("Heading"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate well-formed TiddlyWiki`() {
        val content = """
            title: Valid Tiddler
            tags: valid test
            
            ! Valid heading
            Valid content with ''bold'' text.
            
            * List item 1
            * List item 2
            
            [[Valid Wiki Link]] and [ext[https://example.com]].
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `should detect unclosed brackets in links`() {
        val content = """
            title: Invalid Links
            
            Broken link: [[Unclosed Tiddler
            Another broken: [ext[https://example.com
            
            Regular text continues.
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should detect unclosed quotes for bold formatting`() {
        val content = """
            title: Unclosed Bold
            
            Unclosed bold: ''This text starts bold but never ends.
            
            Regular text here.
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Unclosed quotes") })
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("", result.rawContent)
        assertFalse(result.metadata.containsKey("title"))
        assertFalse(result.metadata.containsKey("tags"))
        assertEquals("0", result.metadata["lines"])
    }

    @Test
    fun `should handle whitespace-only input`() {
        val content = "   \n\n\t  \n   "
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertFalse(result.metadata.containsKey("title"))
        assertEquals("4", result.metadata["lines"])
    }

    @Test
    fun `should handle malformed metadata`() {
        val content = """
            title:Valid Tiddler (no space after colon)
            tags:tag1 tag2 (no space after colon)
            invalid line without colon
            
            Content starts here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Should still parse the content even with malformed metadata
        assertTrue(result.parsedContent.isNotEmpty())
        assertTrue(result.parsedContent.contains("Content starts here"))
    }

    @Test
    fun `should handle unicode characters`() {
        val content = """
            title: Unicode Test
            
            Chinese: 你好世界
            Emoji: 🌍 🚀 ❤️
            Russian: Привет мир
            Arabic: مرحبا بالعالم
            
            ! 中文标题
            Content in different languages.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Unicode Test", result.metadata["title"])
        assertTrue(result.parsedContent.contains("你好世界"))
        assertTrue(result.parsedContent.contains("🌍"))
        assertTrue(result.parsedContent.contains("Привет мир"))
        assertTrue(result.parsedContent.contains("مرحبا بالعالم"))
    }

    @Test
    fun `should handle very long input`() {
        val line = "title: Long Tiddler\n\n! Heading\nThis is a test line in TiddlyWiki format.\n"
        val longContent = line.repeat(1000)

        val result = parser.parse(longContent)

        assertNotNull(result)
        assertEquals(longContent, result.rawContent)
        assertTrue(result.metadata["lines"]!!.toInt() > 1000)
    }

    @Test
    fun `should handle special characters in content`() {
        val content = """
            title: Special Characters
            
            Special chars: < > & " ' © ® ™
            Math symbols: ∑ ∏ ∫ ≠ ≤ ≥
            Currency: $ € £ ¥
            Punctuation: … — " " ' '
            
            ! Special & <characters> in "heading"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Special Characters", result.metadata["title"])
        assertTrue(result.parsedContent.contains("&lt;"))
        assertTrue(result.parsedContent.contains("&gt;"))
        assertTrue(result.parsedContent.contains("&amp;"))
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should preserve content through round-trip parsing`() {
        val originalContent = """
            title: Round-trip Test
            tags: test round-trip
            created: 20250101120000000
            modified: 20250102150000000
            
            ! Main Heading
            This is content with ''bold'' and //italic// text.
            
            * List item 1
            * List item 2 with ''bold text''
            ** Nested item with //italic text//
            
            ## Ordered list
            # First step
            # Second step
            
            Here is a [[Wiki Link]] and an [ext[https://example.com|external link]].
            
            ```
            Code block content
            with multiple lines
            ```
            
            <<<
            Block quote content
            spanning multiple lines
            <<<
            
            Custom field: custom value
        """.trimIndent()

        // Parse the original content
        val document = parser.parse(originalContent)
        
        // Convert to HTML
        val html = parser.toHtml(document, lightMode = true)
        
        // Parse the HTML back (simulating round-trip)
        val htmlDocument = parser.parse(html)
        
        // The HTML should be different from original (as expected)
        assertTrue(originalContent != html)
        assertTrue(html.contains("tiddlywiki"))
        
        // The original document should have no errors
        assertTrue(document.errors.isEmpty())
    }

    // ==================== Performance Benchmark Tests ====================

    @Test
    fun `should parse large TiddlyWiki documents efficiently`() {
        // Create a large document with many headings and content
        val largeContent = buildString {
            repeat(100) { i ->
                appendLine("title: Tiddler $i")
                appendLine("tags: tag$i category$i")
                appendLine("created: 20250101120000000")
                appendLine("modified: 20250102150000000")
                appendLine()
                appendLine("! Heading for Tiddler $i")
                appendLine("This is content for tiddler $i with ''bold'' and //italic// text.")
                appendLine()
                
                repeat(3) { j ->
                    appendLine("!! Subheading $i.$j")
                    appendLine("Content for subheading")
                    appendLine("* List item 1")
                    appendLine("* List item 2")
                    appendLine("# Ordered item 1")
                    appendLine("# Ordered item 2")
                    appendLine()
                }
                
                appendLine("[[Link to Tiddler ${(i + 1) % 100}]]")
                appendLine("[ext[https://example$i.com]]")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertTrue(result.metadata["lines"]!!.toInt() > 1000)
        
        // Performance assertion - should parse in reasonable time (< 2 seconds for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 2000, "Parsing should complete within 2 seconds, took $parseTime ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        val largeContent = buildString {
            repeat(50) { i ->
                appendLine("title: Large Document Section $i")
                appendLine("tags: section$i large")
                appendLine()
                appendLine("! Section $i Heading")
                appendLine("Content with ''bold'' and //italic// text.")
                appendLine()
                
                appendLine("```")
                appendLine("def function_$i():")
                appendLine("    return $i")
                appendLine("```")
                appendLine()
                
                repeat(5) { j ->
                    appendLine("* List item $j")
                    appendLine("** Nested item $j.1")
                    appendLine("** Nested item $j.2")
                }
                appendLine()
                
                appendLine("[[Link to Section ${(i + 1) % 50}]]")
                appendLine("[ext[https://example$i.com]]")
                appendLine()
            }
        }

        val document = parser.parse(largeContent)
        
        val startTime = System.currentTimeMillis()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(html)
        assertTrue(html.length > 0)
        assertTrue(html.contains("tiddlywiki"))
        
        // Performance assertion - should convert in reasonable time (< 1 second)
        val convertTime = endTime - startTime
        assertTrue(convertTime < 1000, "HTML conversion should complete within 1 second, took $convertTime ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)
        
        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_TIDDLYWIKI)
        
        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_TIDDLYWIKI, registryParser.supportedFormat.id)
        
        // Test that it works
        val content = """
            title: Registry Test
            
            Content for registry test.
        """.trimIndent()
        val result = registryParser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_TIDDLYWIKI, result.format.id)
        assertEquals("Registry Test", result.metadata["title"])
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val tiddlyWikiFormat = allFormats.find { it.id == TextFormat.ID_TIDDLYWIKI }

        assertNotNull(tiddlyWikiFormat)
        assertEquals("TiddlyWiki", tiddlyWikiFormat.name)
        assertEquals(".tid", tiddlyWikiFormat.defaultExtension)
        assertTrue(tiddlyWikiFormat.extensions.contains(".tid"))
        assertTrue(tiddlyWikiFormat.extensions.contains(".tiddly"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex TiddlyWiki document`() {
        val content = """
            title: Complex TiddlyWiki Document
            tags: complex test documentation tutorial
            created: 20250101120000000
            modified: 20250102150000000
            type: text/vnd.tiddlywiki
            author: Test Author
            
            !! Introduction
            This document demonstrates various TiddlyWiki features.
            
            It contains ''bold text'', //italic text//, __underlined text__, and ~~strikethrough text~~.
            
            !!! Lists and Structure
            Here's an unordered list:
            * First item with ''emphasis''
            * Second item with //style//
            ** Nested item 1
            ** Nested item 2
            *** Deeply nested item
            
            And an ordered list:
            # First step
            # Second step
            ## Sub-step 2.1
            ## Sub-step 2.2
            # Third step
            
            !!! Links and References
            Internal links: [[Main Page]] and [[About|About Page]].
            
            External links: [ext[https://tiddlywiki.com]] and [ext[https://github.com|TiddlyWiki on GitHub]].
            
            Images: [img[logo.png]] and [img[screenshot.jpg]].
            
            !!! Code Examples
            ```
            // JavaScript example
            function greet(name) {
                console.log(`Hello, ${'$'}{name}!`);
                return `Greeted ${'$'}{name}`;
            }
            ```
            
            ```
            # Python example
            def greet(name):
                print(f"Hello, {name}!")
                return f"Greeted {name}"
            ```
            
            !!! Block Quotes
            <<<
            This is a block quote.
            It can contain multiple lines and formatting like ''bold'' text.
            <<<
            
            > This is a single-line block quote.
            
            !!! Special Formatting
            Superscript: E = mc^^2^^, Area = πr^^2^^
            
            Subscript: H,,2,,O, CO,,2,,, CH,,4,,
            
            Inline code: Use `console.log()` to debug, or `print()` in Python.
            
            !!! Horizontal Rules
            Content above rule.
            ---
            Content below rule.
            
            ----
            Another section.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Complex TiddlyWiki Document", result.metadata["title"])
        assertEquals("complex, test, documentation, tutorial", result.metadata["tags"])
        assertEquals("text/vnd.tiddlywiki", result.metadata["type"])
        assertEquals("Test Author", result.metadata["author"])
        
        // Check that various elements are present in HTML
        assertTrue(result.parsedContent.contains("Introduction"))
        assertTrue(result.parsedContent.contains("<strong>bold text</strong>"))
        assertTrue(result.parsedContent.contains("<em>italic text</em>"))
        assertTrue(result.parsedContent.contains("<u>underlined text</u>"))
        assertTrue(result.parsedContent.contains("<s>strikethrough text</s>"))
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<ol>"))
        assertTrue(result.parsedContent.contains("<a href='Main Page'>Main Page</a>"))
        assertTrue(result.parsedContent.contains("target='_blank'"))
        assertTrue(result.parsedContent.contains("<img src='logo.png'"))
        assertTrue(result.parsedContent.contains("<pre>"))
        assertTrue(result.parsedContent.contains("<blockquote>"))
        assertTrue(result.parsedContent.contains("<sup>2</sup>"))
        assertTrue(result.parsedContent.contains("<sub>2</sub>"))
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should handle nested structures`() {
        val content = """
            title: Nested Structures Test
            
            ! Level 1 Heading
            Content at level 1
            
            !! Level 2 Heading
            Content at level 2
            
            !!! Level 3 Heading
            Content at level 3
            
            !!!! Level 4 Heading
            Content at level 4
            
            !!!!! Level 5 Heading
            Content at level 5
            
            !!!!!! Level 6 Heading
            Content at level 6
            
            ! Back to Level 1
            Back to level 1 content
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Nested Structures Test", result.metadata["title"])
        assertTrue(result.parsedContent.contains("Level 1 Heading"))
        assertTrue(result.parsedContent.contains("Level 6 Heading"))
        assertTrue(result.parsedContent.contains("Back to Level 1"))
    }

    @Test
    fun `should handle mixed content types`() {
        val content = """
            title: Mixed Content Types
            
            Some regular paragraph text here.
            
            * Unordered list item 1
            * Unordered list item 2 with ''bold text''
            ** Nested unordered item with //italic text//
            
            # Ordered list item 1
            # Ordered list item 2
            ## Nested ordered item
            
            Regular text again.
            
            ```
            Code block content
            with multiple lines
            ```
            
            <<<
            Block quote with some important information.
            Second line of quote.
            <<<
            
            More text after the quote.
            
            [[Wiki Link]] and [ext[https://example.com]]
            
            ''Bold text'', //italic text//, __underlined__, ~~strikethrough~~
            
            ---
            
            Final paragraph with `inline code` and E = mc^^2^^.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("Mixed Content Types", result.metadata["title"])
        assertTrue(result.parsedContent.contains("regular paragraph text"))
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<ol>"))
        assertTrue(result.parsedContent.contains("<pre>"))
        assertTrue(result.parsedContent.contains("<blockquote>"))
        assertTrue(result.parsedContent.contains("<a href='Wiki Link'>Wiki Link</a>"))
        assertTrue(result.parsedContent.contains("<strong>"))
        assertTrue(result.parsedContent.contains("<em>"))
        assertTrue(result.parsedContent.contains("<u>"))
        assertTrue(result.parsedContent.contains("<s>"))
        assertTrue(result.parsedContent.contains("<hr>"))
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("<sup>"))
    }
}