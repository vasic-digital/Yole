/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 4: HtmlImporter — expect class declaration.
 *
 * JVM actuals (Desktop + Android) parse HTML via jsoup and convert to
 * Markdown via FlexmarkHtmlConverter (flexmark-html2md-converter 0.64.8,
 * BSD-2-Clause). iOS and Wasm actuals return Result.failure(ImportError.NotSupported)
 * per CONST-035 honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports HTML (.html / .htm) files and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via jsoup + FlexmarkHtmlConverter.
 * - Android (JVM): full implementation via jsoup + FlexmarkHtmlConverter.
 * - iOS:  returns [ImportError.NotSupported] (Kotlin/Native, no JVM).
 * - Web:  returns [ImportError.NotSupported] (Wasm browser sandbox).
 */
expect class HtmlImporter() : DocumentImporter
