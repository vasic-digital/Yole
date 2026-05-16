/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 6: OdtImporter — Desktop (JVM) actual.
 *
 * Uses Apache ODFDOM OdfTextDocument.loadDocument(InputStream) to parse
 * the ODT file, then walks the ODF element tree via the DOM:
 *   - text:h elements → ATX headings (#..######) using the outline-level
 *     attribute (text:outline-level) clamped to 1–6.
 *   - text:p elements → plain paragraph text.
 *   - text:span children → text content concatenated (bold/italic not mapped
 *     here; ODF style inheritance would require resolving automatic styles,
 *     which is out of scope for Phase 6 — tracked in KNOWN_DEFECTS).
 *   - Other elements  → ImportWarning(Info) and skipped.
 *
 * ODFDOM's OdfDocument.loadDocument() is NOT thread-safe; each call creates
 * its own OdfPackage so concurrent imports are safe as long as each OdtImporter
 * instance is not shared across coroutines (import() is called on a single
 * suspend site).
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.CancellationException
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream

actual class OdtImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("odt")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val result = parseOdt(bytes)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("odt", e))
        }
    }

    private fun parseOdt(bytes: ByteArray): ImportedDocument {
        val odfDoc = OdfTextDocument.loadDocument(ByteArrayInputStream(bytes))
        val contentDom = odfDoc.contentDom

        val sb = StringBuilder()
        val warnings = mutableListOf<ImportWarning>()

        // The ODF content DOM root is <office:document-content>.
        // Body content lives under <office:body> / <office:text>.
        val officeText = findOfficeText(contentDom.documentElement)
        if (officeText != null) {
            walkChildren(officeText, sb, warnings)
        } else {
            warnings += ImportWarning(
                severity = Severity.Warning,
                message = "office:text element not found in content.xml — empty output",
            )
        }

        odfDoc.close()
        return ImportedDocument(
            sourceFormat = "odt",
            markdown = sb.trimEnd().toString(),
            warnings = warnings,
        )
    }

    /**
     * Recursively find the first <office:text> element anywhere in the DOM.
     * ODFDOM nests it as: document-content → body → text.
     */
    private fun findOfficeText(node: Node): Element? {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val elem = node as Element
            // ODF namespace prefixes vary; match on local name.
            if (elem.localName == "text" && elem.namespaceURI?.contains("office") == true) {
                return elem
            }
        }
        var child = node.firstChild
        while (child != null) {
            val found = findOfficeText(child)
            if (found != null) return found
            child = child.nextSibling
        }
        return null
    }

    /**
     * Walk direct children of [parent], converting text:h and text:p elements.
     */
    @Suppress("NestedBlockDepth")
    private fun walkChildren(
        parent: Node,
        sb: StringBuilder,
        warnings: MutableList<ImportWarning>,
    ) {
        var child = parent.firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE) {
                val elem = child as Element
                val localName = elem.localName ?: ""
                when {
                    localName == "h" -> {
                        val level = headingLevel(elem)
                        val text = extractText(elem).trim()
                        if (text.isNotEmpty()) {
                            sb.append("#".repeat(level))
                            sb.append(" ")
                            sb.append(text)
                            sb.append("\n\n")
                        }
                    }
                    localName == "p" -> {
                        val text = extractText(elem).trim()
                        if (text.isNotEmpty()) {
                            sb.append(text)
                            sb.append("\n\n")
                        }
                    }
                    localName == "list" -> {
                        // Recurse into list items
                        walkChildren(elem, sb, warnings)
                    }
                    localName == "list-item" -> {
                        // list-item wraps text:p children; recurse
                        walkChildren(elem, sb, warnings)
                    }
                    else -> {
                        warnings += ImportWarning(
                            severity = Severity.Info,
                            message = "Skipped ODF element: text:$localName",
                        )
                    }
                }
            }
            child = child.nextSibling
        }
    }

    /**
     * Resolve the heading level from the `text:outline-level` attribute.
     * Falls back to 1 if the attribute is absent or unparseable.
     * Clamped to the range [1, 6].
     */
    private fun headingLevel(elem: Element): Int {
        val raw = elem.getAttributeNS(ODT_TEXT_NS, "outline-level")
            .ifEmpty { elem.getAttribute("text:outline-level") }
        val level = raw.toIntOrNull() ?: 1
        return level.coerceIn(1, 6)
    }

    /**
     * Concatenate all text-node descendants of [elem].
     * Handles nested text:span, text:a, etc. by recursing into child elements.
     */
    private fun extractText(elem: Element): String {
        val sb = StringBuilder()
        extractTextRecursive(elem, sb)
        return sb.toString()
    }

    private fun extractTextRecursive(node: Node, sb: StringBuilder) {
        var child = node.firstChild
        while (child != null) {
            when (child.nodeType) {
                Node.TEXT_NODE -> sb.append(child.nodeValue ?: "")
                Node.ELEMENT_NODE -> extractTextRecursive(child, sb)
            }
            child = child.nextSibling
        }
    }

    companion object {
        private const val ODT_TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
    }
}
