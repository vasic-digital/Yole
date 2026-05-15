/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: FoldRange value class — one foldable region in a
 * source document, expressed in BOTH line-number form (for fold-gutter
 * rendering) and byte-offset form (for in-memory tree manipulation).
 *#######################################################*/
package digital.vasic.yole.language.affordance

/**
 * A foldable region in a source-code document, produced by running
 * the bundled `folds.scm` query against a parsed tree.
 *
 * @property startLine 0-based index of the first line in the fold.
 * @property endLine   0-based index of the last line in the fold (inclusive).
 * @property startByte byte offset of the first byte of the captured node.
 * @property endByte   byte offset one past the last byte of the captured node.
 */
data class FoldRange(
    val startLine: Int,
    val endLine: Int,
    val startByte: Int,
    val endByte: Int,
)
