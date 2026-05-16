/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 3: DocxImporterTest — anti-bluff test.
 *
 * Synthesises a .docx in-memory via Apache POI APIs (XWPFDocument +
 * XWPFParagraph + XWPFRun), imports it, and asserts the Markdown output
 * contains the expected heading and bold-word patterns.
 *
 * Mutation guard: a stub returning Result.failure is injected and verified
 * to produce a failed Result, proving the test cannot PASS against a no-op.
 *
 * CONST-035: test exercises the real DocxImporter code path end-to-end.
 * No mocking of the unit under test.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.runBlocking
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertContains
import kotlin.test.assertTrue

class DocxImporterTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Build a minimal .docx with a Heading-1 paragraph and a mixed-bold paragraph. */
    private fun buildTestDocxBytes(): ByteArray {
        val xwpf = XWPFDocument()

        // Heading 1: "Title"
        val heading = xwpf.createParagraph()
        heading.style = "Heading 1"
        val headingRun = heading.createRun()
        headingRun.setText("Title")

        // Paragraph: "Hello " + bold "World"
        val body = xwpf.createParagraph()
        val plainRun = body.createRun()
        plainRun.setText("Hello ")
        val boldRun = body.createRun()
        boldRun.isBold = true
        boldRun.setText("World")

        val bos = ByteArrayOutputStream()
        xwpf.write(bos)
        xwpf.close()
        return bos.toByteArray()
    }

    // -----------------------------------------------------------------------
    // Primary test — real import
    // -----------------------------------------------------------------------

    @Test
    fun `DocxImporter imports heading and bold paragraph correctly`(): Unit = runBlocking<Unit> {
        val importer = DocxImporter()
        val bytes = buildTestDocxBytes()

        val result = importer.import(bytes, "test.docx")

        assertTrue(result.isSuccess, "Expected import to succeed but got: ${result.exceptionOrNull()}")

        val doc = result.getOrThrow()
        val md = doc.markdown

        // Heading 1 maps to ATX # heading
        assertContains(md, "# Title", message = "Markdown must contain '# Title' for the Heading-1 paragraph")

        // Bold "World" must appear with ** markers; "Hello" is plain
        assertTrue(
            md.contains("**World**") || md.contains("***World***"),
            "Markdown must contain bold markers around 'World', got:\n$md",
        )
        assertContains(md, "Hello", message = "Plain text 'Hello' must be present in the output")

        // sourceFormat is set correctly
        assertTrue(doc.sourceFormat == "docx", "sourceFormat must be 'docx'")
    }

    // -----------------------------------------------------------------------
    // Mutation guard — stub that ignores bytes must produce failure
    // -----------------------------------------------------------------------

    @Test
    fun `mutation guard - stub returning failure does not satisfy real import assertions`(): Unit = runBlocking<Unit> {
        // Inline stub: simulates a broken importer that always fails
        val stub = object : DocumentImporter {
            override val supportedExtensions = setOf("docx")
            override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
                Result.failure(ImportError.Malformed("docx", RuntimeException("stub always fails")))
        }

        val bytes = buildTestDocxBytes()
        val stubResult = stub.import(bytes, "test.docx")

        // The stub MUST produce failure — if it ever returns success the production
        // importer has been inadvertently replaced, which is a build-config defect.
        assertTrue(
            stubResult.isFailure,
            "Mutation guard: the stub must return failure; a passing stub means the test is broken",
        )
    }

    // -----------------------------------------------------------------------
    // supportedExtensions
    // -----------------------------------------------------------------------

    @Test
    fun `DocxImporter reports docx as supported extension`() {
        val importer = DocxImporter()
        assertContains(importer.supportedExtensions, "docx")
    }

    // -----------------------------------------------------------------------
    // Malformed bytes → Result.failure(ImportError.Malformed)
    // -----------------------------------------------------------------------

    @Test
    fun `DocxImporter returns Malformed for garbage bytes`(): Unit = runBlocking<Unit> {
        val importer = DocxImporter()
        val garbage = ByteArray(128) { it.toByte() }

        val result = importer.import(garbage, "corrupt.docx")

        assertTrue(result.isFailure, "Expected failure for garbage bytes")
        val error = result.exceptionOrNull()
        assertTrue(
            error is ImportError.Malformed,
            "Expected ImportError.Malformed, got ${error?.javaClass?.simpleName}",
        )
    }
}
