/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporter — expect class declaration.
 *
 * Desktop (JVM) actual uses javax.swing.text.rtf.RTFEditorKit to load
 * RTF bytes into a DefaultStyledDocument, then walks its element tree
 * to emit Markdown (paragraph text + bold/italic spans).
 *
 * Android does NOT ship javax.swing.text.rtf.RTFEditorKit — it is a
 * Java SE class absent from the Android SDK. Android actual returns
 * ImportError.NotSupported with tracker #iter-64-android-rtf-no-swing.
 *
 * iOS and Wasm stubs return ImportError.NotSupported per CONST-035
 * honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports Rich Text Format (.rtf) files and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via javax.swing.text.rtf.RTFEditorKit.
 * - Android:  returns [ImportError.NotSupported] — javax.swing absent from Android SDK
 *             (tracker: #iter-64-android-rtf-no-swing).
 * - iOS:      returns [ImportError.NotSupported] (App Store sandbox / no JVM).
 * - Web:      returns [ImportError.NotSupported] (Wasm browser sandbox / no JVM).
 */
expect class RtfImporter() : DocumentImporter
