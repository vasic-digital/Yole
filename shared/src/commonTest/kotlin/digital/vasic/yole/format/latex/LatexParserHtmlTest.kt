/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for LatexParser HTML generation and validation
 *
 *########################################################*/
package digital.vasic.yole.format.latex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LatexParserHtmlTest {

    private val parser = LatexParser()

    // ==================== Document structure ====================

    @Test
    fun testDocumentClassGeneratesHeader() {
        val content = "\\documentclass{article}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("document-header"))
    }

    @Test
    fun testBeginDocumentGeneratesBody() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("document-body"))
    }

    @Test
    fun testTitleExtraction() {
        val content = "\\title{My Paper}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("document-title"))
        assertTrue(html.contains("My Paper"))
    }

    @Test
    fun testAuthorExtraction() {
        val content = "\\author{John Doe}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("document-author"))
        assertTrue(html.contains("John Doe"))
    }

    @Test
    fun testDateExtraction() {
        val content = "\\date{2026-01-01}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("document-date"))
        assertTrue(html.contains("2026-01-01"))
    }

    // ==================== Sections ====================

    @Test
    fun testSectionGeneration() {
        val content = "\\section{Introduction}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("class='section'"))
        assertTrue(html.contains("Introduction"))
    }

    @Test
    fun testSubsectionGeneration() {
        val content = "\\subsection{Background}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("class='subsection'"))
        assertTrue(html.contains("Background"))
    }

    @Test
    fun testParagraphGeneration() {
        val content = "\\paragraph{Note}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("class='paragraph'"))
        assertTrue(html.contains("Note"))
    }

    // ==================== Math mode ====================

    @Test
    fun testDisplayMathGeneration() {
        val content = "\\[\nx^2 + y^2 = z^2\n\\]"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("math-display"))
    }

    // ==================== Lists ====================

    @Test
    fun testItemizeList() {
        val content = "\\begin{itemize}\n\\item First\n\\item Second\n\\end{itemize}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ul class='itemize'>"))
        assertTrue(html.contains("<li class='item'>"))
        assertTrue(html.contains("First"))
        assertTrue(html.contains("Second"))
    }

    @Test
    fun testEnumerateList() {
        val content = "\\begin{enumerate}\n\\item Alpha\n\\item Beta\n\\end{enumerate}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ol class='enumerate'>"))
        assertTrue(html.contains("<li class='item'>"))
    }

    // ==================== Environments ====================

    @Test
    fun testGenericEnvironment() {
        val content = "\\begin{theorem}\nPythagoras\n\\end{theorem}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("class='environment'"))
        assertTrue(html.contains("environment-title"))
        assertTrue(html.contains("theorem"))
    }

    // ==================== Validation ====================

    @Test
    fun testValidDocumentNoErrors() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val doc = parser.parse(content)
        assertTrue(doc.errors.isEmpty(), "Valid LaTeX should produce no errors")
    }

    @Test
    fun testUnclosedMathModeError() {
        val content = "\\[\nx^2"
        val doc = parser.parse(content)
        assertTrue(doc.errors.any { it.contains("Unclosed math mode") })
    }

    @Test
    fun testUnclosedEnvironmentError() {
        val content = "\\begin{theorem}\nContent"
        val doc = parser.parse(content)
        assertTrue(doc.errors.any { it.contains("Unclosed environment") })
    }

    @Test
    fun testMismatchedEnvironmentError() {
        val content = "\\begin{theorem}\n\\end{lemma}"
        val doc = parser.parse(content)
        assertTrue(doc.errors.any { it.contains("Mismatched") })
    }

    @Test
    fun testMalformedCommandError() {
        val content = "\\1abc"
        val doc = parser.parse(content)
        assertTrue(doc.errors.any { it.contains("Malformed") })
    }

    @Test
    fun testUnescapedAmpersandError() {
        val content = "A & B"
        val doc = parser.parse(content)
        assertTrue(doc.errors.any { it.contains("ampersand") })
    }

    @Test
    fun testEscapedAmpersandNoError() {
        val content = "A \\& B"
        val doc = parser.parse(content)
        assertTrue(doc.errors.none { it.contains("ampersand") })
    }

    @Test
    fun testCommentLinesSkippedInValidation() {
        val content = "% This is a comment with & and #"
        val doc = parser.parse(content)
        assertTrue(doc.errors.isEmpty(), "Comments should not trigger validation errors")
    }

    @Test
    fun testMathModeSkipsSpecialCharCheck() {
        val content = "\\[\na & b \\\\\nc & d\n\\]"
        val doc = parser.parse(content)
        assertTrue(doc.errors.none { it.contains("ampersand") },
            "Ampersands in math mode should not trigger errors")
    }

    // ==================== Metadata extraction ====================

    @Test
    fun testMetadataTitle() {
        val content = "\\title{Test Title}"
        val doc = parser.parse(content)
        assertEquals("Test Title", doc.metadata["title"])
    }

    @Test
    fun testMetadataAuthor() {
        val content = "\\author{Jane Smith}"
        val doc = parser.parse(content)
        assertEquals("Jane Smith", doc.metadata["author"])
    }

    @Test
    fun testMetadataDate() {
        val content = "\\date{March 2026}"
        val doc = parser.parse(content)
        assertEquals("March 2026", doc.metadata["date"])
    }

    @Test
    fun testMetadataDocumentClass() {
        val content = "\\documentclass{report}"
        val doc = parser.parse(content)
        assertEquals("report", doc.metadata["documentclass"])
    }

    @Test
    fun testMetadataFromComments() {
        val content = "% Keywords: machine learning, AI"
        val doc = parser.parse(content)
        assertEquals("machine learning, AI", doc.metadata["Keywords"])
    }

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertTrue(doc.errors.isEmpty())
    }

    // ==================== Light/dark mode ====================

    @Test
    fun testLightModeStyleSheet() {
        val content = "\\section{Test}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun testDarkModeStyleSheet() {
        val content = "\\section{Test}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun testLightAndDarkModeDiffer() {
        val content = "\\section{Test}"
        val doc = parser.parse(content)
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark, "Light and dark mode HTML should differ")
    }

    // ==================== HTML wrapping ====================

    @Test
    fun testOutputWrappedInLatexDiv() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<div class='latex'>"))
    }

    @Test
    fun testSectionTitleEscapesHtml() {
        val content = "\\section{A < B & C}"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;") || html.contains("&amp;"))
    }
}
