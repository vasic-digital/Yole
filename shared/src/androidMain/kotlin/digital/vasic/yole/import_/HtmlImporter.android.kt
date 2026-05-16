/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 4: HtmlImporter — Android (JVM) actual.
 *
 * Identical implementation to the Desktop actual; both source sets target
 * JVM and share jsoup + flexmark-html2md-converter. A future refactor may
 * introduce a jvmMain intermediate source set to de-duplicate, but the
 * current KMP configuration has no jvmMain.
 *
 * CancellationException is always rethrown.
 *#######################################################*/
package digital.vasic.yole.import_

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.vladsch.flexmark.util.data.MutableDataSet
import kotlinx.coroutines.CancellationException
import org.jsoup.Jsoup

actual class HtmlImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("html", "htm")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> {
        return try {
            val markdown = convertHtmlToMarkdown(bytes)
            Result.success(
                ImportedDocument(
                    sourceFormat = "html",
                    markdown = markdown,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ImportError.Malformed("html", e))
        }
    }

    private fun convertHtmlToMarkdown(bytes: ByteArray): String {
        val html = bytes.toString(Charsets.UTF_8)
        val document = Jsoup.parse(html, "")
        // Disable Setext-style headings (=== / ---) — prefer ATX (# / ##) for
        // consistency with Yole's Markdown parser which expects ATX headings.
        val options = MutableDataSet()
            .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
        val converter = FlexmarkHtmlConverter.builder(options).build()
        return converter.convert(document).trim()
    }
}
