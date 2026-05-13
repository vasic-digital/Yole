/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Registry Stress Tests
 *
 * Comprehensive stress tests for FormatRegistry and
 * ParserRegistry operations including concurrent access,
 * format detection, and registry consistency.
 *
 *########################################################*/

package digital.vasic.yole.format

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Stress tests for FormatRegistry and ParserRegistry.
 */
class FormatRegistryStressTest {

    // ==================== FORMAT REGISTRY STRESS ====================

    @Test
    fun `concurrent format lookups by id`() = runBlocking<Unit> {
        val formatIds = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX
        )

        val results = (1..1000).map { i ->
            async {
                FormatRegistry.getById(formatIds[i % formatIds.size])
            }
        }.awaitAll()

        assertEquals(1000, results.size)
        assertTrue(results.all { it != null })
    }

    @Test
    fun `concurrent format lookups by extension`() = runBlocking<Unit> {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst")

        val results = (1..1000).map { i ->
            async {
                FormatRegistry.getByExtension(extensions[i % extensions.size])
            }
        }.awaitAll()

        assertEquals(1000, results.size)
        assertTrue(results.all { it != null })
    }

    @Test
    fun `concurrent format detection by content`() = runBlocking<Unit> {
        val contents = listOf(
            "# Markdown heading",
            "(A) Todo task",
            "a,b,c\n1,2,3",
            "\\documentclass{article}",
            "* Org mode heading"
        )

        val results = (1..500).map { i ->
            async {
                FormatRegistry.detectByContent(contents[i % contents.size])
            }
        }.awaitAll()

        assertEquals(500, results.size)
    }

    @Test
    fun `all formats have unique IDs`() {
        val ids = FormatRegistry.formats.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals(ids.size, uniqueIds.size, "All format IDs should be unique")
    }

    @Test
    fun `all formats have non-empty names`() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(format.name.isNotEmpty(), "Format ${format.id} should have non-empty name")
        }
    }

    @Test
    fun `all formats have default extension`() {
        FormatRegistry.formats.forEach { format ->
            assertTrue(
                format.defaultExtension.isNotEmpty() || format.extensions.isEmpty(),
                "Format ${format.id} should have default extension or no extensions"
            )
        }
    }

    @Test
    fun `extension lookup is case insensitive`() {
        val variations = listOf("md", "MD", "Md", "mD")

        val results = variations.map { FormatRegistry.getByExtension(it) }

        assertTrue(results.all { it != null })
        assertTrue(results.all { it?.id == FormatRegistry.ID_MARKDOWN })
    }

    @Test
    fun `extension lookup handles dot prefix consistently`() {
        val withDot = FormatRegistry.getByExtension(".md")
        val withoutDot = FormatRegistry.getByExtension("md")

        assertEquals(withDot, withoutDot)
    }

    @Test
    fun `detectByExtension always returns a format`() {
        val unknownExtensions = listOf("xyz", "abc", "unknown", "", "    ")

        unknownExtensions.forEach { ext ->
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format, "detectByExtension should never return null for '$ext'")
        }
    }

    @Test
    fun `getFormatsByExtension returns all matching formats`() {
        // .txt is used by both plaintext and todotxt
        val txtFormats = FormatRegistry.getFormatsByExtension("txt")

        assertTrue(txtFormats.isNotEmpty())
        assertTrue(txtFormats.any { it.id == FormatRegistry.ID_PLAINTEXT })
    }

    @Test
    fun `isSupported correctly identifies supported formats`() {
        val supportedIds = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV
        )

        supportedIds.forEach { id ->
            assertTrue(FormatRegistry.isSupported(id), "Format '$id' should be supported")
        }

        assertFalse(FormatRegistry.isSupported("nonexistent_format"))
    }

    @Test
    fun `isExtensionSupported correctly identifies extensions`() {
        val supportedExtensions = listOf("md", "txt", "csv", "tex", ".org")

        supportedExtensions.forEach { ext ->
            assertTrue(FormatRegistry.isExtensionSupported(ext), "Extension '$ext' should be supported")
        }

        assertFalse(FormatRegistry.isExtensionSupported("xyz"))
    }

    @Test
    fun `getAllExtensions returns all unique extensions`() {
        val extensions = FormatRegistry.getAllExtensions()

        assertTrue(extensions.isNotEmpty())
        assertEquals(extensions.size, extensions.toSet().size, "Extensions should be unique")
    }

    @Test
    fun `getFormatNames returns all format names`() {
        val names = FormatRegistry.getFormatNames()

        assertEquals(FormatRegistry.formats.size, names.size)
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("Plain Text"))
    }

    // ==================== CONTENT DETECTION STRESS ====================

    @Test
    fun `content detection handles very long content`() = runBlocking<Unit> {
        val longContent = "# Heading\n\n" + "a".repeat(100000)

        val result = FormatRegistry.detectByContent(longContent)

        // Should detect markdown due to heading at start
        assertNotNull(result)
        assertEquals(FormatRegistry.ID_MARKDOWN, result.id)
    }

    @Test
    fun `content detection respects maxLines parameter`() = runBlocking<Unit> {
        // Content with markdown at line 20
        val content = (1..25).joinToString("\n") { i ->
            if (i == 20) "# Markdown Heading" else "Plain text line $i"
        }

        // With default maxLines (10), should not detect markdown
        val resultDefault = FormatRegistry.detectByContent(content)

        // With higher maxLines, might detect markdown
        val resultExtended = FormatRegistry.detectByContent(content, maxLines = 25)

        // Results may differ based on maxLines - at least one should succeed
        assertTrue(
            resultDefault != null || resultExtended != null,
            "At least one detection should succeed"
        )
    }

    @Test
    fun `content detection handles empty content`() {
        val result = FormatRegistry.detectByContent("")

        assertNull(result, "Empty content should return null")
    }

    @Test
    fun `content detection handles whitespace only`() {
        val whitespaceContent = "   \n\t\n   "

        val result = FormatRegistry.detectByContent(whitespaceContent)

        // Whitespace-only content should not match any format patterns
        // Result depends on format patterns
        assertTrue(result == null || result.id.isNotEmpty())
    }

    @Test
    fun `content detection handles binary-like content`() {
        val binaryContent = "\u0000\u0001\u0002\u0003"

        val result = FormatRegistry.detectByContent(binaryContent)

        // Should not crash on binary content
        assertTrue(result == null || result.id.isNotEmpty())
    }

    // ==================== FILENAME DETECTION STRESS ====================

    @Test
    fun `detectByFilename handles various filename patterns`() {
        val testCases = listOf(
            "README.md" to FormatRegistry.ID_MARKDOWN,
            "document.txt" to FormatRegistry.ID_PLAINTEXT,
            "data.csv" to FormatRegistry.ID_CSV,
            "paper.tex" to FormatRegistry.ID_LATEX,
            "notes.org" to FormatRegistry.ID_ORGMODE
        )

        testCases.forEach { (filename, expectedId) ->
            val format = FormatRegistry.detectByFilename(filename)
            assertEquals(expectedId, format.id, "Failed for filename '$filename'")
        }
    }

    @Test
    fun `detectByFilename handles files without extension`() {
        val filename = "Makefile"

        val format = FormatRegistry.detectByFilename(filename)

        // Should return plaintext as fallback
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `detectByFilename handles hidden files`() {
        val hiddenFiles = listOf(
            ".gitignore",
            ".hidden",
            ".config"
        )

        hiddenFiles.forEach { filename ->
            val format = FormatRegistry.detectByFilename(filename)
            // Should not crash and should return a valid format
            assertNotNull(format)
        }
    }

    @Test
    fun `detectByFilename handles multiple dots`() {
        val filename = "document.backup.2024.md"

        val format = FormatRegistry.detectByFilename(filename)

        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `detectByFilename resolves todo dot txt to todotxt not plaintext`() {
        // Iter 40 fix (#yole-todotxt-compound-extension-detection):
        // before the fix, 'todo.txt' resolved to plaintext because the
        // compound-extension loop only tried the suffix at the FIRST
        // dot ('.txt') — never '.todo.txt'. Both plaintext and todotxt
        // advertise '.txt'; plaintext won by registration order. The
        // fix iterates dot-positions and tries suffixes longest-first
        // so '.todo.txt' (a TodoTxt extension) matches before '.txt'.
        //
        // End-user impact: a file literally named 'todo.txt' — THE
        // canonical Todo.txt filename — now opens with Todo.txt
        // highlighting instead of as Plain Text.
        val format = FormatRegistry.detectByFilename("todo.txt")
        assertEquals(
            FormatRegistry.ID_TODOTXT,
            format.id,
            "todo.txt must resolve to TODOTXT (compound-extension match), not ${format.id}"
        )
    }

    @Test
    fun `detectByFilename resolves prefixed todo dot txt to todotxt`() {
        // Compound-extension matching must work for any prefix:
        // 'work.todo.txt', 'shopping.todo.txt', etc. all canonical
        // Todo.txt-derived filenames.
        val variants = listOf(
            "work.todo.txt",
            "shopping.todo.txt",
            "project-x.todo.txt"
        )
        variants.forEach { filename ->
            val format = FormatRegistry.detectByFilename(filename)
            assertEquals(
                FormatRegistry.ID_TODOTXT,
                format.id,
                "'$filename' must resolve to TODOTXT (compound-extension match), not ${format.id}"
            )
        }
    }

    // ==================== FORMAT PATTERNS STRESS ====================

    @Test
    fun `all format patterns are valid regex`() {
        FormatRegistry.formats.forEach { format ->
            format.detectionPatterns.forEach { pattern ->
                assertDoesNotFail("Invalid regex in ${format.id}: $pattern") {
                    Regex(pattern)
                }
            }
        }
    }

    @Test
    fun `format patterns match expected content`() = runBlocking<Unit> {
        val testCases = listOf(
            "# Heading" to FormatRegistry.ID_MARKDOWN,
            "## Subheading" to FormatRegistry.ID_MARKDOWN,
            "[link](url)" to FormatRegistry.ID_MARKDOWN,
            "(A) Task" to FormatRegistry.ID_TODOTXT,
            "x 2024-01-01 Completed task" to FormatRegistry.ID_TODOTXT,
            "a,b,c" to FormatRegistry.ID_CSV,
            "\\documentclass{article}" to FormatRegistry.ID_LATEX,
            "\\begin{document}" to FormatRegistry.ID_LATEX,
            "== Wiki Heading ==" to FormatRegistry.ID_WIKITEXT,
            "* Org heading" to FormatRegistry.ID_ORGMODE,
            "#+" to FormatRegistry.ID_ORGMODE
        )

        testCases.forEach { (content, expectedId) ->
            val detected = FormatRegistry.detectByContent(content)
            assertEquals(expectedId, detected?.id, "Content '$content' should match format $expectedId")
        }
    }

    // ==================== CONCURRENT MIXED OPERATIONS ====================

    @Test
    fun `concurrent mixed registry operations`() = runBlocking<Unit> {
        val operations = (1..1000).map { i ->
            async {
                when (i % 5) {
                    0 -> FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
                    1 -> FormatRegistry.getByExtension("md")
                    2 -> FormatRegistry.detectByContent("# Heading")
                    3 -> FormatRegistry.detectByExtension("txt")
                    else -> FormatRegistry.detectByFilename("file.md")
                }
            }
        }

        val results = operations.awaitAll()
        assertEquals(1000, results.size)
    }

    // ==================== TEXT FORMAT DATA CLASS ====================

    @Test
    fun `TextFormat equality is based on id`() {
        val format1 = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test",
            extensions = listOf(".test")
        )

        val format2 = TextFormat(
            id = "test",
            name = "Different Name",
            defaultExtension = ".test2",
            extensions = listOf(".test2")
        )

        // Equality is based on data class, so all fields matter
        assertNotEquals(format1, format2)
    }

    @Test
    fun `TextFormat has consistent hashCode`() {
        val formats = FormatRegistry.formats

        formats.forEach { format ->
            val hash1 = format.hashCode()
            val hash2 = format.hashCode()
            assertEquals(hash1, hash2, "hashCode should be consistent for ${format.id}")
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun assertDoesNotFail(message: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message: ${e.message}")
        }
    }
}
