/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Android (JVM) actual for [FoldQueryRunner].
 *
 * Functionally identical to the Desktop actual after the bonede-JAR
 * repackage step substitutes Android NDK shared libraries for the
 * linux-gnu ones the upstream bonede JAR ships (see iter-57 Phase 5
 * `#android-tree-sitter-ndk-so-missing`). The same TSQuery /
 * TSQueryCursor JNI surface is reachable on Android once the native
 * library is loaded.
 *
 * Pipeline + threading + anti-bluff anchor: see desktop twin
 * (`shared/src/desktopMain/.../FoldQueryRunner.desktop.kt`).
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

actual class FoldQueryRunner actual constructor() {
    actual suspend fun foldRangesFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<FoldRange> = withContext(Dispatchers.Default) {
        val tsLang = engine.jvmGrammarFor(langId)
            ?: error(
                "grammar `$langId` is not loaded -- call engine.loadGrammar first",
            )
        val querySource = ScmQueryLoader.load(langId, "folds")
        val tree = engine.jvmParseTree(text, langId)
        val query = TSQuery(tsLang, querySource)
        val cursor = TSQueryCursor()
        cursor.exec(query, tree.rootNode)
        val out = mutableListOf<FoldRange>()
        val match = TSQueryMatch()
        while (cursor.nextMatch(match)) {
            for (capture in match.captures) {
                val captureName = query.getCaptureNameForId(capture.index)
                if (captureName == "fold") {
                    val node = capture.node
                    val startPoint = node.startPoint
                    val endPoint = node.endPoint
                    out += FoldRange(
                        startLine = startPoint.row,
                        endLine = endPoint.row,
                        startByte = node.startByte,
                        endByte = node.endByte,
                    )
                }
            }
        }
        out
    }
}
