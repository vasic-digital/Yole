# Yole 1.2.0 — Release Notes (iter-58)

**Release date:** 2026-05-15
**Version:** 1.2.0 (versionCode 120, dotted 0.0.0.1.20)
**Headline feature:** Source-code file support — 55 languages + 5 editor affordances

## What's new

### Language support
- **55 languages** registered via `LanguageRegistry` with per-language
  `LanguageFormat` manifests (id, displayName, extensions, mimeTypes,
  `CommentSyntax`, `IndentRules`, `BracketPairs`, `indentUnit`).
- **47 Tree-Sitter grammars** bundled on Desktop via Maven Central
  (`io.github.bonede:tree-sitter-<lang>`) — 5 ABIs each (linux-x86_64,
  linux-aarch64, windows-x86_64, osx-x86_64, osx-aarch64).
- **165 `.scm` query files** vendored at
  `shared/src/commonMain/resources/grammars/<id>/` (55 languages × 3
  query types: highlights, folds, outline). Primary source:
  nvim-treesitter (Apache-2.0); fallback: helix-editor (MPL-2.0).

### Editor affordances (Android editor)
- **CommentToggleAction** — Ctrl+/ comment/uncomment using
  `CommentSyntax`.
- **IndentEngine** — smart Enter-key indent using `IndentRules` +
  `indentUnit` + optional `indents.scm` Tree-Sitter query.
- **BracketAutoCompleter** — auto-close pairs from `BracketPairs`.
- **OutlineDrawer** — slide-in Composable drawer with symbol list +
  tap-to-navigate.
- **FoldGutter** — chevron gutter with collapse/expand on tap.

### Sub-language tokenization
- **HtmlEmbeddedLang** re-tokenizes `<style>` and `<script>` regions
  using CSS and JavaScript sub-engines; merges byte-offset-adjusted
  token streams back into the outer HTML token list.
- **MarkdownCodeFences** detects fenced code blocks by sub-language
  tag via Markdown's `injections.scm`; re-tokenizes each fence body
  using the matching sub-engine when grammar is available.

### Quality
- **2 new anti-bluff challenges** wired into `make qa-all`:
  `language_grammar_bundle_challenge.sh` + `language_support_completeness_challenge.sh`.
- **`make qa-iter-58-gates`** — single target runs both new challenges
  + the full shared test suite.

## Distributed artifacts (this release)

| Platform | Variant | Filename | SHA-256 |
|----------|---------|----------|---------|
| Android | Release | Yole-Android-1.2.0-Release-0.0.0.1.20.apk | f151c5dd40a0ed4d236f75ddd63a6bdbfa65d27710bfbc846c9f045d113545ea |
| Android | Debug | Yole-Android-1.2.0-Debug-0.0.0.1.20.apk | e9b15671ce6d512cc27834416fda29a094b28f39bffb670bd2afab03bea44e39 |
| Desktop macOS-arm64 | Release | Yole-Desktop-macos-arm64-1.2.0-Release-0.0.0.1.20.dmg | 0c1664ec9dd193201bda93c06643a8adfaf23514fdc067551c0a7aa3a9882b31 |

## Firebase distribution URLs

### Android Release (`0ce4sfjis5h9g`)
- Console: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android/releases/0ce4sfjis5h9g
- Tester share: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:d61715a0a84a42c65d2889/releases/0ce4sfjis5h9g

### Android Debug (`2hgp57k5g8afg`)
- Console: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android/releases/2hgp57k5g8afg
- Tester share: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:d61715a0a84a42c65d2889/releases/2hgp57k5g8afg

## Cross-platform impact summary (CONST-037)

- **Android:** comment toggle, auto-indent, bracket-pair auto-close
  active for all 55 languages. Outline + fold + syntax highlighting
  active for Markdown only. 46 other languages awaiting NDK bulk-build.
- **Desktop macOS-arm64:** full feature set — all 5 affordances +
  syntax highlighting for 47/55 languages. 8-lang gap set (no bonede
  artifact or broken artifact) honestly limited to non-grammar
  affordances.
- **Desktop Linux-x64 / Windows-x64:** BLOCKED — Compose Desktop only
  packages for the current host OS (carry-over from iter-54 / iter-57).
- **Web Wasm:** BLOCKED — `:webApp:wasmJsBrowserDistribution` task not
  wired (carry-over from iter-54 / iter-57). Three non-grammar
  affordances available at runtime for all 55 languages once
  distribution channel is opened.
- **iOS:** BLOCKED — `#phase-7-blocked-on-ios-baseline` +
  `#shared-iosmain-databasefactory-broken` from iter-57 still unresolved.

## Known limitations (carry-over)

- **`#f2-phase-7-android-ndk-bulk-build-pending`** — 47 bonede grammars
  Desktop-only. Android APK ships only markdown native grammar (other
  54 languages graceful-fallback on tokenization).
- **`#f2-phase-7-no-bonede-artifact`** — 7 languages (jsx, xml, vim,
  less, crystal, groovy, bibtex) have no published Maven Central
  bonede artifact. Non-grammar affordances still work via
  `LanguageMetadata` data.
- **`#f2-phase-7-nim-grammar-broken`** — Nim bonede native `.so`
  segfaults on parse; in `BonedeGrammarRegistry.unsupportedLangs`.
- **`#f2-phase-7-ios-xcode-required`** — iOS build scaffold complete
  but `xcrun --sdk iphoneos` fails on build host (CLI Tools only).
- **`#crossbuild-windows-image-provisioning`** — Windows desktop
  builds blocked on Wine container image.
- **`#wasmjs-production-distribution-gap`** — Web Wasm production
  distribution task not wired.

## Anti-bluff anchor

Every claim in this document carries positive evidence:
- Artifact hashes: `docs/qa/iter-58/artifact-hashes.txt`
- Android NDK contents: `docs/qa/iter-58/android-ndk-inventory.txt`
- Build logs: `docs/qa/iter-58/build-android-release.txt`,
  `build-android-debug.txt`, `build-desktop-macos.txt`
- Firebase distribution logs: `docs/qa/iter-58/firebase-distribution-*.txt`
