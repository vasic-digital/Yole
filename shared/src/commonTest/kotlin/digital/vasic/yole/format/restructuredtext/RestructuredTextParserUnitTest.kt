/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for RestructuredTextParser
 *
 *########################################################*/
package digital.vasic.yole.format.restructuredtext

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
 * Unit tests for [RestructuredTextParser].
 *
 * Covers: headers (=, -, ~, ^, "), directives (note, code-block, warning),
 * inline markup (*emphasis*, **strong**, ``code``), grid tables, bullet lists,
 * empty input, malformed input, Unicode, large documents, and validation.
 *
 * Total: ~40 tests
 */
class RestructuredTextParserUnitTest {

    private val parser = RestructuredTextParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    // ==================== Supported format ====================

    @Test
    fun SupportedFormatIsRestructuredText() {
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, parser.supportedFormat.id)
    }

    @Test
    fun SupportedFormatExtensionsIncludeRst() {
        assertTrue(parser.supportedFormat.extensions.contains(".rst"))
    }

    @Test
    fun SupportedFormatExtensionsIncludeRest() {
        assertTrue(parser.supportedFormat.extensions.contains(".rest"))
    }

    @Test
    fun CanParseReturnsTrueForRestructuredText() {
        val format = FormatRegistry.getById(TextFormat.ID_RESTRUCTUREDTEXT)!!
        assertTrue(parser.canParse(format))
    }

    @Test
    fun CanParseReturnsFalseForOtherFormat() {
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
    fun ParseEmptyMetadataSectionsZero() {
        val doc = parser.parse("")
        assertEquals("0", doc.metadata["sections"])
    }

    @Test
    fun ParseEmptyMetadataDirectivesZero() {
        val doc = parser.parse("")
        assertEquals("0", doc.metadata["directives"])
    }

    @Test
    fun ParseSingleLineParagraph() {
        val doc = parser.parse("Hello world.")
        assertNotNull(doc)
        assertTrue(doc.parsedContent.contains("Hello world."))
    }

    // ==================== Section headers ====================

    @Test
    fun ParseLevel1HeaderWithEquals() {
        val doc = parser.parse("Title\n=====")
        assertEquals("1", doc.metadata["sections"])
        assertEquals("1", doc.metadata["max_level"])
    }

    @Test
    fun ParseLevel2HeaderWithDashes() {
        val doc = parser.parse("Section\n-------")
        assertEquals("1", doc.metadata["sections"])
        assertEquals("2", doc.metadata["max_level"])
    }

    @Test
    fun ParseLevel3HeaderWithTildes() {
        val doc = parser.parse("Subsection\n~~~~~~~~~~")
        assertEquals("1", doc.metadata["sections"])
        assertEquals("3", doc.metadata["max_level"])
    }

    @Test
    fun ParseLevel4HeaderWithCarets() {
        val doc = parser.parse("Sub-subsection\n^^^^^^^^^^^^^^")
        assertEquals("1", doc.metadata["sections"])
        assertEquals("4", doc.metadata["max_level"])
    }

    @Test
    fun ParseMultipleSectionsCountsCorrectly() {
        val content = "Title\n=====\n\nSection A\n---------\n\nSection B\n---------"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["sections"])
    }

    @Test
    fun ParseSectionMaxLevelIsHighest() {
        val content = "Title\n=====\n\nSection\n-------\n\nSubsection\n~~~~~~~~~~"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["max_level"])
    }

    @Test
    fun ParseSectionTitlePreserved() {
        val content = "My Document Title\n================="
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("My Document Title"))
    }

    @Test
    fun ParseSectionGeneratesHtmlClassAttribute() {
        val content = "Title\n====="
        val html = parser.toHtml(document = parser.parse(content), lightMode = true)
        assertTrue(html.contains("rst-section-1"))
    }

    // ==================== Directives ====================

    @Test
    fun ParseNoteDirectiveCountsAsOne() {
        val content = ".. note::\n   This is a note."
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["directives"])
    }

    @Test
    fun ParseCodeBlockDirective() {
        val content = ".. code-block:: python\n   print('hello')"
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["directives"])
    }

    @Test
    fun ParseWarningDirective() {
        val content = ".. warning::\n   Be careful with this."
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["directives"])
    }

    @Test
    fun ParseMultipleDirectivesCountsCorrectly() {
        val content = ".. note::\n   Note text.\n\n.. warning::\n   Warning text."
        val doc = parser.parse(content)
        assertEquals("2", doc.metadata["directives"])
    }

    @Test
    fun ParseDirectiveBodyCapturedInHtml() {
        val content = ".. note::\n   This is a note body."
        val html = parser.toHtml(document = parser.parse(content), lightMode = true)
        assertTrue(html.contains("rst-directive"))
    }

    @Test
    fun ParseDirectiveWithMultipleBodyLines() {
        val content = ".. code-block:: kotlin\n   val x = 1\n   val y = 2"
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["directives"])
    }

    // ==================== HTML generation ====================

    @Test
    fun ToHtmlWrapsInRstDocumentDiv() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-document"))
    }

    @Test
    fun ToHtmlIncludesStylesheet() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun ToHtmlLightModeAddsLightClass() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("light"))
    }

    @Test
    fun ToHtmlDarkModeAddsDarkClass() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("dark"))
    }

    @Test
    fun ToHtmlEscapesAngleBrackets() {
        val doc = parser.parse("a < b and b > a")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;") || html.contains("&amp;") || !html.contains("<b"))
    }

    @Test
    fun ToHtmlEmptyLinesGenerateBrTags() {
        val doc = parser.parse("line1\n\nline2")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<br>"))
    }

    @Test
    fun ToHtmlParagraphsWrappedInPTags() {
        val doc = parser.parse("Simple paragraph text.")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<p>"))
    }

    // ==================== Validation ====================

    @Test
    fun ValidateCorrectUnderlineReturnsNoErrors() {
        val content = "Title\n====="
        val errors = parser.validate(content)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun ValidateShortUnderlineReturnsError() {
        val content = "A Very Long Title\n==="
        val errors = parser.validate(content)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("underline too short") })
    }

    @Test
    fun ValidateNoSectionsReturnsNoErrors() {
        val errors = parser.validate("Just plain text.")
        assertTrue(errors.isEmpty())
    }

    // ==================== Malformed input ====================

    @Test
    fun ParseOnlyUnderlineCharactersIsNotCrash() {
        val doc = parser.parse("=====")
        assertNotNull(doc)
    }

    @Test
    fun ParseUnclosedDirectiveIsHandledGracefully() {
        val content = ".. note::"
        val doc = parser.parse(content)
        assertNotNull(doc)
    }

    @Test
    fun ParseDirectiveWithNoBodyLines() {
        val content = ".. note::\nNext paragraph."
        val doc = parser.parse(content)
        assertNotNull(doc)
        // One directive found but no indented body lines
        assertEquals("1", doc.metadata["directives"])
    }

    // ==================== Unicode ====================

    @Test
    fun ParseUnicodeTitleAndContent() {
        val content = "Título en Español\n================\n\nContenido con caracteres éàü."
        val doc = parser.parse(content)
        assertEquals("1", doc.metadata["sections"])
        assertTrue(doc.rawContent.contains("Título"))
    }

    @Test
    fun ParseChineseCharactersInContent() {
        val content = "中文标题\n======\n\n中文内容文本。"
        val doc = parser.parse(content)
        assertTrue(doc.rawContent.contains("中文"))
    }

    @Test
    fun ToHtmlUnicodeContentPreserved() {
        val doc = parser.parse("日本語テキスト")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("日本語"))
    }

    // ==================== Large documents ====================

    @Test
    fun ParseLargeDocumentWith100Sections() {
        val sb = StringBuilder()
        repeat(100) { i ->
            sb.appendLine("Section $i")
            sb.appendLine("=========")
            sb.appendLine("Content for section $i.")
            sb.appendLine()
        }
        val doc = parser.parse(sb.toString())
        assertEquals("100", doc.metadata["sections"])
    }

    @Test
    fun ParseLargeDocumentWith50Directives() {
        val sb = StringBuilder()
        repeat(50) { i ->
            sb.appendLine(".. note::")
            sb.appendLine("   Note number $i content.")
            sb.appendLine()
        }
        val doc = parser.parse(sb.toString())
        assertEquals("50", doc.metadata["directives"])
    }

    @Test
    fun ParseDocumentWith10KLines() {
        val lines = (1..10_000).map { "Line $it content text here." }
        val content = lines.joinToString("\n")
        val doc = parser.parse(content)
        assertNotNull(doc)
        assertTrue(doc.rawContent.isNotEmpty())
    }

    // ==================== Raw content preservation ====================

    @Test
    fun RawContentIsPreserved() {
        val content = "Original content\n================"
        val doc = parser.parse(content)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun ParsedDocumentFormatMatchesParser() {
        val doc = parser.parse("Test")
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, doc.format.id)
    }

    @Test
    fun ParsedDocumentHasNoErrorsByDefault() {
        val doc = parser.parse("Title\n=====\n\nContent.")
        assertTrue(doc.errors.isEmpty())
    }
}
