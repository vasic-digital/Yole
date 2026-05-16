/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 6: OdtImporter — Android (JVM) actual.
 *
 * Strategy: raw ZipInputStream + Android-native XmlPullParser.
 *
 * Apache ODFDOM pulls in Xerces2 (xml-apis + xercesImpl) which conflicts
 * with Android's built-in XML implementation at runtime (Phase 0 §5 finding).
 * To avoid this, the Android actual does NOT use ODFDOM at all.
 * Instead it:
 *   1. Opens the ODT bytes as a ZipInputStream (ODT is a ZIP container).
 *   2. Locates the "content.xml" entry.
 *   3. Reads it with XmlPullParser (android.util.Xml.newPullParser()) — a
 *      platform-native API available since API level 1.
 *   4. Walks the SAX-style pull-parser events:
 *        text:h  → ATX heading (level from text:outline-level attribute)
 *        text:p  → plain paragraph
 *        (text content accumulated per paragraph/heading across nested tags)
 *
 * The same bytes that ODFDOM parses on Desktop are fed to the ZIP path here,
 * so OdtImporterTest on desktopTest also exercises this code path directly
 * (see OdtImporterTest.`Android ZIP path produces equivalent output to ODFDOM`).
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import android.util.Xml
import kotlinx.coroutines.CancellationException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

actual class OdtImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("odt")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val result = parseOdtViaZip(bytes)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("odt", e))
        }
    }

    /**
     * Open [bytes] as a ZIP container, find content.xml, and parse it with
     * the Android XmlPullParser.  Returns an [ImportedDocument] with Markdown.
     */
    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    internal fun parseOdtViaZip(bytes: ByteArray): ImportedDocument {
        val contentXmlBytes = extractContentXml(bytes)
            ?: throw IOException("content.xml not found in ODT archive — not a valid ODT file")

        val sb = StringBuilder()
        val warnings = mutableListOf<ImportWarning>()

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(ByteArrayInputStream(contentXmlBytes), "UTF-8")

        // State machine: we accumulate text across events inside a paragraph
        // or heading block.
        var inBlock = false
        var isHeading = false
        var headingLevel = 1
        val blockText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val localName = parser.name ?: ""
                    when {
                        localName == "h" && isTextNs(parser.namespace) -> {
                            inBlock = true
                            isHeading = true
                            headingLevel = resolveHeadingLevel(parser)
                            blockText.clear()
                        }
                        localName == "p" && isTextNs(parser.namespace) -> {
                            inBlock = true
                            isHeading = false
                            blockText.clear()
                        }
                        localName == "line-break" && isTextNs(parser.namespace) -> {
                            if (inBlock) blockText.append(" ")
                        }
                        localName == "tab" && isTextNs(parser.namespace) -> {
                            if (inBlock) blockText.append("\t")
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val localName = parser.name ?: ""
                    val isBlockEnd = (localName == "h" || localName == "p") &&
                        isTextNs(parser.namespace)
                    if (isBlockEnd && inBlock) {
                        val text = blockText.toString().trim()
                        if (text.isNotEmpty()) {
                            if (isHeading) {
                                sb.append("#".repeat(headingLevel))
                                sb.append(" ")
                                sb.append(text)
                                sb.append("\n\n")
                            } else {
                                sb.append(text)
                                sb.append("\n\n")
                            }
                        }
                        inBlock = false
                        blockText.clear()
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inBlock) {
                        blockText.append(parser.text ?: "")
                    }
                }
            }
            try {
                eventType = parser.next()
            } catch (e: XmlPullParserException) {
                warnings += ImportWarning(severity = Severity.Warning, message = "XML parse error: ${e.message}")
                break
            }
        }

        return ImportedDocument(
            sourceFormat = "odt",
            markdown = sb.trimEnd().toString(),
            warnings = warnings,
        )
    }

    /** Extract the bytes of content.xml from the ODT (ZIP) archive. */
    private fun extractContentXml(bytes: ByteArray): ByteArray? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "content.xml") {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    /** True when [ns] is the ODF text namespace (or empty — some parsers strip it). */
    private fun isTextNs(ns: String?): Boolean {
        return ns.isNullOrEmpty() || ns == ODT_TEXT_NS
    }

    /**
     * Read the `text:outline-level` attribute from the current START_TAG event.
     * Falls back to 1 if absent or unparseable. Clamped to [1, 6].
     */
    private fun resolveHeadingLevel(parser: XmlPullParser): Int {
        for (i in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(i)
            if (name == "outline-level" || name == "text:outline-level") {
                return (parser.getAttributeValue(i).toIntOrNull() ?: 1).coerceIn(1, 6)
            }
        }
        return 1
    }

    companion object {
        private const val ODT_TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
    }
}
