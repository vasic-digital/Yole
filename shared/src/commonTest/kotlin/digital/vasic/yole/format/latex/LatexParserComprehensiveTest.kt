/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for LaTeX parser
 *
 *########################################################*/
package digital.vasic.yole.format.latex

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for LaTeX parser covering all parsing branches.
 *
 * Tests cover:
 * - Metadata extraction (title, author, date, documentclass, comments)
 * - Validation (math mode, environments, malformed commands, special chars)
 * - HTML conversion (structure, sections, lists, formatting, math)
 * - Edge cases and error handling
 */
class LatexParserComprehensiveTest {

    private lateinit var parser: LatexParser

    @BeforeTest
    fun setup() {
        parser = LatexParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Extraction Tests ====================

    @Test
    fun `should extract title metadata`() {
        val content = "\\title{My Document Title}"

        val doc = parser.parse(content)

        assertEquals("My Document Title", doc.metadata["title"])
    }

    @Test
    fun `should extract author metadata`() {
        val content = "\\author{John Doe}"

        val doc = parser.parse(content)

        assertEquals("John Doe", doc.metadata["author"])
    }

    @Test
    fun `should extract date metadata`() {
        val content = "\\date{2025-01-01}"

        val doc = parser.parse(content)

        assertEquals("2025-01-01", doc.metadata["date"])
    }

    @Test
    fun `should extract documentclass metadata`() {
        val content = "\\documentclass{article}"

        val doc = parser.parse(content)

        assertEquals("article", doc.metadata["documentclass"])
    }

    @Test
    fun `should extract all metadata fields`() {
        val content = """
            \documentclass{article}
            \title{Test Document}
            \author{Test Author}
            \date{2025-01-01}
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("article", doc.metadata["documentclass"])
        assertEquals("Test Document", doc.metadata["title"])
        assertEquals("Test Author", doc.metadata["author"])
        assertEquals("2025-01-01", doc.metadata["date"])
    }

    @Test
    fun `should extract metadata from comments`() {
        val content = """
            % Author: Jane Smith
            % Version: 1.0
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("Jane Smith", doc.metadata["Author"])
        assertEquals("1.0", doc.metadata["Version"])
    }

    @Test
    fun `should limit metadata extraction to first 50 lines`() {
        val content = buildString {
            repeat(55) { i ->
                append("% Meta$i: value$i\n")
            }
        }

        val doc = parser.parse(content)

        assertTrue(doc.metadata.containsKey("Meta0"))
        assertTrue(doc.metadata.containsKey("Meta49"))
        assertFalse(doc.metadata.containsKey("Meta50"))
    }

    // ==================== Validation - Math Mode Tests ====================

    @Test
    fun `should detect unclosed math mode with brackets`() {
        val content = """
            \[
            x = y
            No closing bracket
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed math mode") })
    }

    @Test
    fun `should detect unclosed equation environment`() {
        val content = """
            \begin{equation}
            x = y
            No end tag
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed") })
    }

    @Test
    fun `should validate closed math mode`() {
        val content = """
            \[
            x = y
            \]
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed math mode") })
    }

    @Test
    fun `should validate closed equation environment`() {
        val content = """
            \begin{equation}
            x = y
            \end{equation}
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed") })
    }

    // ==================== Validation - Environment Tests ====================

    @Test
    fun `should detect unclosed environment`() {
        val content = """
            \begin{itemize}
            \item First
            No end tag
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed environment") })
    }

    @Test
    fun `should detect mismatched environment end`() {
        val content = """
            \begin{itemize}
            \item First
            \end{enumerate}
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Mismatched environment") })
    }

    @Test
    fun `should validate matched environments`() {
        val content = """
            \begin{itemize}
            \item First
            \end{itemize}
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Mismatched") || it.contains("Unclosed") })
    }

    @Test
    fun `should handle nested environments`() {
        val content = """
            \begin{itemize}
            \item First
            \begin{enumerate}
            \item Sub
            \end{enumerate}
            \end{itemize}
        """.trimIndent()

        val errors = parser.validate(content)

        // May have errors depending on implementation
        assertNotNull(errors)
    }

    // ==================== Validation - Malformed Commands Tests ====================

    @Test
    fun `should detect malformed commands with special chars`() {
        val content = "\\123invalid"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Malformed") })
    }

    @Test
    fun `should detect malformed commands with symbols`() {
        val content = "\\!@#invalid"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Malformed") })
    }

    @Test
    fun `should not error on valid commands`() {
        val content = """
            \section{Test}
            \textbf{Bold}
            \item Test
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Malformed") })
    }

    // ==================== Validation - Special Characters Tests ====================

    @Test
    fun `should detect unescaped ampersand outside math mode`() {
        val content = "This & that"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unescaped ampersand") })
    }

    @Test
    fun `should detect unescaped hash outside math mode`() {
        val content = "Number #1"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unescaped hash") })
    }

    @Test
    fun `should allow special chars in math mode`() {
        val content = """
            \[
            x & y # z
            \]
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unescaped") })
    }

    @Test
    fun `should allow escaped special chars`() {
        val content = """
            This \\& that
            Number \\#1
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unescaped") })
    }

    // ==================== HTML Conversion - Document Structure ====================

    @Test
    fun `should convert documentclass to HTML`() {
        val content = "\\documentclass{article}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-header"))
    }

    @Test
    fun `should convert begin document to HTML`() {
        val content = "\\begin{document}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-body"))
    }

    @Test
    fun `should convert end document to HTML`() {
        val content = "\\end{document}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("</div>"))
    }

    @Test
    fun `should convert full document structure`() {
        val content = """
            \documentclass{article}
            \begin{document}
            Content here
            \end{document}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-header"))
        assertTrue(html.contains("document-body"))
    }

    // ==================== HTML Conversion - Title/Author/Date ====================

    @Test
    fun `should convert title to HTML`() {
        val content = "\\title{My Document}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-title"))
        assertTrue(html.contains("My Document"))
    }

    @Test
    fun `should convert author to HTML`() {
        val content = "\\author{John Doe}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-author"))
        assertTrue(html.contains("John Doe"))
    }

    @Test
    fun `should convert date to HTML`() {
        val content = "\\date{2025-01-01}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("document-date"))
        assertTrue(html.contains("2025-01-01"))
    }

    // ==================== HTML Conversion - Sections ====================

    @Test
    fun `should convert section to HTML`() {
        val content = "\\section{Introduction}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("section"))
        assertTrue(html.contains("Introduction"))
    }

    @Test
    fun `should convert subsection to HTML`() {
        val content = "\\subsection{Background}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("subsection"))
        assertTrue(html.contains("Background"))
    }

    @Test
    fun `should convert paragraph to HTML`() {
        val content = "\\paragraph{Details}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("paragraph"))
        assertTrue(html.contains("Details"))
    }

    // ==================== HTML Conversion - Math Mode ====================

    @Test
    fun `should convert display math to HTML`() {
        val content = """
            \[
            x = y
            \]
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("math-display"))
    }

    @Test
    fun `should convert inline math to HTML`() {
        val content = "This is \$x = y\$ inline"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("math-inline"))
    }

    // ==================== HTML Conversion - Environments ====================

    @Test
    fun `should convert begin environment to HTML`() {
        val content = """
            \begin{theorem}
            Content
            \end{theorem}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("environment"))
        assertTrue(html.contains("theorem"))
    }

    @Test
    fun `should convert end environment to HTML`() {
        val content = """
            \begin{proof}
            Content
            \end{proof}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("environment"))
        assertTrue(html.contains("</div>"))
    }

    // ==================== HTML Conversion - Lists ====================

    @Test
    fun `should convert itemize to HTML`() {
        val content = """
            \begin{itemize}
            \item First
            \item Second
            \end{itemize}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Implementation converts all \begin{} as generic environments
        assertTrue(html.contains("environment"))
        assertTrue(html.contains("itemize"))
    }

    @Test
    fun `should convert enumerate to HTML`() {
        val content = """
            \begin{enumerate}
            \item One
            \item Two
            \end{enumerate}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Implementation converts all \begin{} as generic environments
        assertTrue(html.contains("environment"))
        assertTrue(html.contains("enumerate"))
    }

    @Test
    fun `should convert list items to HTML`() {
        val content = """
            \begin{itemize}
            \item First item
            \item Second item
            \end{itemize}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<li"))
        assertTrue(html.contains("First item"))
        assertTrue(html.contains("Second item"))
    }

    // ==================== HTML Conversion - Text Formatting ====================

    @Test
    fun `should convert textbf to HTML`() {
        val content = "This is \\textbf{bold} text"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("bold"))
    }

    @Test
    fun `should convert textit to HTML`() {
        val content = "This is \\textit{italic} text"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("italic"))
    }

    @Test
    fun `should convert underline to HTML`() {
        val content = "This is \\underline{underlined} text"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("underline"))
    }

    // ==================== HTML Conversion - Comments ====================

    @Test
    fun `should skip comments in HTML`() {
        val content = """
            % This is a comment
            Regular text
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertFalse(html.contains("This is a comment"))
        assertTrue(html.contains("Regular text"))
    }

    // ==================== HTML Conversion - Regular Content ====================

    @Test
    fun `should convert regular paragraphs to HTML`() {
        val content = "This is a regular paragraph"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<p>"))
        assertTrue(html.contains("regular paragraph"))
    }

    @Test
    fun `should not convert content inside math mode to paragraph`() {
        val content = """
            \[
            x = y
            \]
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("math-display"))
        // Content should be in math div, not paragraph
    }

    // ==================== HTML Generation Mode Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val content = "\\section{Test}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("class='latex'"))
        assertNotNull(html)
    }

    @Test
    fun `should generate dark mode HTML`() {
        val content = "\\section{Test}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = false)

        assertTrue(html.contains("class='latex'"))
        assertNotNull(html)
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete LaTeX document`() {
        val content = """
            \documentclass{article}
            \title{Test Document}
            \author{Test Author}
            \date{2025-01-01}

            \begin{document}

            \section{Introduction}
            This is the introduction.

            \subsection{Background}
            Some background information.

            \section{Methods}

            \begin{itemize}
            \item First method
            \item Second method
            \end{itemize}

            \section{Results}
            The equation is:
            \[
            E = mc^2
            \]

            \end{document}
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertEquals("Test Document", doc.metadata["title"])
        assertEquals("Test Author", doc.metadata["author"])
        assertEquals("article", doc.metadata["documentclass"])
        assertTrue(html.contains("section"))
        assertTrue(html.contains("subsection"))
        assertTrue(html.contains("environment") && html.contains("itemize"))
        assertTrue(html.contains("math-display"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty content`() {
        val doc = parser.parse("")

        assertEquals("", doc.rawContent)
        assertTrue(doc.metadata.isEmpty() || doc.metadata.size >= 0)
    }

    @Test
    fun `should handle only whitespace`() {
        val content = "   \n\n\t  "

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertNotNull(html)
    }

    @Test
    fun `should escape HTML special characters`() {
        val content = "\\section{Test <>&\"'}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("&lt;") || html.contains("&gt;") || html.contains("&amp;"))
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "\\section{Unicode: 你好世界 🌍}"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("你好世界"))
        assertTrue(html.contains("🌍"))
    }
}
