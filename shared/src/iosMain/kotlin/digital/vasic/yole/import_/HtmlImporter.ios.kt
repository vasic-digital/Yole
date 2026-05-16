/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 4: HtmlImporter — iOS honest stub.
 *
 * jsoup and flexmark-html2md-converter require a JVM; they are not
 * available on Kotlin/Native iOS. Returns ImportError.NotSupported
 * per CONST-035 honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

actual class HtmlImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("html", "htm")

    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.failure(ImportError.NotSupported("html", "iOS"))
}
