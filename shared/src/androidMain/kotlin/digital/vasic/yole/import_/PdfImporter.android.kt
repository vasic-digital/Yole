/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporter — Android (JVM) actual.
 *
 * Uses pdfbox-android 2.0.27.0 (com.tom-roush:pdfbox-android).
 * This is an Android-safe port of Apache PDFBox 2.0.x:
 *   - Replaces AWT/Swing classes with Android Bitmap APIs.
 *   - Ships no javax.swing or java.awt dependencies.
 *   - Entry point: PDDocument.load(InputStream) — the 2.x API.
 *     (3.x uses Loader.loadPDF(byte[]); Loader does NOT exist in 2.x.)
 *
 * API divergence from Desktop 3.x (documented per plan task §4):
 *   - 2.x entry point: com.tom_roush.pdfbox.pdmodel.PDDocument.load(InputStream)
 *   - 3.x entry point: org.apache.pdfbox.Loader.loadPDF(byte[])
 *   - 2.x text extractor: com.tom_roush.pdfbox.text.PDFTextStripper
 *   - 3.x text extractor: org.apache.pdfbox.text.PDFTextStripper
 *   - writeText(doc, writer) signature: identical in both 2.x and 3.x.
 *   - Image: pdfbox-android PDImageXObject.image returns android.graphics.Bitmap
 *     rather than java.awt.image.BufferedImage (Desktop 3.x).
 *   - PDFBoxResourceLoader.init() is an Android-specific init call required
 *     before first load to locate bundled CMaps from assets.
 *
 * Heading detection uses the same font-size histogram algorithm as the Desktop
 * actual (Phase 0 §3): body-text size = mode; outlier sizes → H1-H4 via
 * HeadingDetector.headingLevelByFontSize. Monospace runs → fenced code blocks.
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import digital.vasic.yole.import_.conversion.CodeBlockDetector
import digital.vasic.yole.import_.conversion.HeadingDetector
import digital.vasic.yole.import_.conversion.ImageExtractor
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
        // pdfbox-android 2.x entry point: PDDocument.load(InputStream).
        // Differs from PDFBox 3.x Desktop: Loader.loadPDF(byte[]) does NOT exist in 2.x.
        // PDFBoxResourceLoader.init() is Android-specific: locates bundled CMaps from assets.
        // Passing null is acceptable when called from unit tests (no Context needed for CMaps
        // that are bundled inside the JAR rather than Android assets).
        PDFBoxResourceLoader.init(null)
        val document: PDDocument = PDDocument.load(ByteArrayInputStream(bytes))
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
        // writeText drives text extraction; collector populates runs as a side-effect.
        val sw = StringWriter()
        collector.writeText(doc, sw)
        val runs = collector.runs

        if (runs.isEmpty()) {
            // iter-75 (#iter-64-pdf-image-only): upgrade severity and improve message.
            return ImportedDocument(
                sourceFormat = "pdf",
                markdown = "",
                warnings = listOf(
                    ImportWarning(
                        Severity.Warning,
                        "This PDF has no extractable text layer. It may be a scanned or " +
                            "image-only PDF. Use OCR software to extract the content before importing.",
                    ),
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

        // ---- Phase 4 — extract images (Android Bitmap path) ----------------
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
                        // pdfbox-android: PDImageXObject.image returns android.graphics.Bitmap.
                        // This differs from Desktop 3.x which returns java.awt.image.BufferedImage.
                        val bitmap: Bitmap? = xObj.image
                        val imgBytes: ByteArray? = bitmap?.let { bmp ->
                            val bos = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos)
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
    // Custom PDFTextStripper that records per-run metadata (2.x API)
    // -----------------------------------------------------------------------

    /** Lightweight carrier for per-run extracted metadata. */
    private data class TextRun(val text: String, val fontSize: Float, val fontName: String)

    /**
     * 2.x PDFTextStripper override — same algorithm as Desktop 3.x but uses
     * com.tom_roush.pdfbox.text.TextPosition (2.x package path).
     *
     * writeString(text, positions) is overridden to capture each run's size/font
     * into `runs` as a side-effect of writeText().
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

            val rawFontName = textPositions.firstOrNull()?.font?.name ?: ""
            val fontName = rawFontName.substringBefore(",").substringBefore("-")

            runs += TextRun(text = text, fontSize = medianSize, fontName = fontName)
            super.writeString(text, textPositions)
        }
    }
}
