/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 3: DocxImporter — expect class declaration.
 *
 * JVM actuals (Desktop + Android) use Apache POI poi-ooxml-lite to
 * parse .docx files and produce Markdown via the Phase-2 conversion
 * helpers (HeadingDetector, TableConverter, ImageExtractor, LinkPreserver).
 * iOS and Wasm actuals return Result.failure(ImportError.NotSupported)
 * per CONST-035 honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports Microsoft Word (.docx) files and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via Apache POI XWPFDocument.
 * - Android (JVM): full implementation via Apache POI XWPFDocument.
 * - iOS:  returns [ImportError.NotSupported] (App Store sandbox).
 * - Web:  returns [ImportError.NotSupported] (Wasm browser sandbox).
 */
expect class DocxImporter() : DocumentImporter
