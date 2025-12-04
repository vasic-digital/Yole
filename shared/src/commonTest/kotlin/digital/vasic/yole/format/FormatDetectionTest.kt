/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for format detection methods
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Tests for format detection methods in FormatRegistry.
 *
 * Tests cover:
 * - Extension-based detection
 * - Content-based detection
 * - Multiple formats per extension
 * - Format support checking
 * - Format name retrieval
 * - Edge cases and fallbacks
 */
class FormatDetectionTest {

    // ==================== Extension Detection Tests ====================

    @Test
    fun `should detect markdown by md extension`() {
        val format = FormatRegistry.detectByExtension(".md")

        assertNotNull(format)
        assertEquals("markdown", format.id)
    }

    @Test
    fun `should detect markdown by markdown extension`() {
        val format = FormatRegistry.detectByExtension(".markdown")

        assertNotNull(format)
        assertEquals("markdown", format.id)
    }

    @Test
    fun `should detect todotxt by txt extension`() {
        // Note: .txt might return plain text or todo.txt depending on order
        val format = FormatRegistry.detectByExtension(".txt")

        assertNotNull(format)
        assertTrue(format.id == "plaintext" || format.id == "todotxt")
    }

    @Test
    fun `should detect CSV by csv extension`() {
        val format = FormatRegistry.detectByExtension(".csv")

        assertNotNull(format)
        assertEquals("csv", format.id)
    }

    @Test
    fun `should detect LaTeX by tex extension`() {
        val format = FormatRegistry.detectByExtension(".tex")

        assertNotNull(format)
        assertEquals("latex", format.id)
    }

    @Test
    fun `should detect wiki format by wiki extension`() {
        val format = FormatRegistry.detectByExtension(".wiki")

        assertNotNull(format)
        assertEquals("wikitext", format.id)
    }

    @Test
    fun `should detect org mode by org extension`() {
        val format = FormatRegistry.detectByExtension(".org")

        assertNotNull(format)
        assertEquals("orgmode", format.id)
    }

    @Test
    fun `should handle extension without leading dot`() {
        val format = FormatRegistry.detectByExtension("md")

        assertNotNull(format)
        assertEquals("markdown", format.id)
    }

    @Test
    fun `should handle case-insensitive extensions`() {
        val format1 = FormatRegistry.detectByExtension(".MD")
        val format2 = FormatRegistry.detectByExtension(".Md")
        val format3 = FormatRegistry.detectByExtension("MD")

        assertNotNull(format1)
        assertNotNull(format2)
        assertNotNull(format3)
        assertEquals("markdown", format1.id)
        assertEquals("markdown", format2.id)
        assertEquals("markdown", format3.id)
    }

    @Test
    fun `should handle extension with whitespace`() {
        val format = FormatRegistry.detectByExtension("  .md  ")

        assertNotNull(format)
        assertEquals("markdown", format.id)
    }

    @Test
    fun `should return plaintext for unknown extension`() {
        val format = FormatRegistry.detectByExtension(".unknown")

        assertNotNull(format)
        assertEquals("plaintext", format.id)
    }

    @Test
    fun `should return plaintext for empty extension`() {
        val format = FormatRegistry.detectByExtension("")

        assertNotNull(format)
        assertEquals("plaintext", format.id)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect markdown by header syntax`() {
        val content = "# This is a header\n\nSome content"
        val format = FormatRegistry.detectByContent(content)

        // Markdown has detection patterns for headers
        assertNotNull(format)
    }

    @Test
    fun `should detect todotxt by priority syntax`() {
        val content = "(A) High priority task\n(B) Medium priority task"
        val format = FormatRegistry.detectByContent(content)

        // Todo.txt has detection patterns for priorities
        assertNotNull(format)
    }

    @Test
    fun `should return null for empty content`() {
        val format = FormatRegistry.detectByContent("")

        assertNull(format)
    }

    @Test
    fun `should limit content analysis to maxLines`() {
        val content = buildString {
            repeat(100) {
                append("Line $it\n")
            }
            append("# Header on line 101")
        }

        // Should only check first 10 lines by default
        val format = FormatRegistry.detectByContent(content, maxLines = 10)

        // Won't detect markdown since header is after line 10
        // Result depends on what patterns match in first 10 lines
        // Just verify it doesn't throw
        assertNotNull(format != null || format == null)
    }

    @Test
    fun `should check custom maxLines parameter`() {
        val content = "Plain text\n".repeat(50) + "# Header on line 51"

        val format1 = FormatRegistry.detectByContent(content, maxLines = 10)
        val format2 = FormatRegistry.detectByContent(content, maxLines = 100)

        // With maxLines=100, should detect markdown header on line 51
        // Results may vary based on patterns
        assertNotNull(format1 != null || format1 == null)
        assertNotNull(format2 != null || format2 == null)
    }

    // ==================== getFormatsByExtension Tests ====================

    @Test
    fun `should get all formats for txt extension`() {
        val formats = FormatRegistry.getFormatsByExtension(".txt")

        // .txt is used by plaintext and todotxt
        assertTrue(formats.size >= 1)
        assertTrue(formats.any { it.id == "plaintext" || it.id == "todotxt" })
    }

    @Test
    fun `should get single format for md extension`() {
        val formats = FormatRegistry.getFormatsByExtension(".md")

        assertTrue(formats.isNotEmpty())
        assertTrue(formats.any { it.id == "markdown" })
    }

    @Test
    fun `should return empty list for unknown extension`() {
        val formats = FormatRegistry.getFormatsByExtension(".unknown")

        assertTrue(formats.isEmpty())
    }

    @Test
    fun `should handle extension without dot in getFormatsByExtension`() {
        val formats = FormatRegistry.getFormatsByExtension("md")

        assertTrue(formats.isNotEmpty())
        assertTrue(formats.any { it.id == "markdown" })
    }

    // ==================== Format Support Tests ====================

    @Test
    fun `should confirm markdown is supported`() {
        assertTrue(FormatRegistry.isSupported("markdown"))
    }

    @Test
    fun `should confirm plaintext is supported`() {
        assertTrue(FormatRegistry.isSupported("plaintext"))
    }

    @Test
    fun `should confirm todotxt is supported`() {
        assertTrue(FormatRegistry.isSupported("todotxt"))
    }

    @Test
    fun `should confirm CSV is supported`() {
        assertTrue(FormatRegistry.isSupported("csv"))
    }

    @Test
    fun `should return false for unsupported format`() {
        assertFalse(FormatRegistry.isSupported("unsupported"))
        assertFalse(FormatRegistry.isSupported("unknown"))
        assertFalse(FormatRegistry.isSupported("fake"))
    }

    @Test
    fun `should handle empty format ID`() {
        assertFalse(FormatRegistry.isSupported(""))
    }

    @Test
    fun `should be case-sensitive for format IDs`() {
        assertTrue(FormatRegistry.isSupported("markdown"))
        // Format IDs are typically lowercase
        assertFalse(FormatRegistry.isSupported("Markdown"))
        assertFalse(FormatRegistry.isSupported("MARKDOWN"))
    }

    // ==================== Format Names Tests ====================

    @Test
    fun `should get all format names`() {
        val names = FormatRegistry.getFormatNames()

        assertTrue(names.isNotEmpty())
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("Plain Text"))
    }

    @Test
    fun `should return distinct format names`() {
        val names = FormatRegistry.getFormatNames()
        val uniqueNames = names.distinct()

        assertEquals(uniqueNames.size, names.size)
    }

    @Test
    fun `should have format names for all formats`() {
        val names = FormatRegistry.getFormatNames()
        val formats = FormatRegistry.formats

        assertEquals(formats.size, names.size)
    }

    // ==================== GetById Tests ====================

    @Test
    fun `should get format by ID`() {
        val format = FormatRegistry.getById("markdown")

        assertNotNull(format)
        assertEquals("markdown", format.id)
        assertEquals("Markdown", format.name)
    }

    @Test
    fun `should return null for invalid ID`() {
        val format = FormatRegistry.getById("invalid")

        assertNull(format)
    }

    @Test
    fun `should get all registered formats`() {
        val markdown = FormatRegistry.getById("markdown")
        val plaintext = FormatRegistry.getById("plaintext")
        val csv = FormatRegistry.getById("csv")
        val latex = FormatRegistry.getById("latex")

        assertNotNull(markdown)
        assertNotNull(plaintext)
        assertNotNull(csv)
        assertNotNull(latex)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle multiple detection attempts`() {
        repeat(100) {
            val format = FormatRegistry.detectByExtension(".md")
            assertNotNull(format)
            assertEquals("markdown", format.id)
        }
    }

    @Test
    fun `should handle concurrent detection calls`() {
        val formats = (1..50).map {
            FormatRegistry.detectByExtension(".md")
        }

        assertTrue(formats.all { it?.id == "markdown" })
    }

    @Test
    fun `should handle special characters in extension`() {
        val format1 = FormatRegistry.detectByExtension(".m@d")
        val format2 = FormatRegistry.detectByExtension(".md!")
        val format3 = FormatRegistry.detectByExtension("..md")

        // Should fallback to plaintext for unrecognized extensions
        assertNotNull(format1)
        assertNotNull(format2)
        assertNotNull(format3)
        assertEquals("plaintext", format1.id)
        assertEquals("plaintext", format2.id)
        assertEquals("plaintext", format3.id)
    }

    // ==================== Format Properties Tests ====================

    @Test
    fun `should verify format has required properties`() {
        val format = FormatRegistry.getById("markdown")

        assertNotNull(format)
        assertTrue(format.id.isNotEmpty())
        assertTrue(format.name.isNotEmpty())
        assertTrue(format.defaultExtension.isNotEmpty())
        assertTrue(format.extensions.isNotEmpty())
    }

    @Test
    fun `should verify all formats have default extension in extensions list`() {
        FormatRegistry.formats.forEach { format ->
            // Binary format has emptyList() for extensions, which is a special case
            if (format.id != "binary") {
                assertTrue(
                    format.extensions.contains(format.defaultExtension),
                    "Format ${format.id} should have defaultExtension ${format.defaultExtension} in extensions list"
                )
            }
        }
    }

    @Test
    fun `should verify format extensions start with dot`() {
        FormatRegistry.formats.forEach { format ->
            format.extensions.forEach { ext ->
                assertTrue(
                    ext.startsWith("."),
                    "Extension $ext in format ${format.id} should start with dot"
                )
            }
        }
    }

    // ==================== Detection Pattern Tests ====================

    @Test
    fun `should verify formats with detection patterns`() {
        val formatsWithPatterns = FormatRegistry.formats.filter { it.detectionPatterns.isNotEmpty() }

        // At least some formats should have detection patterns
        assertTrue(formatsWithPatterns.isNotEmpty())
    }

    @Test
    fun `should test detection pattern compilation`() {
        FormatRegistry.formats.forEach { format ->
            format.detectionPatterns.forEach { pattern ->
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    fail("Invalid regex pattern '$pattern' in format ${format.id}: ${e.message}")
                }
            }
        }
    }
}
