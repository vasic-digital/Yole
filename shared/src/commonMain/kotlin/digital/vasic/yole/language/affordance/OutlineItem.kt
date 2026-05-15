/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: OutlineItem value class -- one entry in a
 * document's outline/breadcrumb, produced by running the bundled
 * `outline.scm` query against a parsed source tree.
 *#######################################################*/
package digital.vasic.yole.language.affordance

/**
 * One entry in a source document's outline (the breadcrumb / Go-To-Symbol
 * UI), produced by running the bundled `outline.scm` query against the
 * parsed tree. The vendored `outline.scm` files come from
 * `helix-editor/helix/runtime/queries/<lang>/tags.scm` (MPL-2.0) per
 * research-report.md §2 -- using github-linguist-compatible capture
 * names like `@definition.function`, `@definition.class`,
 * `@definition.section` for headings.
 *
 * @property name      the captured text content of the symbol (e.g.,
 *                     `Heading`, `foo`, `MyClass`).
 * @property kind      the capture name's tail after `definition.`
 *                     (e.g., `section`, `function`, `class`, `method`,
 *                     `field`). For markdown headings this is
 *                     `section` per helix's `tags.scm`.
 * @property startByte byte offset of the first byte of the captured node.
 * @property endByte   byte offset one past the last byte of the captured node.
 * @property startLine 0-based index of the first line of the symbol.
 */
data class OutlineItem(
    val name: String,
    val kind: String,
    val startByte: Int,
    val endByte: Int,
    val startLine: Int = -1,
)
