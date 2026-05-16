/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporter — Android honest stub.
 *
 * javax.swing.text.rtf.RTFEditorKit is a Java SE class and is NOT
 * shipped with the Android SDK. Attempting to reference it at runtime
 * on Android would throw NoClassDefFoundError.
 *
 * Returns ImportError.NotSupported per CONST-035 honest-degradation policy.
 * Tracker: #iter-64-android-rtf-no-swing
 *
 * Long-term path: integrate a pure-Kotlin / Android-safe RTF parser
 * (e.g. a stripped RTF tokeniser or an AOSP-compatible alternative)
 * once one reaches sufficient maturity for the Yole quality bar.
 *#######################################################*/
package digital.vasic.yole.import_

actual class RtfImporter actual constructor() : DocumentImporter {

    override val supportedExtensions: Set<String> = setOf("rtf")

    // #iter-64-android-rtf-no-swing: javax.swing absent from Android SDK.
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.failure(ImportError.NotSupported("rtf", "Android"))
}
