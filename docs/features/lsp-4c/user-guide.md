<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Refactoring Capabilities — User Guide (iter-63, v1.6.0)

> **Feature 4c of 5** — closes the LSP arc opened in iter-61 (LSP hosting) and
> expanded in iter-62 (diagnostics + hover + go-to-definition).

---

## Overview

iter-63 ships five new language-server capabilities on top of the existing
LSP integration:

| # | Capability | Android | Desktop | iOS | Web |
|---|-----------|---------|---------|-----|-----|
| 1 | **Rename** | Toolbar button + F2 | F2 key | stub | stub |
| 2 | **Code Actions** | Lightbulb gutter icon | lightbulb visible | stub | stub |
| 3 | **Signature Help** | Inline pill above cursor | popup (deferred — see §7.3) | stub | stub |
| 4 | **Formatting** | Ctrl+Shift+F / on-save | Ctrl+Shift+F | stub | stub |
| 5 | **Find References** | Toolbar button + Shift+F12 | Shift+F12 | stub | stub |

iOS and Web stubs return `null`/empty lists — the capabilities compile and the
UI gracefully shows nothing rather than crashing.

---

## 1 — Rename

### What it does

Sends a `textDocument/rename` request to the language server for the symbol
under the cursor and presents a multi-file diff preview before applying.

### How to invoke

| Platform | Method |
|----------|--------|
| Android | Tap **Rename** toolbar button (only visible when langId is active) |
| Android | Press **F2** on a physical or Bluetooth keyboard |
| Desktop | Press **F2** |

### Workflow

1. A text field dialog prompts for the new name.
2. On confirm, `LspRenameRequester.rename(langId, uri, line, char, newName)` is
   called with a 2 s timeout.
3. If the server returns a `WorkspaceEdit`, the **Rename Preview Panel** opens
   as a modal bottom sheet (Android) / `Window` (Desktop future).
4. The panel lists every affected file as a collapsible row showing the
   filename and edit count. Expanding a row shows a before→after diff for each
   `TextEdit`.
5. Tap **Apply** to invoke `WorkspaceEditApplier.apply(edit, sources)` and
   persist the changes. Tap **Cancel** to dismiss without changes.
6. If the server returns null (no rename target, timeout, or disconnected), a
   toast is shown.

### Known gaps / deferred trackers

- **`#iter-63-longpress-gesture-detector`** — Rename is currently accessible
  only through the toolbar button and F2 key. A full long-press context menu on
  `BasicTextField` was designed but deferred; the `Indication` API interaction
  with Compose selection handles is complex and requires a dedicated iteration.
  The toolbar surface covers the common mobile touch case.

---

## 2 — Code Actions

### What it does

Polls the language server every 500 ms for available code actions on visible
lines (quick-fixes, refactors, source actions). A lightbulb icon appears in the
third gutter column when actions are available.

### Gutter column order

```
[diagnostic-dot]  [lightbulb]  [fold-chevron]
```

### How to invoke

1. Position the cursor on a line that has at least one code action.
2. An amber lightbulb icon appears in the second gutter column.
3. Tap (Android) or click (Desktop, future) the lightbulb.
4. A dropdown menu lists all available actions by `title`.
5. Select an action:
   - If the action carries a `WorkspaceEdit` it is applied immediately via
     `WorkspaceEditApplier` and the editor reloads.
   - If the action carries a `command` it is forwarded to the server's
     `workspace/executeCommand` endpoint.

### Known gaps / deferred trackers

None beyond desktop click wiring (same as §1's desktop gap above).

---

## 3 — Signature Help

### What it does

When you type `(` or `,` inside a function call, Yole asks the language server
for parameter signatures and highlights the active parameter.

### Per-platform UX

| Platform | Surface | Dismissed by |
|----------|---------|--------------|
| Android | Small **pill chip** above the cursor line; active parameter **bold** | `)` keystroke or cursor leaves parentheses |
| Desktop | **Popup tooltip** anchored near cursor; expands to show full documentation | `)` keystroke or 30 s auto-dismiss |

### Known gaps / deferred trackers

- **`#iter-63-desktop-signature-help-popup-deferred`** — The `SignatureHelpPopup`
  Composable and `SignatureHelpTrigger` are compiled and tested, but are not yet
  wired into the Desktop `IdeEditorScreen`. Desktop users will not see signature
  help until a follow-up wiring iteration.
- **`#iter-63-server-trigger-chars-hardcoded`** — The LSP specification allows
  each server to advertise its own trigger characters via
  `serverCapabilities.signatureHelpProvider.triggerCharacters`. iter-63 hardcodes
  `{';', '}', '\n'}` for on-type formatting trigger chars and `(` / `,` for
  signature-help trigger chars. A future iteration will query
  `serverCapabilities` at startup and derive the character sets dynamically.

---

## 4 — Formatting

### What it does

Sends a `textDocument/formatting` request to the language server and applies
the returned list of `TextEdit` patches to the document.

### Trigger modes

| Mode | How to invoke | Applies when |
|------|--------------|--------------|
| On-save | Save the file | Always (Settings toggle pending — see below) |
| Explicit | **Ctrl+Shift+F** | Always, bypasses toggle |
| On-type | Type `;`, `}`, or `\n` | Always (trigger chars hardcoded) |

### Known gaps / deferred trackers

- **`#iter-63-format-on-save-settings-toggle`** — The `FormattingTrigger` passes
  `settings = { false }` unconditionally (format-on-save is effectively always
  enabled). A dedicated Settings screen toggle (`formatOnSave`, default true)
  was designed but deferred to keep the Settings screen stable while the
  infrastructure is validated.
- **`#iter-63-on-type-edit-apply`** — When on-type formatting fires, the
  `List<TextEdit>` is obtained from the server but **not yet applied** to the
  editor buffer. The round-trip to the server is tested; the final buffer-update
  step requires integrating with `BasicTextField`'s mutable `TextFieldState`,
  which has API constraints. Deferred to the next LSP iteration.
- **`#iter-63-explicit-format-edit-apply`** — Same as above for the
  Ctrl+Shift+F explicit path. The server call succeeds; buffer patching deferred.

---

## 5 — Find References

### What it does

Sends a `textDocument/references` request and opens a persistent **References
Panel** (bottom drawer, ~200 dp) listing all locations where the symbol is used.

### How to invoke

| Platform | Method |
|----------|--------|
| Android | Tap **Refs** toolbar button (langId-gated) |
| Android | Press **Shift+F12** on a physical keyboard |
| Desktop | Press **Shift+F12** |

### References Panel

- Rows show: **filename**, **1-based line number**, **context-line preview**.
- Tap a row to push the current position onto `EditorNavigationStack` and
  open the referenced file at the target line.
- The panel persists across navigations; tap the close chevron to dismiss.
- Mirrors the iter-62 `DiagnosticsProblemsPanel` layout and interaction model.

### Known gaps / deferred trackers

- Same desktop wiring gap as §1 / §3: desktop UI not yet wired
  (`#iter-63-desktop-signature-help-popup-deferred` covers the full desktop
  editor wiring package).

---

## Language server support

All five capabilities are available for any language server registered via
`LspServerRegistry` that advertises the corresponding capability in its
`ServerCapabilities` response. Capabilities absent from the server's declaration
degrade gracefully (timeout → null/empty → no UI shown).

Language servers shipped with Yole v1.6.0 are the same set as v1.5.0 (15
servers). Rename, code actions, formatting, and references availability per
server depends on the server's own implementation maturity — not all servers
support all five capabilities.

---

## Keyboard shortcuts (Android + Desktop)

| Action | Shortcut |
|--------|---------|
| Rename | F2 |
| Explicit format | Ctrl+Shift+F |
| Find references | Shift+F12 |
| Hover (existing, iter-62) | F1 |
| Go to definition (existing, iter-62) | Ctrl+Click / Ctrl+F12 |

---

## Data flow summary

```
User gesture / keystroke
        │
        ▼
IdeEditorScreen (YoleApp.kt)
        │
        ├─► LspXxxRequester (adapter interface, testable)
        │         │
        │         ▼
        │   LspServerHost.xxx() — suspend, withTimeout
        │         │
        │         ▼
        │   LSP4J LanguageServer.textDocumentService.xxx()
        │         │
        │         ▼
        │   mapLspXxx() helper (LSP4J → Yole types)
        │
        ├─► WorkspaceEditApplier (rename / code action edit path)
        │
        ├─► ReferencesPanel / RenamePreviewPanel / SignatureHelpPill (UI)
        │
        └─► EditorNavigationStack (references navigation)
```
