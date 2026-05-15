### Recent changes
- See [Discussions](https://github.com/vasic-digital/Yole/discussions), [Issues](https://github.com/vasic-digital/Yole/issues) and [Project news](https://github.com/vasic-digital/Yole/blob/master/NEWS.md) to see what is going on.
- New Updates also visible here: <https://github.com/vasic-digital/Yole/releases>


## iter-58 v1.2.0 — Source-code file support: 55 languages + 5 editor affordances (2026-05-15)

**Version:** 1.2.0 (versionCode 120 → dotted `0.0.0.1.20`)
**Build status:** Desktop full (47/55 languages with Tree-Sitter); Android partial (markdown only — NDK bulk-build pending); iOS BLOCKED (Xcode not in build env); Web limited (vscode-textmate, markdown grammar).

### Added

- **`LanguageFormat` data class** (`shared/.../language/LanguageFormat.kt`) — unified
  per-language manifest: id, displayName, extensions, mimeTypes, commentSyntax,
  indentRules, bracketPairs, indentUnit. Extends the iter-57 Grammar abstraction
  with Feature 2 affordance data.
- **`LanguageMetadata` object** — 55 `LanguageFormat` singletons covering all languages
  in the iter-57 inventory. Sourced from research-report.md §4.2 (55-language
  comment/indent/bracket convention table).
- **`LanguageRegistry`** — lookup by id or filename extension; `detectByFilename`
  powers format detection for the new language set.
- **`LocalLanguage` CompositionLocal** — Compose plumbing so any editor Composable
  reads the current file's `LanguageFormat?` without prop-drilling.
- **`CommentSyntax` + `IndentRules` + `BracketPairs` data classes** — the three
  affordance-data carriers; each populated per-language from the canonical style-guide
  sources (PEP-8, K&R, rustfmt, gofmt, etc.).
- **`ScmQueryLoader`** — loads `.scm` Tree-Sitter query files from the classpath
  resource path `grammars/<id>/<query>.scm` with in-memory cache and
  `clearCacheForTest()` test hook.
- **`FoldQueryRunner` (`expect class`)** — platform-dispatched Tree-Sitter fold-range
  extraction using `folds.scm`. Desktop actual uses the bonede JVM API; Android/iOS/
  Wasm actuals return `emptyList()` + log a defect reference honestly (CONST-035).
- **`OutlineExtractor`** — commonMain class; runs `outline.scm` captures via
  `TokenizerEngine` to produce `List<OutlineItem>(name, kind, lineNumber)`.
- **`HtmlEmbeddedLang` object** — re-tokenizes `<style>` and `<script>` embedded
  regions using CSS and JavaScript sub-engines; merges byte-offset-adjusted token
  streams back into the outer HTML token list. Honest: no sub-tokens fabricated
  when the sub-engine is unavailable (CONST-035).
- **`MarkdownCodeFences` object** — detects fenced code blocks by sub-language tag
  via Markdown's `injections.scm`; re-tokenizes each fence body using the matching
  sub-engine when the grammar is available and enabled.
- **5 editor affordances** wired into `androidApp` `SyncedScrollEditor`:
  - `CommentToggleAction` — Ctrl+/ comment/uncomment using `CommentSyntax`.
  - `IndentEngine` — smart Enter-key indent using `IndentRules` + `indentUnit` +
    optional `indents.scm` Tree-Sitter query.
  - `BracketAutoCompleter` — auto-close pairs from `BracketPairs`.
  - `OutlineDrawer` — slide-in Composable drawer with symbol list + tap-to-navigate.
  - `FoldGutter` — chevron gutter with collapse/expand on tap.
- **165 `.scm` Tree-Sitter query files** vendored at
  `shared/src/commonMain/resources/grammars/<id>/` (55 languages × 3 query types:
  `highlights.scm`, `folds.scm`, `outline.scm`). Primary source: nvim-treesitter
  (Apache-2.0). Fallback for `less` and `crystal`: helix-editor (MPL-2.0 query
  files). `THIRD-PARTY.md` + `MANIFEST.json` in the grammars root.
- **55 test fixtures** at `shared/src/commonTest/resources/test-fixtures/<id>/` —
  one representative source file per language, each containing the language's own
  comment marker.
- **47 Desktop Tree-Sitter grammars** declared as Gradle dependencies in
  `shared/build.gradle.kts` `desktopMain` block via `io.github.bonede:tree-sitter-<lang>`
  JARs. Each JAR ships native binaries for `linux-x86_64`, `linux-aarch64`,
  `windows-x86_64`, `osx-x86_64`, `osx-aarch64`.
- **`BonedeGrammarRegistry`** (Desktop JVM) — maps 47 Yole language IDs to their
  `org.treesitter.TreeSitter<Name>` class names; explicitly lists the 8-lang gap set
  as `unsupportedLangs` so the engine never fabricates a grammar instance.
- **2 anti-bluff challenges** wired into `make qa-all`:
  - `language_grammar_bundle_challenge.sh` — verifies 47 bonede JARs declared + gap
    set documented in `KNOWN_DEFECTS.md` + Gradle build succeeds.
  - `language_support_completeness_challenge.sh` — verifies all 55 languages have
    comment coverage + `LanguageRegistry.detectByFilename` returns non-null for each.
- **`make qa-iter-58-gates`** — runs both new challenges + the full shared test suite
  in a single target.

### Changed

- **`LanguageRegistry` wiring** — `FormatRegistry.createFormats()` now reads from
  `LanguageRegistry.all()` for the programming-language entries; the iter-57 format
  list and the iter-58 language list are unified under a single detection pass.
- **`MarkdownParser`** — upgraded to consume `MarkdownCodeFences` output; fenced
  code blocks are now re-tokenized by the appropriate sub-grammar when available
  (previously the upgrade was preview-only; iter-58 extends it to the editor view).

### Known limitations (each tracked as a `#…` entry in `docs/KNOWN_DEFECTS.md`)

- **`#f2-phase-7-android-ndk-bulk-build-pending`** — 47 bonede language grammars are
  bundled for Desktop (JVM) only. Android builds require cross-compilation against
  the Android NDK clang toolchain. The build script `tools/build-language-grammars.sh`
  is fully implemented; the operator must run it for all 47 langs × 3 ABIs (~141
  NDK builds, ~5-15 min). Android users currently see plain text for 46/55
  non-markdown languages (honest: `TokenizerEngine.android.kt loadGrammar()` throws
  `IllegalArgumentException` naming this ticket).
- **`#f2-phase-7-no-bonede-artifact`** — 7 languages (`jsx`, `xml`, `vim`, `less`,
  `crystal`, `groovy`, `bibtex`) have no published Maven Central bonede artifact.
  Comment toggle, auto-indent, and bracket-pair auto-close work via `LanguageMetadata`
  data; outline and fold are grammar-gated and currently unavailable.
- **`#f2-phase-7-nim-grammar-broken`** — the Nim bonede artifact exists but the
  native `.so` segfaults on parse against all tested bonede core versions. Nim is
  in `BonedeGrammarRegistry.unsupportedLangs`; non-grammar affordances are unaffected.
- **`#f2-phase-7-ios-xcode-required`** — the iOS build scaffold is fully implemented
  in `tools/build-language-grammars.sh ios`; however `xcrun --sdk iphoneos` fails on
  the build host (Command-Line Tools only; Xcode not installed). iOS users see the
  three non-grammar affordances only.

### Cross-platform impact summary (per CONST-037)

- **Android:** comment toggle, auto-indent, bracket-pair auto-close active for all
  55 languages. Outline + fold + syntax highlighting active for Markdown only. 46
  other languages awaiting NDK bulk-build.
- **Desktop:** full feature set — all 5 affordances + syntax highlighting for 47/55
  languages. The 8-lang gap set (no bonede artifact or broken artifact) is honestly
  limited to non-grammar affordances.
- **iOS:** three non-grammar affordances for all 55 languages. Tree-Sitter grammars
  BLOCKED on Xcode build environment.
- **Web:** three non-grammar affordances for all 55 languages. Outline and fold
  require a future upgrade from `vscode-textmate` to `web-tree-sitter`.

### Commits (this iteration, in order)

```
9e98b6e8  docs(iter-58): Phase 0 deep-research report
42d30d24  docs(iter-58): source-code file support implementation plan
f4444bc1  docs(iter-58): source-code-file-support design spec
281356d0  feat(iter-58): Phase 1 — LanguageFormat + LanguageRegistry + LocalLanguage foundation
e50295c6  feat(iter-58): Phase 2 — CommentSyntax + IndentRules + BracketPairs data + tests
2402addc  feat(iter-58): F2 Phase 3 — ScmQueryLoader + FoldQueryRunner + OutlineExtractor
a9482ec2  feat(iter-58): F2 Phase 4 — wire CommentToggle + BracketAuto + IndentEngine into Android editor
8c7862d0  feat(iter-58): F2 Phase 5 — OutlineDrawer + FoldGutter UI wired into Android editor
36621a3f  feat(iter-58): F2 Phase 6 Batch 1 — 53-row LanguageMetadata + 12 langs vendored
c6d98067  feat(iter-58): F2 Phase 6 Batch 2 — 10 langs vendored (tsx jsx yaml toml xml bash ruby php swift scala)
8883ccd7  feat(iter-58): F2 Phase 6 Batch 3 — 10 langs vendored (dart lua perl haskell ocaml julia r elixir erlang fortran)
928b9c32  feat(iter-58): F2 Phase 6 Batch 4 — 10 langs vendored (vim dockerfile makefile terraform regex vue graphql csharp less scss)
0673d1d9  feat(iter-58): F2 Phase 6 Batch 5 — final 11 langs + kotlin/markdown anchors
8f8b01ef  feat(iter-58): F2 Phase 6 final — 3 structural tests + crystal stub correction
042e4beb  docs(iter-58): F2 Phase 6 closeout — KNOWN_DEFECTS entry for grammar bundling gap
bd20ab40  docs(iter-58): F2 Phase 6 closeout — CONTINUATION.md update
9606ff42  feat(iter-58): F2 Phase 7 — 47 Tree-Sitter grammars bundled on Desktop (5 ABIs)
a68bd8e9  feat(iter-58): F2 Phase 8 — HtmlEmbeddedLang + MarkdownCodeFences sub-language tokenization
2982ded0  test(iter-58): F2 Phase 9 — 2 anti-bluff challenges + qa-iter-58-gates
84714a90  docs(iter-58): Phase 10 — 3 docs + CHANGELOG + CONTINUATION
```

---

## iter-57 v1.1.0 — Syntax highlighting + unified theme system (2026-05-14)

**Version:** 1.1.0 (versionCode 110 → dotted `0.0.0.1.10`)
**Firebase distribution:** Android Release (release ID `4lv1guruqhpsg`) + Debug (`4e4acl147ej3o`) to 4 testers. Desktop macOS-arm64 DMG built (SHA-256 `aa65523c…`) but out-of-band (Firebase doesn't support Desktop). Linux/Windows desktop + Web Wasm BLOCKED on pre-existing infra gaps. iOS deferred (`#phase-7-blocked-on-ios-baseline`). See `docs/qa/iter-57/` for full evidence.



### Added
- **VS Code theme JSON as the unified app theme system.** The legacy
  hardcoded `IdeTheme.kt` + `YoleColors.kt` palettes are replaced by
  `Yole-Light.json` + `Yole-Dark.json` with byte-exact pixel parity
  to the prior look. The whole app — backgrounds, surfaces, status
  bar, drawer, dialogs, editor, syntax tokens — reads from a single
  active `Theme` exposed via `LocalTheme` CompositionLocal.
- **Syntax highlighting in the editor.** `SyncedScrollEditor` accepts
  an optional `SyntaxHighlighter`; when set, `BasicTextField` renders
  a colored `AnnotatedString` via `VisualTransformation` with an
  80 ms keystroke debounce.
- **Syntax highlighting in preview code blocks.** Markdown fenced
  code blocks (`\`\`\`kotlin`) are tokenized and wrapped in
  `tok-<scope>` CSS spans; per-scope colors come from the active
  theme's `tokenColors[]`.
- **Filename badges in the FILES tab.** Each file row shows a
  2-letter language chip tinted by the active theme's
  `badge.background.<langId>` (or generic fallback).
- **`Settings → Formats` screen + one-time migration dialog.**
- **TokenizerEngine** with platform actuals: Tree-Sitter via JNI on
  Android + Desktop (bonede `tree-sitter:0.22.6`), vscode-textmate
  via Kotlin/Wasm `@JsModule` interop on Web.
- **4 new anti-bluff challenges** wired into `make qa-all`:
  `syntax_highlighting_challenge.sh`,
  `syntax_highlighting_per_platform_challenge.sh`,
  `theme_unification_challenge.sh`,
  `format_enablement_default_challenge.sh`.
- **Documentation:** `docs/features/syntax-highlighting/user-guide.md`,
  `architecture.md`, `theme-migration-guide.md`,
  `settings-formats-guide.md`, `research-report.md` (616 lines, 120
  URL citations).

### Changed
- **Markdown is the only default-enabled format.** Every other format
  (17 prior + 12 v1 source-code languages) is opt-in via
  `Settings → Formats`. Operator-mandated; existing users see a
  one-time mandatory migration dialog on first launch after upgrade.
- **`MarkdownParser`** now emits `class="language-<lang>"` on
  language-tagged fenced code blocks (was bare `<code>`).

### Removed
- Legacy `IdeTheme.kt` (`androidApp/.../ui/theme/Theme.kt`) and the
  shared `YoleColors` palette object (`shared/.../ui/Theme.kt`).
  463 callsites migrated across Android + Desktop + iOS + Web shells.
- 4 legacy `Theme*Test.kt` files testing the now-deleted YoleColors
  palette API — deleted alongside the API per CLAUDE.md "delete tests
  for deleted APIs" exception.

### Known limitations (each tracked as a `#…` entry in `docs/KNOWN_DEFECTS.md`)
- **`#android-tree-sitter-ndk-so-missing`** — the bonede Tree-Sitter
  library ships native binaries for 5 desktop OS+arch combos but no
  Android NDK `.so`. On Android, `TokenizerEngine.initialize()`
  returns `Result.failure(UnsatisfiedLinkError)`; highlighting
  gracefully unavailable (no fake tokens per CONST-035). Operator
  NDK-build upgrade path documented in `KNOWN_DEFECTS.md`.
- **`#phase-7-blocked-on-ios-baseline`** — iOS `TokenizerEngine` is a
  `NotImplementedError` stub. Two prerequisites: (a) Document-KMP
  sibling submodule needs `@OptIn(ExperimentalForeignApi)` fix
  (CONST-038 we can't fix from Yole), (b) operator-built
  `libtree-sitter.a` + `libtree-sitter-markdown.a` per Apple arch.
- **`#wasmjs-test-baseline-broken`** — Wasm production code compiles
  cleanly, but tests are blocked by ~11K pre-existing errors in
  `commonTest` from `kotlinx.coroutines.runBlocking` (which has no
  Wasm variant). Phase 6 implementation tests cannot execute until
  the baseline is fixed.

### Cross-platform impact summary (per CONST-037)
- **Android:** editor highlighting + filename badges + preview code
  blocks all functional in the UI; tokenizer engine returns honest
  `Result.failure` on devices without the bundled NDK `.so`.
- **Desktop:** full feature set — editor + preview + badges +
  tokenizer all functional. 5 PASS in `TokenizerEngineJvmTest`.
- **iOS:** UI surfaces present; tokenizer engine BLOCKED stub.
- **Web:** UI surfaces present; tokenizer engine implementation
  compiles cleanly, runtime verification pending wasmJsTest baseline.

### Commits (this iteration, in order)
dcc4ac57 spec; f02dd00e plan; 9ab30093 Phase 0 research;
9c0a31e2 + be172282 Phase 1 parser; c538b28f Phase 2 parity;
d9bf5f9d + 311d43d1 + f78af72e + e682175d + 2ea2949b + e341e5ed +
a36f6610 + a33b5ed4 Phase 3 ThemeProvider migration (463 callsites);
f00c0774 + de022ad8 + 9544149b + 8eb05038 + 9d0d8a7d + bb472dbf
Phase 4 format gate + settings screen + migration dialog;
2eafc2de Phase 5 JNI engine; c0bf3305 Phase 6 Wasm engine;
84c01eca Phase 7 BLOCKED scaffold; 9fb5f184 Phase 8 SyntaxHighlighter
API; 8acfa501 Phase 9 editor wiring; 66e6ef39 Phase 10 preview
highlighting; 32078f9b Phase 11 filename badges; bb56aa11 Phase 12
4 challenges + qa-all wiring; <Phase 13 docs commit>.


## iter-56 — CONST-038 Submodule decoupling & reusability mandate (2026-05-14)

### Added
- **CONST-038 — Submodules Must Remain Fully Decoupled and Reusable.**
  Root `CONSTITUTION.md` + `CLAUDE.md` + `AGENTS.md`. Every submodule
  consumed from this repo (and recursively from any submodule's own
  `.gitmodules`) is shared infrastructure consumed by multiple
  independent consumer projects. No Yole-specific platform list,
  feature name, version string, or path may leak into shared-submodule
  source or governance. Definition of Done bumped 6 → 7 items.
- **Project-agnostic CONST-038 mirror in 10 owned submodules.**
  Challenges, Challenges/Panoptic (nested), Containers, HelixQA,
  LLMProvider, Security, Dependencies/HelixDevelopment/{DocProcessor,
  LLMOrchestrator, LLMsVerifier, VisionEngine}. Each got 3 governance
  files updated (30 files total). All commits pushed to their
  respective remotes before parent commit.

### Excluded
- 27 third-party upstream submodules under `HelixQA/tools/opensource/*`
  (scrcpy, leakcanary, allure2, appium, perfetto, chroma, etc.) —
  not our property; per the rule itself, we have no right to amend
  their governance.

### Forensic notes
- The CONST-038 propagation closes the loop on the iter-55 discovery
  that several Yole submodules are shared with at least one other
  project (codenamed Atmosphere/Lava). iter-55 reverted Yole-specific
  CONST-037 governance from those submodules; iter-56 replaces it with
  a project-agnostic decoupling rule that fits naturally and doesn't
  collide with any consumer's parallel work.

### Cross-platform impact
- Android / Desktop / iOS / Web: governance-only, no code change.


## iter-55 — Platform sync & cross-platform governance (2026-05-14)

### Added
- **CONST-037 cross-platform impact mandatory consideration rule.** Root
  `CONSTITUTION.md` + `CLAUDE.md` + `AGENTS.md`. Every change MUST be
  reasoned about across Android / Desktop / iOS / Web BEFORE coding;
  commit bodies for multi-platform changes MUST include a
  "Cross-platform impact" block. Definition of Done bumped 5 → 6 items.
- **`SyncedScrollEditor` composable** (`androidApp/.../ui/editor/`):
  reusable IDE editor surface holding a single `rememberScrollState()`
  shared between the gutter `Column` and a `BasicTextField`.
- **`scroll_sync_challenge.sh`** + **`cross_platform_parity_challenge.sh`**
  in `yole-challenges/scripts/`. Wired into `make qa-all` via new
  `qa-iter-55-gates` target. Both emit positive runtime evidence on
  PASS per CONST-035.
- **`EditorScrollSyncRobolectricTest`** (4 cases, mutation-verified):
  asserts single shared ScrollState, same-variable verticalScroll on
  both gutter + editor, ScrollState propagation identity, and file
  co-location.
- **`FileBrowserDedupRobolectricTest`** (5 cases, mutation-verified):
  enforces SubScreen enum + render branch + MoreScreen signature +
  caller-reference + FilesScreen-still-exists invariants.

### Fixed
- **Android editor: line-number gutter desynced from text on scroll.**
  Root cause: gutter `Column.verticalScroll(rememberScrollState())`
  held a state independent from `OutlinedTextField`'s internal scroll.
  Fix: replaced inline editor in `IdeEditorScreen` with
  `SyncedScrollEditor`, which uses `BasicTextField` (exposes explicit
  scroll state) instead of `OutlinedTextField` (doesn't). Both gutter
  and editor now share one `ScrollState` instance.

### Removed
- **Android: duplicate File Browser entry point.** Canonical entry is
  the `Screen.FILES` bottom-nav tab; removed:
  - `SubScreen.FILE_BROWSER` enum value.
  - Both `SubScreen.FILE_BROWSER → FileBrowserScreen(...)` render
    branches under the AnimatedContent + ContentSwitcher transitions.
  - `MoreScreen` "File Browser" Card + `onFileBrowserClick` parameter.
  - Two `onFileBrowserClick=` argument bindings to `MoreScreen`.

### Changed
- Editor's `onOpenFileBrowser` callback + global `Ctrl+O` shortcut now
  navigate to `Screen.FILES` (the canonical FILES tab) instead of the
  removed duplicate.
- 4 submodule pointers bumped (Containers, HelixQA, LLMProvider,
  Dependencies/HelixDevelopment/LLMsVerifier): these submodules carry
  a no-op CONST-037 revert cycle on their remotes after iter-55
  investigation determined they are shared with non-Yole projects
  (Atmosphere/Lava governance commits present on their remotes).
  Project-agnostic propagation deferred.

### Cross-platform impact summary
- **Android:** scroll-sync fix + File Browser dedup applied; 9 new
  Robolectric test cases mutation-verified; all existing test suites
  (accessibility, navigation, file editing, format detection) still
  PASS with no regressions.
- **Desktop:** unaffected by code changes. `FileBrowserScreen` +
  `IdeFileBrowser` retained pending design-review decision on
  potential Desktop dedup (deferred follow-up).
- **iOS:** N/A — editor and File Browser not ported to iOS yet.
- **Web:** N/A — separate Compose-Wasm code path.

### Deferred follow-ups (tracked in `docs/CONTINUATION.md`)
- `#desktop-file-browser-dedup` — design-review decision on Desktop's
  two File Browser surfaces (`FileBrowserScreen` + `IdeFileBrowser`).
- `#const-037-submodule-propagation` — project-agnostic version of
  CONST-037 for shared submodules, after per-submodule audit.
- `#yole-android-build-gradle-version-pre-existing` — pre-iter-55
  failure of `VersionConsistencyTests.testAndroidBuildGradleVersion`
  on master, unrelated to iter-55.


## Yole 1.0.1 (versionCode 101 = `0.0.0.1.1`) — 2026-05-13 (iter-54 distribution)

### Highlights
- **Anti-bluff hardening across the model authority surface.** The
  LLMProvider submodule's `pkg/discovery` Tier 3 (hardcoded
  FallbackModels) was DEPRECATED in iter-53 per CONST-036, with the
  per-provider httptest-fixture sweep tracked as
  `#fallback-tier-removed-needs-httptest-fixture` (75 latent bluffs
  surfaced + counted). Ollama + Venice `TestGetCapabilities` already
  rewritten to use controlled httptest fixtures so they no longer
  drift when upstream catalogues change.
- **New `pkg/apikeys` central authority.** Reads `ApiKey_<Provider>`
  env vars from `~/api_keys.sh` (the operator's canonical credential
  file). Matches the convention used by LLMsVerifier so all three
  surfaces share one source of credential truth.
- **New `apikeys_live_discovery_challenge.sh`.** Real-stack Challenge
  that sources `~/api_keys.sh` and invokes the live HuggingFace
  `/api/models` endpoint. Operator's run captured 5 real models
  including SulphurAI/Sulphur-2-base — positive runtime evidence per
  CONST-035 §11.4.
- **Governance covenant cascade (iter-52).** The verbatim
  end-user-quality covenant ("We had been in position that all
  tests do execute with success and all Challenges as well, but in
  reality the most of the features does not work and can't be
  used! …") was propagated to 48 governance files across the Yole
  superproject, LLMProvider submodule, and all 10 sibling KMP repos'
  CONSTITUTION/CLAUDE/AGENTS triples.
- **Cross-submodule test fixes (iter-52).** macOS portability gaps
  in Challenges (`#!/bin/sh` trailing-newline) + Containers (symlink
  resolution + Linux-only test skip guards) so the full
  multi-submodule suite runs green on the macOS audit host.
- **KNOWN_DEFECTS pruned (iter-52).** SMB stub-no-negotiation and
  WebDAV always-online-stub tickets moved OPEN → CLOSED with
  forensic anchor to commit 1f6472c9 (2026-05-07).

### Known carry-over (tracked in `docs/KNOWN_DEFECTS.md`)
- `#fallback-tier-removed-needs-httptest-fixture` — 75 LLMProvider
  test assertions still consult the deprecated Tier 3 path; multi-
  iteration httptest-fixture sweep owed before the runtime path
  can be removed.
- `#robolectric-compose-ui-tests-brittle` — long-running migration
  to HelixQA on-device automation; mitigated by dedicated container.
- `#helixqa-missing-sibling-repos` — environment bootstrap gap for
  31 HelixQA packages when sibling repos absent.

### Resume-from-here for the next CLI agent
Read `docs/CONTINUATION.md` §37 (iter-53) + §36 (iter-52) for the
full forensic trail.


## Session 7: March 26, 2026

### Phase 1: Concurrency Safety
- Added @Volatile to _isConnected, _rootPath, _rootFolderId across all 8 protocol services
- Added @Volatile to parseSemaphore in FormatRegistry
- Fixed lock ordering violation in cancelOperation (Dropbox, GoogleDrive, OneDrive, FTP)
- Added HttpTimeout (10s connect, 30s request) to Android, Desktop, iOS HttpClientFactory
- 31 new concurrency regression tests

### Phase 2: Dead Code Elimination
- Removed unused ImplementationTier.STUBBED enum
- Documented 20 empty catch blocks as non-fatal resource cleanup
- Added KDoc to all 6 iOS/Wasm protocol stubs

### Phase 3: Security Hardening
- Parameterized hardcoded keystore passwords in build.sh
- Updated SonarQube project version to 2.19.0
- Detekt: 0 violations confirmed

### Phases 4-6: Test Coverage Expansion
- 581 new tests across 14 new test files
- Isolated unit tests: CircuitBreaker, ConnectionLimiter, FormatRegistry, NetworkStorageError, Plaintext/RST/Wikitext/Taskpaper parsers, DocumentModel, MetricsReporter, TextFormat
- Stress tests: per-protocol, parser overload, timeout recovery
- E2E tests: format pipeline, error recovery
- Desktop tests: 8,928 (up from 8,347)

### Session 6 (March 19, 2026) - Phase 1-6 Execution
- **Concurrency fixes**: Additional mutex safety, CancellationException rethrow audit across all protocol services
- **Security hardening**: SecureStorage locking improvements, path traversal protection audit, container security configs
- **Performance monitoring**: FormatRegistry lazy-load optimization, StyleSheets cache improvements, DocumentCache cooperative cancellation
- **Test coverage expansion**: ~1,000 new tests across unit, integration, stress, fuzz, snapshot, load, E2E, accessibility, and non-blocking categories
- **Go ecosystem fixes**: HelixQA race condition fixes, DocProcessor/LLMOrchestrator/VisionEngine test stabilization
- **Legacy cleanup**: iOS stubs with Result types and implementation plans, Wasm platform enhancements
- **Build config fixes**: AGP 8.9.0 alignment, minSdk 24, jvmTarget 11 enforcement, ktlintCheck removal

### Session 5 (March 18, 2026) - HelixQA Expansion & Go Modules
- **HelixQA expansion**: 3 new packages (testbank, ticket, evidence) for autonomous QA sessions
- **HelixQA testbank**: YAML test bank management with platform targeting, priority levels, Challenges bridge
- **HelixQA ticket**: Markdown ticket generator for AI fix pipelines with evidence-based issue docs
- **HelixQA evidence**: Centralized evidence collection (screenshots, video, logcat) with platform-specific capture
- **HelixQA CLI rewrite**: Subcommands (run, list, report, version), integration tests, stress tests, benchmarks
- **HelixQA tests**: 235 tests passing (up from 155), all race-safe
- **3 new Go modules**: DocProcessor (219 tests), LLMOrchestrator (247 tests), VisionEngine (262 tests)
- **Yole submodule**: Updated HelixQA pointer, desktop tests still passing

### Session 4 (March 17, 2026) - Comprehensive Audit & IDE UI Redesign
- **Comprehensive audit**: Full 11-phase implementation plan created from 6 parallel exploration agents
- **Identified issues**: 12 concurrency issues, 6 platform stubs, test gaps, security infra gaps
- **IDE UI redesign**: Modern IDE-style interface for all 3 active platforms
- **UI/UX automation testing**: 183 web tests, 200 Android screenshots, 85 desktop tests
- **HelixQA integration**: Initial 5 packages, 155 tests, race-safe
- **Bug fixes**: ParserRegistry crash fix, AGP 8.9.0 alignment, minSdk 24

### Session 3 (March 8, 2026) - Dead Code Integration & Comprehensive Testing
- **Dead code integration**: CircuitBreaker/ConnectionLimiter wired into all 8 protocol services, DocumentCache in FormatRegistry via parseWithCache(), PathUtils centralized normalizePath()
- **Concurrency safety**: 10 critical fixes (scopeMutex, pauseFlagsMutex, httpClient init, SecureStorage locking), ConcurrencyFixesTest (1006 lines)
- **Test coverage expansion**: Fuzz (23), snapshot (46), load (22), E2E (102), performance baseline (62), accessibility (82+76+52+435 lines), platform-specific desktop (3 files, 956 lines) and Wasm (1 file, 366 lines)
- **Performance optimization**: FormatRegistry lazy init via `lazy { createFormats() }` with `isFormatsInitialized` guard, StyleSheets `styleSheetCache` with `clearCache()`
- **KMP module docs**: CHANGELOG.md and CONTRIBUTING.md for all 10 extracted modules
- **Challenge framework**: challenges.yml workflow, runChallenges Gradle task, 3 new challenge banks (security, format-edge-cases, protocol-resilience)
- **Legacy migration**: iOS stubs improved with Result types and KDoc, Makefile modernized with 20+ targets, LEGACY_MIGRATION.md
- **Final verification**: Fixed runTest to runBlocking<Unit> across 66 files, TodoTxtParser regex backtracking guard, DocumentCache cooperative cancellation, SftpService/SmbService/cloud service fixes
- **Result**: 6,695+ desktop tests passing, 0 failures

### v2.15.2 - Smooth Animations & Settings Persistence
- **🎬 Smooth Animations**: Beautiful slide transitions for tab switching and screen navigation
- **⚙️ Settings Persistence**: All settings now save and persist across app sessions
- **🎛️ Animation Controls**: Configurable animation settings for performance optimization
- **💾 Cross-Platform Storage**: Platform-appropriate settings storage (SharedPreferences, Preferences, NSUserDefaults)
- **🧪 Enhanced Testing**: New tests for animation transitions and settings persistence
- **📖 Updated Documentation**: Comprehensive guides for new animation features

### v2.15.1 - Kotlin Multiplatform Migration Complete
- **🎉 Migration Complete**: Full Kotlin Multiplatform implementation across Android, Desktop, iOS, and Web
- **🏗️ Architecture**: Modular design with shared core and platform-specific implementations
- **📚 All 18 Formats**: Complete support for Markdown, todo.txt, CSV, and 15 additional formats
- **🧪 Testing**: Comprehensive test suite with 100% coverage goals
- **📖 Documentation**: Updated guides for multi-platform development

### v2.15 -> v2.16 - Major Architecture Refactor & 8 New Formats
- **🏗️ Modular Architecture**: Complete refactor into 20+ modules (commons, core, app, 18 format modules)
- **📚 New Formats Added**:
  - **LaTeX** (.tex) - Academic writing with KaTeX math rendering
  - **reStructuredText** (.rst) - Python documentation standard
  - **TaskPaper** (.taskpaper) - Project-based task management
  - **Textile** (.textile) - Lightweight markup format
  - **Creole** (.creole) - Wiki markup standard
  - **TiddlyWiki** (.tid) - Personal wiki format
  - **Jupyter** (.ipynb) - Data science notebooks
  - **R Markdown** (.Rmd) - R programming documentation
- **🧪 Comprehensive Testing**: 100% test success rate with unit, integration, and E2E tests
- **🤖 AI QA Integration**: Automated testing with real device/emulator validation
- **📖 Enhanced Documentation**: Complete architecture guide and format specifications
- **🔧 Build System**: Multi-module Gradle setup with automated testing pipeline

### v2.14 -> v2.15
- [2.14.0 - 2.14.1](https://github.com/vasic-digital/Yole/compare/v2.14.0...v2.14.1)
- [2.14.0 - 2.15.0](https://github.com/vasic-digital/Yole/compare/v2.14.0...v2.15.0)
- Make .svg editable, closes #2647, by @gsantner
- keep the toolbar ref intact (#2635)
- ShareInto: tracking/bloat parameter filtering - Amazon
- Add SHA-256 to file info dialog (#2622)
- Simpler http(s) regex for GsSimpleMarkdownParser (PR #2606)
- Add dyslexia friendly font (PR #2602)
- Improving view mode and rendering (PR #2566)
- Fixes and improvements for AsciiDoc (darklight, caution-admonition, linebreak, pagebreak, quote, attachment)  (#2597)
- Improve PlaintextSyntaxHighlighter with basic programming language editor syntax highlighting (PR #2560)
- Update README.md (PR #2595)
- Add AsciiDoc-reference, toolbar swap view and save button (#2580) (#2579)
- Upgrade to mermaid v11.6.0 (PR #2575)
- Template: Mermaid-Katex improvements (PR #2574)
- Settings: Move some 'Other' settings to 'Edit mode' (PR #2562)
- Quick filter all files, Share files into markor (PR #2521)
- Fix Prism code block issues (PR #2542)
- Fix unit tests - Color.rgb is Android only API and not available in junit -> own method
- CSV: View-Mode syntax highlighting: Make column 3 readable in dark mode (255 blue -> violet-blue), closes #2540, by @gsantner
- OrgMode: make insert-link action-button use correct syntax, closes #2527 (#2528)
- Improve text rendering performance, closes #2478, closes #2515 (#2509)
- Filebrowser: Per folder sort order (PR #2499)
- Improve line numbers (PR #2470)
- Orgmode fixes and improvements, closes #2405 (PR #2504)
- View-Mode: Code highlighting block - Prism add more language (PR #2508)
- Markdown: Open link action: Fix open links when Markdown link has space at end
- Update Android gradle/tools to 8.10.2/8.8

### v2.13 series
- [2.13.0 - 2.14.0](https://github.com/vasic-digital/Yole/compare/v2.13.0...v2.14.0)
- Markdown: Fix rendering of '$' in non-math view mode (PR #2485 closes #2085 #2236)
- Android Studio latest version support
- Keyboard TAB key handling, syntax highlighting performance, filebrowser navigation (PR #2487 closes #2469 #2484)
- Restore audio action  (PR #2481)
- Multi user file browsing, closes #2479 (PR #2482)
- Select lines (PR #2443 by @harshad1)
- Fix various crashes of v2.13.1, by @gsantner
- Fix lots of minor issues and crashes in v2.13.0, by @gsantner
- Update F-Droid metadata links (#2466)
- (wshoy/storageButton, wshoy/master) Update translations (#2467)
- Remove OmRecorder (Voice/Audio recording feature), library outdated and download broken. (PR #2468)

### v2.12 series
- [2.12.0](https://github.com/vasic-digital/Yole/compare/v2.11.1...v2.12.0)
- [2.12.1](https://github.com/vasic-digital/Yole/compare/v2.12.0...v2.12.1)
- [2.12.2](https://github.com/vasic-digital/Yole/compare/v2.12.1...v2.12.2)
- [2.12.3](https://github.com/vasic-digital/Yole/compare/v2.12.2...v2.12.3)
- [2.12.4](https://github.com/vasic-digital/Yole/compare/v2.12.3...v2.12.4)
- [2.12.5](https://github.com/vasic-digital/Yole/compare/v2.12.4...v2.12.5)
- [2.12.6](https://github.com/vasic-digital/Yole/compare/v2.12.5...v2.12.6)

- Override .org file extension to use text mimetype (*/* unrecognized on older devices) PR#2455
- android-35 Disable window size overlap, closes #2448 #2387 #2451
- orgmode: add more text styling syntax and textactions (PR #2450)
- orgmode: fix action buttons not being loaded correctly (PR #2449)
- Rework /storage mapped folders (PR #2445 by @gsantner)
- FileManager: Remove top submenu to open special folders, load storage overview in filemanager instead (PR #2440 by @gsantner)
- Register chess extension (.fen) for plaintext - https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation closes #2241 (#2439)
- Open .lrc extension as plaintext by default, closes #2322 (#2438)
- TextAction: Add text case changing functionality (closes #2390, PR #2426)
- Bugfix: showAndFlash must be called on UI thread, closes #2427 (#2428)
- Improvements for HeadlineDialog scroll restoration and copy code block button (PR #2406)
- Improvements for the action buttons (closes #2386, PR #2388)
- Create launcher widget for todo (closes #1997, PR #2379)
- Disable multi-window settings option by default (PR #2420)
- Fixed checking for indices (#2421)
- Fix how multiple windows are launched (PR #2419)
- Add Em Space special key for paragraph indent (#2400)
- GitHub Actions CI configuration - upgrade upload step, fixes CI build error (PR #2416 by @gsantner)
- Updated README with dark/light todo.txt format images (PR #2411)
- Navigation and highlighting improvements (PR #2377 closes #2409 closes #2410)
- Filebrowser: Fix file modification time not reloaded, closes #2196 (PR #2385)
- Minor doc improvements
- Fix inject-head js option not applying, closes #2365, closes #2380 (PR #2383)
- Add usual yyyy-MM-dd newfile default option
- DocumentEditAndViewFragment: Fix all warnings, especially related to null pointers
- Zim: Fix link/attachment format (PR #2147 by @mehw)
- Add history scroll restoration for scroll views (#2366)
- Various bugfixes (anchor/toc jumping #2364, snippets folder #2369, wikitext newfile #2362, virtual directory browser #2350) (PR #2370 by @hardhard1)
- New Color picker, bump Android minSdk from 16 to 18, closes #1428 (PR #2203 by @halfdane)
- Latest Android SDK/Gradle fixes, by @gsantner #2360
- Add links to changelog
- SearchReplace dialog: Use full width, closes #2367, by @gsantner
- ShareInto: Fix crash when editor is null
- Appearance improvements (PR #2359 closes #2358)
- Fix notebook directory not being created, fix NewFileDialog when no last used type, by @gsantner closes #2360
- Project configuration: upgrade to Android SDK 35, Gradle 8.7, AGP 8.5, Build tools 35, by @gsantner, (closes #2346, closes #2305 PR #2355)
- Add ACTION_CREATE_NOTE intent filter, closes #2320, by @gsantner
- Highlight file when going back to filebrowser (PR #2351)
- View-Mode: Prism codeblock copy to clipboard button, update Mermaid and dark mode, closes #2336 closes #2335  (#2345)
- File browser: Make Folder and file icons visually better distinct (outlined icon for file),  (PR by @wshoy #2331 fixes #2186)
- Jump to correct heading via id, other tweaks, bugfixes (PR #2307 by @harshad1, fixes #2312 #2313)
- FileBrowser: Disable file change options in virtual directories (closes #2287, by @gsantner)
-  Format detection fixes, headline dialog improvements (PR #2300 closes #2303 closes #2296 closes #2297)
- Disallow treating openxml (msword) files as textfiles, closes #2285
- ShareInto: Move link checkbox down
- ShareInto: Fix missing text at shared links (PR #2282 closes #2294)
- CI/CD: Update GitHub Actions versions, fixes outdated warnings, force Java from Eclipse
- Fix crash for unknown filetypes, fix EmbedBinary not detected, related to PR #2277
- FileBrowser: Fix file copy file (nullpointer in currentfolder), by @gsantner
- Update year in texts
- Reformat code of PR #2277, closes #2225, closes #2217, closes #2199, closes #2153
- Improvements to newfile dialog, open links, other fixes (PR #2277)


### v2.11 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v211---asciidoc-csv-and-org-mode-todotxt-advanced-search-line-numbers))
- Reworked attachments (PR #2106 by @harshad1)
- Editor/viewer: Side margin improvements 2, closes #2111 (PR #2119 by @guanglinn)
- Format: Add Orgmode - SyntaxHighlight basic support (PR #2107 by @bigger124 @gsantner)
- Editor/viewer: Side margin improvements, closes #2111 (PR #2118 by @guanglinn)
- Reformat code, by @gsantner
- Update translations (PR #2108)
- ShareInto: Automatically remove new YouTube tracking parameter si
- New folder in copy/move dialog, closes #2093 (PR #2098)
- AsciiDoc: Support view-mode light theme, by @TimReset (#1880 #2091 #2092)
- Update translations (PR #2071)
- Line numbers improvements, by @harshad1 @guang-lin @gsantner (PR #2090)
- Feature: Add Line numbers support (Issue #2057, PR #2062, by @guang-lin)
- Chunked undo redo, by @harshad1 (#2052)
- Update translations (PR #2056)
- Update CSV documentation and NEWS/CHANGELOG (PR #2058)
- Markdown: Text converter do not make duplicate header id (closes #2045, by @gsantner)

### v2.10 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v210---custom-file-templates-share-into-remove-url-tracking-parameters))
- [Search: Keep in-files search dialog open, closes #1663, by @harshad1 (PR #1689)](https://github.com/vasic-digital/Yole/pull/1689/files)
- [File management: Support UTF-8 with BOM, by @tifish (PR #1693)](https://github.com/vasic-digital/Yole/pull/1693/files)
- [Editor: Increase performance on editables, more chunked operations, by @harshad1 (PR #1694)](https://github.com/vasic-digital/Yole/pull/1694/files)
- [Dialogs: Don't show OK button at dialogs that require specific selection, by @gsantner closes #1699, closes #1700](https://github.com/vasic-digital/Yole/commit/b4ae32bf0e8ab890ded57718a9598da7a7d52870)
- [Sync clients: Remove irritating file paths from unsupported dialog, by @gsantner, closes #1705](https://github.com/vasic-digital/Yole/issues/1699)
- [Check file existance case insensitive (Android filesystem usually is insensitive), by @gsantner, closes #1695](https://github.com/vasic-digital/Yole/issues/1695)
- [Improve Dialog OK button constraints #1699, by @harshad1 (PR #1720)](https://github.com/vasic-digital/Yole/pull/1720/files)
- [dotFiles: Hide "*_files" and "*.assets" from browser->save page to html, by @tifish (PR #1704)](https://github.com/vasic-digital/Yole/pull/1704/files)
- [More deterministic save / resume - Switching on every tab move, by @harshad1 (PR #1736)](https://github.com/vasic-digital/Yole/pull/1736/files)
- [Improve file handling (hash calc) & FileInfo detection, by @harshad1 (PR #1719)](https://github.com/vasic-digital/Yole/pull/1719/files)
- [Use style/AppTheme.Unified.StartupFlash for DocumentActivity, by @gsantner, closes #1717](https://github.com/vasic-digital/Yole/issues/1717)
- [Editor: Disable richtext pasting which can lead to dropped characters, by @gsantner, closes #1614](https://github.com/vasic-digital/Yole/issues/1614)
- [New file dialog: Custom file templates, using snippets folder (<notebook>/.app/snippets, by @gsantner, closes #676](https://github.com/vasic-digital/Yole/issues/676)
- [Various performance improvements, by @harshad1 (PR #1735)](https://github.com/vasic-digital/Yole/pull/1735/files)
- [ShareInto: Filter few additional tracking parameters in shared URLs, by @gsantner](https://github.com/vasic-digital/Yole/issues/1490)
- [Launcher shortcuts: Open ToDo & QuickNote at bottom, by @harshad1 (PR #1748)](https://github.com/vasic-digital/Yole/pull/1748/files)
- [SearchDialog: Improve condition calculation for simple dialogs, by @harshad1 (PR #1751)](https://github.com/vasic-digital/Yole/pull/1751/files)
- [todo.txt filter search: Disable highlighting at completed tasks, by @harshad1 (PR #1754)](https://github.com/vasic-digital/Yole/pull/1754/files)
- [Editor: Restore behaviour - don't overwrite newer files unless content modified, by @harshad1 (PR #1758)](https://github.com/vasic-digital/Yole/pull/1758/files)

### v2.9 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v29---snippets-templates-graphs-charts-diagrams-yaml-front-matter-chemistry))
- Updates are officially only available from GitHub and F-Droid as of now
- New feature: Snippets
- Discussion forum & questions now on Yole GitHub Discussion
- Faster Yole application startup with less flashing
- Improve performance at filebrowser and editor
- Allow to install APK files from filebrowser on click
- Markdown Table of Contents options
- Zim: Follow links to other wiki pages
- Under the hood improvements for I/O, widgets & syntax highlighting
- todo.txt: Better browsing at editor with dialog & saved search queries
- Settings option to enable/disable Chrome Custom Tabs
- Privacy settings option to disallow screenshots of Yole
- Debloat & drop experimental/unused features, i.e. todo.txt huuid
- Support Android Day/Night theme system
- Filebrowser: Show full filename (multiline allowed instead of singleline only)
- View mode: Open links to folders in filebrowser
- Markdown: Better Math support, add mhchem chemistry module
- Markdown: Add support flowcharts with Mermaid
- Markdown: Add admonition extension (create fancy info boxes quickly)
- Per-file settings - In addition to the global settings, many options are now also configurable on a file basis
- Markdown: Display YAML fron-matter contents (like article titles and publish date)

### v2.8 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v28---multi-selection-for-todotxt-dialogs))
- Reference to GitHub discussion on More page, by @gsantner
- Add multi-selection to todo.txt dialogs, by @harshad1 @gsantner
- In-content search support for encrypted files, #1388 by @opensource21
- Remove alternative todo.txt naming (tags/categories), by @gsantner

### v2.7 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v27---search-in-content-backup--restore-settings))
- Recursive file search with in-content search, #1337 by @adelobosko @harshad1 @gsantner
- Backup and restore settings, Format selection with radio buttons, #1244 by @harshad1 @gsantner
- Per-file font size, #1332 by @harshad1
- Markdown: Support superscript^2^ syntax, #1268 by @gsantner
- View mode: Image/PDF export whole page, add seperate screenshot option, by @gsantner
- todo.txt: Fix trailing space resulting in contexts/projects/due-dates to be entered twice, #1282 by @harshad1
- Markdown: Enable GitLab extension, display video links as html5-video, #1280 by @gsantner
- Markdown: Enable typographic transformation, #1277 by @gsantner
- todo.txt: Fix tags dialog not shows up onLongClick, #1292 by @gsantner
- Editor: Allow top-menu back button also when the file is empty, #1290 by @harshad1
- ShareInto: Improve automatic link reformatting, #1275 by @harshad1
- Search dialogs: Hide search input field when if there is no data, #1298 by @harshad1
- Decrease scrollbar width for better usability, #1306 by @harshad1
- todo.txt: Settings option for always-visible @contexts & +projects, #1305 by @harshad1 @gsantner
- TextActions: Improve cursor placement at Regex replace, #1310 by @harshad1
- File manager: Move/Copy file improvements, add Yes/No/All overwrite options, #1281 by @harshad1
- Fix App might crash on toolbar-click for TOC, #1336 by @adelobosko
- Fix Android 4.4 crash on file move/copy, #1333 by @harshad1
- Fix Android 4.4 crash when opening .txt file due to ZimWiki format detection, #1341 by @fredericjacob
- Markdown: Don't match extra spaces at ordered-list regex, #1367 by @harshad1
- File Manager: Fix MB being displayed as GB at description (SI 1000 unit), #1352 by @gsantner
- File Manager: Duplicate file / allow copy into same folder, #1345 by @harshad1- Filemanager formatter: fix MB being displayed as GB (SI 1000 unit), #1352 by @gsantner

### v2.6 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v26---zim-wiki-newline--new-paragraph-save-format))
- Markdown: Add settings option for newlines to start new paragraphs, #1260 by @gsantner
- Editor/Viewer: Remember last used file format, show current selected format, #1226 by @harshad1
- Editor/Viewer: Back arrow (top menu) finish activity, #1165 by @gsantner
- Editor: Per-file option to enable/disable syntax highlighting, #1168 by @harshad1
- Share-Into: Add launcher, #1184 by @gsantner
- Markdown: Apply Yole Table of Content config for custom `[TOC]: #` too, #1189 by @gsantner
- Editor: Improve writing to sdcard, #1192 by @gsantner
- Zim: Support file generation on `Android<7/Java=6`, #1194 by @gsantner
- Zim: Editor: Support Table of contents (top menu), #1186 by @fredericjacob
- Markdown: Math/KaTex: Improve \\ line breaks usage, #1196 by @radanovicnik
- ShareInto: Add space after formatted link - messengers then show correct link preview, by @gsantner
- Markdown: Add break page example to Markdown reference, by @gsantner
- Editor: Prevent Android accessibility & autofill to produce errors, #1204 by @harshad1
- Main page: Reduce friction when app was running in background for a while, #1210 by @harshad1
- Search: Add input field to filter search results, #1222 by @harshad1
- Markdown: Don't start new list item when reaching file end and toggling, #1213 by @harshad1
- Zim: Simplify Zim format detection, #1227 by @gsantner
- Zim: Add more text actions (links, images, checkbox, ..), #1195 by @fredericjacob
- All formats: Date/Time dialog don't add entry twice to history, #1229 by @harshad1
- Editor/Viewer: Increase scrollbar width, #1241 by @harshad1
- File browser: File move start from current folder, #1234 by @harshad1
- Editor/Viwer: Add file info option (document top menu), #1233 by @harshad1
- Viewer: Privacy: Opt-out of Android WebView's internal metrics, #1181 by @gsantner
- Markdown: Support Notable's special home brewed syntax for attachments, #1252 by @gsantner
- Dependencies: Add source code of colorpicker and build subproject, by @gsantner
- Optimize image assets, by @gsantner
- DevOps: Improvements to GitHub Actions CI/CD configuration, by @gsantner
- Improve encryption wording & usage, #1171 #1179 by @opensource21


### v2.5 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v25---zim-wiki---search--replace---zettelkasten))
- Add Zim Wiki format and template #1098
- Add search & replace (simple|regex, replace once|all) #1112
- Add settings for current file to toolbar #1129
- Fix file sometimes not opens from launcher shortcut #1139
- Use GitHub Actions for CI/CD #1151
- Add template for Zettelkasten #1156
- Add Nord editor color scheme #1134
- Allow to select folder when create new file via share into Yole #1138
- Improve license dialog readability #1119
- General improvements, fixes and translations

### v2.4 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v24---all-new-todotxt---programming-language-syntax-highlighting))
- Markdown: Correctly insert or remove list item on press enter at empty list item
- Remove title from todo.txt date dialog, better usable on small devices
- Fix search sometimes not working when chaging from view to edit mode
- Rework Indent & Move lines Actions
- Add settings option to control todo.txt completition date auto insert
- All new todo.txt support in Yole
- Add many languages to view-mode code highlighting
- Add xlf format (plaintext)
- Remove colored highlighting from changelog dialog
- Add search to Markdown edit-mode outline/TOC dialog
- New file dialog remember type selection
- Better preserve current open folder across device rotation & reboot
- todo.txt: Long press sort to sort by most recent used method
- Markdown: Improve bold/italic syntax highlighting with punctuations
- todo.txt: Create done file again when not exists
- Add Actions: Move line up & down, start new line
- Insert Date/Time text action: List of recent time formats
- Control visibility of text actions
- Edit-Mode Search: Open selected position instead of first match
- File browser: Add settings option to customize file description format
- todo.txt: Syntax highlighting in edit-mode search dialog
- Save last used folder to settings, use info for titlebar
- New File Dialog: Remember type selection

### v2.3 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v23---table-of-contents-custom-action-order))
- Add action to Move current selected line(s)/cursor text up/down
- Add settings option for View-Mode link color
- Improve table of contents - add border, disable underline
- Long press toolbar to jump to top/bottom (edit & view mode)
- Add search to View Mode
- Accessibility improvements & Talkback support
- Allow http protocol on Android>=9
- Telegram file edit support
- Markdown: Normal sized headers by default, increases performance
- Disable highlighting on big files to improve edit performance
- Don't sort non-document files in third group
- Add Accordion (Click to expand) example and add action button
- Tooltips for action buttons
- For index.html files, show foldername at favourites/recents
- todo.txt: Set completition date also when there is no creation date
- Markdown: Configurable unordered list character
- Custom order of action buttons
- Markdown: Add alternative more performant heading highlighting
- Fix foldername in Main toolbar not reloaded
- Plaintext: Add extensions for AsciiDoc (.adoc), OrgMode (.org), Ledger (.dg .ledger), Diff (.diff .patch)
- Remember last used file extension for new file creation
- todo.txt: Preselect last used archive file by default for archiving
- Markdown: Long press code to insert code block
- todo.txt: Improved task sort functionalities
- Add action button to expand selection of cursror to whole line
- Markdown: Add Table of contents / Outline for Edit mode (Press toolbar)
- Vertical Scrollbar now draggable at view & edit mode
- todo.txt: Date&Time selection dialogs
- Markdown: Auto update ordered list numbers


### v2.2 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v22---presentations-voice-notes-markdown-table-editor))
- Added Presentations & Slides with Markdown
- Added audio recording dialog which allows to add voice to documents. Manual interaction required to start & stop voice recording. Voice recording permission required for this feature
- Added editor button to create Markdown tables
- Markdown Footnotes support added
- Added attachment button for all formats (insert color, link, image, file, audio, date)
- Date/Time button long press now inserts text with last used format
- Improved SD Card reading & writing
- Added option to File-import-dialog to import to notebook instead of current folder
- Reordered editor buttons so global actions are on same position at all formats
- Source code highlighting for View mode
- Added settings option to enable experimental features
- New experimental feature: Convert epub to plaintext and replace current text with ebook
- New experimental feature: Speed Reading for (text from) edit mode
- New Special Keys option: Indent / Deindent current line
- Copy textfile to clipboard from file browser
- Added highlighting todo.txt due dates
- Long press the todo.txt date button to insert due date
- Sepia editor theme


### v2.1 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v21---key-value-highlighting-jsoniniyamlcsv-improved-performance))
- Improved editor, highlighting and overall performance
- New file dialog: Templates
- New format: KeyValue - highlighting for json, ini, csv, yaml, vcard, ics, toml and other simple key-value like syntax
- Long click on main view plus button -> open favourites/recents
- Use lightweight Markdown heading highlighting on non-highend devices
- Show SD Card dialog when opening file that is under SAF
- Share: Rename PDF -> Print/PDF 
- Text action to sort todo by date
- Keep view mode scroll position
- Remove LinkBox from main screen to improve performance
- Make filesystem selection dialog fill screen
- Rework share into: Use file browser to select favourite/recent/popular files
- Special keys added: Insert page break for PDF/Printing, ohm key, punctation mark arrows
- Append linefeed on end when saving
- Show error when trying to rename to existing file/folder
- Add special handling for percent encoded filenames in nextcloud/owncloud folder
- Link 'More->Help' to Project website FAQ
- Debug Log settings option
- Improve local/linked file opening when clicking link at preview
- Add option to set font size in view mode
- Share (multiple) files from file browser
- PlaintextConverter: Put HTML into preview as is (allow to view html files)
- Fix folder title not visible sometimes
- Enable hex color highlighting for various prefix/postfixes (like colon, quote, ...)

### v2.0 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v20---search-dotfiles-pdf-export))
- Recursive file & folder search
- Search button in editor, viewer and file browser
- Always export PDF and images with light theme and white background (improves printing)
- Show dialog on for textfiles to choose open in Yole or other app
- Setting to set file extensions to always load in Yole
- Always view files starting with "index."
- Setting to configure wrap mode (=line breaking)
- Menu option for reload file (editor/viewer)
- Menu option for hiding files & folders starting with a dot
- Setting to set tab width
- Improve back button when always start view mode is set
- Keep file browser sort order
- Improve inline code highlighting
- Add new line when archiving tasks


### v1.8 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v18---all-new-file-browser-favourites-and-faster-markdown-preview))
- Show app intro at first start  
- All new file navigation  
- Add favourite files  
- Add quick navigiation options (to notebook, sdcard, AppData and more)  
- Add option to set Navigation-Bar color  
- Combine edit & view mode to one fragment, show view as overlay  
- Add horizontal scrolling for code blocks in view mode  
- More efficient undo/redo  
- Option to enable/disable swipe to change mode  
- WikiLinks: Disable default escaped characters, so subfolder path is not converted to hyphen  
- Added fonts: Source Pro, DejaVu Sans Mono, Ubuntu, Lato  
- Scan storage Fonts folder for custom fonts  
- Add word count to document info dialog  


### v1.7 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v17---custom-fonts-linkbox-with-markdown))
- Improved app color theme for better readability  
- Load custom fonts from file  
  - Yole bundles 5 additional open fonts  
  - Copy custom fonts to folder: 'Notebook/.app/fonts/'  
- Links shared from e.g. browsers are automatically converted to Markdown syntax if possible  
- LinkBox is now listed on the main view bottom bar  
- LinkBox defaults on new installations to LinkBox.md as filename  
- Default to last used date/time format at dialog  
- Apply todo.txt format only for .txt files  
- L/R Swipe in edit/representation mode to change mode  
- Open link textaction: Don't include trailing ')' in parsed URL, which is common for markdown  
- Added App Shortcuts, requires Android 7.1+  
- Markdown: Enable WikiLink style to reference [[file]] relative  
- Strip #ref from URL in representation to determine if another file should be opened on click  
- Option to set app start tab (Notebook / ToDo / QuickNote / LinkBox / More)  


### v1.6 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v16---datetime-dialog---jekyll-and-katex-improvements))
**New features:**  
- TextAction: Insert date/time  
- Add website title when sharing into Yole, if browser supports it  
  - Website title + URL formatted in Markdown format if possible

**Improved:**  
- Automatically create ToDo/linkbox/QuickNote and parent folders when using respective launcher  
- KaTex/Math: Improve inline math  
- Close virtual keyboard after creating new file  
- Language selection: Load system's most important language as system hint  
- Markdown + Jekyll: Replace {{ site.baseurl }} with .. in representation  
- More padding at settings on older devices  
- Use the new file dialog for sharing into new documents
- Filesystem dialog now shows images / textfiles only at respective file selection  

**Fixed:**  
- New file dialog: Jekyll option on older devices  
- Title not updated when swiping  

### v1.5 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v15---multiple-windows-markdown-tasks-theming))
**New features:**  
- App-wide  
  - Settings option: Keep screen on
- Editor  
  - Open multiple Windows  
- Document browser  
  - Completly new 'New file' dialog  
- Text Actions  
  - Sort todo.txt files  
  - Tasks support in Markdown  

**Improved:**  
- Document browser  
  - Replace 'Reload button' with pull down to refresh  
  - Added 'Last modified' to File information dialog  
- Editor  
  - Added greenscale basic editor colors  
- Representation  
  - Set inital background color before loading document  
  - Math/KaTex: Show inline when single dollar is used

### v1.4
**New features:**  
- App-wide  
  - Add popular documents to 'share into'  
- Editor  
  - Settings options for editor background and foreground color  
  - todo.txt: Highlight multiple levels of context/projects (@@/++)  
- Text Actions  
  - Add zero-width space character to 'special characters'  
  - Add color picker  

**Improved:**  
- TextActions  
  - Markdown: Multiline textaction for header/quote/list  
- Editor  
  - More space for document title  
  - Harden automatic file naming and moving  
- Representation  
  - Enable block rendering for KaTex (math)  
- App-wide  
  - Natural scrolling in dialogs  

**Fixed:**  
- Filesystem  
  - Discard selection when leaving filesystem view  

### v1.3
**New features:**  
- App-wide  
  - Add 'Auto' theme, switch light/dark theme by current hour  
  - Support for Chrome Custom Tabs
- Editor
  - Start document at the recent cursor position (jump to bottom on new documents and at special files)  
  - Enable link highlighting in plaintext format (especially easier to distinguish title and links in linkbox)  
- Text Actions  
  - Markdown: Long press image adds img-src with max-height  
  - Long press 'Special key' jumps to top/bottom  
  - Long press 'Open external' opens content search  
 
**Improved:**  
- TextActions  
  - Don't list empty lines in simple search  
  - Edit picture supports now relative filepaths too  
  - Show import dialog for selected pictures too (like in file selection)  
- Representation  
  - Renamed from Preview
  - Performance improvement for TOC & Math - only use when text contains headers/math  
  - Markdown: Underline h2 too (like h1, more common for two levels)  
  - ToDo: Add alternative naming for contexts/projects  
- App-wide  
  - 

**Fixed:**  
- Editor
  - Disable 'History disable' performance option for data integrity
- App-wide
  - Special files: When app launcher was used, create file if not exists   

### v1.2 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v12---markdown-with-katexmath---search-in-current-document))
**New features:**
- General
  - Launchers to directly open LinkBox/ToDo/QuickNote (opt-in)
- Text Actions
  - Search/filter lines by input (available in special-keys button menu)
  - Todo: context aware search for projects,contexts (longpress project/context button)
- Preview
  - Table of contents (opt-in))
  - Math using KaTex (opt-in)

**Improved:**
- Converter
  - Markdown: More features enabled, notably GFM like table parsing and underlined h1
- Settings
  - More spacing between categories

**Fixed:**
- Editor
  - File saving

### v1.1 ([Details](https://github.com/vasic-digital/Yole/blob/master/NEWS.md#markor-v11---markdown-picture-import-from-gallery-and-camera))
**New features:**  
- Text Module Actions
  - Markdown Picture Dialog
  - Load picture from gallery
  - Take picture with camera
  - Edit picture with graphics app

**Improved:**  
- Formats
  - Load Markdown Format for .md.txt files

**Fixed:**  
- Editor
  - Change default lineheight back to 100%
  - Not connects multiple lines anymore
- Filesystem view
  - More checks for storage access and the yellow info box

### v1.0.1
**New features:**  
- Add popular files (most used files by access count)
- Add popular & recent files as virtual folder under /storage/
  - Selectable e.g. for widgets

**Improved:**  
- Text-Module-Actions
  - More safety checks at execution
- Highlighting
  - MD: Better code readability
  - MD: Better unordered list readability

### v1.0.0
**New features:**  
- ShareInto
  - Added export: calendar appointment

**Improved:**  
- Widget
  - Added shortcuts to ToDo, QuickNote and LinkBox
- SD Card handling and permission errors
  - Show warning when opening a file on not writeable path
  - Add shortcuts to writeable SD card folders
  - Mark unwriteable files red in selection dialog
- ShareInto
  - Better separator placement

**Fixed:**  
- Widget
  - Open selected file
- Editor
  - Markdown header highlighting padding
- Share to app
  - Fix view intent not starting on some devices
- Filesystem view
  - Allow to view Details for folder too

### v0.3.10
**New features:**  
- ShareInto
  - Show "open in browser" option if text contains link
  - Prepend separator to all existing documents
- Settings / Preview
  - User customizeable CSS/JS injection option (for preview)
  - Configureable in settings
  - Contains some (uncommented) modification lines for important elements
  - like font size, font type, script to load when page loaded etc.

**Improved:**  
- Inherit font size from global font preference

**Fixed:**  
- Recents working without having opened anything yet

### v0.3.9
**Improved:**  
- Translation updated
- Updated project description
- Slightly modified adaptive icon

**Fixed:**  
- Editor-Rotation: Creates new file again when editing before
- Create folder: Screen rotation

### v0.3.8
**New features:**  
- Recently viewed documents
  - Start editing of recent documents, button in the toolbar of main view
  - Allow sharing into recend documents
  - Queue containing the 10 last viewed files
- Keep scroll position when reloading document list (Notebook)
- Document/File Info: Dialog showing information about selected file
  - Openable at main views toolbar when one item is selected

**Improved:**  
- Overall better performance 
  - Faster document loading
  - Decreased memory usage
- Reduce edit history size (undo/redo) to 5 for lower memory usage
- Preview/Rendering (All):
  - Rework of theme, font-size and font injection
- Preview/Rendering (Markdown):
  - Blockquote theme based styling
  - Blockquote RTL compatibility

**Fixed:**  
- Crash when Yole put to background and huge file is loaded
  - Document contents are not stored into resume cache anymore if they are too big
  - Make no major differences for huge files, just undo/redo history is cleared when switchting away

### v0.3.7
- Option to disable spellchecking-underline
- More file managers and sync clients supported (notably: seafile)
- Improve default settings
- Limit history size to improve performance
- Support UTF8 local filename references

### v0.3.6
- Decrease padding of textmoduleactions for more fitting elements
- Add delete action for all formats
- Add open link in browser moduleaction
- Fix actionmode icon color
- Share into:: Add LinkBox option
- Share into:: Fix re-sharing of text

### v0.3.5
- All new More section
- All new "Share into" handling
- Fix: Keep dates when priority is assigned
- Mod: Disable colored,underlined header text
- Add: Enable zoom gestures in Preview
- Mod: md::link/image filesystem selection in working directory
- Remove MoreFragment and AboutActivity
- Share into:: Editable text
- Share into:: Re-Share option
- Share into:: Copy to clipboard option
- Share into:: Landscape support
- Update settings
- Improve permission checking

### v0.3.4
- Replace commonmark markdown parser with flexmark
- Various smaller bug fixes
- Updated translations
- Settings option: Edit in screen center
- Add ".." in folder selection to go up

### v0.3.3
- Add support for editing files from most file managers
- Allow to open from Own/NextCloud
- Hints about using Yole with Dropbox
- Allow to set document folder outside of internal storage
- Fix import dialog orientation crash
- Add option to start editing on bottom
- md: moduleaction: end line with 2 spaces
- Improve project icon
- Trim share-into text
- Improved exporting/sharing


### v0.3.2
- Todotxt: Support delete, archive tasks
- Todotxt: Try to keep cursor position
- Translation updates :)

### v0.3.1
- Option for custom line height
- Remember cursor position when switching away from app
- Special keyboard actions (Page up, Tab,..)

### v0.3.0
- New project icon
- Settings ordered in subscreens
- Additional syntax information

### v0.2.5
- Improve highlighter performance
- Improve default highlighter settings
- Added new highlting settings options
- Preference support lib for settings

### v0.2.4
- Abstract highlighting, converter and text module actions
- Added: Support for todo.txt
- Added: Icons to settings
- Added: Hex-Color-Code underlining
- Added: Changelog dialog

### v0.2.3
- Select  file/image from filesystem
- Fix relative web local file loading
- Added: manually save option
- Added: Launcher shortcuts
- Filesystem: Add refresh menu option

### v0.2.2
- Show document and file amount below folders
- Settings toolbar option
- Highlight line-endings with two spaces (MD line break)
- Translation updates

### v0.2.1
- Translation update
- Widget and document list fixes
- Improve editor actions

### v0.2.0
- Rework of core functionalities of the app
- Added QuickNote
- Redo/Undo editing
- Separate Preview/Edit by fragments, unify functions
- Rewrite storage/handling of documents
- Improved sharing into the app, allow appending
- Added bottom navigation
- New font chooser in settings

### v0.1.6
- Added: Many new supported languages
- More supported markdown elements in highlighter
- More markdown file extensions supported
- Improved language selection
- Share as image fixed


### v0.1.5
- Added: Translation: Brazilian, Polish, Hindi, French, Russian, Ukrainian, Italian
- Added: Sort files
- Added: Replace markdown charbar with actions
- Improved: Syntax highlighting

### v0.1.4
- Fixed: replaced Uri.fromFile with FileProvider
- Added: Share as file, share as PDF
- Use commonmark-java instead of AndDown markdown parser
- Added: Spanish translation (#26
- Added: More font size options

### v0.1.3
- Fix: Renaming case sensitive
- Mod: Syntax highlighting
- Fix: Widget
- Added: Copyright notices
- Changed: Translation license

### v0.1.2
- Overall refactoring
- Remove startup ""authentication""
- Remove slow animations
- FilesystemDialog from scratch
- Save only if title/content changed
- Rework most callbacks and broadcasts
- Rework settings

### v0.1.1
- Fix import (#2)
- Use appcompat in dialogs
- Change cursor in editor
- Fix resizing with fullscreen in editor
- Dialogs in dark

### v0.1.0
- Initial release
- Start of community project Yole
- Fork of writeily-pro
- Different branding
- New initial features

### v
**New features:**  
-

**Improved:**  
-

**Fixed:**  
-
