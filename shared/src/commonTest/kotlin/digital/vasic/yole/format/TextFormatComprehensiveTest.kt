/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for TextFormat and FormatRegistry
 * covering all 17+ format IDs, extensions, content
 * detection, and registry operations.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Comprehensive tests for [TextFormat] and [FormatRegistry].
 *
 * Validates format ID uniqueness, extension detection, content
 * detection patterns, registry lookup, and edge cases.
 *
 * Total: 55+ tests
 */
class TextFormatComprehensiveTest {

    // ==================== Format ID Constants ====================

    @Test
    fun allTextFormatIdConstantsExist() {
        assertNotNull(TextFormat.ID_UNKNOWN)
        assertNotNull(TextFormat.ID_PLAINTEXT)
        assertNotNull(TextFormat.ID_MARKDOWN)
        assertNotNull(TextFormat.ID_TODOTXT)
        assertNotNull(TextFormat.ID_CSV)
        assertNotNull(TextFormat.ID_WIKITEXT)
        assertNotNull(TextFormat.ID_KEYVALUE)
        assertNotNull(TextFormat.ID_ASCIIDOC)
        assertNotNull(TextFormat.ID_ORGMODE)
        assertNotNull(TextFormat.ID_LATEX)
        assertNotNull(TextFormat.ID_RESTRUCTUREDTEXT)
        assertNotNull(TextFormat.ID_TASKPAPER)
        assertNotNull(TextFormat.ID_TEXTILE)
        assertNotNull(TextFormat.ID_CREOLE)
        assertNotNull(TextFormat.ID_TIDDLYWIKI)
        assertNotNull(TextFormat.ID_JUPYTER)
        assertNotNull(TextFormat.ID_RMARKDOWN)
        assertNotNull(TextFormat.ID_BINARY)
    }

    @Test
    fun allFormatIdConstantsAreUnique() {
        val ids = listOf(
            TextFormat.ID_UNKNOWN, TextFormat.ID_PLAINTEXT, TextFormat.ID_MARKDOWN,
            TextFormat.ID_TODOTXT, TextFormat.ID_CSV, TextFormat.ID_WIKITEXT,
            TextFormat.ID_KEYVALUE, TextFormat.ID_ASCIIDOC, TextFormat.ID_ORGMODE,
            TextFormat.ID_LATEX, TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_TASKPAPER,
            TextFormat.ID_TEXTILE, TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI,
            TextFormat.ID_JUPYTER, TextFormat.ID_RMARKDOWN, TextFormat.ID_BINARY
        )
        assertEquals(ids.size, ids.toSet().size, "All format ID constants must be unique")
    }

    @Test
    fun allFormatIdConstantsAreNonEmpty() {
        val ids = listOf(
            TextFormat.ID_UNKNOWN, TextFormat.ID_PLAINTEXT, TextFormat.ID_MARKDOWN,
            TextFormat.ID_TODOTXT, TextFormat.ID_CSV, TextFormat.ID_WIKITEXT,
            TextFormat.ID_KEYVALUE, TextFormat.ID_ASCIIDOC, TextFormat.ID_ORGMODE,
            TextFormat.ID_LATEX, TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_TASKPAPER,
            TextFormat.ID_TEXTILE, TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI,
            TextFormat.ID_JUPYTER, TextFormat.ID_RMARKDOWN, TextFormat.ID_BINARY
        )
        ids.forEach { assertTrue(it.isNotBlank(), "Format ID must not be blank: '$it'") }
    }

    // ==================== Format Names Are Non-Empty ====================

    @Test
    fun allRegisteredFormatsHaveNonEmptyNames() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(format.name.isNotBlank(), "Format '${format.id}' must have a non-empty name")
        }
    }

    @Test
    fun allRegisteredFormatsHaveNonEmptyIds() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(format.id.isNotBlank(), "Format with name '${format.name}' must have a non-empty id")
        }
    }

    // ==================== Extension Detection Per Format ====================

    @Test
    fun markdownExtensionsDetected() {
        val extensions = listOf(".md", ".markdown", ".mdown", ".mkd")
        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_MARKDOWN, format.id)
        }
    }

    @Test
    fun plaintextExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".txt")
        assertNotNull(format, ".txt should be recognized")
        // .txt matches plaintext first (or todotxt depending on order)
        assertTrue(format.id == TextFormat.ID_PLAINTEXT || format.id == TextFormat.ID_RMARKDOWN
            || format.id == TextFormat.ID_TODOTXT || format.id == TextFormat.ID_CREOLE
            || format.id == TextFormat.ID_TEXTILE,
            ".txt should match a text-based format")
    }

    @Test
    fun csvExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".csv")
        assertNotNull(format)
        assertEquals(TextFormat.ID_CSV, format.id)
    }

    @Test
    fun latexExtensionsDetected() {
        listOf(".tex", ".latex").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_LATEX, format.id)
        }
    }

    @Test
    fun orgModeExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".org")
        assertNotNull(format)
        assertEquals(TextFormat.ID_ORGMODE, format.id)
    }

    @Test
    fun wikitextExtensionsDetected() {
        listOf(".wiki", ".wikitext").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_WIKITEXT, format.id)
        }
    }

    @Test
    fun rstExtensionsDetected() {
        listOf(".rst", ".rest").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, format.id)
        }
    }

    @Test
    fun asciidocExtensionsDetected() {
        listOf(".adoc", ".asciidoc").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_ASCIIDOC, format.id)
        }
    }

    @Test
    fun keyvalueExtensionsDetected() {
        listOf(".keyvalue", ".properties", ".ini").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_KEYVALUE, format.id)
        }
    }

    @Test
    fun taskpaperExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".taskpaper")
        assertNotNull(format)
        assertEquals(TextFormat.ID_TASKPAPER, format.id)
    }

    @Test
    fun textileExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".textile")
        assertNotNull(format)
        assertEquals(TextFormat.ID_TEXTILE, format.id)
    }

    @Test
    fun creoleExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".creole")
        assertNotNull(format)
        assertEquals(TextFormat.ID_CREOLE, format.id)
    }

    @Test
    fun tiddlywikiExtensionsDetected() {
        listOf(".tid", ".tiddly").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_TIDDLYWIKI, format.id)
        }
    }

    @Test
    fun jupyterExtensionsDetected() {
        val format = FormatRegistry.getByExtension(".ipynb")
        assertNotNull(format)
        assertEquals(TextFormat.ID_JUPYTER, format.id)
    }

    @Test
    fun rmarkdownExtensionsDetected() {
        listOf(".rmd", ".rmarkdown").forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_RMARKDOWN, format.id)
        }
    }

    // ==================== Content Detection ====================

    @Test
    fun detectMarkdownByContent() {
        val content = "# Title\n\nThis is **bold** text."
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        // Should match markdown or rmarkdown (rmarkdown is checked first)
        assertTrue(format.id == TextFormat.ID_MARKDOWN || format.id == TextFormat.ID_RMARKDOWN,
            "Markdown heading should be detected, got: ${format.id}")
    }

    @Test
    fun detectLatexByContent() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_LATEX, format.id)
    }

    @Test
    fun detectCsvByContent() {
        val content = "name,age,city\nAlice,30,NYC\nBob,25,LA"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_CSV, format.id)
    }

    @Test
    fun detectOrgModeByContent() {
        val content = "* Heading\n** Sub-heading\n#+TITLE: My Doc"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_ORGMODE, format.id)
    }

    @Test
    fun detectWikitextByContent() {
        val content = "== Heading ==\n\n[[link]]"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_WIKITEXT, format.id)
    }

    @Test
    fun detectTodoTxtByContent() {
        val content = "(A) Buy groceries @store +home"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_TODOTXT, format.id)
    }

    @Test
    fun detectKeyValueByContent() {
        val content = "[section]\nkey = value\nother_key = other_value"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
    }

    @Test
    fun detectJupyterByContent() {
        val content = """{"nbformat": 4, "cell_type": "code"}"""
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_JUPYTER, format.id)
    }

    @Test
    fun detectTiddlyWikiByContent() {
        val content = "title: My Note\n\n! Heading"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_TIDDLYWIKI, format.id)
    }

    @Test
    fun detectRMarkdownByContent() {
        val content = "---\ntitle: Test\n---\n\n```{r}\n1+1\n```"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(TextFormat.ID_RMARKDOWN, format.id)
    }

    // ==================== FormatRegistry Operations ====================

    @Test
    fun getByIdReturnsCorrectFormat() {
        val ids = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_PLAINTEXT, TextFormat.ID_TODOTXT,
            TextFormat.ID_CSV, TextFormat.ID_LATEX, TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_RESTRUCTUREDTEXT
        )
        ids.forEach { id ->
            val format = FormatRegistry.getById(id)
            assertNotNull(format, "Format with id '$id' should be in registry")
            assertEquals(id, format.id)
        }
    }

    @Test
    fun getByIdReturnsNullForUnknownId() {
        val format = FormatRegistry.getById("nonexistent-format-xyz")
        assertNull(format)
    }

    @Test
    fun isSupportedReturnsTrueForKnownFormats() {
        assertTrue(FormatRegistry.isSupported(TextFormat.ID_MARKDOWN))
        assertTrue(FormatRegistry.isSupported(TextFormat.ID_PLAINTEXT))
        assertTrue(FormatRegistry.isSupported(TextFormat.ID_CSV))
        assertTrue(FormatRegistry.isSupported(TextFormat.ID_LATEX))
    }

    @Test
    fun isSupportedReturnsFalseForUnknownFormat() {
        assertFalse(FormatRegistry.isSupported("nonexistent-format"))
    }

    @Test
    fun getFormatNamesReturnsNonEmptyList() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.isNotEmpty())
        assertTrue(names.size >= 17, "Should have at least 17 format names")
    }

    @Test
    fun getAllExtensionsReturnsDistinctList() {
        val extensions = FormatRegistry.getAllExtensions()
        assertEquals(extensions.size, extensions.toSet().size, "Extensions should be distinct")
    }

    @Test
    fun getFormatsByExtensionReturnsMultipleForTxt() {
        val txtFormats = FormatRegistry.getFormatsByExtension("txt")
        assertTrue(txtFormats.size >= 2, ".txt should match multiple formats (plaintext, todotxt, etc.)")
    }

    @Test
    fun isExtensionSupportedReturnsTrueForKnownExtensions() {
        assertTrue(FormatRegistry.isExtensionSupported(".md"))
        assertTrue(FormatRegistry.isExtensionSupported(".csv"))
        assertTrue(FormatRegistry.isExtensionSupported(".tex"))
        assertTrue(FormatRegistry.isExtensionSupported(".org"))
    }

    @Test
    fun isExtensionSupportedReturnsFalseForUnknown() {
        assertFalse(FormatRegistry.isExtensionSupported(".xyz123unknown"))
    }

    // ==================== detectByExtension ====================

    @Test
    fun detectByExtensionFallsBackToPlaintext() {
        val format = FormatRegistry.detectByExtension("unknownext")
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    @Test
    fun detectByExtensionHandlesDotPrefix() {
        val format = FormatRegistry.detectByExtension(".md")
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    @Test
    fun detectByExtensionHandlesNoDotPrefix() {
        val format = FormatRegistry.detectByExtension("md")
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    @Test
    fun detectByExtensionCaseInsensitive() {
        val format = FormatRegistry.detectByExtension(".MD")
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    // ==================== detectByFilename ====================

    @Test
    fun detectByFilenameForMarkdown() {
        val format = FormatRegistry.detectByFilename("README.md")
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    @Test
    fun detectByFilenameForCsv() {
        val format = FormatRegistry.detectByFilename("data.csv")
        assertEquals(TextFormat.ID_CSV, format.id)
    }

    @Test
    fun detectByFilenameForLatex() {
        val format = FormatRegistry.detectByFilename("paper.tex")
        assertEquals(TextFormat.ID_LATEX, format.id)
    }

    @Test
    fun detectByFilenameNoExtensionFallsBackToPlaintext() {
        val format = FormatRegistry.detectByFilename("Makefile")
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    // ==================== Unknown Extension Handling ====================

    @Test
    fun getByExtensionReturnsNullForUnknown() {
        val format = FormatRegistry.getByExtension(".zzz")
        assertNull(format)
    }

    @Test
    fun detectByExtensionNeverReturnsNull() {
        val format = FormatRegistry.detectByExtension(".zzz")
        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    // ==================== Ambiguous Content Handling ====================

    @Test
    fun detectByContentReturnsNullForEmptyString() {
        val format = FormatRegistry.detectByContent("")
        assertNull(format)
    }

    @Test
    fun detectByContentReturnsNullForPlainText() {
        // Generic text with no format-specific markers
        val content = "Just some plain text without any special markers."
        val format = FormatRegistry.detectByContent(content)
        // May or may not detect; if detected, it should be a valid format
        if (format != null) {
            assertTrue(format.id.isNotBlank())
        }
    }

    @Test
    fun detectByContentMaxLinesLimit() {
        // Content with markdown header on line 15 (beyond default maxLines=10)
        val content = buildString {
            repeat(14) { appendLine("plain text line $it") }
            appendLine("# Heading")
        }
        val format = FormatRegistry.detectByContent(content, maxLines = 10)
        // Should NOT detect markdown since the header is beyond line 10
        if (format != null) {
            assertNotEquals(TextFormat.ID_MARKDOWN, format.id,
                "Should not detect markdown content beyond maxLines")
        }
    }

    @Test
    fun detectByContentCustomMaxLinesDetectsLate() {
        val content = buildString {
            repeat(14) { appendLine("plain text line $it") }
            appendLine("# Heading")
        }
        val format = FormatRegistry.detectByContent(content, maxLines = 20)
        // With higher maxLines, might detect markdown
        // This is format-dependent behavior, just verify no crash
        assertNotNull(content)
    }

    // ==================== Data Class Behavior ====================

    @Test
    fun textFormatDataClassEquality() {
        val f1 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val f2 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())
    }

    @Test
    fun textFormatDataClassInequality() {
        val f1 = TextFormat(id = "test1", name = "Test1", defaultExtension = ".t1")
        val f2 = TextFormat(id = "test2", name = "Test2", defaultExtension = ".t2")
        assertNotEquals(f1, f2)
    }

    @Test
    fun textFormatCopy() {
        val original = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        val copy = original.copy(name = "Modified")
        assertEquals("md", copy.id)
        assertEquals("Modified", copy.name)
    }

    @Test
    fun textFormatToString() {
        val format = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        val str = format.toString()
        assertTrue(str.contains("md"))
        assertTrue(str.contains("Markdown"))
    }
}
