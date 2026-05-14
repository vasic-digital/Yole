/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 5: Android actual for the platform-specific tokenizer
 * engine. Uses the same io.github.bonede:tree-sitter-ng binding as the
 * Desktop actual; the Java API is identical. However the upstream JAR
 * (versions 0.22.6 and 0.26.6 inspected on 2026-05-14) does NOT bundle
 * an `aarch64-linux-android-tree-sitter.so` — its native binary set is
 * x86_64-linux, aarch64-linux, x86_64-macos, aarch64-macos, x86_64-windows
 * only. Consequence: on a real Android device, the binding's
 * `NativeUtils.loadLib("lib/tree-sitter")` throws UnsatisfiedLinkError
 * at first use.
 *
 * The honest path per CONST-035 (anti-bluff): we trigger that load
 * eagerly in [initialize] and return `Result.failure` so callers can
 * gracefully fall back to plain text per spec §4 "Engine load failed at
 * startup". We do NOT silently swap in a hardcoded token list; that
 * would violate the anti-bluff covenant.
 *
 * Upgrade path (operator action required):
 *   1. Clone github.com/bonede/tree-sitter-ng.
 *   2. Run its Android NDK build (set ANDROID_NDK_HOME, then
 *      `./gradlew :tree-sitter:assembleAndroidNative` per the
 *      project README), producing libtree-sitter.so + libtree-sitter-markdown.so
 *      for arm64-v8a, armeabi-v7a, x86_64.
 *   3. Drop the resulting .so files into androidApp/src/main/jniLibs/<abi>/
 *      and amend this actual to load them via System.loadLibrary("tree-sitter")
 *      before constructing the first TSParser.
 * Once that path is taken, replace this actual's body with the same
 * implementation as TokenizerEngine.desktop.kt — the Java API is
 * identical and the test bar (real tokens from real parser) becomes
 * achievable on Android too.
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
 * Android actual for [TokenizerEngine].
 *
 * Functionally identical to the Desktop actual — same Java binding,
 * same parse-tree walk. Diverges only in [initialize], which
 * eagerly forces the native library load so the failure surfaces
 * at app startup rather than at first highlight attempt.
 *
 * Returns `Result.failure(EngineUnavailableException)` on devices
 * where the bonede tree-sitter-ng JAR does not bundle a compatible
 * Android NDK build (the current upstream situation). Higher layers
 * MUST inspect the result and disable highlighting on failure.
 */
actual class TokenizerEngine actual constructor() {
    private val initialized = AtomicBoolean(false)
    private val loadedGrammars = ConcurrentHashMap<String, TSLanguage>()

    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (initialized.get()) return@withContext Result.success(Unit)
        runCatching {
            // Force NativeUtils.loadLib() to fire now. On stock upstream
            // JARs this throws UnsatisfiedLinkError on Android because no
            // aarch64-linux-android-tree-sitter.so is bundled. We let
            // the exception propagate up to runCatching → Result.failure.
            val probeParser = TSParser()
            check(TSParser.ts_parser_new() != 0L) {
                "tree-sitter native library failed to allocate a parser on Android"
            }
            // Sanity check: probe the markdown grammar too. Same JAR-bundling
            // limitation applies to libtree-sitter-markdown.so.
            @Suppress("UNUSED_VARIABLE")
            val probeLang = TreeSitterMarkdown()
            @Suppress("UNUSED_VARIABLE")
            val unused = probeParser
            initialized.set(true)
        }
    }

    actual suspend fun loadGrammar(lang: String): Unit = withContext(Dispatchers.IO) {
        check(initialized.get()) {
            "TokenizerEngine.initialize() must be called first (and must succeed)"
        }
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
            check(initialized.get()) {
                "TokenizerEngine.initialize() must be called first (and must succeed)"
            }
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
