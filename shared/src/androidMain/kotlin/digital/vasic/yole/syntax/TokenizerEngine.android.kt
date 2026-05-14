/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 — Android actual for the platform-specific tokenizer engine.
 *
 * Backed by Tree-Sitter via JNI through the io.github.bonede:tree-sitter-ng
 * binding. The upstream JAR does NOT publish Android NDK shared libraries
 * — it ships only x86_64/aarch64 linux-gnu/macos and x86_64-windows .so
 * files — so shared/build.gradle.kts:
 *
 *   1. Builds libtree-sitter.so and libtree-sitter-markdown.so for the
 *      three Android ABIs (arm64-v8a, armeabi-v7a, x86_64) — the prebuilt
 *      binaries are checked in under
 *      shared/native/android-tree-sitter/<abi>/ and surfaced through the
 *      standard Android jniLibs convention so AGP packages them at
 *      <apk>/lib/<abi>/lib*.so.
 *   2. Repackages the bonede tree-sitter JAR with a drop-in replacement
 *      `org.treesitter.utils.NativeUtils.class` (Yole source at
 *      shared/native/android-tree-sitter/java/) that, on Android (detected
 *      via java.vm.vendor / Dalvik / ART), routes loadLib(name) through
 *      `System.loadLibrary` rather than the bonede classpath-extract +
 *      System.load(absPath) flow. The Android linker resolves the call
 *      to the correct ABI .so packaged in step 1 automatically.
 *
 * Closes the iter-57 Phase 5 defect `#android-tree-sitter-ndk-so-missing`.
 *
 * Architecture coverage: arm64-v8a, armeabi-v7a, x86_64.
 *
 * After the repackage step, the Android tokenize() walk is bit-for-bit
 * identical with the Desktop JVM actual.
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
 * Functionally identical to the Desktop actual after the
 * `repackageBonedeJarsForAndroid` Gradle task substitutes Android NDK
 * .so files for the linux-gnu ones the bonede upstream JAR ships.
 *
 * The Android NDK build is sourced from
 * `shared/native/android-tree-sitter/<abi>/lib{tree-sitter,tree-sitter-markdown}.so`
 * — these binaries are checked into the repository and rebuilt when
 * the upstream tree-sitter / tree-sitter-markdown grammar versions in
 * `gradle/libs.versions.toml` change.
 */
actual class TokenizerEngine actual constructor() {
    private val initialized = AtomicBoolean(false)
    private val loadedGrammars = ConcurrentHashMap<String, TSLanguage>()

    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (initialized.get()) return@withContext Result.success(Unit)
        runCatching {
            // Touch TSParser to fire its static initialiser, which calls
            // NativeUtils.loadLib("lib/tree-sitter"). Our Yole replacement
            // NativeUtils (patched into the bonede JAR by
            // repackageTreeSitterJarForAndroid) detects Android at static-
            // init time and routes the load through
            // System.loadLibrary("tree-sitter") — picking up the .so
            // packaged at <apk>/lib/<abi>/libtree-sitter.so by AGP via
            // the jniLibs srcDirs entry in shared/build.gradle.kts.
            //
            // If the replacement step was somehow skipped (e.g. someone
            // consumed the raw bonede artefact directly), the original
            // bonede code path on Android would attempt to dlopen the
            // bundled glibc binary and throw UnsatisfiedLinkError; we
            // capture that with runCatching → Result.failure so callers
            // can fall back to plain text per spec §4 — no fake tokens.
            check(TSParser.ts_parser_new() != 0L) {
                "tree-sitter native library failed to allocate a parser on Android"
            }
            // Sanity-check the grammar load too. `TreeSitterMarkdown.version()`
            // is a public JNI method on TSLanguage and exercising it confirms
            // the .so really resolved on the device.
            val probeLang = TreeSitterMarkdown()
            require(probeLang.version() > 0) {
                "tree-sitter-markdown grammar version probe returned ${probeLang.version()}"
            }
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

    /**
     * Pre-order DFS over the parse tree. For each leaf node we emit a
     * Token spanning the same byte range with the node's grammar type
     * as the scope. Bit-for-bit identical to the Desktop actual to keep
     * the cross-platform highlight output stable.
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
