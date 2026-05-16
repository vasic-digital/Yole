/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverBlock — Compose-renderable representation
 * of a parsed hover Markdown block. Designed for compact popup
 * rendering: skips link/image/table (rendered as FallbackText).
 *
 * Cross-platform (CONST-037):
 *   - All targets: pure Kotlin sealed class; no platform dependencies.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Sealed hierarchy representing a single block-level element parsed from
 * LSP hover Markdown. Phase 6 [HoverPopup] renders these into Compose UI.
 *
 * Design principles:
 * - Only the block types realistically found in LSP hover output are
 *   represented as dedicated variants. Everything else (links, images,
 *   tables, blockquotes) falls through to [FallbackText] so the popup
 *   always shows *something* rather than silently dropping content.
 * - [InlineCodeSpan] is included for completeness even though Flexmark
 *   surfaces inline spans only inside paragraphs; callers that walk
 *   paragraph children may emit this variant.
 */
sealed class HoverBlock {
    /** A plain-text or inline-formatted paragraph. */
    data class Paragraph(val text: String) : HoverBlock()

    /** An ATX or setext heading (level 1–6). */
    data class Heading(val level: Int, val text: String) : HoverBlock()

    /** A fenced (```lang) or indented code block. */
    data class CodeBlock(val lang: String?, val code: String) : HoverBlock()

    /** A standalone inline code span (uncommon at block level). */
    data class InlineCodeSpan(val text: String) : HoverBlock()

    /** Unsupported block node rendered as its raw source text. */
    data class FallbackText(val raw: String) : HoverBlock()
}
