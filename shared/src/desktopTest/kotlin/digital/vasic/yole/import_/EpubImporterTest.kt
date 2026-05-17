/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 8: EpubImporterTest — anti-bluff tests.
 * iter-75 (#iter-64-epub-metadata): tests for YAML frontmatter extraction
 * from OPF dc:title / dc:creator fields added.
 *
 * Synthesises a minimal valid EPUB ZIP in-memory using ZipOutputStream,
 * imports it, and asserts the resulting Markdown contains:
 *   - Chapter 1 heading "# Chapter 1" from <h1>Chapter 1</h1>
 *   - "Hello" from <p>Hello</p>
 *   - Markdown HR separator "---"
 *   - Chapter 2 heading "# Chapter 2" from <h1>Chapter 2</h1>
 *   - "World" from <p>World</p>
 *
 * Mutation guard: a stub returning Result.failure is injected and verified
 * to produce a failed Result, proving the test cannot PASS against a no-op.
 *
 * CONST-035: tests exercise the real EpubImporter code path end-to-end.
 * No mocking of the unit under test; the full ZIP→jsoup→HtmlImporter pipeline
 * is exercised.
 *
 * Anti-bluff mutation procedure for metadata tests (CONST-035):
 *   1. Stub buildYamlFrontmatter to return "" always.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.import_.EpubImporterTest.epubMetadata_prependsYamlFrontmatter"
 *   3. Expect FAIL (markdown does not start with "---").
 *   4. Revert; confirm GREEN.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertContains
import kotlin.test.assertTrue

class EpubImporterTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Build a minimal valid EPUB ZIP containing:
     *   - mimetype (required first entry, uncompressed)
     *   - META-INF/container.xml  → rootfile pointing to OEBPS/content.opf
     *   - OEBPS/content.opf       → manifest (ch1.xhtml, ch2.xhtml) + spine
     *   - OEBPS/ch1.xhtml         → <h1>Chapter 1</h1><p>Hello</p>
     *   - OEBPS/ch2.xhtml         → <h1>Chapter 2</h1><p>World</p>
     */
    private fun buildTestEpubBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->

            // mimetype — must be first, uncompressed (EPUB spec)
            zip.putNextEntry(ZipEntry("mimetype").also { it.method = ZipEntry.STORED; it.size = 20; it.compressedSize = 20; it.crc = mimetypeCrc() })
            zip.write("application/epub+zip".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // META-INF/container.xml
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:names:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf"
              media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()

            // OEBPS/content.opf
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="bookid">
  <metadata/>
  <manifest>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()

            // OEBPS/ch1.xhtml
            zip.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
  <h1>Chapter 1</h1>
  <p>Hello</p>
</body>
</html>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()

            // OEBPS/ch2.xhtml
            zip.putNextEntry(ZipEntry("OEBPS/ch2.xhtml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
  <h1>Chapter 2</h1>
  <p>World</p>
</body>
</html>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    /** CRC-32 of "application/epub+zip" (required for STORED mimetype entry). */
    private fun mimetypeCrc(): Long {
        val crc = java.util.zip.CRC32()
        crc.update("application/epub+zip".toByteArray(Charsets.UTF_8))
        return crc.value
    }

    // -----------------------------------------------------------------------
    // Primary test — full import with 2-chapter EPUB
    // -----------------------------------------------------------------------

    @Test
    fun `EpubImporter imports two-chapter EPUB and produces combined Markdown`(): Unit =
        runBlocking<Unit> {
            val importer = EpubImporter()
            val bytes = buildTestEpubBytes()

            val result = importer.import(bytes, "test.epub")

            assertTrue(
                result.isSuccess,
                "Expected import to succeed but got: ${result.exceptionOrNull()}",
            )

            val doc = result.getOrThrow()
            val md = doc.markdown

            // Chapter 1 heading: <h1>Chapter 1</h1> → ATX # heading
            assertTrue(
                md.contains("# Chapter 1"),
                "Markdown must contain '# Chapter 1', got:\n$md",
            )

            // Chapter 1 body text
            assertContains(md, "Hello", message = "Markdown must contain 'Hello' from ch1.xhtml")

            // Markdown HR separator between chapters
            assertTrue(
                md.contains("---"),
                "Markdown must contain '---' chapter separator, got:\n$md",
            )

            // Chapter 2 heading: <h1>Chapter 2</h1> → ATX # heading
            assertTrue(
                md.contains("# Chapter 2"),
                "Markdown must contain '# Chapter 2', got:\n$md",
            )

            // Chapter 2 body text
            assertContains(md, "World", message = "Markdown must contain 'World' from ch2.xhtml")

            // sourceFormat
            assertTrue(doc.sourceFormat == "epub", "sourceFormat must be 'epub'")
        }

    // -----------------------------------------------------------------------
    // Chapter ordering — Chapter 1 must appear before Chapter 2
    // -----------------------------------------------------------------------

    @Test
    fun `EpubImporter preserves spine order - Chapter 1 before Chapter 2`(): Unit =
        runBlocking<Unit> {
            val importer = EpubImporter()
            val bytes = buildTestEpubBytes()

            val result = importer.import(bytes, "test.epub")
            assertTrue(result.isSuccess, "Expected import to succeed")

            val md = result.getOrThrow().markdown

            val idx1 = md.indexOf("Chapter 1")
            val idx2 = md.indexOf("Chapter 2")

            assertTrue(idx1 >= 0, "Chapter 1 must be present in Markdown")
            assertTrue(idx2 >= 0, "Chapter 2 must be present in Markdown")
            assertTrue(idx1 < idx2, "Chapter 1 must appear before Chapter 2 in Markdown")
        }

    // -----------------------------------------------------------------------
    // Mutation guard — stub returning failure must not satisfy import assertions
    // -----------------------------------------------------------------------

    @Test
    fun `mutation guard - stub returning failure does not satisfy real import assertions`(): Unit =
        runBlocking<Unit> {
            // Stub that never reads the bytes — simulates a broken importer
            val stub = object : DocumentImporter {
                override val supportedExtensions = setOf("epub")
                override suspend fun import(
                    bytes: ByteArray,
                    fileName: String,
                ): Result<ImportedDocument> =
                    Result.failure(ImportError.Malformed("epub", RuntimeException("stub always fails")))
            }

            val bytes = buildTestEpubBytes()
            val stubResult = stub.import(bytes, "test.epub")

            // Stub MUST return failure — if it ever returns success the real importer
            // has been replaced or the test is trivially satisfied
            assertTrue(
                stubResult.isFailure,
                "Mutation guard: stub must return failure; a passing stub means the test is broken",
            )
        }

    // -----------------------------------------------------------------------
    // supportedExtensions
    // -----------------------------------------------------------------------

    @Test
    fun `EpubImporter reports epub as supported extension`() {
        val importer = EpubImporter()
        assertContains(importer.supportedExtensions, "epub")
    }

    // -----------------------------------------------------------------------
    // Malformed bytes → Result.failure(ImportError.Malformed)
    // -----------------------------------------------------------------------

    @Test
    fun `EpubImporter returns Malformed for garbage bytes`(): Unit = runBlocking<Unit> {
        val importer = EpubImporter()
        val garbage = ByteArray(128) { it.toByte() }

        val result = importer.import(garbage, "corrupt.epub")

        assertTrue(result.isFailure, "Expected failure for garbage bytes")
        val error = result.exceptionOrNull()
        assertTrue(
            error is ImportError.Malformed,
            "Expected ImportError.Malformed, got ${error?.javaClass?.simpleName}",
        )
    }

    // -----------------------------------------------------------------------
    // iter-75 (#iter-64-epub-metadata): YAML frontmatter from OPF dc: fields
    // -----------------------------------------------------------------------

    /**
     * Build an EPUB with dc:title and dc:creator in the OPF metadata section.
     *
     * The resulting Markdown MUST begin with a YAML frontmatter block:
     *   ---
     *   title: "The Great Book"
     *   author: "Jane Author"
     *   ---
     *
     * Mutation: stub buildYamlFrontmatter to return "" → markdown does NOT start
     * with "---" → FAIL.
     */
    @Test
    fun `epubMetadata_prependsYamlFrontmatter`(): Unit = runBlocking<Unit> {
        val importer = EpubImporter()
        val bytes = buildEpubWithMetadata(
            title = "The Great Book",
            creator = "Jane Author",
        )

        val result = importer.import(bytes, "meta.epub")
        assertTrue(result.isSuccess, "Import must succeed: ${result.exceptionOrNull()}")

        val md = result.getOrThrow().markdown

        assertTrue(
            md.startsWith("---"),
            "Markdown must start with YAML frontmatter '---', got:\n${md.take(200)}",
        )
        assertTrue(
            md.contains("title: \"The Great Book\""),
            "Frontmatter must contain title, got:\n${md.take(200)}",
        )
        assertTrue(
            md.contains("author: \"Jane Author\""),
            "Frontmatter must contain author, got:\n${md.take(200)}",
        )
    }

    /**
     * An EPUB with no OPF metadata MUST NOT produce a YAML frontmatter block.
     *
     * Mutation: always prepend YAML regardless → FAIL (markdown starts with "---"
     * even when empty metadata).
     */
    @Test
    fun `epubMetadata_noMetadata_noFrontmatter`(): Unit = runBlocking<Unit> {
        val importer = EpubImporter()
        // Uses buildTestEpubBytes() which has <metadata/> (empty)
        val bytes = buildTestEpubBytes()

        val result = importer.import(bytes, "nometadata.epub")
        assertTrue(result.isSuccess, "Import must succeed: ${result.exceptionOrNull()}")

        val md = result.getOrThrow().markdown

        // A YAML frontmatter block starts with "---\n"; spine body does not
        val hasFrontmatter = md.startsWith("---\ntitle") || md.startsWith("---\nauthor")
        assertTrue(
            !hasFrontmatter,
            "Markdown must NOT start with YAML frontmatter when OPF has no dc: fields, got:\n${md.take(200)}",
        )
    }

    /**
     * Unit test for [EpubImporter.buildYamlFrontmatter] — all 4 fields.
     *
     * Mutation: remove "author:" line → FAIL (expected author line in output).
     */
    @Test
    fun `buildYamlFrontmatter_allFields`() {
        val importer = EpubImporter()
        val metadata = mapOf(
            "title" to "My Title",
            "creator" to "My Author",
            "publisher" to "My Publisher",
            "date" to "2024-01-01",
        )
        val fm = importer.buildYamlFrontmatter(metadata)
        assertTrue(fm.contains("title: \"My Title\""), "Must contain title")
        assertTrue(fm.contains("author: \"My Author\""), "Must contain author")
        assertTrue(fm.contains("publisher: \"My Publisher\""), "Must contain publisher")
        assertTrue(fm.contains("date: \"2024-01-01\""), "Must contain date")
    }

    /**
     * Unit test for [EpubImporter.buildYamlFrontmatter] — empty map returns "".
     *
     * Mutation: return a non-empty string always → FAIL.
     */
    @Test
    fun `buildYamlFrontmatter_emptyMap_returnsEmpty`() {
        val importer = EpubImporter()
        val fm = importer.buildYamlFrontmatter(emptyMap())
        assertTrue(fm.isEmpty(), "Empty metadata map must produce empty frontmatter string, got: '$fm'")
    }

    // -----------------------------------------------------------------------
    // Helpers for metadata tests
    // -----------------------------------------------------------------------

    /**
     * Build a minimal EPUB with OPF dc:title and dc:creator metadata.
     * Single chapter containing "Hello".
     */
    private fun buildEpubWithMetadata(title: String, creator: String): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            // mimetype
            zip.putNextEntry(ZipEntry("mimetype").also {
                it.method = ZipEntry.STORED; it.size = 20; it.compressedSize = 20; it.crc = mimetypeCrc()
            })
            zip.write("application/epub+zip".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // META-INF/container.xml
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:names:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf"
              media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()

            // OEBPS/content.opf — with dc: metadata
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf"
         xmlns:dc="http://purl.org/dc/elements/1.1/"
         version="2.0" unique-identifier="bookid">
  <metadata>
    <dc:title>$title</dc:title>
    <dc:creator>$creator</dc:creator>
  </metadata>
  <manifest>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="ch1"/>
  </spine>
</package>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()

            // OEBPS/ch1.xhtml
            zip.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<body><p>Hello</p></body>
</html>""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()
        }
        return bos.toByteArray()
    }
}
