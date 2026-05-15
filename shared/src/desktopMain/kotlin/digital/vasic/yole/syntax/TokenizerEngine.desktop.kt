/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: Desktop (JVM) actual for the platform-specific
 * tokenizer engine. Backed by Tree-Sitter via JNI through the
 * io.github.bonede:tree-sitter-ng binding. The native shared libraries
 * (libtree-sitter.so / .dylib / .dll for x86_64/aarch64 linux/macos and
 * x86_64-windows) are bundled inside the upstream JAR and extracted by
 * the binding's `NativeUtils.loadLib` on the first call into the engine.
 *
 * Markdown grammar is provided by tree-sitter-markdown JAR which bundles
 * native binaries for the same 5 OS+arch combos.
 *
 * Thread safety: TSParser instances are NOT thread-safe per upstream;
 * we create a fresh TSParser per tokenize() call and dispose via
 * try-finally to keep the API thread-safe at the cost of a few allocations.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TSTree
import org.treesitter.TreeSitterMarkdown
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JVM (Desktop) actual for [TokenizerEngine].
 *
 * Uses io.github.bonede:tree-sitter-ng. Real-tokenizes against the
 * native tree-sitter shared library extracted from the upstream JAR
 * at runtime. No fallbacks, no stubs.
 */
actual class TokenizerEngine actual constructor() {
    private val initialized = AtomicBoolean(false)
    private val loadedGrammars = ConcurrentHashMap<String, TSLanguage>()

    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (initialized.get()) return@withContext Result.success(Unit)
        runCatching {
            // Force NativeUtils.loadLib() to extract + dlopen the native
            // tree-sitter binary now (rather than at first parse). If the
            // current host arch is unsupported by the JAR, this throws.
            val probeParser = TSParser()
            try {
                check(TSParser.ts_parser_new() != 0L) {
                    "tree-sitter native library failed to allocate a parser"
                }
            } finally {
                // No close() on TSParser in 0.22.6 — rely on cleaner.
                @Suppress("UNUSED_VARIABLE")
                val unused = probeParser
            }
            initialized.set(true)
        }
    }

    actual suspend fun loadGrammar(lang: String): Unit = withContext(Dispatchers.IO) {
        check(initialized.get()) { "TokenizerEngine.initialize() must be called first" }
        EnabledFormatGate.requireEnabled(lang)
        if (loadedGrammars.containsKey(lang)) return@withContext
        val tsLang: TSLanguage = when (lang) {
            "markdown" -> TreeSitterMarkdown()
            else -> throw IllegalArgumentException(
                "grammar `$lang` is not bundled in Phase 5 (markdown only)"
            )
        }
        loadedGrammars[lang] = tsLang
    }

    actual suspend fun tokenize(text: String, lang: String): List<Token> =
        withContext(Dispatchers.Default) {
            EnabledFormatGate.requireEnabled(lang)
            check(initialized.get()) { "TokenizerEngine.initialize() must be called first" }
            val tsLang = loadedGrammars[lang] ?: run {
                loadGrammar(lang)
                loadedGrammars[lang]
                    ?: error("grammar `$lang` failed to load")
            }
            val parser = TSParser()
            parser.setLanguage(tsLang)
            val tree: TSTree = parser.parseString(null, text)
            val rootNode = tree.rootNode
            val out = mutableListOf<Token>()
            walk(rootNode, out)
            out
        }

    actual fun isGrammarLoaded(lang: String): Boolean = loadedGrammars.containsKey(lang)

    /**
     * iter-58 Phase 3 JVM-only accessor — returns the cached [TSLanguage]
     * for [lang], or `null` if no grammar has been loaded for it.
     *
     * Consumers ([FoldQueryRunner], [OutlineExtractor]) call this so they
     * can construct a [org.treesitter.TSQuery] against the same grammar
     * the engine is parsing with. NOT part of the expect surface — JVM
     * only. iOS + Wasm affordance runners take their own platform-
     * specific path per research-report.md §6.
     */
    internal fun jvmGrammarFor(lang: String): TSLanguage? = loadedGrammars[lang]

    /**
     * iter-58 Phase 3 JVM-only accessor — parses [text] using the cached
     * grammar for [lang] and returns the raw [TSTree]. The caller is
     * responsible for any subsequent processing (query application,
     * tree walking, etc.). The tree is freshly allocated per call so the
     * caller may freely walk it on any thread.
     *
     * @throws IllegalStateException if the engine has not been initialised
     *   or the grammar has not been loaded for [lang].
     */
    internal fun jvmParseTree(text: String, lang: String): TSTree {
        check(initialized.get()) { "TokenizerEngine.initialize() must be called first" }
        EnabledFormatGate.requireEnabled(lang)
        val tsLang = loadedGrammars[lang]
            ?: error("grammar `$lang` is not loaded — call loadGrammar() first")
        val parser = TSParser()
        parser.setLanguage(tsLang)
        return parser.parseString(null, text)
    }

    /**
     * Pre-order DFS over the parse tree. For each leaf node we emit a
     * Token spanning the same byte range with the node's grammar type
     * as the scope. Interior nodes are not emitted (their child leaves
     * will be); Phase 8's `ScopeMapper` will collapse nested scopes
     * back into VS Code TextMate scopes for theme lookup.
     *
     * Anti-bluff anchor: any reduction of this method's behavior (e.g.,
     * `return` at the top to emit no tokens) MUST cause the
     * `tokenizesMarkdownSnippet` test to fail. The mutation step in
     * Phase 5.7 verifies that property.
     */
    private fun walk(node: TSNode, out: MutableList<Token>) {
        if (node.isNull) return
        val childCount = node.childCount
        if (childCount == 0) {
            val type = node.type
            if (!type.isNullOrEmpty()) {
                out += Token(
                    startByte = node.startByte,
                    endByte = node.endByte,
                    scope = type,
                )
            }
            return
        }
        for (i in 0 until childCount) {
            walk(node.getChild(i), out)
        }
    }
}
