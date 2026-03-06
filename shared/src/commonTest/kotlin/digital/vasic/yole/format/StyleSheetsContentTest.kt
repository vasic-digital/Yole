/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Content validation tests for StyleSheets CSS constants
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleSheetsContentTest {

    // ==================== Markdown styles ====================

    @Test
    fun testMarkdownStylesContainStyleTag() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains("<style>"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains("</style>"))
    }

    @Test
    fun testMarkdownStylesContainCssClass() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown"))
    }

    @Test
    fun testMarkdownStylesContainHeadings() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h1"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h2"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h3"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h4"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h5"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown h6"))
    }

    @Test
    fun testMarkdownStylesContainCodeElements() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown code"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown pre"))
    }

    @Test
    fun testMarkdownStylesContainTableElements() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown table"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown table th"))
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown table td"))
    }

    @Test
    fun testMarkdownStylesContainLinkStyles() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown a"))
    }

    @Test
    fun testMarkdownStylesContainBlockquote() {
        assertTrue(StyleSheets.MARKDOWN_STYLES.contains(".markdown blockquote"))
    }

    // ==================== WikiText styles ====================

    @Test
    fun testWikitextStylesContainCssClass() {
        assertTrue(StyleSheets.WIKITEXT_STYLES.contains(".wikitext"))
    }

    @Test
    fun testWikitextStylesContainChecklist() {
        assertTrue(StyleSheets.WIKITEXT_STYLES.contains(".checklist"))
        assertTrue(StyleSheets.WIKITEXT_STYLES.contains(".checked"))
        assertTrue(StyleSheets.WIKITEXT_STYLES.contains(".crossed"))
    }

    @Test
    fun testWikitextStylesContainHighlight() {
        assertTrue(StyleSheets.WIKITEXT_STYLES.contains(".highlight"))
    }

    // ==================== RST styles light/dark ====================

    @Test
    fun testRstLightContainsLightClass() {
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".light"))
    }

    @Test
    fun testRstDarkContainsDarkClass() {
        assertTrue(StyleSheets.RST_STYLES_DARK.contains(".dark"))
    }

    @Test
    fun testRstStylesContainSections() {
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-section-1"))
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-section-6"))
        assertTrue(StyleSheets.RST_STYLES_DARK.contains(".rst-section-1"))
        assertTrue(StyleSheets.RST_STYLES_DARK.contains(".rst-section-6"))
    }

    @Test
    fun testRstStylesContainDirective() {
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-directive"))
        assertTrue(StyleSheets.RST_STYLES_DARK.contains(".rst-directive"))
    }

    @Test
    fun testRstStylesContainAdmonitions() {
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-admonition"))
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-admonition.note"))
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-admonition.warning"))
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-admonition.danger"))
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains(".rst-admonition.tip"))
    }

    @Test
    fun testRstLightBackgroundIsWhite() {
        assertTrue(StyleSheets.RST_STYLES_LIGHT.contains("background: white"))
    }

    @Test
    fun testRstDarkBackgroundIsDark() {
        assertTrue(StyleSheets.RST_STYLES_DARK.contains("background: #1e1e1e"))
    }

    // ==================== OrgMode styles light/dark ====================

    @Test
    fun testOrgmodeLightContainsLightClass() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".light"))
    }

    @Test
    fun testOrgmodeDarkContainsDarkClass() {
        assertTrue(StyleSheets.ORGMODE_STYLES_DARK.contains(".dark"))
    }

    @Test
    fun testOrgmodeStylesContainHeadings() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-heading-1"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-heading-6"))
    }

    @Test
    fun testOrgmodeStylesContainTodoStates() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-todo-todo"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-todo-done"))
    }

    @Test
    fun testOrgmodeStylesContainProperties() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-properties"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-property-key"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-property-value"))
    }

    @Test
    fun testOrgmodeStylesContainBlock() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-block"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-block-header"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-block-content"))
    }

    @Test
    fun testOrgmodeStylesContainInlineFormatting() {
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-bold"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-italic"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-underline"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-strikethrough"))
        assertTrue(StyleSheets.ORGMODE_STYLES_LIGHT.contains(".org-verbatim"))
    }

    // ==================== AsciiDoc styles light/dark ====================

    @Test
    fun testAsciidocStylesContainHeadings() {
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".asciidoc h1"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".asciidoc h6"))
    }

    @Test
    fun testAsciidocStylesContainAdmonitions() {
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".admonition-note"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".admonition-tip"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".admonition-warning"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".admonition-important"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".admonition-caution"))
    }

    @Test
    fun testAsciidocLightHasLightBackground() {
        assertFalse(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains("background-color: #2d2d2d"))
    }

    @Test
    fun testAsciidocDarkHasDarkBackground() {
        assertTrue(StyleSheets.ASCIIDOC_STYLES_DARK.contains("background-color: #2d2d2d"))
    }

    @Test
    fun testAsciidocStylesContainTableElements() {
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".asciidoc table"))
        assertTrue(StyleSheets.ASCIIDOC_STYLES_LIGHT.contains(".asciidoc th"))
    }

    // ==================== LaTeX styles light/dark ====================

    @Test
    fun testLatexStylesContainDocumentElements() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".document-title"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".document-author"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".document-date"))
    }

    @Test
    fun testLatexStylesContainSections() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".section"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".subsection"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".paragraph"))
    }

    @Test
    fun testLatexStylesContainMathElements() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".math"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".math-inline"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".math-display"))
    }

    @Test
    fun testLatexStylesContainEnvironment() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".environment"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".environment-title"))
    }

    @Test
    fun testLatexStylesContainListElements() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".itemize"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".enumerate"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".item"))
    }

    @Test
    fun testLatexStylesContainInlineFormatting() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".bold"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".italic"))
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains(".underline"))
    }

    @Test
    fun testLatexLightHasSerifFont() {
        assertTrue(StyleSheets.LATEX_STYLES_LIGHT.contains("font-family: serif"))
    }

    @Test
    fun testLatexDarkHasDarkBackground() {
        assertTrue(StyleSheets.LATEX_STYLES_DARK.contains("background-color: #2d2d2d"))
    }

    // ==================== getStyleSheet edge cases ====================

    @Test
    fun testGetStyleSheetMarkdownLightAndDarkReturnSame() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, false)
        assertEquals(light, dark)
    }

    @Test
    fun testGetStyleSheetWikitextLightAndDarkReturnSame() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, false)
        assertEquals(light, dark)
    }

    @Test
    fun testGetStyleSheetUnknownReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet("xyz", true))
        assertEquals("", StyleSheets.getStyleSheet("xyz", false))
    }

    @Test
    fun testGetStyleSheetEmptyIdReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet("", true))
    }

    @Test
    fun testGetStyleSheetPlaintextReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet(TextFormat.ID_PLAINTEXT, true))
    }

    @Test
    fun testGetStyleSheetCsvReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet(TextFormat.ID_CSV, true))
    }

    @Test
    fun testGetStyleSheetTodotxtReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet(TextFormat.ID_TODOTXT, true))
    }

    @Test
    fun testGetStyleSheetBinaryReturnsEmpty() {
        assertEquals("", StyleSheets.getStyleSheet(TextFormat.ID_BINARY, true))
    }

    @Test
    fun testGetStyleSheetAllThemeFormatsReturnNonEmpty() {
        val themedFormats = listOf(
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )
        themedFormats.forEach { formatId ->
            val light = StyleSheets.getStyleSheet(formatId, true)
            val dark = StyleSheets.getStyleSheet(formatId, false)
            assertTrue(light.isNotEmpty(), "$formatId light should not be empty")
            assertTrue(dark.isNotEmpty(), "$formatId dark should not be empty")
            assertFalse(light == dark, "$formatId light and dark should differ")
        }
    }

    // ==================== CSS validity checks ====================

    @Test
    fun testAllStylesStartWithStyleTag() {
        val allStyles = listOf(
            StyleSheets.MARKDOWN_STYLES,
            StyleSheets.WIKITEXT_STYLES,
            StyleSheets.RST_STYLES_LIGHT,
            StyleSheets.RST_STYLES_DARK,
            StyleSheets.ORGMODE_STYLES_LIGHT,
            StyleSheets.ORGMODE_STYLES_DARK,
            StyleSheets.ASCIIDOC_STYLES_LIGHT,
            StyleSheets.ASCIIDOC_STYLES_DARK,
            StyleSheets.LATEX_STYLES_LIGHT,
            StyleSheets.LATEX_STYLES_DARK
        )
        allStyles.forEach { style ->
            assertTrue(style.trimStart().startsWith("<style>"), "Style should start with <style> tag")
            assertTrue(style.trimEnd().endsWith("</style>"), "Style should end with </style> tag")
        }
    }

    @Test
    fun testAllStylesContainFontFamily() {
        val allStyles = listOf(
            StyleSheets.MARKDOWN_STYLES,
            StyleSheets.WIKITEXT_STYLES,
            StyleSheets.RST_STYLES_LIGHT,
            StyleSheets.ORGMODE_STYLES_LIGHT,
            StyleSheets.ASCIIDOC_STYLES_LIGHT,
            StyleSheets.LATEX_STYLES_LIGHT
        )
        allStyles.forEach { style ->
            assertTrue(style.contains("font-family"), "Style should contain font-family declaration")
        }
    }

    @Test
    fun testAllStylesContainLineHeight() {
        val allStyles = listOf(
            StyleSheets.MARKDOWN_STYLES,
            StyleSheets.WIKITEXT_STYLES,
            StyleSheets.RST_STYLES_LIGHT,
            StyleSheets.ORGMODE_STYLES_LIGHT,
            StyleSheets.ASCIIDOC_STYLES_LIGHT,
            StyleSheets.LATEX_STYLES_LIGHT
        )
        allStyles.forEach { style ->
            assertTrue(style.contains("line-height"), "Style should contain line-height declaration")
        }
    }
}
