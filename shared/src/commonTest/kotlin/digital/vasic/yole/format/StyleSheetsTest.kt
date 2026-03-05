/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StyleSheets utility
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StyleSheetsTest {

    @Test
    fun testMarkdownStylesReturned() {
        val styles = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(styles.isNotEmpty(), "Markdown styles should not be empty")
    }

    @Test
    fun testWikitextStylesReturned() {
        val styles = StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, lightMode = true)
        assertTrue(styles.isNotEmpty(), "Wikitext styles should not be empty")
    }

    @Test
    fun testRstLightAndDarkDiffer() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, lightMode = true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, lightMode = false)
        assertTrue(light.isNotEmpty())
        assertTrue(dark.isNotEmpty())
        assertNotEquals(light, dark, "RST light and dark styles should differ")
    }

    @Test
    fun testOrgmodeLightAndDarkDiffer() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode = true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode = false)
        assertTrue(light.isNotEmpty())
        assertTrue(dark.isNotEmpty())
        assertNotEquals(light, dark, "OrgMode light and dark styles should differ")
    }

    @Test
    fun testAsciidocLightAndDarkDiffer() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, lightMode = true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, lightMode = false)
        assertTrue(light.isNotEmpty())
        assertTrue(dark.isNotEmpty())
        assertNotEquals(light, dark, "AsciiDoc light and dark styles should differ")
    }

    @Test
    fun testLatexLightAndDarkDiffer() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = false)
        assertTrue(light.isNotEmpty())
        assertTrue(dark.isNotEmpty())
        assertNotEquals(light, dark, "LaTeX light and dark styles should differ")
    }

    @Test
    fun testUnknownFormatReturnsEmpty() {
        val styles = StyleSheets.getStyleSheet("unknown_format", lightMode = true)
        assertEquals("", styles, "Unknown format should return empty string")
    }

    @Test
    fun testMarkdownStylesSameForLightAndDark() {
        // Markdown uses same styles regardless of theme
        val light = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        assertEquals(light, dark, "Markdown styles should be same for both themes")
    }
}
