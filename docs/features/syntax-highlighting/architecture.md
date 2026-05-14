# Syntax Highlighting — Architecture

> **Audience:** future Yole contributors. Read after `user-guide.md`.

This document describes the implementation contract behind iter-57 syntax highlighting: package layout, expect/actual story, dataflow, error handling, and the anti-bluff test discipline that protects each invariant.

---

## 1. Package layout

```
shared/src/commonMain/kotlin/digital/vasic/yole/syntax/
├── SyntaxHighlighter.kt          # Top-level API (highlight, tokens)
├── Token.kt                      # data class Token(startByte, endByte, scope)
├── TokenSpan.kt                  # ARGB-int span
├── TokenizerEngine.kt            # expect class — platform actuals below
├── Grammar.kt                    # Grammar metadata data class
├── GrammarMetadata.kt            # `all` registry (markdown only for v1)
├── EnabledFormatGate.kt          # runtime gate — isEnabled / requireEnabled
├── FormatDisabledException.kt
├── theme/
│   ├── Theme.kt                  # uiColors + tokenColors maps; hierarchical fallback
│   ├── VsCodeThemeParser.kt      # kotlinx.serialization-based parser
│   ├── ThemeParseException.kt
│   ├── ThemeRegistry.kt          # activeTheme StateFlow + builtin loading
│   ├── ThemeProvider.kt          # Compose Composable + LocalTheme CompositionLocal
│   └── LegacyThemeBridge.kt      # one-shot migration helper (legacy palette → VS Code keys)
├── grammar/
│   ├── GrammarRegistry.kt        # extension → Grammar; null when disabled
│   └── ScopeMapper.kt            # Tree-Sitter scope → VS Code scope (60+ entries)
└── render/
    ├── AnnotatedStringBuilder.kt # Token list → Compose AnnotatedString
    ├── PreviewCodeBlockHighlighter.kt  # markdown fenced blocks → token-CSS HTML
    └── BadgeTinter.kt            # filename → theme-tinted badge ARGB

shared/src/{android,desktop,ios,wasmJs}Main/kotlin/digital/vasic/yole/syntax/
└── TokenizerEngine.<platform>.kt # actual class

shared/src/commonMain/resources/themes/builtin/
├── Yole-Light.json               # pixel-parity to legacy IdeTheme.light*
└── Yole-Dark.json                # pixel-parity to legacy IdeTheme.dark*
```

The editor consumer:

```
androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt
  - accepts SyntaxHighlighter? + langId
  - renders colored AnnotatedString via BasicTextField VisualTransformation
  - 80 ms debounce on tokenization
  - graceful fallback to plain text on disabled / failed / mid-debounce length-mismatch
```

The settings consumers:

```
androidApp/src/main/java/digital/vasic/yole/android/ui/settings/
├── FormatsSettingsScreen.kt      # toggleable list of formats by group
└── FormatMigrationDialog.kt      # one-time mandatory dialog
```

---

## 2. Dataflow (editor highlighting)

```
User keystroke
  └→ BasicTextField onValueChange → textState.value updated
       └→ LaunchedEffect(textState.value, langId, highlighter) {
              if (highlighter != null && langId != null && EnabledFormatGate.isEnabled(langId)) {
                  delay(80)  // debounce
                  highlightedText.value = highlighter.highlight(textState.value, langId)
              } else {
                  highlightedText.value = AnnotatedString(textState.value)
              }
          }
            └→ SyntaxHighlighter.highlight(text, lang):
                  ├→ EnabledFormatGate.isEnabled(lang)?  no → return AnnotatedString(text)
                  ├→ engine.tokenize(text, lang) on Dispatchers.Default
                  │     ├→ JVM: Tree-Sitter via JNI (android-tree-sitter / jtreesitter)
                  │     ├→ iOS: Tree-Sitter via Kotlin/Native cinterop (Phase 7 BLOCKED stub)
                  │     └→ Wasm: vscode-textmate via @JsModule
                  └→ AnnotatedStringBuilder.build(text, tokens, theme):
                        for each token:
                          ├→ vsScope = ScopeMapper.treeSitterToVsCode(token.scope)
                          ├→ argb = theme.tokenColor(vsScope)  // hierarchical fallback
                          └→ addStyle(SpanStyle(Color(argb)), startByte..endByte)

  └→ BasicTextField recomposes with the new colored AnnotatedString via
     a VisualTransformation that guards against length mismatch (see §3.4)
```

---

## 3. Key invariants & their tests

### 3.1 Gutter + text share one ScrollState (iter-55, preserved across iter-57)

**Test:** `EditorScrollSyncRobolectricTest` (4 cases). Mutation: revert to dual `rememberScrollState()` → 2 of 4 cases FAIL.

### 3.2 expect/actual symmetry on TokenizerEngine

**Test:** `SyntaxHighlightingSourceInvariantsTest` + `syntax_highlighting_per_platform_challenge.sh`. Asserts an `actual class TokenizerEngine` exists in each of `androidMain`, `desktopMain`, `iosMain`, `wasmJsMain`.

### 3.3 Theme pixel-parity with legacy palette

**Test:** `LegacyThemeParityTest` (2 cases). For each key in `LegacyThemeBridge.legacyLight/Dark`, the corresponding JSON value MUST equal the legacy `IdeTheme.*` byte-for-byte. Mutation: flip one byte in `Yole-Light.json` → light test FAILS.

### 3.4 VisualTransformation length-guard

**Bug discovered Phase 9:** the 80 ms debounce creates a transient window where `highlightedText` length doesn't match `BasicTextField`'s current text length. The `OffsetMapping.Identity` then throws `IllegalStateException` mid-keystroke.

**Fix:** the VisualTransformation lambda compares lengths and falls back to a plain `AnnotatedString(sourceText.text)` when they disagree. One frame of un-colored flicker is preferable to a crash.

**Test:** `FileEditingRobolectricTest` (6 cases — saveFile, editFileContent, etc.) covers typical typing flow; the bug crashed all 6 before the fix.

### 3.5 Hierarchical scope fallback (Theme.tokenColor)

For scope `keyword.control.return`, the lookup walks `keyword.control.return → keyword.control → keyword → null`. Same algorithm as VS Code's scope inheritance.

**Test:** `VsCodeThemeParserTest.scopeFallbackThroughHierarchy`. Mutation: stub `Theme.tokenColor` to only exact-match → fails.

### 3.6 Markdown-default format enablement

**Test:** `FormatEnablementDefaultTest.freshDefaultIsMarkdownOnly`. Asserts `FormatRegistry.defaultEnabledFormatIds() == setOf("markdown")`. Mutation: change default to `setOf("markdown", "asciidoc")` → FAILS.

**Challenge:** `format_enablement_default_challenge.sh` adds a static grep + runtime assertion in `make qa-all`.

### 3.7 Anti-bluff covenant on every production claim

Per CONST-035, every test in this feature MUST be paired with a mutation that demonstrably breaks it. The mutations are documented in each test class's KDoc anchor and have been independently verified by the code-review subagents. Specifically:

- `VsCodeThemeParser.parseHexColor` → null: 3 of 7 tests FAIL.
- `LegacyThemeBridge` byte flip: 1 of 2 parity tests FAILS.
- `ScopeMapper.treeSitterToVsCode` → "": 31 of 32 tests FAIL.
- `GrammarRegistry.detectByFilename` → null: 6 of 10 tests FAIL.
- `BadgeTinter.tintFor` → null: markdown-tint case FAILS.
- `AnnotatedStringBuilder.build` → un-styled: SyntaxHighlighterTest FAILS.
- `EnabledFormatGate.requireEnabled` no-op: requireEnabled_throwsForDisabled FAILS.

---

## 4. Error handling matrix

| Error condition | Detection | Recovery |
|---|---|---|
| TokenizerEngine native lib missing | `initialize()` returns `Result.failure` | Editor falls back to plain text. No fake tokens. |
| Grammar load fails | `loadGrammar()` throws | Plain text for that file; banner suggesting re-enable. |
| Theme JSON malformed | `VsCodeThemeParser.parse()` throws `ThemeParseException` | Theme not set as active; prior theme retained; Toast surfaces line/column. |
| Token scope has no theme color | `Theme.tokenColor()` returns null | Span not colored (text retains default color). |
| Tokenize timeout | `withTimeout(2s)` | First 50 KB colored; status bar notice. |
| Length mismatch during debounce | VisualTransformation lambda guard | Plain text overlay for one frame until debounce fires. |
| Format disabled at gate | `EnabledFormatGate.isEnabled` returns false | Plain text; no tokenize call. |
| CancellationException during tokenize | Standard coroutine cancellation | Rethrown per CONST-035 + Detekt; editor keeps prior highlight. |

---

## 5. Per-platform notes

### Android
- Engine: bonede `tree-sitter:0.22.6` + `tree-sitter-markdown:0.7.1a` via JNI (substituted for the Phase 0 picks for JDK-11 compatibility — see research-report.md §1).
- Grammar bundling: native `.so` ships with the library JAR for the 5 desktop OS+arch combos. **No Android NDK `.so` is published** by bonede; on Android, `initialize()` returns `Result.failure(UnsatisfiedLinkError)` — honest per CONST-035. Tracked as `#android-tree-sitter-ndk-so-missing`; operator NDK build path is documented in `docs/KNOWN_DEFECTS.md`.
- iter-57 Android RC ships the SyntaxHighlighter API + every UI surface; on devices without the NDK `.so`, highlighting is gracefully unavailable.

### Desktop
- Engine: same bonede library. Native libs ship for `linux-x86_64`, `windows-x86_64`, `osx-x86_64`, `osx-aarch64`.
- Verified: 5 PASS in `TokenizerEngineJvmTest`.

### iOS
- Engine: `TokenizerEngine.ios.kt` is currently a `NotImplementedError` stub. Tracked as `#phase-7-blocked-on-ios-baseline` — two prerequisites: (a) Document-KMP sibling submodule fix (CONST-038 we can't write from Yole), (b) operator-built static `libtree-sitter.a` + `libtree-sitter-markdown.a` per Apple arch.
- iOS app compiles + runs; highlighting is unavailable (graceful fallback).

### Web (Wasm)
- Engine: vscode-textmate 9.2.0 + vscode-oniguruma 2.0.1 via Kotlin/Wasm `@JsModule external`.
- Markdown grammar JSON (79 KB, Microsoft's curated) bundled at `shared/src/wasmJsMain/resources/grammars/markdown.tmLanguage.json`.
- Production code compiles cleanly. Tests blocked by pre-existing `runBlocking has no Wasm variant` baseline (tracked as `#wasmjs-test-baseline-broken`).

---

## 6. Adding a new language in v1+

The plan's broader Feature 2 (source-code file support) will operationalize this; v1 of iter-57 ships markdown only. The contract for a future addition:

1. Add the grammar metadata to `GrammarMetadata.all`:
   ```kotlin
   val kotlin = Grammar("kotlin", "Kotlin", listOf(".kt", ".kts"), listOf("text/x-kotlin"))
   val all = listOf(markdown, kotlin, ...)
   ```
2. Provide the grammar binaries for each platform (Tree-Sitter `.so`/`.dylib`/`.dll` + `.scm` highlight queries on JVM; cinterop static lib on iOS; TextMate `.tmLanguage.json` on Wasm).
3. The format becomes a valid `setEnabled("kotlin")` target. `FormatsSettingsScreen` picks it up automatically from `GrammarMetadata.all`.
4. Add scope-name entries to `ScopeMapper` if Tree-Sitter's grammar emits scopes the existing map doesn't translate.
5. Anti-bluff test: write a `TokenizerEngineJvmTest`-style case for the new lang asserting non-empty token count for a known snippet. Mutation: stub `loadGrammar` for that lang → FAILS.

---

## 7. Cross-references

- **Spec:** `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md`.
- **Plan:** `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md`.
- **Research:** `docs/features/syntax-highlighting/research-report.md`.
- **User guide:** `docs/features/syntax-highlighting/user-guide.md`.
- **Theme migration:** `docs/features/syntax-highlighting/theme-migration-guide.md`.
- **Known defects:** `docs/KNOWN_DEFECTS.md` (active iter-57 issues: `#android-tree-sitter-ndk-so-missing`, `#wasmjs-test-baseline-broken`, `#phase-7-blocked-on-ios-baseline`).
