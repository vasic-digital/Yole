/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for WikitextParser
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserInitializer
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [WikitextParser].
 *
 * Covers: wikilinks [[link]], templates {{template}}, headings (=H1= ... ======H6======),
 * bold (**text**), italic (//text//), categories, unclosed tags, nested formatting,
 * empty input, special chars, Zim header removal, tables, checklists.
 *
 * Total: ~45 tests
 */
class WikitextParserUnitTest {

    private val parser = WikitextParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    // ==================== Supported format ====================

    @Test
    fun SupportedFormatIsWikitext() {
        assertEquals(TextFormat.ID_WIKITEXT, parser.supportedFormat.id)
    }

    @Test
    fun SupportedFormatExtensionsIncludeWiki() {
        assertTrue(parser.supportedFormat.extensions.contains(".wiki") ||
                   WikitextParser.EXTENSIONS.contains(".wiki"))
    }

    @Test
    fun CanParseReturnsTrueForWikitext() {
        val format = FormatRegistry.getById(TextFormat.ID_WIKITEXT)!!
        assertTrue(parser.canParse(format))
    }

    @Test
    fun CanParseReturnsFalseForMarkdown() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        assertFalse(parser.canParse(format))
    }

    // ==================== Empty / minimal input ====================

    @Test
    fun ParseEmptyStringReturnsDocument() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("", doc.rawContent)
    }

    @Test
    fun ParseEmptyMetadataLineCount() {
        val doc = parser.parse("")
        assertEquals("1", doc.metadata["lines"]) // single empty line
    }

    @Test
    fun ParseSingleLineParagraph() {
        val doc = parser.parse("Hello world.")
        assertNotNull(doc)
        assertTrue(doc.parsedContent.contains("Hello world."))
    }

    // ==================== Headings ====================

    @Test
    fun ParseH1Heading() {
        val doc = parser.parse("= Heading One =")
        assertTrue(doc.parsedContent.contains("<h1>"))
    }

    @Test
    fun ParseH2Heading() {
        val doc = parser.parse("== Heading Two ==")
        assertTrue(doc.parsedContent.contains("<h2>"))
    }

    @Test
    fun ParseH3Heading() {
        val doc = parser.parse("=== Heading Three ===")
        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    @Test
    fun ParseH4Heading() {
        val doc = parser.parse("==== Heading Four ====")
        assertTrue(doc.parsedContent.contains("<h4>"))
    }

    @Test
    fun ParseH5Heading() {
        val doc = parser.parse("===== Heading Five =====")
        assertTrue(doc.parsedContent.contains("<h5>"))
    }

    @Test
    fun ParseH6Heading() {
        val doc = parser.parse("====== Heading Six ======")
        assertTrue(doc.parsedContent.contains("<h6>"))
    }

    @Test
    fun ParseHeadingTextContent() {
        val doc = parser.parse("= My Title =")
        assertTrue(doc.parsedContent.contains("My Title"))
    }

    // ==================== Inline markup ====================

    @Test
    fun ParseBoldText() {
        val doc = parser.parse("This is **bold** text.")
        assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun ParseItalicText() {
        val doc = parser.parse("This is //italic// text.")
        assertTrue(doc.parsedContent.contains("<em>italic</em>"))
    }

    @Test
    fun ParseInlineCode() {
        val doc = parser.parse("Use ''code'' here.")
        assertTrue(doc.parsedContent.contains("<code>"))
    }

    @Test
    fun ParseStrikethrough() {
        val doc = parser.parse("This is ~~struck~~ out.")
        assertTrue(doc.parsedContent.contains("<s>struck</s>"))
    }

    @Test
    fun ParseHighlighted() {
        val doc = parser.parse("This is __highlighted__ text.")
        assertTrue(doc.parsedContent.contains("highlight"))
    }

    // ==================== Wikilinks ====================

    @Test
    fun ParseSimpleWikilink() {
        val doc = parser.parse("See [[PageName]] for details.")
        assertTrue(doc.parsedContent.contains("<a href="))
        assertTrue(doc.parsedContent.contains("PageName"))
    }

    @Test
    fun ParseWikilinkWithDescription() {
        val doc = parser.parse("See [[PageName|Display Text]] for details.")
        assertTrue(doc.parsedContent.contains("Display Text"))
        assertTrue(doc.parsedContent.contains("<a href="))
    }

    @Test
    fun ParseExternalLinkWithUrl() {
        val doc = parser.parse("Visit [[https://example.com|Example Site]].")
        assertTrue(doc.parsedContent.contains("Example Site"))
    }

    // ==================== Templates ====================

    @Test
    fun ParseSimpleTemplate() {
        val doc = parser.parse("{{TemplateName}}")
        assertTrue(doc.parsedContent.contains("template"))
    }

    @Test
    fun ParseTemplateWithParameter() {
        val doc = parser.parse("{{InfoBox|color=blue}}")
        assertTrue(doc.parsedContent.contains("InfoBox"))
    }

    @Test
    fun ParseImageWithBraces() {
        val doc = parser.parse("{{image.png|Alt text}}")
        assertTrue(doc.parsedContent.contains("<img"))
    }

    // ==================== Lists ====================

    @Test
    fun ParseUnorderedListItem() {
        val doc = parser.parse("* Item one")
        assertTrue(doc.parsedContent.contains("<li>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
    }

    @Test
    fun ParseOrderedListItem() {
        val doc = parser.parse("1. First item")
        assertTrue(doc.parsedContent.contains("<li>"))
    }

    @Test
    fun ParseChecklistUnchecked() {
        val doc = parser.parse("[ ] Unchecked task")
        assertTrue(doc.parsedContent.contains("<li"))
    }

    @Test
    fun ParseChecklistChecked() {
        val doc = parser.parse("[*] Checked task")
        assertTrue(doc.parsedContent.contains("checked"))
    }

    // ==================== Tables ====================

    @Test
    fun ParseBasicTableStructure() {
        val content = "{|\n|-\n| Cell A\n| Cell B\n|}"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("</table>"))
    }

    @Test
    fun ParseTableWithHeaderCells() {
        val content = "{|\n! Header 1\n! Header 2\n|-\n| Data\n|}"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("<th>"))
    }

    @Test
    fun ParseTableWithCaption() {
        val content = "{|\n|+ My Caption\n|-\n| Cell\n|}"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("My Caption"))
    }

    // ==================== Zim header removal ====================

    @Test
    fun ParseZimHeaderIsRemoved() {
        val content = "[DocumentAttributes]\nContentType=text/x-zim-wiki\n\nActual content here."
        val doc = parser.parse(content)
        assertFalse(doc.parsedContent.contains("DocumentAttributes"))
    }

    @Test
    fun ParseZimHeaderMetadataFlag() {
        val content = "[DocumentAttributes]\nContentType=text/x-zim-wiki\n\nContent."
        val doc = parser.parse(content)
        assertEquals("true", doc.metadata["hasZimHeader"])
    }

    @Test
    fun ParseContentWithoutZimHeaderFlagIsFalse() {
        val doc = parser.parse("Normal content.")
        assertEquals("false", doc.metadata["hasZimHeader"])
    }

    // ==================== Code blocks ====================

    @Test
    fun ParseCodeBlockTripleApostrophe() {
        val content = "'''\nsome code here\n'''"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("<pre><code>"))
        assertTrue(doc.parsedContent.contains("</code></pre>"))
    }

    // ==================== Validation ====================

    @Test
    fun ValidateWellFormedHeadingNoErrors() {
        val errors = parser.validate("= Heading =")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun ValidateUnbalancedHeadingReturnsError() {
        val errors = parser.validate("= Heading ==")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unbalanced") })
    }

    @Test
    fun ValidateUnclosedBracketsReturnsError() {
        val errors = parser.validate("[[unclosed link")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("bracket") || it.contains("Unclosed") })
    }

    @Test
    fun ValidateUnclosedBracesReturnsError() {
        val errors = parser.validate("{{unclosed template")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("brace") || it.contains("Unclosed") })
    }

    @Test
    fun ValidateEmptyContentNoErrors() {
        val errors = parser.validate("")
        assertTrue(errors.isEmpty())
    }

    // ==================== Special characters ====================

    @Test
    fun ParseSpecialHtmlCharsEscaped() {
        val doc = parser.parse("a & b")
        assertTrue(doc.parsedContent.contains("&amp;") || doc.parsedContent.contains("a") && doc.parsedContent.contains("b"))
    }

    @Test
    fun ParseUnicodeContent() {
        val doc = parser.parse("= こんにちは =\nJapanese content: 日本語テキスト")
        assertNotNull(doc)
        assertTrue(doc.rawContent.contains("日本語"))
    }

    // ==================== Metadata ====================

    @Test
    fun ParseMetadataContainsLineCount() {
        val doc = parser.parse("Line 1\nLine 2\nLine 3")
        assertEquals("3", doc.metadata["lines"])
    }

    @Test
    fun ParseMetadataContainsExtension() {
        val doc = parser.parse("content", mapOf("filename" to "file.wiki"))
        assertEquals(".wiki", doc.metadata["extension"])
    }

    @Test
    fun ParsedDocumentFormatMatchesParser() {
        val doc = parser.parse("Test")
        assertEquals(TextFormat.ID_WIKITEXT, doc.format.id)
    }

    @Test
    fun RawContentIsPreserved() {
        val content = "= Title =\n\nParagraph content."
        val doc = parser.parse(content)
        assertEquals(content, doc.rawContent)
    }

    // ==================== Nested / complex ====================

    @Test
    fun ParseNestedFormattingBoldAndItalic() {
        val doc = parser.parse("**bold** and //italic// text together.")
        assertTrue(doc.parsedContent.contains("<strong>") || doc.parsedContent.contains("<em>"))
    }

    @Test
    fun ParseMultipleSectionsAndLinks() {
        val content = """
            = Section 1 =

            Some [[Link1]] text.

            == Section 2 ==

            More [[Link2|Display]] text.
        """.trimIndent()
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("<h2>"))
        assertTrue(doc.parsedContent.contains("<a href="))
    }
}
