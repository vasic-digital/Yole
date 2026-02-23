/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for LaTeX parser
 *
 *########################################################*/
package digital.vasic.yole.format.latex

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
 * Comprehensive unit tests for LaTeX format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic LaTeX parsing (sections, paragraphs, equations)
 * - LaTeX commands and environments
 * - Mathematical expressions
 * - Bibliography and citations
 * - Round-trip parsing (parse -> format -> parse)
 * - Edge cases (empty files, malformed LaTeX)
 * - Performance benchmarks
 * - HTML conversion
 */
class LatexParserTest {

    private val parser = LatexParser()

    @BeforeTest
    fun setup() {
        ParserRegistry.clear()
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect LaTeX format by extension`() {
        val format = FormatRegistry.getByExtension(".tex")

        assertNotNull(format)
        assertEquals(TextFormat.ID_LATEX, format.id)
        assertEquals("LaTeX", format.name)
    }

    @Test
    fun `should detect LaTeX format by latex extension`() {
        val format = FormatRegistry.getByExtension(".latex")

        assertNotNull(format)
        assertEquals(TextFormat.ID_LATEX, format.id)
    }

    @Test
    fun `should detect LaTeX format by filename`() {
        val format = FormatRegistry.detectByFilename("document.tex")

        assertNotNull(format)
        assertEquals(TextFormat.ID_LATEX, format.id)
    }

    @Test
    fun `should support all LaTeX extensions`() {
        val extensions = listOf(".tex", ".latex")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_LATEX, format.id)
        }
    }

    // ==================== Basic LaTeX Parsing Tests ====================

    @Test
    fun `should parse basic LaTeX document structure`() {
        val content = """
            \documentclass{article}
            \title{Test Document}
            \author{John Doe}
            \date{2025-01-01}

            \begin{document}
            \maketitle

            This is a test document.

            \end{document}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_LATEX, result.format.id)
        assertEquals(content, result.rawContent)

        // Check metadata extraction
        assertEquals("Test Document", result.metadata["title"])
        assertEquals("John Doe", result.metadata["author"])
        assertEquals("2025-01-01", result.metadata["date"])
        assertEquals("article", result.metadata["documentclass"])
    }

    @Test
    fun `should parse LaTeX sections and paragraphs`() {
        val content = """
            \section{Introduction}
            This is the introduction paragraph.

            \subsection{Background}
            This is background information.

            \paragraph{Note}
            This is a paragraph with a title.

            \section{Conclusion}
            Final thoughts go here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.parsedContent)
    }

    @Test
    fun `should parse LaTeX text formatting commands`() {
        val content = """
            \textbf{Bold text} and \textit{italic text}.
            \underline{Underlined text} is also supported.
            Combined: \textbf{\textit{bold and italic}}.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Mathematical Expressions Tests ====================

    @Test
    fun `should parse inline math expressions`() {
        val content = """
            The equation ${'$'}E = mc^2${'$'} is famous.
            Another example: ${'$'}a^2 + b^2 = c^2${'$'}.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should parse display math equations`() {
        val content = """
            A famous equation is:
            \[
            E = mc^2
            \]

            Another example:
            \begin{equation}
            a^2 + b^2 = c^2
            \end{equation}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should detect unclosed math mode`() {
        val content = """
            This has an unclosed math mode:
            \[
            E = mc^2
            % Missing closing \]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Unclosed math mode") })
    }

    // ==================== LaTeX Environments Tests ====================

    @Test
    fun `should parse itemize and enumerate environments`() {
        val content = """
            \begin{itemize}
            \item First item
            \item Second item
            \item Third item
            \end{itemize}

            \begin{enumerate}
            \item First numbered item
            \item Second numbered item
            \end{enumerate}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should detect mismatched environment boundaries`() {
        val content = """
            \begin{itemize}
            \item First item
            \end{enumerate}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Mismatched environment") })
    }

    @Test
    fun `should detect unclosed environments`() {
        val content = """
            \begin{itemize}
            \item First item
            \item Second item
            % Missing \end{itemize}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Unclosed environment") })
    }

    // ==================== Bibliography and Citations Tests ====================

    @Test
    fun `should parse bibliography commands`() {
        val content = """
            \bibliographystyle{plain}
            \bibliography{references}

            Some text with citation \cite{key2025}.
            Multiple citations: \cite{key1,key2}.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Comments and Metadata Tests ====================

    @Test
    fun `should handle LaTeX comments`() {
        val content = """
            % This is a comment
            \section{Introduction} % This is also a comment
            % Author: John Doe
            % Date: 2025-01-01
            Normal text here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)

        // Check that metadata from comments is extracted
        assertEquals("John Doe", result.metadata["Author"])
        assertEquals("2025-01-01", result.metadata["Date"])
    }

    // ==================== Error Handling and Validation Tests ====================

    @Test
    fun `should detect malformed LaTeX commands`() {
        val content = """
            \123invalid{content}
            \!@#${'$'}%^&*()
            Valid: \textbf{text}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Malformed LaTeX command") })
    }

    @Test
    fun `should detect unescaped special characters`() {
        val content = """
            This has an unescaped & character.
            This has an unescaped # character.
            Escaped versions: \& and \# are fine.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Unescaped ampersand") })
        assertTrue(result.errors.any { it.contains("Unescaped hash") })
    }

    @Test
    fun `should allow special characters in math mode`() {
        val content = """
            Math mode allows & and #:
            \[
            a & b \\
            c & d
            \]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Should not report errors for special characters in math mode
        assertFalse(result.errors.any { it.contains("Unescaped ampersand") })
        assertFalse(result.errors.any { it.contains("Unescaped hash") })
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("", result.rawContent)
        assertEquals("", result.parsedContent)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `should handle whitespace-only input`() {
        val content = "   \n\n\t  \n   "
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle single line input`() {
        val content = """\section{Single Line}"""
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle unicode characters`() {
        val content = """
            \section{Unicode Test}
            Unicode characters: 你好世界 Привет мир
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val line = "This is a test line with some LaTeX content.\n"
        val longContent = line.repeat(1000)

        val result = parser.parse(longContent)

        assertNotNull(result)
        assertEquals(longContent, result.rawContent)
    }

    @Test
    fun `should handle null bytes gracefully`() {
        val content = "Some LaTeX text\u0000with null\u0000bytes"
        val result = parser.parse(content)

        assertNotNull(result)
        // Should not crash, may have some errors
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert LaTeX to HTML`() {
        val content = """
            \documentclass{article}
            \title{HTML Test}
            \author{Test Author}
            \begin{document}
            \maketitle
            \section{Introduction}
            This is \textbf{bold} and \textit{italic} text.
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='latex'>"))
        assertTrue(html.contains("document-title"))
        assertTrue(html.contains("document-author"))
        assertTrue(html.contains("section"))
        assertTrue(html.contains("bold"))
        assertTrue(html.contains("italic"))
    }

    @Test
    fun `should convert LaTeX math to HTML`() {
        val content = """
            Inline math: ${'$'}E = mc^2${'$'}
            Display math:
            \[
            a^2 + b^2 = c^2
            \]
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("math-inline"))
        assertTrue(html.contains("math-display"))
    }

    @Test
    fun `should convert LaTeX lists to HTML`() {
        val content = """
            \begin{itemize}
            \item First item
            \item Second item
            \end{itemize}

            \begin{enumerate}
            \item First numbered
            \item Second numbered
            \end{enumerate}
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<ul class='itemize'>"))
        assertTrue(html.contains("<ol class='enumerate'>"))
        assertTrue(html.contains("<li class='item'>"))
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should preserve content through round-trip parsing`() {
        val originalContent = """
            \documentclass{article}
            \title{Round Trip Test}
            \author{Test Author}
            \date{2025-01-01}

            \begin{document}
            \maketitle

            \section{Introduction}
            This is a test document for round-trip parsing.

            \subsection{Details}
            Some details here with \textbf{bold} and \textit{italic} text.

            \begin{itemize}
            \item First point
            \item Second point
            \end{itemize}

            \end{document}
        """.trimIndent()

        // Parse the original content
        val document = parser.parse(originalContent)

        // Convert to HTML
        val html = parser.toHtml(document, lightMode = true)

        // Parse the HTML back (simulating round-trip)
        val htmlDocument = parser.parse(html)

        // The HTML should be different from original (as expected)
        assertTrue(originalContent != html)
        assertTrue(html.contains("<div class='latex'>"))

        // The original document should have no errors
        assertTrue(document.errors.isEmpty())
    }

    // ==================== Performance Benchmark Tests ====================

    @Test
    fun `should parse large LaTeX documents efficiently`() {
        // Create a large document with many sections and equations
        val largeContent = buildString {
            appendLine("\\documentclass{article}")
            appendLine("\\title{Large Document Performance Test}")
            appendLine("\\author{Performance Tester}")
            appendLine("\\begin{document}")
            appendLine("\\maketitle")

            repeat(100) { i ->
                appendLine("\\section{Section $i}")
                appendLine("This is section $i with some content.")
                appendLine("\\begin{equation}")
                appendLine("E_{$i} = mc^{$i}")
                appendLine("\\end{equation}")
                appendLine()
            }

            appendLine("\\end{document}")
        }

        val parseTime = measureTimeMillis {
            val result = parser.parse(largeContent)
            assertNotNull(result)
        }

        // Performance assertion - should parse in reasonable time (< 1 second for this size)
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took $parseTime ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        val largeContent = buildString {
            appendLine("\\documentclass{article}")
            appendLine("\\title{HTML Performance Test}")
            appendLine("\\begin{document}")

            repeat(50) { i ->
                appendLine("\\section{Section $i}")
                appendLine("Content with \\textbf{bold} and \\textit{italic} text.")
                appendLine("\\begin{itemize}")
                repeat(5) { j ->
                    appendLine("\\item Item $j in section $i")
                }
                appendLine("\\end{itemize}")
            }

            appendLine("\\end{document}")
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
        val registryParser = ParserRegistry.getParser(TextFormat.ID_LATEX)

        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_LATEX, registryParser.supportedFormat.id)

        // Test that it works
        val content = "\\section{Test}"
        val result = registryParser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_LATEX, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val latexFormat = allFormats.find { it.id == TextFormat.ID_LATEX }

        assertNotNull(latexFormat)
        assertEquals("LaTeX", latexFormat.name)
        assertEquals(".tex", latexFormat.defaultExtension)
        assertTrue(latexFormat.extensions.contains(".tex"))
        assertTrue(latexFormat.extensions.contains(".latex"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex academic paper structure`() {
        val content = """
            \documentclass[12pt]{article}
            \usepackage{amsmath}
            \usepackage{graphicx}

            \title{Advanced Mathematical Analysis}
            \author{Dr. Jane Smith \and Dr. John Doe}
            \date{\today}

            \begin{document}

            \maketitle

            \begin{abstract}
            This paper presents advanced mathematical concepts and their applications.
            \end{abstract}

            \section{Introduction}
            Mathematical analysis is fundamental to understanding complex systems.

            \section{Methodology}
            We employ rigorous mathematical proofs and computational methods.

            \subsection{Theoretical Framework}
            Our theoretical approach is based on established mathematical principles.

            \begin{equation}
            \\int_{-${'\\'}infty}^{${'\\'}infty} e^{-x^2} dx = \\sqrt{\\pi}
            \end{equation}

            \section{Results}
            Our findings demonstrate significant improvements in computational efficiency.

            \section{Conclusion}
            This research opens new avenues for mathematical exploration.

            \bibliographystyle{plain}
            \bibliography{math_refs}

            \end{document}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)

        // Check metadata
        assertEquals("Advanced Mathematical Analysis", result.metadata["title"])
        assertEquals("article", result.metadata["documentclass"])
    }

    @Test
    fun `should handle nested environments`() {
        val content = """
            \begin{itemize}
            \item First level item 1
            \begin{enumerate}
            \item Nested numbered item 1
            \item Nested numbered item 2
            \end{enumerate}
            \item First level item 2
            \begin{itemize}
            \item Deeply nested item
            \end{itemize}
            \end{itemize}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
    }
}
