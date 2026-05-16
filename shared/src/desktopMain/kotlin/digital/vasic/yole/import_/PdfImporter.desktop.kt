/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporter — Desktop (JVM) actual.
 *
 * Uses Apache PDFBox 3.0.7 (org.apache.pdfbox:pdfbox).
 *
 * 3.x API differences from 2.x (documented for Android comparison):
 *   - Entry point:  Loader.loadPDF(byte[])  (3.x)
 *                   PDDocument.load(InputStream)  (2.x)
 *   - PDFTextStripper.getText(doc) works the same in both versions.
 *   - PDPage / PDResources / PDImageXObject surface is identical.
 *   - Loader class is new in 3.x; does not exist in 2.x.
 *
 * Heading detection algorithm (Phase 0 §3):
 *   1. Walk all pages; record (text, fontSize, fontName) per character run via
 *      a custom PDFTextStripper override that intercepts writeString.
 *   2. Build a font-size frequency histogram across all runs (weighted by char count).
 *   3. Body-text size = mode (highest total character count).
 *   4. All sizes strictly larger than the mode, sorted descending, map to
 *      heading levels H1-H4 via HeadingDetector.headingLevelByFontSize.
 *   5. Monospace runs (CodeBlockDetector.isMonospaceRun) → fenced code blocks.
 *   6. Embedded images via PDImageXObject → ImageExtractor.fromBytes →
 *      markdown `![](image_<n>.<ext>)` reference.
 *   7. Low-confidence detections (single-char large-font runs) emit ImportWarning.
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import digital.vasic.yole.import_.conversion.CodeBlockDetector
import digital.vasic.yole.import_.conversion.HeadingDetector
import digital.vasic.yole.import_.conversion.ImageExtractor
import kotlinx.coroutines.CancellationException
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.StringWriter

actual class PdfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("pdf")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val result = parsePdf(bytes)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("pdf", e))
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun parsePdf(bytes: ByteArray): ImportedDocument {
        // PDFBox 3.x entry point: Loader.loadPDF(byte[]).
        // This replaces the 2.x PDDocument.load(InputStream) call.
        val document: PDDocument = Loader.loadPDF(bytes)
        document.use { doc ->
            return buildImportedDocument(doc)
        }
    }

    private fun buildImportedDocument(doc: PDDocument): ImportedDocument {
        // ---- Phase 1 — collect runs ----------------------------------------
        val collector = RunCollector()
        collector.sortByPosition = true
        collector.startPage = 1
        collector.endPage = doc.numberOfPages
        // getText() drives the text-extraction pipeline and populates collector.runs.
        // The return value (plain text string) is discarded — we use runs instead.
        val sw = StringWriter()
        collector.writeText(doc, sw)
        val runs = collector.runs

        if (runs.isEmpty()) {
            return ImportedDocument(
                sourceFormat = "pdf",
                markdown = "",
                warnings = listOf(
                    ImportWarning(Severity.Info, "No text runs extracted from PDF"),
                ),
            )
        }

        // ---- Phase 2 — font-size histogram (mode = body text) --------------
        val sizeFrequency = mutableMapOf<Float, Int>()
        for (run in runs) {
            sizeFrequency[run.fontSize] = (sizeFrequency[run.fontSize] ?: 0) + run.text.length
        }
        val bodySize: Float = sizeFrequency.maxByOrNull { it.value }?.key ?: runs[0].fontSize

        // ---- Phase 3 — sorted distinct sizes for heading lookup -------------
        val sortedDistinctSizes: List<Float> = sizeFrequency.keys
            .sortedDescending()
            .distinct()

        // ---- Phase 4 — extract images --------------------------------------
        val images = mutableListOf<String>() // markdown refs
        val imageWarnings = mutableListOf<ImportWarning>()
        var imageIndex = 0
        for (pageIdx in 0 until doc.numberOfPages) {
            val page = doc.getPage(pageIdx)
            val resources = page.resources ?: continue
            try {
                val xObjectNames = resources.xObjectNames ?: continue
                for (xObjName in xObjectNames) {
                    val xObj = resources.getXObject(xObjName)
                    if (xObj is PDImageXObject) {
                        val ext = (xObj.suffix ?: "png").lowercase()
                        // toImage() returns java.awt.image.BufferedImage in PDFBox 3.x Desktop JVM.
                        val awtImg = xObj.image
                        val imgBytes: ByteArray? = awtImg?.let { img ->
                            val bos = java.io.ByteArrayOutputStream()
                            javax.imageio.ImageIO.write(img, ext.ifEmpty { "png" }, bos)
                            bos.toByteArray()
                        }
                        if (imgBytes != null && imgBytes.isNotEmpty()) {
                            val extracted = ImageExtractor.fromBytes(
                                bytes = imgBytes,
                                format = ext,
                                name = "image_$imageIndex.$ext",
                            )
                            images += "![](${extracted.suggestedFileName})"
                            imageIndex++
                        }
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                imageWarnings += ImportWarning(
                    Severity.Info,
                    "Image extraction failed on page ${pageIdx + 1}: ${e.message}",
                    pageOrSection = "page ${pageIdx + 1}",
                )
            }
        }

        // ---- Phase 5 — emit markdown ---------------------------------------
        val sb = StringBuilder()
        val warnings = mutableListOf<ImportWarning>()
        warnings += imageWarnings

        val pendingBodyRuns = mutableListOf<String>()

        fun flushBody() {
            val text = pendingBodyRuns.joinToString(" ").trim()
            if (text.isNotEmpty()) {
                sb.append(text)
                sb.append("\n\n")
            }
            pendingBodyRuns.clear()
        }

        for (run in runs) {
            val text = run.text.trim()
            if (text.isEmpty()) continue

            when {
                CodeBlockDetector.isMonospaceRun(run.fontName) -> {
                    flushBody()
                    sb.append("```\n")
                    sb.append(text)
                    sb.append("\n```\n\n")
                }
                run.fontSize > bodySize -> {
                    val level = HeadingDetector.headingLevelByFontSize(run.fontSize, sortedDistinctSizes)
                    if (level != null) {
                        flushBody()
                        val sizeDelta = run.fontSize - bodySize
                        if (sizeDelta < 1.5f && text.split(" ").size == 1) {
                            warnings += ImportWarning(
                                Severity.Info,
                                "Low-confidence heading: \"$text\" at ${run.fontSize} vs body $bodySize",
                            )
                        }
                        sb.append("#".repeat(level))
                        sb.append(" ")
                        sb.append(text)
                        sb.append("\n\n")
                    } else {
                        pendingBodyRuns += text
                    }
                }
                else -> {
                    pendingBodyRuns += text
                }
            }
        }
        flushBody()

        for (mdRef in images) {
            sb.append(mdRef)
            sb.append("\n\n")
        }

        return ImportedDocument(
            sourceFormat = "pdf",
            markdown = sb.trimEnd().toString(),
            warnings = warnings,
        )
    }

    // -----------------------------------------------------------------------
    // Custom PDFTextStripper that records per-run metadata
    // -----------------------------------------------------------------------

    /** Lightweight carrier for per-run extracted metadata. */
    private data class TextRun(val text: String, val fontSize: Float, val fontName: String)

    /**
     * Extends [PDFTextStripper] to capture each character run's font size and
     * font name in addition to the extracted text.
     *
     * PDFBox 3.x: writeString(text, positions) is called per character-sequence.
     * We compute the median font size across positions in the run to smooth out
     * per-character size jitter common in embedded-font PDFs.
     *
     * The `runs` list is populated as a side-effect of writeText/getText.
     */
    private class RunCollector : PDFTextStripper() {

        val runs = mutableListOf<TextRun>()

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            if (text.isBlank()) {
                super.writeString(text, textPositions)
                return
            }
            val sizes = textPositions.map { it.fontSize }
            val sortedSizes = sizes.sorted()
            val medianSize = if (sortedSizes.isEmpty()) 12f
            else sortedSizes[sortedSizes.size / 2]

            // Strip common style suffixes from font names (e.g. "Helvetica-Bold" → "Helvetica")
            val rawFontName = textPositions.firstOrNull()?.font?.name ?: ""
            val fontName = rawFontName.substringBefore(",").substringBefore("-")

            runs += TextRun(text = text, fontSize = medianSize, fontName = fontName)
            super.writeString(text, textPositions)
        }
    }
}
