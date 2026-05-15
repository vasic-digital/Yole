/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Desktop (JVM) actual for [FoldQueryRunner].
 *
 * Backed by the io.github.bonede:tree-sitter binding's query API:
 *   - org.treesitter.TSQuery
 *   - org.treesitter.TSQueryCursor
 *   - org.treesitter.TSQueryMatch
 *   - org.treesitter.TSQueryCapture
 *
 * Lookup verified by inspecting the upstream JAR
 * (`tree-sitter-0.22.6-...jar` -> org/treesitter/TSQuery.class +
 * TSQueryCursor.class). API surface matches research-report.md §6.1.
 *
 * Pipeline:
 *   1. Engine has already initialised + loaded the markdown grammar.
 *   2. [TokenizerEngine.jvmParseTree] re-parses [text] with that grammar
 *      and returns a fresh [TSTree].
 *   3. [ScmQueryLoader] returns the bundled `folds.scm` query source.
 *   4. We compile that source into a [TSQuery] against the loaded
 *      [TSLanguage], allocate a [TSQueryCursor], and run the query
 *      against the tree's root node via the binding's `exec` method.
 *   5. Iterate `cursor.nextMatch(match)` -- each successful call mutates
 *      the supplied [TSQueryMatch] in place with the captures. For
 *      every capture named `fold`, emit a [FoldRange] with the
 *      captured node's start/end byte + row offsets.
 *
 * Threading: the bonede TSParser/TSTree pair is NOT thread-safe per
 * upstream, so we re-parse inside a Dispatchers.Default scope and never
 * share the tree across coroutines. The TSQuery is allocated per call
 * for the same reason.
 *
 * Anti-bluff anchor (CONST-035): replacing this body with
 * `return emptyList()` would cause the
 * `markdownHeadingProducesFoldRange` test in
 * `shared/src/desktopTest/.../FoldQueryRunnerTest.kt` to FAIL because
 * the test input `# Heading\n\nLine 1\nLine 2\n` produces a `(section)`
 * node which the bundled `folds.scm` captures as `@fold`.
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
                "grammar `$langId` is not loaded -- call engine.loadGrammar(\"$langId\") first",
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
