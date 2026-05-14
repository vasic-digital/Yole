# Syntax Highlighting — User Guide

> **Audience:** end users of Yole on Android, Desktop (Linux / Windows / macOS), iOS, and Web.

iter-57 introduced syntax highlighting across three surfaces — the editor, the rendered preview pane, and the FILES tab — driven by the VS Code theme JSON format. This guide walks through enabling languages, switching themes, and bringing your own theme files.

---

## 1. What's new in iter-57

| Surface | What changed |
|---|---|
| Editor | The line-number gutter and text body share a single ScrollState; while you type Kotlin/Python/Rust/etc. (once enabled), tokens are colored by the active theme. 80 ms debounce keeps it responsive on long files. |
| Preview | Fenced code blocks inside markdown/asciidoc/etc. previews are colorized using the same theme — `\`\`\`kotlin` blocks get Kotlin coloring. |
| FILES tab | Each file row now shows a small 2-letter language badge tinted by the active theme. Disabled formats render no badge. |
| Themes | The entire app — backgrounds, status bar, drawers, dialogs, editor — is driven by a single VS Code theme JSON. The legacy Yole light/dark palettes ship as `Yole-Light.json` / `Yole-Dark.json` with **byte-exact** parity. |

---

## 2. Default state after upgrade

On first launch after upgrading to iter-57:

1. **Markdown is the only enabled format.** Everything else (AsciiDoc, csv, json, Kotlin, Python, JS, …) is opt-in. This is a deliberate operator constraint — most users don't need all 30+ formats and the smaller working set keeps the editor responsive.
2. A **mandatory one-time dialog** appears: *"Yole now enables only Markdown by default. Keep your previous formats enabled, or adopt the new default?"* Pick one — the choice is saved and never asked again.
3. **The active theme is unchanged** — Yole-Dark or Yole-Light, whichever you had before. The pixel-parity test guarantees the same RGB values you saw pre-upgrade.

---

## 3. Enabling additional formats

**Settings → Formats**. The screen groups formats into three sections:

- **Default (always on):** Markdown. Not toggleable.
- **Text formats:** AsciiDoc, reStructuredText, Org Mode, CSV, JSON, LaTeX, etc.
- **Programming languages (v1):** Kotlin, Java, Python, JavaScript, TypeScript, Go, Rust, C, C++, HTML, CSS, SQL.

Each row shows the format's file extensions and (where applicable) the estimated grammar download size. Tapping the switch:
- **Enables** the format immediately — files with the corresponding extension will now open as that type and receive highlighting.
- **Disables** the format — files of that type open as plaintext until you re-enable.

> **Why opt-in?** Each grammar adds memory + (on platforms that bundle native libraries) APK/IPA size. Defaulting to markdown only keeps the install lean. You enable what you actually use.

---

## 4. Switching themes

**Settings → Theme**. The dropdown lists every theme bundled with Yole plus any user themes you've installed.

Bundled themes (all valid VS Code theme JSON):
- **Yole Light** — the historical Yole light palette, pixel-parity to pre-iter-57.
- **Yole Dark** — the historical Yole dark palette, pixel-parity.

Selecting a theme:
- App background, surface, accent, status bar, drawer, dialog, editor — all repaint immediately.
- Syntax token colors in the editor and preview update on the next tokenize (≤80 ms after selection).
- FILES tab badges retint immediately.

---

## 5. Bringing your own theme

Yole reads **standard VS Code theme JSON files** (`*.json` or `*.color-theme.json`). To install a custom theme:

1. Download or write a VS Code theme JSON. Marketplace themes work — strip the `.vsix` package and locate `themes/*.json` inside.
2. Drop the file into Yole's theme directory:
   - **Android:** `/sdcard/Yole/themes/` (or your app data directory's `themes/` subfolder).
   - **Desktop (Linux):** `~/.local/share/Yole/themes/`.
   - **Desktop (Windows):** `%APPDATA%\Yole\themes\`.
   - **Desktop (macOS):** `~/Library/Application Support/Yole/themes/`.
   - **iOS:** Files app → On My iPad/iPhone → Yole → themes/ (when supported by your iOS version).
   - **Web:** drag-and-drop or use the upload dialog at Settings → Themes → Add.
3. The theme appears in the Settings → Theme dropdown on next app start (or pull-to-refresh on the dropdown).

### What if my theme uses unsupported keys?

Yole supports the standard VS Code `colors.*` keys (35 of them used by Yole — see `docs/features/syntax-highlighting/theme-migration-guide.md` for the full mapping table) and the entire `tokenColors[]` array. Keys Yole doesn't know are silently ignored. Missing keys fall back to a reasonable default (usually the equivalent in `Yole-Dark.json` / `Yole-Light.json` depending on the theme's `type`).

### Yole-specific extensions

If your theme defines colors under the `yole.*` private key space (e.g., `yole.bottomNav.iconActive`, `yole.preview.codeBlockBorder`), Yole uses them directly. Otherwise it falls back to the standard VS Code key (e.g., `activityBar.foreground`). Themes that don't define `yole.*` work fine.

---

## 6. Troubleshooting

**The editor is plain — no colors appear.**
- Verify the file's language is enabled in Settings → Formats.
- Check the status bar — if it says "Highlighting unavailable on this device", the platform's tokenizer engine failed to load. On Android this currently means the Tree-Sitter NDK shared library isn't bundled for your device's ABI (tracked as `#android-tree-sitter-ndk-so-missing` in `docs/KNOWN_DEFECTS.md`). Highlighting will work on Desktop and Web today; full Android support requires the operator's NDK build.

**A custom theme's colors look wrong.**
- VS Code uses `#RRGGBB` hex; Yole also accepts `#RRGGBBAA`. Any other format is silently ignored. Validate with a JSON formatter first.
- Per-token coloring uses TextMate scope conventions. The mapping from Tree-Sitter's scope names to VS Code's is documented in `docs/features/syntax-highlighting/architecture.md` §3 (ScopeMapper).

**The badge in the FILES tab is the wrong color.**
- Per-language badge tints are defined under `colors["badge.background.<langId>"]` (e.g., `badge.background.markdown`). Without per-lang entries the theme's generic `badge.background` is used.
- If no badge appears at all for a file you expect to be highlighted, check Settings → Formats — the format may be disabled.

**The preview pane shows un-styled code blocks.**
- Fenced code blocks must use the standard markdown convention `\`\`\`<langId>` (e.g., `\`\`\`kotlin`). Untagged code blocks (` ``` ` with no language) are intentionally rendered plain.
- If the code block IS tagged but still uncolored, the lang's format may be disabled (toggle in Settings → Formats) or the lang's grammar isn't bundled in your version of Yole (markdown is the only bundled grammar in iter-57 v1 — other grammars are CDN-fetched on enable per spec §3.7).

---

## 7. Privacy

Theme JSON files are read locally — never uploaded to any server. Grammar files (when fetched on enable) come from a public CDN; the URL is logged in your app's diagnostics if you have logging enabled. No telemetry about which themes or formats you use is collected.

---

## 8. Cross-reference

- **Architecture deep-dive:** `docs/features/syntax-highlighting/architecture.md`.
- **Theme schema mapping:** `docs/features/syntax-highlighting/theme-migration-guide.md`.
- **Settings → Formats walkthrough:** `docs/features/syntax-highlighting/settings-formats-guide.md`.
- **iter-57 forensic anchor:** `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` + `docs/superpowers/plans/2026-05-14-syntax-highlighting-plan.md` + `docs/features/syntax-highlighting/research-report.md`.
