/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 8: EpubImporter — Android (JVM) actual.
 * iter-75 (#iter-64-epub-metadata): OPF metadata (dc:title, dc:creator)
 * extracted and prepended as YAML frontmatter.
 *
 * Identical implementation to the Desktop actual; both source sets target
 * JVM and share ZipInputStream + jsoup + HtmlImporter.  A future refactor
 * may introduce a jvmMain intermediate source set to de-duplicate, but the
 * current KMP configuration has no jvmMain.
 *
 * Roll-own EPUB parsing pipeline (no new deps — uses existing jsoup + JVM stdlib):
 *
 *   1. ZipInputStream(ByteArrayInputStream(bytes)) — enumerate all ZIP entries.
 *   2. Read META-INF/container.xml → jsoup.parse() → extract OPF path.
 *   3. Read OPF → jsoup.parse() → extract metadata + manifest (id → href) + spine order.
 *   4. For each spine item: read chapter bytes → HtmlImporter().import() → markdown.
 *   5. Concatenate with "\n\n---\n\n" separator.
 *   6. Prepend YAML frontmatter from OPF metadata if available.
 *   7. Aggregate warnings.
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.CancellationException
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

actual class EpubImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("epub")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val result = parseEpub(bytes)
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("epub", e))
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    @Suppress("NestedBlockDepth")
    private suspend fun parseEpub(bytes: ByteArray): ImportedDocument {
        // Step 1 — read all ZIP entries into memory
        val entries: Map<String, ByteArray> = readZipEntries(bytes)

        // Step 2 — locate OPF path from META-INF/container.xml
        val containerXml = entries["META-INF/container.xml"]
            ?: error("META-INF/container.xml not found in EPUB archive")
        val opfPath = extractOpfPath(containerXml)

        // Step 3 — parse OPF file (metadata + manifest + spine)
        val opfBytes = entries[opfPath]
            ?: error("OPF file '$opfPath' not found in EPUB archive")
        val (metadata, manifest, spineIdrefs) = parseOpf(opfBytes)

        // OPF directory is used to resolve relative hrefs
        val opfDir = opfPath.substringBeforeLast("/", missingDelimiterValue = "")

        // Step 4 — import each spine chapter via HtmlImporter
        val htmlImporter = HtmlImporter()
        val chapterMarkdowns = mutableListOf<String>()
        val allWarnings = mutableListOf<ImportWarning>()

        for (idref in spineIdrefs) {
            val href = manifest[idref] ?: continue
            val entryPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
            val normalisedPath = entryPath.replace('\\', '/')
            val chapterBytes = entries[normalisedPath] ?: continue

            val chapterResult = htmlImporter.import(chapterBytes, href)
            if (chapterResult.isSuccess) {
                val chapterDoc = chapterResult.getOrThrow()
                val md = chapterDoc.markdown.trim()
                if (md.isNotEmpty()) {
                    chapterMarkdowns += md
                }
                allWarnings += chapterDoc.warnings
            } else {
                allWarnings += ImportWarning(
                    severity = Severity.Warning,
                    message = "Chapter '$href' (idref=$idref) failed to import: " +
                        "${chapterResult.exceptionOrNull()?.message}",
                    pageOrSection = idref,
                )
            }
        }

        // Step 5 — concatenate chapters with Markdown HR separator
        val chapterBody = chapterMarkdowns.joinToString(separator = "\n\n---\n\n")

        // Step 6 — prepend YAML frontmatter if dc:title or dc:creator present.
        // iter-75 (#iter-64-epub-metadata)
        val frontmatter = buildYamlFrontmatter(metadata)
        val combinedMarkdown = if (frontmatter.isNotEmpty()) {
            "---\n$frontmatter---\n\n$chapterBody"
        } else {
            chapterBody
        }

        return ImportedDocument(
            sourceFormat = "epub",
            markdown = combinedMarkdown,
            warnings = allWarnings,
        )
    }

    /**
     * Build a YAML frontmatter block (without the surrounding "---" delimiters)
     * from the OPF [metadata] map. Returns an empty string if no known fields present.
     */
    internal fun buildYamlFrontmatter(metadata: Map<String, String>): String {
        val lines = mutableListOf<String>()
        metadata["title"]?.let { lines += "title: \"${it.replace("\"", "\\\"")}\"" }
        metadata["creator"]?.let { lines += "author: \"${it.replace("\"", "\\\"")}\"" }
        metadata["publisher"]?.let { lines += "publisher: \"${it.replace("\"", "\\\"")}\"" }
        metadata["date"]?.let { lines += "date: \"$it\"" }
        if (lines.isEmpty()) return ""
        return lines.joinToString(separator = "\n") + "\n"
    }

    private fun readZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.replace('\\', '/')
                    entries[name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun extractOpfPath(containerXmlBytes: ByteArray): String {
        val xml = containerXmlBytes.toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
        val rootfile = doc.selectFirst("rootfile")
            ?: error("No <rootfile> element in container.xml")
        val path = rootfile.attr("full-path")
        if (path.isBlank()) error("<rootfile> has no full-path attribute")
        return path
    }

    internal fun parseOpf(opfBytes: ByteArray): Triple<Map<String, String>, Map<String, String>, List<String>> {
        val xml = opfBytes.toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())

        // Metadata: extract Dublin Core fields (dc:title, dc:creator, dc:publisher, dc:date).
        // jsoup's XML parser retains "dc:title" tag names literally — match by local name
        // after the last colon.
        val metadata = mutableMapOf<String, String>()
        val metadataEl = doc.selectFirst("metadata")
        if (metadataEl != null) {
            for (child in metadataEl.children()) {
                val localName = child.tagName().substringAfterLast(':')
                val text = child.text().trim()
                if (text.isNotBlank() && localName in setOf("title", "creator", "publisher", "date")) {
                    metadata.putIfAbsent(localName, text)
                }
            }
        }

        val manifest = mutableMapOf<String, String>()
        for (item in doc.select("manifest item")) {
            val mediaType = item.attr("media-type").lowercase()
            if (mediaType.contains("html") || mediaType.contains("xhtml")) {
                val id = item.attr("id")
                val href = item.attr("href")
                if (id.isNotBlank() && href.isNotBlank()) {
                    manifest[id] = href
                }
            }
        }

        val spineIdrefs = doc.select("spine itemref").map { it.attr("idref") }

        return Triple(metadata, manifest, spineIdrefs)
    }
}
