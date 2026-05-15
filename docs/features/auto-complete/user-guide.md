<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Auto-Complete — User Guide

> **Audience:** end users of Yole on Android, Desktop (Linux / Windows / macOS), iOS, and Web.

iter-60 introduces auto-complete for source-code and markup files. As you type, Yole suggests words from the current document, identifiers extracted by the outline engine, and language-specific snippets — all ranked by relevance.

---

## 1. What auto-complete does

Auto-complete watches your cursor position and, when you have typed at least 2 characters, offers a ranked list of candidate completions: words already in the document (ranked by frequency), identifiers from the file's outline (functions, classes, variables), and language-specific snippet templates. Accepting a snippet expands it in-place and positions your cursor at the first editable placeholder.

---

## 2. How to trigger it

### Desktop and Web

- **Implicit:** after typing 2 or more characters, the popup appears automatically (80 ms debounce).
- **Explicit:** press **Ctrl+Space** at any time to force the popup open regardless of prefix length.

### Android (mobile)

- **Implicit:** same 2-character threshold and 80 ms debounce as Desktop.
- **Explicit:** tap the **"Suggest"** toolbar button (wrench icon, next to the outline button in the editor toolbar).

### iOS

Auto-complete is deferred on iOS. The completion engine and snippet bundles compile for iOS (`commonMain`), but the popup UI and toolbar button require the syntax-highlighting infrastructure to stabilise first. Tracked in `docs/KNOWN_DEFECTS.md` under `#shared-iosmain-databasefactory-broken` (same blocker as iter-57 and iter-58 iOS work).

---

## 3. Popup navigation

### Desktop

| Key | Action |
|---|---|
| **↑ / ↓** | Move selection up / down |
| **Enter** or **Tab** | Accept selected item |
| **Esc** | Dismiss popup |

### Mobile (Android)

- **Tap an item** to accept it.
- **Tap outside the popup** (anywhere in the editor) to dismiss.

---

## 4. Snippets

### Bundled languages

55 language bundles ship with Yole v1.3.0. See [snippet-coverage-matrix.md](snippet-coverage-matrix.md) for the full list and per-language counts.

Snippet items are shown with a `{}` badge in the popup. They are only offered when the file's language is recognised (i.e., the extension maps to a LanguageMetadata entry).

### Placeholder navigation

Snippets follow the VS Code snippet syntax. After accepting a snippet, Yole expands it and selects the first `${N:placeholder}` field so you can type immediately.

| Key | Action |
|---|---|
| **Tab** | Jump to the next placeholder |
| **Esc** | Exit placeholder navigation; cursor stays where it is |

The final `$0` marker is the cursor's resting position after all placeholders have been filled.

Example — accepting the `fn` snippet in a Kotlin file:

```
fun ${1:name}(${2:params}): ${3:Unit} {
    $0
}
```

Tab moves: `name` → `params` → `Unit` → body position.

### User-extensible snippets (v2)

In v1 the snippet bundles are read-only. User-defined snippet directories are not yet wired. This is tracked in the design spec §11 and planned for v2 / Feature 4.

---

## 5. Languages currently supported

See [snippet-coverage-matrix.md](snippet-coverage-matrix.md) for the complete table of all 55 bundled languages, their snippet counts, and ranker disposition.

---

## 6. Known limitations

The following gaps exist in v1 and are tracked in `docs/KNOWN_DEFECTS.md`:

- **Popup positioning** — the popup anchors to the bottom-left of the editor surface rather than directly below the cursor. Cursor-rect positioning via `TextLayoutResult` was deferred because it would require exposing `TextLayoutResult` outside `SyncedScrollEditor`, risking the iter-57 `VisualTransformation` length-guard. A later phase will wire it properly.
- **Snippet `${N|a,b,c|}` choice lists** — the VS Code choice-list syntax is parsed but the UI renders a plain text field; the `a,b,c` alternatives are ignored in v1.
- **Snippet `$VARIABLE` substitution** — variables (`$TM_FILENAME`, `$CURRENT_DATE`, etc.) are treated as literal text in v1.
- **Desktop popup UI** — the completion engine and snippet pipeline run on Desktop, but the `CompletionPopup` composable is in `androidApp` and not yet ported to `desktopApp`. Desktop users can use the engine programmatically (e.g., via tests); the popup UI ships Desktop in a follow-up.
- **iOS and Web** — as noted above, the popup UI is deferred on both platforms.
- **User snippet directories** — not wired in v1. Deferred to v2 (see §4 above).
