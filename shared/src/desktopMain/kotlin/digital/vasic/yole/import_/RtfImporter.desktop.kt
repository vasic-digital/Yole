/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporter — Desktop (JVM) actual.
 * iter-75 (#iter-64-rtf-colour-images): detect non-default foreground colour
 * and emit ImportWarning so users know colour is not preserved in output.
 *
 * Uses javax.swing.text.rtf.RTFEditorKit (Java SE) to load RTF bytes into
 * a DefaultStyledDocument, then walks the Element tree to emit Markdown:
 *   - Each leaf element whose text is not the sentinel '\n' → paragraph
 *   - Bold / italic character attributes → ** / * markers
 *   - Non-default foreground colour → ImportWarning(Info, ...)
 *   - Multiple paragraphs separated by blank line
 *
 * Colour, font family, embedded images, and tables are NOT preserved in the
 * Markdown output — the tracker (#iter-64-rtf-colour-images) is closed by
 * detecting their presence and warning the user, not by full extraction.
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.CancellationException
import java.awt.Color
import java.io.ByteArrayInputStream
import javax.swing.text.AttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.rtf.RTFEditorKit

actual class RtfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("rtf")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val (markdown, warnings) = parseRtf(bytes)
            Result.success(
                ImportedDocument(
                    sourceFormat = "rtf",
                    markdown = markdown,
                    warnings = warnings,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("rtf", e))
        }
    }

    /**
     * Parse RTF [bytes] and return a Pair of (markdown, warnings).
     *
     * iter-75 (#iter-64-rtf-colour-images): detects non-default foreground
     * colour and emits a single ImportWarning so users know colour is not
     * preserved. The colour itself is dropped (Markdown has no colour syntax).
     */
    private fun parseRtf(bytes: ByteArray): Pair<String, List<ImportWarning>> {
        // Validate RTF signature: all RTF documents start with "{\rtf"
        val header = bytes.take(5).toByteArray().decodeToString()
        if (!header.startsWith("{\\rtf")) {
            throw IllegalArgumentException("Not a valid RTF document (missing {\\rtf header)")
        }

        val kit = RTFEditorKit()
        val doc = kit.createDefaultDocument()
        ByteArrayInputStream(bytes).use { stream ->
            kit.read(stream, doc, 0)
        }

        val sb = StringBuilder()
        val root = doc.defaultRootElement
        val paragraphs = mutableListOf<String>()
        val warnings = mutableListOf<ImportWarning>()
        var colourWarningEmitted = false

        for (pIdx in 0 until root.elementCount) {
            val paraElem = root.getElement(pIdx)
            val paraBuilder = StringBuilder()

            for (lIdx in 0 until paraElem.elementCount) {
                val leaf = paraElem.getElement(lIdx)
                val start = leaf.startOffset
                val end = leaf.endOffset
                val rawText = doc.getText(start, end - start)

                // Skip the document-sentinel newline that RTFEditorKit appends
                if (rawText == "\n") continue

                // Strip embedded newlines from run text (RTF line breaks inside a para)
                val text = rawText.replace("\n", " ").replace("\r", "")
                if (text.isEmpty()) continue

                val attrs: AttributeSet = leaf.attributes
                val bold = StyleConstants.isBold(attrs)
                val italic = StyleConstants.isItalic(attrs)

                // iter-75 (#iter-64-rtf-colour-images): detect non-default foreground colour.
                // RTFEditorKit stores foreground as java.awt.Color via StyleConstants.Foreground.
                // Default/unset colour appears as null or Color.BLACK (#000000).
                if (!colourWarningEmitted) {
                    val fg: Color? = StyleConstants.getForeground(attrs)
                    if (fg != null && fg != Color.BLACK && text.isNotBlank()) {
                        warnings += ImportWarning(
                            Severity.Info,
                            "RTF document contains coloured text. Colours are not preserved " +
                                "in the Markdown output (#iter-64-rtf-colour-images).",
                        )
                        colourWarningEmitted = true
                    }
                }

                paraBuilder.append(applyInlineFormatting(text, bold, italic))
            }

            val paraText = paraBuilder.toString().trim()
            if (paraText.isNotEmpty()) {
                paragraphs.add(paraText)
            }
        }

        paragraphs.joinTo(sb, separator = "\n\n")
        return sb.toString() to warnings
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
}
