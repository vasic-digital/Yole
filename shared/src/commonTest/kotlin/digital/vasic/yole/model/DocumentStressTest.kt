/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Document Model Stress Tests
 *
 * Comprehensive stress tests for the Document model
 * including serialization, format detection, and
 * concurrent access patterns.
 *
 *########################################################*/

package digital.vasic.yole.model

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Stress tests for Document model operations.
 */
class DocumentStressTest {

    // ==================== CREATION STRESS ====================

    @Test
    fun `create many documents concurrently`() = runBlocking<Unit> {
        val documents = (1..1000).map { i ->
            async {
                Document(
                    path = "/path/to/doc$i.md",
                    title = "doc$i",
                    extension = "md",
                    format = Document.FORMAT_MARKDOWN
                )
            }
        }.awaitAll()

        assertEquals(1000, documents.size)
        assertTrue(documents.all { it.format == Document.FORMAT_MARKDOWN })
    }

    @Test
    fun `create documents with all format types`() = runBlocking<Unit> {
        val formats = listOf(
            Document.FORMAT_PLAINTEXT to "txt",
            Document.FORMAT_MARKDOWN to "md",
            Document.FORMAT_TODOTXT to "txt",
            Document.FORMAT_CSV to "csv",
            Document.FORMAT_LATEX to "tex",
            Document.FORMAT_ASCIIDOC to "adoc",
            Document.FORMAT_ORGMODE to "org",
            Document.FORMAT_WIKITEXT to "wiki",
            Document.FORMAT_RESTRUCTUREDTEXT to "rst",
            Document.FORMAT_TASKPAPER to "taskpaper",
            Document.FORMAT_TEXTILE to "textile",
            Document.FORMAT_CREOLE to "creole",
            Document.FORMAT_TIDDLYWIKI to "tid",
            Document.FORMAT_JUPYTER to "ipynb",
            Document.FORMAT_RMARKDOWN to "rmd",
            Document.FORMAT_KEYVALUE to "ini",
            Document.FORMAT_BINARY to "bin"
        )

        formats.forEach { (format, extension) ->
            val doc = Document(
                path = "/path/to/file.$extension",
                title = "file",
                extension = extension,
                format = format
            )

            assertEquals(format, doc.format)
            assertEquals(extension, doc.extension)
        }
    }

    // ==================== FORMAT DETECTION STRESS ====================

    @Test
    fun `detect format by extension for all supported extensions`() = runBlocking<Unit> {
        val documents = FormatRegistry.getAllExtensions().map { ext ->
            async {
                val doc = Document(
                    path = "/path/to/file$ext",
                    title = "file",
                    extension = ext.removePrefix("."),
                    format = Document.FORMAT_UNKNOWN
                )
                doc.detectFormatByExtension()
                doc
            }
        }.awaitAll()

        assertTrue(documents.all { it.format != Document.FORMAT_UNKNOWN })
    }

    @Test
    fun `detect format by content concurrently`() = runBlocking<Unit> {
        val contentSamples = listOf(
            "# Markdown Heading\n\nParagraph with **bold** text." to "markdown",
            "(A) Todo task @context +project" to "todotxt",
            "a,b,c\n1,2,3" to "csv",
            "\\documentclass{article}\n\\begin{document}" to "latex",
            "* Org heading\n** Sub heading" to "orgmode",
            "== Wiki Heading ==" to "wikitext"
        )

        val results = contentSamples.map { (content, _) ->
            async {
                val doc = Document(
                    path = "/path/to/file.txt",
                    title = "file",
                    extension = "txt",
                    format = Document.FORMAT_UNKNOWN
                )
                doc.detectFormatByContent(content)
                doc
            }
        }.awaitAll()

        assertEquals(contentSamples.size, results.size)
    }

    @Test
    fun `format detection is idempotent`() = runBlocking<Unit> {
        val doc = Document(
            path = "/path/to/file.md",
            title = "file",
            extension = "md",
            format = Document.FORMAT_UNKNOWN
        )

        // Detect multiple times
        repeat(100) {
            doc.detectFormatByExtension()
        }

        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    // ==================== FILENAME GENERATION STRESS ====================

    @Test
    fun `filename property handles various extensions`() = runBlocking<Unit> {
        val testCases = listOf(
            Triple("doc", "md", "doc.md"),
            Triple("doc", "markdown", "doc.markdown"),
            Triple("doc", "", "doc"),
            Triple("doc.backup", "md", "doc.backup.md"),
            Triple("", "md", ".md"),
            Triple("doc", "tar.gz", "doc.tar.gz")
        )

        testCases.forEach { (title, ext, expected) ->
            val doc = Document(
                path = "/path/to/$expected",
                title = title,
                extension = ext
            )

            assertEquals(expected, doc.filename, "Failed for title='$title', ext='$ext'")
        }
    }

    @Test
    fun `filename with special characters`() = runBlocking<Unit> {
        val specialTitles = listOf(
            "doc with spaces",
            "doc-with-dashes",
            "doc_with_underscores",
            "doc.with.dots",
            "doc(with)parens",
            "doc[with]brackets"
        )

        specialTitles.forEach { title ->
            val doc = Document(
                path = "/path/to/$title.md",
                title = title,
                extension = "md"
            )

            assertEquals("$title.md", doc.filename)
        }
    }

    // ==================== CHANGE TRACKING STRESS ====================

    @Test
    fun `change tracking reset is consistent`() = runBlocking<Unit> {
        val doc = Document(
            path = "/path/to/file.md",
            title = "file",
            extension = "md",
            modTime = 1000L,
            touchTime = 2000L
        )

        repeat(100) {
            doc.resetChangeTracking()
            assertEquals(-1L, doc.modTime)
            assertEquals(-1L, doc.touchTime)
        }
    }

    @Test
    fun `touch updates time correctly`() = runBlocking<Unit> {
        val doc = Document(
            path = "/path/to/file.md",
            title = "file",
            extension = "md"
        )

        val times = (1..100).map {
            doc.touch()
            doc.touchTime
        }

        // Touch times should be monotonically increasing (or equal if called in same millisecond)
        times.zipWithNext().forEach { (prev, curr) ->
            assertTrue(curr >= prev, "Touch time should not decrease")
        }
    }

    // ==================== SERIALIZATION STRESS ====================

    @Test
    fun `serialize and deserialize many documents`() = runBlocking<Unit> {
        val json = Json { ignoreUnknownKeys = true }

        val documents = (1..500).map { i ->
            Document(
                path = "/path/to/doc$i.md",
                title = "doc$i",
                extension = "md",
                format = Document.FORMAT_MARKDOWN
            )
        }

        val serialized = documents.map { json.encodeToString(Document.serializer(), it) }
        val deserialized = serialized.map { json.decodeFromString(Document.serializer(), it) }

        assertEquals(documents.size, deserialized.size)
        documents.zip(deserialized).forEach { (original, restored) ->
            assertEquals(original.path, restored.path)
            assertEquals(original.title, restored.title)
            assertEquals(original.extension, restored.extension)
            assertEquals(original.format, restored.format)
        }
    }

    @Test
    fun `serialization handles special characters`() = runBlocking<Unit> {
        val json = Json { ignoreUnknownKeys = true }

        val specialDocs = listOf(
            Document(path = "/path/with spaces/file.md", title = "file with spaces", extension = "md"),
            Document(path = "/path/with\"quotes\"/file.md", title = "file\"quotes", extension = "md"),
            Document(path = "/path/with\ttabs/file.md", title = "file\ttabs", extension = "md"),
            Document(path = "/path/unicode/\u00e9\u00e8\u00ea/file.md", title = "\u00e9\u00e8\u00ea", extension = "md")
        )

        specialDocs.forEach { doc ->
            val serialized = json.encodeToString(Document.serializer(), doc)
            val restored = json.decodeFromString(Document.serializer(), serialized)

            assertEquals(doc.path, restored.path)
            assertEquals(doc.title, restored.title)
        }
    }

    // ==================== EQUALITY AND HASHING STRESS ====================

    @Test
    fun `equals and hashCode are consistent`() = runBlocking<Unit> {
        val documents = (1..1000).map { i ->
            Document(
                path = "/path/to/doc${i % 100}.md",
                title = "doc${i % 100}",
                extension = "md",
                format = Document.FORMAT_MARKDOWN
            )
        }

        // Documents with same path, title, format should be equal
        val grouped = documents.groupBy { it }
        grouped.forEach { (_, equalDocs) ->
            assertTrue(equalDocs.all { it.hashCode() == equalDocs.first().hashCode() })
        }
    }

    @Test
    fun `different documents have different hashes`() = runBlocking<Unit> {
        val documents = (1..100).map { i ->
            Document(
                path = "/path/to/unique/doc$i.md",
                title = "uniquedoc$i",
                extension = "md",
                format = Document.FORMAT_MARKDOWN
            )
        }

        val uniqueHashes = documents.map { it.hashCode() }.toSet()
        // With 100 unique documents, we expect mostly unique hashes
        // Allow for some collisions
        assertTrue(uniqueHashes.size > 80, "Expected mostly unique hashes, got ${uniqueHashes.size}")
    }

    // ==================== TEXT FORMAT ACCESS STRESS ====================

    @Test
    fun `getTextFormat handles all format types`() = runBlocking<Unit> {
        val formatIds = listOf(
            Document.FORMAT_PLAINTEXT,
            Document.FORMAT_MARKDOWN,
            Document.FORMAT_TODOTXT,
            Document.FORMAT_CSV,
            Document.FORMAT_LATEX,
            Document.FORMAT_ASCIIDOC,
            Document.FORMAT_ORGMODE,
            Document.FORMAT_WIKITEXT
        )

        formatIds.forEach { formatId ->
            val doc = Document(
                path = "/path/to/file.txt",
                title = "file",
                extension = "txt",
                format = formatId
            )

            val textFormat = doc.getTextFormat()
            assertNotNull(textFormat)
            assertEquals(formatId, textFormat.id)
        }
    }

    @Test
    fun `getTextFormat handles unknown format gracefully`() = runBlocking<Unit> {
        val doc = Document(
            path = "/path/to/file.xyz",
            title = "file",
            extension = "xyz",
            format = "nonexistent_format"
        )

        val textFormat = doc.getTextFormat()
        // Should return a fallback format, not throw
        assertNotNull(textFormat)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `document handles empty path`() = runBlocking<Unit> {
        val doc = Document(
            path = "",
            title = "",
            extension = ""
        )

        assertEquals("", doc.path)
        assertEquals("", doc.title)
        assertEquals("", doc.extension)
        assertEquals("", doc.filename)
    }

    @Test
    fun `document handles very long paths`() = runBlocking<Unit> {
        val longPath = "/" + "a".repeat(10000) + "/file.md"

        val doc = Document(
            path = longPath,
            title = "file",
            extension = "md"
        )

        assertEquals(longPath, doc.path)
    }

    @Test
    fun `document handles unicode paths`() = runBlocking<Unit> {
        val unicodePaths = listOf(
            "/documents/\u6587\u6863/file.md",
            "/\u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u044b/file.md",
            "/\u30c9\u30ad\u30e5\u30e1\u30f3\u30c8/file.md"
        )

        unicodePaths.forEach { path ->
            val doc = Document(
                path = path,
                title = "file",
                extension = "md"
            )

            assertEquals(path, doc.path)
        }
    }

    // ==================== FORMAT CONSTANTS VERIFICATION ====================

    @Test
    fun `format constants match TextFormat constants`() {
        assertEquals(TextFormat.ID_UNKNOWN, Document.FORMAT_UNKNOWN)
        assertEquals(TextFormat.ID_PLAINTEXT, Document.FORMAT_PLAINTEXT)
        assertEquals(TextFormat.ID_MARKDOWN, Document.FORMAT_MARKDOWN)
        assertEquals(TextFormat.ID_TODOTXT, Document.FORMAT_TODOTXT)
        assertEquals(TextFormat.ID_CSV, Document.FORMAT_CSV)
        assertEquals(TextFormat.ID_LATEX, Document.FORMAT_LATEX)
        assertEquals(TextFormat.ID_ASCIIDOC, Document.FORMAT_ASCIIDOC)
        assertEquals(TextFormat.ID_ORGMODE, Document.FORMAT_ORGMODE)
        assertEquals(TextFormat.ID_WIKITEXT, Document.FORMAT_WIKITEXT)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, Document.FORMAT_RESTRUCTUREDTEXT)
        assertEquals(TextFormat.ID_TASKPAPER, Document.FORMAT_TASKPAPER)
        assertEquals(TextFormat.ID_TEXTILE, Document.FORMAT_TEXTILE)
        assertEquals(TextFormat.ID_CREOLE, Document.FORMAT_CREOLE)
        assertEquals(TextFormat.ID_TIDDLYWIKI, Document.FORMAT_TIDDLYWIKI)
        assertEquals(TextFormat.ID_JUPYTER, Document.FORMAT_JUPYTER)
        assertEquals(TextFormat.ID_RMARKDOWN, Document.FORMAT_RMARKDOWN)
        assertEquals(TextFormat.ID_KEYVALUE, Document.FORMAT_KEYVALUE)
        assertEquals(TextFormat.ID_BINARY, Document.FORMAT_BINARY)
    }
}
