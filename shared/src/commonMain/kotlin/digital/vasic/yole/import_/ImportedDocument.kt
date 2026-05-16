/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 1: ImportedDocument + ImportWarning.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Severity level for an [ImportWarning] produced during document conversion.
 */
enum class Severity { Info, Warning }

/**
 * A diagnostic message emitted by a [DocumentImporter] during conversion.
 *
 * @property severity How urgent the message is.
 * @property message Human-readable description.
 * @property pageOrSection Optional location hint (page number or section title).
 */
data class ImportWarning(
    val severity: Severity,
    val message: String,
    val pageOrSection: String? = null,
)

/**
 * The result of a successful document import.
 *
 * @property sourceFormat Short format identifier, e.g. "docx", "pdf".
 * @property markdown The converted Markdown text.
 * @property assetsDir Optional directory where extracted images were written.
 * @property warnings Non-fatal issues discovered during conversion.
 */
data class ImportedDocument(
    val sourceFormat: String,
    val markdown: String,
    val assetsDir: String? = null,
    val warnings: List<ImportWarning> = emptyList(),
)
