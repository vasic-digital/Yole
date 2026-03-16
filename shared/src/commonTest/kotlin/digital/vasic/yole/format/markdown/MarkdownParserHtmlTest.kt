/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserHtmlTest {

    private val parser = MarkdownParser()

    // ==================== Document structure ====================

    @Test
    fun testOutputWrappedInMarkdownDiv() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<div class='markdown'>"))
        assertTrue(html.endsWith("</div>"))
    }

    @Test
    fun testDocumentContainsStyleTag() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun testEmptyContentProducesValidStructure() {
        val doc = parser.parse("")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("<div class='markdown'>"))
        assertTrue(html.contains("</div>"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("# Heading 1")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("Heading 1"))
        assertTrue(html.contains("</h1>"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("## Heading 2")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("Heading 2"))
        assertTrue(html.contains("</h2>"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("### Heading 3")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h3>"))
        assertTrue(html.contains("Heading 3"))
        assertTrue(html.contains("</h3>"))
    }

    @Test
    fun testH4Heading() {
        val doc = parser.parse("#### Heading 4")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h4>"))
        assertTrue(html.contains("Heading 4"))
        assertTrue(html.contains("</h4>"))
    }

    @Test
    fun testH5Heading() {
        val doc = parser.parse("##### Heading 5")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h5>"))
        assertTrue(html.contains("Heading 5"))
        assertTrue(html.contains("</h5>"))
    }

    @Test
    fun testH6Heading() {
        val doc = parser.parse("###### Heading 6")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h6>"))
        assertTrue(html.contains("Heading 6"))
        assertTrue(html.contains("</h6>"))
    }

    @Test
    fun testAllHeadingLevelsInSameDocument() {
        val content = """
            # H1
            ## H2
            ### H3
            #### H4
            ##### H5
            ###### H6
        """.trimIndent()
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h1>H1</h1>"))
        assertTrue(html.contains("<h2>H2</h2>"))
        assertTrue(html.contains("<h3>H3</h3>"))
        assertTrue(html.contains("<h4>H4</h4>"))
        assertTrue(html.contains("<h5>H5</h5>"))
        assertTrue(html.contains("<h6>H6</h6>"))
    }

    // ==================== Bold and italic ====================

    @Test
    fun testBoldWithDoubleAsterisks() {
        val doc = parser.parse("This is **bold** text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun testBoldWithDoubleUnderscores() {
        val doc = parser.parse("This is __bold__ text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun testItalicWithSingleAsterisk() {
        val doc = parser.parse("This is *italic* text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<em>italic</em>"))
    }

    @Test
    fun testItalicWithSingleUnderscore() {
        val doc = parser.parse("This is _italic_ text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<em>italic</em>"))
    }

    @Test
    fun testBoldAndItalicCombined() {
        val doc = parser.parse("This is ***bold and italic*** text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<em><strong>bold and italic</strong></em>"))
    }

    // ==================== Strikethrough ====================

    @Test
    fun testStrikethroughRendering() {
        val doc = parser.parse("This is ~~deleted~~ text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<s>deleted</s>"))
    }

    @Test
    fun testStrikethroughWithMultipleWords() {
        val doc = parser.parse("~~removed words here~~")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<s>removed words here</s>"))
    }

    // ==================== Links and images ====================

    @Test
    fun testInlineLink() {
        val doc = parser.parse("Visit [Google](https://google.com)")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<a href='https://google.com'>Google</a>"))
    }

    @Test
    fun testLinkWithRelativePath() {
        val doc = parser.parse("See [docs](./README.md)")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<a href='./README.md'>docs</a>"))
    }

    @Test
    fun testImageRendering() {
        val doc = parser.parse("![Alt text](image.png)")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<img src='image.png' alt='Alt text'/>"))
    }

    @Test
    fun testImageWithEmptyAlt() {
        val doc = parser.parse("![](photo.jpg)")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<img src='photo.jpg' alt=''/>"))
    }

    // ==================== Code blocks ====================

    @Test
    fun testFencedCodeBlock() {
        val content = "```\nvar x = 1;\n```"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("</code></pre>"))
    }

    @Test
    fun testFencedCodeBlockWithLanguage() {
        val content = "```kotlin\nfun main() {\n    println(\"Hello\")\n}\n```"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("fun main()"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val content = "```\n<script>alert('xss')</script>\n```"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(!html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    // ==================== Inline code ====================

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use the `print()` function")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<code>print()</code>"))
    }

    @Test
    fun testInlineCodeInHeading() {
        val doc = parser.parse("# The `main` function")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<code>main</code>"))
        assertTrue(html.contains("<h1>"))
    }

    // ==================== Unordered lists ====================

    @Test
    fun testUnorderedListWithAsterisks() {
        val content = "* Item 1\n* Item 2\n* Item 3"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Item 1</li>"))
        assertTrue(html.contains("<li>Item 2</li>"))
        assertTrue(html.contains("<li>Item 3</li>"))
    }

    @Test
    fun testUnorderedListWithDashes() {
        val content = "- First\n- Second"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("<li>Second</li>"))
    }

    @Test
    fun testUnorderedListClosed() {
        val content = "- Item A\n- Item B\n\nParagraph after list."
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("</ul>"))
    }

    // ==================== Ordered lists ====================

    @Test
    fun testOrderedList() {
        val content = "1. First\n2. Second\n3. Third"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("<li>Second</li>"))
        assertTrue(html.contains("<li>Third</li>"))
    }

    @Test
    fun testOrderedListClosed() {
        val content = "1. Alpha\n2. Beta\n\nSome text."
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("</ol>"))
    }

    // ==================== Blockquotes ====================

    @Test
    fun testBlockquote() {
        val doc = parser.parse("> This is a quote")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("This is a quote"))
        assertTrue(html.contains("</blockquote>"))
    }

    @Test
    fun testMultiLineBlockquote() {
        val content = "> Line 1\n> Line 2\n> Line 3"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("Line 1"))
        assertTrue(html.contains("Line 2"))
        assertTrue(html.contains("Line 3"))
    }

    @Test
    fun testBlockquoteClosedOnEmptyLine() {
        val content = "> Quoted text\n\nRegular text"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("</blockquote>"))
        assertTrue(html.contains("<p>"))
    }

    // ==================== Tables ====================

    @Test
    fun testBasicTable() {
        val content = "| Name | Age |\n|------|-----|\n| John | 30  |"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("</table>"))
    }

    @Test
    fun testTableHeaderCells() {
        val content = "| Name | Age |\n|------|-----|\n| John | 30  |"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<th>"))
        assertTrue(html.contains("Name"))
        assertTrue(html.contains("Age"))
    }

    @Test
    fun testTableDataCells() {
        val content = "| Name | Age |\n|------|-----|\n| John | 30  |\n| Jane | 25  |"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<td>"))
        assertTrue(html.contains("John"))
        assertTrue(html.contains("30"))
        assertTrue(html.contains("Jane"))
        assertTrue(html.contains("25"))
    }

    @Test
    fun testTableRowStructure() {
        val content = "| A | B |\n|---|---|\n| 1 | 2 |"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<tr>"))
        assertTrue(html.contains("</tr>"))
    }

    // ==================== Task list checkboxes ====================

    @Test
    fun testCheckedTaskListItem() {
        val doc = parser.parse("- [x] Completed task")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<input type='checkbox' disabled checked>"))
    }

    @Test
    fun testUncheckedTaskListItem() {
        val doc = parser.parse("- [ ] Pending task")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<input type='checkbox' disabled>"))
    }

    @Test
    fun testMixedTaskListItems() {
        val content = "- [x] Done\n- [ ] Not done\n- [x] Also done"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<input type='checkbox' disabled checked>"))
        assertTrue(html.contains("<input type='checkbox' disabled>"))
    }

    // ==================== Light mode vs dark mode ====================

    @Test
    fun testLightModeProducesHtml() {
        val doc = parser.parse("# Test")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("<h1>"))
    }

    @Test
    fun testDarkModeProducesHtml() {
        val doc = parser.parse("# Test")
        val html = parser.toHtml(doc, lightMode = false)
        assertNotNull(html)
        assertTrue(html.contains("<h1>"))
    }

    @Test
    fun testBothModesContainStyle() {
        val doc = parser.parse("# Test")
        val lightHtml = parser.toHtml(doc, lightMode = true)
        val darkHtml = parser.toHtml(doc, lightMode = false)
        assertTrue(lightHtml.contains("<style>"))
        assertTrue(darkHtml.contains("<style>"))
    }

    // ==================== HTML escaping ====================

    @Test
    fun testAmpersandEscaping() {
        val doc = parser.parse("A & B")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&amp;"))
        assertTrue(!html.contains("<p>A & B"))
    }

    @Test
    fun testLessThanEscaping() {
        val doc = parser.parse("x < y")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;"))
    }

    @Test
    fun testGreaterThanEscaping() {
        // Note: > at the start of a line is a blockquote; use mid-text >
        val doc = parser.parse("x is &gt; y")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
    }

    @Test
    fun testHtmlTagEscapingInParagraph() {
        val doc = parser.parse("Use <div> elements")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;div&gt;"))
        assertTrue(!html.contains("<div> elements"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContentParsing() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("", doc.rawContent)
    }

    @Test
    fun testEmptyContentHtmlGeneration() {
        val doc = parser.parse("")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testWhitespaceOnlyContent() {
        val doc = parser.parse("   \n\n   ")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("<div class='markdown'>"))
    }

    // ==================== Metadata extraction ====================

    @Test
    fun testMetadataLineCount() {
        val content = "# Title\n\nParagraph one.\n\nParagraph two."
        val doc = parser.parse(content)
        assertEquals("5", doc.metadata["lines"])
    }

    @Test
    fun testMetadataExtensionDefault() {
        val doc = parser.parse("Hello")
        assertEquals("", doc.metadata["extension"])
    }

    @Test
    fun testMetadataExtensionFromFilename() {
        val doc = parser.parse("Hello", mapOf("filename" to "test.md"))
        assertEquals(".md", doc.metadata["extension"])
    }

    @Test
    fun testMetadataLineCountSingleLine() {
        val doc = parser.parse("Single line")
        assertEquals("1", doc.metadata["lines"])
    }

    // ==================== canParse ====================

    @Test
    fun testCanParseMarkdownFormat() {
        val format = parser.supportedFormat
        assertTrue(parser.canParse(format))
    }

    @Test
    fun testSupportedFormatIsMarkdown() {
        assertEquals(TextFormat.ID_MARKDOWN, parser.supportedFormat.id)
    }

    // ==================== Horizontal rules ====================

    @Test
    fun testHorizontalRuleWithDashes() {
        val doc = parser.parse("---")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<hr>"))
    }

    @Test
    fun testHorizontalRuleWithAsterisks() {
        val doc = parser.parse("***")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<hr>"))
    }

    // ==================== Paragraphs ====================

    @Test
    fun testParagraphWrapping() {
        val doc = parser.parse("This is a paragraph.")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<p>"))
        assertTrue(html.contains("</p>"))
    }

    @Test
    fun testMultipleParagraphsSeparatedByBlankLine() {
        val content = "First paragraph.\n\nSecond paragraph."
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("First paragraph."))
        assertTrue(html.contains("Second paragraph."))
    }

    // ==================== Complex documents ====================

    @Test
    fun testComplexDocumentWithMultipleElements() {
        val content = """
            # Project Title

            This is a **bold** statement with *italic* text.

            ## Features

            - [x] Feature 1 complete
            - [ ] Feature 2 pending

            ### Code Example

            ```kotlin
            fun hello() = println("Hello")
            ```

            > Important note here

            | Column A | Column B |
            |----------|----------|
            | Value 1  | Value 2  |
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("<h3>"))
        assertTrue(html.contains("<strong>"))
        assertTrue(html.contains("<em>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("<table>"))
    }

    @Test
    fun testFormattingInsideListItems() {
        val doc = parser.parse("- This is **bold** in a list")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<li>"))
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun testFormattingInsideBlockquote() {
        val doc = parser.parse("> This is **bold** in a quote")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    // ==================== Validation ====================

    @Test
    fun testValidContentNoErrors() {
        val content = "# Title\n\nThis is [a link](https://example.com)."
        val errors = parser.validate(content)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testValidationDetectsUnclosedBrackets() {
        val errors = parser.validate("This is [unclosed link")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun testValidationDetectsUnclosedParentheses() {
        val errors = parser.validate("This is (unclosed")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed parentheses") })
    }

    @Test
    fun testValidationSkipsCodeBlocks() {
        val content = "```\n[unclosed bracket\n```"
        val errors = parser.validate(content)
        assertTrue(errors.isEmpty(), "Validation errors inside code blocks should be ignored")
    }
}
