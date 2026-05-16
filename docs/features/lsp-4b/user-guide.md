<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LSP Capability Expansion — User Guide

> **Audience:** end users of Yole on Android, Desktop (Linux / Windows / macOS), iOS, and Web.
> Read `docs/features/lsp/user-guide.md` first — this document builds on it.

iter-62 expands the LSP integration shipped in iter-61 with three new capabilities: real-time inline diagnostics, hover documentation popups, and go-to-definition navigation. These surfaces are fully wired on Android against the same 8 staged language servers as iter-61; Desktop UI wiring is deferred (see Known limitations below).

---

## 1. What iter-62 adds

iter-62 introduces inline error highlighting (gutter dots + underlines), a collapsible Problems panel listing all diagnostics for the current file, a hover popup that shows Markdown-formatted documentation for the symbol under the cursor, and go-to-definition navigation that jumps to the declaration of the focused identifier.

---

## 2. Diagnostic surfaces

When the language server publishes diagnostics for the open file, Yole renders them across three complementary surfaces simultaneously.

### 2.1 Gutter dots

A colored dot appears to the left of each line that has at least one diagnostic, next to (not replacing) the fold chevrons. If a line has diagnostics of multiple severities, the dot reflects the highest-severity one.

| Severity | Color |
|---|---|
| Error | Red (light: `#E53935`, dark: `#EF9A9A`) |
| Warning | Amber (light: `#F57C00`, dark: `#FFCC02`) |
| Information | Blue (light: `#1976D2`, dark: `#90CAF9`) |
| Hint | Grey (light: `#757575`, dark: `#B0BEC5`) |

The palette follows VS Code's severity conventions to reduce learning friction.

### 2.2 Inline underlines

Each diagnostic range in the editor text receives a straight underline colored by severity. The underline spans the exact character range reported by the language server (clamped to valid text bounds). Wavy underlines (more typical of error UX) are deferred to a follow-up iteration.

### 2.3 Problems panel

Tap the **"N issues"** badge button in the editor toolbar (where N is the count of diagnostics for the current file) to toggle a collapsible bottom drawer approximately 200 dp tall. The panel shows all diagnostics for the open file sorted by line number, each row displaying the severity icon, the 1-based line number, and the diagnostic message.

Tapping a row currently dismisses the panel. Scrolling the editor to that line is deferred (see `#iter-62-phase-8-problems-scroll-to-line` in Known limitations).

---

## 3. Hover popup

The hover popup displays Markdown-formatted documentation for the identifier at the cursor position, as reported by the language server.

### 3.1 How to trigger it

#### Desktop

- **Mouse dwell (implicit):** Rest the cursor over an identifier for 300 ms. The popup appears automatically.
  - **Note (v1):** The Tree-Sitter identifier filter is stubbed in v1 — hover fires for any position, not just identifiers. Tracked as `#iter-62-phase-8-tree-sitter-hover-filter-stubbed`.
- **Keyboard (explicit):** Press **F1** at the cursor position to request hover immediately, bypassing the 300 ms dwell and the identifier filter.

#### Android (mobile)

- **Long-press → "Show info":** Long-press on the identifier to open the context menu, then tap "Show info".
- **Keyboard (explicit):** If a hardware keyboard is connected, **F1** triggers hover immediately.

### 3.2 Popup content

The popup renders the language server's Markdown hover response. Supported block types:

| Markdown element | Rendered as |
|---|---|
| Paragraph | Body text |
| Heading (levels 1–6) | Bold text at level-appropriate size |
| Fenced code block | Monospace block with language label |
| Indented code block | Monospace block (no language label) |
| Inline code span | Monospace inline |
| Any other block (tables, links, etc.) | FallbackText (raw Markdown string) |

Tables and hyperlinks are not rendered natively in v1 — they fall back to showing the raw Markdown source. This is an honest limitation, not a display error.

### 3.3 Known popup positioning limitation

In v1, the popup is anchored at the upper-left corner of the editor area, not at the pixel position of the cursor. Precise cursor-pixel anchoring requires Layout coordinates that are not yet threaded through from the text field internals. Tracked as `#iter-62-phase-8-hover-precise-anchor`.

---

## 4. Go-to-definition

Go-to-definition requests the language server for the declaration location of the symbol at the cursor and navigates there.

### 4.1 How to trigger it

#### Desktop

- **Keyboard:** Press **F12** to invoke go-to-definition at the cursor position.

#### Android (mobile)

- **Long-press → "Go to definition":** Long-press on the identifier to open the context menu, then tap "Go to definition".

### 4.2 Navigation behavior

| Scenario | Behavior |
|---|---|
| **1 result** | Editor navigates in-place to the definition (cursor moves; file does not change in v1) |
| **N results (> 1)** | A bottom-sheet chooser lists all candidate locations by filename and line. Tap to navigate. |
| **0 results** | A toast notification is shown ("No definition found") |

### 4.3 Back navigation

After navigating to a definition, the device-back gesture (Android) or **Alt+Left** (Desktop) restores the previous cursor position. The navigation stack holds up to 100 entries and suppresses consecutive-duplicate pushes.

**v1 limitation:** back navigation restores the cursor position within the same file only. Cross-file back navigation (returning to the file you navigated *from*) is deferred. Tracked as `#iter-62-phase-8-cross-file-back-nav`.

**v1 limitation for Java:** jdtls returns `jdt://` URIs for standard library definitions (e.g., `java.lang.String`). Yole cannot resolve `jdt://` URIs — a toast is shown instead of navigating. Tracked as `#iter-62-jdt-uri-scheme-unsupported`.

---

## 5. Per-platform availability

| Platform | Diagnostics | Hover | Go-to-definition | Notes |
|---|---|---|---|---|
| **Desktop macOS-arm64** | UI deferred | UI deferred | UI deferred | LSP host connected (iter-61); editor UI wiring deferred `#iter-62-desktop-editor-lsp-wiring` |
| **Desktop Linux / Windows** | UI deferred | UI deferred | UI deferred | Code-ready; distribution gated `#crossbuild-linux-windows-infra` |
| **Android** | Full | Full | Full | Against all 8 staged servers; binary-blocked on NDK binaries |
| **iOS** | N/A | N/A | N/A | Hard-blocked (App Store §2.5.2) |
| **Web Wasm** | Deferred | Deferred | Deferred | Native subprocess not possible in browser sandbox |

> **Android binary note:** the Android UI is fully wired but LSP binaries for Android are not yet cross-compiled (`#crossbuild-android-ndk-lsp`). Diagnostics, hover, and definition data flow once the binary acquisition gap closes — the UI surfaces are ready.

---

## 6. Known limitations

The following are honest limitations in v1.5.0. Each is tracked in `docs/KNOWN_DEFECTS.md`.

- **`#iter-62-phase-8-tree-sitter-hover-filter-stubbed`** — Hover fires for any cursor position, including whitespace and keywords. The intended behavior (hover only over identifiers as classified by the Tree-Sitter AST) is stubbed pending AST position lookup integration.
- **`#iter-62-phase-8-hover-precise-anchor`** — The hover popup appears in the upper-left corner of the editor area, not at the precise cursor position. Pixel-accurate anchoring is deferred.
- **`#iter-62-phase-8-problems-scroll-to-line`** — Tapping a row in the Problems panel dismisses the panel rather than scrolling the editor to that line. Scroll state is intentionally encapsulated inside `SyncedScrollEditor` (iter-55 invariant); a clean threading mechanism is deferred.
- **`#iter-62-phase-8-cross-file-back-nav`** — Back navigation restores position within the same file only. Cross-file back (reopening the originating file) is deferred.
- **`#iter-62-jdt-uri-scheme-unsupported`** — Go-to-definition for Java standard library symbols fails with a toast; `jdt://` URIs returned by jdtls are not resolvable by Yole's file system layer.
- **`#iter-62-desktop-editor-lsp-wiring`** — The Desktop editor (`IdeEditorScreen` on Desktop) does not yet receive diagnostics, hover, or go-to-definition events. The LSP host is connected (iter-61); the UI composables exist; wiring is deferred.
- **`#iter-62-gopls-no-go-toolchain`** — gopls requires a Go toolchain to be installed and on `PATH` at server startup. If no toolchain is found, the server fails to initialize and diagnostics/hover/definition are unavailable for Go files. Installing Go resolves this.
