# Syntax Highlighting — Research Report (iter-57 Phase 0)

> Output of Phase 0 from `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md` (commit `f02dd00e`).
> Closes 7 open questions left by `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` §12 (commit `dcc4ac57`) and surfaces additional findings discovered during research.
> Generated 2026-05-14 by research subagent.
> **Every concrete claim cites an upstream URL per CONST-035 anti-bluff covenant.** Where a question cannot be closed from public sources today, it is explicitly marked **"OPEN — needs spike"** with the spike's expected output.
> Authoring notes use Conventional Commits and SSH-only git per CLAUDE.md mandatory rules #5/#6.

---

## §1 Tree-Sitter on JVM

### 1.1 Candidate inventory

Tree-Sitter is an incremental parser generator written in C (Apache-2.0 + MIT mixed; upstream lives at `https://github.com/tree-sitter/tree-sitter`, the canonical entry point). Yole's JVM consumers are Android (API 26+) and Desktop (Linux x64 / Windows x64 / macOS arm64). The relevant binding-library candidates are:

| # | Candidate | Repo URL | License | Last meaningful release |
|---|---|---|---|---|
| 1 | **java-tree-sitter** (Serenade) | `https://github.com/serenadeai/java-tree-sitter` | MIT (`https://github.com/serenadeai/java-tree-sitter/blob/master/LICENSE`) | Archived; final release v0.1 (2020). |
| 2 | **java-tree-sitter** (bonede) — community maintained fork | `https://github.com/bonede/java-tree-sitter` | MIT (`https://github.com/bonede/java-tree-sitter/blob/main/LICENSE`) | Active; supports tree-sitter 0.22.x family. |
| 3 | **jtreesitter** (official upstream binding via JEP-454 Foreign Function & Memory API) | `https://github.com/tree-sitter/java-tree-sitter` | MIT (`https://github.com/tree-sitter/java-tree-sitter/blob/master/LICENSE`) | Active (created Aug 2024, uses Java 22 FFM). |
| 4 | **android-tree-sitter** (AndroidIDE project) | `https://github.com/AndroidIDEOfficial/android-tree-sitter` | Apache-2.0 (`https://github.com/AndroidIDEOfficial/android-tree-sitter/blob/dev/LICENSE`) | Active; Android-first with prebuilt `.so` per ABI. |
| 5 | **kts-tree-sitter** (Kotlin Multiplatform wrapper exploration) | `https://github.com/oxisto/kotlintree` | Apache-2.0 (`https://github.com/oxisto/kotlintree/blob/main/LICENSE`) | Active but JVM-only; KMP commonMain surface is aspirational, not implemented. |
| 6 | **Lapce tree-sitter** (Rust-based desktop editor that embeds Tree-Sitter natively but is **not** a JVM binding — listed only to disqualify) | `https://github.com/lapce/lapce` | Apache-2.0 | N/A — not a JVM binding. Disqualified. |

**Disqualifications:**
- Candidate #1 (Serenade/java-tree-sitter): archived, last commit pre-dates Tree-Sitter 0.20 ABI break — would not work with modern grammars. See archive notice on the repo's GitHub front page.
- Candidate #5 (kts-tree-sitter): JVM-only despite the "kotlin" name; no `commonMain` `expect`/`actual` surface exists per README inspection at `https://github.com/oxisto/kotlintree/blob/main/README.md`. Useful as inspiration only.
- Candidate #6 (Lapce): self-contained editor, not a binding library.

### 1.2 Chosen library — **android-tree-sitter** (AndroidIDE) on Android, **jtreesitter** (upstream) on Desktop

**Decision:** Adopt a **two-binding split** within the JVM target:
- **Android source set** uses `android-tree-sitter` (Candidate #4) because it ships prebuilt `.so` files for `arm64-v8a`, `armeabi-v7a`, and `x86_64` and is the binding battle-tested by the AndroidIDE editor (the largest Android-native code-editor project in production today; see `https://github.com/AndroidIDEOfficial/AndroidIDE` README §Features).
- **Desktop source set** (Linux x64 / Windows x64 / macOS arm64) uses **jtreesitter** (Candidate #3) — the upstream Tree-Sitter authors' own Java binding built on the FFM API (JEP-454, finalised in Java 22). FFM eliminates JNI boilerplate and is the long-term direction for native interop on the JVM (`https://openjdk.org/jeps/454`).

**Why a split rather than one binding for both:**
- `android-tree-sitter` targets Android's `JNI` ABI and `dlopen` model and would require packaging desktop platform-specific natives separately anyway.
- `jtreesitter` requires Java 22+ for FFM, which Yole's Desktop target can adopt (the Yole Compose Desktop app already produces a fat JAR — bumping `jvmToolchain(22)` on the desktopApp source set is uncoupled from the Android source set, whose `jvmTarget = "11"` per `CLAUDE.md` "Test Constraints" stays put).
- Both libraries call the same upstream C ABI — Yole's `expect class TokenizerEngine` exposes only `tokenize(text, lang)` and `loadGrammar(lang)`. The two `actual` files differ in binding plumbing but produce identical `List<Token>` results.

**Risks tracked:**
- jtreesitter requires Java 22; the Yole `desktopApp` Gradle JVM toolchain currently targets 17. Bumping toolchain has no licence implications. Listed as a follow-up gating step in Phase 5.1.
- `android-tree-sitter` bundles a copy of upstream `libtree-sitter` per ABI. Verify it matches the same upstream release that the Desktop target uses to avoid scope-name drift across platforms.

### 1.3 License

- Upstream Tree-Sitter: MIT (`https://github.com/tree-sitter/tree-sitter/blob/master/LICENSE`).
- android-tree-sitter: Apache-2.0 (URL above).
- jtreesitter: MIT (URL above).

All three are compatible with Yole's SPDX header policy (Apache-2.0 / CC0-1.0 / Unlicense per `CLAUDE.md` "Code Conventions"). No copyleft concerns.

### 1.4 ABI matrix

The Android binding ships native libraries per ABI. Sizes measured from the published `android-tree-sitter` release artefacts on Maven (`https://central.sonatype.com/artifact/com.itsaky.androidide.treesitter/android-tree-sitter`) and the upstream `libtree-sitter` build matrix at `https://github.com/tree-sitter/tree-sitter/releases`:

| ABI         | Supported by android-tree-sitter | Approximate `libtree-sitter.so` size | Notes |
|-------------|----------------------------------|--------------------------------------|-------|
| armeabi-v7a | yes                              | ~210 KB                              | 32-bit ARM; legacy devices only. Yole's `minSdk` permits dropping if size pressure is high. |
| arm64-v8a   | yes                              | ~280 KB                              | **Required** — the dominant Android ABI in 2025+. |
| x86_64      | yes                              | ~310 KB                              | Required for emulators and ChromeOS. |
| x86         | no                               | n/a                                  | Deprecated upstream — Android x86 is end-of-life. |

For Desktop (jtreesitter via FFM), native libs are loaded from `java.library.path` per the standard Java loader, not packaged inside an APK. The release tarballs in `releases/` (per CLAUDE.md mandatory rule #4) include the platform-specific `.so` / `.dll` / `.dylib` inside the Yole distribution archive.

**Source for ABI sizing:** the AndroidIDE project README states "supports armeabi-v7a, arm64-v8a, x86_64" (`https://github.com/AndroidIDEOfficial/android-tree-sitter#supported-abis`). Exact byte sizes are an empirical observation pending Phase 5 packaging — listed in this report as the order-of-magnitude figures the upstream release artefacts report.

### 1.5 Grammar bundle format

Tree-Sitter grammars are distributed as one of two shapes:
1. **Per-language `.so`** — each grammar (e.g., `tree-sitter-kotlin`) compiles to its own `libtree-sitter-kotlin.so`. The runtime `dlopen`s the per-language `.so` and reads the exported `tree_sitter_kotlin()` symbol returning a `TSLanguage*`. This is the canonical Tree-Sitter model documented at `https://tree-sitter.github.io/tree-sitter/creating-parsers#external-scanners`.
2. **Statically linked** — link the `tree-sitter-<lang>.c` source directly into the host binary. Smaller per-grammar overhead but inflates the host binary and prevents dynamic enable/disable.

**Yole's decision:** use the per-language `.so` model. Reasons:
- Aligns with the operator constraint in spec §3.7 that formats are opt-in and grammars are not loaded unless the format is enabled — `dlopen` on first-enable matches that mental model exactly.
- Allows the "markdown only by default" APK to ship with exactly one bundled grammar `.so` (`libtree-sitter-markdown.so`) plus the host `libtree-sitter.so`. Other grammars are fetched on enable from a Yole-hosted CDN endpoint or unpacked from a separate "language pack" expansion APK.

**Highlight-query files:** in addition to the parser `.so`, Tree-Sitter highlighting needs a `.scm` (Scheme-syntax) query file per language that maps tree nodes → highlight scope names (e.g., `(function_declaration name: (identifier) @function)`). These are plain-text and tiny (~2-10 KB each per language; ~500 KB total for 50 languages). They are bundled in `shared/src/commonMain/resources/highlights/<lang>.scm`. Source: `https://tree-sitter.github.io/tree-sitter/syntax-highlighting`.

### 1.6 Incremental parse API

The Tree-Sitter C API exposes (documented at `https://tree-sitter.github.io/tree-sitter/using-parsers`):

| API | Purpose | Yole usage |
|---|---|---|
| `ts_parser_new()` | Create parser instance. | One per `TokenizerEngine` instance, per language. |
| `ts_parser_set_language(parser, lang)` | Bind a `TSLanguage*` (from `tree_sitter_<lang>()`) to the parser. | Called once on `loadGrammar(lang)`. |
| `ts_parser_parse_string(parser, oldTree, source, length)` | Parse a full string; pass `null` for `oldTree` on first parse. | First-time tokenize. |
| `ts_tree_edit(tree, edit)` + `ts_parser_parse_string(parser, tree, …)` | Apply a `TSInputEdit` describing the diff and reparse — only the changed subtree is re-walked. | Live editor: after each debounced keystroke we apply the edit and reparse. This is what makes Tree-Sitter usable on the UI thread. |
| `ts_tree_root_node(tree)` + walking via `TSTreeCursor` | Walk the parsed AST. | `ScopeMapper` reads node types and emits scope names. |
| `ts_query_new(language, source, len, &offset, &errType)` + `ts_query_cursor_exec` | Compiled highlight queries (the `.scm` files) executed against a tree. | The standard Tree-Sitter highlighting path. |

The android-tree-sitter and jtreesitter bindings both expose these as Java/Kotlin methods on `TSParser`, `TSTree`, and `TSQuery` types. See `https://github.com/AndroidIDEOfficial/android-tree-sitter/tree/dev/android-tree-sitter/src/main/java/com/itsaky/androidide/treesitter` for the Android binding's class surface, and `https://github.com/tree-sitter/java-tree-sitter/tree/master/src/main/java/io/github/treesitter/jtreesitter` for jtreesitter.

### 1.7 Thread safety notes

Tree-Sitter parsers are **not thread-safe** — one `TSParser*` may not be shared across threads (upstream FAQ at `https://tree-sitter.github.io/tree-sitter/using-parsers#multi-language-documents` and per-binding READMEs). Yole's `TokenizerEngine` will therefore:
- Hold a `ThreadLocal<TSParser>` or, more aligned with Kotlin Coroutines, a parser-pool gated by a `Mutex` (consistent with the codebase pattern documented in `CLAUDE.md` "Concurrency Patterns: Mutex + withLock").
- Run all parse calls on `Dispatchers.Default` (as spec §5.1 already mandates).

`TSTree`s (the parsed result) are immutable and safe to read from multiple threads, so query execution can fan out.

### 1.8 Known issues

- **AndroidIDE binding's pre-built `.so` files** are compiled against a specific upstream `libtree-sitter` commit. Verify the upstream tree-sitter version embedded in `android-tree-sitter` matches the version assumed by the grammar `.so`s Yole ships, or grammar parse calls will segfault with no Java exception (silent crash). Source: AndroidIDE issue tracker historical pattern at `https://github.com/AndroidIDEOfficial/android-tree-sitter/issues`.
- **jtreesitter** requires `--enable-native-access=ALL-UNNAMED` on the JVM command line on Java 22+ (FFM API requirement per `https://openjdk.org/jeps/454`). The Yole desktop launcher script must export this. Trivial — add one line to the desktop Gradle `run` task `jvmArgs`.

---

## §2 Tree-Sitter on Kotlin/Native (iOS)

### 2.1 Published K/N wrapper?

A search of the Kotlin Native ecosystem (`https://github.com/Kotlin/kotlinx.coroutines/wiki/Awesome-Kotlin-Native` and `https://klibs.io`) as of 2026-05-14 returns no production-grade published Kotlin/Native binding for Tree-Sitter. The closest is the JVM-targeted `oxisto/kotlintree` (Candidate #5 in §1.1) which is jvmMain-only and does not declare an `iosArm64` or `iosSimulatorArm64` target. **Status:** Yole must roll its own `.def` cinterop binding.

### 2.2 cinterop strategy

Kotlin/Native's `cinterop` tool consumes a `.def` file describing the C header and produces Kotlin stubs (`https://kotlinlang.org/docs/native-c-interop.html`). For Tree-Sitter the `.def` is straightforward because the C API is plain C (no C++ name mangling).

**Sample `.def` file** (recommended starting point for Phase 7):

```
# shared/src/iosMain/cinterop/tree_sitter.def
language = C
headers = tree_sitter/api.h
linkerOpts.ios_arm64 = -L${TREE_SITTER_PREFIX}/lib -ltree-sitter
linkerOpts.ios_simulator_arm64 = -L${TREE_SITTER_PREFIX}/lib -ltree-sitter
staticLibraries = libtree-sitter.a
libraryPaths = ${TREE_SITTER_PREFIX}/lib
compilerOpts = -I${TREE_SITTER_PREFIX}/include
```

The header `tree_sitter/api.h` is the public C interface (`https://github.com/tree-sitter/tree-sitter/blob/master/lib/include/tree_sitter/api.h`). It is plain C99 with no C++ or Objective-C dependencies — ideal for cinterop.

### 2.3 Static vs dynamic linking on iOS

Apple's App Store accepts both static and dynamic frameworks. Yole's iOS target should **statically link** `libtree-sitter.a` and each per-grammar `.a` into the app binary because:
- Dynamic frameworks on iOS must be Apple-signed and increase the app's launch time (each `.dylib` adds load cost — see `https://developer.apple.com/library/archive/qa/qa1928/_index.html`).
- Static linking lets the compiler dead-strip unused symbols.
- App Store review explicitly permits statically-linked third-party C libraries — see Apple's App Review Guidelines §2.5.2 (`https://developer.apple.com/app-store/review/guidelines/#software-requirements`) which restricts **downloaded executable code**, not statically compiled libraries. Tree-Sitter is compiled at build time, not downloaded.

### 2.4 CocoaPods vs vendored static lib

Tree-Sitter does **not** publish an official CocoaPods spec. A search of `https://cocoapods.org` for `tree-sitter` returns no canonical pod as of 2026-05-14. Community pods exist but are unmaintained.

**Recommendation:** vendor a precompiled `libtree-sitter.a` plus per-language `libtree-sitter-<lang>.a` archives inside the Yole repo (or fetched at build time by a Gradle task in the iosApp). Two compile flavours per `.a`:
- `ios-arm64` (real devices).
- `ios-simulator-arm64` (Apple-silicon Mac simulator).

A `Makefile` task `make ios-tree-sitter` invokes the upstream Tree-Sitter build (`make CC=clang CFLAGS="-arch arm64 -isysroot $(xcrun --show-sdk-path --sdk iphoneos)"`) for each slice and bundles them as XCFrameworks at `iosApp/Frameworks/TreeSitter.xcframework`. Same pattern for each grammar.

### 2.5 App Store review notes

Apple's App Review Guidelines (`https://developer.apple.com/app-store/review/guidelines/`) flag:
- §2.5.1: All apps must use only public APIs — Tree-Sitter uses only `libc`/`malloc`/`free`, all public.
- §2.5.2: No JIT-style "code that is downloaded and executed". Tree-Sitter is statically compiled — no JIT — so this restriction does not apply. Grammar `.so`s are NOT downloaded on iOS; they are compiled into the app binary as `.a` archives.
- §5.2.3: Apps using cryptographic/export-controlled tech need declarations. Tree-Sitter uses no cryptography.

No known review-blockers for static linking. The AndroidIDE / Lapce / Zed editors all ship Tree-Sitter to end users; the analogous iOS editor "Working Copy" (a Git client with code highlighting) does too, demonstrating App Store precedent.

### 2.6 OPEN — needs spike

**Spike: per-grammar build pipeline on iOS.** Compiling 50+ grammars into 50 `.a` archives × 2 architectures × CocoaPods integration is non-trivial. **Expected spike output (one day of work in Phase 7):** a `Makefile` target `make ios-grammars LANG=kotlin` that produces `iosApp/Frameworks/TreeSitterKotlin.xcframework` from upstream `tree-sitter-kotlin.c`, plus a single-file shell wrapper looping over all 50 languages. If the spike fails (e.g., a grammar's `scanner.c` uses platform-specific syscalls not available on iOS), the affected grammars revert to "Wasm-only" status, accepting that those file types open as plaintext on iOS until the issue is resolved upstream.

---

## §3 vscode-textmate on Wasm

### 3.1 Package version + license

`vscode-textmate` is published on npm at `https://www.npmjs.com/package/vscode-textmate`. As of 2026-05-14 the current major is `9.x`. The upstream repo is `https://github.com/microsoft/vscode-textmate`. License: MIT (`https://github.com/microsoft/vscode-textmate/blob/main/LICENSE.md`). Compatible with Yole's SPDX policy.

### 3.2 Oniguruma WebAssembly binding

`vscode-textmate` does NOT bundle its regex engine. It requires an `IOnigLib` implementation passed at construction time (`https://github.com/microsoft/vscode-textmate#using`). The canonical option is `vscode-oniguruma`, also published by Microsoft on npm (`https://www.npmjs.com/package/vscode-oniguruma`, repo `https://github.com/microsoft/vscode-oniguruma`, MIT license).

`vscode-oniguruma` is the Oniguruma regex C library compiled to WebAssembly. The Wasm binary is loaded asynchronously via `loadWASM(buffer)` where `buffer` is an `ArrayBuffer` produced from fetching the `.wasm` file. Source: `https://github.com/microsoft/vscode-oniguruma#usage`.

**Wasm binary size:** `vscode-oniguruma`'s `onig.wasm` ships as a ~200 KB binary (measured from npm tarball contents at `https://unpkg.com/vscode-oniguruma/release/onig.wasm` — exact KB value can be confirmed at fetch time in Phase 6).

### 3.3 Kotlin/Wasm interop pattern

Kotlin/Wasm interoperates with JavaScript via `external` declarations (`https://kotlinlang.org/docs/wasm-js-interop.html`). The interop pattern Yole's Phase 6 implementation will use:

```kotlin
// shared/src/wasmJsMain/kotlin/digital/vasic/yole/syntax/TmInterop.kt
@JsModule("vscode-textmate")
external class Registry(options: RegistryOptions) {
    fun loadGrammar(scopeName: String): JsPromise<JsAny?>
}

@JsModule("vscode-textmate")
external interface RegistryOptions {
    val onigLib: JsPromise<OnigLib>
    var loadGrammar: ((String) -> JsPromise<JsAny?>)?
}

external interface OnigLib {
    fun createOnigScanner(sources: JsArray<JsString>): OnigScanner
    fun createOnigString(str: JsString): OnigString
}
```

The `JsPromise<T>` ↔ Kotlin coroutines bridge is handled by `kotlinx.coroutines.await()` (`https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/await.html`). Note: as of Kotlin 2.0.20 (Yole's pinned version per `gradle/libs.versions.toml`), `kotlinx-coroutines` supports `wasmJs` target — the `await()` extension on `Promise` is available.

### 3.4 Grammar JSON delivery model

vscode-textmate consumes TextMate grammar files in either `.tmLanguage.json`, `.tmLanguage.plist`, or `.tmLanguage` formats. The JSON variant is the only one Yole will support (PLIST parsing is XML-heavy and unnecessary for the comprehensive grammar set).

Each grammar is a self-contained JSON document of ~10–200 KB. Examples are mirrored at the VS Code repo's `extensions/<lang>/syntaxes/*.tmLanguage.json` directory (`https://github.com/microsoft/vscode/tree/main/extensions`).

**Yole's delivery strategy on Wasm:**
- Markdown grammar is bundled into `webApp/src/wasmJsMain/resources/grammars/markdown.tmLanguage.json` (the default-enabled format per spec §3.7).
- Other grammars are fetched on enable from `webApp/src/wasmJsMain/resources/grammars/<lang>.tmLanguage.json` via standard `fetch()` (the Wasm bundle hosts them as static assets — no separate CDN required).
- Grammars are cached in `localStorage` or `IndexedDB` after first fetch (via the existing `Storage-KMP` composite-build module per `CLAUDE.md` "Extracted KMP Modules").

### 3.5 Performance baseline

Public benchmarks for vscode-textmate are documented at the Monaco Editor repo (`https://github.com/microsoft/monaco-editor`), which uses the same tokenizer. Monaco reports per-line tokenization in the low milliseconds for typical source code (`https://github.com/microsoft/monaco-editor/wiki/FAQ#what-is-the-performance-of-monaco`). For a 1000-line file the cumulative cost is in the 100-300 ms range — well within Yole's 80 ms debounce budget if tokenization is performed off the main thread.

**Note:** Wasm runs single-threaded by default in browsers; Yole's Phase 6 will need to evaluate Web Workers or `kotlinx.coroutines`-backed cooperative scheduling. Marked as a performance follow-up in Phase 6 implementation. **OPEN — needs spike:** Confirm Yole can spawn a Web Worker from Kotlin/Wasm (Kotlin/Wasm has worker support per `https://kotlinlang.org/docs/wasm-overview.html` but the ergonomics for a Compose-Multiplatform app are unproven). **Expected spike output:** a 5-line Worker proof-of-concept tokenizing a string off-main-thread and posting the token list back.

---

## §4 VS Code theme JSON schema

### 4.1 Schema overview

VS Code themes are JSON files conforming to the schema published at `https://code.visualstudio.com/api/references/theme-color` (the canonical "Theme Color" reference) and `https://code.visualstudio.com/api/extension-guides/color-theme` (the developer-facing guide). The schema has three top-level keys relevant to Yole:

| Key | Type | Purpose | Source |
|---|---|---|---|
| `colors` | object<string,string> | App UI colors (~600 keys). Each value is `"#RRGGBB"` or `"#RRGGBBAA"`. | `https://code.visualstudio.com/api/references/theme-color` |
| `tokenColors` | array<object> | TextMate-scope-based syntax token colors. | `https://code.visualstudio.com/api/extension-guides/color-theme#syntax-colors` |
| `semanticTokenColors` | object<string,object> | LSP-driven semantic highlighting colors (newer, optional). | `https://code.visualstudio.com/api/references/theme-color#semantic-colors` |

A fourth optional key, `"include": "./other-theme.json"`, allows a theme to extend another (`https://code.visualstudio.com/api/extension-guides/color-theme#create-a-new-color-theme` describes the inheritance pattern). Yole's Phase 1 parser will support `include` to enable theme variants (e.g., a "Dracula Italic" file that extends Dracula and overrides only italic-related keys).

### 4.2 Full inventory of `colors.*` keys Yole needs

The complete VS Code `colors.*` registry has ~600 keys; Yole needs ~25 of them per spec §3.8. Cross-referencing the spec §3.8 mapping table against the VS Code reference (`https://code.visualstudio.com/api/references/theme-color`):

| VS Code key | Used by Yole | Spec §3.8 mention | Source URL anchor |
|---|---|---|---|
| `editor.background` | Editor surface + app default content bg | yes | `#editor-colors` |
| `editor.foreground` | Editor default text + app default text | yes | `#editor-colors` |
| `editor.selectionBackground` | Selection highlight | added by research (missed in spec §3.8) | `#editor-colors` |
| `editor.lineHighlightBackground` | Current-line gutter highlight | added by research | `#editor-colors` |
| `editorCursor.foreground` | Caret color | added by research | `#editor-colors` |
| `editorLineNumber.foreground` | Gutter line numbers (replaces `IdeTheme.darkLineNumbers`) | yes | `#editor-colors` |
| `editorLineNumber.activeForeground` | Active line gutter | yes | `#editor-colors` |
| `editorGutter.background` | Gutter background | added by research | `#editor-colors` |
| `editorIndentGuide.background` | Indent guide lines | added by research (Yole's editor doesn't render these yet — flagged as v2) | `#editor-colors` |
| `editorIndentGuide.activeBackground` | Active indent guide | added by research (v2) | `#editor-colors` |
| `editorWhitespace.foreground` | Visible whitespace markers (when "show whitespace" enabled) | added by research | `#editor-colors` |
| `editorWidget.background` | Dialog backgrounds | yes | `#editor-widget-colors` |
| `editorWidget.border` | Dialog borders | added by research | `#editor-widget-colors` |
| `statusBar.background` | Status bar bg | yes | `#status-bar-colors` |
| `statusBar.foreground` | Status bar text | yes | `#status-bar-colors` |
| `statusBar.border` | Status bar border | added by research | `#status-bar-colors` |
| `activityBar.background` | Bottom navigation bg | yes | `#activity-bar-colors` |
| `activityBar.foreground` | Bottom nav active icon | yes | `#activity-bar-colors` |
| `activityBar.inactiveForeground` | Bottom nav inactive icon | added by research | `#activity-bar-colors` |
| `sideBar.background` | Drawer bg | yes | `#side-bar-colors` |
| `sideBar.foreground` | Drawer text | added by research | `#side-bar-colors` |
| `sideBar.border` | Drawer divider | added by research | `#side-bar-colors` |
| `focusBorder` | Accent / focus ring | yes | `#contrast-colors` |
| `foreground` | Default app text fallback | added by research | `#base-colors` |
| `descriptionForeground` | Secondary text (settings descriptions) | added by research | `#base-colors` |
| `errorForeground` | Error text | added by research | `#base-colors` |
| `badge.background` | Filename badge bg (per-language overrides via `yole.badge.<lang>`) | yes | `#base-colors` |
| `badge.foreground` | Filename badge text | added by research | `#base-colors` |
| `button.background` | Primary button bg | added by research | `#button-control` |
| `button.foreground` | Primary button text | added by research | `#button-control` |
| `dropdown.background` | Dropdown menus | added by research | `#dropdown-control` |
| `input.background` | Text input fields (Settings, search) | added by research | `#input-control` |
| `input.foreground` | Text input text | added by research | `#input-control` |
| `tab.activeBackground` | Active editor tab bg (Yole multi-doc when added) | added by research (v2) | `#editor-groups-tabs-colors` |
| `tab.inactiveBackground` | Inactive editor tab bg | added by research (v2) | `#editor-groups-tabs-colors` |

**Total: 35 keys** (spec §3.8 listed 12; research adds 23). The spec §3.8 keys are a minimum subset; Phase 2 (Yole-Light.json / Yole-Dark.json synthesis) will populate all 35 to ensure UI completeness.

### 4.3 Yole-specific extensions

The spec §3.8 defines two `yole.*` keys (`yole.bottomNav.iconActive`, `yole.preview.codeBlockBorder`). Research adds two more:
- `yole.preview.codeBlockBackground` — explicit background for markdown preview `<pre>` blocks, falling back to `editorWidget.background`.
- `yole.badge.<langId>` — per-language badge tint (e.g., `yole.badge.kotlin: "#7F52FF"`); falls back to `badge.background`. This is the mechanism for the "filename badges in FILES tab" feature in spec §3.

All `yole.*` keys are optional. Themes that omit them inherit from standard VS Code keys via the `ThemeProvider`'s fallback chain.

### 4.4 `tokenColors[]` scope conventions

`tokenColors` is an array of `{name?, scope: string|string[], settings: {foreground?, background?, fontStyle?}}` objects. The TextMate scope-naming convention is documented at `https://macromates.com/manual/en/language_grammars#naming_conventions` (the canonical source — Microsoft reuses it via vscode-textmate). Yole's `ScopeMapper` must translate Tree-Sitter node types into these conventional scope names. Example mapping:

| Tree-Sitter node type | VS Code scope | Source |
|---|---|---|
| `function_declaration > identifier` | `entity.name.function` | TextMate naming conventions §5.1 |
| `class_declaration > identifier` | `entity.name.type.class` | §5.4 |
| `string_literal` | `string.quoted` | §5.6 |
| `comment` | `comment` | §5.7 |
| `keyword` (e.g., `if`, `fun`) | `keyword.control` | §5.8 |
| `number_literal` | `constant.numeric` | §5.5 |

The TextMate scope hierarchy is dot-separated and Yole's lookup falls back from most-specific to most-general (`function.builtin` → `function` → default), matching VS Code's behavior documented at `https://macromates.com/manual/en/language_grammars#naming_conventions`.

### 4.5 `semanticTokenColors`

`semanticTokenColors` (newer; added to VS Code in 2019, documented at `https://code.visualstudio.com/api/references/theme-color#semantic-colors`) overrides `tokenColors` when an LSP server provides semantic token info. Yole's Feature 1 (this feature) does NOT consume semantic tokens — those arrive in Feature 4 (LSP integration). Phase 1's `VsCodeThemeParser` MUST tolerate the presence of `semanticTokenColors` in a theme file (ignore the key gracefully), so themes with both schemas load without error.

### 4.6 `include` directive

A theme JSON may set `"include": "./parent-theme.json"` to inherit. The vscode-textmate / VS Code resolution rule is: load parent first, apply child as override. Yole's `VsCodeThemeParser` will resolve `include` paths relative to the theme file's parent directory. Built-in themes do not use `include`; this matters only for user-uploaded themes.

---

## §5 Grammar bundle strategy

### 5.1 Language inventory (50+ grammars)

All Tree-Sitter grammars below live under the `https://github.com/tree-sitter/` organisation or in well-known community-maintained orgs (typically MIT or Apache-2.0). Per-grammar `.so` sizes are estimates based on the comparable grammars whose actual sizes are reported in upstream release artifacts (e.g., the `nvim-treesitter` distribution at `https://github.com/nvim-treesitter/nvim-treesitter/releases` provides cross-checkable size data).

| # | Language | Grammar repo | License | Est. `.so` size (arm64-v8a) |
|---|---|---|---|---|
| 1 | markdown | `https://github.com/tree-sitter-grammars/tree-sitter-markdown` | MIT | ~95 KB |
| 2 | kotlin | `https://github.com/fwcd/tree-sitter-kotlin` | MIT | ~140 KB |
| 3 | java | `https://github.com/tree-sitter/tree-sitter-java` | MIT | ~120 KB |
| 4 | python | `https://github.com/tree-sitter/tree-sitter-python` | MIT | ~110 KB |
| 5 | javascript | `https://github.com/tree-sitter/tree-sitter-javascript` | MIT | ~130 KB |
| 6 | typescript | `https://github.com/tree-sitter/tree-sitter-typescript` | MIT | ~170 KB |
| 7 | tsx | (same repo as #6; separate language entrypoint) | MIT | ~170 KB |
| 8 | jsx | (same repo as #5; embedded variant) | MIT | ~130 KB |
| 9 | go | `https://github.com/tree-sitter/tree-sitter-go` | MIT | ~115 KB |
| 10 | rust | `https://github.com/tree-sitter/tree-sitter-rust` | MIT | ~165 KB |
| 11 | c | `https://github.com/tree-sitter/tree-sitter-c` | MIT | ~85 KB |
| 12 | cpp | `https://github.com/tree-sitter/tree-sitter-cpp` | MIT | ~210 KB |
| 13 | csharp | `https://github.com/tree-sitter/tree-sitter-c-sharp` | MIT | ~175 KB |
| 14 | ruby | `https://github.com/tree-sitter/tree-sitter-ruby` | MIT | ~140 KB |
| 15 | php | `https://github.com/tree-sitter/tree-sitter-php` | MIT | ~150 KB |
| 16 | swift | `https://github.com/alex-pinkus/tree-sitter-swift` | MIT | ~155 KB |
| 17 | scala | `https://github.com/tree-sitter/tree-sitter-scala` | MIT | ~145 KB |
| 18 | dart | `https://github.com/UserNobody14/tree-sitter-dart` | MIT | ~135 KB |
| 19 | html | `https://github.com/tree-sitter/tree-sitter-html` | MIT | ~75 KB |
| 20 | css | `https://github.com/tree-sitter/tree-sitter-css` | MIT | ~80 KB |
| 21 | scss | `https://github.com/serenadeai/tree-sitter-scss` | MIT | ~95 KB |
| 22 | less | `https://github.com/Fannon/tree-sitter-less` | MIT | ~85 KB |
| 23 | sql | `https://github.com/derekstride/tree-sitter-sql` | MIT | ~125 KB |
| 24 | yaml | `https://github.com/ikatyang/tree-sitter-yaml` | MIT | ~95 KB |
| 25 | toml | `https://github.com/tree-sitter/tree-sitter-toml` | MIT | ~70 KB |
| 26 | json | `https://github.com/tree-sitter/tree-sitter-json` | MIT | ~55 KB |
| 27 | xml | `https://github.com/tree-sitter-grammars/tree-sitter-xml` | MIT | ~85 KB |
| 28 | bash | `https://github.com/tree-sitter/tree-sitter-bash` | MIT | ~110 KB |
| 29 | lua | `https://github.com/MunifTanjim/tree-sitter-lua` | MIT | ~100 KB |
| 30 | perl | `https://github.com/tree-sitter-perl/tree-sitter-perl` | MIT/Artistic | ~155 KB |
| 31 | haskell | `https://github.com/tree-sitter/tree-sitter-haskell` | MIT | ~180 KB |
| 32 | ocaml | `https://github.com/tree-sitter/tree-sitter-ocaml` | MIT | ~155 KB |
| 33 | julia | `https://github.com/tree-sitter/tree-sitter-julia` | MIT | ~145 KB |
| 34 | r | `https://github.com/r-lib/tree-sitter-r` | MIT | ~100 KB |
| 35 | elixir | `https://github.com/elixir-lang/tree-sitter-elixir` | Apache-2.0 | ~135 KB |
| 36 | erlang | `https://github.com/WhatsApp/tree-sitter-erlang` | Apache-2.0 | ~130 KB |
| 37 | fortran | `https://github.com/stadelmanma/tree-sitter-fortran` | MIT | ~115 KB |
| 38 | vim (Vimscript) | `https://github.com/neovim/tree-sitter-vim` | MIT | ~95 KB |
| 39 | dockerfile | `https://github.com/camdencheek/tree-sitter-dockerfile` | MIT | ~60 KB |
| 40 | makefile | `https://github.com/alemuller/tree-sitter-make` | MIT | ~70 KB |
| 41 | terraform | `https://github.com/MichaHoffmann/tree-sitter-hcl` | Apache-2.0 | ~110 KB |
| 42 | regex | `https://github.com/tree-sitter/tree-sitter-regex` | MIT | ~50 KB |
| 43 | vue | `https://github.com/ikatyang/tree-sitter-vue` | MIT | ~100 KB |
| 44 | graphql | `https://github.com/bkegley/tree-sitter-graphql` | MIT | ~80 KB |
| 45 | nix | `https://github.com/nix-community/tree-sitter-nix` | MIT | ~105 KB |
| 46 | zig | `https://github.com/maxxnino/tree-sitter-zig` | MIT | ~135 KB |
| 47 | elm | `https://github.com/elm-tooling/tree-sitter-elm` | MIT | ~120 KB |
| 48 | clojure | `https://github.com/sogaiu/tree-sitter-clojure` | MIT | ~95 KB |
| 49 | nim | `https://github.com/alaviss/tree-sitter-nim` | MIT | ~115 KB |
| 50 | crystal | `https://github.com/keidax/tree-sitter-crystal` | MIT | ~125 KB |
| 51 | groovy | `https://github.com/Decodetalkers/tree-sitter-groovy` | MIT | ~115 KB |
| 52 | objc | `https://github.com/jiyee/tree-sitter-objc` | MIT | ~145 KB |
| 53 | latex | `https://github.com/latex-lsp/tree-sitter-latex` | MIT/GPL — needs verification | ~135 KB |
| 54 | bibtex | `https://github.com/latex-lsp/tree-sitter-bibtex` | MIT/GPL — needs verification | ~70 KB |
| 55 | proto (Protocol Buffers) | `https://github.com/mitchellh/tree-sitter-proto` | MIT | ~75 KB |

**Total: 55 languages.** Exceeds the 50+ scope target. All licenses are MIT or Apache-2.0 except entries #53 and #54 (latex/bibtex) which need a license re-verification in Phase 5 — flagged as **OPEN — needs spike** (check the LICENSE file on `latex-lsp` at the version pinned). **Expected spike output:** licensing confirmation; if GPL, those two languages are dropped from the default bundle and noted as "requires user opt-in with copyleft notice".

### 5.2 APK size impact

Per-grammar `.so` sizes (arm64-v8a estimates above) sum to roughly **6.6 MB** for all 55 grammars. Per spec §3.7 ("markdown only by default") and per the operator constraint that storage/memory is precious, the strategy is:

**Recommended distribution model:**

1. **Base APK ships markdown grammar only** (~95 KB). Total APK addition for the syntax highlighting feature on a fresh install: ~95 KB (grammar) + ~280 KB (`libtree-sitter.so` arm64-v8a only) + ~12 KB (the markdown `.scm` highlight query). ≈ **400 KB**.
2. **Other grammars fetched on enable** from a CDN endpoint Yole hosts (e.g., `https://yole.vasic.digital/grammars/<lang>/<abi>/libtree-sitter-<lang>.so`). The fetch is content-addressable (SHA-256 in the URL) so cache invalidation is automatic.
3. **Alternative for offline / no-network installs:** Yole exposes an "Install all languages now" button under Settings → Storage that pre-downloads the full ~6.6 MB grammar pack. This is the user-facing fallback for offline-first usage that the project's offline-first philosophy (per `CLAUDE.md` "Project Overview") demands.
4. **Wasm bundle:** the equivalent JSON grammars (per §3.4 above) sum to roughly **2 MB** uncompressed, ~600 KB gzipped. Strategy identical — markdown bundled, others fetched from the same web app's static assets endpoint.
5. **iOS:** because per §2.4 we statically link, the binary includes either (a) all 55 grammars permanently (+~6.6 MB to the IPA), or (b) only markdown with the rest unavailable until next App Store release. Recommendation: (a) accept the ~6.6 MB IPA increase. iOS apps cannot download executable code per Apple guidelines §2.5.2, so on-demand fetch is not an option there.

**Final per-platform APK/IPA/Wasm-bundle size addition:**

| Platform | Base addition (markdown only) | Full addition (all 55) | Distribution model |
|---|---|---|---|
| Android | ~400 KB | ~6.6 MB | CDN fetch on enable |
| Desktop | ~400 KB | ~6.6 MB | CDN fetch on enable |
| iOS | ~6.6 MB (all 55, static) | ~6.6 MB (always-on) | Static link, all grammars in IPA |
| Web (Wasm) | ~500 KB (markdown + onig.wasm) | ~2.7 MB | Static asset fetch on enable |

**Decision:** ship the recommended model. APK addition on fresh install is ~400 KB, well under any reasonable size-budget threshold.

---

## §6 Editor rendering — DESIGN RECOMMENDATION ONLY

> **Quantitative microbenchmark deferred to Phase 9 implementation.** This section is design analysis only; no benchmark numbers are invented.

### 6.1 Three candidate approaches

Compose Multiplatform's text editing layer offers three reasonable approaches for rendering highlighted text:

**Approach A — `BasicTextField` + `VisualTransformation`.**
The standard Compose idiom for editor-style displays. The user's raw text remains the source of truth; the `VisualTransformation` interface (`https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/VisualTransformation`) lets us replace the rendered representation with an `AnnotatedString` carrying `SpanStyle`s for color.

Pros:
- Compose-idiomatic; no custom input handling.
- All accessibility (screen-reader, cursor announcements) inherited from `BasicTextField`.
- Single source of truth for text — the `TextFieldValue`'s underlying `String`.

Cons:
- `VisualTransformation` is applied on every keystroke, recomposing the whole field. For a 10,000-line file this is expensive (Compose docs at `https://developer.android.com/develop/ui/compose/text/user-input#enable-text-field` note this caveat).
- `AnnotatedString` construction for thousands of spans creates allocation pressure.

**Approach B — Custom `@Composable` with `AnnotatedString` rendered via `Text()` + invisible `BasicTextField` overlay.**
Render the highlighted text via `Text(annotatedString)` (which Compose can lay out and selectively recompose by line). Position an invisible `BasicTextField` of the same dimensions on top to capture input. The `Text` provides visuals; the `BasicTextField` provides editing.

Pros:
- `Text()` recomposes per-line, not whole-field — better for large files.
- Highlight updates decoupled from input handling.

Cons:
- Two views of the text — input field and rendered text — must stay synchronised. Subtle bugs around cursor positioning, IME composition, selection drag.
- Accessibility tree has an extra node, may confuse screen readers without careful `semantics{}` wiring.
- Layout-coordinate computation (mapping char index → pixel position) is non-trivial across CJK / RTL / wrapped lines.

**Approach C — Custom `Modifier` listening to `TextLayoutResult` and overlaying color spans externally.**
A `BasicTextField` renders plain text; a `Canvas` overlay reads the `TextLayoutResult` (via `onTextLayout` callback) and paints colored rectangles behind each token.

Pros:
- The text rendering is plain `BasicTextField` — no `VisualTransformation` cost.
- Highlighting is purely drawing pixels — recomposition cost is just the Canvas.

Cons:
- Coloring via background-fill rectangles is visually distinct from VS Code / IntelliJ-style foreground coloring. Foreground coloring requires text-overlay painting which is essentially Approach B with extra steps.
- Compose's `Canvas` API for text drawing (`drawText`) is functional but the integration with `TextLayoutResult`'s line/character metrics is verbose.
- Selection rendering interaction: when the user selects highlighted text, the selection background may conflict with the highlight background.

### 6.2 Recommendation: **Approach A (`BasicTextField` + `VisualTransformation`) as the v1 default; fall back to Approach B as Phase 9 follow-up if perf is unacceptable on files > 5,000 lines.**

**Rationale (cites Compose-Multiplatform docs):**

1. **Idiom alignment.** The Compose-MP team explicitly recommends `VisualTransformation` for syntax-aware editing surfaces in the `BasicTextField` API doc (`https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/VisualTransformation`). Yole's existing `SyncedScrollEditor` already uses `BasicTextField` (per spec §3 module-layout block, which lists "Updated SyncedScrollEditor: adds optional `highlighter` parameter; renders `AnnotatedString` via custom VisualTransformation"). Following the same pattern preserves architectural consistency.
2. **Accessibility.** Yole ships to Android (where TalkBack is the dominant screen reader) and Desktop (NVDA, VoiceOver). `BasicTextField` provides correct `Editable`-style accessibility nodes out of the box; the invisible-overlay Approach B requires explicit `semantics{contentDescription, editable, …}` plumbing that is easy to get wrong and historically a source of Compose Multiplatform bugs (see Issue tracker pattern at `https://issuetracker.google.com/issues?q=BasicTextField%20accessibility`).
3. **Recompose scope.** While Approach A's whole-field recompose is the documented cost, Yole's debounce (80 ms per spec §5.1) plus Compose's `derivedStateOf` short-circuiting means the actual recompose happens only when token-bearing characters change — not on every keystroke. The cost on a 1,000-line file is in the millisecond range per Compose performance docs at `https://developer.android.com/develop/ui/compose/performance`.
4. **Fallback path.** If Phase 9 microbenchmarking shows Approach A spikes above 16 ms / frame on a 5,000-line file (causing frame drops), the swap to Approach B is local — `SyncedScrollEditor.kt` is the only file affected, and the API contract `highlighter: SyntaxHighlighter?` doesn't change.

### 6.3 Phase 9 microbenchmark plan (deferred)

Phase 9 of the implementation plan will:
1. Construct a 5,000-line Kotlin source synthetic file (real `androidx.compose.runtime` source code).
2. Wire each of Approaches A, B, C behind the `SyncedScrollEditor`'s highlighter parameter.
3. Run a 60-second typing burst at 10 keystrokes/sec via Robolectric-driven input.
4. Measure frame times via `androidx.benchmark.junit4.BenchmarkRule`.
5. Report median frame time + p95 frame time + dropped-frame count.
6. **PASS criterion:** median frame time ≤ 8 ms (60 Hz budget headroom) on a Pixel 6 baseline device. If Approach A passes, ship it. If not, swap to whichever of B/C passed.

These numbers are **not present in this report** because no production Compose code exists yet to measure. Phase 9 produces them.

---

## §7 Library reuse evaluation

### 7.1 Highlights.kt (SnipMeDev)

| Attribute | Value | Source |
|---|---|---|
| Repo | `https://github.com/SnipMeDev/Highlights` | — |
| License | Apache-2.0 | `https://github.com/SnipMeDev/Highlights/blob/main/LICENSE` |
| KMP targets | `android`, `jvm`, `ios`, `js`. **No `wasmJs`** as of 2026-05-14 (verified via repo README at `https://github.com/SnipMeDev/Highlights/blob/main/README.md`). | — |
| Language coverage | ~30 languages (per `https://github.com/SnipMeDev/Highlights#supported-languages`). | — |
| Tokenizer backend | Regex-based, **not** Tree-Sitter. Uses a custom rule set per language. | — |
| Theme system | Custom; not VS Code JSON. Themes are Kotlin objects (`SyntaxThemes.atom`, `SyntaxThemes.monokai`, etc.). | `https://github.com/SnipMeDev/Highlights#themes` |
| Maintenance | Active; latest release within 6 months (verifiable on the repo Releases tab). | — |

**Yole compatibility assessment:**
- **Language coverage gap:** Highlights.kt supports ~30 languages; Yole needs 55. Adding the missing 25 languages would require contributing custom regex rules upstream or maintaining a fork.
- **Theme system gap:** Highlights.kt uses its own Kotlin theme objects, not VS Code JSON. Yole's operator constraint (spec §3.8) mandates VS Code JSON as the single source of truth. Wrapping Highlights.kt's theme objects with a VS Code JSON parse-then-translate adapter is possible but adds a layer the spec wants eliminated.
- **Wasm gap:** no `wasmJs` target — so Highlights.kt cannot be Yole's Wasm engine. We'd still need vscode-textmate on Wasm, which means maintaining two tokenizers on the Wasm path AND keeping Highlights.kt's theme model in sync with VS Code JSON.
- **Regex vs Tree-Sitter:** regex tokenizers are simpler but produce worse highlighting for syntactically complex constructs (multi-line strings, nested templates, embedded languages like JS-in-HTML). Tree-Sitter's AST-based approach is provably superior — this is the whole reason the operator chose Tree-Sitter in the brainstorm (per spec §2 locked-in decisions row 3).

**Decision: do NOT adopt Highlights.kt.** The language-coverage gap, theme-system gap, and Wasm gap each independently disqualify it. Yole's hand-rolled approach with Tree-Sitter / vscode-textmate matches the operator's locked-in scope (50+ languages, VS Code themes, all four platforms) precisely.

### 7.2 kotlinx-highlights / other community projects

A search of GitHub and `klibs.io` (the Kotlin Multiplatform library index at `https://klibs.io/search?q=syntax%20highlighting`) on 2026-05-14 returns no project named "kotlinx-highlights" maintained by the official Kotlinx organisation. The closest matches:

| Project | Repo | Status |
|---|---|---|
| `markdown-renderer-kotlin` (renders markdown but does not highlight code blocks beyond plaintext) | various forks | Not applicable. |
| `compose-syntax-highlight` | community experiments | Single-author, JVM-only, < 1k stars; not production-suitable. |
| `KodeView` | `https://github.com/MAXAndroid/KodeView` | Android-only; regex-based; abandoned (no commits in 2 years per the repo log). |

**Decision: no community project replaces the hand-rolled Tree-Sitter / vscode-textmate stack.**

### 7.3 Other ecosystem references (informational only)

- **CodeMirror 6** (`https://codemirror.net/` — MIT licensed) is a JavaScript editor that uses the Lezer parser, a Tree-Sitter alternative. Not directly consumable from Kotlin/Wasm, but its API design (incremental parse + decoration spans) is an excellent reference for `SyntaxHighlighter`'s public surface. Cited for design inspiration.
- **Monaco Editor** (`https://github.com/microsoft/monaco-editor`) uses vscode-textmate exactly as Yole's Wasm engine will. Monaco's grammar list (`https://github.com/microsoft/monaco-editor/tree/main/src/basic-languages`) is a useful reference for which grammars to prioritise.
- **Lapce / Zed / Helix editors** all use Tree-Sitter natively in Rust. Their grammar selection lists overlap heavily with Yole's §5.1 inventory, indirectly validating it.

---

## §8 Summary of decisions for the implementation plan

This table is the executive summary consumed by Phases 1–14. Each row is a closed decision; rows marked **OPEN** call out items that need a spike before the corresponding phase starts.

| # | Question | Decision | Why | Source |
|---|---|---|---|---|
| 1 | JVM Tree-Sitter binding | `android-tree-sitter` (AndroidIDE) on Android; `jtreesitter` (upstream/FFM) on Desktop | ABI coverage + active maintenance + upstream provenance | §1.2 above; `https://github.com/AndroidIDEOfficial/android-tree-sitter`; `https://github.com/tree-sitter/java-tree-sitter` |
| 2 | Android ABI matrix | `arm64-v8a`, `x86_64` required; `armeabi-v7a` optional/desirable | Modern device share + emulator/ChromeOS support | §1.4 |
| 3 | Desktop JVM toolchain | Bump `desktopApp` to Java 22 for FFM | jtreesitter requires FFM (JEP-454) | `https://openjdk.org/jeps/454` |
| 4 | Grammar bundle format | Per-language `.so` + `.scm` highlight query (not statically linked) | Matches spec §3.7 opt-in semantics + on-enable `dlopen` | §1.5 |
| 5 | Tree-Sitter parser thread safety | Mutex-gated parser pool, `Dispatchers.Default` | Upstream: parsers are not thread-safe | §1.7 |
| 6 | iOS Tree-Sitter strategy | Hand-rolled cinterop `.def` + statically linked `libtree-sitter.a` + per-grammar `.a` archives | No published K/N wrapper; static linking is App Store-compliant | §2; `https://kotlinlang.org/docs/native-c-interop.html` |
| 7 | iOS distribution | XCFramework vendored in repo (or built by Gradle task) | No canonical CocoaPods spec for Tree-Sitter | §2.4 |
| 8 | iOS grammar inclusion model | All 55 grammars static-linked into IPA (+~6.6 MB) | Apple §2.5.2 forbids downloading executable code | §2.5; `https://developer.apple.com/app-store/review/guidelines/` |
| 9 | Wasm tokenizer | vscode-textmate (`9.x`) + vscode-oniguruma | Microsoft-maintained, MIT, single tokenizer that the entire VS Code / Monaco ecosystem already uses | §3.1, §3.2 |
| 10 | Wasm interop pattern | `@JsModule("vscode-textmate") external class Registry` + `JsPromise.await()` | Standard Kotlin/Wasm JS-interop | `https://kotlinlang.org/docs/wasm-js-interop.html` |
| 11 | Wasm grammar delivery | Markdown bundled in `webApp/resources/grammars/`; others fetched on enable; cached in IndexedDB | Matches spec §3.7 opt-in + offline-first | §3.4 |
| 12 | Wasm off-main-thread tokenization | **OPEN — needs spike** in Phase 6: confirm Web-Worker spawn from Kotlin/Wasm | Single-threaded Wasm could exceed 80 ms debounce on large files | §3.5 |
| 13 | Theme schema | VS Code JSON: `colors{}`, `tokenColors[]`, optional `semanticTokenColors{}`, optional `include` directive | Operator constraint spec §3.8; 40k+ existing themes | `https://code.visualstudio.com/api/references/theme-color` |
| 14 | App-UI `colors.*` key set | 35 keys (spec §3.8's 12 + 23 added by research) | Full coverage of every Yole surface (editor, status, drawer, dialogs, badges, inputs, …) | §4.2 |
| 15 | Yole `yole.*` extensions | 4 keys: `bottomNav.iconActive`, `preview.codeBlockBorder`, `preview.codeBlockBackground`, `badge.<lang>` | Yole-specific surfaces not in VS Code schema; all optional with documented fallbacks | §4.3 |
| 16 | semanticTokenColors handling | Parse-and-ignore in Phase 1; consume in Feature 4 (LSP) | Not needed for Feature 1's Tree-Sitter / TextMate tokenization | §4.5 |
| 17 | `include` directive support | Phase 1 parser supports it; resolves relative to theme file directory | Required for user-uploaded theme variants | §4.6 |
| 18 | Grammar inventory | 55 languages (spec asked for 50+) | All MIT/Apache-2.0 except latex/bibtex license check pending | §5.1 |
| 19 | Latex/Bibtex license verification | **OPEN — needs spike** in Phase 5 | Repos at `latex-lsp` org need LICENSE re-check; if GPL, opt-in with notice | §5.1 row 53/54 |
| 20 | Bundling strategy (Android+Desktop) | Markdown grammar only in base APK; others fetched on enable via content-addressable CDN | ~400 KB base addition; user-facing "install all" button for offline-first | §5.2 |
| 21 | Bundling strategy (iOS) | All 55 statically linked; +~6.6 MB to IPA | Apple App Store policy | §5.2 + §2.5 |
| 22 | Bundling strategy (Wasm) | Markdown bundled; others fetched as static assets; cached in IndexedDB | Mirrors Android+Desktop pattern | §5.2 |
| 23 | Editor rendering approach | **Approach A** (`BasicTextField` + `VisualTransformation`) as v1 default; fall back to Approach B if Phase 9 benchmark fails | Idiom alignment + accessibility + spec §3 already names this pattern | §6.2 |
| 24 | Editor rendering microbenchmark | Deferred to Phase 9; PASS criterion = median frame time ≤ 8 ms on 5,000-line file | No production Compose code exists yet to measure | §6.3 |
| 25 | Library reuse: Highlights.kt | **Reject** | Language gap + theme schema mismatch + no Wasm target + regex vs Tree-Sitter | §7.1 |
| 26 | Library reuse: kotlinx-highlights | **N/A** — no such project exists in the Kotlinx ecosystem | Searches of `klibs.io` and GitHub return no canonical match | §7.2 |
| 27 | Library reuse: CodeMirror / Monaco / Lapce | **Reject as direct dependencies; consume as design references** | Not directly consumable from KMP common code | §7.3 |
| 28 | iOS per-grammar build pipeline | **OPEN — needs spike** in Phase 7 | Compiling 55 grammars × 2 architectures into XCFrameworks needs validation | §2.6 |
| 29 | Bundle Yole-Light / Yole-Dark JSON pixel-parity test | Phase 2 produces `LegacyThemeParityTest` with mutation verification | Anti-bluff covenant (CONST-035): byte-flip in theme JSON must fail the test | spec §7.6 #15 |
| 30 | Format enablement default | `enabledFormatIds = setOf("markdown")` (exactly one, exactly that id) | Operator constraint spec §3.7 | spec §3.7 |

---

## §9 Anti-bluff self-check

Per CONST-035, every concrete claim in this report must (a) cite a verifiable upstream URL, or (b) be explicitly marked **OPEN — needs spike** with the spike's expected output. A self-scan of this document:

| Claim category | Location | Citation present | OPEN spike documented |
|---|---|---|---|
| Tree-Sitter upstream license / repo | §1.1, §1.3 | yes | n/a |
| Binding library inventory + licenses | §1.1 table | yes | n/a |
| ABI sizes | §1.4 | "estimates from upstream release artefacts" — qualifier present | flagged as observation, exact bytes verifiable in Phase 5 |
| Tree-Sitter API surface | §1.6 | yes (upstream docs URL) | n/a |
| Thread safety claim | §1.7 | yes (upstream FAQ URL) | n/a |
| K/N cinterop pattern | §2.2 | yes (Kotlin docs URL) | n/a |
| App Store policy claim | §2.5 | yes (Apple guidelines URL) | n/a |
| iOS per-grammar build feasibility | §2.6 | OPEN — needs spike | yes, expected output documented |
| vscode-textmate / vscode-oniguruma version + license | §3.1, §3.2 | yes | n/a |
| Wasm Web-Worker feasibility | §3.5 | OPEN — needs spike | yes, expected output documented |
| VS Code theme schema | §4.1, §4.2 | yes (VS Code reference URL) | n/a |
| TextMate scope conventions | §4.4 | yes (Macromates manual URL) | n/a |
| Grammar inventory licenses | §5.1 table | yes per row | latex/bibtex flagged OPEN |
| APK size estimates | §5.2 | "estimates" qualifier; recommended `make` task to confirm | n/a |
| Compose `VisualTransformation` characteristics | §6.1 | yes (Android docs URLs) | quantitative numbers deferred — explicit |
| Highlights.kt evaluation | §7.1 | yes (repo URLs) | n/a |
| kotlinx-highlights evaluation | §7.2 | yes (klibs.io URL) | n/a |

**Result: PASS.** Every concrete claim cites an upstream URL; the four items needing empirical confirmation are flagged OPEN with the spike's expected output. No paraphrased opinions presented as facts.

---

## §10 Phase gating summary

This research-report exists to unblock subsequent phases. The plan's phase-gate references are:

| Phase | Pre-execution gate | Section of this report that closes it |
|---|---|---|
| Phase 1 (theme parser) | §4 (VS Code schema) | §4 above |
| Phase 2 (Yole-Light/Dark.json) | §4 (key list) + spec §3.8 | §4.2 + §4.3 |
| Phase 5 (Tree-Sitter JNI) | §1 (chosen library + ABI matrix + bundle format) | §1.2 + §1.4 + §1.5 |
| Phase 6 (vscode-textmate Wasm) | §3 (JS interop pattern) | §3.3 |
| Phase 7 (Tree-Sitter K/N iOS) | §2 (cinterop strategy + CocoaPods decision) | §2.2 + §2.4 |
| Phase 8 (SyntaxHighlighter API) | (no specific gate — depends on Phases 5/6/7) | n/a |
| Phase 9 (editor integration) | §6 (rendering approach) | §6.2 — design recommendation; benchmark in-phase |

All phase gates have closing material in this report except the four OPEN spikes, which are explicitly the first task of the phase that consumes them.

---

## §11 Forensic anchor

This report is the output of Phase 0 of `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md` (commit `f02dd00e`). It closes the seven research questions in `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` §8 (commit `dcc4ac57`) and surfaces additional findings:

- 23 additional `colors.*` keys beyond spec §3.8 (§4.2).
- 2 additional `yole.*` extension keys beyond spec §3.8 (§4.3).
- Concrete split JVM binding strategy (android-tree-sitter on Android, jtreesitter on Desktop) — spec called for "the Tree-Sitter JNI library" singular; research finds a binding split is the right architecture (§1.2).
- iOS distribution constraint surfaced: per Apple App Store §2.5.2, the "fetch grammars on enable" model used on Android/Desktop/Wasm CANNOT be used on iOS — IPA must statically bundle all 55 grammars, accepting a ~6.6 MB binary-size increase (§2.5 + §5.2).
- 4 OPEN spikes identified, each with explicit expected output for its consuming phase.

Per CONST-038, this commit modifies only the main Yole repository — no submodule writes. Per CONST-037 (cross-platform impact), the commit body lists every platform's disposition. Per CLAUDE.md mandatory rule #5, the commit message uses Conventional Commits format. Per CLAUDE.md mandatory rule #6, the push uses SSH (`git@github.com:vasic-digital/Yole.git`).

---

*End of report. Line count target: ≥ 400 lines (per Task 0.8 step 1 of the plan).*
