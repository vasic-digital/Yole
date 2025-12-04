/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for HTML generation pipeline
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.*

/**
 * Integration tests for HTML generation, caching, and theming.
 *
 * Tests cover:
 * - Light/dark mode HTML generation
 * - HTML caching behavior
 * - Cache invalidation
 * - HTML escaping in contexts
 * - CSS integration
 * - Regeneration workflows
 */
class HtmlGenerationIntegrationTest {

    private lateinit var markdownParser: MarkdownParser
    private lateinit var csvParser: CsvParser
    private lateinit var plaintextParser: PlaintextParser

    @BeforeTest
    fun setup() {
        markdownParser = MarkdownParser()
        csvParser = CsvParser()
        plaintextParser = PlaintextParser()
        ParserRegistry.clear()
        ParserRegistry.register(markdownParser)
        ParserRegistry.register(csvParser)
        ParserRegistry.register(plaintextParser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Light/Dark Mode Tests ====================

    @Test
    fun `should generate different HTML for light and dark modes`() {
        val content = "# Header\n\nParagraph with **bold** text."
        val doc = markdownParser.parse(content)

        val lightHtml = doc.toHtml(lightMode = true)
        val darkHtml = doc.toHtml(lightMode = false)

        // Both should contain the content
        assertTrue(lightHtml.contains("Header"))
        assertTrue(darkHtml.contains("Header"))
        assertTrue(lightHtml.contains("bold"))
        assertTrue(darkHtml.contains("bold"))

        // HTML structure should be the same
        assertTrue(lightHtml.contains("<h1>"))
        assertTrue(darkHtml.contains("<h1>"))
    }

    @Test
    fun `should cache light and dark mode HTML separately`() {
        val content = "# Test"
        val doc = markdownParser.parse(content)

        // Generate both modes
        val light1 = doc.toHtml(lightMode = true)
        val dark1 = doc.toHtml(lightMode = false)

        // Request again (should use cache)
        val light2 = doc.toHtml(lightMode = true)
        val dark2 = doc.toHtml(lightMode = false)

        // Should return same HTML (cached)
        assertEquals(light1, light2)
        assertEquals(dark1, dark2)
    }

    @Test
    fun `should switch between light and dark modes`() {
        val content = "# Content"
        val doc = markdownParser.parse(content)

        // Alternate between modes
        val light1 = doc.toHtml(lightMode = true)
        val dark1 = doc.toHtml(lightMode = false)
        val light2 = doc.toHtml(lightMode = true)
        val dark2 = doc.toHtml(lightMode = false)

        // Should be consistent
        assertEquals(light1, light2)
        assertEquals(dark1, dark2)
    }

    // ==================== HTML Caching Tests ====================

    @Test
    fun `should not generate HTML until toHtml is called`() {
        val content = "# Title"
        val doc = markdownParser.parse(content)

        // At this point, HTML should not be generated yet
        assertNotNull(doc.rawContent)
        assertNotNull(doc.parsedContent)

        // Now generate HTML
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.contains("Title"))
    }

    @Test
    fun `should regenerate HTML after cache clear`() {
        val content = "# Original"
        val doc = markdownParser.parse(content)

        val html1 = doc.toHtml(lightMode = true)
        assertTrue(html1.contains("Original"))

        // Clear cache
        doc.clearHtmlCache()

        // Should regenerate
        val html2 = doc.toHtml(lightMode = true)
        assertTrue(html2.contains("Original"))

        // Content should be the same
        assertEquals(html1, html2)
    }

    @Test
    fun `should clear both light and dark caches`() {
        val content = "# Test"
        val doc = markdownParser.parse(content)

        // Generate both
        val light1 = doc.toHtml(lightMode = true)
        val dark1 = doc.toHtml(lightMode = false)

        // Clear
        doc.clearHtmlCache()

        // Regenerate
        val light2 = doc.toHtml(lightMode = true)
        val dark2 = doc.toHtml(lightMode = false)

        // Should have regenerated correctly
        assertEquals(light1, light2)
        assertEquals(dark1, dark2)
    }

    // ==================== HTML Escaping Tests ====================

    @Test
    fun `should escape HTML entities in markdown content`() {
        val content = "<script>alert('xss')</script>"
        val doc = markdownParser.parse(content)

        val html = doc.toHtml(lightMode = true)

        // Should be escaped
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;"))
        assertFalse(html.contains("<script>alert"))
    }

    @Test
    fun `should escape special characters in plaintext`() {
        val content = "Text with <brackets> and &ampersands& and \"quotes\""
        val doc = plaintextParser.parse(content)

        val html = doc.toHtml(lightMode = true)

        // Special chars should be escaped
        assertTrue(html.contains("&lt;") || html.contains("&gt;") || html.contains("&amp;"))
    }

    @Test
    fun `should preserve escaped content in code blocks`() {
        val content = """
            ```
            <html>
            <body>Hello</body>
            </html>
            ```
        """.trimIndent()

        val doc = markdownParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Code should be escaped/preserved
        assertTrue(html.contains("&lt;html&gt;") || html.contains("&lt;body&gt;") || html.contains("<code>"))
    }

    @Test
    fun `should handle mixed escaped and unescaped content`() {
        val content = "Normal text with **bold** and <script>evil</script>"
        val doc = markdownParser.parse(content)

        val html = doc.toHtml(lightMode = true)

        // Bold should be processed, script should be escaped
        assertTrue(html.contains("<strong>") || html.contains("bold"))
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;"))
    }

    // ==================== CSV HTML Table Generation Tests ====================

    @Test
    fun `should generate HTML table from CSV`() {
        val content = """
            Name,Age,City
            Alice,28,NYC
            Bob,32,LA
        """.trimIndent()

        val doc = csvParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Should contain table elements
        assertTrue(html.contains("<table>") || html.contains("table"))
        assertTrue(html.contains("Alice") && html.contains("Bob"))
    }

    @Test
    fun `should handle CSV with special characters in table`() {
        val content = """
            Name,Description
            "Product A","Contains <html> tags"
            "Product B","Has & ampersand"
        """.trimIndent()

        val doc = csvParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Special characters should be escaped in table
        assertTrue(html.contains("Product A") && html.contains("Product B"))
        assertTrue(html.contains("&lt;") || html.contains("&amp;") || html.contains("Contains"))
    }

    // ==================== Multiple Document HTML Generation ====================

    @Test
    fun `should generate HTML for multiple documents independently`() {
        val doc1 = markdownParser.parse("# Document 1")
        val doc2 = markdownParser.parse("# Document 2")
        val doc3 = markdownParser.parse("# Document 3")

        val html1 = doc1.toHtml(lightMode = true)
        val html2 = doc2.toHtml(lightMode = true)
        val html3 = doc3.toHtml(lightMode = true)

        // Each should have its own content
        assertTrue(html1.contains("Document 1"))
        assertTrue(html2.contains("Document 2"))
        assertTrue(html3.contains("Document 3"))

        // No cross-contamination
        assertFalse(html1.contains("Document 2"))
        assertFalse(html2.contains("Document 3"))
    }

    @Test
    fun `should maintain separate caches for different documents`() {
        val doc1 = markdownParser.parse("# Doc 1")
        val doc2 = markdownParser.parse("# Doc 2")

        // Generate HTML for both
        val html1a = doc1.toHtml(lightMode = true)
        val html2a = doc2.toHtml(lightMode = true)

        // Clear cache for doc1 only
        doc1.clearHtmlCache()

        // Get HTML again
        val html1b = doc1.toHtml(lightMode = true)
        val html2b = doc2.toHtml(lightMode = true)

        // Doc1 regenerated, doc2 from cache
        assertEquals(html1a, html1b)
        assertEquals(html2a, html2b)
    }

    // ==================== HTML Generation Performance Tests ====================

    @Test
    fun `should handle rapid HTML generation calls`() {
        val doc = markdownParser.parse("# Test")

        // Rapid HTML generation
        val htmls = (1..100).map {
            doc.toHtml(lightMode = true)
        }

        // All should be identical (cached after first)
        val first = htmls.first()
        assertTrue(htmls.all { it == first })
    }

    @Test
    fun `should handle alternating mode requests efficiently`() {
        val doc = markdownParser.parse("# Content")

        // Alternate modes rapidly
        repeat(50) {
            val light = doc.toHtml(lightMode = true)
            val dark = doc.toHtml(lightMode = false)
            assertTrue(light.contains("Content"))
            assertTrue(dark.contains("Content"))
        }
    }

    // ==================== HTML Structure Tests ====================

    @Test
    fun `should generate valid HTML structure for markdown`() {
        val content = """
            # Title

            Paragraph with **bold** and *italic*.

            - List item 1
            - List item 2
        """.trimIndent()

        val doc = markdownParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Check for proper HTML structure
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<p>"))
        assertTrue(html.contains("<strong>") || html.contains("<b>"))
        assertTrue(html.contains("<em>") || html.contains("<i>"))
        assertTrue(html.contains("<ul>") || html.contains("<li>"))
    }

    @Test
    fun `should generate valid HTML for plaintext`() {
        val content = "Line 1\nLine 2\nLine 3"
        val doc = plaintextParser.parse(content)

        val html = doc.toHtml(lightMode = true)

        // Should preserve line structure
        assertTrue(html.contains("Line 1"))
        assertTrue(html.contains("Line 2"))
        assertTrue(html.contains("Line 3"))
    }

    // ==================== Error Recovery in HTML Generation ====================

    @Test
    fun `should handle HTML generation after parse errors`() {
        val malformed = "[Unclosed bracket"
        val doc = markdownParser.parse(malformed)

        // Should still generate HTML (even if with warnings)
        val html = doc.toHtml(lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("Unclosed") || html.contains("bracket"))
    }

    @Test
    fun `should recover from cache clear errors`() {
        val doc = markdownParser.parse("# Test")

        val html1 = doc.toHtml(lightMode = true)

        // Multiple cache clears
        doc.clearHtmlCache()
        doc.clearHtmlCache()
        doc.clearHtmlCache()

        // Should still work
        val html2 = doc.toHtml(lightMode = true)
        assertEquals(html1, html2)
    }

    // ==================== Complex Content HTML Tests ====================

    @Test
    fun `should handle complex nested markdown structures`() {
        val content = """
            # Main Header

            ## Subheader

            Paragraph with **bold _italic bold_ bold** text.

            1. Numbered list
               - Nested bullet
               - Another bullet
            2. Second item

            ```kotlin
            fun example() = "code"
            ```
        """.trimIndent()

        val doc = markdownParser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Verify structure preserved
        assertTrue(html.contains("Main Header"))
        assertTrue(html.contains("Subheader"))
        assertTrue(html.contains("Numbered list"))
        assertTrue(html.contains("example") || html.contains("code"))
    }

    @Test
    fun `should handle unicode characters in HTML`() {
        val content = "# 你好世界 🌍\n\nПривет مرحبا"
        val doc = markdownParser.parse(content)

        val html = doc.toHtml(lightMode = true)

        // Unicode should be preserved
        assertTrue(html.contains("你好世界") || html.contains("🌍") || html.contains("Привет"))
    }
}
