/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 7: iOS actual for the platform-specific tokenizer
 * engine.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ PHASE 7 BLOCKED — pre-existing iOS K/N baseline defect          │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ This file remains an honest `NotImplementedError` stub because  │
 * │ `:shared:compileKotlinIosArm64` cannot be built today — the     │
 * │ sibling submodule `:Document-KMP:compileKotlinIosArm64` fails   │
 * │ at compile with                                                  │
 * │                                                                 │
 * │   /Users/milosvasic/Projects/Document-KMP/                      │
 * │     src/iosMain/kotlin/digital/vasic/document/                  │
 * │     Document.ios.kt:9:50                                        │
 * │   This declaration needs opt-in. Its usage must be marked       │
 * │   with '@kotlinx.cinterop.ExperimentalForeignApi' or            │
 * │   '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'      │
 * │                                                                 │
 * │   /Users/milosvasic/Projects/Document-KMP/                      │
 * │     src/iosMain/kotlin/digital/vasic/document/                  │
 * │     Document.ios.kt:10:30                                       │
 * │   Unresolved reference 'objectForKey'.                          │
 * │                                                                 │
 * │ Yole consumes Document-KMP via `includeBuild()` (sibling        │
 * │ composite build); per CONST-038 we cannot patch a sibling       │
 * │ submodule from this repo. Phase 7 is BLOCKED on that            │
 * │ upstream fix — see KNOWN_DEFECTS.md entry                       │
 * │ `#phase-7-blocked-on-ios-baseline`.                             │
 * │                                                                 │
 * │ Per CONST-035 anti-bluff covenant: we DO NOT emit fake tokens.  │
 * │ `initialize()` honestly returns `Result.failure`; the editor    │
 * │ falls back to plain text on iOS per spec §4 "Engine load        │
 * │ failed at startup".                                             │
 * │                                                                 │
 * │ Scaffold present for unblocking:                                │
 * │   - `shared/src/iosMain/cinterop/tree-sitter.def` (commented    │
 * │     directives describing the linking strategy from Phase 0     │
 * │     research §2.2/§2.3/§2.4).                                   │
 * │   - This stub's contract matches the JVM/Wasm actuals so the    │
 * │     real implementation lands by replacing this file alone.     │
 * │                                                                 │
 * │ Implementation guidance (when the upstream is fixed):           │
 * │   1. Uncomment the directives in `tree-sitter.def`.              │
 * │   2. Vendor `libtree-sitter.a` + `libtree-sitter-markdown.a`    │
 * │      under `shared/src/iosMain/nativeLibs/{ios_arm64,            │
 * │      ios_simulator_arm64,macos_arm64}/` (operator spike per     │
 * │      Phase 0 §2.6).                                             │
 * │   3. Wire the cinterop into `shared/build.gradle.kts`           │
 * │      (`cinterops.create("tree_sitter") { ... }`).               │
 * │   4. Replace `initialize()` / `loadGrammar()` / `tokenize()`    │
 * │      bodies below with `ts_parser_new`, `ts_parser_set_language`,│
 * │      `ts_parser_parse_string`, `ts_tree_root_node`, walking the │
 * │      tree via `ts_tree_cursor_*`. Return `List<Token>` with     │
 * │      `startByte`, `endByte`, and tree-sitter node-type scope.   │
 * │                                                                 │
 * └─────────────────────────────────────────────────────────────────┘
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * iOS placeholder actual for [TokenizerEngine]. All calls except
 * [initialize] throw `IllegalStateException` because the engine
 * never successfully initializes — [initialize] returns
 * `Result.failure` so callers gracefully fall back to plain text.
 *
 * BLOCKED on `#phase-7-blocked-on-ios-baseline` (pre-existing
 * `:Document-KMP:compileKotlinIosArm64` failure, CONST-038). Real
 * Kotlin/Native cinterop binding lands as soon as the upstream
 * fix unblocks `:shared:compileKotlinIosArm64`.
 */
actual class TokenizerEngine actual constructor() {
    actual suspend fun initialize(): Result<Unit> =
        Result.failure(
            NotImplementedError(
                "TokenizerEngine iOS actual blocked on " +
                    "#phase-7-blocked-on-ios-baseline (Document-KMP " +
                    "iOS compile failure, CONST-038 sibling submodule). " +
                    "See KNOWN_DEFECTS.md.",
            ),
        )

    actual suspend fun loadGrammar(lang: String): Unit =
        error(
            "TokenizerEngine iOS actual blocked on " +
                "#phase-7-blocked-on-ios-baseline",
        )

    actual suspend fun tokenize(text: String, lang: String): List<Token> =
        error(
            "TokenizerEngine iOS actual blocked on " +
                "#phase-7-blocked-on-ios-baseline",
        )

    actual fun isGrammarLoaded(lang: String): Boolean = false
}
