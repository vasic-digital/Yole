/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporter — Desktop (JVM) actual.
 *
 * Uses javax.swing.text.rtf.RTFEditorKit (Java SE) to load RTF bytes into
 * a DefaultStyledDocument, then walks the Element tree to emit Markdown:
 *   - Each leaf element whose text is not the sentinel '\n' → paragraph
 *   - Bold / italic character attributes → ** / * markers
 *   - Multiple paragraphs separated by blank line
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import javax.swing.text.AttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.rtf.RTFEditorKit

actual class RtfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("rtf")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val markdown = parseRtf(bytes)
            Result.success(
                ImportedDocument(
                    sourceFormat = "rtf",
                    markdown = markdown,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("rtf", e))
        }
    }

    private fun parseRtf(bytes: ByteArray): String {
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

                paraBuilder.append(applyInlineFormatting(text, bold, italic))
            }

            val paraText = paraBuilder.toString().trim()
            if (paraText.isNotEmpty()) {
                paragraphs.add(paraText)
            }
        }

        paragraphs.joinTo(sb, separator = "\n\n")
        return sb.toString()
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
