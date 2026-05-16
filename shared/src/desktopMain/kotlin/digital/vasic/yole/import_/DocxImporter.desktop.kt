/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 3: DocxImporter — Desktop (JVM) actual.
 *
 * Uses Apache POI XWPFDocument (poi-ooxml) to walk .docx body elements
 * and produce Markdown:
 *   - XWPFParagraph with named heading style → ATX heading (#)
 *   - XWPFParagraph plain text              → paragraph (bold/italic markers)
 *   - XWPFTable                             → GFM table via TableConverter
 *   - Embedded pictures in XWPFRun          → ![](image_N.ext) via ImageExtractor
 *   - Hyperlinks via XWPFHyperlinkRun       → [text](url) via LinkPreserver
 *   - Other body elements                   → ImportWarning(Info, …)
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import digital.vasic.yole.import_.conversion.ImageExtractor
import digital.vasic.yole.import_.conversion.LinkPreserver
import digital.vasic.yole.import_.conversion.TableConverter
import kotlinx.coroutines.CancellationException
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.ByteArrayInputStream

actual class DocxImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("docx")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val result = parseDocx(bytes)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("docx", e))
        }
    }

    private fun parseDocx(bytes: ByteArray): ImportedDocument {
        val xwpf = XWPFDocument(ByteArrayInputStream(bytes))
        val sb = StringBuilder()
        val warnings = mutableListOf<ImportWarning>()
        var imageIndex = 0

        for (bodyElement in xwpf.bodyElements) {
            when (bodyElement) {
                is XWPFParagraph -> {
                    val (mdText, newIdx) = processParagraph(bodyElement, imageIndex, warnings)
                    imageIndex = newIdx
                    if (mdText.isNotEmpty()) {
                        sb.append(mdText)
                        sb.append("\n\n")
                    }
                }
                is XWPFTable -> {
                    val rows = bodyElement.rows.map { row ->
                        row.tableCells.map { cell -> cell.text.trim() }
                    }
                    val tableMarkdown = TableConverter.toMarkdownTable(rows)
                    if (tableMarkdown.isNotEmpty()) {
                        sb.append(tableMarkdown)
                        sb.append("\n\n")
                    }
                }
                else -> {
                    val typeName = bodyElement::class.simpleName ?: "Unknown"
                    warnings += ImportWarning(severity = Severity.Info, message = "Skipped element: $typeName")
                }
            }
        }

        xwpf.close()
        return ImportedDocument(
            sourceFormat = "docx",
            markdown = sb.trimEnd().toString(),
            warnings = warnings,
        )
    }

    /**
     * Converts a paragraph to a Markdown line.
     * Returns a pair of (markdown text, updated image index).
     */
    @Suppress("NestedBlockDepth")
    private fun processParagraph(
        para: XWPFParagraph,
        imageIndex: Int,
        warnings: MutableList<ImportWarning>,
    ): Pair<String, Int> {
        val headingLevel = headingLevelFromStyle(para.style)
        var currentIdx = imageIndex
        val sb = StringBuilder()

        for (run in para.runs) {
            val pics = run.embeddedPictures
            if (pics.isNotEmpty()) {
                for (pic in pics) {
                    val picData = pic.pictureData
                    val fmt = picData.suggestFileExtension().lowercase().let { if (it.isBlank()) "png" else it }
                    val name = "image_$currentIdx.$fmt"
                    currentIdx++
                    ImageExtractor.fromBytes(picData.data, fmt, name)
                    sb.append("![]($name)")
                }
                continue
            }

            if (run is XWPFHyperlinkRun) {
                // In POI 5.x the hyperlink URI is in the document relationship.
                // XWPFHyperlinkRun.hyperlinkId → document part relation → externalTarget.
                val hyperlinkId = run.hyperlinkId
                val url = try {
                    val rel = para.document.packagePart.getRelationship(hyperlinkId)
                    rel?.targetURI?.toString() ?: ""
                } catch (_: Exception) {
                    ""
                }
                val runText = run.getText(0) ?: ""
                val linkText = applyInlineFormatting(runText, run.isBold == true, run.isItalic == true)
                sb.append(LinkPreserver.toMarkdownLink(linkText, url))
                continue
            }

            val runText = run.getText(0) ?: ""
            if (runText.isEmpty()) continue
            sb.append(applyInlineFormatting(runText, run.isBold == true, run.isItalic == true))
        }

        val text = sb.toString()
        if (text.isBlank()) return Pair("", currentIdx)

        val finalText = if (headingLevel != null) {
            "${"#".repeat(headingLevel)} $text"
        } else {
            text
        }

        return Pair(finalText, currentIdx)
    }

    private fun applyInlineFormatting(text: String, bold: Boolean, italic: Boolean): String {
        if (text.isEmpty()) return text
        return when {
            bold && italic -> "***$text***"
            bold           -> "**$text**"
            italic         -> "*$text*"
            else           -> text
        }
    }

    private fun headingLevelFromStyle(style: String?): Int? {
        if (style == null) return null
        return WORD_HEADING_STYLES[style.trim()]
    }

    companion object {
        private val WORD_HEADING_STYLES: Map<String, Int> = mapOf(
            "Heading 1" to 1,
            "Heading 2" to 2,
            "Heading 3" to 3,
            "Heading 4" to 4,
            "Heading 5" to 5,
            "Heading 6" to 6,
            "heading 1" to 1,
            "heading 2" to 2,
            "heading 3" to 3,
            "heading 4" to 4,
            "heading 5" to 5,
            "heading 6" to 6,
            "1" to 1,
            "2" to 2,
            "3" to 3,
            "4" to 4,
            "5" to 5,
            "6" to 6,
        )
    }
}
