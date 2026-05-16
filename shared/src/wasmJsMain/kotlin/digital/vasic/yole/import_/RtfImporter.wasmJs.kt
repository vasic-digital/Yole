/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporter — Web/Wasm honest stub.
 *
 * javax.swing.text.rtf.RTFEditorKit requires a JVM; it cannot run
 * inside a browser Wasm sandbox. Returns ImportError.NotSupported
 * per CONST-035 honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

actual class RtfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("rtf")

    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.failure(ImportError.NotSupported("rtf", "Web"))
}
