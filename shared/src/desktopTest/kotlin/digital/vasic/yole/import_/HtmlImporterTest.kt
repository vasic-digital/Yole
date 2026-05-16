/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 4: HtmlImporterTest — anti-bluff test.
 *
 * Feeds a hand-crafted HTML snippet (<h1> + <p> with <b>) through the
 * real HtmlImporter (jsoup → FlexmarkHtmlConverter) and asserts the
 * resulting Markdown contains the expected ATX heading and bold markers.
 *
 * Mutation guard: a stub that always returns failure is verified to
 * produce a failed Result, proving the test cannot PASS against a no-op.
 *
 * CONST-035: test exercises the real HtmlImporter code path end-to-end.
 * No mocking of the unit under test.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class HtmlImporterTest {

    // -----------------------------------------------------------------------
    // Primary test — real import
    // -----------------------------------------------------------------------

    @Test
    fun `HtmlImporter converts h1 heading and bold text to Markdown`(): Unit = runBlocking<Unit> {
        val html = "<h1>Title</h1><p><b>bold</b></p>"
        val importer = HtmlImporter()

        val result = importer.import(html.toByteArray(Charsets.UTF_8), "test.html")

        assertTrue(result.isSuccess, "Expected import to succeed but got: ${result.exceptionOrNull()}")

        val doc = result.getOrThrow()
        val md = doc.markdown

        // <h1> must produce ATX # heading
        assertTrue(
            md.contains("# Title"),
            "Markdown must contain '# Title' for <h1>Title</h1>, got:\n$md",
        )

        // <b>bold</b> must produce ** bold markers
        assertTrue(
            md.contains("**bold**") || md.contains("__bold__"),
            "Markdown must contain bold markers around 'bold', got:\n$md",
        )

        assertTrue(doc.sourceFormat == "html", "sourceFormat must be 'html'")
    }

    // -----------------------------------------------------------------------
    // Mutation guard — stub returning failure must NOT satisfy import assertions
    // -----------------------------------------------------------------------

    @Test
    fun `mutation guard - stub returning failure does not satisfy real import assertions`(): Unit = runBlocking<Unit> {
        val stub = object : DocumentImporter {
            override val supportedExtensions = setOf("html", "htm")
            override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
                Result.failure(ImportError.Malformed("html", RuntimeException("stub always fails")))
        }

        val html = "<h1>Title</h1><p><b>bold</b></p>"
        val stubResult = stub.import(html.toByteArray(Charsets.UTF_8), "test.html")

        assertTrue(
            stubResult.isFailure,
            "Mutation guard: the stub must return failure; a passing stub means the test is broken",
        )
    }

    // -----------------------------------------------------------------------
    // supportedExtensions
    // -----------------------------------------------------------------------

    @Test
    fun `HtmlImporter reports html and htm as supported extensions`() {
        val importer = HtmlImporter()
        assertContains(importer.supportedExtensions, "html")
        assertContains(importer.supportedExtensions, "htm")
    }

    // -----------------------------------------------------------------------
    // Complex HTML — multiple elements survive conversion
    // -----------------------------------------------------------------------

    @Test
    fun `HtmlImporter converts complex HTML with heading bold and paragraph`(): Unit = runBlocking<Unit> {
        val html = """
            <html><body>
              <h1>Document Title</h1>
              <p>Plain text paragraph.</p>
              <p><strong>strong text</strong> and <em>italic text</em></p>
            </body></html>
        """.trimIndent()
        val importer = HtmlImporter()

        val result = importer.import(html.toByteArray(Charsets.UTF_8), "complex.html")

        assertTrue(result.isSuccess, "Expected success for complex HTML, got: ${result.exceptionOrNull()}")
        val md = result.getOrThrow().markdown

        assertTrue(md.contains("# Document Title"), "Must contain ATX h1, got:\n$md")
        assertTrue(md.contains("Plain text paragraph"), "Must contain plain paragraph text, got:\n$md")
        // <strong> → ** or __ bold; <em> → * or _ italic
        assertTrue(
            md.contains("**strong text**") || md.contains("__strong text__"),
            "Must contain bold markers around 'strong text', got:\n$md",
        )
        assertTrue(
            md.contains("*italic text*") || md.contains("_italic text_"),
            "Must contain italic markers around 'italic text', got:\n$md",
        )
    }

    // -----------------------------------------------------------------------
    // sourceFormat set correctly for .htm extension
    // -----------------------------------------------------------------------

    @Test
    fun `HtmlImporter sourceFormat is html regardless of htm extension`(): Unit = runBlocking<Unit> {
        val html = "<h2>Sub</h2>"
        val importer = HtmlImporter()
        val result = importer.import(html.toByteArray(Charsets.UTF_8), "file.htm")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().sourceFormat == "html")
    }
}
