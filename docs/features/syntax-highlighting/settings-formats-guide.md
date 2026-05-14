# Settings → Formats — Walkthrough

> **Audience:** end users.

iter-57 introduces a `Settings → Formats` screen that lets you control which file formats and programming languages Yole knows how to render with full parsing + syntax highlighting. **Markdown is the only default-enabled format**; everything else is opt-in.

---

## 1. Why opt-in?

Yole supports 30+ formats out of the box. Most users only need a small subset. Defaulting to Markdown-only keeps:

- **Initial install lean.** Grammars (50+ language tokenizers) are CDN-fetched lazily when you enable them, not bundled in the base APK/IPA.
- **Memory footprint small.** Each loaded grammar holds parser state. A first-launch session with one grammar uses materially less RAM than one with 50.
- **Settings searchable.** The full list of formats fits on one scroll.

When you decide a workflow needs another format (e.g., you start editing CSV files daily), you toggle it on — and that's the only step.

---

## 2. Reaching the screen

1. Open Yole.
2. Tap **More** (bottom-nav) → **Settings** → **Formats**.

On Desktop and Web, the path mirrors mobile: top-level menu → Settings → Formats.

---

## 3. Layout

The screen groups formats into three sections:

### 3.1 Default (always on)

A single row: **Markdown**. The switch is greyed out — Markdown cannot be disabled. This is a hard system constraint (every Yole user's first interaction is with `.md` files).

### 3.2 Text formats (toggle)

The 17 non-markdown text formats Yole's parsers can handle:

- AsciiDoc, reStructuredText, Org Mode, TiddlyWiki, Creole, MediaWiki Wikitext, Textile
- CSV (with delimiter auto-detect)
- JSON (with pretty-printing + token-class HTML rendering)
- LaTeX, R Markdown
- TodoTxt, TaskPaper
- Key-Value (`.properties`, `.env` style)
- Jupyter Notebook (`.ipynb`)
- Plain text (always available; toggle controls whether `.txt` files are recognized as a first-class type vs falling through)

Each row shows:
- The format's display name.
- The file extensions it claims (e.g., `.md`, `.markdown`, `.mdown`, `.mkd`).
- A short one-line description.
- The toggle switch (off = grey, on = green/theme-accent).

### 3.3 Programming languages (toggle)

The 12 v1 source-code languages:

- Kotlin, Java, Python, JavaScript, TypeScript, Go, Rust, C, C++, HTML, CSS, SQL

Each row:
- Language name + file extensions.
- Estimated grammar download size (currently small placeholder; on enable Yole fetches the actual grammar file).
- Toggle switch.

> The broader 50+ language set (per the spec) lands in Feature 2 of the 5-feature initiative — see `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md`. iter-57's v1 ships 12.

---

## 4. Toggling a format

Tap the switch on any toggleable row. Effects:

1. **Settings persistence.** Your choice is saved to local app storage (`enabledFormatIds` key). On next app launch the gate restores from this set.
2. **EnabledFormatGate update.** The in-memory `EnabledFormatGate` reflects the new set immediately (within one frame).
3. **Open files re-evaluate.** If you have a `.kt` file open and you enable Kotlin, the file becomes treated as Kotlin source — the editor begins highlighting, the preview pane (if relevant) starts colorizing fenced kotlin code blocks, and the FILES tab gets a kotlin badge for it.
4. **Markdown's switch is uninteractive.** Tapping it does nothing (the toggle is `enabled = false` in Compose terms).

---

## 5. The one-time migration dialog

On first launch after upgrading from a pre-iter-57 build that had ALL formats enabled, Yole displays a mandatory dialog:

> **"Format defaults changed"**
>
> Yole now enables only Markdown by default. Keep your previous formats enabled, or adopt the new default?
>
> — *Keep mine* / *Use new default*

The choice is:
- **Keep mine** — your prior `enabledFormatIds` set is preserved (could be all formats); the migration flag is set so the dialog never asks again.
- **Use new default** — `enabledFormatIds` is set to `{markdown}`; you can later enable specific formats from Settings → Formats.

The dialog cannot be dismissed without choosing (back-press + tap-outside both no-op). This is intentional — the change is significant enough that silent acceptance would surprise users.

---

## 6. What happens when you open a file whose format is disabled?

Yole opens it as plaintext:
- No syntax highlighting in the editor.
- No fenced code-block coloring in preview.
- A small banner appears at the top of the editor: *"`*.kt` is Kotlin source. Enable Kotlin in Settings → Formats to see highlighting?"* with a single "Enable" button.
- The file is fully editable; you just don't get format-specific treatment.

This is by design — Yole won't surprise you by silently doing format-specific things on a file you didn't expect to.

---

## 7. Programmatic API

For contributors writing tests:

```kotlin
EnabledFormatGate.setEnabled(setOf("markdown", "kotlin"))
assertTrue(EnabledFormatGate.isEnabled("kotlin"))
EnabledFormatGate.requireEnabled("kotlin")  // throws if not enabled

FormatRegistry.setFormatEnabled("python")    // also mirrors into the gate
FormatRegistry.setEnabledFormatIds(setOf("markdown", "asciidoc"))  // bulk
```

The gate is the runtime source of truth; `FormatRegistry`'s setters propagate into it. `EnabledFormatGate.enabled: StateFlow<Set<String>>` allows reactive observation in Composables.

---

## 8. Default value reasoning

The default (`setOf("markdown")`) is enforced by `FormatRegistry.defaultEnabledFormatIds(): Set<String> = setOf(ID_MARKDOWN)`. Changing this default to anything broader breaks `format_enablement_default_challenge.sh` (a `make qa-all` gate) AND the unit-test `FormatEnablementDefaultTest.freshDefaultIsMarkdownOnly`. The two-layer enforcement reflects the operator constraint added during iter-57 spec brainstorming.

---

## 9. Cross-reference

- `docs/features/syntax-highlighting/user-guide.md`
- `docs/features/syntax-highlighting/architecture.md` §3.6
- `docs/superpowers/specs/2026-05-14-syntax-highlighting-design.md` §3.7
- `shared/src/commonMain/kotlin/digital/vasic/yole/syntax/EnabledFormatGate.kt`
- `androidApp/src/main/java/digital/vasic/yole/android/ui/settings/FormatsSettingsScreen.kt`
- `androidApp/src/main/java/digital/vasic/yole/android/ui/settings/FormatMigrationDialog.kt`
