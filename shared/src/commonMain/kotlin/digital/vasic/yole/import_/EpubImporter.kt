/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 8: EpubImporter — expect class declaration.
 *
 * JVM actuals (Desktop + Android) implement roll-own EPUB parsing using:
 *   - java.util.zip.ZipInputStream — enumerate ZIP entries (no new deps).
 *   - org.jsoup.Jsoup — parse container.xml and OPF manifest/spine (jsoup
 *     is already a transitive dep via HtmlImporter Phase 4).
 *   - HtmlImporter() — convert each spine chapter's XHTML to Markdown
 *     (reuses existing infrastructure, no new deps).
 * No epublib or other third-party EPUB library is used (Phase 0 §6 finding:
 * epublib last commit 2021, abandoned).
 *
 * iOS and Wasm stubs return ImportError.NotSupported per CONST-035
 * honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports EPUB (.epub) files and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via ZipInputStream + jsoup + HtmlImporter.
 * - Android (JVM): full implementation via ZipInputStream + jsoup + HtmlImporter.
 * - iOS:  returns [ImportError.NotSupported] (Kotlin/Native, no JVM).
 * - Web:  returns [ImportError.NotSupported] (Wasm browser sandbox).
 */
expect class EpubImporter() : DocumentImporter
