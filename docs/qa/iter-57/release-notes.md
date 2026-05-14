# Yole 1.1.0 — Release Notes (iter-57)

**Release date:** 2026-05-14
**Version:** 1.1.0 (versionCode 110, dotted 0.0.0.1.10)
**Headline feature:** Syntax highlighting + unified VS Code theme system

## What's new

### Syntax highlighting
- Editor highlighting in `SyncedScrollEditor` via debounced (80 ms) `VisualTransformation`.
- Preview code-block highlighting for fenced blocks (e.g. ```` ```kotlin ````).
- Filename badges in the FILES tab — 2-letter language chips colored by theme.

### Unified theme system
- VS Code theme JSON is now the single source of truth for app colors.
- `Yole-Light.json` + `Yole-Dark.json` shipped with byte-exact parity to the legacy palette.
- Legacy `IdeTheme.kt` + `YoleColors.kt` removed; 463 callsites migrated.

### Format enablement
- Markdown is the only default-enabled format.
- 17 prior formats + 12 new source-code languages are opt-in via `Settings → Formats`.
- One-time migration dialog on upgrade.

### Tokenizer engines
- Tree-Sitter via JNI on Android + Desktop (bonede `tree-sitter:0.22.6`).
- vscode-textmate via Kotlin/Wasm `@JsModule` interop on Web.

### Quality
- 4 new anti-bluff challenges added to `make qa-all`.
- Documentation expanded (user guide, architecture, migration guide).

## Distributed artifacts (this release)

| Platform | Variant | Filename | SHA-256 |
|----------|---------|----------|---------|
| Android | Release | Yole-Android-1.1.0-Release-0.0.0.1.10.apk | d08d8205ad3562176909e721147c23249bebbdecb9b3ba2c656b289d17df4935 |
| Android | Debug | Yole-Android-1.1.0-Debug-0.0.0.1.10.apk | 6f6d0afffc84142834b12bcf161dde306801a6bca56a1145e1351a65ade101c0 |
| Desktop macOS-arm64 | Release | Yole-Desktop-macos-arm64-1.1.0-Release-0.0.0.1.10.dmg | aa65523c86d8ceaa67de0552330c09646355cbe5fb61cf41fdefe9a16315e962 |

## Known limitations (carry-over)

- **`#android-tree-sitter-ndk-so-missing`** — RESOLVED in build 110 (NDK .so now bundled).
- **`#phase-7-blocked-on-ios-baseline`** — iOS deferred to v1.2 pending Document-KMP sibling fix.
- **`#wasmjs-test-baseline-broken`** — Wasm test infra baseline gap (does not affect runtime).
- **Desktop Linux-x64 / Windows-x64** — Compose Desktop only packages for the current host OS;
  Linux + Windows builds require their respective hosts (or `Containers/` cross-build, see
  `#crossbuild-windows-image-provisioning`). Not blocking macOS release.
- **Web Wasm** — `:webApp:wasmJsBrowserDistribution` task not wired (pre-existing webApp
  module config gap, also recorded in iter-54 closeout). Pre-existing infra defect; not
  introduced in iter-57.

## Anti-bluff anchor

Every claim in this document carries positive evidence:
- Artifact hashes: `docs/qa/iter-57/artifact-hashes.txt`
- Android NDK contents: `docs/qa/iter-57/android-ndk-verify.txt`
- Build logs: `docs/qa/iter-57/build-android.log`, `build-desktop-macos.log`,
  `build-web-wasm.log`
- Firebase distribution logs: `docs/qa/iter-57/firebase-distribution-*.log`
