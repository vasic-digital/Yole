# Theme Migration Guide

> **For users:** how to bring pre-iter-57 custom themes forward.
> **For contributors:** the legacy-palette → VS Code key mapping table that powers `LegacyThemeBridge`.

iter-57 replaces Yole's hardcoded `IdeTheme.kt` + `YoleColors.kt` Kotlin palettes with a unified VS Code theme JSON system. This guide documents the mapping so anyone with a pre-iter-57 custom theme (or with knowledge of the legacy color names) can build a working VS Code JSON theme for Yole.

---

## 1. Legacy → VS Code key mapping

The table below records every legacy palette field consumed by Yole's UI and its VS Code JSON equivalent. `LegacyThemeBridge` (in `shared/src/commonMain/.../syntax/theme/LegacyThemeBridge.kt`) is the source of truth — the `LegacyThemeParityTest` verifies the JSON values match the legacy constants byte-for-byte.

| Legacy field | VS Code key (used by Yole) |
|---|---|
| `IdeTheme.lightBackground` | `colors["editor.background"]` (in Light themes) |
| `IdeTheme.darkBackground` | `colors["editor.background"]` (in Dark themes) |
| `IdeTheme.lightText` | `colors["editor.foreground"]` |
| `IdeTheme.darkText` | `colors["editor.foreground"]` |
| `IdeTheme.lightLineNumbers` | `colors["editorLineNumber.foreground"]` |
| `IdeTheme.darkLineNumbers` | `colors["editorLineNumber.foreground"]` |
| `IdeTheme.lightSurface` | `colors["sideBar.background"]` or `colors["editorWidget.background"]` (per usage site) |
| `IdeTheme.darkSurface` | same |
| `IdeTheme.lightBorder` | `colors["editorWidget.border"]` |
| `IdeTheme.darkBorder` | same |
| `YoleColors.Ide.LightMutedText` | `colors["editorLineNumber.foreground"]` (inactive variant) |
| `YoleColors.AccentBlue` | `colors["focusBorder"]` + `colors["badge.background"]` |
| `YoleColors.AccentGreen` | (used in `colors["activityBar.foreground"]` accent contexts) |
| `YoleColors.Dark.SurfacePrimary` | `colors["editorWidget.background"]` |
| `YoleColors.Dark.SurfaceSecondary` | `colors["sideBar.background"]` |

The full mapping has 35 keys (14 from the legacy palette + 21 added during Phase 0 research §4). See `docs/features/syntax-highlighting/research-report.md` §4 for the complete inventory.

---

## 2. Yole-specific extensions

VS Code's standard schema covers most needs. Yole defines four optional extensions under a private `yole.*` key namespace for surfaces the standard schema doesn't address:

| Extension key | Purpose | Fallback if missing |
|---|---|---|
| `yole.bottomNav.iconActive` | Bottom navigation bar active-icon tint | `activityBar.foreground` |
| `yole.preview.codeBlockBorder` | Markdown preview fenced-code border | `editorWidget.background` |
| `yole.statusBar.fileTypeBadge` | The badge tint behind the file-type indicator in the status bar | `badge.background` |
| `yole.tabs.activeBackground` | Active editor tab background | `editor.background` |

A theme that defines NONE of these still works perfectly — it just inherits the VS Code defaults.

---

## 3. Per-language badge tints (Yole-Light + Yole-Dark)

Both bundled themes define language-specific badge tints under `colors["badge.background.<langId>"]`. A custom theme can override or extend these:

```jsonc
{
  "colors": {
    "badge.background": "#888888",
    "badge.background.markdown": "#00AA00",
    "badge.background.kotlin": "#7F52FF",
    "badge.background.python": "#3776AB",
    "badge.background.javascript": "#F7DF1E",
    ...
  }
}
```

When `BadgeTinter.tintFor("README.md", theme)` runs, it looks up `badge.background.markdown` first; if absent, falls back to `badge.background`; if also absent, returns null (no chip rendered).

---

## 4. Worked example — porting a pre-iter-57 user theme

Suppose your pre-iter-57 custom theme was a Kotlin file defining:

```kotlin
val MyDarkBackground = Color(0xFF202020)
val MyDarkText = Color(0xFFE0E0E0)
val MyAccent = Color(0xFF66BB6A)
val MyLineNumbers = Color(0xFF707070)
```

The equivalent VS Code JSON:

```json
{
  "name": "My Dark",
  "type": "dark",
  "colors": {
    "editor.background": "#202020",
    "editor.foreground": "#e0e0e0",
    "editorLineNumber.foreground": "#707070",
    "focusBorder": "#66bb6a",
    "badge.background": "#66bb6a",
    "activityBar.background": "#202020",
    "activityBar.foreground": "#66bb6a",
    "statusBar.background": "#1a1a1a",
    "statusBar.foreground": "#e0e0e0",
    "sideBar.background": "#252525",
    "editorWidget.background": "#252525",
    "editorWidget.border": "#3a3a3a"
  },
  "tokenColors": [
    { "scope": "comment", "settings": { "foreground": "#808080" } },
    { "scope": ["keyword", "storage"], "settings": { "foreground": "#66bb6a" } },
    { "scope": "string", "settings": { "foreground": "#ce9178" } },
    { "scope": "constant", "settings": { "foreground": "#b5cea8" } },
    { "scope": "entity.name.function", "settings": { "foreground": "#dcdcaa" } },
    { "scope": "entity.name.type", "settings": { "foreground": "#4ec9b0" } }
  ]
}
```

Save as `My-Dark.json` and drop into your platform's Yole theme directory (see `user-guide.md` §5). The theme appears in the Settings → Theme dropdown.

---

## 5. Hex format

Both `#RRGGBB` and `#RRGGBBAA` are accepted. Any other format (3-digit shorthand, `rgb()` strings, named colors) is silently ignored — Yole prefers explicit failure to fall-through bluffing. The parser tests cover these edge cases.

---

## 6. tokenColors[].scope reference

Yole respects VS Code's TextMate scope conventions. For each token Tree-Sitter / vscode-textmate produces, Yole runs it through `ScopeMapper.treeSitterToVsCode()` (Tree-Sitter scope names are slightly different from VS Code's). The supported scope categories:

- `comment`, `comment.line`, `comment.block`
- `keyword`, `keyword.control`, `keyword.operator`, `storage`, `storage.type`, `storage.modifier`
- `string`, `string.quoted`, `string.template`, `constant.character.escape`
- `constant`, `constant.numeric`, `constant.language`, `constant.language.boolean`
- `entity.name.function`, `support.function.builtin`, `entity.name.function.macro`
- `entity.name.type`, `support.type.builtin`, `entity.name.class`
- `variable`, `variable.language`, `variable.parameter`
- `punctuation`, `punctuation.section.bracket`, `punctuation.separator`
- `entity.name.tag`, `entity.other.attribute-name`
- Markup-specific (markdown): `markup.heading`, `markup.heading.1`–`6`, `markup.bold`, `markup.italic`, `markup.underline.link`

A theme that defines colors for these scopes will color the corresponding tokens. Scopes Yole doesn't recognize fall through `Theme.tokenColor`'s hierarchical lookup (e.g., `keyword.control.return` falls back to `keyword.control` then `keyword`).

---

## 7. Cross-reference

- `docs/features/syntax-highlighting/user-guide.md`
- `docs/features/syntax-highlighting/architecture.md`
- `docs/features/syntax-highlighting/research-report.md` §4 (full schema enumeration)
- `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/theme/LegacyThemeBridge.kt` (the canonical map)
