/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge case tests for FormatRegistry detection logic,
 * format lookup, and content analysis
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatRegistryEdgeCaseTest {

    // ==================== getById edge cases ====================

    @Test
    fun testGetByIdReturnsNullForEmptyString() {
        assertNull(FormatRegistry.getById(""))
    }

    @Test
    fun testGetByIdReturnsNullForWhitespace() {
        assertNull(FormatRegistry.getById("  "))
    }

    @Test
    fun testGetByIdIsCaseSensitive() {
        assertNull(FormatRegistry.getById("Markdown"))
        assertNull(FormatRegistry.getById("MARKDOWN"))
        assertNotNull(FormatRegistry.getById("markdown"))
    }

    @Test
    fun testGetByIdAllRegisteredIds() {
        val expectedIds = listOf(
            FormatRegistry.ID_RMARKDOWN, FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_PLAINTEXT, FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_WIKITEXT,
            FormatRegistry.ID_ORGMODE, FormatRegistry.ID_CREOLE,
            FormatRegistry.ID_TIDDLYWIKI, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ASCIIDOC, FormatRegistry.ID_RESTRUCTUREDTEXT,
            FormatRegistry.ID_KEYVALUE, FormatRegistry.ID_TASKPAPER,
            FormatRegistry.ID_TEXTILE, FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_BINARY, FormatRegistry.ID_DROPBOX,
            FormatRegistry.ID_FTP, FormatRegistry.ID_GOOGLEDRIVE,
            FormatRegistry.ID_ONEDRIVE, FormatRegistry.ID_SFTP
        )
        expectedIds.forEach { id ->
            assertNotNull(FormatRegistry.getById(id), "getById should find format '$id'")
        }
    }

    @Test
    fun testGetByIdNetworkFormats() {
        assertNotNull(FormatRegistry.getById(FormatRegistry.ID_DROPBOX))
        assertNotNull(FormatRegistry.getById(FormatRegistry.ID_FTP))
        assertNotNull(FormatRegistry.getById(FormatRegistry.ID_GOOGLEDRIVE))
        assertNotNull(FormatRegistry.getById(FormatRegistry.ID_ONEDRIVE))
        assertNotNull(FormatRegistry.getById(FormatRegistry.ID_SFTP))
    }

    // ==================== getByExtension edge cases ====================

    @Test
    fun testGetByExtensionWithLeadingSpaces() {
        val format = FormatRegistry.getByExtension("  md  ")
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun testGetByExtensionReturnsNullForEmpty() {
        // Empty string after trimming becomes "." which matches nothing
        val format = FormatRegistry.getByExtension("")
        assertNull(format)
    }

    @Test
    fun testGetByExtensionMultipleFormatsShareTxt() {
        // .txt is shared by plaintext, todotxt, creole, textile
        val format = FormatRegistry.getByExtension("txt")
        assertNotNull(format)
        // First match wins due to list ordering — plaintext comes first
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun testGetByExtensionAllMarkdownExtensions() {
        val mdExtensions = listOf("md", "markdown", "mdown", "mkd")
        mdExtensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Should find format for .$ext")
            assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
        }
    }

    @Test
    fun testGetByExtensionRmarkdownExtensions() {
        val rmdExtensions = listOf("rmd", "rmarkdown")
        rmdExtensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Should find format for .$ext")
            assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)
        }
    }

    @Test
    fun testGetByExtensionWithDoubleDot() {
        // ".md" should work same as "md"
        val format = FormatRegistry.getByExtension(".md")
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    // ==================== getFormatsByExtension edge cases ====================

    @Test
    fun testGetFormatsByExtensionTxtReturnsMultiple() {
        val formats = FormatRegistry.getFormatsByExtension("txt")
        assertTrue(formats.size >= 2, "At least plaintext and todotxt share .txt")
        val ids = formats.map { it.id }
        assertTrue(ids.contains(FormatRegistry.ID_PLAINTEXT))
        assertTrue(ids.contains(FormatRegistry.ID_TODOTXT))
    }

    @Test
    fun testGetFormatsByExtensionUnknownReturnsEmpty() {
        val formats = FormatRegistry.getFormatsByExtension("zzz")
        assertTrue(formats.isEmpty())
    }

    @Test
    fun testGetFormatsByExtensionCsvReturnsSingle() {
        val formats = FormatRegistry.getFormatsByExtension("csv")
        assertEquals(1, formats.size)
        assertEquals(FormatRegistry.ID_CSV, formats[0].id)
    }

    // ==================== detectByExtension fallback ====================

    @Test
    fun testDetectByExtensionFallsBackToPlaintext() {
        val format = FormatRegistry.detectByExtension("zzz")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun testDetectByExtensionEmptyFallsBackToPlaintext() {
        val format = FormatRegistry.detectByExtension("")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun testDetectByExtensionSpecialCharsExtension() {
        val format = FormatRegistry.detectByExtension("a b c")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    // ==================== detectByFilename edge cases ====================

    @Test
    fun testDetectByFilenameDotsOnly() {
        val format = FormatRegistry.detectByFilename("...")
        // substringAfterLast('.') returns empty string
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun testDetectByFilenameNoExtension() {
        val format = FormatRegistry.detectByFilename("LICENSE")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun testDetectByFilenameHiddenFileWithExtension() {
        val format = FormatRegistry.detectByFilename(".readme.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun testDetectByFilenameComplex() {
        val format = FormatRegistry.detectByFilename("archive.2024.01.15.csv")
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    // ==================== detectByContent edge cases ====================

    @Test
    fun testDetectByContentRMarkdown() {
        val content = "```{r setup}\nlibrary(ggplot2)\n```"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)
    }

    @Test
    fun testDetectByContentLatexDocumentClass() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_LATEX, format.id)
    }

    @Test
    fun testDetectByContentLatexBeginDocument() {
        val content = "\\begin{document}\nHello\n\\end{document}"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_LATEX, format.id)
    }

    @Test
    fun testDetectByContentOrgModeHeading() {
        val content = "* Top level heading\n** Second level\n*** Third level"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)
    }

    @Test
    fun testDetectByContentOrgModeDirective() {
        val content = "#+TITLE: My Document\n#+AUTHOR: Author"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)
    }

    @Test
    fun testDetectByContentWikiHeading() {
        val content = "== Wiki Heading ==\nSome text"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)
    }

    @Test
    fun testDetectByContentWikiLink() {
        val content = "[[Internal Link]]\nSome text"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)
    }

    @Test
    fun testDetectByContentTiddlyWikiTitle() {
        val content = "title: My Tiddler\ntags: [[tag1]] [[tag2]]"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, format.id)
    }

    @Test
    fun testDetectByContentJupyterNotebook() {
        val content = """{"nbformat": 4, "cells": []}"""
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun testDetectByContentJupyterCellType() {
        val content = """{"cell_type": "code", "source": []}"""
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun testDetectByContentCsv() {
        val content = "name,age,city\nAlice,30,NYC\nBob,25,LA"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    @Test
    fun testDetectByContentKeyValue() {
        // Note: key-value pattern is ^[a-zA-Z_]+\s*= (no digits in key name)
        val content = "database_host = localhost\nserver_port = 8080"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_KEYVALUE, format.id)
    }

    @Test
    fun testDetectByContentKeyValueIni() {
        val content = "[section]\nkey = value"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_KEYVALUE, format.id)
    }

    @Test
    fun testDetectByContentMaxLinesLimit() {
        // Put the pattern at line 15, but limit to 5 lines
        val lines = (1..20).map { "plain text line $it" }.toMutableList()
        lines[14] = "# Markdown Heading"
        val content = lines.joinToString("\n")
        val format = FormatRegistry.detectByContent(content, maxLines = 5)
        // Should not detect markdown since it's beyond the maxLines limit
        assertTrue(format == null || format.id != FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun testDetectByContentSingleLine() {
        val content = "# Hello"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun testDetectByContentMaxLinesOne() {
        val content = "# Heading\nNormal text"
        val format = FormatRegistry.detectByContent(content, maxLines = 1)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    // ==================== isSupported ====================

    @Test
    fun testIsSupportedForAllFormats() {
        val allIds = FormatRegistry.formats.map { it.id }
        allIds.forEach { id ->
            assertTrue(FormatRegistry.isSupported(id), "'$id' should be supported")
        }
    }

    @Test
    fun testIsSupportedFalseForNonexistent() {
        assertFalse(FormatRegistry.isSupported("html"))
        assertFalse(FormatRegistry.isSupported("xml"))
        assertFalse(FormatRegistry.isSupported(""))
    }

    // ==================== isExtensionSupported ====================

    @Test
    fun testIsExtensionSupportedForAllKnownExtensions() {
        val knownExts = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst",
            "adoc", "ini", "taskpaper", "textile", "creole", "ipynb", "rmd",
            "markdown", "mdown", "mkd", "asciidoc", "rest", "tiddly", "tid",
            "properties", "keyvalue", "rmarkdown", "latex", "wikitext")
        knownExts.forEach { ext ->
            assertTrue(FormatRegistry.isExtensionSupported(ext),
                "Extension '$ext' should be supported")
        }
    }

    @Test
    fun testIsExtensionSupportedFalseForBinary() {
        // Binary format has no extensions
        assertFalse(FormatRegistry.isExtensionSupported("bin"))
    }

    // ==================== getAllExtensions ====================

    @Test
    fun testGetAllExtensionsContainsExpectedExtensions() {
        val extensions = FormatRegistry.getAllExtensions()
        assertTrue(extensions.contains(".md"))
        assertTrue(extensions.contains(".txt"))
        assertTrue(extensions.contains(".csv"))
        assertTrue(extensions.contains(".tex"))
        assertTrue(extensions.contains(".org"))
    }

    @Test
    fun testGetAllExtensionsNoDuplicates() {
        val extensions = FormatRegistry.getAllExtensions()
        assertEquals(extensions.size, extensions.toSet().size)
    }

    // ==================== getFormatNames ====================

    @Test
    fun testGetFormatNamesMatchesFormatsSize() {
        val names = FormatRegistry.getFormatNames()
        assertEquals(FormatRegistry.formats.size, names.size)
    }

    @Test
    fun testGetFormatNamesContainsAllNames() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("Plain Text"))
        assertTrue(names.contains("Todo.txt"))
        assertTrue(names.contains("CSV"))
        assertTrue(names.contains("LaTeX"))
        assertTrue(names.contains("Binary"))
        assertTrue(names.contains("Dropbox"))
        assertTrue(names.contains("FTP"))
    }

    // ==================== Format structure validation ====================

    @Test
    fun testFormatsListIsNotEmpty() {
        assertTrue(FormatRegistry.formats.isNotEmpty())
    }

    @Test
    fun testTotalFormatCount() {
        // 18 text formats + 5 network formats = 23
        assertEquals(23, FormatRegistry.formats.size)
    }

    @Test
    fun testNetworkFormatsHaveEmptyExtensions() {
        val networkIds = listOf(
            FormatRegistry.ID_DROPBOX, FormatRegistry.ID_FTP,
            FormatRegistry.ID_GOOGLEDRIVE, FormatRegistry.ID_ONEDRIVE,
            FormatRegistry.ID_SFTP
        )
        networkIds.forEach { id ->
            val format = FormatRegistry.getById(id)
            assertNotNull(format)
            assertTrue(format.extensions.isEmpty(), "Network format '$id' should have no extensions")
        }
    }

    @Test
    fun testBinaryFormatHasNoExtensions() {
        val binary = FormatRegistry.getById(FormatRegistry.ID_BINARY)
        assertNotNull(binary)
        assertTrue(binary.extensions.isEmpty())
    }

    @Test
    fun testBinaryFormatHasNoDetectionPatterns() {
        val binary = FormatRegistry.getById(FormatRegistry.ID_BINARY)
        assertNotNull(binary)
        assertTrue(binary.detectionPatterns.isEmpty())
    }

    @Test
    fun testRMarkdownComesBeforeMarkdown() {
        val rmdIndex = FormatRegistry.formats.indexOfFirst { it.id == FormatRegistry.ID_RMARKDOWN }
        val mdIndex = FormatRegistry.formats.indexOfFirst { it.id == FormatRegistry.ID_MARKDOWN }
        assertTrue(rmdIndex < mdIndex, "R Markdown should come before Markdown for correct detection priority")
    }

    @Test
    fun testFormatIdConstantsMatchRegistryIds() {
        assertEquals(FormatRegistry.ID_UNKNOWN, "unknown")
        assertEquals(FormatRegistry.ID_PLAINTEXT, "plaintext")
        assertEquals(FormatRegistry.ID_MARKDOWN, "markdown")
        assertEquals(FormatRegistry.ID_TODOTXT, "todotxt")
        assertEquals(FormatRegistry.ID_CSV, "csv")
        assertEquals(FormatRegistry.ID_WIKITEXT, "wikitext")
        assertEquals(FormatRegistry.ID_KEYVALUE, "keyvalue")
        assertEquals(FormatRegistry.ID_ASCIIDOC, "asciidoc")
        assertEquals(FormatRegistry.ID_ORGMODE, "orgmode")
        assertEquals(FormatRegistry.ID_LATEX, "latex")
        assertEquals(FormatRegistry.ID_RESTRUCTUREDTEXT, "restructuredtext")
        assertEquals(FormatRegistry.ID_TASKPAPER, "taskpaper")
        assertEquals(FormatRegistry.ID_TEXTILE, "textile")
        assertEquals(FormatRegistry.ID_CREOLE, "creole")
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, "tiddlywiki")
        assertEquals(FormatRegistry.ID_JUPYTER, "jupyter")
        assertEquals(FormatRegistry.ID_RMARKDOWN, "rmarkdown")
        assertEquals(FormatRegistry.ID_BINARY, "binary")
        assertEquals(FormatRegistry.ID_DROPBOX, "dropbox")
        assertEquals(FormatRegistry.ID_FTP, "ftp")
        assertEquals(FormatRegistry.ID_GOOGLEDRIVE, "googledrive")
        assertEquals(FormatRegistry.ID_ONEDRIVE, "onedrive")
        assertEquals(FormatRegistry.ID_SFTP, "sftp")
    }
}
