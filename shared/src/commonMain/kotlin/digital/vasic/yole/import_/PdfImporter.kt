/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 7: PdfImporter — expect class declaration.
 *
 * PDF import requires per-platform library splits:
 *
 *   Desktop (JVM): Apache PDFBox 3.0.7 (org.apache.pdfbox:pdfbox).
 *     Entry point: Loader.loadPDF(bytes) — the 3.x API replaces the 2.x
 *     PDDocument.load(InputStream) call. Heading detection uses a font-size
 *     histogram: body-text size = mode; outlier clusters → H1-H4 via
 *     HeadingDetector.headingLevelByFontSize.
 *
 *   Android (JVM): pdfbox-android 2.0.27.0 (com.tom-roush:pdfbox-android).
 *     An Android-safe port of PDFBox 2.0.x; replaces AWT with Android Bitmap
 *     APIs, ships no Swing/AWT dependencies. Entry point: PDDocument.load(InputStream)
 *     (2.x API — differs from 3.x). Same heading-detection + emit logic adapted
 *     to 2.x class paths.
 *
 *   iOS + Wasm: NotSupported stubs (no JVM, no PDFBox).
 *
 * CONST-035: honest-degradation on unsupported platforms.
 * CONST-037: all four platforms reasoned about above.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports PDF files (.pdf) and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via Apache PDFBox 3.0.7
 *   (Loader.loadPDF API; font-size histogram heading detection).
 * - Android (JVM): full implementation via pdfbox-android 2.0.27.0
 *   (PDDocument.load API; same heading-detection logic, 2.x class paths).
 * - iOS:  returns [ImportError.NotSupported] (App Store sandbox / no JVM).
 * - Web:  returns [ImportError.NotSupported] (Wasm browser sandbox / no JVM).
 */
expect class PdfImporter() : DocumentImporter
