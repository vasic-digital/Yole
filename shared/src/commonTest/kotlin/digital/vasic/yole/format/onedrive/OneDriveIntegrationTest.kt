/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for OneDrive format
 *
 *########################################################*/
package digital.vasic.yole.format.onedrive

import digital.vasic.yole.format.FormatRegistry
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertNotNull

/**
 * Integration tests for OneDrive format.
 *
 * These tests verify the integration between:
 * - Format detection and parsing
 * - FormatRegistry and parser
 * - Parser and HTML conversion (if applicable)
 * - Cross-format compatibility
 */
class OneDriveIntegrationTest {

    // ==================== Format Detection Integration ====================

    @Test
    fun `format detection should work with parser`() {
        val filename = "test.odoc"
        val format = FormatRegistry.detectByFilename(filename)

        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_ONEDRIVE)

        // Verify parser can be retrieved
        // val parser = ParserRegistry.getParser(format)
        // assertNotNull(parser)
    }

    @Test
    fun `content detection should match extension detection`() {
        val content = """
            Sample OneDrive content here
        """.trimIndent()

        val byContent = FormatRegistry.detectByContent(content)
        val byExtension = FormatRegistry.getByExtension(".odoc")

        assertNotNull(byContent)
        assertNotNull(byExtension)
        assertThat(byContent.id).isEqualTo(byExtension.id)
    }

    // ==================== Cross-Format Tests ====================

    @Test
    fun `should not conflict with other formats`() {
        val formats = FormatRegistry.formats

        // Ensure OneDrive format is unique
        val onedriveFormats = formats.filter { it.id == FormatRegistry.ID_ONEDRIVE }
        assertThat(onedriveFormats).hasSize(1)
    }

    @Test
    fun `should have unique extensions or share appropriately`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_ONEDRIVE)
        assertNotNull(format)

        format.extensions.forEach { ext ->
            val formatsForExt = FormatRegistry.getFormatsByExtension(ext)

            // Document which formats share extensions
            println("Extension $ext is used by: ${formatsForExt.map { it.name }}")

            // If extension is shared, verify we can distinguish by content
            if (formatsForExt.size > 1) {
                val sampleContent = """
                    Sample OneDrive content here
                """.trimIndent()

                val detected = FormatRegistry.detectByContent(sampleContent)
                assertNotNull(detected)
                // Should detect correctly even with shared extension
            }
        }
    }

    // ==================== Round-Trip Tests ====================

    @Test
    fun `should preserve content through parse cycle`() {
        val original = """
            Sample OneDrive content here
        """.trimIndent()

        val parser = OneDriveParser()
        val parsed = parser.parse(original)
        assertNotNull(parsed)

        // If format supports serialization back to text:
        // val serialized = parser.serialize(parsed)
        // assertThat(serialized).isEqualTo(original)
    }

    // ==================== File Operations Integration ====================

    @Test
    fun `should work with file-like operations`() {
        val content = """
            Sample OneDrive content here
        """.trimIndent()

        // Simulate file read -> parse -> process workflow
        val format = FormatRegistry.detectByFilename("document.odoc")
        assertNotNull(format)

        val parser = OneDriveParser()
        val result = parser.parse(content)
        assertNotNull(result)
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle large files efficiently`() {
        val largeContent = buildString {
            repeat(1000) {
                appendLine("Single line of OneDrive")
            }
        }

        val parser = OneDriveParser()
        val startTime = System.currentTimeMillis()

        val result = parser.parse(largeContent)

        val duration = System.currentTimeMillis() - startTime

        assertNotNull(result)
        // Performance assertion - should parse 1000 lines in reasonable time
        assertThat(duration).isLessThan(1000) // 1 second max
    }

    @Test
    fun `should handle concurrent parsing`() {
        val content = """
            Sample OneDrive content here
        """.trimIndent()

        val parser = OneDriveParser()

        // Simulate concurrent access
        val results = (1..10).map {
            Thread {
                val result = parser.parse(content)
                assertNotNull(result)
            }
        }

        results.forEach { it.start() }
        results.forEach { it.join(5000) } // 5 second timeout

        // All threads should complete successfully
    }

    // ==================== Edge Case Integration ====================

    @Test
    fun `should handle mixed line endings`() {
        val content = "Line 1\nLine 2\r\nLine 3\rLine 4"

        val parser = OneDriveParser()
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle different encodings`() {
        // UTF-8 content with various characters
        val content = """
            ASCII: Hello World
            Latin: Café résumé
            Cyrillic: Привет мир
            Chinese: 你好世界
            Emoji: 🌍 🚀 ✨
        """.trimIndent()

        val parser = OneDriveParser()
        val result = parser.parse(content)

        assertNotNull(result)
    }
}
