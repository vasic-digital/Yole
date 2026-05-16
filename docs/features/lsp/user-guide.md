<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LSP Integration — User Guide

> **Audience:** end users of Yole on Android, Desktop (Linux / Windows / macOS), iOS, and Web.

iter-61 adds Language Server Protocol (LSP) support as the fourth completion provider in Yole's auto-complete pipeline. When you open a supported source file, Yole spawns the matching language server in the background and feeds its type-aware completions into the same popup you already know from iter-60.

---

## 1. What LSP integration does

LSP integration launches a language-specific server process (e.g., `rust-analyzer`, `gopls`, `clangd`) the first time you open a matching file, then streams type-aware, project-aware completions into the existing auto-complete popup alongside word-frequency and snippet results. On Desktop macOS-arm64 this happens automatically — no configuration required.

---

## 2. How to trigger it

LSP completions surface through the same trigger mechanism introduced in iter-60. No new trigger is needed.

### Desktop and Web

- **Implicit:** type ≥ 2 characters; the popup appears after an 80 ms debounce. LSP items appear alongside token and snippet results.
- **Explicit:** press **Ctrl+Space** at any time to force the popup open regardless of prefix length.

### Android (mobile)

- **Implicit:** same 2-character threshold and 80 ms debounce.
- **Explicit:** tap the **"Suggest"** toolbar button (wrench icon) in the editor toolbar.

> **Android v1 note:** LSP completions are not available on Android in v1. The Settings → Language Servers screen opens and shows the v1 disclaimer ("Not available on Android (v1)") for all 15 servers. JVM-bundled servers (jdtls, kotlin-language-server) and Node-bundled servers (pyright, typescript-language-server, bash-language-server, yaml-language-server) will unlock in a follow-up phase when their runtime DFMs land. Token-frequency, snippet, and identifier completions continue to work on Android as before.

### iOS

LSP is hard-blocked on iOS by App Store Review Guideline 2.5.2, which prohibits apps from running downloaded executable code. `LspCompletionProvider.ios.kt` returns an empty list. This block is indefinite and cannot be worked around without a fundamental platform change. See [App Store Review Guidelines §2.5.2](https://developer.apple.com/app-store/review/guidelines/#2.5.2).

### Web (Wasm)

Native LSP binaries cannot run inside a browser sandbox. LSP is deferred on Web; the provider returns an empty list.

---

## 3. Per-platform availability at a glance

| Platform | LSP status | Notes |
|---|---|---|
| **Desktop macOS-arm64** | Shipped — 8 servers bundled | rust-analyzer, clangd (C+C++), marksman, lua-language-server, zls, haskell-language-server, jdtls, kotlin-language-server |
| **Desktop Linux / Windows** | Code ready, distribution pending | Gated on `#crossbuild-linux-windows-infra` |
| **Android** | Settings UX ships honest "v1" stub | Native binaries pending `#crossbuild-android-ndk-lsp`; JVM/Node DFMs planned follow-up |
| **iOS** | Hard-blocked (App Store §2.5.2) | Returns empty list |
| **Web (Wasm)** | Deferred | Returns empty list |

---

## 4. Cold-start expectations

Language servers start lazily on the first file open. Subsequent completions in the same project reuse the running server with no delay.

| Server | Language(s) | Typical cold-start |
|---|---|---|
| `marksman` | Markdown | < 1 s |
| `gopls` | Go | 2–5 s |
| `rust-analyzer` | Rust | 5–30 s (first `.rs` open in a new workspace; builds index) |
| `jdtls` | Java | 20+ s (downloads ~150 MB of build dependencies on first project open — see Known gaps §5) |
| `clangd` | C, C++ | 3–10 s (background index after first open) |
| `lua-language-server` | Lua | 1–3 s |
| `zls` | Zig | 2–5 s |
| `haskell-language-server` | Haskell | 20–60 s (first open in a Cabal/Stack project) |
| `kotlin-language-server` | Kotlin | 5–15 s |

Servers shut down automatically after 5 minutes of idle time and restart transparently on the next file open.

---

## 5. Known gaps

The following limitations exist in v1 and are tracked in `docs/KNOWN_DEFECTS.md`:

- **`#iter-61-jdtls-project-build-deps-online`** — jdtls downloads ~150 MB of Maven/Gradle build dependencies on the first project open. This requires an internet connection and blocks completion until the download completes. A bundled-dependencies mode is planned for a follow-up.
- **Workspace root auto-detection only** — Yole walks up the directory tree from the open file looking for a project marker (e.g., `Cargo.toml`, `go.mod`, `tsconfig.json`). There is no "open as workspace" dialog in v1. If the project marker is not found within 20 parent directories, LSP falls back to the file's directory.
- **Diagnostics, hover, go-to-definition not in v1** — LSP-driven inline diagnostics, hover tooltips, and go-to-definition are planned for iter-62 (Feature 4b). v1 exposes completion only.
- **`#crossbuild-android-ndk-lsp`** — Android native LSP binaries not yet cross-compiled.
- **`#crossbuild-linux-windows-infra`** — Desktop Linux/Windows binaries not yet distributed.
- **`#wasmjs-production-distribution-gap`** — Web Wasm LSP deferred.
- **iOS hard-block** — App Store §2.5.2 prohibits subprocess execution.
