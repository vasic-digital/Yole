/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Desktop (JVM) actual for [OutlineExtractor].
 *
 * Identical pipeline to [FoldQueryRunner.desktop.kt] but consumes the
 * `outline.scm` query (vendored from helix's `tags.scm`) and emits
 * [OutlineItem]s instead of [FoldRange]s.
 *
 * Capture name convention: helix's `tags.scm` uses
 * `@definition.<kind>` (e.g., `@definition.section` for markdown
 * headings, `@definition.function` for function declarations). We
 * extract `<kind>` by splitting on the first `.` -- the prefix
 * `definition` is implicit for outline items.
 *
 * Name extraction: the captured node's byte range covers the entire
 * heading (including the `#` markers + content + trailing newline).
 * We slice the original [text] by `node.startByte..node.endByte` and
 * trim whitespace + leading `#` characters to produce a clean display
 * name. For richer language grammars (e.g., Kotlin/Java) where the
 * capture lands directly on the identifier node, this trim is a no-op.
 *
 * Anti-bluff anchor (CONST-035): same as [FoldQueryRunner.desktop.kt].
 * Stubbing this body to `return emptyList()` would cause the
 * `markdownHeadingsProduceOutlineItems` test to FAIL.
 *
 *########################################################*/
package digital.vasic.yole.language.affordance

import digital.vasic.yole.language.ScmQueryLoader
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor
import org.treesitter.TSQueryMatch

private const val DEFINITION_PREFIX = "definition."

actual class OutlineExtractor actual constructor() {
    actual suspend fun outlineFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<OutlineItem> = withContext(Dispatchers.Default) {
        val tsLang = engine.jvmGrammarFor(langId)
            ?: error(
                "grammar `$langId` is not loaded -- call engine.loadGrammar first",
            )
        val querySource = ScmQueryLoader.load(langId, "outline")
        val tree = engine.jvmParseTree(text, langId)
        val query = TSQuery(tsLang, querySource)
        val cursor = TSQueryCursor()
        cursor.exec(query, tree.rootNode)
        val out = mutableListOf<OutlineItem>()
        val match = TSQueryMatch()
        // Encode text to UTF-8 bytes once so we can slice by tree-sitter's
        // byte offsets correctly even with multi-byte chars in the source.
        val textBytes = text.encodeToByteArray()
        while (cursor.nextMatch(match)) {
            for (capture in match.captures) {
                val captureName = query.getCaptureNameForId(capture.index) ?: continue
                if (!captureName.startsWith(DEFINITION_PREFIX)) continue
                val kind = captureName.substring(DEFINITION_PREFIX.length)
                val node = capture.node
                val startByte = node.startByte
                val endByte = node.endByte
                val safeStart = startByte.coerceIn(0, textBytes.size)
                val safeEnd = endByte.coerceIn(safeStart, textBytes.size)
                val raw = textBytes.decodeToString(safeStart, safeEnd)
                val name = raw.trim().trimStart('#').trim()
                out += OutlineItem(
                    name = name,
                    kind = kind,
                    startByte = startByte,
                    endByte = endByte,
                    startLine = node.startPoint.row,
                )
            }
        }
        out
    }
}
