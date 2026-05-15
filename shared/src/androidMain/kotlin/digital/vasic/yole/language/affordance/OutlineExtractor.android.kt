/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Android (JVM) actual for [OutlineExtractor].
 *
 * Functionally identical to the Desktop actual after the bonede-JAR
 * repackage step substitutes Android NDK shared libraries for the
 * linux-gnu ones the upstream bonede JAR ships. See desktop twin for
 * pipeline + anti-bluff anchor commentary.
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
                "grammar `$langId` is not loaded; call engine.loadGrammar first",
            )
        val querySource = ScmQueryLoader.load(langId, "outline")
        val tree = engine.jvmParseTree(text, langId)
        val query = TSQuery(tsLang, querySource)
        val cursor = TSQueryCursor()
        cursor.exec(query, tree.rootNode)
        val out = mutableListOf<OutlineItem>()
        val match = TSQueryMatch()
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
