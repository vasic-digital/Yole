/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Extended unit tests for TextFormat covering all 17
 * format IDs, extension lists, detection patterns,
 * metadata, equality, and property-based checks.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Extended unit tests for [TextFormat] and its companion constants.
 *
 * Validates all 17 text format IDs exist on both [TextFormat.Companion]
 * and [FormatRegistry], that every registered format has correct metadata,
 * extension detection works per format, detection patterns compile as
 * valid regexes, and data class semantics hold.
 *
 * Total: 48 tests
 */
class TextFormatExtendedTest {

    // ==================== TextFormat Companion ID Constants ====================

    @Test
    fun companionHasUnknownId() {
        assertEquals("unknown", TextFormat.ID_UNKNOWN)
    }

    @Test
    fun companionHasPlaintextId() {
        assertEquals("plaintext", TextFormat.ID_PLAINTEXT)
    }

    @Test
    fun companionHasMarkdownId() {
        assertEquals("markdown", TextFormat.ID_MARKDOWN)
    }

    @Test
    fun companionHasTodotxtId() {
        assertEquals("todotxt", TextFormat.ID_TODOTXT)
    }

    @Test
    fun companionHasCsvId() {
        assertEquals("csv", TextFormat.ID_CSV)
    }

    @Test
    fun companionHasWikitextId() {
        assertEquals("wikitext", TextFormat.ID_WIKITEXT)
    }

    @Test
    fun companionHasKeyValueId() {
        assertEquals("keyvalue", TextFormat.ID_KEYVALUE)
    }

    @Test
    fun companionHasAsciidocId() {
        assertEquals("asciidoc", TextFormat.ID_ASCIIDOC)
    }

    @Test
    fun companionHasOrgmodeId() {
        assertEquals("orgmode", TextFormat.ID_ORGMODE)
    }

    @Test
    fun companionHasLatexId() {
        assertEquals("latex", TextFormat.ID_LATEX)
    }

    @Test
    fun companionHasRestructuredtextId() {
        assertEquals("restructuredtext", TextFormat.ID_RESTRUCTUREDTEXT)
    }

    @Test
    fun companionHasTaskpaperIdId() {
        assertEquals("taskpaper", TextFormat.ID_TASKPAPER)
    }

    @Test
    fun companionHasTextileId() {
        assertEquals("textile", TextFormat.ID_TEXTILE)
    }

    @Test
    fun companionHasCreoleId() {
        assertEquals("creole", TextFormat.ID_CREOLE)
    }

    @Test
    fun companionHasTiddlywikiId() {
        assertEquals("tiddlywiki", TextFormat.ID_TIDDLYWIKI)
    }

    @Test
    fun companionHasJupyterId() {
        assertEquals("jupyter", TextFormat.ID_JUPYTER)
    }

    @Test
    fun companionHasRmarkdownId() {
        assertEquals("rmarkdown", TextFormat.ID_RMARKDOWN)
    }

    @Test
    fun companionHasBinaryId() {
        assertEquals("binary", TextFormat.ID_BINARY)
    }

    @Test
    fun allCompanionIdsAreDistinct() {
        val ids = listOf(
            TextFormat.ID_UNKNOWN,
            TextFormat.ID_PLAINTEXT,
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_TODOTXT,
            TextFormat.ID_CSV,
            TextFormat.ID_WIKITEXT,
            TextFormat.ID_KEYVALUE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_LATEX,
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_TASKPAPER,
            TextFormat.ID_TEXTILE,
            TextFormat.ID_CREOLE,
            TextFormat.ID_TIDDLYWIKI,
            TextFormat.ID_JUPYTER,
            TextFormat.ID_RMARKDOWN,
            TextFormat.ID_BINARY
        )
        assertEquals(ids.size, ids.distinct().size, "Duplicate ID found in TextFormat companion")
    }

    // ==================== FormatRegistry Has All 17 Text Formats ====================

    @Test
    fun registryContainsAllSeventeenTextFormats() {
        val requiredIds = listOf(
            TextFormat.ID_PLAINTEXT, TextFormat.ID_MARKDOWN, TextFormat.ID_TODOTXT,
            TextFormat.ID_CSV, TextFormat.ID_WIKITEXT, TextFormat.ID_KEYVALUE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_ORGMODE, TextFormat.ID_LATEX,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_TASKPAPER, TextFormat.ID_TEXTILE,
            TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI, TextFormat.ID_JUPYTER,
            TextFormat.ID_RMARKDOWN, TextFormat.ID_BINARY
        )
        val registeredIds = FormatRegistry.formats.map { it.id }
        for (id in requiredIds) {
            assertTrue(registeredIds.contains(id), "FormatRegistry is missing format: $id")
        }
    }

    // ==================== Format Metadata Correctness ====================

    @Test
    fun markdownFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_MARKDOWN)
        assertNotNull(fmt)
        assertEquals("Markdown", fmt.name)
        assertTrue(fmt.extensions.contains(".md"))
        assertTrue(fmt.extensions.contains(".markdown"))
        assertEquals(".md", fmt.defaultExtension)
        assertTrue(fmt.detectionPatterns.isNotEmpty())
    }

    @Test
    fun plaintextFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_PLAINTEXT)
        assertNotNull(fmt)
        assertEquals("Plain Text", fmt.name)
        assertTrue(fmt.extensions.contains(".txt"))
        assertEquals(".txt", fmt.defaultExtension)
    }

    @Test
    fun csvFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_CSV)
        assertNotNull(fmt)
        assertEquals("CSV", fmt.name)
        assertTrue(fmt.extensions.contains(".csv"))
        assertEquals(".csv", fmt.defaultExtension)
    }

    @Test
    fun latexFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_LATEX)
        assertNotNull(fmt)
        assertEquals("LaTeX", fmt.name)
        assertTrue(fmt.extensions.contains(".tex"))
        assertEquals(".tex", fmt.defaultExtension)
        assertTrue(fmt.detectionPatterns.isNotEmpty())
    }

    @Test
    fun orgmodeFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_ORGMODE)
        assertNotNull(fmt)
        assertEquals("Org Mode", fmt.name)
        assertTrue(fmt.extensions.contains(".org"))
        assertEquals(".org", fmt.defaultExtension)
    }

    @Test
    fun asciidocFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_ASCIIDOC)
        assertNotNull(fmt)
        assertEquals("AsciiDoc", fmt.name)
        assertTrue(fmt.extensions.contains(".adoc"))
    }

    @Test
    fun restructuredtextFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_RESTRUCTUREDTEXT)
        assertNotNull(fmt)
        assertEquals("reStructuredText", fmt.name)
        assertTrue(fmt.extensions.contains(".rst"))
    }

    @Test
    fun jupyterFormatHasCorrectMetadata() {
        val fmt = FormatRegistry.getById(TextFormat.ID_JUPYTER)
        assertNotNull(fmt)
        assertEquals("Jupyter Notebook", fmt.name)
        assertTrue(fmt.extensions.contains(".ipynb"))
    }

    @Test
    fun binaryFormatHasNoExtensions() {
        val fmt = FormatRegistry.getById(TextFormat.ID_BINARY)
        assertNotNull(fmt)
        assertEquals("Binary", fmt.name)
        assertTrue(fmt.extensions.isEmpty())
    }

    @Test
    fun rmarkdownFormatHasRmdExtension() {
        val fmt = FormatRegistry.getById(TextFormat.ID_RMARKDOWN)
        assertNotNull(fmt)
        assertEquals("R Markdown", fmt.name)
        assertTrue(fmt.extensions.contains(".rmd"))
    }

    // ==================== Extension Detection ====================

    @Test
    fun mdExtensionDetectsMarkdown() {
        assertTrue(FormatRegistry.isExtensionSupported(".md"))
        val fmt = FormatRegistry.getByExtension(".md")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_MARKDOWN, fmt.id)
    }

    @Test
    fun texExtensionDetectsLatex() {
        assertTrue(FormatRegistry.isExtensionSupported(".tex"))
        val fmt = FormatRegistry.getByExtension(".tex")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_LATEX, fmt.id)
    }

    @Test
    fun orgExtensionDetectsOrgMode() {
        assertTrue(FormatRegistry.isExtensionSupported(".org"))
        val fmt = FormatRegistry.getByExtension(".org")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_ORGMODE, fmt.id)
    }

    @Test
    fun ipynbExtensionDetectsJupyter() {
        assertTrue(FormatRegistry.isExtensionSupported(".ipynb"))
        val fmt = FormatRegistry.getByExtension(".ipynb")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_JUPYTER, fmt.id)
    }

    @Test
    fun adocExtensionDetectsAsciidoc() {
        assertTrue(FormatRegistry.isExtensionSupported(".adoc"))
        val fmt = FormatRegistry.getByExtension(".adoc")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_ASCIIDOC, fmt.id)
    }

    @Test
    fun rstExtensionDetectsRestructuredText() {
        assertTrue(FormatRegistry.isExtensionSupported(".rst"))
        val fmt = FormatRegistry.getByExtension(".rst")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, fmt.id)
    }

    @Test
    fun csvExtensionDetectsCsv() {
        assertTrue(FormatRegistry.isExtensionSupported(".csv"))
        val fmt = FormatRegistry.getByExtension(".csv")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_CSV, fmt.id)
    }

    @Test
    fun wikiExtensionDetectsWikitext() {
        assertTrue(FormatRegistry.isExtensionSupported(".wiki"))
        val fmt = FormatRegistry.getByExtension(".wiki")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_WIKITEXT, fmt.id)
    }

    @Test
    fun taskpaperExtensionDetectsTaskpaper() {
        assertTrue(FormatRegistry.isExtensionSupported(".taskpaper"))
        val fmt = FormatRegistry.getByExtension(".taskpaper")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_TASKPAPER, fmt.id)
    }

    @Test
    fun rmdExtensionDetectsRmarkdown() {
        assertTrue(FormatRegistry.isExtensionSupported(".rmd"))
        val fmt = FormatRegistry.getByExtension(".rmd")
        assertNotNull(fmt)
        assertEquals(TextFormat.ID_RMARKDOWN, fmt.id)
    }

    // ==================== Detection Patterns Are Valid Regexes ====================

    @Test
    fun allDetectionPatternsCompileAsValidRegexes() {
        for (format in FormatRegistry.formats) {
            for (pattern in format.detectionPatterns) {
                try {
                    Regex(pattern, RegexOption.MULTILINE)
                } catch (e: Exception) {
                    assertTrue(false, "Invalid regex pattern '$pattern' in format '${format.id}': ${e.message}")
                }
            }
        }
    }

    // ==================== TextFormat Data Class Semantics ====================

    @Test
    fun textFormatEquality() {
        val fmt1 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val fmt2 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        assertEquals(fmt1, fmt2)
    }

    @Test
    fun textFormatInequalityOnId() {
        val fmt1 = TextFormat(id = "a", name = "A", defaultExtension = ".a")
        val fmt2 = TextFormat(id = "b", name = "A", defaultExtension = ".a")
        assertFalse(fmt1 == fmt2)
    }

    @Test
    fun textFormatDefaultExtensionUsedAsExtensionWhenNotProvided() {
        val fmt = TextFormat(id = "x", name = "X", defaultExtension = ".x")
        assertEquals(listOf(".x"), fmt.extensions)
    }

    @Test
    fun textFormatEmptyDetectionPatternsByDefault() {
        val fmt = TextFormat(id = "x", name = "X", defaultExtension = ".x")
        assertTrue(fmt.detectionPatterns.isEmpty())
    }

    @Test
    fun textFormatCustomExtensionListOverridesDefault() {
        val fmt = TextFormat(
            id = "multi",
            name = "Multi",
            defaultExtension = ".m",
            extensions = listOf(".m", ".multi", ".mn")
        )
        assertEquals(3, fmt.extensions.size)
        assertTrue(fmt.extensions.contains(".multi"))
    }

    @Test
    fun textFormatCopyProducesNewInstance() {
        val original = TextFormat(id = "orig", name = "Original", defaultExtension = ".o")
        val copy = original.copy(name = "Copy")
        assertEquals("orig", copy.id)
        assertEquals("Copy", copy.name)
        assertEquals(".o", copy.defaultExtension)
        assertFalse(original === copy)
    }

    // ==================== Property-Based: All Formats Have Non-Empty ID and Name ====================

    @Test
    fun allRegisteredFormatsHaveNonEmptyId() {
        for (fmt in FormatRegistry.formats) {
            assertTrue(fmt.id.isNotEmpty(), "Format has empty id: $fmt")
        }
    }

    @Test
    fun allRegisteredFormatsHaveNonEmptyName() {
        for (fmt in FormatRegistry.formats) {
            assertTrue(fmt.name.isNotEmpty(), "Format '${fmt.id}' has empty name")
        }
    }

    @Test
    fun allRegisteredFormatsHaveUniqueIds() {
        val ids = FormatRegistry.formats.map { it.id }
        assertEquals(ids.size, ids.distinct().size, "Duplicate format IDs found")
    }

    @Test
    fun allDetectionPatternsAreNonEmpty() {
        for (fmt in FormatRegistry.formats) {
            for (pattern in fmt.detectionPatterns) {
                assertTrue(pattern.isNotEmpty(), "Empty detection pattern in format '${fmt.id}'")
            }
        }
    }
}
