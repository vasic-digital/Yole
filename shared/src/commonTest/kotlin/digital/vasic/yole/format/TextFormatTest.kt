/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for TextFormat data class
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for TextFormat data class.
 *
 * Tests cover:
 * - Constructor with all parameters
 * - Constructor with default parameters
 * - Data class properties
 * - Data class equality and hashing
 * - Data class copy functionality
 * - Companion object constants
 * - toString() and componentN() functions
 *
 * Target Coverage: 100% (data class)
 */
class TextFormatTest {

    // ==================== Constructor Tests ====================

    @Test
    fun `constructor should create format with all parameters`() {
        val format = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test",
            extensions = listOf(".test", ".tst"),
            detectionPatterns = listOf("^test ", "^TEST")
        )

        assertEquals("test", format.id)
        assertEquals("Test Format", format.name)
        assertEquals(".test", format.defaultExtension)
        assertEquals(listOf(".test", ".tst"), format.extensions)
        assertEquals(listOf("^test ", "^TEST"), format.detectionPatterns)
    }

    @Test
    fun `constructor should use default extension when extensions not provided`() {
        val format = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test"
        )

        assertEquals(listOf(".test"), format.extensions)
    }

    @Test
    fun `constructor should use empty list when detectionPatterns not provided`() {
        val format = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test"
        )

        assertTrue(format.detectionPatterns.isEmpty())
    }

    @Test
    fun `constructor should handle multiple extensions`() {
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            extensions = listOf(".md", ".markdown", ".mdown", ".mkd")
        )

        assertEquals(4, format.extensions.size)
        assertTrue(format.extensions.contains(".md"))
        assertTrue(format.extensions.contains(".markdown"))
        assertTrue(format.extensions.contains(".mdown"))
        assertTrue(format.extensions.contains(".mkd"))
    }

    @Test
    fun `constructor should handle multiple detection patterns`() {
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            detectionPatterns = listOf("^#+ ", "^\\[.*\\]\\(.*\\)", "^\\*\\*.*\\*\\*")
        )

        assertEquals(3, format.detectionPatterns.size)
    }

    // ==================== Property Tests ====================

    @Test
    fun `id property should return correct value`() {
        val format = TextFormat(
            id = "latex",
            name = "LaTeX",
            defaultExtension = ".tex"
        )

        assertEquals("latex", format.id)
    }

    @Test
    fun `name property should return correct value`() {
        val format = TextFormat(
            id = "latex",
            name = "LaTeX",
            defaultExtension = ".tex"
        )

        assertEquals("LaTeX", format.name)
    }

    @Test
    fun `defaultExtension property should return correct value`() {
        val format = TextFormat(
            id = "latex",
            name = "LaTeX",
            defaultExtension = ".tex"
        )

        assertEquals(".tex", format.defaultExtension)
    }

    @Test
    fun `extensions property should return correct list`() {
        val extensions = listOf(".md", ".markdown")
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            extensions = extensions
        )

        assertEquals(extensions, format.extensions)
    }

    @Test
    fun `detectionPatterns property should return correct list`() {
        val patterns = listOf("^#+ ", "^\\*\\* ")
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            detectionPatterns = patterns
        )

        assertEquals(patterns, format.detectionPatterns)
    }

    // ==================== Data Class Equality Tests ====================

    @Test
    fun `equals should return true for identical formats`() {
        val format1 = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )
        val format2 = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )

        assertEquals(format1, format2)
    }

    @Test
    fun `equals should return false for different ids`() {
        val format1 = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )
        val format2 = TextFormat(
            id = "latex",
            name = "Markdown",
            defaultExtension = ".md"
        )

        assertNotEquals(format1, format2)
    }

    @Test
    fun `equals should return false for different names`() {
        val format1 = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test"
        )
        val format2 = TextFormat(
            id = "test",
            name = "Different Name",
            defaultExtension = ".test"
        )

        assertNotEquals(format1, format2)
    }

    @Test
    fun `equals should return false for different defaultExtensions`() {
        val format1 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test"
        )
        val format2 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".tst"
        )

        assertNotEquals(format1, format2)
    }

    @Test
    fun `equals should return false for different extensions`() {
        val format1 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            extensions = listOf(".test", ".tst")
        )
        val format2 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            extensions = listOf(".test")
        )

        assertNotEquals(format1, format2)
    }

    @Test
    fun `equals should return false for different detectionPatterns`() {
        val format1 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            detectionPatterns = listOf("^test")
        )
        val format2 = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            detectionPatterns = listOf("^TEST")
        )

        assertNotEquals(format1, format2)
    }

    @Test
    fun `hashCode should be same for equal objects`() {
        val format1 = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )
        val format2 = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )

        assertEquals(format1.hashCode(), format2.hashCode())
    }

    // ==================== Data Class Copy Tests ====================

    @Test
    fun `copy should create identical format when no parameters changed`() {
        val original = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            extensions = listOf(".md", ".markdown"),
            detectionPatterns = listOf("^#+ ")
        )

        val copy = original.copy()

        assertEquals(original, copy)
    }

    @Test
    fun `copy should change only specified id parameter`() {
        val original = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )

        val copy = original.copy(id = "modified")

        assertEquals("modified", copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(original.defaultExtension, copy.defaultExtension)
    }

    @Test
    fun `copy should change only specified name parameter`() {
        val original = TextFormat(
            id = "test",
            name = "Original",
            defaultExtension = ".test"
        )

        val copy = original.copy(name = "Modified")

        assertEquals(original.id, copy.id)
        assertEquals("Modified", copy.name)
        assertEquals(original.defaultExtension, copy.defaultExtension)
    }

    @Test
    fun `copy should change only specified defaultExtension parameter`() {
        val original = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test"
        )

        val copy = original.copy(defaultExtension = ".tst")

        assertEquals(original.id, copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(".tst", copy.defaultExtension)
    }

    @Test
    fun `copy should change only specified extensions parameter`() {
        val original = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            extensions = listOf(".test")
        )

        val newExtensions = listOf(".test", ".tst", ".t")
        val copy = original.copy(extensions = newExtensions)

        assertEquals(original.id, copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(newExtensions, copy.extensions)
    }

    @Test
    fun `copy should change multiple parameters`() {
        val original = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test"
        )

        val copy = original.copy(
            id = "modified",
            name = "Modified Test"
        )

        assertEquals("modified", copy.id)
        assertEquals("Modified Test", copy.name)
        assertEquals(original.defaultExtension, copy.defaultExtension)
    }

    // ==================== Data Class Component Functions Tests ====================

    @Test
    fun `component1 should return id`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test"
        )

        val (id) = format

        assertEquals("test", id)
    }

    @Test
    fun `component2 should return name`() {
        val format = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test"
        )

        val (_, name) = format

        assertEquals("Test Format", name)
    }

    @Test
    fun `component3 should return defaultExtension`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test"
        )

        val (_, _, defaultExtension) = format

        assertEquals(".test", defaultExtension)
    }

    @Test
    fun `destructuring should work for all components`() {
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md",
            extensions = listOf(".md", ".markdown"),
            detectionPatterns = listOf("^#+ ")
        )

        val (id, name, defaultExt, exts, patterns) = format

        assertEquals("markdown", id)
        assertEquals("Markdown", name)
        assertEquals(".md", defaultExt)
        assertEquals(listOf(".md", ".markdown"), exts)
        assertEquals(listOf("^#+ "), patterns)
    }

    // ==================== ToString Tests ====================

    @Test
    fun `toString should include all properties`() {
        val format = TextFormat(
            id = "markdown",
            name = "Markdown",
            defaultExtension = ".md"
        )

        val string = format.toString()

        assertTrue(string.contains("markdown"))
        assertTrue(string.contains("Markdown"))
        assertTrue(string.contains(".md"))
    }

    // ==================== Companion Object Constants Tests ====================

    @Test
    fun `companion object should have all format ID constants`() {
        assertEquals("unknown", TextFormat.ID_UNKNOWN)
        assertEquals("plaintext", TextFormat.ID_PLAINTEXT)
        assertEquals("markdown", TextFormat.ID_MARKDOWN)
        assertEquals("todotxt", TextFormat.ID_TODOTXT)
        assertEquals("csv", TextFormat.ID_CSV)
        assertEquals("wikitext", TextFormat.ID_WIKITEXT)
        assertEquals("keyvalue", TextFormat.ID_KEYVALUE)
        assertEquals("asciidoc", TextFormat.ID_ASCIIDOC)
        assertEquals("orgmode", TextFormat.ID_ORGMODE)
        assertEquals("latex", TextFormat.ID_LATEX)
        assertEquals("restructuredtext", TextFormat.ID_RESTRUCTUREDTEXT)
        assertEquals("taskpaper", TextFormat.ID_TASKPAPER)
        assertEquals("textile", TextFormat.ID_TEXTILE)
        assertEquals("creole", TextFormat.ID_CREOLE)
        assertEquals("tiddlywiki", TextFormat.ID_TIDDLYWIKI)
        assertEquals("jupyter", TextFormat.ID_JUPYTER)
        assertEquals("rmarkdown", TextFormat.ID_RMARKDOWN)
        assertEquals("binary", TextFormat.ID_BINARY)
    }

    @Test
    fun `format ID constants should be lowercase`() {
        val constants = listOf(
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

        constants.forEach { id ->
            assertEquals(id.lowercase(), id, "Format ID '$id' should be lowercase")
        }
    }

    @Test
    fun `format ID constants should not contain spaces`() {
        val constants = listOf(
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

        constants.forEach { id ->
            assertTrue(!id.contains(" "), "Format ID '$id' should not contain spaces")
        }
    }

    @Test
    fun `all format ID constants should be unique`() {
        val constants = listOf(
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

        val uniqueConstants = constants.distinct()
        assertEquals(constants.size, uniqueConstants.size, "All format ID constants should be unique")
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty strings`() {
        val format = TextFormat(
            id = "",
            name = "",
            defaultExtension = ""
        )

        assertEquals("", format.id)
        assertEquals("", format.name)
        assertEquals("", format.defaultExtension)
    }

    @Test
    fun `should handle special characters in id`() {
        val format = TextFormat(
            id = "test-format_123",
            name = "Test",
            defaultExtension = ".test"
        )

        assertEquals("test-format_123", format.id)
    }

    @Test
    fun `should handle special characters in name`() {
        val format = TextFormat(
            id = "test",
            name = "Test Format (v2.0) [Beta]",
            defaultExtension = ".test"
        )

        assertEquals("Test Format (v2.0) [Beta]", format.name)
    }

    @Test
    fun `should handle extension without dot`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = "test"
        )

        assertEquals("test", format.defaultExtension)
    }

    @Test
    fun `should handle empty extensions list`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            extensions = emptyList()
        )

        assertTrue(format.extensions.isEmpty())
    }

    @Test
    fun `should handle empty detectionPatterns list`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            detectionPatterns = emptyList()
        )

        assertTrue(format.detectionPatterns.isEmpty())
    }

    @Test
    fun `should handle regex special characters in detectionPatterns`() {
        val format = TextFormat(
            id = "test",
            name = "Test",
            defaultExtension = ".test",
            detectionPatterns = listOf(
                "^\\[.*\\]\\(.*\\)",
                "\\\\documentclass",
                "^\\*\\*.*\\*\\*"
            )
        )

        assertEquals(3, format.detectionPatterns.size)
        assertTrue(format.detectionPatterns.contains("^\\[.*\\]\\(.*\\)"))
        assertTrue(format.detectionPatterns.contains("\\\\documentclass"))
        assertTrue(format.detectionPatterns.contains("^\\*\\*.*\\*\\*"))
    }
}
