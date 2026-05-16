/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer — Android JVM actual.
 *
 * Identical Flexmark walker to the Desktop actual. Flexmark runs on
 * the Android JVM (minSdk 21+, JDK bytecode level 11).
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this file — full Flexmark walker.
 *   - Desktop: identical body in HoverMarkdownRenderer.desktop.kt.
 *   - iOS/Wasm: honest FallbackText stub.
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
