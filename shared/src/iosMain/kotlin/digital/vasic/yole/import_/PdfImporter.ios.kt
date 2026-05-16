/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporter — iOS honest stub.
 *
 * Apache PDFBox requires a JVM; it cannot run on Kotlin/Native (iOS).
 * Returns ImportError.NotSupported per CONST-035 honest-degradation policy.
 *
 * Long-term path: integrate PDFKit (Apple-native PDF engine) via a
 * Kotlin/Native interop layer once the Yole iOS surface reaches general
 * availability. Tracked in docs/CONTINUATION.md.
 *#######################################################*/
package digital.vasic.yole.import_

actual class PdfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("pdf")

    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.failure(ImportError.NotSupported("pdf", "iOS"))
}
