/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporter — Web/Wasm honest stub.
 *
 * Apache PDFBox requires a JVM; it cannot run inside a browser Wasm sandbox.
 * Returns ImportError.NotSupported per CONST-035 honest-degradation policy.
 *
 * Long-term path: integrate pdf.js (browser-native PDF renderer) via a
 * Kotlin/Wasm JS interop layer. Tracked in docs/CONTINUATION.md.
 *#######################################################*/
package digital.vasic.yole.import_

actual class PdfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("pdf")

    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.failure(ImportError.NotSupported("pdf", "Web"))
}
