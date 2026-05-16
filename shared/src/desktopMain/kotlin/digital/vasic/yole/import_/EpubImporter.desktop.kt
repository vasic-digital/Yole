/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 8: EpubImporter — Desktop (JVM) actual.
 *
 * Roll-own EPUB parsing pipeline (no new deps — uses existing jsoup + JVM stdlib):
 *
 *   1. ZipInputStream(ByteArrayInputStream(bytes)) — enumerate all ZIP entries,
 *      collecting each entry's bytes into a name → bytes map.
 *   2. Read META-INF/container.xml → jsoup.parse() → locate
 *      <rootfile full-path="..."> → extract the OPF path.
 *   3. Read OPF file bytes → jsoup.parse() → extract:
 *        - manifest: <item id="..." href="..." media-type="..."/> → id → href map.
 *        - spine:    <itemref idref="..."/> in document order → ordered list of idrefs.
 *   4. For each spine item: resolve href relative to the OPF directory,
 *      look up entry bytes → invoke HtmlImporter().import() → collect markdown.
 *   5. Concatenate chapter markdown with "\n\n---\n\n" (Markdown HR) as separator.
 *   6. Aggregate warnings from all chapters.
 *   7. Return Result.success(ImportedDocument) or Result.failure(ImportError.Malformed).
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

        // Step 3 — parse OPF file
        val opfBytes = entries[opfPath]
            ?: error("OPF file '$opfPath' not found in EPUB archive")
        val (manifest, spineIdrefs) = parseOpf(opfBytes)

        // OPF directory is used to resolve relative hrefs
        val opfDir = opfPath.substringBeforeLast("/", missingDelimiterValue = "")

        // Step 4 — import each spine chapter via HtmlImporter
        val htmlImporter = HtmlImporter()
        val chapterMarkdowns = mutableListOf<String>()
        val allWarnings = mutableListOf<ImportWarning>()

        for (idref in spineIdrefs) {
            val href = manifest[idref] ?: continue
            val entryPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
            // Normalise path separators (some EPUBs use backslash)
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
        val combinedMarkdown = chapterMarkdowns.joinToString(separator = "\n\n---\n\n")

        return ImportedDocument(
            sourceFormat = "epub",
            markdown = combinedMarkdown,
            warnings = allWarnings,
        )
    }

    /**
     * Read every entry from [bytes] (a ZIP/EPUB archive) into a map of
     * normalised entry name (forward slashes) → raw bytes.
     */
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

    /**
     * Parse container.xml and return the full-path attribute of the first
     * <rootfile> element, which points to the OPF package document.
     *
     * Example container.xml:
     * ```xml
     * <container>
     *   <rootfiles>
     *     <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
     *   </rootfiles>
     * </container>
     * ```
     */
    private fun extractOpfPath(containerXmlBytes: ByteArray): String {
        val xml = containerXmlBytes.toString(Charsets.UTF_8)
        // jsoup can parse XML with parser(org.jsoup.parser.Parser.xmlParser())
        val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
        val rootfile = doc.selectFirst("rootfile")
            ?: error("No <rootfile> element in container.xml")
        val path = rootfile.attr("full-path")
        if (path.isBlank()) error("<rootfile> has no full-path attribute")
        return path
    }

    /**
     * Parse the OPF package document and return:
     *   - manifest: map of item id → href (relative to OPF dir)
     *   - spineIdrefs: ordered list of idrefs from the <spine>
     *
     * Only items with XHTML or HTML media-types are added to the manifest
     * (images and other resources are skipped).
     */
    private fun parseOpf(opfBytes: ByteArray): Pair<Map<String, String>, List<String>> {
        val xml = opfBytes.toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())

        // Manifest: collect id → href for text/xhtml and text/html items
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

        // Spine: ordered idrefs
        val spineIdrefs = doc.select("spine itemref").map { it.attr("idref") }

        return manifest to spineIdrefs
    }
}
