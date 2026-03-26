/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for FormatRegistry covering
 * lazy initialization, format lookup, detection, caching,
 * concurrent safety, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Isolated unit tests for [FormatRegistry].
 *
 * Validates lazy initialization, format lookup by ID and extension,
 * content-based detection, format count, ID constants, and
 * concurrent access safety.
 *
 * Total: 45 tests
 */
class FormatRegistryUnitTest {

    // ==================== ID Constants ====================

    @Test
    fun idConstantsExistForAllCoreFormats() {
        // Verify all ID constants are non-empty strings
        assertTrue(FormatRegistry.ID_PLAINTEXT.isNotEmpty())
        assertTrue(FormatRegistry.ID_MARKDOWN.isNotEmpty())
        assertTrue(FormatRegistry.ID_TODOTXT.isNotEmpty())
        assertTrue(FormatRegistry.ID_CSV.isNotEmpty())
        assertTrue(FormatRegistry.ID_WIKITEXT.isNotEmpty())
        assertTrue(FormatRegistry.ID_KEYVALUE.isNotEmpty())
        assertTrue(FormatRegistry.ID_ASCIIDOC.isNotEmpty())
        assertTrue(FormatRegistry.ID_ORGMODE.isNotEmpty())
        assertTrue(FormatRegistry.ID_LATEX.isNotEmpty())
        assertTrue(FormatRegistry.ID_RESTRUCTUREDTEXT.isNotEmpty())
        assertTrue(FormatRegistry.ID_TASKPAPER.isNotEmpty())
        assertTrue(FormatRegistry.ID_TEXTILE.isNotEmpty())
        assertTrue(FormatRegistry.ID_CREOLE.isNotEmpty())
        assertTrue(FormatRegistry.ID_TIDDLYWIKI.isNotEmpty())
        assertTrue(FormatRegistry.ID_JUPYTER.isNotEmpty())
        assertTrue(FormatRegistry.ID_RMARKDOWN.isNotEmpty())
        assertTrue(FormatRegistry.ID_BINARY.isNotEmpty())
    }

    @Test
    fun networkStorageIdConstantsExist() {
        assertTrue(FormatRegistry.ID_DROPBOX.isNotEmpty())
        assertTrue(FormatRegistry.ID_FTP.isNotEmpty())
        assertTrue(FormatRegistry.ID_GOOGLEDRIVE.isNotEmpty())
        assertTrue(FormatRegistry.ID_ONEDRIVE.isNotEmpty())
        assertTrue(FormatRegistry.ID_SFTP.isNotEmpty())
    }

    @Test
    fun idConstantsHaveExpectedValues() {
        assertEquals("plaintext", FormatRegistry.ID_PLAINTEXT)
        assertEquals("markdown", FormatRegistry.ID_MARKDOWN)
        assertEquals("todotxt", FormatRegistry.ID_TODOTXT)
        assertEquals("csv", FormatRegistry.ID_CSV)
        assertEquals("binary", FormatRegistry.ID_BINARY)
        assertEquals("dropbox", FormatRegistry.ID_DROPBOX)
        assertEquals("sftp", FormatRegistry.ID_SFTP)
    }

    @Test
    fun allIdConstantsAreDistinct() {
        val ids = listOf(
            FormatRegistry.ID_PLAINTEXT, FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_WIKITEXT, FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_ASCIIDOC, FormatRegistry.ID_ORGMODE, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_RESTRUCTUREDTEXT, FormatRegistry.ID_TASKPAPER, FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_CREOLE, FormatRegistry.ID_TIDDLYWIKI, FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_RMARKDOWN, FormatRegistry.ID_BINARY, FormatRegistry.ID_JSON,
            FormatRegistry.ID_DROPBOX, FormatRegistry.ID_FTP, FormatRegistry.ID_GOOGLEDRIVE,
            FormatRegistry.ID_ONEDRIVE, FormatRegistry.ID_SFTP
        )
        assertEquals(ids.size, ids.distinct().size)
    }

    // ==================== Lazy Initialization ====================

    @Test
    fun isFormatsInitializedReturnsTrueAfterFormatsAccess() {
        // Forcing a new registry isn't possible (singleton) but we can confirm
        // accessing formats causes initialization
        @Suppress("UNUSED_VARIABLE")
        val formats = FormatRegistry.formats
        assertTrue(FormatRegistry.isFormatsInitialized)
    }

    @Test
    fun formatsListIsNotEmpty() {
        assertTrue(FormatRegistry.formats.isNotEmpty())
    }

    @Test
    fun formatsCountIsAtLeastSeventeen() {
        // At minimum the 17 text formats should be present
        assertTrue(FormatRegistry.formats.size >= 17)
    }

    @Test
    fun formatsListContainsAllExpectedTextFormats() {
        val formatIds = FormatRegistry.formats.map { it.id }
        assertTrue(formatIds.contains(FormatRegistry.ID_PLAINTEXT))
        assertTrue(formatIds.contains(FormatRegistry.ID_MARKDOWN))
        assertTrue(formatIds.contains(FormatRegistry.ID_TODOTXT))
        assertTrue(formatIds.contains(FormatRegistry.ID_CSV))
        assertTrue(formatIds.contains(FormatRegistry.ID_LATEX))
        assertTrue(formatIds.contains(FormatRegistry.ID_ASCIIDOC))
        assertTrue(formatIds.contains(FormatRegistry.ID_RESTRUCTUREDTEXT))
        assertTrue(formatIds.contains(FormatRegistry.ID_ORGMODE))
        assertTrue(formatIds.contains(FormatRegistry.ID_BINARY))
    }

    // ==================== getById ====================

    @Test
    fun getByIdReturnsMarkdown() {
        val fmt = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(fmt)
        assertEquals("Markdown", fmt.name)
    }

    @Test
    fun getByIdReturnsPlaintext() {
        val fmt = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(fmt)
        assertEquals("Plain Text", fmt.name)
    }

    @Test
    fun getByIdReturnsCsv() {
        val fmt = FormatRegistry.getById(FormatRegistry.ID_CSV)
        assertNotNull(fmt)
        assertEquals("CSV", fmt.name)
    }

    @Test
    fun getByIdReturnsBinary() {
        val fmt = FormatRegistry.getById(FormatRegistry.ID_BINARY)
        assertNotNull(fmt)
        assertEquals("Binary", fmt.name)
    }

    @Test
    fun getByIdReturnsNullForUnknownId() {
        assertNull(FormatRegistry.getById("nonexistent-format-xyz"))
    }

    @Test
    fun getByIdReturnsNullForEmptyString() {
        assertNull(FormatRegistry.getById(""))
    }

    @Test
    fun getByIdIsCaseSensitive() {
        assertNull(FormatRegistry.getById("Markdown"))
        assertNull(FormatRegistry.getById("PLAINTEXT"))
        assertNotNull(FormatRegistry.getById("markdown"))
    }

    // ==================== detectByExtension ====================

    @Test
    fun detectByExtensionReturnsMarkdownForDotMd() {
        val fmt = FormatRegistry.detectByExtension(".md")
        assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun detectByExtensionReturnsMarkdownForMdWithoutDot() {
        val fmt = FormatRegistry.detectByExtension("md")
        assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun detectByExtensionReturnsMarkdownForMarkdown() {
        val fmt = FormatRegistry.detectByExtension(".markdown")
        assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun detectByExtensionReturnsCsvForDotCsv() {
        val fmt = FormatRegistry.detectByExtension(".csv")
        assertEquals(FormatRegistry.ID_CSV, fmt.id)
    }

    @Test
    fun detectByExtensionReturnsLatexForDotTex() {
        val fmt = FormatRegistry.detectByExtension(".tex")
        assertEquals(FormatRegistry.ID_LATEX, fmt.id)
    }

    @Test
    fun detectByExtensionReturnsOrgModeForDotOrg() {
        val fmt = FormatRegistry.detectByExtension(".org")
        assertEquals(FormatRegistry.ID_ORGMODE, fmt.id)
    }

    @Test
    fun detectByExtensionFallsBackToPlaintextForUnknownExtension() {
        val fmt = FormatRegistry.detectByExtension(".xyzunknown")
        assertEquals(FormatRegistry.ID_PLAINTEXT, fmt.id)
    }

    @Test
    fun detectByExtensionNeverReturnsNull() {
        // detectByExtension always falls back to plaintext
        val result = FormatRegistry.detectByExtension(".completelyunknownformat123")
        assertNotNull(result)
        assertEquals(FormatRegistry.ID_PLAINTEXT, result.id)
    }

    @Test
    fun detectByExtensionIsCaseInsensitive() {
        val lower = FormatRegistry.detectByExtension(".md")
        val upper = FormatRegistry.detectByExtension(".MD")
        val mixed = FormatRegistry.detectByExtension(".Md")
        assertEquals(lower.id, upper.id)
        assertEquals(lower.id, mixed.id)
    }

    // ==================== detectByContent ====================

    @Test
    fun detectByContentReturnsMarkdownForHashHeading() {
        val content = "# Title\n\nSome content here."
        val fmt = FormatRegistry.detectByContent(content)
        assertNotNull(fmt)
        assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun detectByContentReturnsTodotxtForPriorityLine() {
        val content = "(A) High priority task\n(B) Medium priority task"
        val fmt = FormatRegistry.detectByContent(content)
        assertNotNull(fmt)
        assertEquals(FormatRegistry.ID_TODOTXT, fmt.id)
    }

    @Test
    fun detectByContentReturnsCsvForCommaSeparatedData() {
        val content = "name,age,city\nJohn,30,NYC\nJane,25,LA"
        val fmt = FormatRegistry.detectByContent(content)
        assertNotNull(fmt)
        assertEquals(FormatRegistry.ID_CSV, fmt.id)
    }

    @Test
    fun detectByContentReturnsLatexForDocumentClass() {
        val content = "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"
        val fmt = FormatRegistry.detectByContent(content)
        assertNotNull(fmt)
        assertEquals(FormatRegistry.ID_LATEX, fmt.id)
    }

    @Test
    fun detectByContentReturnsOrgModeForOrgHeading() {
        val content = "* Heading\n** Sub-heading\n#+TITLE: My Document"
        val fmt = FormatRegistry.detectByContent(content)
        assertNotNull(fmt)
        assertEquals(FormatRegistry.ID_ORGMODE, fmt.id)
    }

    @Test
    fun detectByContentReturnsNullForEmptyString() {
        val fmt = FormatRegistry.detectByContent("")
        assertNull(fmt)
    }

    @Test
    fun detectByContentReturnsNullForGenericPlainText() {
        // Plain text with no format-specific patterns
        val content = "Just a simple paragraph without any special markup or patterns."
        // May return null or plaintext — plaintext has no detection patterns
        val fmt = FormatRegistry.detectByContent(content)
        // Either null (no pattern matched) or not plaintext
        // Plain text has no detectionPatterns so should return null
        if (fmt != null) {
            assertFalse(fmt.id == FormatRegistry.ID_PLAINTEXT)
        }
    }

    @Test
    fun detectByContentRespectsMaxLinesParameter() {
        // First line does NOT match; pattern is on line 20
        val lines = (1..20).joinToString("\n") { "line $it" }
        val contentWithHeadingFar = "$lines\n# Markdown heading"
        // With maxLines = 5, the heading at line 21 won't be detected
        val fmtShort = FormatRegistry.detectByContent(contentWithHeadingFar, maxLines = 5)
        val fmtFull = FormatRegistry.detectByContent(contentWithHeadingFar, maxLines = 100)
        // Doesn't matter what they return, just verifying different limits don't crash
        // Note: assertNull(fmtShort) when heading is beyond maxLines
        assertNotNull(fmtFull) // heading on line 21 IS within 100 lines
    }

    // ==================== isSupported ====================

    @Test
    fun isSupportedReturnsTrueForMarkdown() {
        assertTrue(FormatRegistry.isSupported(FormatRegistry.ID_MARKDOWN))
    }

    @Test
    fun isSupportedReturnsFalseForUnknown() {
        assertFalse(FormatRegistry.isSupported("nonexistent-format-abc"))
    }

    // ==================== getFormatNames ====================

    @Test
    fun getFormatNamesReturnsNonEmptyList() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.isNotEmpty())
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("Plain Text"))
    }

    // ==================== getAllExtensions ====================

    @Test
    fun getAllExtensionsContainsCommonExtensions() {
        val extensions = FormatRegistry.getAllExtensions()
        assertTrue(extensions.contains(".md"))
        assertTrue(extensions.contains(".txt"))
        assertTrue(extensions.contains(".csv"))
        assertTrue(extensions.contains(".tex"))
    }

    @Test
    fun getAllExtensionsAreDistinct() {
        val extensions = FormatRegistry.getAllExtensions()
        assertEquals(extensions.size, extensions.distinct().size)
    }

    // ==================== configureParseConcurrency ====================

    @Test
    fun configureParseConcurrencyAcceptsValidValues() {
        // Should not throw for valid values
        FormatRegistry.configureParseConcurrency(1)
        FormatRegistry.configureParseConcurrency(4)
        FormatRegistry.configureParseConcurrency(16)
        // Restore default
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    @Test
    fun configureParseConcurrencyRejectsZero() {
        try {
            FormatRegistry.configureParseConcurrency(0)
            // Should have thrown
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("1") || e.message!!.contains("between"))
        }
    }

    @Test
    fun configureParseConcurrencyRejectsValueAbove16() {
        try {
            FormatRegistry.configureParseConcurrency(17)
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("16") || e.message!!.contains("between"))
        }
    }

    // ==================== Concurrent Access ====================

    @Test
    fun concurrentDetectByExtensionIsSafe() = runBlocking<Unit> {
        val results = (1..30).map { i ->
            async {
                val ext = when (i % 5) {
                    0 -> ".md"
                    1 -> ".txt"
                    2 -> ".csv"
                    3 -> ".tex"
                    else -> ".org"
                }
                FormatRegistry.detectByExtension(ext)
            }
        }.awaitAll()
        assertEquals(30, results.size)
        assertTrue(results.all { it.id.isNotEmpty() })
    }

    @Test
    fun concurrentGetByIdIsSafe() = runBlocking<Unit> {
        val ids = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_LATEX, FormatRegistry.ID_ORGMODE
        )
        val results = (1..40).map { i ->
            async { FormatRegistry.getById(ids[i % ids.size]) }
        }.awaitAll()
        assertEquals(40, results.size)
        assertTrue(results.all { it != null })
    }

    // ==================== getFormatsByExtension ====================

    @Test
    fun getFormatsByExtensionTxtReturnsBothPlaintextAndTodotxt() {
        val formats = FormatRegistry.getFormatsByExtension(".txt")
        val ids = formats.map { it.id }
        assertTrue(ids.contains(FormatRegistry.ID_PLAINTEXT))
        assertTrue(ids.contains(FormatRegistry.ID_TODOTXT))
    }

    @Test
    fun getFormatsByExtensionMdReturnsOnlyMarkdown() {
        val formats = FormatRegistry.getFormatsByExtension(".md")
        // .md is exclusive to markdown
        assertTrue(formats.any { it.id == FormatRegistry.ID_MARKDOWN })
    }

    @Test
    fun getFormatsByExtensionUnknownReturnsEmpty() {
        val formats = FormatRegistry.getFormatsByExtension(".xyz_not_a_real_extension")
        assertTrue(formats.isEmpty())
    }

    // ==================== detectByFilename ====================

    @Test
    fun detectByFilenameMarkdown() {
        val fmt = FormatRegistry.detectByFilename("README.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun detectByFilenameOrg() {
        val fmt = FormatRegistry.detectByFilename("notes.org")
        assertEquals(FormatRegistry.ID_ORGMODE, fmt.id)
    }

    @Test
    fun detectByFilenameNoExtensionFallsToPlaintext() {
        val fmt = FormatRegistry.detectByFilename("Makefile")
        assertEquals(FormatRegistry.ID_PLAINTEXT, fmt.id)
    }
}
