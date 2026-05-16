/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer — Desktop JVM actual.
 *
 * Walks the Flexmark Node tree emitting HoverBlock items.
 *
 * Supported nodes:
 *   - Heading            → HoverBlock.Heading(level, text)
 *   - Paragraph          → HoverBlock.Paragraph(chars.trim())
 *   - FencedCodeBlock    → HoverBlock.CodeBlock(lang?, code)
 *   - IndentedCodeBlock  → HoverBlock.CodeBlock(null, code)
 *
 * Unsupported nodes (Link, Image, Table, BulletList, OrderedList,
 * BlockQuote, ThematicBreak …) → HoverBlock.FallbackText(chars.trim())
 *
 * The Flexmark Parser singleton is built once and shared (thread-safe:
 * Flexmark's Parser is immutable after build()).
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: this file — full Flexmark walker.
 *   - Android: identical body in HoverMarkdownRenderer.android.kt.
 *   - iOS/Wasm: honest FallbackText stub.
 *
 * Mutation procedure (CONST-035):
 *   Replace render() body with:
 *     return if (markdown.isEmpty()) emptyList()
 *            else listOf(HoverBlock.FallbackText(markdown))
 *   Re-run desktopTest — ≥ 4 tests FAIL.
 *   Revert → all 8 PASS.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.IndentedCodeBlock
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node

actual object HoverMarkdownRenderer {

    private val parser: Parser = Parser.builder().build()

    actual fun render(markdown: String): List<HoverBlock> {
        if (markdown.isEmpty()) return emptyList()
        val doc = parser.parse(markdown)
        return buildList {
            var node: Node? = doc.firstChild
            while (node != null) {
                when (node) {
                    is Heading -> add(
                        HoverBlock.Heading(
                            level = node.level,
                            text = node.text.toString().trim(),
                        )
                    )
                    is Paragraph -> add(
                        HoverBlock.Paragraph(
                            text = node.chars.toString().trim(),
                        )
                    )
                    is FencedCodeBlock -> add(
                        HoverBlock.CodeBlock(
                            lang = node.info.toString().trim().ifEmpty { null },
                            code = node.contentChars.toString(),
                        )
                    )
                    is IndentedCodeBlock -> add(
                        HoverBlock.CodeBlock(
                            lang = null,
                            code = node.contentChars.toString(),
                        )
                    )
                    else -> {
                        val raw = node.chars.toString().trim()
                        if (raw.isNotEmpty()) add(HoverBlock.FallbackText(raw))
                    }
                }
                node = node.next
            }
        }
    }
}
