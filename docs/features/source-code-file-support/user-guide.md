# Source-Code File Support — User Guide

> **Audience:** end users of Yole on Android, Desktop (Linux / Windows / macOS), iOS, and Web.

iter-58 introduces first-class source-code file support across 55 programming languages. When you open a source file in Yole you now get five editor affordances — comment toggling, smart auto-indent, an outline panel, bracket-pair auto-close, and a fold gutter — layered on top of the iter-57 syntax highlighting already in the editor.

---

## 1. What changed in iter-58

| Affordance | What it does |
|---|---|
| **Comment toggle** | Ctrl+/ (Cmd+/ on macOS) comments or uncomments the current line using the correct per-language marker (`//` for Kotlin/Go/C, `#` for Python/Bash, `--` for SQL, `<!-- -->` for HTML/Markdown, etc.). Multi-line selections toggle each line independently. |
| **Smart auto-indent** | Pressing Enter after an indent-opening token (`{`, `:`, `do`, `then`, etc. per the language's rules) inserts a new line at the next indentation level automatically. The correct indent unit is used per language (4 spaces for Kotlin/Java/Python, 2 spaces for JS/TS/HTML, a tab for Go). |
| **Outline panel** | A drawer accessible from the toolbar shows the document's structural symbols — functions, classes, methods, variables, sections — extracted from the file using Tree-Sitter query files. Tap a symbol to jump to it. |
| **Bracket-pair auto-close** | When you type `(`, `[`, `{`, `"`, or `'`, the matching closing character is inserted immediately after the cursor. The cursor lands between the pair, ready to type. |
| **Fold gutter** | Lines at the start of a foldable block (function bodies, class bodies, if-blocks, multi-line comments) show a chevron (`▼`/`▶`) in the left gutter. Tap the chevron to collapse or expand that block. |

---

## 2. Supported languages

### Full Tree-Sitter support on Desktop (47 languages)

The following languages have real Tree-Sitter grammar binaries bundled with Yole Desktop. All five affordances are active when the file's language is enabled.

| Language | Extensions | Indent |
|---|---|---|
| Bash | `.sh`, `.bash` | 2 sp |
| C | `.c`, `.h` | 4 sp |
| C++ | `.cpp`, `.cc`, `.cxx`, `.hpp` | 4 sp |
| C# | `.cs` | 4 sp |
| Clojure | `.clj`, `.cljs`, `.cljc` | 2 sp |
| CSS | `.css` | 2 sp |
| Dart | `.dart` | 2 sp |
| Dockerfile | `Dockerfile`, `.dockerfile` | 2 sp |
| Elixir | `.ex`, `.exs` | 2 sp |
| Elm | `.elm` | 4 sp |
| Erlang | `.erl`, `.hrl` | 4 sp |
| Fortran | `.f`, `.f90`, `.f95`, `.f03` | 2 sp |
| Go | `.go` | tab |
| GraphQL | `.graphql`, `.gql` | 2 sp |
| Haskell | `.hs`, `.lhs` | 2 sp |
| HTML | `.html`, `.htm`, `.xhtml` | 2 sp |
| Java | `.java` | 4 sp |
| JavaScript | `.js`, `.mjs`, `.cjs` | 2 sp |
| JSON | `.json`, `.jsonc` | 2 sp |
| Julia | `.jl` | 4 sp |
| Kotlin | `.kt`, `.kts` | 4 sp |
| LaTeX | `.tex`, `.latex` | 4 sp |
| Lua | `.lua` | 2 sp |
| Makefile | `Makefile`, `GNUmakefile` | tab |
| Markdown | `.md`, `.markdown` | 2 sp |
| Nix | `.nix` | 2 sp |
| Obj-C | `.m`, `.mm` | 4 sp |
| OCaml | `.ml`, `.mli` | 2 sp |
| Perl | `.pl`, `.pm` | 4 sp |
| PHP | `.php` | 4 sp |
| Protobuf | `.proto` | 2 sp |
| Python | `.py`, `.pyi`, `.pyw` | 4 sp |
| R | `.r`, `.R` | 2 sp |
| Regex | `.re` | — |
| Ruby | `.rb` | 2 sp |
| Rust | `.rs` | 4 sp |
| Scala | `.scala` | 2 sp |
| SCSS | `.scss` | 2 sp |
| SQL | `.sql` | 4 sp |
| Swift | `.swift` | 4 sp |
| Terraform / HCL | `.tf`, `.hcl` | 2 sp |
| TOML | `.toml` | 2 sp |
| TSX | `.tsx` | 2 sp |
| TypeScript | `.ts`, `.mts`, `.cts` | 2 sp |
| Vue | `.vue` | 2 sp |
| YAML | `.yaml`, `.yml` | 2 sp |
| Zig | `.zig` | 4 sp |

### Format-level recognition only (8 languages)

The following languages are recognized by Yole (language metadata + comment toggle + auto-indent + bracket-pair rules) but do **not** have a bundled Tree-Sitter grammar today. The outline panel and fold gutter are unavailable for these languages. Syntax highlighting requires a grammar; see "Known limitations" below.

| Language | Extensions | Gap reason |
|---|---|---|
| BibTeX | `.bib` | No bonede JAR artifact |
| Crystal | `.cr` | No bonede JAR artifact |
| Groovy | `.groovy`, `.gradle` | No bonede JAR artifact |
| JSX | `.jsx` | No bonede JAR artifact (uses JavaScript grammar indirectly) |
| Less | `.less` | No bonede JAR artifact |
| Nim | `.nim` | Bonede JAR segfaults — tracked as `#f2-phase-7-nim-grammar-broken` |
| Vim script | `.vim` | No bonede JAR artifact |
| XML | `.xml` | No bonede JAR artifact |

---

## 3. How to use each affordance

### 3.1 Comment toggle (Ctrl+/ / Cmd+/)

1. Position your cursor anywhere on the line you want to comment, or select multiple lines.
2. Press **Ctrl+/** (Linux/Windows) or **Cmd+/** (macOS). On Android, use the toolbar shortcut button labeled `//`.
3. The line(s) are prefixed with the language's line-comment marker, or wrapped in a block comment when the language has no line comment (HTML, CSS).
4. Press the same shortcut again to uncomment.

> **Example in Kotlin:** `val x = 1` becomes `// val x = 1`.
> **Example in Python:** `def foo():` becomes `# def foo():`.
> **Example in HTML:** `<div>` becomes `<!-- <div> -->`.

### 3.2 Auto-indent (Enter key)

When you press Enter after a line that ends with a block-opening token, Yole inserts a new line at an increased indentation level and places your cursor there.

- **Brace languages (Kotlin, Java, Go, Rust, …):** any line ending with `{` triggers an indent.
- **Colon languages (Python, YAML):** any line ending with `:` triggers an indent.
- **HTML:** a line ending with an open tag (e.g. `<div>`) triggers an indent; the matching close tag is left on the next line.

If the language has an `indents.scm` Tree-Sitter query file (most of the 47 bundled languages do), the engine consults it for richer multi-level indent decisions. The simple-token fallback is always present as a safety net.

### 3.3 Outline panel

1. Tap the **Outline** button in the editor toolbar (the `⋮` menu on smaller screens has it under "View → Outline").
2. A drawer slides in from the left showing the document's symbols — functions, classes, structs, sections (H-headings for Markdown), etc.
3. Tap any symbol to jump to it in the editor. The drawer stays open so you can continue navigating.
4. Tap the button again or swipe the drawer closed to dismiss.

The outline is populated by the `outline.scm` Tree-Sitter query file for the active language. On languages where no bundled grammar is available, the outline panel shows "Outline unavailable for this language."

### 3.4 Bracket-pair auto-close

Type any of the following opening characters and the matching closer is inserted automatically, with the cursor between the pair:

| You type | Inserted |
|---|---|
| `(` | `()` |
| `[` | `[]` |
| `{` | `{}` |
| `"` | `""` |
| `'` | `''` |

> **Backtick pairs** are enabled for Markdown (`` ` `` → ` `` ` ` ``).

To disable bracket auto-close for a single typing action, press **Escape** immediately after the auto-close event. The closing character is removed and you are left at the position after the opening character.

### 3.5 Fold gutter

The fold gutter appears as a narrow strip to the left of the line-number gutter. It is only visible on Desktop (the Android editor renders it but at a narrower width due to touch-target constraints).

- A **▼ chevron** appears on lines that start a foldable block (function declaration, class body, `if` block, multi-line comment, etc.).
- Tap or click the chevron to collapse the block — its contents are replaced by `…` on the same line as the opening.
- Tap the **▶ chevron** (now on the collapsed line) to expand it again.
- All foldable regions are determined by the language's `folds.scm` Tree-Sitter query.

---

## 4. Enabling languages

Source-code languages are opt-in, exactly as in iter-57's Settings → Formats screen.

1. Open **Settings → Formats**.
2. Scroll to the **Programming languages** section.
3. Toggle the switch next to a language to enable it. Files with that language's extension will open with all affordances active.
4. Toggling OFF removes highlighting and affordances for that language; files open as plain text until you re-enable.

The 8 languages without a bundled Tree-Sitter grammar appear in the list with a note: "Limited support — outline and fold unavailable."

---

## 5. Per-platform reality

| Feature | Desktop (5 ABIs) | Android | iOS | Web (Wasm) |
|---|---|---|---|---|
| Language recognition (55 langs) | Full | Full | Full | Full |
| Comment toggle | Full | Full | Full | Full |
| Auto-indent | Full | Full | Full | Full |
| Bracket-pair auto-close | Full | Full | Full | Full |
| Syntax highlighting (47 langs) | Full — Tree-Sitter | Only markdown (NDK pending) | BLOCKED — Xcode required | Limited — markdown only |
| Outline panel (47 langs) | Full | Only markdown | BLOCKED | Limited |
| Fold gutter (47 langs) | Full | Only markdown | BLOCKED | Limited |

> **Why is Android limited?** The Desktop Tree-Sitter grammar JARs ship glibc-linked `.so` files that Android's Bionic linker rejects. Building 47 language grammars against the Android NDK is a known pending task tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-android-ndk-bulk-build-pending`. The iter-57 Markdown grammar (built against the NDK separately) is the only grammar available on Android today.

> **Why is iOS BLOCKED?** The iOS build requires Xcode + the iOS SDK (`xcrun --sdk iphoneos`) for cross-compiling Tree-Sitter static libraries. The build host has only Command-Line Tools installed. Tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-ios-xcode-required`.

> **Why is Web limited?** The Wasm engine uses `vscode-textmate` rather than Tree-Sitter. Full multi-language grammar support on Wasm is a follow-up item.

---

## 6. Embedded languages (HTML and Markdown)

Two special cases are handled by dedicated sub-language tokenizers:

### HTML: embedded `<style>` and `<script>`

When you open an HTML file, Yole automatically re-tokenizes the body of any `<style>` block using the CSS grammar and any `<script>` block using the JavaScript grammar (when those grammars are available and enabled). The editor shows proper CSS/JS coloring inside those embedded regions, not plain HTML tokens.

### Markdown: code fences

When you open a Markdown file, language-tagged fenced code blocks (` ```kotlin `, ` ```python `, etc.) are tokenized by the corresponding language's grammar. The result appears in the editor as colored syntax inside the fence. If the sub-language's grammar is not available or not enabled, the fence content is plain text (no fake coloring per CONST-035).

---

## 7. Privacy

All language metadata, Tree-Sitter grammar binaries, and query files ship inside the Yole binary — no network requests are made to tokenize or outline your source files. Your code is never uploaded to any server for any of these features.

---

## 8. Troubleshooting

**"Outline unavailable for this language."**
The active file's language is in the 8-lang gap set (no bundled Tree-Sitter grammar). See the table in §2.

**Comment toggle inserts the wrong marker.**
The file's language may not have been detected correctly. Check the language indicator in the status bar. If it shows "Plain Text", the file's extension is not registered to any language — enable the language in Settings → Formats or rename the file to use a recognized extension.

**Auto-indent does not trigger.**
Make sure the language is enabled in Settings → Formats. On Android, the feature is tied to the same feature-gate. Indent rules for whitespace-significant languages (Python, YAML) rely on `indents.scm` being available; those files are bundled in the app.

**Fold chevrons not visible.**
Fold gutter is only visible when the active language has a `folds.scm` Tree-Sitter query and a bundled grammar. If neither the grammar nor the query is available, the gutter strip is present but empty.

**Android — plain text in all 46 non-markdown languages.**
This is expected for iter-58. Syntax highlighting, outline, and fold all require a Tree-Sitter grammar binary compiled for Android NDK. Only Markdown ships that binary today. The other 46 grammars are pending an operator NDK bulk-build. See `docs/KNOWN_DEFECTS.md#f2-phase-7-android-ndk-bulk-build-pending`.

---

## 9. Cross-reference

- **Architecture deep-dive:** `docs/features/source-code-file-support/architecture.md`.
- **Language coverage matrix:** `docs/features/source-code-file-support/language-coverage-matrix.md`.
- **Known limitations:** `docs/KNOWN_DEFECTS.md`.
- **Research report:** `docs/features/source-code-file-support/research-report.md`.
- **iter-58 CHANGELOG entry:** `CHANGELOG.md` (iter-58 section).
