# Syntax Highlighting — Design Spec (Feature 1 of 5)

> **Status:** Brainstorm complete; awaiting user review of this file before invoking writing-plans.
> **Author:** brainstormed 2026-05-14 with operator.
> **Sequence:** Feature 1 of 5 in the comprehensive editor capability initiative. Features 2–5 (source-code file support, auto-complete, LSP integration, import from) follow in dependency order.

---

## 1. Goal

Add syntax highlighting to Yole as a first-class feature visible in three surfaces (the editor, the preview pane, and filename badges in the FILES tab), covering 50+ programming and markup languages, themable via the existing VS Code theme JSON format ecosystem, and shipping coherently to all four Yole user-visible platforms (Android, Desktop, iOS, Web) before any Firebase distribution.

**Secondary, equal-weight goal (operator constraint added 2026-05-14):** Unify Yole's entire theme system on VS Code JSON. The same JSON file that colorizes syntax tokens (via `tokenColors[]`) also colorizes the app UI itself (via `colors.*`) — backgrounds, surfaces, text, accents, line numbers, status bar, gutter, etc. All currently-hardcoded `IdeTheme.darkBackground` / `IdeTheme.lightBackground` / `YoleColors.*` get ported to VS Code JSON theme files and consumed through a single `ThemeProvider`. There is exactly one theme system in Yole after this feature.

**Tertiary constraint (operator 2026-05-14):** Markdown is the only format enabled by default. The other 17 existing formats and every newly-added source-code language MUST be opt-in via Settings. See §3.7.

## 2. Locked-in scope decisions (from brainstorming session)

| Decision | Choice |
|---|---|
| Sequencing in the 5-feature initiative | Dependency order, one at a time. Syntax highlighting is feature 1. |
| Language scope | Comprehensive — 50+ languages via Tree-Sitter + TextMate grammar bundles. |
| Tokenization engine | **Hybrid** — Tree-Sitter on JVM (Android+Desktop) + Kotlin/Native (iOS); TextMate on Wasm (Web). |
| Platform parity expectation for v1 | All 4 platforms before first Firebase distribution. |
| Theme system | VS Code theme JSON format directly (40 000+ existing themes consumable). |
| Surfaces where highlighting appears | Editor + Preview code blocks + filename badges in FILES tab. |
| Architecture | **Approach A** — shared module (`shared/src/commonMain/kotlin/digital/vasic/yole/syntax/`) with platform-specific engine adapters via `expect/actual`. |
| Format enablement | **Markdown is the only default-enabled format.** Every other format (the existing 17 non-markdown parsers) and every source-code language MUST be explicitly opted-in by the user via Settings. New constraint added by operator 2026-05-14 mid-spec; see §3.7 below. |
| Theme unification | **One theme system for the whole app.** Yole's existing IdeTheme / YoleColors hardcoded palettes are ported to VS Code JSON theme files (one file per theme, covering both `colors.*` for app UI and `tokenColors[]` for syntax). All consumers (app UI, editor, preview, filename badges) read through a single `ThemeProvider`. New constraint added by operator 2026-05-14 mid-spec; see §3.8 below. |

Per **CONST-038**, the syntax module is not yet a standalone submodule because Yole is the only consumer today. If a second consumer (e.g. Atmosphere/Lava) needs the same highlighter, the module is structured for clean extraction into a `SyntaxHighlighting-KMP` repo: the `expect class TokenizerEngine` boundary becomes the submodule's public API.

Per **CONST-037** (cross-platform impact), every commit in this feature MUST include a Cross-platform impact block listing each of Android/Desktop/iOS/Web. Some commits will legitimately list "deferred to later sub-task" for a platform, but never silently skip one.

## 3. Architecture & module layout

```
shared/src/commonMain/kotlin/digital/vasic/yole/syntax/
├── SyntaxHighlighter.kt          # Top-level API entry point
├── Token.kt                      # data class Token(range, scope, depth)
├── TokenSpan.kt                  # AnnotatedString-ready (range + Color)
├── TokenizerEngine.kt            # expect class — implemented per platform
├── Grammar.kt                    # Grammar metadata (lang id, scope mapping)
├── theme/
│   ├── Theme.kt                  # data class Theme(name, scopeToColor: Map<String,Color>)
│   ├── VsCodeThemeParser.kt      # Reads VS Code .json theme files
│   ├── ThemeRegistry.kt          # Lazy-loaded available themes + active StateFlow
│   └── builtin/                  # Bundled themes (light, dark, dracula, …)
├── grammar/
│   ├── GrammarRegistry.kt        # Lazy-loaded available languages
│   └── ScopeMapper.kt            # Tree-Sitter scope → VS Code scope translator
└── render/
    ├── HighlightedText.kt        # Composable: text → AnnotatedString
    └── PreviewCodeBlockHighlighter.kt  # Markdown/etc. code-fence highlighter

shared/src/androidMain/kotlin/digital/vasic/yole/syntax/
└── TokenizerEngine.android.kt    # Tree-Sitter via tree-sitter-java JNI

shared/src/desktopMain/kotlin/digital/vasic/yole/syntax/
└── TokenizerEngine.desktop.kt    # Tree-Sitter via same JNI (different .so/.dll/.dylib)

shared/src/iosMain/kotlin/digital/vasic/yole/syntax/
└── TokenizerEngine.ios.kt        # Tree-Sitter via Kotlin/Native cinterop

shared/src/wasmJsMain/kotlin/digital/vasic/yole/syntax/
└── TokenizerEngine.wasmJs.kt     # vscode-textmate via JS interop

androidApp/src/main/.../ui/editor/
└── SyncedScrollEditor.kt         # MODIFIED — accepts optional SyntaxHighlighter

desktopApp/src/main/.../ui/
└── (editor surface gets the same SyntaxHighlighter parameter)

iosApp/src/iosMain/.../          # iOS editor surface
webApp/src/wasmJsMain/.../       # Web editor surface
```

**Invariants:**

- Public surface (`SyntaxHighlighter`, `Theme`, `Token`) is 100% in `commonMain`.
- `expect class TokenizerEngine` exposes exactly two async functions: `suspend fun tokenize(text: String, lang: String): List<Token>` and `suspend fun loadGrammar(lang: String)`.
- Theme parser is shared — VS Code's JSON schema is platform-independent.
- Filename badges reuse `GrammarRegistry.detectByExtension(name)` for tinting.

### 3.7 Settings-driven format enablement (operator constraint 2026-05-14)

**Rule:** Markdown is the ONLY format enabled at first install (or after Settings → Reset). The user opts every other format / language in explicitly via Settings → Formats. This applies uniformly to:

- The existing 17 non-markdown format parsers (asciidoc, csv, json, latex, todotxt, …).
- Every source-code language added by Feature 2 (kotlin, python, javascript, …).
- The Tree-Sitter / TextMate grammars themselves: a grammar is **not loaded** unless its format is enabled (saves memory, APK/IPA size, and Wasm bundle weight).

**Storage:** Reuses the existing `enabledFormatIds: Set<String>` settings key (already in `androidApp/.../ui/YoleApp.kt` and persisted via `Settings.setEnabledFormatIds`). The default value becomes `setOf("markdown")` after this feature; previously it was effectively all-enabled.

**Settings UI:** Settings → Formats lists every available format/language grouped into sections:

```
Default (always on):
  ☑ Markdown                                  [system, can't disable]

Text formats (toggle):
  ☐ AsciiDoc                                  [size: 0.2 MB]
  ☐ reStructuredText                          [size: 0.2 MB]
  ☐ Org Mode                                  [size: 0.2 MB]
  …17 entries

Programming languages (toggle):
  ☐ Kotlin                                    [size: 0.4 MB]
  ☐ Python                                    [size: 0.3 MB]
  ☐ JavaScript                                [size: 0.3 MB]
  …50+ entries with per-grammar size shown

Each row also shows: file extensions, brief description, "Required for: <surface>".
```

**Editor behavior when a file's format is disabled:** the file opens, but the format is treated as plaintext (no highlighting, no special parsing). A non-blocking banner says: "*.kt is Kotlin source. Enable Kotlin in Settings → Formats to see highlighting?" with an "Enable" button that flips the toggle.

**Filename-badge behavior:** disabled formats show a generic gray badge; enabled formats show their theme-tinted badge.

**Migration for existing users:** on first launch after the upgrade, Yole detects the prior all-enabled state and offers a one-time **mandatory dialog** (cannot be dismissed without choosing): "Yole now enables only Markdown by default. Keep your previous formats enabled, or adopt the new default?" — choice is persisted; never asked again.

### 3.8 Unified theme system (operator constraint 2026-05-14)

**Rule:** Yole has exactly one theme system after this feature. It is the VS Code theme JSON format. Every color in the app — app background, status bar, tab bar, drawer, dialog, buttons, text, accents, gutter, line numbers, syntax tokens, badge tints — is sourced from the active VS Code theme JSON file's `colors.*` (app UI) and `tokenColors[]` (syntax) sections. There is no parallel `IdeTheme` / `YoleColors` Kotlin object.

**Migration:**

1. Yole's current hardcoded palettes (`IdeTheme.darkBackground = Color(0xFF1E1E1E)`, `YoleColors.Ide.LightMutedText`, etc.) are read out and re-expressed as two bundled VS Code JSON theme files:
   - `themes/builtin/Yole-Light.json` (mirrors today's light mode pixel-for-pixel).
   - `themes/builtin/Yole-Dark.json` (mirrors today's dark mode pixel-for-pixel).
2. `IdeTheme.kt` and `YoleColors.kt` are deleted. Every callsite that referenced them now reads from `ThemeProvider.current.color("editor.background")` etc.
3. A new `ThemeProvider` Composable wraps the entire app at the root, exposes the active `Theme` via CompositionLocal, and re-emits on theme change. Located in `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/ThemeProvider.kt` (Compose-multiplatform — usable from all 4 app shells).
4. The existing Settings "Theme mode" three-way switch (Light/Dark/System) becomes a four-way control: a dropdown of all available themes (Yole-Light, Yole-Dark, Dracula, Solarized Light, Solarized Dark, Dracula, One Dark, Monokai, GitHub Dark, Tokyo Night, Nord, Catppuccin, …) plus a "Match system" auto-pair (which themes are the light/dark pair for system match).
5. Theme JSONs are stored under:
   - JVM: bundled in resources `themes/builtin/*.json`; user themes under platform-specific app-data directory.
   - Wasm: bundled in `webApp/src/wasmJsMain/resources/themes/builtin/*.json`, hosted as static assets; user themes from IndexedDB or upload.
6. **Pixel-parity test** (anti-bluff): `Yole-Light.json` rendered through the new pipeline MUST produce the same RGB values as today's hardcoded `IdeTheme.light*`. Same for dark. Tested in `LegacyThemeParityTest` with mutation verification (flip a byte in `Yole-Light.json` → test FAILS).

**Schema mapping** (VS Code key → Yole consumer):

| VS Code key | Used by |
|---|---|
| `colors["editor.background"]` | Editor surface background, also app default content background. |
| `colors["editor.foreground"]` | Editor default text, also app default text color. |
| `colors["editorLineNumber.foreground"]` | Gutter line numbers (replaces current `IdeTheme.darkLineNumbers`). |
| `colors["editorLineNumber.activeForeground"]` | Active line gutter color. |
| `colors["statusBar.background"]` | Bottom status bar (line/col/format indicator). |
| `colors["statusBar.foreground"]` | Status bar text. |
| `colors["activityBar.background"]` | Bottom navigation bar background. |
| `colors["activityBar.foreground"]` | Bottom nav icon active color. |
| `colors["sideBar.background"]` | Drawer background. |
| `colors["editorWidget.background"]` | Dialog backgrounds. |
| `colors["focusBorder"]` | Accent / focus ring. |
| `colors["badge.background"]` + per-language overrides | Filename badge tint per language. |
| `tokenColors[]` | Syntax token colors as in standard VS Code. |

Yole-specific extensions (defined under a private `yole:` JSON key namespace so they don't conflict with VS Code's schema):
- `yole.bottomNav.iconActive` — bottom nav icon active color (fallback: `activityBar.foreground`).
- `yole.preview.codeBlockBorder` — markdown preview code block border (fallback: `editorWidget.background`).

These extensions are optional in any VS Code theme JSON. Themes that don't define them fall back to the standard VS Code keys.

## 4. Components & responsibilities

| Component | Purpose | Public surface |
|---|---|---|
| `SyntaxHighlighter` | Top-level entry; holds a `TokenizerEngine` + active `Theme`. | `highlight(text, lang): AnnotatedString`; `highlightFlow(textFlow, lang): Flow<AnnotatedString>`. |
| `TokenizerEngine` (expect) | The only platform-specific class. JVM uses Tree-Sitter JNI; iOS Kotlin/Native; Wasm vscode-textmate. | `suspend fun tokenize(text, lang)` + `suspend fun loadGrammar(lang)`. |
| `Theme` | Immutable scope → Color map; parsed from VS Code JSON. | `colorFor(scope: String): Color?`. |
| `VsCodeThemeParser` | Pure-Kotlin JSON parser using `kotlinx.serialization`. Understands VS Code `tokenColors` + `colors` schema. | `parse(json: String): Theme`. |
| `ThemeRegistry` | Discovers bundled + user themes (filesystem on JVM, asset bundle on Wasm). | `setActive(name)`; `activeTheme: StateFlow<Theme>`; `available(): List<String>`. |
| `GrammarRegistry` | Maps file extensions/MIME → language id; lazy-loads grammars. | `detectByExtension(filename)`; `loadGrammar(lang)`. |
| `ScopeMapper` | Translates Tree-Sitter scope names → VS Code scope names. | `treeSitterToVsCode(scope: String): String`. |
| `HighlightedText` (Composable) | Memoized rendering of text through the active highlighter. | Drop-in replacement for `Text(text)`. |
| `PreviewCodeBlockHighlighter` | Bridges existing format pipeline — replaces `<pre><code class="lang-x">` with token-CSS-class spans. | Plugs into existing `ParsedDocument.toHtml()`. |
| Updated `SyncedScrollEditor` | Adds optional `highlighter: SyntaxHighlighter?` parameter; renders AnnotatedString via custom `VisualTransformation`. | Backward-compatible — null highlighter = plain-text behavior. |
| `ThemeProvider` (Composable, new) | Wraps the entire app; provides active `Theme` via CompositionLocal; re-emits on theme change. Replaces existing `IdeTheme` / `YoleColors`. | `CompositionLocal<Theme>`; subordinate composables read with `LocalTheme.current.color(key)`. |
| `LegacyThemeBridge` (one-shot migration helper) | Maps the existing hardcoded `IdeTheme.dark*` / `YoleColors.*` Kotlin field values into the equivalent VS Code JSON keys. Used by the parity test in §7. | `bundleYoleLightJson(): String`; `bundleYoleDarkJson(): String`. Deleted in v1.1 after migration confidence is high. |
| `EnabledFormatGate` | Wraps every callsite that loads a grammar or invokes a format parser; refuses to act if the format is not in `Settings.enabledFormatIds`. | `requireEnabled(formatId)` throws `FormatDisabledException`; `isEnabled(formatId): Boolean`. |
| Settings → Formats screen | New screen listing every available format/language grouped (Default / Text formats / Programming languages); per-row toggle persists to `enabledFormatIds`. | Composable consumed by Settings sub-navigation. |

## 5. Data flow

### 5.1 Editor highlighting (live)

```
User keystroke
  → SyncedScrollEditor.textState.value updated (BasicTextField)
  → debounced(80ms) → SyntaxHighlighter.highlight(text, lang)
  → TokenizerEngine.tokenize(text, lang)              [platform actual]
     • JVM: Tree-Sitter JNI returns List<TokenNode>
     • iOS: Tree-Sitter cinterop returns same shape
     • Wasm: vscode-textmate.tokenizeLine() iterated returns same shape
  → List<Token>(range, scope)
  → ScopeMapper.treeSitterToVsCode(scope)             [only on Tree-Sitter platforms]
  → Theme.colorFor(scope)
  → AnnotatedString with SpanStyle per token range
  → BasicTextField receives the colored AnnotatedString via custom VisualTransformation
  → Compose recomposes only the changed range
```

Critical detail: tokenization happens on `Dispatchers.Default`. The editor never blocks the UI thread on tokenize. For files > 100 KB, Tree-Sitter's incremental parse re-uses the prior tree — only the changed subtree is re-walked. The TextMate (Wasm) path tokenizes line-at-a-time with per-line cache.

### 5.2 Preview code-block highlighting

```
ParsedDocument.toHtml() (existing)
  → PreviewCodeBlockHighlighter post-processes the HTML:
      for each <pre><code class="lang-{x}">…</code></pre>:
        SyntaxHighlighter.highlight(rawCode, x)
          → emits HTML with <span class="tok-{scope}">…</span>
  → StyleSheets.kt (existing) injects CSS mapping .tok-{scope} → theme color
  → WebView (Android), Desktop SwingWebView, iOS WKWebView, Wasm DOM all render
```

Reuses the existing `ParsedDocument` cache: highlight pass runs only on cache-miss.

### 5.3 Filename badges

```
FilesScreen renders each file row:
  GrammarRegistry.detectByExtension(file.name) → lang id (or "plaintext")
  Theme.colorFor("badge.${lang}") → tint Color
  Draw small colored chip next to filename
```

Pure metadata; zero tokenization cost.

## 6. Error handling

| Error | Detection | Recovery |
|---|---|---|
| Engine load failed at startup | `TokenizerEngine.initialize()` returns `Result.failure` | Editor falls back to plain text + logs telemetry. Settings shows yellow banner: "Syntax highlighting unavailable on this device". User can still edit. |
| Grammar load failed | `loadGrammar(lang)` throws | Highlight that file as plaintext. Inline notice in editor header: "Highlighting unavailable for .xyz files". |
| Malformed VS Code theme JSON | `VsCodeThemeParser.parse()` throws `ThemeParseException` | Refuse to set as active; keep prior theme; Toast: "Theme {name}.json failed to parse: line {n}, column {m}". |
| Theme missing color for scope | `Theme.colorFor(scope)` returns `null` | Fall back through scope hierarchy (`function.builtin` → `function` → default text color). Standard VS Code behavior. |
| Tokenizer timeout on huge file (> 5 MB) | `withTimeout(2.seconds)` wraps `tokenize` | Highlight first 50 KB; mark rest plain. Status bar: "Large file — highlighting first 50 KB". |
| Theme bundle missing on Wasm | Asset fetch fails (HTTP 404) | Use baked-in fallback; dev-console warning. |
| `CancellationException` during tokenize | Standard coroutine cancellation | Rethrow (CONST-035 + Detekt `SwallowedException`). Editor keeps prior highlight until new one completes. |

All error paths are covered by anti-bluff tests in §7.

## 7. Testing strategy (anti-bluff)

### 7.1 Unit tests (`commonTest`, no UI)

1. **`VsCodeThemeParserTest`** — parses 12 real VS Code theme JSONs (Light+, Dark+, Solarized Light/Dark, Dracula, One Dark, Monokai, Material, Quiet Light, GitHub Dark, Tokyo Night, Nord, Catppuccin). Asserts known scope → color mapping. Includes 2 deliberately malformed JSONs that MUST throw `ThemeParseException`.
2. **`ScopeMapperTest`** — table-driven mapping of 60+ Tree-Sitter scopes → VS Code scopes, covering every category (function, keyword, type, string, comment, punctuation, …).
3. **`GrammarRegistryTest`** — extension detection for 30 most-used languages. `.kt` → kotlin, `.tsx` → typescriptreact, ambiguous `.h` → list-with-confidence.
4. **`ThemeRegistryTest`** — discovery + active-theme StateFlow + theme-switch propagation.

### 7.2 Platform-engine integration tests

5. **`TokenizerEngineJvmTest`** (desktopTest) — feeds each of the 50+ grammars a 100-line snippet of real code, asserts token count is non-trivial (≥ line count), and that the first non-whitespace token's scope matches the expected category.
6. **`TokenizerEngineWasmTest`** (wasmJsTest) — same snippets, same assertions, through the TextMate path.
7. **iOS Kotlin/Native test** — same.

### 7.3 Anti-bluff structural tests

8. **`SyntaxHighlightingSourceInvariantsTest`** — like `EditorScrollSyncRobolectricTest` from iter-55. Asserts source-level rules: `commonMain/.../syntax/` contains zero platform-specific imports; every `expect` has 4 `actuals`; no `runBlocking` in production. Mutation-verified.
9. **`ThemeFidelityTest`** — for each bundled theme, take a known code snippet, tokenize, render to AnnotatedString, hash the spans → assert deterministic. Reverting a theme byte must change the hash; mutation-verified.

### 7.4 End-to-end Robolectric (Android)

10. **`EditorHighlightingRobolectricTest`** — types `fun main() { println("hi") }` into the editor with kotlin grammar active. Asserts the rendered AnnotatedString contains a `SpanStyle` at the `fun` range with the active theme's keyword color. Mutation: revert the BasicTextField → plain `Text` → test must FAIL.

### 7.5 Challenges (wired into `make qa-all`)

11. **`yole-challenges/scripts/syntax_highlighting_challenge.sh`** — runs all 7 test suites; requires positive PASS evidence (case counts + log paths) per CONST-035.
12. **`yole-challenges/scripts/syntax_highlighting_per_platform_challenge.sh`** — verifies all 4 `*Main` source sets contain a `TokenizerEngine` actual, per CONST-037.
13. **`yole-challenges/scripts/theme_unification_challenge.sh`** — grep-asserts that `IdeTheme.kt` and `YoleColors.kt` no longer exist in the codebase (after migration), AND that any production source referencing legacy theme symbols is flagged. Runtime layer: runs `LegacyThemeParityTest` and `ThemeUiCoverageTest`.
14. **`yole-challenges/scripts/format_enablement_default_challenge.sh`** — asserts the default `enabledFormatIds` is `setOf("markdown")` (exactly one, exactly that id). Mutation-verified: changing the default to any other set MUST fail. Plus a runtime probe that loads a fresh `SettingsState` and asserts only markdown is enabled.

### 7.6 New anti-bluff tests for the two operator constraints

15. **`LegacyThemeParityTest`** (`commonTest` or `desktopTest`, JVM) — for each color field in the old `IdeTheme` / `YoleColors`:
    a. Read the old hardcoded value via the `LegacyThemeBridge` helper.
    b. Load the new `Yole-Light.json` / `Yole-Dark.json` via the new parser.
    c. Assert exact RGB equality.
    Mutation: flip any byte in `Yole-Light.json` → test FAILS.
16. **`ThemeUiCoverageTest`** (`desktopTest` JVM) — instantiates every Composable that uses `LocalTheme.current` and asserts each invocation returns a non-null Color. Catches missing keys after theme migration. Mutation: delete `colors["editor.background"]` from `Yole-Light.json` → test FAILS.
17. **`FormatEnablementDefaultTest`** (`commonTest`) — fresh install: `Settings.getEnabledFormatIds()` returns exactly `setOf("markdown")`. Mutation: change the default to `setOf("markdown", "asciidoc")` → test FAILS.
18. **`FormatEnablementGateTest`** (`commonTest`) — attempting to load a Kotlin grammar while Kotlin is disabled throws `FormatDisabledException`. Mutation: remove the gate's `require()` → test FAILS.
19. **`FormatsSettingsScreenRobolectricTest`** (`androidApp` Robolectric) — opens the Settings → Formats screen; asserts every grouped section is present; toggling a row updates `enabledFormatIds` in the SettingsStore. Mutation: short-circuit the toggle callback → test FAILS.

### 7.6 Anti-bluff covenant verification

Every test above carries mutation-verification discipline from iter-55: at least one mutation that deletes the production code → the test MUST FAIL. The mutation procedure is captured in each test class's KDoc and re-executed before commit.

## 8. Deep-research checklist (per operator mandate)

Before writing any code, the implementation plan MUST include a research task that produces a report covering:

- **Tree-Sitter on JVM**: state of the art for tree-sitter-java JNI wrappers; license; ABI stability; how Android packages native libs per-ABI (`armeabi-v7a` + `arm64-v8a` + `x86_64`); how to bundle grammars (.so + .json scope file) without bloating APK; Tree-Sitter incremental parse API; how to handle large files.
- **Tree-Sitter on Kotlin/Native for iOS**: cinterop bindings; tree-sitter as a CocoaPods dependency; static linking constraints; App Store review notes on native libs.
- **vscode-textmate on Wasm**: JS-interop bridge from Kotlin/Wasm; how vscode-textmate loads grammar JSON; oniguruma regex engine on Wasm.
- **VS Code theme JSON schema**: official schema; supported keys (`tokenColors`, `colors`, `semanticTokenColors`); how scope inheritance works.
- **Industry references**: how IntelliJ implements editor highlighting; CodeMirror 6's Lezer approach (potential fallback); Highlights.kt and kotlinx-highlights libraries (Compose-native, possible reuse).
- **Open-source codebases to study**: tree-sitter-android (Yiri/code-editor-android), CodeMirror 6, vscode-textmate, Atom legacy first-mate, Sublime Text Lapce editor (Rust + Tree-Sitter).

This research feeds the implementation plan; assumptions about engine APIs are validated against actual upstream docs before any line of code is written.

## 9. Documentation deliverables

Independent of code, this feature ships with:

- `docs/features/syntax-highlighting/user-guide.md` — end-user documentation (how to switch themes, add custom themes, see supported languages, enable formats in Settings).
- `docs/features/syntax-highlighting/architecture.md` — implementation reference for future contributors; diagrams of token pipeline, theme parser flow, per-platform engine bindings.
- `docs/features/syntax-highlighting/research-report.md` — output of §8.
- `docs/features/syntax-highlighting/theme-migration-guide.md` — for users carrying custom themes from prior versions, mapping legacy palette keys → VS Code JSON keys. Also describes the `yole.*` extension namespace.
- `docs/features/syntax-highlighting/settings-formats-guide.md` — end-user documentation for the new Settings → Formats screen.
- `docs/diagrams/syntax-highlighting-dataflow.svg` — Mermaid or D2 diagram of the token pipeline.
- `docs/diagrams/theme-unification-flow.svg` — diagram showing legacy IdeTheme → VS Code JSON migration.
- `docs/diagrams/settings-formats-screen.svg` — Settings → Formats wireframe.
- Website update (under existing site infrastructure) — feature landing page with:
  - Screenshots of editor + preview + filename badges across the 4 platforms.
  - Live Wasm demo of theme switching.
  - Gallery of bundled themes.
  - "Enable formats in Settings" walkthrough video / GIF.
- Updated CHANGELOG.md entry.
- Updated docs/CONTINUATION.md section per CONST-036.

## 10. Firebase distribution checklist

Only after all 50+ unit tests + 12 platform-engine tests + the 2 challenges PASS, AND all 4 platforms compile + smoke-test through a real user-facing flow:

1. Android debug + release APK with bundled Tree-Sitter `.so` per ABI.
2. Desktop tarballs for Linux x64 / Windows x64 / macOS arm64 with platform-specific Tree-Sitter native libs.
3. iOS IPA with Tree-Sitter linked via Kotlin/Native.
4. Web Wasm bundle with vscode-textmate grammar JSONs.

Per CONST §6.K, container-strict release. Per `KNOWN_DEFECTS.md`, the existing `#helixqa-missing-sibling-repos` environmental dependency does not block this feature.

## 11. Out of scope for v1

- Auto-complete (feature 3).
- LSP-driven semantic highlighting (feature 4, on top of LSP integration).
- Diagnostics / error squigglies (feature 4).
- Theme marketplace UI (v2 — for now, drop-in JSON files in a known directory work).
- Bracket-pair colorization (v2).
- Folding regions (v2).
- Per-format granular enablement *sub-options* (e.g., "enable Kotlin syntax but not auto-complete suggestions") — v1 has one toggle per format covering all sub-capabilities.

## 12. Open questions for the implementation plan

- Specific Tree-Sitter JNI library version (latest stable as of 2026-05-14 — research task validates).
- Grammar bundle distribution strategy: vendor compiled `.so` per language + ABI, or fetch on-demand from a CDN at first-use? Tradeoff: APK size vs. cold-start latency.
- Whether Compose's `BasicTextField` `VisualTransformation` or a custom `TextLayoutResult`-based renderer gives the best perf on long files. Microbenchmark required.

## 13. Forensic anchor

This spec is the product of brainstorming session 2026-05-14, in which the operator chose:
- 50+ language scope ("comprehensive: Tree-Sitter / TextMate full bundle")
- Hybrid engine ("Tree-Sitter on JVM + KN, TextMate on Wasm")
- All-4-platforms-before-ship ("v1 ships only after Android + Desktop + iOS + Web all work")
- VS Code theme JSON format directly
- Editor + Preview + filename badges
- Approach A architecture (shared module with expect/actual)

Approval recorded at each of the 7 question/answer rounds. No question's choice was a default — every answer was explicit.

**Two additional constraints added by the operator mid-spec (2026-05-14), each verbatim:**

> "Default selected enabled format should still be Markdown, however all other supports — other formats and source code handling MUST BE enabled from the application Settings!"

> "Regarding the themes, we MUST use now VSCode themes format everywhere and all existing themes to be ported into this format so we have proper consistency! Make sure everything is covered with all supported types of the tests and Challenges with full anti-bluff policy enforcement EVERYWHERE! All documentation and relevant materials MUST BE fully updated and extended!"

These two mandates were folded into the design as §3.7 (format enablement) and §3.8 (theme unification), along with the matching anti-bluff tests in §7.6 and the expanded documentation deliverables in §9.

---

**Next steps after operator review of this spec:**

1. Operator reviews + requests any changes.
2. Invoke `superpowers:writing-plans` skill to produce the detailed implementation plan at `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md`.
3. The plan begins with the deep-research task in §8 (output goes to `docs/features/syntax-highlighting/research-report.md` before any code).
4. Implementation phases follow with bite-sized TDD tasks, mutation-verified anti-bluff tests, cross-platform impact tracking in each commit body per CONST-037, and Firebase distribution at the end.
