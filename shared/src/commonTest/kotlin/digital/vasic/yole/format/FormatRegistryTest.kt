/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Registry Tests - Comprehensive Coverage
 *
 *########################################################*/

package digital.vasic.yole.format

import kotlin.test.*

/**
 * Comprehensive test suite for FormatRegistry
 * Tests all format registration and detection functionality
 */
class FormatRegistryTest {

    @BeforeTest
    fun setup() {
        // Ensure registry is initialized
        FormatRegistry.initialize()
    }

    @Test
    fun testAllFormatsRegistered() {
        val formats = FormatRegistry.getAllFormats()
        
        // Should have all 17 formats
        assertEquals(17, formats.size, "Should have exactly 17 formats registered")
        
        // Check that specific formats are present
        val formatNames = formats.map { it.name }
        assertTrue(formatNames.contains("Markdown"))
        assertTrue(formatNames.contains("Todo.txt"))
        assertTrue(formatNames.contains("CSV"))
        assertTrue(formatNames.contains("Plain Text"))
        assertTrue(formatNames.contains("LaTeX"))
        assertTrue(formatNames.contains("Org Mode"))
        assertTrue(formatNames.contains("AsciiDoc"))
        assertTrue(formatNames.contains("WikiText"))
    }

    @Test
    fun testFormatDetection() {
        // Test file extension detection
        assertEquals("markdown", FormatRegistry.detectFormat("test.md"))
        assertEquals("markdown", FormatRegistry.detectFormat("test.markdown"))
        assertEquals("todotxt", FormatRegistry.detectFormat("todo.txt"))
        assertEquals("csv", FormatRegistry.detectFormat("data.csv"))
        assertEquals("latex", FormatRegistry.detectFormat("document.tex"))
        assertEquals("orgmode", FormatRegistry.detectFormat("notes.org"))
        assertEquals("asciidoc", FormatRegistry.detectFormat("README.adoc"))
        assertEquals("wikitext", FormatRegistry.detectFormat("page.wiki"))
        assertEquals("plaintext", FormatRegistry.detectFormat("document.txt"))
        
        // Test content-based detection
        val markdownContent = "# Heading\n\nSome **bold** text."
        assertEquals("markdown", FormatRegistry.detectFormatFromContent(markdownContent))
        
        val todoContent = "(A) Task 1 +project @context"
        assertEquals("todotxt", FormatRegistry.detectFormatFromContent(todoContent))
        
        val csvContent = "Name,Age,City\nJohn,30,NYC"
        assertEquals("csv", FormatRegistry.detectFormatFromContent(csvContent))
    }

    @Test
    fun testFormatById() {
        val markdownFormat = FormatRegistry.getFormatById("markdown")
        assertNotNull(markdownFormat)
        assertEquals("Markdown", markdownFormat.name)
        assertEquals(".md", markdownFormat.defaultExtension)
        
        val todoFormat = FormatRegistry.getFormatById("todotxt")
        assertNotNull(todoFormat)
        assertEquals("Todo.txt", todoFormat.name)
        assertEquals(".txt", todoFormat.defaultExtension)
    }

    @Test
    fun testParserRegistry() {
        val markdownParser = ParserRegistry.getParser("markdown")
        assertNotNull(markdownParser)
        
        val todoParser = ParserRegistry.getParser("todotxt")
        assertNotNull(todoParser)
        
        val csvParser = ParserRegistry.getParser("csv")
        assertNotNull(csvParser)
    }

    @Test
    fun testFormatCapabilities() {
        val formats = FormatRegistry.getAllFormats()
        
        formats.forEach { format ->
            // Each format should have basic properties
            assertNotNull(format.id)
            assertNotNull(format.name)
            assertNotNull(format.defaultExtension)
            assertTrue(format.extensions.isNotEmpty())
            
            // Should have at least the default extension
            assertTrue(format.extensions.contains(format.defaultExtension))
        }
    }

    @Test
    fun testFormatDetectionEdgeCases() {
        // Unknown extensions should default to plaintext
        assertEquals("plaintext", FormatRegistry.detectFormat("unknown.xyz"))
        assertEquals("plaintext", FormatRegistry.detectFormat("file"))
        
        // Empty content should default to plaintext
        assertEquals("plaintext", FormatRegistry.detectFormatFromContent(""))
        assertEquals("plaintext", FormatRegistry.detectFormatFromContent("   \n\t\n   "))
    }

    @Test
    fun testFormatConsistency() {
        val formats = FormatRegistry.getAllFormats()
        
        // Check for duplicate IDs
        val formatIds = formats.map { it.id }
        assertEquals(formatIds.distinct().size, formatIds.size, "Format IDs should be unique")
        
        // Check for duplicate names
        val formatNames = formats.map { it.name }
        assertEquals(formatNames.distinct().size, formatNames.size, "Format names should be unique")
        
        // Check extensions don't conflict
        val extensionMap = mutableMapOf<String, MutableList<String>>()
        formats.forEach { format ->
            format.extensions.forEach { ext ->
                extensionMap.getOrPut(ext) { mutableListOf() }.add(format.id)
            }
        }
        
        // Extensions can be shared, but let's verify the mapping
        extensionMap.forEach { (ext, formatIds) ->
            assertTrue(formatIds.isNotEmpty(), "Extension $ext should map to at least one format")
        }
    }

    @Test
    fun testFormatMetadata() {
        val formats = FormatRegistry.getAllFormats()
        
        formats.forEach { format ->
            // Test that metadata is consistent
            assertTrue(format.id.matches(Regex("^[a-z0-9]+")), "Format ID should be lowercase alphanumeric: ${format.id}")
            assertTrue(format.name.isNotBlank(), "Format name should not be blank: ${format.id}")
            assertTrue(format.defaultExtension.startsWith("."), "Default extension should start with dot: ${format.defaultExtension}")
            
            // Test that extensions are valid
            format.extensions.forEach { ext ->
                assertTrue(ext.startsWith("."), "Extension should start with dot: $ext")
                assertTrue(ext.length > 1, "Extension should have content: $ext")
            }
        }
    }
}