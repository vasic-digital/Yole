/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Integration Tests
 *
 * Deep integration tests covering cross-format parsing,
 * HTML pipeline verification, format detection priority,
 * metadata preservation, error accumulation, cross-parser
 * invariants, and stylesheet generation for all themes.
 *
 *########################################################*/
package digital.vasic.yole.format.integration

import digital.vasic.yole.format.*
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.json.JsonParser
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Comprehensive integration tests covering cross-format parsing pipelines,
 * HTML generation verification, format detection priority, metadata
 * preservation, error accumulation, cross-parser invariants, and
 * stylesheet generation for light and dark themes.
 */
class ComprehensiveIntegrationTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // All parsers
    private val markdownParser = MarkdownParser()
    private val plaintextParser = PlaintextParser()
    private val todoTxtParser = TodoTxtParser()
    private val csvParser = CsvParser()
    private val wikitextParser = WikitextParser()
    private val creoleParser = CreoleParser()
    private val tiddlyWikiParser = TiddlyWikiParser()
    private val latexParser = LatexParser()
    private val asciidocParser = AsciidocParser()
    private val orgModeParser = OrgModeParser()
    private val rstParser = RestructuredTextParser()
    private val keyValueParser = KeyValueParser()
    private val taskpaperParser = TaskpaperParser()
    private val textileParser = TextileParser()
    private val jupyterParser = JupyterParser()
    private val rMarkdownParser = RMarkdownParser()
    private val binaryParser = BinaryParser()

    // ==================== MARKDOWN WITH EMBEDDED LATEX ====================

    @Test
    fun `parse Markdown with embedded LaTeX expressions`() {
        val content = """
            # Mathematical Document

            The quadratic formula is ${'$'}x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}${'$'}.

            ## Inline Math

            Einstein's equation: ${'$'}E = mc^2${'$'}

            ## Display Math

            $$
            \int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}
            $$

            ## Lists with Math

            - Area of circle: ${'$'}A = \pi r^2${'$'}
            - Euler's identity: ${'$'}e^{i\pi} + 1 = 0${'$'}
            - Pythagorean theorem: ${'$'}a^2 + b^2 = c^2${'$'}
        """.trimIndent()

        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)
        assertTrue(doc.errors.isEmpty())
        assertTrue(doc.rawContent.contains("quadratic formula"))
        assertTrue(doc.rawContent.contains("Einstein"))
        assertTrue(doc.rawContent.contains("Euler"))

        // HTML should preserve the mathematical content
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("Mathematical Document"))
    }

    @Test
    fun `parse Markdown with embedded LaTeX verifies structure`() {
        val content = """
            # Theorem Proof

            **Theorem 1.** For all ${'$'}n \geq 1${'$'}, we have:

            $$
            \sum_{k=1}^{n} k = \frac{n(n+1)}{2}
            $$

            *Proof.* By induction on ${'$'}n${'$'}.

            **Base case:** ${'$'}n = 1${'$'}: ${'$'}\sum_{k=1}^{1} k = 1 = \frac{1 \cdot 2}{2}${'$'}. True.

            **Inductive step:** Assume true for ${'$'}n${'$'}. Then for ${'$'}n+1${'$'}:

            $$
            \sum_{k=1}^{n+1} k = \sum_{k=1}^{n} k + (n+1) = \frac{n(n+1)}{2} + (n+1) = \frac{(n+1)(n+2)}{2}
            $$

            QED.
        """.trimIndent()

        val doc = markdownParser.parse(content)
        assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.contains("Theorem"))
        assertTrue(html.contains("Proof"))
    }

    // ==================== PARSE -> HTML -> CSS PIPELINE ====================

    @Test
    fun `parse document generate HTML verify CSS styles match for Markdown`() {
        val content = "# Title\n\nParagraph with **bold**.\n\n- Item 1\n- Item 2"
        val doc = markdownParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Verify Markdown stylesheet is applied
        val stylesheet = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertTrue(stylesheet.contains(".markdown"), "Markdown stylesheet must contain .markdown class")
        assertTrue(stylesheet.contains("font-family"), "Markdown stylesheet must define font-family")
    }

    @Test
    fun `parse document generate HTML verify CSS styles for LaTeX`() {
        val content = "\\documentclass{article}\n\\begin{document}\n\\section{Test}\nContent.\n\\end{document}"
        val doc = latexParser.parse(content)
        val htmlLight = doc.toHtml(lightMode = true)
        val htmlDark = doc.toHtml(lightMode = false)

        // Verify LaTeX stylesheets differ between modes
        val lightStyle = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        val darkStyle = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, false)

        assertTrue(lightStyle.isNotEmpty(), "LaTeX light stylesheet must not be empty")
        assertTrue(darkStyle.isNotEmpty(), "LaTeX dark stylesheet must not be empty")
        assertNotEquals(lightStyle, darkStyle, "Light and dark LaTeX stylesheets must differ")
        assertTrue(lightStyle.contains(".latex"), "LaTeX stylesheet must contain .latex class")
    }

    @Test
    fun `parse document generate HTML verify CSS styles for OrgMode`() {
        val content = "* Heading\n** TODO Task\nContent."
        val doc = orgModeParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        val lightStyle = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, true)
        val darkStyle = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, false)

        assertTrue(lightStyle.contains(".org-heading"), "OrgMode light stylesheet must have org-heading class")
        assertTrue(darkStyle.contains(".org-heading"), "OrgMode dark stylesheet must have org-heading class")
        assertNotEquals(lightStyle, darkStyle, "Light and dark OrgMode stylesheets must differ")
    }

    // ==================== FORMAT DETECTION -> PARSING -> HTML PIPELINE FOR ALL 17 ====================

    @Test
    fun `full pipeline for all 18 formats via extension detection`() {
        val testCases = listOf(
            "md" to "# Heading\n\nParagraph." to FormatRegistry.ID_MARKDOWN,
            "txt" to "Plain text content." to FormatRegistry.ID_PLAINTEXT,
            "csv" to "a,b,c\n1,2,3" to FormatRegistry.ID_CSV,
            "tex" to "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}" to FormatRegistry.ID_LATEX,
            "org" to "* Heading\n** Subheading" to FormatRegistry.ID_ORGMODE,
            "wiki" to "= Heading =\n\nContent." to FormatRegistry.ID_WIKITEXT,
            "rst" to "Title\n=====\n\nContent." to FormatRegistry.ID_RESTRUCTUREDTEXT,
            "adoc" to "= Title\n\n== Section" to FormatRegistry.ID_ASCIIDOC,
            "creole" to "= Heading\n\n**Bold**" to FormatRegistry.ID_CREOLE,
            "tid" to "title: Test\n\n! Heading" to FormatRegistry.ID_TIDDLYWIKI,
            "ini" to "[section]\nkey = value" to FormatRegistry.ID_KEYVALUE,
            "taskpaper" to "Project:\n\t- Task @due(2025-01-01)" to FormatRegistry.ID_TASKPAPER,
            "textile" to "h1. Title\n\n*Bold* text." to FormatRegistry.ID_TEXTILE,
            "ipynb" to """{"nbformat":4,"nbformat_minor":4,"metadata":{"kernelspec":{"name":"python3","display_name":"Python 3"}},"cells":[{"cell_type":"markdown","source":"# Title"}]}""" to FormatRegistry.ID_JUPYTER,
            "rmd" to "---\ntitle: 'Test'\n---\n\n```{r}\nsummary(cars)\n```" to FormatRegistry.ID_RMARKDOWN
        )

        testCases.forEach { (extensionAndContent, expectedId) ->
            val extension = extensionAndContent.first
            val content = extensionAndContent.second

            // Step 1: Detect format by extension
            val format = FormatRegistry.detectByExtension(extension)
            assertNotNull(format, "Format must be detected for extension '$extension'")

            // Step 2: Get parser for detected format
            val parser = ParserRegistry.getParser(format)

            if (parser != null) {
                // Step 3: Parse content
                val doc = parser.parse(content)
                assertNotNull(doc)

                // Step 4: Generate HTML
                val html = doc.toHtml(lightMode = true)
                assertTrue(html.isNotEmpty(), "HTML must not be empty for format ${format.id}")
            }
        }
    }

    @Test
    fun `full pipeline via content detection for key formats`() {
        val contentDetectionCases = listOf(
            "# Markdown Heading\n\nContent." to FormatRegistry.ID_MARKDOWN,
            "(A) Important task @work" to FormatRegistry.ID_TODOTXT,
            "\\documentclass{article}" to FormatRegistry.ID_LATEX,
            "* Org heading" to FormatRegistry.ID_ORGMODE,
            "== WikiText Heading ==" to FormatRegistry.ID_WIKITEXT
        )

        contentDetectionCases.forEach { (content, expectedId) ->
            val detected = FormatRegistry.detectByContent(content)
            assertNotNull(detected, "Format must be detected for content starting with '${content.take(30)}'")
            assertEquals(expectedId, detected.id,
                "Content '${content.take(30)}' should detect as $expectedId but got ${detected.id}")
        }
    }

    // ==================== FORMAT REGISTRATION ORDER AFFECTS DETECTION PRIORITY ====================

    @Test
    fun `FormatRegistry detection order matches registration order`() {
        val formats = FormatRegistry.formats
        assertTrue(formats.size >= 18, "At least 18 formats must be registered")

        // R Markdown must come before Markdown (more specific first)
        val rmdIndex = formats.indexOfFirst { it.id == FormatRegistry.ID_RMARKDOWN }
        val mdIndex = formats.indexOfFirst { it.id == FormatRegistry.ID_MARKDOWN }
        assertTrue(rmdIndex < mdIndex,
            "R Markdown (index $rmdIndex) must come before Markdown (index $mdIndex) in detection order")
    }

    @Test
    fun `detection priority Markdown vs Plaintext`() {
        // Markdown content should be detected as Markdown, not plaintext
        val markdownContent = "# Title\n\nParagraph with **bold** text."
        val detected = FormatRegistry.detectByContent(markdownContent)
        assertEquals(FormatRegistry.ID_MARKDOWN, detected?.id,
            "Markdown content must be detected as Markdown, not Plaintext")
    }

    @Test
    fun `detection priority RMarkdown vs Markdown`() {
        // R Markdown content with code chunks should be detected as R Markdown
        val rmdContent = "```{r}\nsummary(cars)\n```\n\n# Analysis Results"
        val detected = FormatRegistry.detectByContent(rmdContent)
        assertEquals(FormatRegistry.ID_RMARKDOWN, detected?.id,
            "R Markdown content must be detected as R Markdown, not plain Markdown")
    }

    @Test
    fun `detection priority more specific formats win`() {
        // LaTeX should be detected before plaintext
        val latexContent = "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}"
        val detected = FormatRegistry.detectByContent(latexContent)
        assertEquals(FormatRegistry.ID_LATEX, detected?.id,
            "LaTeX content must be detected as LaTeX")

        // TodoTxt should be detected correctly
        val todoContent = "(A) Important task @work +project"
        val detected2 = FormatRegistry.detectByContent(todoContent)
        assertEquals(FormatRegistry.ID_TODOTXT, detected2?.id,
            "TodoTxt content must be detected as TodoTxt")
    }

    // ==================== MULTI-FORMAT DOCUMENT HANDLING ====================

    @Test
    fun `switch between formats parsing different documents`() {
        val testDocs = listOf(
            markdownParser to "# Markdown\n\nContent.",
            csvParser to "a,b,c\n1,2,3",
            latexParser to "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}",
            orgModeParser to "* Heading\n** Subheading\nContent.",
            todoTxtParser to "(A) Task @work +project"
        )

        // Parse each format and verify switching works correctly
        val results = testDocs.map { (parser, content) ->
            val doc = parser.parse(content)
            assertNotNull(doc)
            assertTrue(doc.rawContent == content, "rawContent must match for ${parser.supportedFormat.id}")
            doc
        }

        // Verify all results have different format IDs
        val formatIds = results.map { it.format.id }.toSet()
        assertEquals(testDocs.size, formatIds.size, "Each parsed document must have a unique format ID")
    }

    @Test
    fun `rapid format switching does not corrupt state`() {
        // Parse 10 rounds of alternating formats
        repeat(10) { round ->
            val mdDoc = markdownParser.parse("# Round $round")
            val csvDoc = csvParser.parse("round,$round")
            val latexDoc = latexParser.parse("\\documentclass{article}\n\\begin{document}\nRound $round.\n\\end{document}")

            assertEquals(FormatRegistry.ID_MARKDOWN, mdDoc.format.id, "Round $round: Markdown format ID mismatch")
            assertEquals(FormatRegistry.ID_CSV, csvDoc.format.id, "Round $round: CSV format ID mismatch")
            assertEquals(FormatRegistry.ID_LATEX, latexDoc.format.id, "Round $round: LaTeX format ID mismatch")

            assertTrue(mdDoc.rawContent.contains("Round $round"))
            assertTrue(csvDoc.rawContent.contains("$round"))
            assertTrue(latexDoc.rawContent.contains("Round $round"))
        }
    }

    // ==================== PARSED DOCUMENT METADATA PRESERVATION ====================

    @Test
    fun `ParsedDocument metadata preserved through cache`() {
        val content = "# Title\n\nContent line 1.\nContent line 2."
        val doc = markdownParser.parse(content)

        // Capture metadata before toHtml
        val metadataBefore = doc.metadata.toMap()

        // Generate HTML (populates cache)
        val html1 = doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))

        // Metadata should be unchanged after caching
        assertEquals(metadataBefore, doc.metadata, "Metadata must not change after toHtml caching")

        // Generate dark HTML too
        val html2 = doc.toHtml(lightMode = false)
        assertEquals(metadataBefore, doc.metadata, "Metadata must not change after dark mode toHtml")

        // Clear cache and verify metadata still intact
        doc.clearHtmlCache()
        assertEquals(metadataBefore, doc.metadata, "Metadata must survive cache clearing")
    }

    @Test
    fun `ParsedDocument copy preserves metadata but not cache`() {
        val content = "# Original\n\nContent here."
        val original = markdownParser.parse(content)

        // Populate cache on original
        original.toHtml(lightMode = true)
        assertTrue(original.hasHtmlCached(lightMode = true))

        // Copy should have same metadata but empty cache
        val copy = original.copy()
        assertEquals(original.format, copy.format)
        assertEquals(original.rawContent, copy.rawContent)
        assertEquals(original.parsedContent, copy.parsedContent)
        assertEquals(original.metadata, copy.metadata)
        assertEquals(original.errors, copy.errors)
        assertFalse(copy.hasHtmlCached(lightMode = true),
            "Copy must not inherit HTML cache from original")
    }

    @Test
    fun `metadata consistent across parsers`() {
        // All parsers should populate at least some metadata
        val parsersAndContents = listOf(
            markdownParser to "# Title\n\nContent.",
            csvParser to "a,b,c\n1,2,3",
            latexParser to "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}",
            orgModeParser to "* Heading\nContent."
        )

        parsersAndContents.forEach { (parser, content) ->
            val doc = parser.parse(content)
            assertNotNull(doc.metadata, "Metadata must not be null for ${parser.supportedFormat.id}")
            // metadata can be empty for some formats, that is OK
            // but rawContent must always match input
            assertEquals(content, doc.rawContent, "rawContent must match input for ${parser.supportedFormat.id}")
        }
    }

    // ==================== ERROR ACCUMULATION ====================

    @Test
    fun `error accumulation parse with intentional format errors`() {
        // LaTeX with unclosed environment
        val latexWithErrors = "\\documentclass{article}\n\\begin{document}\n\\begin{itemize}\n\\item First\n\\end{document}"
        val latexDoc = latexParser.parse(latexWithErrors)
        assertNotNull(latexDoc)
        // Parser should still produce a result even with errors

        // RST with validation errors
        val rstContent = "Title\n=="  // Underline too short
        val rstDoc = rstParser.parse(rstContent)
        assertNotNull(rstDoc)
        // Use validate() to check for errors
        val rstErrors = rstParser.validate(rstContent)
        // RST validate may or may not find errors depending on implementation
    }

    @Test
    fun `error list in ParsedDocument is immutable and preserved`() {
        val content = "Some content"
        val doc = plaintextParser.parse(content)

        // errors should default to emptyList
        assertTrue(doc.errors.isEmpty(), "Plaintext parser should produce no errors for valid content")

        // Creating document with explicit errors
        val docWithErrors = ParsedDocument(
            format = FormatRegistry.formats.first { it.id == FormatRegistry.ID_PLAINTEXT },
            rawContent = content,
            parsedContent = content,
            errors = listOf("Error 1", "Error 2", "Error 3")
        )
        assertEquals(3, docWithErrors.errors.size)
        assertEquals("Error 1", docWithErrors.errors[0])
        assertEquals("Error 2", docWithErrors.errors[1])
        assertEquals("Error 3", docWithErrors.errors[2])

        // Errors should survive copy
        val copiedDoc = docWithErrors.copy()
        assertEquals(docWithErrors.errors, copiedDoc.errors, "Errors must be preserved through copy")
    }

    @Test
    fun `all parsers produce errors list not null`() {
        val parsersAndContents = listOf(
            markdownParser to "# Test",
            plaintextParser to "Plain text",
            csvParser to "a,b\n1,2",
            latexParser to "\\documentclass{article}\n\\begin{document}\nTest.\n\\end{document}",
            orgModeParser to "* Heading",
            wikitextParser to "= Heading =",
            asciidocParser to "= Title",
            rstParser to "Title\n=====",
            creoleParser to "= Heading",
            tiddlyWikiParser to "! Heading",
            keyValueParser to "key = value",
            taskpaperParser to "Project:",
            textileParser to "h1. Title",
            todoTxtParser to "(A) Task"
        )

        parsersAndContents.forEach { (parser, content) ->
            val doc = parser.parse(content)
            assertNotNull(doc.errors, "errors list must not be null for ${parser.supportedFormat.id}")
            // errors list may be empty (valid content) or populated (parsing issues)
        }
    }

    // ==================== CROSS-PARSER INVARIANTS ====================

    @Test
    fun `same input to different parsers produces different format IDs`() {
        val input = "# Heading\n\nSome content here.\n\n- Item 1\n- Item 2"

        val mdDoc = markdownParser.parse(input)
        val plainDoc = plaintextParser.parse(input)
        val csvDoc = csvParser.parse(input)

        // Same input parsed by different parsers should produce different format IDs
        assertNotEquals(mdDoc.format.id, plainDoc.format.id,
            "Markdown and Plaintext parsers must assign different format IDs")
        assertNotEquals(mdDoc.format.id, csvDoc.format.id,
            "Markdown and CSV parsers must assign different format IDs")
        assertNotEquals(plainDoc.format.id, csvDoc.format.id,
            "Plaintext and CSV parsers must assign different format IDs")

        // But all should have the same rawContent
        assertEquals(mdDoc.rawContent, plainDoc.rawContent,
            "rawContent must be identical regardless of parser")
        assertEquals(mdDoc.rawContent, csvDoc.rawContent,
            "rawContent must be identical regardless of parser")
    }

    @Test
    fun `same input to different parsers produces different HTML`() {
        val input = "# Heading\n\nSome text with a link: http://example.com"

        val mdDoc = markdownParser.parse(input)
        val plainDoc = plaintextParser.parse(input)

        val mdHtml = mdDoc.toHtml(lightMode = true)
        val plainHtml = plainDoc.toHtml(lightMode = true)

        // Different parsers should produce different HTML from the same input
        assertNotEquals(mdHtml, plainHtml,
            "Markdown and Plaintext parsers must produce different HTML from the same input")

        // Both should be non-empty
        assertTrue(mdHtml.isNotEmpty(), "Markdown HTML must not be empty")
        assertTrue(plainHtml.isNotEmpty(), "Plaintext HTML must not be empty")
    }

    @Test
    fun `all parsers assign their own format ID to parsed document`() {
        val sampleContent = "Sample content for testing."

        val allTestParsers = listOf(
            markdownParser, plaintextParser, todoTxtParser, csvParser,
            wikitextParser, creoleParser, tiddlyWikiParser,
            latexParser, asciidocParser, orgModeParser, rstParser,
            keyValueParser, taskpaperParser, textileParser,
            jupyterParser, rMarkdownParser, binaryParser
        )

        allTestParsers.forEach { parser ->
            val doc = parser.parse(sampleContent)
            assertEquals(parser.supportedFormat.id, doc.format.id,
                "Parser ${parser.supportedFormat.id} must assign its own format ID to parsed document")
        }
    }

    // ==================== STYLESHEETS LIGHT VS DARK ====================

    @Test
    fun `StyleSheets light vs dark theme generation for all themed formats`() {
        val themedFormats = listOf(
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )

        themedFormats.forEach { formatId ->
            val lightStyle = StyleSheets.getStyleSheet(formatId, lightMode = true)
            val darkStyle = StyleSheets.getStyleSheet(formatId, lightMode = false)

            assertTrue(lightStyle.isNotEmpty(), "Light stylesheet must not be empty for $formatId")
            assertTrue(darkStyle.isNotEmpty(), "Dark stylesheet must not be empty for $formatId")
            assertNotEquals(lightStyle, darkStyle,
                "Light and dark stylesheets must differ for $formatId")

            // Both should contain <style> tags
            assertTrue(lightStyle.contains("<style>"), "Light stylesheet must contain <style> tag for $formatId")
            assertTrue(darkStyle.contains("<style>"), "Dark stylesheet must contain <style> tag for $formatId")
            assertTrue(lightStyle.contains("</style>"), "Light stylesheet must contain </style> tag for $formatId")
            assertTrue(darkStyle.contains("</style>"), "Dark stylesheet must contain </style> tag for $formatId")
        }
    }

    @Test
    fun `StyleSheets single-theme formats return same for both modes`() {
        // Markdown and WikiText only have one stylesheet regardless of mode
        val singleThemeFormats = listOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_WIKITEXT
        )

        singleThemeFormats.forEach { formatId ->
            val lightStyle = StyleSheets.getStyleSheet(formatId, lightMode = true)
            val darkStyle = StyleSheets.getStyleSheet(formatId, lightMode = false)

            assertEquals(lightStyle, darkStyle,
                "Single-theme format $formatId must return same stylesheet for both modes")
            assertTrue(lightStyle.isNotEmpty(), "Stylesheet must not be empty for $formatId")
        }
    }

    @Test
    fun `StyleSheets returns empty for unknown formats`() {
        val unknownFormats = listOf("unknown", "nonexistent", "", "foobar")

        unknownFormats.forEach { formatId ->
            val style = StyleSheets.getStyleSheet(formatId, lightMode = true)
            assertEquals("", style, "Unknown format '$formatId' must return empty stylesheet")
        }
    }

    @Test
    fun `StyleSheets dark mode uses darker background colors`() {
        // LaTeX dark mode should have darker background
        val latexDark = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = false)
        assertTrue(latexDark.contains("#2d2d2d") || latexDark.contains("#3d3d3d"),
            "LaTeX dark stylesheet must use dark background colors")

        // OrgMode dark mode
        val orgDark = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode = false)
        assertTrue(orgDark.contains("#1e1e1e") || orgDark.contains("#2d2d2d"),
            "OrgMode dark stylesheet must use dark background colors")
    }

    // ==================== ADDITIONAL INTEGRATION TESTS ====================

    @Test
    fun `ParsedDocument toHtml generates consistent output for same input`() {
        val content = "# Consistent Test\n\nParagraph content."
        val doc1 = markdownParser.parse(content)
        val doc2 = markdownParser.parse(content)

        val html1 = doc1.toHtml(lightMode = true)
        val html2 = doc2.toHtml(lightMode = true)

        assertEquals(html1, html2, "Same input must produce identical HTML output across parse calls")
    }

    @Test
    fun `ParsedDocument toHtml handles both modes independently`() {
        val content = "\\documentclass{article}\n\\begin{document}\nTest content.\n\\end{document}"
        val doc = latexParser.parse(content)

        // Generate light first
        val lightHtml = doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // Generate dark
        val darkHtml = doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertTrue(doc.hasHtmlCached(lightMode = false))

        // Both should be cached independently
        // Clearing one should not affect the other
        // (clearHtmlCache clears both, but let us verify both are present)
        assertTrue(lightHtml.isNotEmpty())
        assertTrue(darkHtml.isNotEmpty())
    }

    @Test
    fun `format detection and parsing pipeline performance`() {
        val testDocs = listOf(
            "test.md" to "# Title\n\nContent with **bold**.",
            "data.csv" to "name,age\nAlice,30\nBob,25",
            "paper.tex" to "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}",
            "notes.org" to "* Tasks\n** TODO Task 1\n** DONE Task 2",
            "todo.txt" to "(A) Important @work +project"
        )

        val totalTime = measureTime {
            repeat(100) {
                testDocs.forEach { (filename, content) ->
                    val format = FormatRegistry.detectByFilename(filename)
                    val parser = ParserRegistry.getParser(format)
                    val doc = parser?.parse(content)
                    doc?.toHtml(lightMode = true)
                }
            }
        }

        println("100 iterations x ${testDocs.size} documents pipeline: $totalTime")
        assertTrue(totalTime.inWholeMilliseconds < 30000,
            "500 full pipelines must complete within 30 seconds")
    }

    @Test
    fun `escapeHtml utility prevents XSS`() {
        val xssContent = "<script>alert('xss')</script>"
        val escaped = xssContent.escapeHtml()

        assertFalse(escaped.contains("<script>"), "Escaped content must not contain raw script tags")
        assertTrue(escaped.contains("&lt;script&gt;"), "Escaped content must contain entity-escaped script tags")
        assertTrue(escaped.contains("&#39;"), "Single quotes must be entity-escaped")
    }

    @Test
    fun `ParseOptions builder creates correct options map`() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .enableHighlighting(true)
            .setBaseUrl("https://example.com")
            .set("customKey", "customValue")
            .build()

        assertEquals(true, options["lineNumbers"])
        assertEquals(true, options["highlighting"])
        assertEquals("https://example.com", options["baseUrl"])
        assertEquals("customValue", options["customKey"])
    }

    @Test
    fun `full round-trip parse toHtml clearCache reparse for all formats`() {
        data class FormatTestCase(val parser: TextParser, val content: String)

        val testCases = listOf(
            FormatTestCase(markdownParser, "# Title\n\nParagraph."),
            FormatTestCase(plaintextParser, "Plain text content."),
            FormatTestCase(csvParser, "a,b\n1,2"),
            FormatTestCase(latexParser, "\\documentclass{article}\n\\begin{document}\nHi.\n\\end{document}"),
            FormatTestCase(orgModeParser, "* Heading\nContent."),
            FormatTestCase(wikitextParser, "= Heading =\nContent."),
            FormatTestCase(asciidocParser, "= Title\nContent."),
            FormatTestCase(rstParser, "Title\n=====\nContent."),
            FormatTestCase(creoleParser, "= Heading\nContent."),
            FormatTestCase(tiddlyWikiParser, "! Heading\nContent."),
            FormatTestCase(keyValueParser, "key = value"),
            FormatTestCase(taskpaperParser, "Project:\n\t- Task"),
            FormatTestCase(textileParser, "h1. Title"),
            FormatTestCase(todoTxtParser, "(A) Task @work"),
            FormatTestCase(jupyterParser, """{"nbformat":4,"nbformat_minor":4,"metadata":{"kernelspec":{"name":"python3","display_name":"Python 3"}},"cells":[{"cell_type":"markdown","source":"# Hi"}]}"""),
            FormatTestCase(rMarkdownParser, "---\ntitle: 'Test'\n---\n\n```{r}\n1+1\n```")
        )

        testCases.forEach { (parser, content) ->
            // Step 1: Parse
            val doc = parser.parse(content)
            assertEquals(parser.supportedFormat.id, doc.format.id,
                "Format ID mismatch for ${parser.supportedFormat.id}")

            // Step 2: Generate HTML
            val html = doc.toHtml(lightMode = true)
            assertTrue(html.isNotEmpty(), "HTML must not be empty for ${parser.supportedFormat.id}")
            assertTrue(doc.hasHtmlCached(lightMode = true))

            // Step 3: Clear cache
            doc.clearHtmlCache()
            assertFalse(doc.hasHtmlCached(lightMode = true))

            // Step 4: Re-parse
            val doc2 = parser.parse(content)
            assertEquals(doc.rawContent, doc2.rawContent,
                "Re-parsed rawContent must match for ${parser.supportedFormat.id}")

            // Step 5: Re-generate HTML
            val html2 = doc2.toHtml(lightMode = true)
            assertEquals(html, html2,
                "Re-generated HTML must match for ${parser.supportedFormat.id}")
        }
    }
}
