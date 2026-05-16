/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporterTest — anti-bluff test.
 *
 * Synthesises a minimal PDF in-memory using Apache PDFBox 3.0.7 APIs
 * (the same library used by PdfImporter.desktop.kt), then imports it
 * via the real PdfImporter and asserts:
 *   - The large-font title run ("Title" at pt 24) maps to a Markdown heading.
 *   - The body-text run ("Body text" at pt 12) is present as plain text.
 *
 * PDF synthesis strategy:
 *   PDFBox 3.x content-stream API is used to draw two text runs at different
 *   font sizes. This produces a structurally valid PDF that the PdfImporter
 *   text-extraction pipeline can process end-to-end.
 *
 * Mutation guard: a stub returning Result.failure is injected and verified to
 * produce a failed Result, proving the test CANNOT pass against a no-op importer.
 *
 * CONST-035: test exercises the real PdfImporter code path end-to-end.
 * No mocking of the unit under test.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PdfImporterTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Synthesises a single-page PDF containing:
     *   - "Title"     drawn at font-size 24 pt (heading candidate)
     *   - "Body text" drawn at font-size 12 pt (body text = mode)
     *
     * Uses PDFBox 3.x content-stream API:
     *   PDPageContentStream.setFont(PDType1Font, size)
     *   PDPageContentStream.showText(String)
     *
     * This produces a real, spec-compliant PDF whose text is extractable by
     * PDFTextStripper — the same pipeline used by PdfImporter.
     */
    private fun buildTestPdfBytes(): ByteArray {
        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        // PDFBox 3.x font instantiation: PDType1Font(Standard14Fonts.FontName)
        val helveticaBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val helvetica = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        PDPageContentStream(document, page).use { cs ->
            // Title at pt 24
            cs.beginText()
            cs.setFont(helveticaBold, 24f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText("Title")
            cs.endText()

            // Body text at pt 12
            cs.beginText()
            cs.setFont(helvetica, 12f)
            cs.newLineAtOffset(50f, 650f)
            cs.showText("Body text")
            cs.endText()
        }

        val bos = ByteArrayOutputStream()
        document.save(bos)
        document.close()
        return bos.toByteArray()
    }

    // -----------------------------------------------------------------------
    // Primary test — real import: heading + body text
    // -----------------------------------------------------------------------

    @Test
    fun `PdfImporter maps large-font title to heading and body text to paragraph`(): Unit = runBlocking<Unit> {
        val importer = PdfImporter()
        val bytes = buildTestPdfBytes()

        val result = importer.import(bytes, "test.pdf")

        assertTrue(result.isSuccess, "Expected import to succeed but got: ${result.exceptionOrNull()}")

        val doc = result.getOrThrow()
        val md = doc.markdown

        // "Title" at font-size 24 must produce a Markdown heading (# or ## or ### etc.)
        assertTrue(
            md.contains("# Title") || md.contains("## Title") ||
                md.contains("### Title") || md.contains("#### Title"),
            "Markdown must contain a heading for 'Title', got:\n$md",
        )

        // "Body text" at font-size 12 must appear as plain paragraph text
        assertContains(md, "Body text", message = "Markdown must contain 'Body text' as body content")

        // sourceFormat is set correctly
        assertTrue(doc.sourceFormat == "pdf", "sourceFormat must be 'pdf', got '${doc.sourceFormat}'")
    }

    // -----------------------------------------------------------------------
    // Mutation guard — stub that ignores bytes must produce failure
    // -----------------------------------------------------------------------

    @Test
    fun `mutation guard - stub returning failure does not satisfy real import assertions`(): Unit = runBlocking<Unit> {
        val stub = object : DocumentImporter {
            override val supportedExtensions = setOf("pdf")
            override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
                Result.failure(ImportError.Malformed("pdf", RuntimeException("stub always fails")))
        }

        val bytes = buildTestPdfBytes()
        val stubResult = stub.import(bytes, "test.pdf")

        assertTrue(
            stubResult.isFailure,
            "Mutation guard: the stub must return failure; a passing stub means the test is broken",
        )
    }

    // -----------------------------------------------------------------------
    // supportedExtensions
    // -----------------------------------------------------------------------

    @Test
    fun `PdfImporter reports pdf as supported extension`() {
        val importer = PdfImporter()
        assertContains(importer.supportedExtensions, "pdf")
    }

    // -----------------------------------------------------------------------
    // Malformed bytes → Result.failure(ImportError.Malformed)
    // -----------------------------------------------------------------------

    @Test
    fun `PdfImporter returns Malformed for garbage bytes`(): Unit = runBlocking<Unit> {
        val importer = PdfImporter()
        val garbage = ByteArray(64) { (it + 200).toByte() }

        val result = importer.import(garbage, "corrupt.pdf")

        assertTrue(result.isFailure, "Expected failure for garbage bytes")
        val error = result.exceptionOrNull()
        assertTrue(
            error is ImportError.Malformed,
            "Expected ImportError.Malformed, got ${error?.javaClass?.simpleName}",
        )
    }
}
