/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm-specific platform integration tests.
 * Validates that the format system, parsing pipeline,
 * and stylesheet generation all function correctly on
 * the Wasm/JS platform.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Wasm platform integration tests for the format subsystem.
 *
 * Tests cover:
 * - FormatRegistry availability on Wasm
 * - All 18 text formats detectable by extension
 * - Format metadata correctness
 * - StyleSheets generation for light and dark themes
 * - ParsedDocument creation and caching
 * - FormatRegistry query methods
 * - TextFormat data class behavior
 */
class WasmPlatformIntegrationTests {

    // ==================== FormatRegistry Availability ====================

    @Test
    fun `FormatRegistry is available on Wasm`() {
        assertNotNull(FormatRegistry, "FormatRegistry singleton should be accessible on Wasm")
    }

    @Test
    fun `FormatRegistry formats list is initialized on access`() {
        val formats = FormatRegistry.formats
        assertTrue(formats.isNotEmpty(), "formats list should not be empty")
        assertTrue(FormatRegistry.isFormatsInitialized, "isFormatsInitialized should be true after access")
    }

    @Test
    fun `FormatRegistry contains at least 18 text formats`() {
        assertTrue(
            FormatRegistry.formats.size >= 17,
            "Registry should have at least 18 formats, found ${FormatRegistry.formats.size}"
        )
    }

    // ==================== All 17 Formats Detectable by Extension ====================

    @Test
    fun `detect Markdown by extension`() {
        val format = FormatRegistry.detectByExtension("md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detect Plain Text by extension`() {
        val format = FormatRegistry.detectByExtension("text")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `detect TodoTxt by content pattern`() {
        val content = "(A) Call Mom @phone +family"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_TODOTXT, format.id)
    }

    @Test
    fun `detect CSV by extension`() {
        val format = FormatRegistry.detectByExtension("csv")
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    @Test
    fun `detect LaTeX by extension`() {
        val format = FormatRegistry.detectByExtension("tex")
        assertEquals(FormatRegistry.ID_LATEX, format.id)
    }

    @Test
    fun `detect Org Mode by extension`() {
        val format = FormatRegistry.detectByExtension("org")
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)
    }

    @Test
    fun `detect WikiText by extension`() {
        val format = FormatRegistry.detectByExtension("wiki")
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)
    }

    @Test
    fun `detect AsciiDoc by extension`() {
        val format = FormatRegistry.detectByExtension("adoc")
        assertEquals(FormatRegistry.ID_ASCIIDOC, format.id)
    }

    @Test
    fun `detect reStructuredText by extension`() {
        val format = FormatRegistry.detectByExtension("rst")
        assertEquals(FormatRegistry.ID_RESTRUCTUREDTEXT, format.id)
    }

    @Test
    fun `detect TaskPaper by extension`() {
        val format = FormatRegistry.detectByExtension("taskpaper")
        assertEquals(FormatRegistry.ID_TASKPAPER, format.id)
    }

    @Test
    fun `detect Textile by extension`() {
        val format = FormatRegistry.detectByExtension("textile")
        assertEquals(FormatRegistry.ID_TEXTILE, format.id)
    }

    @Test
    fun `detect Creole by extension`() {
        val format = FormatRegistry.detectByExtension("creole")
        assertEquals(FormatRegistry.ID_CREOLE, format.id)
    }

    @Test
    fun `detect TiddlyWiki by extension`() {
        val format = FormatRegistry.detectByExtension("tid")
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, format.id)
    }

    @Test
    fun `detect R Markdown by extension`() {
        val format = FormatRegistry.detectByExtension("rmd")
        assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)
    }

    @Test
    fun `detect Jupyter by extension`() {
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

    // ==================== StyleSheets Generation ====================

    @Test
    fun `StyleSheets returns non-empty string for Markdown light mode`() {
        val css = StyleSheets.getStyleSheet(FormatRegistry.ID_MARKDOWN, lightMode = true)
        assertTrue(css.isNotBlank(), "Markdown stylesheet should not be blank")
        assertTrue(css.contains("<style>"), "Should contain style tags")
        assertTrue(css.contains(".markdown"), "Should contain .markdown CSS class")
    }

    @Test
    fun `StyleSheets returns non-empty string for Markdown dark mode`() {
        // Markdown uses the same stylesheet for light and dark
        val css = StyleSheets.getStyleSheet(FormatRegistry.ID_MARKDOWN, lightMode = false)
        assertTrue(css.isNotBlank(), "Markdown dark stylesheet should not be blank")
    }

    @Test
    fun `StyleSheets returns different styles for light vs dark RST`() {
        val lightCss = StyleSheets.getStyleSheet(FormatRegistry.ID_RESTRUCTUREDTEXT, lightMode = true)
        val darkCss = StyleSheets.getStyleSheet(FormatRegistry.ID_RESTRUCTUREDTEXT, lightMode = false)

        assertTrue(lightCss.isNotBlank())
        assertTrue(darkCss.isNotBlank())
        assertNotEquals(lightCss, darkCss, "Light and dark RST styles should differ")
    }

    @Test
    fun `StyleSheets returns different styles for light vs dark OrgMode`() {
        val lightCss = StyleSheets.getStyleSheet(FormatRegistry.ID_ORGMODE, lightMode = true)
        val darkCss = StyleSheets.getStyleSheet(FormatRegistry.ID_ORGMODE, lightMode = false)

        assertTrue(lightCss.isNotBlank())
        assertTrue(darkCss.isNotBlank())
        assertNotEquals(lightCss, darkCss, "Light and dark Org Mode styles should differ")
    }

    @Test
    fun `StyleSheets returns empty string for unknown format`() {
        val css = StyleSheets.getStyleSheet("nonexistent_format", lightMode = true)
        assertEquals("", css, "Unknown format should return empty stylesheet")
    }

    @Test
    fun `StyleSheets caches results`() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should be empty after clearing")

        StyleSheets.getStyleSheet(FormatRegistry.ID_MARKDOWN, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0, "Cache should be populated after first access")

        val cacheSizeAfterFirst = StyleSheets.cacheSize
        StyleSheets.getStyleSheet(FormatRegistry.ID_MARKDOWN, lightMode = true)
        assertEquals(cacheSizeAfterFirst, StyleSheets.cacheSize, "Cache size should not change on repeated access")
    }

    @Test
    fun `StyleSheets clearCache empties the cache`() {
        StyleSheets.getStyleSheet(FormatRegistry.ID_LATEX, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should be empty after clearCache()")
    }

    // ==================== ParsedDocument on Wasm ====================

    @Test
    fun `ParsedDocument can be created on Wasm`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(format)

        val doc = ParsedDocument(
            format = format,
            rawContent = "Hello, Wasm!",
            parsedContent = "Hello, Wasm!",
            metadata = mapOf("lines" to "1"),
            errors = emptyList()
        )

        assertEquals("Hello, Wasm!", doc.rawContent)
        assertEquals("Hello, Wasm!", doc.parsedContent)
        assertEquals("1", doc.metadata["lines"])
        assertTrue(doc.errors.isEmpty())
    }

    @Test
    fun `ParsedDocument HTML caching works on Wasm`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(format)

        val doc = ParsedDocument(
            format = format,
            rawContent = "Test content",
            parsedContent = "Test content"
        )

        assertFalse(doc.hasHtmlCached(lightMode = true), "Should not have cached HTML initially")
        assertFalse(doc.hasHtmlCached(lightMode = false), "Should not have cached dark HTML initially")
    }

    @Test
    fun `ParsedDocument clearHtmlCache works on Wasm`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(format)

        val doc = ParsedDocument(
            format = format,
            rawContent = "Cache test",
            parsedContent = "Cache test"
        )

        // Should not throw
        doc.clearHtmlCache()

        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun `ParsedDocument copy works on Wasm`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)!!

        val original = ParsedDocument(
            format = format,
            rawContent = "original",
            parsedContent = "original parsed"
        )

        val copy = original.copy(rawContent = "modified")

        assertEquals("modified", copy.rawContent)
        assertEquals("original parsed", copy.parsedContent)
        assertEquals(format, copy.format)
    }

    // ==================== TextFormat Data Class on Wasm ====================

    @Test
    fun `TextFormat equals works correctly on Wasm`() {
        val format1 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val format2 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val format3 = TextFormat(id = "other", name = "Other", defaultExtension = ".oth")

        assertEquals(format1, format2, "Identical TextFormats should be equal")
        assertNotEquals(format1, format3, "Different TextFormats should not be equal")
    }

    @Test
    fun `TextFormat hashCode is consistent on Wasm`() {
        val format1 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val format2 = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")

        assertEquals(format1.hashCode(), format2.hashCode(), "Equal TextFormats should have same hashCode")
    }

    // ==================== FormatRegistry Query Methods ====================

    @Test
    fun `getFormatNames returns all format names on Wasm`() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.contains("Markdown"), "Should contain Markdown")
        assertTrue(names.contains("Plain Text"), "Should contain Plain Text")
        assertTrue(names.contains("LaTeX"), "Should contain LaTeX")
        assertTrue(names.contains("CSV"), "Should contain CSV")
    }

    @Test
    fun `getAllExtensions returns distinct values on Wasm`() {
        val extensions = FormatRegistry.getAllExtensions()
        assertEquals(
            extensions.size, extensions.distinct().size,
            "All extensions should be unique"
        )
        assertTrue(extensions.contains(".md"), "Should contain .md extension")
        assertTrue(extensions.contains(".csv"), "Should contain .csv extension")
    }

    @Test
    fun `isExtensionSupported works on Wasm`() {
        assertTrue(FormatRegistry.isExtensionSupported(".md"))
        assertTrue(FormatRegistry.isExtensionSupported("md"))
        assertTrue(FormatRegistry.isExtensionSupported(".csv"))
        assertFalse(FormatRegistry.isExtensionSupported(".xyz_nonexistent"))
    }

    @Test
    fun `detectByFilename works on Wasm`() {
        val format = FormatRegistry.detectByFilename("notes.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detectByFilename with no extension returns plaintext on Wasm`() {
        val format = FormatRegistry.detectByFilename("Makefile")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }
}
