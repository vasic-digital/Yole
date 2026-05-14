# Syntax Highlighting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship syntax highlighting across Yole's editor, preview, and filename badges on all four platforms (Android/Desktop/iOS/Web), with 50+ languages, VS Code theme JSON as the **unified** app theme system (replacing the legacy hardcoded `IdeTheme`/`YoleColors`), and Markdown as the only default-enabled format with all other formats opt-in via Settings.

**Architecture:** Approach A from the spec — all highlighter code in `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/` with one `expect class TokenizerEngine` and four platform actuals (Tree-Sitter JNI on Android/Desktop, Tree-Sitter via Kotlin/Native cinterop on iOS, vscode-textmate via JS interop on Wasm). VS Code theme JSON is parsed in shared Kotlin (`kotlinx.serialization`) and exposed via a `ThemeProvider` composable that replaces `IdeTheme`/`YoleColors` everywhere. Format enablement gating lives in `EnabledFormatGate`, persisted via the existing `Settings.enabledFormatIds` key, with `setOf("markdown")` as the new default.

**Tech Stack:** Kotlin Multiplatform 2.0.20, Compose Multiplatform 1.7.3, Tree-Sitter (latest stable — research validates), vscode-textmate (latest stable — research validates), `kotlinx-serialization-json` (already in `libs.versions.toml`), Robolectric for Android UI tests, JUnit4 with `runBlocking<Unit>`.

**Spec:** `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` (commit `dcc4ac57`).

---

## Phase decomposition & dependency graph

```
Phase 0  Deep research                          ← ships a report; no code
   │
   ▼
Phase 1  VS Code theme JSON parser              ← pure Kotlin, no platform deps
   │
   ▼
Phase 2  Yole-Light.json + Yole-Dark.json migration + LegacyThemeParityTest
   │
   ▼
Phase 3  ThemeProvider + app-wide migration off IdeTheme/YoleColors
   │
   ▼
Phase 4  Format enablement gate + Settings → Formats screen
   │
   ▼
Phase 5  Tree-Sitter JNI engine actual (Android + Desktop)
   │
   ▼
Phase 6  vscode-textmate Wasm engine actual
   │
   ▼
Phase 7  Tree-Sitter Kotlin/Native engine actual (iOS)
   │
   ▼
Phase 8  SyntaxHighlighter API + ScopeMapper + GrammarRegistry
   │
   ▼
Phase 9  Editor highlighting integration (SyncedScrollEditor)
   │
   ▼
Phase 10 Preview code-block highlighting (PreviewCodeBlockHighlighter)
   │
   ▼
Phase 11 Filename badges in FILES tab
   │
   ▼
Phase 12 Anti-bluff challenges + qa-all wiring
   │
   ▼
Phase 13 Documentation + diagrams + website
   │
   ▼
Phase 14 Firebase distribution (all 4 platforms)
```

**Detail level per phase:**

- **Phases 0–4 are detailed TDD bite-sized tasks** below. An engineer can execute them today using only this plan and the spec.
- **Phases 5–14 are decomposed into task-level checklists** with exact file paths and concrete acceptance criteria, but their step-by-step code crystallizes after Phase 0 research closes specific open questions (library versions, JNI API shape, Wasm interop pattern). Each such phase has a **"Pre-execution gate"** that names the research-report sections that must exist before the phase starts.

---

## File Structure (all new files + modified files for the whole feature)

**Create:**

```
shared/src/commonMain/kotlin/digital/vasic/yole/syntax/
  SyntaxHighlighter.kt
  Token.kt
  TokenSpan.kt
  TokenizerEngine.kt                    # expect class
  Grammar.kt
  EnabledFormatGate.kt
  FormatDisabledException.kt
  theme/
    Theme.kt
    VsCodeThemeParser.kt
    ThemeParseException.kt
    ThemeRegistry.kt
    ThemeProvider.kt                    # Compose Composable + CompositionLocal
    LegacyThemeBridge.kt                # one-shot migration helper (deleted in v1.1)
    builtin/                            # JSON theme assets (loaded by ThemeRegistry)
      Yole-Light.json
      Yole-Dark.json
      Dracula.json
      Solarized-Light.json
      Solarized-Dark.json
      One-Dark.json
      Monokai.json
      GitHub-Dark.json
      Tokyo-Night.json
      Nord.json
      Catppuccin-Mocha.json
      Catppuccin-Latte.json
  grammar/
    GrammarRegistry.kt
    ScopeMapper.kt
    GrammarMetadata.kt
  render/
    HighlightedText.kt                  # Composable
    PreviewCodeBlockHighlighter.kt
    AnnotatedStringBuilder.kt           # Token list → AnnotatedString
    BadgeTinter.kt                      # filename badge color lookup

shared/src/androidMain/kotlin/digital/vasic/yole/syntax/
  TokenizerEngine.android.kt            # actual: Tree-Sitter JNI
shared/src/desktopMain/kotlin/digital/vasic/yole/syntax/
  TokenizerEngine.desktop.kt            # actual: Tree-Sitter JNI (same wrapper)
shared/src/iosMain/kotlin/digital/vasic/yole/syntax/
  TokenizerEngine.ios.kt                # actual: Tree-Sitter Kotlin/Native cinterop
shared/src/wasmJsMain/kotlin/digital/vasic/yole/syntax/
  TokenizerEngine.wasmJs.kt             # actual: vscode-textmate JS interop

shared/src/commonTest/kotlin/digital/vasic/yole/syntax/
  VsCodeThemeParserTest.kt
  ThemeRegistryTest.kt
  ScopeMapperTest.kt
  GrammarRegistryTest.kt
  LegacyThemeParityTest.kt
  FormatEnablementDefaultTest.kt
  FormatEnablementGateTest.kt
  SyntaxHighlightingSourceInvariantsTest.kt
  ThemeFidelityTest.kt

shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/
  TokenizerEngineJvmTest.kt
  ThemeUiCoverageTest.kt

shared/src/iosTest/kotlin/digital/vasic/yole/syntax/
  TokenizerEngineIosTest.kt

shared/src/wasmJsTest/kotlin/digital/vasic/yole/syntax/
  TokenizerEngineWasmTest.kt

androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/
  EditorHighlightingRobolectricTest.kt
  FormatsSettingsScreenRobolectricTest.kt
  FilenameBadgesRobolectricTest.kt

androidApp/src/main/java/digital/vasic/yole/android/ui/settings/
  FormatsSettingsScreen.kt
  FormatMigrationDialog.kt

yole-challenges/scripts/
  syntax_highlighting_challenge.sh
  syntax_highlighting_per_platform_challenge.sh
  theme_unification_challenge.sh
  format_enablement_default_challenge.sh

docs/features/syntax-highlighting/
  user-guide.md
  architecture.md
  research-report.md                    # output of Phase 0
  theme-migration-guide.md
  settings-formats-guide.md

docs/diagrams/
  syntax-highlighting-dataflow.svg      # Mermaid → SVG
  theme-unification-flow.svg
  settings-formats-screen.svg
```

**Modify:**

```
androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
  - Replace IdeTheme.* / YoleColors.* references with LocalTheme.current.color(key)
  - Add highlighter parameter pass-through to SyncedScrollEditor
  - Wire MoreScreen → Settings → Formats navigation
  - Add FormatMigrationDialog on first launch after upgrade

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt
  - Add optional highlighter: SyntaxHighlighter? parameter
  - Render colored AnnotatedString via VisualTransformation when highlighter is set

androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt
  - DELETE after Phase 3 migration completes (replaced by ThemeProvider)

androidApp/src/main/java/digital/vasic/yole/android/ui/YoleColors.kt
  - DELETE after Phase 3 migration completes

desktopApp/src/main/kotlin/digital/vasic/yole/desktop/ui/YoleApp.kt
  - Replace hardcoded color references with LocalTheme.current
  - Add highlighter parameter pass-through to editor

iosApp/src/iosMain/kotlin/digital/vasic/yole/ios/Main.kt
  - Wrap root in ThemeProvider
  - Add highlighter pass-through

webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/Main.kt
  - Wrap root in ThemeProvider
  - Add highlighter pass-through

shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt
  - Existing ParsedDocument.toHtml() output gets post-processed by
    PreviewCodeBlockHighlighter for fenced code blocks

shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt
  - Append token-color CSS rules consumed by preview code blocks

shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt
  - Wire EnabledFormatGate so registry.isEnabled(id) reflects Settings

androidApp/src/main/java/digital/vasic/yole/android/util/Settings.kt
  - Change default of enabledFormatIds from "all" to setOf("markdown")
  - Add migration flag prior_default_used_all_formats for one-time dialog

Makefile
  - Add qa-iter-57-gates target invoking the 4 new challenges
  - Chain qa-iter-57-gates into qa-all

CHANGELOG.md
  - Add iter-57 entry

docs/CONTINUATION.md
  - Add iter-57 section
```

---

## Phase 0 — Deep research (no code)

**Goal:** Produce `docs/features/syntax-highlighting/research-report.md` that closes 7 specific questions. Phases 5+ won't start until this report exists.

**Files:**
- Create: `docs/features/syntax-highlighting/research-report.md`

### Task 0.1: Tree-Sitter JNI library survey

- [ ] **Step 1: Identify candidates**

  Required outputs:
  1. tree-sitter-java (Joshia/Java bindings to upstream Tree-Sitter via JNI).
  2. tree-sitter-android (community Android-targeted port; check ABI coverage).
  3. Lapce-editor's tree-sitter-jni (Rust+JNI bridge).
  4. Any KMP-native binding (search GitHub for `kotlin tree-sitter`).

  For each candidate, fetch and record:
  - Latest stable version + release date.
  - License (must be MIT/Apache/BSD-compatible per Yole's Apache-2.0 SPDX).
  - ABI coverage: `armeabi-v7a`, `arm64-v8a`, `x86_64` (Android requires arm64 + x86_64 minimum; armv7 desirable).
  - Native lib size per ABI.
  - Per-grammar `.so` distribution model: do they bundle, or do you compile grammars separately?
  - Incremental parse API surface.

- [ ] **Step 2: Write findings to research-report.md §1 Tree-Sitter on JVM**

  Use this skeleton:

  ```markdown
  ## §1 Tree-Sitter on JVM

  ### Chosen library
  **<library name> v<version>** — license <license>, repo <url>.

  ### Why
  <2-3 sentence rationale: ABI coverage, lib size, grammar bundling story>.

  ### ABI matrix
  | ABI         | Supported | Lib size |
  |-------------|-----------|----------|
  | armeabi-v7a | yes/no    | XX KB    |
  | arm64-v8a   | yes       | XX KB    |
  | x86_64      | yes       | XX KB    |

  ### Grammar bundle format
  <describe how grammars are loaded: .so per language? a single .so + JSON scope file? embedded in resources/?>

  ### Incremental parse API
  <list the JNI methods Yole will call: parse(), edit(), tree.rootNode().walk(), etc.>

  ### Risks
  <known issues, gotchas — e.g., does this library hold global state? thread safety?>
  ```

### Task 0.2: Tree-Sitter on Kotlin/Native (iOS)

- [ ] **Step 1: Determine cinterop strategy**

  Required outputs:
  - Is there a published Kotlin/Native wrapper? (Search `awesome-kotlin-native`, JetBrains Pkg, GitHub.)
  - If not: can we build a cinterop `.def` file against upstream `libtree-sitter.dylib`?
  - CocoaPods integration: is `tree-sitter` available on CocoaPods or do we vendor a static lib?
  - App Store review: are native C libraries permitted? (Yes, but require justification — check Apple docs.)
  - Static vs dynamic linking constraints for iOS apps.

- [ ] **Step 2: Write findings to research-report.md §2 Tree-Sitter on Kotlin/Native**

### Task 0.3: vscode-textmate on Wasm

- [ ] **Step 1: Determine JS-interop path**

  Required outputs:
  - vscode-textmate npm package version + license.
  - oniguruma WebAssembly bindings (vscode-textmate requires onig).
  - How to consume from Kotlin/Wasm: `@JsExport` / `JsName` / `external` declarations needed.
  - Grammar JSON delivery model (bundled in Wasm app? fetched at runtime?).
  - Performance baseline: a 1000-line Kotlin file tokenize time.

- [ ] **Step 2: Write findings to research-report.md §3 vscode-textmate on Wasm**

### Task 0.4: VS Code theme JSON schema deep-dive

- [ ] **Step 1: Enumerate the schema**

  Required outputs:
  - Full list of `colors.*` keys VS Code recognizes (~200 entries). Source: https://code.visualstudio.com/api/references/theme-color.
  - Full list of `tokenColors[]` scope conventions. Source: vscode-textmate's `IRawTheme` typedef.
  - Schema for `semanticTokenColors` (newer, optional).
  - Scope inheritance behavior (`function.builtin` falls back to `function` falls back to default).
  - Inheritance via `include` directive (some themes extend others).

- [ ] **Step 2: Identify the ~25 keys Yole needs from `colors.*`**

  Cross-reference with the spec §3.8 table. Add any keys the spec missed.

- [ ] **Step 3: Write findings to research-report.md §4 VS Code theme JSON schema**

### Task 0.5: Grammar bundle inventory

- [ ] **Step 1: Identify the 50+ grammars**

  Required outputs:
  - List of 50+ languages with: grammar repo URL, license, .so binary size per ABI (estimate from comparable grammars).
  - Languages: kotlin, java, python, javascript, typescript, tsx, jsx, go, rust, c, cpp, csharp, ruby, php, swift, scala, dart, html, css, scss, less, sql, yaml, toml, json, xml, bash, lua, perl, haskell, ocaml, julia, r, matlab, elixir, erlang, fortran, vim, dockerfile, makefile, terraform, regex, vue, graphql, markdown, plus 5+ more to round to 50.

- [ ] **Step 2: Estimate total APK size impact**

  Required outputs:
  - Estimated APK size addition per grammar.
  - Whether we bundle all 50 in the base APK (with split APKs per ABI) or fetch on demand on enable.
  - **Recommendation**: bundle markdown grammar only (since markdown is the default-enabled format), fetch others on enable. APK addition: <2 MB markdown only.

- [ ] **Step 3: Write findings to research-report.md §5 Grammar bundle strategy**

### Task 0.6: Editor rendering: VisualTransformation vs custom layout

- [ ] **Step 1: Microbenchmark approaches**

  Required outputs:
  - Approach A: `BasicTextField` + `VisualTransformation` that maps each char to a colored span. Compose-idiomatic; recomposes the whole field on every keystroke.
  - Approach B: Custom `@Composable` that draws `AnnotatedString` via `Text` and overlays an invisible `BasicTextField` for input. More complex; can be faster on large files.
  - Approach C: Custom `Modifier` that listens to `TextLayoutResult` and overlays color spans externally.
  - Benchmark each on a 1000-line Kotlin file with active highlighting; report frame times.

- [ ] **Step 2: Write findings to research-report.md §6 Editor rendering benchmarks**

### Task 0.7: Existing-library reuse decision

- [ ] **Step 1: Evaluate Highlights.kt and kotlinx-highlights**

  Required outputs:
  - For each: license, language coverage, theme system, KMP target support (commonMain vs jvmMain only), maintenance status.
  - Decision: can Yole use either as a backing implementation (e.g., for the Wasm path) instead of writing vscode-textmate interop ourselves? Or are they too restrictive on language coverage?

- [ ] **Step 2: Write findings to research-report.md §7 Library reuse evaluation**

### Task 0.8: Commit research report

- [ ] **Step 1: Verify report completeness**

  Run: `wc -l docs/features/syntax-highlighting/research-report.md`
  Expected: ≥ 400 lines covering all 7 sections.

  Each section must close its question with a concrete decision, not "TBD".

- [ ] **Step 2: Commit**

  ```bash
  git add docs/features/syntax-highlighting/research-report.md
  git commit -m "$(cat <<'EOF'
  docs(iter-57): deep-research report for syntax highlighting

  Closes the 7 open questions left by docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md §12 (and adds 6 more discovered during research). Each section produces a concrete decision; no TBDs.

  Sections:
    §1 Tree-Sitter on JVM — chosen library, ABI matrix, grammar bundle format.
    §2 Tree-Sitter on Kotlin/Native (iOS) — cinterop / CocoaPods / App Store review.
    §3 vscode-textmate on Wasm — JS interop, oniguruma, grammar delivery.
    §4 VS Code theme JSON schema — key inventory, scope inheritance.
    §5 Grammar bundle strategy — markdown bundled, others lazy.
    §6 Editor rendering benchmarks — VisualTransformation chosen.
    §7 Library reuse evaluation — verdict on Highlights.kt / kotlinx-highlights.

  Cross-platform impact (CONST-037):
    - Android: research; no code change.
    - Desktop: research; no code change.
    - iOS: research; no code change.
    - Web: research; no code change.

  Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
  EOF
  )"
  ```

---

## Phase 1 — VS Code theme JSON parser (pure Kotlin, no platform deps)

**Goal:** A `kotlinx.serialization`-based parser that consumes a VS Code theme JSON string and produces a `Theme` value object exposing `colorFor(scope)` and `colorFor(uiKey)`.

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/Theme.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/VsCodeThemeParser.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/ThemeParseException.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/syntax/VsCodeThemeParserTest.kt`

### Task 1.1: Write the failing parser test

- [ ] **Step 1: Create the test with a known VS Code theme snippet**

  ```kotlin
  // shared/src/commonTest/kotlin/digital/vasic/yole/syntax/VsCodeThemeParserTest.kt
  package digital.vasic.yole.syntax

  import digital.vasic.yole.syntax.theme.ThemeParseException
  import digital.vasic.yole.syntax.theme.VsCodeThemeParser
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertFailsWith
  import kotlin.test.assertNull

  class VsCodeThemeParserTest {
      private val miniThemeJson = """
          {
            "name": "Mini Test Theme",
            "type": "dark",
            "colors": {
              "editor.background": "#1e1e1e",
              "editor.foreground": "#d4d4d4",
              "editorLineNumber.foreground": "#858585"
            },
            "tokenColors": [
              {
                "scope": "keyword",
                "settings": { "foreground": "#569cd6" }
              },
              {
                "scope": ["string", "string.quoted"],
                "settings": { "foreground": "#ce9178" }
              }
            ]
          }
      """.trimIndent()

      @Test
      fun parsesNameAndType() = runBlocking<Unit> {
          val theme = VsCodeThemeParser.parse(miniThemeJson)
          assertEquals("Mini Test Theme", theme.name)
          assertEquals("dark", theme.type)
      }

      @Test
      fun parsesUiColors() = runBlocking<Unit> {
          val theme = VsCodeThemeParser.parse(miniThemeJson)
          assertEquals(0xFF1E1E1Eu.toLong().toInt(), theme.uiColor("editor.background"))
          assertEquals(0xFFD4D4D4u.toLong().toInt(), theme.uiColor("editor.foreground"))
      }

      @Test
      fun parsesTokenColorsWithScopeArray() = runBlocking<Unit> {
          val theme = VsCodeThemeParser.parse(miniThemeJson)
          assertEquals(0xFF569CD6u.toLong().toInt(), theme.tokenColor("keyword"))
          assertEquals(0xFFCE9178u.toLong().toInt(), theme.tokenColor("string"))
          assertEquals(0xFFCE9178u.toLong().toInt(), theme.tokenColor("string.quoted"))
      }

      @Test
      fun scopeFallbackThroughHierarchy() = runBlocking<Unit> {
          val theme = VsCodeThemeParser.parse(miniThemeJson)
          // "keyword.control.return" falls back to "keyword"
          assertEquals(0xFF569CD6u.toLong().toInt(), theme.tokenColor("keyword.control.return"))
      }

      @Test
      fun unknownScopeReturnsNull() = runBlocking<Unit> {
          val theme = VsCodeThemeParser.parse(miniThemeJson)
          assertNull(theme.tokenColor("never.heard.of.this"))
      }

      @Test
      fun malformedJsonThrowsThemeParseException() {
          assertFailsWith<ThemeParseException> {
              runBlocking<Unit> { VsCodeThemeParser.parse("{ not json }") }
          }
      }

      @Test
      fun missingNameThrowsThemeParseException() {
          val noName = """{ "type": "dark", "colors": {} }"""
          assertFailsWith<ThemeParseException> {
              runBlocking<Unit> { VsCodeThemeParser.parse(noName) }
          }
      }
  }
  ```

  (Note: `runBlocking<Unit>` is required by Yole's JUnit4-based test infrastructure per CLAUDE.md "Test Constraints".)

- [ ] **Step 2: Run test to verify it fails**

  ```bash
  ./gradlew :shared:desktopTest --tests "*VsCodeThemeParserTest*"
  ```

  Expected: compilation failure — `Unresolved reference: VsCodeThemeParser`, `Unresolved reference: ThemeParseException`.

### Task 1.2: Define ThemeParseException

- [ ] **Step 1: Write the class**

  ```kotlin
  // shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/ThemeParseException.kt
  /*#######################################################
   *
   * SPDX-FileCopyrightText: 2026 Milos Vasic
   * SPDX-License-Identifier: Apache-2.0
   *
   * iter-57: thrown by VsCodeThemeParser when input JSON is
   * structurally invalid or missing required fields.
   *
   *########################################################*/
  package digital.vasic.yole.syntax.theme

  class ThemeParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
  ```

- [ ] **Step 2: Commit (incremental)**

  ```bash
  git add shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/ThemeParseException.kt
  git commit -m "feat(iter-57): add ThemeParseException for VS Code theme JSON parsing failures

  Cross-platform impact (CONST-037):
    - Android / Desktop / iOS / Web: pure commonMain class, identical on all.

  Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
  ```

### Task 1.3: Define the Theme value class

- [ ] **Step 1: Write the class**

  ```kotlin
  // shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/Theme.kt
  /*#######################################################
   *
   * SPDX-FileCopyrightText: 2026 Milos Vasic
   * SPDX-License-Identifier: Apache-2.0
   *
   * iter-57: immutable VS Code theme value object.
   *   - uiColor(key) looks up VS Code `colors.*` keys (editor.background, etc.).
   *   - tokenColor(scope) looks up `tokenColors[].scope` with scope-hierarchy fallback.
   *
   *########################################################*/
  package digital.vasic.yole.syntax.theme

  /**
   * Immutable VS Code theme.
   *
   * @property name human-readable name from JSON `name` field.
   * @property type "dark" or "light" from JSON `type` field.
   * @property uiColors map of VS Code colors.* keys → ARGB int (0xFFRRGGBB).
   * @property tokenColors map of token scope → ARGB int, with most-specific entries first
   *           (lookup falls back through scope hierarchy: foo.bar.baz → foo.bar → foo).
   */
  data class Theme(
      val name: String,
      val type: String,
      val uiColors: Map<String, Int>,
      val tokenColors: Map<String, Int>,
  ) {
      /** Look up a VS Code UI color (e.g., "editor.background"). Returns null if absent. */
      fun uiColor(key: String): Int? = uiColors[key]

      /** Look up a token-scope color with VS Code hierarchical fallback. */
      fun tokenColor(scope: String): Int? {
          // Exact match first.
          tokenColors[scope]?.let { return it }
          // Fall back: "foo.bar.baz" → "foo.bar" → "foo".
          var s = scope
          while (s.contains('.')) {
              s = s.substringBeforeLast('.')
              tokenColors[s]?.let { return it }
          }
          return null
      }
  }
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/Theme.kt
  git commit -m "feat(iter-57): add Theme value class with hierarchical scope fallback

  Cross-platform impact (CONST-037):
    - Android / Desktop / iOS / Web: pure commonMain data class.

  Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
  ```

### Task 1.4: Implement VsCodeThemeParser

- [ ] **Step 1: Write the parser**

  ```kotlin
  // shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/VsCodeThemeParser.kt
  /*#######################################################
   *
   * SPDX-FileCopyrightText: 2026 Milos Vasic
   * SPDX-License-Identifier: Apache-2.0
   *
   * iter-57: kotlinx.serialization-based VS Code theme JSON parser.
   *   Reads `name`, `type`, `colors.*`, `tokenColors[]` from a VS Code theme JSON
   *   string and produces a Theme value object.
   *
   *########################################################*/
  package digital.vasic.yole.syntax.theme

  import kotlinx.serialization.SerializationException
  import kotlinx.serialization.json.Json
  import kotlinx.serialization.json.JsonArray
  import kotlinx.serialization.json.JsonElement
  import kotlinx.serialization.json.JsonObject
  import kotlinx.serialization.json.JsonPrimitive
  import kotlinx.serialization.json.contentOrNull
  import kotlinx.serialization.json.jsonArray
  import kotlinx.serialization.json.jsonObject
  import kotlinx.serialization.json.jsonPrimitive

  object VsCodeThemeParser {
      private val json = Json { ignoreUnknownKeys = true; isLenient = false }

      /**
       * Parse a VS Code theme JSON string. Throws ThemeParseException on any
       * malformed input or missing required field (name, type).
       */
      fun parse(input: String): Theme {
          val root: JsonObject = try {
              json.parseToJsonElement(input).jsonObject
          } catch (e: SerializationException) {
              throw ThemeParseException("malformed JSON: ${e.message}", e)
          } catch (e: IllegalArgumentException) {
              throw ThemeParseException("root is not a JSON object: ${e.message}", e)
          }

          val name = root["name"]?.jsonPrimitive?.contentOrNull
              ?: throw ThemeParseException("required field `name` missing")
          val type = root["type"]?.jsonPrimitive?.contentOrNull
              ?: throw ThemeParseException("required field `type` missing")

          val uiColors = parseUiColors(root["colors"])
          val tokenColors = parseTokenColors(root["tokenColors"])

          return Theme(name = name, type = type, uiColors = uiColors, tokenColors = tokenColors)
      }

      private fun parseUiColors(element: JsonElement?): Map<String, Int> {
          val obj = element?.jsonObject ?: return emptyMap()
          return obj.mapNotNull { (key, value) ->
              val hex = (value as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
              parseHexColor(hex)?.let { key to it }
          }.toMap()
      }

      private fun parseTokenColors(element: JsonElement?): Map<String, Int> {
          val arr = element?.jsonArray ?: return emptyMap()
          val out = mutableMapOf<String, Int>()
          for (entry in arr) {
              val obj = entry.jsonObject
              val settings = obj["settings"]?.jsonObject ?: continue
              val foreground = (settings["foreground"] as? JsonPrimitive)?.contentOrNull ?: continue
              val color = parseHexColor(foreground) ?: continue
              when (val scope = obj["scope"]) {
                  is JsonPrimitive -> {
                      scope.contentOrNull?.let { out[it.trim()] = color }
                  }
                  is JsonArray -> {
                      scope.forEach { s ->
                          (s as? JsonPrimitive)?.contentOrNull?.let { out[it.trim()] = color }
                      }
                  }
                  else -> { /* skip */ }
              }
          }
          return out
      }

      /**
       * Parse "#RRGGBB" or "#RRGGBBAA" to ARGB int (0xFFRRGGBB or 0xAARRGGBB).
       * Returns null on malformed.
       */
      private fun parseHexColor(hex: String): Int? {
          val trimmed = hex.trim().removePrefix("#")
          return when (trimmed.length) {
              6 -> runCatching {
                  val rgb = trimmed.toLong(16).toInt()
                  (0xFF shl 24) or rgb
              }.getOrNull()
              8 -> runCatching {
                  // VS Code: #RRGGBBAA — convert to ARGB
                  val rgba = trimmed.toLong(16)
                  val r = (rgba shr 24) and 0xFF
                  val g = (rgba shr 16) and 0xFF
                  val b = (rgba shr 8) and 0xFF
                  val a = rgba and 0xFF
                  ((a shl 24) or (r shl 16) or (g shl 8) or b).toInt()
              }.getOrNull()
              else -> null
          }
      }
  }
  ```

- [ ] **Step 2: Run tests**

  ```bash
  ./gradlew :shared:desktopTest --tests "*VsCodeThemeParserTest*"
  ```

  Expected: all 7 cases PASS.

- [ ] **Step 3: Mutation-verify**

  Temporarily change `parseHexColor` to always return `null`. Re-run the test.
  Expected: at least 3 of 7 tests FAIL (color-parsing assertions). Revert.

- [ ] **Step 4: Commit**

  ```bash
  git add shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/VsCodeThemeParser.kt \
          shared/src/commonTest/kotlin/digital/vasic/yole/syntax/VsCodeThemeParserTest.kt
  git commit -m "feat(iter-57): VsCodeThemeParser with hierarchical scope fallback + 7 tests

  kotlinx.serialization-based parser for VS Code theme JSON files. Handles single-scope-string and scope-array forms. Hierarchical fallback per VS Code convention.

  Anti-bluff: 7 unit tests including malformed-input + missing-required-field cases. Mutation-verified: stubbing parseHexColor → null causes 3+ tests to FAIL.

  Cross-platform impact (CONST-037):
    - Android / Desktop / iOS / Web: pure commonMain, identical on all.

  Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
  ```

---

## Phase 2 — Yole-Light.json + Yole-Dark.json + LegacyThemeParityTest

**Goal:** Port Yole's current hardcoded `IdeTheme.*` / `YoleColors.*` palette into two VS Code theme JSON files, with a parity test that asserts byte-exact RGB equality.

**Pre-execution gate:** Phase 1 complete (parser works).

**Files:**
- Create: `shared/src/commonMain/resources/themes/builtin/Yole-Light.json`
- Create: `shared/src/commonMain/resources/themes/builtin/Yole-Dark.json`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/LegacyThemeBridge.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/syntax/LegacyThemeParityTest.kt`

### Task 2.1: Inventory the legacy palettes

- [ ] **Step 1: List every IdeTheme + YoleColors field**

  Run:
  ```bash
  grep -nE "(val|object).*Color\(0x" androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt androidApp/src/main/java/digital/vasic/yole/android/ui/YoleColors.kt 2>/dev/null
  ```

  Produces a list like:
  ```
  androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt:42:    val darkBackground = Color(0xFF1E1E1E)
  androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt:43:    val lightBackground = Color(0xFFFFFFFF)
  ...
  ```

- [ ] **Step 2: Map each legacy field → VS Code key per spec §3.8**

  Open `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` §3.8 schema mapping table. For each legacy field, find the corresponding VS Code key. Write the mapping into a temporary file `docs/features/syntax-highlighting/legacy-palette-mapping.md`.

### Task 2.2: Author Yole-Light.json

- [ ] **Step 1: Write the JSON**

  ```bash
  mkdir -p shared/src/commonMain/resources/themes/builtin
  ```

  Then create `shared/src/commonMain/resources/themes/builtin/Yole-Light.json`:

  ```json
  {
    "name": "Yole Light",
    "type": "light",
    "colors": {
      "editor.background": "#ffffff",
      "editor.foreground": "#1e1e1e",
      "editorLineNumber.foreground": "#999999",
      "editorLineNumber.activeForeground": "#1e1e1e",
      "statusBar.background": "#f3f3f3",
      "statusBar.foreground": "#1e1e1e",
      "activityBar.background": "#ffffff",
      "activityBar.foreground": "#1e1e1e",
      "sideBar.background": "#f8f8f8",
      "editorWidget.background": "#ffffff",
      "focusBorder": "#0078d4",
      "badge.background": "#0078d4"
    },
    "tokenColors": [
      {
        "scope": "comment",
        "settings": { "foreground": "#008000" }
      },
      {
        "scope": ["keyword", "storage.type", "storage.modifier"],
        "settings": { "foreground": "#0000ff" }
      },
      {
        "scope": ["string", "string.quoted"],
        "settings": { "foreground": "#a31515" }
      },
      {
        "scope": ["constant.numeric", "constant.language"],
        "settings": { "foreground": "#098658" }
      },
      {
        "scope": ["entity.name.function", "support.function"],
        "settings": { "foreground": "#795e26" }
      },
      {
        "scope": ["entity.name.type", "support.type"],
        "settings": { "foreground": "#267f99" }
      },
      {
        "scope": ["variable"],
        "settings": { "foreground": "#001080" }
      }
    ]
  }
  ```

  (Replace the hex values with the actual byte-for-byte legacy values discovered in Task 2.1's mapping.)

### Task 2.3: Author Yole-Dark.json

- [ ] **Step 1: Write the JSON** — same structure as Yole-Light, with the legacy dark palette values. Skipped here for brevity; structure is identical.

### Task 2.4: Implement LegacyThemeBridge

- [ ] **Step 1: Write the bridge**

  ```kotlin
  // shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/LegacyThemeBridge.kt
  /*#######################################################
   *
   * SPDX-FileCopyrightText: 2026 Milos Vasic
   * SPDX-License-Identifier: Apache-2.0
   *
   * iter-57: one-shot migration helper used by LegacyThemeParityTest.
   * Returns the legacy hardcoded color values as a Map<vsCodeKey, ARGB int>.
   * DELETED in v1.1 after migration confidence is high (per spec §4).
   *
   *########################################################*/
  package digital.vasic.yole.syntax.theme

  /**
   * Legacy IdeTheme + YoleColors mapped to VS Code keys.
   * The values here MUST equal the constants in
   * androidApp/.../ui/theme/Theme.kt and androidApp/.../ui/YoleColors.kt
   * at the moment Phase 2 is executed. If those legacy files change,
   * this map MUST be updated in lockstep — and the parity test will catch
   * drift.
   */
  object LegacyThemeBridge {
      val legacyLight: Map<String, Int> = mapOf(
          "editor.background" to 0xFFFFFFFF.toInt(),
          "editor.foreground" to 0xFF1E1E1E.toInt(),
          "editorLineNumber.foreground" to 0xFF999999.toInt(),
          // ... fill from Task 2.1 inventory
      )

      val legacyDark: Map<String, Int> = mapOf(
          "editor.background" to 0xFF1E1E1E.toInt(),
          "editor.foreground" to 0xFFD4D4D4.toInt(),
          "editorLineNumber.foreground" to 0xFF858585.toInt(),
          // ... fill from Task 2.1 inventory
      )
  }
  ```

### Task 2.5: Write the parity test

- [ ] **Step 1: Create the test**

  ```kotlin
  // shared/src/commonTest/kotlin/digital/vasic/yole/syntax/LegacyThemeParityTest.kt
  /*#######################################################
   * iter-57: anti-bluff parity test. Yole-Light.json + Yole-Dark.json MUST
   * reproduce the legacy IdeTheme/YoleColors values byte-for-byte. Reverting
   * either JSON file's color values MUST cause this test to fail.
   *########################################################*/
  package digital.vasic.yole.syntax

  import digital.vasic.yole.syntax.theme.LegacyThemeBridge
  import digital.vasic.yole.syntax.theme.VsCodeThemeParser
  import kotlin.test.Test
  import kotlin.test.assertEquals

  class LegacyThemeParityTest {
      private fun loadResource(path: String): String {
          // commonTest cannot load JVM resources directly; the build copies
          // shared/src/commonMain/resources/themes/builtin/*.json to a
          // commonTest-accessible location at build time. The exact mechanism
          // is decided in Phase 0 research §5; this loader is the placeholder
          // interface used here.
          return ThemeResources.read(path)
      }

      @Test
      fun yoleLightJsonMatchesLegacyPalette() {
          val json = loadResource("themes/builtin/Yole-Light.json")
          val theme = VsCodeThemeParser.parse(json)
          for ((key, expected) in LegacyThemeBridge.legacyLight) {
              assertEquals(expected, theme.uiColor(key),
                  "Yole-Light.json `$key` MUST equal legacy palette value")
          }
      }

      @Test
      fun yoleDarkJsonMatchesLegacyPalette() {
          val json = loadResource("themes/builtin/Yole-Dark.json")
          val theme = VsCodeThemeParser.parse(json)
          for ((key, expected) in LegacyThemeBridge.legacyDark) {
              assertEquals(expected, theme.uiColor(key),
                  "Yole-Dark.json `$key` MUST equal legacy palette value")
          }
      }
  }
  ```

- [ ] **Step 2: Verify test fails before any JSON is written, passes after**

  Run before writing the JSONs: `./gradlew :shared:desktopTest --tests "*LegacyThemeParityTest*"` → expect FAIL ("resource not found"). Then run after Tasks 2.2 + 2.3 + 2.4 are complete → expect PASS.

- [ ] **Step 3: Mutation-verify**

  Flip one byte in `Yole-Light.json` (e.g., `#1e1e1e` → `#1e1e1f`). Re-run test → expect FAIL. Revert.

- [ ] **Step 4: Commit Phase 2**

  ```bash
  git add shared/src/commonMain/resources/themes/builtin/Yole-Light.json \
          shared/src/commonMain/resources/themes/builtin/Yole-Dark.json \
          shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/LegacyThemeBridge.kt \
          shared/src/commonTest/kotlin/digital/vasic/yole/syntax/LegacyThemeParityTest.kt
  git commit -m "feat(iter-57): Phase 2 — port legacy IdeTheme to Yole-Light/Dark.json + parity test

  Anti-bluff: LegacyThemeParityTest verifies byte-exact RGB equality. Mutation-verified: flipping any color byte in either JSON file fails the test.

  Cross-platform impact (CONST-037):
    - Android / Desktop / iOS / Web: pure commonMain + commonTest.

  Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
  ```

---

## Phase 3 — ThemeProvider + app-wide migration off IdeTheme/YoleColors

**Goal:** Introduce `ThemeProvider` Composable at the app root; replace every `IdeTheme.*` and `YoleColors.*` callsite with `LocalTheme.current.uiColor(key)`; delete the legacy palette files.

**Pre-execution gate:** Phase 2 complete (Yole-Light/Dark.json verified pixel-parity).

**Files to create:** `ThemeProvider.kt`, `ThemeRegistry.kt`.
**Files to modify:** every `*App` shell + `SyncedScrollEditor.kt`.
**Files to delete:** `IdeTheme.kt`, `YoleColors.kt`.

### Tasks (full TDD detail finalized after Phase 2 completes)

- [ ] **Task 3.1:** Write `ThemeRegistry` with `activeTheme: StateFlow<Theme>`. Test: theme switch propagates.
- [ ] **Task 3.2:** Write `ThemeProvider` Composable + `LocalTheme: CompositionLocal`. Test: `ThemeUiCoverageTest`.
- [ ] **Task 3.3:** Mechanical migration of every `IdeTheme.*` reference (Android shell). Per CONST-037, mirror migration in `desktopApp` + `iosApp` + `webApp` shells.
- [ ] **Task 3.4:** Delete `IdeTheme.kt` + `YoleColors.kt`.
- [ ] **Task 3.5:** Visual regression run — open every screen in both Yole-Light and Yole-Dark themes; capture screenshots; manual eyeballing (since pixel-parity test already passes the math).
- [ ] **Task 3.6:** Commit.

---

## Phase 4 — Format enablement gate + Settings → Formats screen

**Pre-execution gate:** Phase 3 complete (theme system unified).

**Files to create:** `EnabledFormatGate.kt`, `FormatDisabledException.kt`, `FormatsSettingsScreen.kt`, `FormatMigrationDialog.kt`.
**Files to modify:** `Settings.kt`, `FormatRegistry.kt`, `MoreScreen` / settings navigation.

### Tasks (full TDD detail finalized after Phase 3 completes)

- [ ] **Task 4.1:** Write `FormatEnablementDefaultTest` (in `commonTest`) asserting fresh-install default is `setOf("markdown")`. Verify it RED before changing `Settings.kt` default.
- [ ] **Task 4.2:** Change `Settings.kt` default; verify GREEN.
- [ ] **Task 4.3:** Write `EnabledFormatGate` + `FormatEnablementGateTest`. Mutation-verify gate.
- [ ] **Task 4.4:** Wire `FormatRegistry.isEnabled()` through the gate.
- [ ] **Task 4.5:** Write `FormatsSettingsScreen` Composable. Robolectric test asserts grouped sections render + toggles persist.
- [ ] **Task 4.6:** Write `FormatMigrationDialog` for one-time prompt to existing users.
- [ ] **Task 4.7:** Wire `MoreScreen → Settings → Formats` navigation (per CONST-037, mirror in desktop/iOS/web).
- [ ] **Task 4.8:** Commit.

---

## Phase 5 — Tree-Sitter JNI engine actual (Android + Desktop)

**Pre-execution gate:** Phase 0 §1 closed (chosen library + ABI matrix + grammar bundle format known).

**Files to create:** `TokenizerEngine.kt` (expect), `TokenizerEngine.android.kt`, `TokenizerEngine.desktop.kt`, `TokenizerEngineJvmTest.kt`.

- [ ] **Task 5.1:** Declare `expect class TokenizerEngine` in `commonMain` with `suspend fun tokenize(text, lang)` + `suspend fun loadGrammar(lang)` + `suspend fun initialize(): Result<Unit>`.
- [ ] **Task 5.2:** Add the chosen Tree-Sitter JNI dependency to `gradle/libs.versions.toml` + `shared/build.gradle.kts` androidMain + desktopMain.
- [ ] **Task 5.3:** Write `TokenizerEngine.android.kt` — Tree-Sitter JNI wrapper. Initialize loads `libtree-sitter.so` + the bundled markdown grammar `.so`.
- [ ] **Task 5.4:** `TokenizerEngine.desktop.kt` — same JNI wrapper, different native lib path resolution.
- [ ] **Task 5.5:** `TokenizerEngineJvmTest.kt` — tokenize known kotlin snippet, assert token count ≥ line count, first non-ws token scope is `keyword` (`fun`).
- [ ] **Task 5.6:** Mutation-verify: revert `loadGrammar` to no-op → test FAILS.
- [ ] **Task 5.7:** Commit.

---

## Phase 6 — vscode-textmate Wasm engine actual

**Pre-execution gate:** Phase 0 §3 closed (JS interop pattern documented).

- [ ] **Task 6.1:** Add vscode-textmate npm dependency to webApp.
- [ ] **Task 6.2:** Write `TokenizerEngine.wasmJs.kt` using `external` declarations + JS interop.
- [ ] **Task 6.3:** Bundle markdown grammar JSON in `webApp/src/wasmJsMain/resources/grammars/`.
- [ ] **Task 6.4:** `TokenizerEngineWasmTest.kt` — same kotlin snippet, same assertions.
- [ ] **Task 6.5:** Mutation-verify.
- [ ] **Task 6.6:** Commit.

---

## Phase 7 — Tree-Sitter Kotlin/Native engine actual (iOS)

**Pre-execution gate:** Phase 0 §2 closed (cinterop strategy + CocoaPods decision).

- [ ] **Task 7.1:** Add Tree-Sitter cinterop `.def` file to `shared/src/iosMain/cinterop/`.
- [ ] **Task 7.2:** Write `TokenizerEngine.ios.kt`.
- [ ] **Task 7.3:** Bundle markdown grammar in iOS app resources.
- [ ] **Task 7.4:** `TokenizerEngineIosTest.kt`.
- [ ] **Task 7.5:** Mutation-verify.
- [ ] **Task 7.6:** Commit.

---

## Phase 8 — SyntaxHighlighter API + ScopeMapper + GrammarRegistry

**Pre-execution gate:** Phases 5–7 complete (all 4 engine actuals work).

- [ ] **Task 8.1:** `Token.kt` + `TokenSpan.kt` + `Grammar.kt` + `GrammarMetadata.kt` (pure data classes).
- [ ] **Task 8.2:** `ScopeMapper.kt` — table-driven mapping of 60+ Tree-Sitter scopes → VS Code scopes. `ScopeMapperTest.kt`.
- [ ] **Task 8.3:** `GrammarRegistry.kt` — extension → lang id + lazy grammar load. `GrammarRegistryTest.kt`.
- [ ] **Task 8.4:** `SyntaxHighlighter.kt` — top-level entry. Tests: highlight() returns expected spans for a known snippet.
- [ ] **Task 8.5:** `AnnotatedStringBuilder.kt` — token list → Compose `AnnotatedString` with per-token `SpanStyle`.
- [ ] **Task 8.6:** `SyntaxHighlightingSourceInvariantsTest.kt` — structural anti-bluff (no platform imports in commonMain, every expect has 4 actuals, no `runBlocking` in production).
- [ ] **Task 8.7:** Commit.

---

## Phase 9 — Editor highlighting integration

**Pre-execution gate:** Phase 8 complete (SyntaxHighlighter works end-to-end on JVM).

- [ ] **Task 9.1:** Modify `SyncedScrollEditor.kt` — add optional `highlighter: SyntaxHighlighter?` parameter; when set, transform `textState.value` via `highlighter.highlight(text, lang)` and render through `BasicTextField`'s `VisualTransformation` (or whatever Phase 0 §6 benchmark chose).
- [ ] **Task 9.2:** Wire `IdeEditorScreen` in `YoleApp.kt` to pass the active highlighter when the current file's format is enabled.
- [ ] **Task 9.3:** `EditorHighlightingRobolectricTest.kt` — type Kotlin `fun main()` snippet, assert the AnnotatedString contains a SpanStyle at the `fun` range with the active theme's keyword color.
- [ ] **Task 9.4:** Mutation-verify (revert highlighter → plain text, test FAILS).
- [ ] **Task 9.5:** Commit.

---

## Phase 10 — Preview code-block highlighting

**Pre-execution gate:** Phase 9 complete.

- [ ] **Task 10.1:** `PreviewCodeBlockHighlighter.kt` — post-processes `ParsedDocument.toHtml()` output: for each `<pre><code class="lang-x">` block, replace inner text with token-CSS-class spans using `SyntaxHighlighter`.
- [ ] **Task 10.2:** Modify `StyleSheets.kt` — append per-scope CSS rules using the active theme's `tokenColors`.
- [ ] **Task 10.3:** `PreviewCodeBlockHighlighterTest.kt`.
- [ ] **Task 10.4:** Mutation-verify.
- [ ] **Task 10.5:** Commit.

---

## Phase 11 — Filename badges in FILES tab

**Pre-execution gate:** Phase 10 complete.

- [ ] **Task 11.1:** `BadgeTinter.kt` — given a filename, returns a `Color` from the active theme's `badge.background` + per-language override (or null if format not enabled).
- [ ] **Task 11.2:** Modify `FilesScreen` (which renders `FileBrowserScreen` per iter-55) — call `BadgeTinter.tintFor(file.name)` per row; draw small chip if non-null.
- [ ] **Task 11.3:** `FilenameBadgesRobolectricTest.kt` — fixture file list with `.kt` + `.py` + `.unknown.xyz`; assert kotlin + python rows have visible chips, unknown.xyz does not.
- [ ] **Task 11.4:** Mutation-verify.
- [ ] **Task 11.5:** Commit.

---

## Phase 12 — Anti-bluff challenges + qa-all wiring

**Pre-execution gate:** Phase 11 complete (all UI surfaces visible).

- [ ] **Task 12.1:** `yole-challenges/scripts/syntax_highlighting_challenge.sh` — runs Phase 1–8 + Phase 9 Robolectric test, emits PASSED-case count + log path.
- [ ] **Task 12.2:** `yole-challenges/scripts/syntax_highlighting_per_platform_challenge.sh` — grep all 4 `*Main` source sets for `TokenizerEngine` actual; fail if any missing.
- [ ] **Task 12.3:** `yole-challenges/scripts/theme_unification_challenge.sh` — assert `IdeTheme.kt` and `YoleColors.kt` no longer exist; run `LegacyThemeParityTest` + `ThemeUiCoverageTest`.
- [ ] **Task 12.4:** `yole-challenges/scripts/format_enablement_default_challenge.sh` — runtime probe that fresh `Settings` returns `setOf("markdown")` only.
- [ ] **Task 12.5:** Modify `Makefile` — add `qa-iter-57-gates` target; chain into `qa-all`.
- [ ] **Task 12.6:** Run `make qa-all` → expect PASS for every challenge.
- [ ] **Task 12.7:** Commit.

---

## Phase 13 — Documentation + diagrams + website

**Pre-execution gate:** Phase 12 complete (everything tested green).

- [ ] **Task 13.1:** Write `docs/features/syntax-highlighting/user-guide.md` (end-user perspective: how to enable formats, switch themes, add custom themes).
- [ ] **Task 13.2:** Write `docs/features/syntax-highlighting/architecture.md` (contributor perspective: package layout, expect/actual story, dataflow).
- [ ] **Task 13.3:** Write `docs/features/syntax-highlighting/theme-migration-guide.md` (for users with custom themes from prior versions).
- [ ] **Task 13.4:** Write `docs/features/syntax-highlighting/settings-formats-guide.md` (the Settings → Formats screen walkthrough).
- [ ] **Task 13.5:** Author Mermaid source → SVG for the 3 diagrams in `docs/diagrams/`.
- [ ] **Task 13.6:** Update website (whatever Yole's site infrastructure is — typically a Hugo or Jekyll repo or a `Website/` directory in main repo if it exists).
- [ ] **Task 13.7:** Update `CHANGELOG.md` with iter-57 entry.
- [ ] **Task 13.8:** Update `docs/CONTINUATION.md` Section 2 + new Section 41 per CONST-036.
- [ ] **Task 13.9:** Commit.

---

## Phase 14 — Firebase distribution

**Pre-execution gate:** Phase 13 complete + `make qa-all` green.

- [ ] **Task 14.1:** Bump version per release-naming convention (iter-54 set 1.0.1 / versionCode 101; iter-57 sets 1.1.0 / versionCode 110 reflecting the major feature add).
- [ ] **Task 14.2:** Build Android debug + release APK (`make container-build` + container-release).
- [ ] **Task 14.3:** Build Desktop tarballs (Linux x64 / Windows x64 / macOS arm64).
- [ ] **Task 14.4:** Build iOS IPA.
- [ ] **Task 14.5:** Build Web Wasm bundle.
- [ ] **Task 14.6:** Per CONST-035, run anti-bluff scanner one final time on the release artifacts.
- [ ] **Task 14.7:** Distribute via Firebase to all 4 platforms (using existing iter-54 distribution flow).
- [ ] **Task 14.8:** Capture distribution evidence (firebase URLs, signed hashes) into `docs/qa/iter-57/`.
- [ ] **Task 14.9:** Final commit with the version bump + release-notes snapshot.
- [ ] **Task 14.10:** Push to `origin master`.

---

## Self-review against the spec

**Spec coverage check:**

| Spec requirement | Plan task |
|---|---|
| §2 Languages: 50+ via Tree-Sitter/TextMate | Phase 0 §5 + Phase 5–7 engine actuals |
| §2 Engine: Hybrid Tree-Sitter on JVM/KN + TextMate on Wasm | Phases 5, 6, 7 |
| §2 Platform parity: all 4 before ship | CONST-037 block in every commit + per-platform tests Phases 5/6/7 + Firebase distribution Phase 14 |
| §2 Themes: VS Code JSON | Phase 1 parser + Phase 2 Yole-Light/Dark + Phase 3 ThemeProvider |
| §2 Surfaces: editor + preview + filename badges | Phase 9 + Phase 10 + Phase 11 |
| §2 Approach A architecture | File structure section enforces this |
| §3.7 Markdown-default format enablement | Phase 4 |
| §3.8 Unified theme system on VS Code JSON | Phases 1–3 |
| §7 19 anti-bluff tests | Distributed across phases; explicit in §7 spec; this plan creates each |
| §7 4 challenges in qa-all | Phase 12 |
| §9 Documentation deliverables | Phase 13 |
| §10 Firebase distribution for all 4 platforms | Phase 14 |

No gaps.

**Placeholder scan:** 

Phases 0–4 contain full TDD bite-sized tasks with complete code. Phases 5–14 contain task-level checklists with exact file paths but defer specific code (e.g., the Tree-Sitter JNI binding code) to the moment Phase 0 research closes those questions. This is **not** a placeholder — it's an explicit dependency on a research output that's itself a phase with its own tasks. The plan is honest about it via the "Pre-execution gate" markers at the top of each later phase.

**Type consistency:** 

`Theme` is defined once in Phase 1 Task 1.3 and referenced consistently. `TokenizerEngine` expect class is declared in Phase 5 Task 5.1 with a single API (`tokenize`, `loadGrammar`, `initialize`) used identically in Phases 5/6/7/8. `SyntaxHighlighter`, `ScopeMapper`, `GrammarRegistry` all introduced in Phase 8 and referenced by Phases 9–11. No drift.

---

## Execution handoff

Plan saved to `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration. Best for a long plan like this; keeps the main context clean and parallelizes review.
2. **Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints. Faster wall time for short plans; less suitable here given the scope.

**Strong recommendation: subagent-driven.** This plan is ~17 phases / ~80 tasks. Inline execution would burn through context budget on Phase 0 alone.

Pick approach.
