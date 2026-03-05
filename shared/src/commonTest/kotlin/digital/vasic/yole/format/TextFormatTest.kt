/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TextFormat data class
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TextFormatTest {

    @Test
    fun testTextFormatCreation() {
        val format = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".tst",
            extensions = listOf(".tst", ".test"),
            detectionPatterns = listOf("^test:")
        )
        assertEquals("test", format.id)
        assertEquals("Test Format", format.name)
        assertEquals(".tst", format.defaultExtension)
        assertEquals(2, format.extensions.size)
        assertEquals(1, format.detectionPatterns.size)
    }

    @Test
    fun testDefaultExtensionUsedWhenNoExtensionsList() {
        val format = TextFormat(
            id = "simple",
            name = "Simple",
            defaultExtension = ".sim"
        )
        assertEquals(listOf(".sim"), format.extensions)
    }

    @Test
    fun testEmptyDetectionPatternsDefault() {
        val format = TextFormat(id = "x", name = "X", defaultExtension = ".x")
        assertTrue(format.detectionPatterns.isEmpty())
    }

    @Test
    fun testDataClassEquality() {
        val f1 = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        val f2 = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())
    }

    @Test
    fun testDataClassInequality() {
        val f1 = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        val f2 = TextFormat(id = "txt", name = "Plain Text", defaultExtension = ".txt")
        assertNotEquals(f1, f2)
    }

    @Test
    fun testDataClassCopy() {
        val original = TextFormat(id = "md", name = "Markdown", defaultExtension = ".md")
        val copy = original.copy(name = "Modified")
        assertEquals("md", copy.id)
        assertEquals("Modified", copy.name)
    }

    @Test
    fun testFormatIdConstants() {
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
}
