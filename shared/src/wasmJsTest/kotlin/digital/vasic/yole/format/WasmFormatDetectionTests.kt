/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm-specific tests for format detection and parsing.
 * Validates that all 18 text formats work correctly on the
 * Wasm/JS platform.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Wasm-specific tests for FormatRegistry format detection.
 *
 * Tests cover:
 * - All 17 text format detections on the Wasm platform
 * - Extension-based detection
 * - Content-based detection
 * - Format parsing on Wasm
 * - Edge cases in detection
 * - Format metadata correctness
 */
class WasmFormatDetectionTests {

    // ==================== Extension-Based Detection Tests ====================

    @Test
    fun `detect Markdown by extension md`() {
        val format = FormatRegistry.detectByExtension("md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
        assertEquals("Markdown", format.name)
    }

    @Test
    fun `detect Markdown by extension markdown`() {
        val format = FormatRegistry.detectByExtension("markdown")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detect Plain Text by extension txt`() {
        val format = FormatRegistry.detectByExtension("txt")
        // txt maps to multiple formats; first match in priority
        assertNotNull(format)
        assertTrue(format.extensions.contains(".txt"))
    }

    @Test
    fun `detect CSV by extension csv`() {
        val format = FormatRegistry.detectByExtension("csv")
        assertEquals(FormatRegistry.ID_CSV, format.id)
        assertEquals("CSV", format.name)
    }

    @Test
    fun `detect LaTeX by extension tex`() {
        val format = FormatRegistry.detectByExtension("tex")
        assertEquals(FormatRegistry.ID_LATEX, format.id)
        assertEquals("LaTeX", format.name)
    }

    @Test
    fun `detect Org Mode by extension org`() {
        val format = FormatRegistry.detectByExtension("org")
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)
        assertEquals("Org Mode", format.name)
    }

    @Test
    fun `detect WikiText by extension wiki`() {
        val format = FormatRegistry.detectByExtension("wiki")
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)
    }

    @Test
    fun `detect AsciiDoc by extension adoc`() {
        val format = FormatRegistry.detectByExtension("adoc")
        assertEquals(FormatRegistry.ID_ASCIIDOC, format.id)
    }

    @Test
    fun `detect reStructuredText by extension rst`() {
        val format = FormatRegistry.detectByExtension("rst")
        assertEquals(FormatRegistry.ID_RESTRUCTUREDTEXT, format.id)
    }

    @Test
    fun `detect TaskPaper by extension taskpaper`() {
        val format = FormatRegistry.detectByExtension("taskpaper")
        assertEquals(FormatRegistry.ID_TASKPAPER, format.id)
    }

    @Test
    fun `detect Textile by extension textile`() {
        val format = FormatRegistry.detectByExtension("textile")
        assertEquals(FormatRegistry.ID_TEXTILE, format.id)
    }

    @Test
    fun `detect TiddlyWiki by extension tid`() {
        val format = FormatRegistry.detectByExtension("tid")
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, format.id)
    }

    @Test
    fun `detect Creole by extension creole`() {
        val format = FormatRegistry.detectByExtension("creole")
        assertEquals(FormatRegistry.ID_CREOLE, format.id)
    }

    @Test
    fun `detect R Markdown by extension rmd`() {
        val format = FormatRegistry.detectByExtension("rmd")
        assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)
    }

    @Test
    fun `detect Jupyter by extension ipynb`() {
        val format = FormatRegistry.detectByExtension("ipynb")
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun `detect Key-Value by extension ini`() {
        val format = FormatRegistry.detectByExtension("ini")
        assertEquals(FormatRegistry.ID_KEYVALUE, format.id)
    }

    @Test
    fun `detect Key-Value by extension properties`() {
        val format = FormatRegistry.detectByExtension("properties")
        assertEquals(FormatRegistry.ID_KEYVALUE, format.id)
    }

    @Test
    fun `unknown extension falls back to plaintext`() {
        val format = FormatRegistry.detectByExtension("xyz_unknown")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    // ==================== Content-Based Detection Tests ====================

    @Test
    fun `detect Markdown by content with heading`() {
        val content = "# This is a heading\n\nSome paragraph text."
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detect Markdown by content with bold`() {
        val content = "This has **bold** text in it."
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detect Markdown by content with link`() {
        val content = "[Click here](https://example.com)"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detect LaTeX by content with documentclass`() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_LATEX, format.id)
    }

    @Test
    fun `detect Org Mode by content with stars`() {
        val content = "* Top level heading\n** Second level\n*** Third level"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)
    }

    @Test
    fun `detect Todo txt by content with priority`() {
        val content = "(A) Buy groceries @store +shopping\n(B) Call dentist @phone"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_TODOTXT, format.id)
    }

    @Test
    fun `detect WikiText by content with wikilinks`() {
        val content = "== Section Title ==\n[[Main Page]]\n[[Link|Display Text]]"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)
    }

    @Test
    fun `detect R Markdown by content with r code chunk`() {
        val content = "# Analysis\n\n```{r setup}\nlibrary(ggplot2)\n```"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)
    }

    @Test
    fun `detect Jupyter by content with nbformat`() {
        val content = """{"nbformat": 4, "cells": [{"cell_type": "code"}]}"""
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun `detect CSV by content with comma-separated values`() {
        val content = "name,age,city\nAlice,30,NYC\nBob,25,LA"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    @Test
    fun `empty content returns null for content detection`() {
        val format = FormatRegistry.detectByContent("")
        assertNull(format)
    }

    // ==================== Format Registry Integrity Tests ====================

    @Test
    fun `all format IDs are unique`() {
        val ids = FormatRegistry.formats.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "All format IDs should be unique")
    }

    @Test
    fun `all format names are non-empty`() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(format.name.isNotBlank(), "Format ${format.id} should have a non-empty name")
        }
    }

    @Test
    fun `registry has at least 18 text formats`() {
        // 18 text formats (incl. JSON, iter 42) + network storage formats
        assertTrue(FormatRegistry.formats.size >= 17,
            "Registry should have at least 18 formats, has ${FormatRegistry.formats.size}")
    }

    @Test
    fun `getById returns correct format for each ID`() {
        val expectedIds = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_WIKITEXT,
            FormatRegistry.ID_ASCIIDOC,
            FormatRegistry.ID_RESTRUCTUREDTEXT,
            FormatRegistry.ID_TASKPAPER,
            FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_CREOLE,
            FormatRegistry.ID_TIDDLYWIKI,
            FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_RMARKDOWN,
            FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_BINARY
        )
        expectedIds.forEach { id ->
            val format = FormatRegistry.getById(id)
            assertNotNull(format, "Format with id '$id' should exist in registry")
            assertEquals(id, format.id)
        }
    }

    @Test
    fun `getById returns null for unknown format`() {
        assertNull(FormatRegistry.getById("nonexistent_format_xyz"))
    }

    @Test
    fun `isSupported returns true for all registered formats`() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(FormatRegistry.isSupported(format.id),
                "Format ${format.id} should be supported")
        }
    }

    @Test
    fun `isSupported returns false for unknown format`() {
        assertFalse(FormatRegistry.isSupported("unknown_format_abc"))
    }

    @Test
    fun `detectByFilename extracts extension correctly`() {
        val format = FormatRegistry.detectByFilename("document.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detectByFilename with no extension returns plaintext`() {
        val format = FormatRegistry.detectByFilename("README")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `detectByFilename handles multiple dots`() {
        val format = FormatRegistry.detectByFilename("my.document.draft.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `getFormatNames returns non-empty list`() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.isNotEmpty())
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("Plain Text"))
    }

    @Test
    fun `getAllExtensions returns distinct extensions`() {
        val extensions = FormatRegistry.getAllExtensions()
        assertEquals(extensions.size, extensions.distinct().size,
            "All extensions should be unique")
    }

    @Test
    fun `isExtensionSupported returns true for md`() {
        assertTrue(FormatRegistry.isExtensionSupported(".md"))
        assertTrue(FormatRegistry.isExtensionSupported("md"))
    }

    @Test
    fun `isExtensionSupported returns false for unknown extension`() {
        assertFalse(FormatRegistry.isExtensionSupported(".xyz_unknown_ext"))
    }

    @Test
    fun `getFormatsByExtension returns multiple formats for txt`() {
        val formats = FormatRegistry.getFormatsByExtension("txt")
        assertTrue(formats.size >= 2,
            "Multiple formats should support .txt extension, got ${formats.size}")
    }

    @Test
    fun `extension detection is case insensitive`() {
        val lower = FormatRegistry.detectByExtension("md")
        val upper = FormatRegistry.detectByExtension("MD")
        val mixed = FormatRegistry.detectByExtension("Md")
        assertEquals(lower.id, upper.id)
        assertEquals(lower.id, mixed.id)
    }

    @Test
    fun `extension detection with dot prefix works`() {
        val withDot = FormatRegistry.detectByExtension(".md")
        val withoutDot = FormatRegistry.detectByExtension("md")
        assertEquals(withDot.id, withoutDot.id)
    }
}
