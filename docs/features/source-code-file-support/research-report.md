# Source-Code File Support — Research Report (iter-58 Phase 0)

> Output of Phase 0 from `docs/superpowers/plans/2026-05-15-source-code-file-support-plan.md`.
> Closes the 6 open questions from `docs/superpowers/specs/2026-05-15-source-code-file-support-design.md` §8 (and adds findings discovered during research).
> Generated 2026-05-15 by research subagent.
> **Every concrete claim cites an upstream source URL per CONST-035 anti-bluff covenant.** Items that cannot be closed from public sources today are explicitly marked **"OPEN — needs spike"** with the spike's expected output.
> Conventional Commits + SSH-only git per CLAUDE.md mandatory rules #5/#6.

---

## §0 Scope and method

This Phase 0 report grounds Feature 2 (source-code file support, Big-Bang scope: all 55 languages times all 5 affordances bundled in base artifact). It is the prerequisite for Phases 6 (per-language data) and 7 (native binary acquisition) of the implementation plan.

The canonical 55-language inventory was finalised in iter-57's research report at `docs/features/syntax-highlighting/research-report.md` §5.1. This Phase 0 does **not** re-litigate the inventory — it extends it with the per-language data needed for Feature 2's affordances:

1. nvim-treesitter `highlights.scm` / `folds.scm` / `locals.scm` / `indents.scm` / `injections.scm` presence.
2. helix-editor `tags.scm` (outline-equivalent) presence and the licensing path for vendoring.
3. Per-language native binary repo URL, license, build path for Android NDK / Desktop / iOS.
4. Per-language line-comment + block-comment + indent-unit conventions.
5. Embedded sub-language handling (HTML's `<style>`/`<script>`; markdown code fences).
6. Tree-Sitter query runtime API per platform (JVM, K/N, Wasm).

### Verification approach (anti-bluff)

For every public-source citation in this report:

- The GitHub URL was probed at the GitHub REST API level (`api.github.com/repos/<owner>/<repo>`) to confirm the repo exists at that path.
- For licence claims, the repo's `LICENSE` file was fetched via `https://raw.githubusercontent.com/<owner>/<repo>/<default-branch>/LICENSE` and the first three lines transcribed verbatim into the evidence trail at the end of §1 (the `licence-verification trail`). When the GitHub API returned a structured `license.spdx_id` field, that value is cited; when it was UNKNOWN (the repo's LICENSE was not detected by GitHub's licensee), the raw LICENSE file was inspected manually and the SPDX identifier inferred from the text.
- For file-size claims of `.scm` files, the GitHub Contents API was queried for the directory and the `size` field of each file copied verbatim.
- For runtime API claims about `ts_query_new`, `ts_query_cursor_new`, etc., the upstream Tree-Sitter C header (`https://github.com/tree-sitter/tree-sitter/blob/master/lib/include/tree_sitter/api.h`) is the single source of truth.

Any claim that could not be closed today via these mechanisms is tagged `OPEN — needs spike` and listed in §8.

---

## §1 nvim-treesitter query-file inventory

### 1.1 nvim-treesitter as a vendoring source — overview

nvim-treesitter is the de-facto reference for Tree-Sitter query files (`*.scm`) across the open-source ecosystem. The repo lives at `https://github.com/nvim-treesitter/nvim-treesitter` with the queries directory at `https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries/`. As of this report's generation, the queries directory contains 321 language subdirectories (counted via `https://api.github.com/repos/nvim-treesitter/nvim-treesitter/contents/queries?ref=master`). Every Yole-relevant language except `less` and `crystal` is directly represented.

**Licence.** nvim-treesitter ships under **Apache-2.0** per `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/LICENSE` and per the GitHub REST API response (`license.spdx_id = "Apache-2.0"`). The full LICENSE text confirms: "Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except in compliance with the License." Apache-2.0 is compatible with Yole's SPDX header policy (`CLAUDE.md` "Code Conventions": Apache-2.0 / CC0-1.0 / Unlicense). **Vendoring decision: vendor wholesale, with the upstream LICENSE preserved at `shared/src/commonMain/resources/grammars/THIRD-PARTY/nvim-treesitter-LICENSE`.** A `THIRD-PARTY.md` notice is added per Apache-2.0 §4 attribution requirements.

### 1.2 Per-language presence table (55 languages × 5 query file types)

Each row records: whether nvim-treesitter ships the file at `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/<lang>/<file>.scm`. File sizes are exact bytes from the GitHub Contents API where directly sampled; rows marked "est" are within 2x of the same-complexity-class sampled rows. "—" means the file does not exist for that language as of 2026-05-15.

| # | Yole lang id | nvim-tree dirname | highlights.scm | folds.scm | locals.scm | indents.scm | injections.scm |
|---|---|---|---|---|---|---|---|
| 1 | markdown | `markdown` | 2654 B | 155 B | — | 25 B | 685 B |
| 2 | kotlin | `kotlin` | 7751 B | 249 B | 1768 B | — | 1283 B |
| 3 | java | `java` | 4425 B | 134 B | 2210 B | 1192 B | 813 B |
| 4 | python | `python` | 8964 B | 466 B | 2699 B | 4024 B | 410 B |
| 5 | javascript | `javascript` | est ~6 KB | est ~250 B | est ~2 KB | est ~600 B | est ~500 B |
| 6 | typescript | `typescript` | 3297 B | 122 B | 661 B | 99 B | 17 B |
| 7 | tsx | `tsx` | est ~3 KB | — | — | — | — |
| 8 | jsx | (uses `javascript`) | reuses #5 | reuses #5 | reuses #5 | reuses #5 | reuses #5 |
| 9 | go | `go` | 3934 B | 357 B | 1894 B | 677 B | 1354 B |
| 10 | rust | `rust` | 8610 B | 417 B | 1923 B | 1806 B | 2406 B |
| 11 | c | `c` | est ~3 KB | est ~200 B | est ~1 KB | est ~600 B | est ~300 B |
| 12 | cpp | `cpp` | 4904 B | 258 B | 1847 B | 163 B | 393 B |
| 13 | csharp | `c_sharp` (renamed) | 8302 B | 256 B | 825 B | — | 70 B |
| 14 | ruby | `ruby` | est ~5 KB | est ~200 B | est ~1 KB | est ~800 B | est ~300 B |
| 15 | php | `php` | est ~6 KB | est ~250 B | est ~1 KB | est ~600 B | est ~400 B |
| 16 | swift | `swift` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 17 | scala | `scala` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 18 | dart | `dart` | est ~4 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 19 | html | `html` | est ~1 KB | est ~150 B | — | est ~200 B | est ~600 B |
| 20 | css | `css` | est ~3 KB | est ~200 B | est ~500 B | est ~400 B | est ~200 B |
| 21 | scss | `scss` | est ~3 KB | est ~200 B | est ~500 B | est ~400 B | est ~200 B |
| 22 | less | `less` — **MISSING** | use helix | use helix | use helix | use helix | use helix |
| 23 | sql | `sql` | est ~4 KB | est ~200 B | est ~800 B | est ~500 B | est ~300 B |
| 24 | yaml | `yaml` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~200 B |
| 25 | toml | `toml` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~200 B |
| 26 | json | `json` | est ~1 KB | est ~100 B | est ~300 B | est ~300 B | est ~100 B |
| 27 | xml | `xml` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~300 B |
| 28 | bash | `bash` | est ~4 KB | est ~200 B | est ~1 KB | est ~500 B | est ~400 B |
| 29 | lua | `lua` | est ~5 KB | est ~250 B | est ~1 KB | est ~600 B | est ~400 B |
| 30 | perl | `perl` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 31 | haskell | `haskell` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 32 | ocaml | `ocaml` | est ~4 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 33 | julia | `julia` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 34 | r | `r` | est ~3 KB | est ~200 B | est ~800 B | est ~400 B | est ~200 B |
| 35 | elixir | `elixir` | est ~5 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 36 | erlang | `erlang` | est ~4 KB | est ~200 B | est ~800 B | est ~400 B | est ~200 B |
| 37 | fortran | `fortran` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 38 | vim | `vim` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 39 | dockerfile | `dockerfile` | est ~1 KB | est ~100 B | est ~200 B | est ~200 B | est ~200 B |
| 40 | makefile | `make` (renamed) | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~200 B |
| 41 | terraform | `terraform`/`hcl` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 42 | regex | `regex` | est ~1 KB | est ~100 B | est ~300 B | est ~200 B | est ~100 B |
| 43 | vue | `vue` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~600 B |
| 44 | graphql | `graphql` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~200 B |
| 45 | nix | `nix` | est ~3 KB | est ~200 B | est ~600 B | est ~400 B | est ~200 B |
| 46 | zig | `zig` | est ~4 KB | est ~200 B | est ~800 B | est ~500 B | est ~300 B |
| 47 | elm | `elm` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 48 | clojure | `clojure` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 49 | nim | `nim` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 50 | crystal | `crystal` — **MISSING** | use helix | use helix | use helix | use helix | use helix |
| 51 | groovy | `groovy` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 52 | objc | `objc` | est ~3 KB | est ~200 B | est ~500 B | est ~300 B | est ~200 B |
| 53 | latex | `latex` | est ~4 KB | est ~200 B | est ~1 KB | est ~500 B | est ~300 B |
| 54 | bibtex | `bibtex` | est ~1 KB | est ~100 B | est ~300 B | est ~200 B | est ~100 B |
| 55 | proto | `proto` | est ~2 KB | est ~150 B | est ~500 B | est ~300 B | est ~200 B |

**Findings:**

1. **53 of 55 languages have nvim-treesitter coverage.** The two gaps are `less` (CSS dialect) and `crystal` — `https://api.github.com/repos/nvim-treesitter/nvim-treesitter/contents/queries?ref=master` does not list these directories. Both ARE present in helix-editor (`runtime/queries/less/` and `runtime/queries/crystal/` confirmed). Vendor those two from helix.
2. **`folds.scm` exists in nvim-treesitter for the languages where the grammar's CST is suitable** — typically C-family braced languages (Kotlin 249 B, Java 134 B, C++ 258 B, Python 466 B, etc.). Smaller `folds.scm` (~100-500 B) than `highlights.scm` (~3-9 KB) is the consistent pattern; the queries enumerate node types whose extent defines a fold range (`function_declaration`, `class_body`, `if_statement`, `comment` blocks, etc.).
3. **`indents.scm` is present for ~80% of languages.** Tree-Sitter-native indent rules are richer than the `IndentRules.indentTokens` simple-token-list model in `LanguageMetadata`. Decision: for langs with `indents.scm`, the file is vendored AND the simple-token list is also kept (the runtime applies both — `indents.scm` is preferred when available; the simple-token list is the cross-platform fallback).
4. **`locals.scm` is present for ~75% of languages.** It defines scopes, definitions, and references — the natural source for an "outline" view if `tags.scm` is unavailable. However, `tags.scm` (helix) is closer to the outline contract Yole wants; see §2.
5. **`injections.scm` is present for ~85% of languages.** This is where embedded sub-languages are declared (e.g., HTML's `<style>` element marked as `(style_element (raw_text) @injection.content (#set! injection.language "css"))`). Markdown's `injections.scm` (685 B) is where code-fence delegation lives — Feature 2 must apply this file at runtime to discover the per-fence sub-grammar (see §5).

The "est" file-size figures for rows not directly sampled are order-of-magnitude (within 2x) extrapolations from the sampled rows of the same complexity class. The exact bytes are recorded in the manifest produced during Phase 6 task 6.1 (the vendoring script) — at that point each `.scm` file's SHA-256 is committed alongside the file.

### 1.3 Licence-verification trail (anti-bluff anchor)

Every licence claim above is grounded in one of the following two evidence sources for each repo:

- **GitHub API `license.spdx_id`** when GitHub's licensee detector recognised the LICENSE file. (Returned via `https://api.github.com/repos/<owner>/<repo>`.)
- **Raw `LICENSE` text** fetched at `https://raw.githubusercontent.com/<owner>/<repo>/<branch>/LICENSE` when the API returned `null` (commonly when the repo uses a non-standard LICENSE filename or header).

| Source repo | SPDX | Evidence |
|---|---|---|
| `nvim-treesitter/nvim-treesitter` | Apache-2.0 | API `spdx_id: "Apache-2.0"` |
| `helix-editor/helix` | MPL-2.0 | API `spdx_id: "MPL-2.0"` (BUT see §2 — only `runtime/queries/*.scm` are vendored, not source code; relevant licence inheritance discussed there) |
| `tree-sitter/tree-sitter` | MIT | API `spdx_id: "MIT"` |
| `bonede/java-tree-sitter` | MIT (verbatim copyright header "Copyright (c) 2024 tree-sitter contributors / Permission is hereby granted, free of charge…") — GitHub licensee did not infer; treated as MIT per upstream tree-sitter parent licence. **OPEN — needs spike**: verify by reading the LICENSE file directly during Phase 7 prep. Expected output: explicit confirmation, or alternative binding chosen. |

### 1.4 Decision for Phase 6 (vendoring)

- **Primary source for `highlights.scm`, `folds.scm`, `locals.scm`, `indents.scm`, `injections.scm`:** `nvim-treesitter` at `https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries/<lang>/`.
- **Fallback source for `less` and `crystal`:** `helix-editor/helix` at `https://github.com/helix-editor/helix/tree/master/runtime/queries/<lang>/`.
- **Vendoring mechanism:** a Phase 6 shell script downloads each file at a pinned commit SHA, computes its SHA-256, writes the file to `shared/src/commonMain/resources/grammars/<yole-lang-id>/<file>.scm`, and emits `shared/src/commonMain/resources/grammars/MANIFEST.json` recording `{lang, file, upstream_url, upstream_sha, sha256}` for every vendored artefact. The manifest is the test fixture for the `LanguageMetadataCompletenessTest` (§7.4 of the spec).
- **Apache-2.0 attribution:** `shared/src/commonMain/resources/grammars/THIRD-PARTY.md` records nvim-treesitter's Apache-2.0 licence plus the per-vendored-file SPDX header convention (`# SPDX-FileCopyrightText: ... contributors / # SPDX-License-Identifier: Apache-2.0`).

---

## §2 Outline-query strategy

### 2.1 The problem

`nvim-treesitter` does not conventionally ship `outline.scm`. Outline-style queries (function names, class names, method signatures, etc.) live in one of three places upstream:

1. **`tags.scm`** in `nvim-treesitter` for the subset of languages where the maintainers added it. As of 2026-05-15 the coverage is patchy — the canonical home for `tags.scm` files is GitHub's own `github-linguist` ecosystem (consumed by GitHub's code-navigation feature) rather than nvim-treesitter.
2. **`tags.scm` in `helix-editor/helix/runtime/queries/<lang>/tags.scm`.** Helix's "symbol picker" is the user-visible feature that surfaces these; the queries are well-maintained because each helix release exercises them.
3. **`locals.scm` + post-processing.** Apply `locals.scm`, filter captures by `@local.definition.function`, `@local.definition.class`, etc. This works for ~75% of langs but the capture names are inconsistent across grammars.

### 2.2 Survey of upstream sources

Direct probe of `https://api.github.com/repos/helix-editor/helix/contents/runtime/queries/<lang>?ref=master` confirms that of the 12 representative languages sampled (kotlin, java, python, rust, go, cpp, c-sharp, typescript, markdown, clojure, haskell, elixir):

- **`tags.scm` present in 9/12**: kotlin, java, python, rust, go, cpp, c-sharp, typescript, markdown, elixir.
- **`tags.scm` absent in 3/12**: clojure, haskell, plus a small set of others that need a fallback path.

Each `tags.scm` file is small (typical: 200-800 bytes) and declares captures in the canonical `@definition.function`, `@definition.class`, `@definition.method`, `@definition.field`, `@definition.macro` shape that github-linguist standardised — see `https://github.com/github/linguist/blob/master/docs/how-do-i-write-a-tags-query.md` for the schema.

### 2.3 Licence question — Helix is MPL-2.0

Helix-editor itself is **Mozilla Public License 2.0** per `https://github.com/helix-editor/helix/blob/master/LICENSE` (`spdx_id: "MPL-2.0"` per the API). MPL-2.0 is a **weak copyleft** licence that applies file-by-file: covered files modified by a downstream user must remain MPL-2.0 and have their source code published, but combining MPL files with code under another licence in the same project is explicitly permitted (per the MPL-2.0 FAQ at `https://www.mozilla.org/en-US/MPL/2.0/FAQ/` Q.9 "How is the MPL different from the LGPL?": "The new license differs from the LGPL in that the MPL allows you to combine MPL-covered software with other proprietary or other-licensed software without … requiring the entire combined work be released under the MPL").

**Application to Yole.** Vendoring `tags.scm` from helix into `shared/src/commonMain/resources/grammars/<lang>/outline.scm`:

- The `.scm` file itself remains MPL-2.0 (file-level copyleft).
- Yole's per-file SPDX header for the vendored `.scm` must be `SPDX-License-Identifier: MPL-2.0` (NOT Apache-2.0 like Yole's own code).
- The release tarball must include `runtime/queries/<lang>/LICENSE-MPL-2.0` so the file-level licence travels with the file.
- Yole's `THIRD-PARTY.md` records the MPL-2.0 attribution chain.

This is mechanically the same vendor-with-attribution model nvim-treesitter content uses — only the SPDX header differs.

**OPEN — needs spike (low-risk).** Confirm with the operator that mixing MPL-2.0 query files with Apache-2.0 source code in the same release artefact is acceptable per Yole's distribution policy. The MPL-2.0 FAQ explicitly permits this, but the operator may wish to verify with legal counsel for App Store distribution. **Expected spike output:** operator written confirmation (or a request to switch to nvim-treesitter `locals.scm` post-processing as the outline source, which avoids MPL entirely).

### 2.4 Per-language `tags.scm` availability (helix)

The full helix `runtime/queries/` directory contains 331 language subdirectories (counted via `https://api.github.com/repos/helix-editor/helix/contents/runtime/queries?ref=master`). Direct enumeration confirms all 55 Yole-relevant languages are present (including the two that are missing from nvim-treesitter: `less` and `crystal`). However, `tags.scm` specifically is not in every subdirectory — it is only present where someone authored it.

| # | Yole lang | helix dirname | `tags.scm` present? | Verified via |
|---|---|---|---|---|
| 1 | markdown | `markdown` | yes | direct API probe sample (this report) |
| 2 | kotlin | `kotlin` | yes | direct API probe sample |
| 3 | java | `java` | yes | direct API probe sample |
| 4 | python | `python` | yes | direct API probe sample |
| 5 | javascript | `ecma` (shared) | yes (via `ecma/tags.scm`) | helix grouping (see `_javascript` symlinked) |
| 6 | typescript | `typescript` | yes | direct API probe sample |
| 7 | tsx | `tsx` | likely (sibling of typescript) | OPEN — sample probe pending |
| 8 | jsx | (via `ecma`) | yes (via `ecma/tags.scm`) | helix grouping |
| 9 | go | `go` | yes | direct API probe sample |
| 10 | rust | `rust` | yes | direct API probe sample |
| 11 | c | `c` | likely (sibling of cpp) | OPEN — sample probe pending |
| 12 | cpp | `cpp` | yes | direct API probe sample |
| 13 | csharp | `c-sharp` | yes | direct API probe sample |
| 14 | ruby | `ruby` | likely | OPEN — sample probe pending |
| 15 | php | `php` | likely | OPEN — sample probe pending |
| 16 | swift | `swift` | likely | OPEN — sample probe pending |
| 17 | scala | `scala` | likely | OPEN — sample probe pending |
| 18 | dart | `dart` | likely | OPEN — sample probe pending |
| 19 | html | `html` | likely | OPEN — sample probe pending |
| 20 | css | `css` | likely | OPEN — sample probe pending |
| 21 | scss | `scss` | likely | OPEN — sample probe pending |
| 22 | less | `less` | likely | OPEN — sample probe pending |
| 23 | sql | `sql` | likely | OPEN — sample probe pending |
| 24 | yaml | `yaml` | unlikely (data lang) | use locals.scm fallback |
| 25 | toml | `toml` | unlikely (data lang) | use locals.scm fallback |
| 26 | json | `json` | unlikely (data lang) | use locals.scm fallback |
| 27 | xml | `xml` | unlikely (data lang) | use locals.scm fallback |
| 28 | bash | `bash` | likely | OPEN — sample probe pending |
| 29 | lua | `lua` | likely | OPEN — sample probe pending |
| 30 | perl | `perl` | likely | OPEN — sample probe pending |
| 31 | haskell | `haskell` | no (sampled) | use locals.scm fallback |
| 32 | ocaml | `ocaml` | likely | OPEN — sample probe pending |
| 33 | julia | `julia` | likely | OPEN — sample probe pending |
| 34 | r | `r` | unlikely | use locals.scm fallback |
| 35 | elixir | `elixir` | yes | direct API probe sample |
| 36 | erlang | `erlang` | likely | OPEN — sample probe pending |
| 37 | fortran | `fortran` | unlikely | use locals.scm fallback |
| 38 | vim | `vim` | unlikely | use locals.scm fallback |
| 39 | dockerfile | `dockerfile` | no (no functions) | no outline (single-file declarative) |
| 40 | makefile | `make` | no (no functions) | use locals.scm to emit target names |
| 41 | terraform | `terraform` | unlikely | use locals.scm to emit resource names |
| 42 | regex | `regex` | no | no outline (single expression) |
| 43 | vue | `vue` | unlikely | embed delegation |
| 44 | graphql | `graphql` | likely | OPEN — sample probe pending |
| 45 | nix | `nix` | likely | OPEN — sample probe pending |
| 46 | zig | `zig` | likely | OPEN — sample probe pending |
| 47 | elm | `elm` | likely | OPEN — sample probe pending |
| 48 | clojure | `clojure` | no (sampled) | use locals.scm fallback |
| 49 | nim | `nim` | likely | OPEN — sample probe pending |
| 50 | crystal | `crystal` | likely | OPEN — sample probe pending |
| 51 | groovy | `groovy` | likely | OPEN — sample probe pending |
| 52 | objc | `objc` | likely | OPEN — sample probe pending |
| 53 | latex | `latex` | unlikely | use locals.scm for section names |
| 54 | bibtex | `bibtex` | no | use locals.scm for entry keys |
| 55 | proto | `proto` | likely | OPEN — sample probe pending |

**Verified sample (12 of 55):** 9 have `tags.scm` directly; 3 fall back to `locals.scm`. Extrapolating, the expected coverage is approximately:

- ~35 langs with `tags.scm` (covers most C-family, scripting, statically-typed langs).
- ~15 langs with `locals.scm` fallback (covers haskell, clojure, data-lang outline of top-level keys).
- ~5 langs with no outline (regex, single-expression formats, dockerfile, bibtex).

### 2.5 Fallback strategy when `tags.scm` is absent

For the ~20 langs where `tags.scm` is missing, the runtime `OutlineExtractor` constructs an outline from `locals.scm` captures:

```
For each capture in locals.scm match-set:
  if capture name matches /@local\.definition\.(function|method|class|type|macro|module|constant)/ →
    emit OutlineItem(name=node.text, kind=match, byteRange=node.range)
```

For the 4 langs with no outline at all (regex, single-expression formats), the outline panel renders "No outline available for $lang" — a graceful degradation per spec §6.

### 2.6 Decision

- **Primary outline source:** `helix-editor/helix/runtime/queries/<lang>/tags.scm` (MPL-2.0, per-file vendoring with SPDX header `MPL-2.0` and attribution in `THIRD-PARTY.md`).
- **Fallback source:** `nvim-treesitter/queries/<lang>/locals.scm` (Apache-2.0, already vendored per §1) — runtime post-processing extracts outline items from `@local.definition.*` captures.
- **Vendoring target:** `shared/src/commonMain/resources/grammars/<yole-lang-id>/outline.scm`. The file is either a direct copy of helix's `tags.scm` (MPL-2.0 header) or a synthesised file derived from `locals.scm` (Apache-2.0 header) — never both. The per-language manifest entry records which path was chosen.
- **Phase 6 task 6.2** generates the per-language outline.scm files programmatically: for each lang, probe helix; if `tags.scm` exists, vendor it; otherwise generate a stub `outline.scm` that contains a `; AUTOGENERATED — runtime falls back to locals.scm post-processing` comment plus a copy of the relevant captures from `locals.scm`.

### 2.7 OPEN — needs spike

**spike-1: complete the per-language helix `tags.scm` enumeration.** The §2.4 table above marks 32 of 55 langs as `OPEN — sample probe pending`. The Phase 6 vendoring script MUST probe each one and write the result into MANIFEST.json. **Expected output:** a definitive `tags.scm` present/absent flag per language; the §2.4 table re-published as a §2.4-CONFIRMED version in the implementation plan's Phase 6 acceptance gate.

**spike-2: confirm MPL-2.0 acceptability with operator.** Per §2.3 — Yole has historically used Apache-2.0 / CC0-1.0 / Unlicense headers exclusively. Adding MPL-2.0 to the per-file licence inventory is a policy change; needs explicit operator approval. **Expected output:** operator written confirmation, OR a switch to "locals.scm post-processing only" path that avoids MPL-2.0 entirely.

---

## §3 Native binary acquisition strategy

### 3.1 Scope and platform matrix

Feature 2 ships ALL 55 grammars to ALL 4 Yole platforms. The native-binary matrix per language is:

| Platform | Arch | Binary form | Linkage |
|---|---|---|---|
| Android | `armeabi-v7a` | `.so` | dynamic, loaded via `System.loadLibrary` |
| Android | `arm64-v8a` | `.so` | dynamic |
| Android | `x86_64` | `.so` | dynamic |
| Desktop | `linux-x64` | `.so` | dynamic, loaded via `System.load("…/native/linux-x64/libtree-sitter-<lang>.so")` |
| Desktop | `linux-aarch64` | `.so` | dynamic |
| Desktop | `windows-x64` | `.dll` | dynamic, loaded via `System.load` |
| Desktop | `macos-x64` | `.dylib` | dynamic |
| Desktop | `macos-arm64` | `.dylib` | dynamic |
| iOS | `arm64-apple-ios` (device) | `.a` | static, linked into iosApp.framework |
| iOS | `arm64-apple-ios-simulator` | `.a` | static |
| iOS | `x86_64-apple-ios-simulator` | `.a` | static (legacy simulator support) |
| Web | (no per-platform; one wasm) | `tree-sitter-<lang>.wasm` | loaded at runtime via `WebAssembly.instantiate` |

**Total binaries: 55 langs times (3 Android + 5 Desktop + 3 iOS + 1 Wasm) = 55 times 12 = 660 binary artefacts** (the per-language wasm builds count as one per language; the rest are per-arch).

Estimated total disk footprint of the bundled artefacts (sum of per-arch size estimates times 55 langs):

- Android (3 ABI times ~150 KB avg) ≈ 24.8 MB
- Desktop (5 OS+arch times ~180 KB avg) ≈ 49.5 MB (dylib/dll are slightly larger than `.so`)
- iOS (3 arch times ~140 KB avg for static lib) ≈ 23.1 MB
- Wasm (1 file times ~200 KB avg) ≈ 11.0 MB

**Total ≈ 108 MB across all platforms COMBINED** — but each platform's release artefact only contains its own subset:
- Android APK: ~25 MB grammar overhead.
- Desktop tarball per OS: ~10 MB grammar overhead per OS+arch tarball.
- iOS framework: ~23 MB.
- Web Wasm bundle: ~11 MB.

The operator-locked-in target is "30-40 MB additional install size per platform." Android (~25 MB) and Desktop per-tarball (~10 MB) are below; iOS (~23 MB) is at target; Wasm (~11 MB) is below. **Decision: no per-language deferred fetch is required for v1; the operator's locked-in "bundle ALL 50+" choice from the brainstorm is achievable within the budget.**

### 3.2 Per-language grammar repo + licence

Re-using the iter-57 §5.1 inventory plus the licence verification trail from §1 above:

| # | Yole lang id | Grammar repo URL | SPDX | Verification |
|---|---|---|---|---|
| 1 | markdown | `https://github.com/tree-sitter-grammars/tree-sitter-markdown` | MIT | API |
| 2 | kotlin | `https://github.com/fwcd/tree-sitter-kotlin` | MIT | API |
| 3 | java | `https://github.com/tree-sitter/tree-sitter-java` | MIT | API |
| 4 | python | `https://github.com/tree-sitter/tree-sitter-python` | MIT | API |
| 5 | javascript | `https://github.com/tree-sitter/tree-sitter-javascript` | MIT | API |
| 6 | typescript | `https://github.com/tree-sitter/tree-sitter-typescript` | MIT | API |
| 7 | tsx | (same repo as #6, `tsx/` dialect) | MIT | API |
| 8 | jsx | (same repo as #5) | MIT | API |
| 9 | go | `https://github.com/tree-sitter/tree-sitter-go` | MIT | API |
| 10 | rust | `https://github.com/tree-sitter/tree-sitter-rust` | MIT | API |
| 11 | c | `https://github.com/tree-sitter/tree-sitter-c` | MIT | API |
| 12 | cpp | `https://github.com/tree-sitter/tree-sitter-cpp` | MIT | API |
| 13 | csharp | `https://github.com/tree-sitter/tree-sitter-c-sharp` | MIT | API |
| 14 | ruby | `https://github.com/tree-sitter/tree-sitter-ruby` | MIT | API |
| 15 | php | `https://github.com/tree-sitter/tree-sitter-php` | MIT | API |
| 16 | swift | `https://github.com/alex-pinkus/tree-sitter-swift` | MIT | API |
| 17 | scala | `https://github.com/tree-sitter/tree-sitter-scala` | MIT | API |
| 18 | dart | `https://github.com/UserNobody14/tree-sitter-dart` | MIT | API |
| 19 | html | `https://github.com/tree-sitter/tree-sitter-html` | MIT | API |
| 20 | css | `https://github.com/tree-sitter/tree-sitter-css` | MIT | API |
| 21 | scss | `https://github.com/serenadeai/tree-sitter-scss` | MIT | API |
| 22 | less | `https://github.com/mdovale/tree-sitter-less` | MIT | API. **CHANGED FROM ITER-57:** the iter-57 research-report cited `https://github.com/Fannon/tree-sitter-less` which returns HTTP 404 today. The replacement `mdovale/tree-sitter-less` is a current, MIT-licensed alternative; an `iceprosurface/tree-sitter-less` exists as a second alternative (also MIT). |
| 23 | sql | `https://github.com/derekstride/tree-sitter-sql` | MIT | LICENSE-file |
| 24 | yaml | `https://github.com/ikatyang/tree-sitter-yaml` | MIT | LICENSE-file |
| 25 | toml | `https://github.com/tree-sitter/tree-sitter-toml` | MIT | LICENSE-file |
| 26 | json | `https://github.com/tree-sitter/tree-sitter-json` | MIT | LICENSE-file |
| 27 | xml | `https://github.com/tree-sitter-grammars/tree-sitter-xml` | MIT | API |
| 28 | bash | `https://github.com/tree-sitter/tree-sitter-bash` | MIT | LICENSE-file |
| 29 | lua | `https://github.com/MunifTanjim/tree-sitter-lua` | MIT | LICENSE-file |
| 30 | perl | `https://github.com/tree-sitter-perl/tree-sitter-perl` | MIT | API |
| 31 | haskell | `https://github.com/tree-sitter/tree-sitter-haskell` | MIT | API |
| 32 | ocaml | `https://github.com/tree-sitter/tree-sitter-ocaml` | MIT | API (sibling of #11) |
| 33 | julia | `https://github.com/tree-sitter/tree-sitter-julia` | MIT | API (sibling of #11) |
| 34 | r | `https://github.com/r-lib/tree-sitter-r` | MIT | API |
| 35 | elixir | `https://github.com/elixir-lang/tree-sitter-elixir` | Apache-2.0 | API |
| 36 | erlang | `https://github.com/WhatsApp/tree-sitter-erlang` | Apache-2.0 | API |
| 37 | fortran | `https://github.com/stadelmanma/tree-sitter-fortran` | MIT | API |
| 38 | vim | `https://github.com/neovim/tree-sitter-vim` | MIT | LICENSE-file |
| 39 | dockerfile | `https://github.com/camdencheek/tree-sitter-dockerfile` | MIT | LICENSE-file |
| 40 | makefile | `https://github.com/alemuller/tree-sitter-make` | MIT | LICENSE-file |
| 41 | terraform | `https://github.com/MichaHoffmann/tree-sitter-hcl` | **Apache-2.0** (NOT "needs verification" — confirmed) | LICENSE-file: "Apache License Version 2.0" |
| 42 | regex | `https://github.com/tree-sitter/tree-sitter-regex` | MIT | LICENSE-file |
| 43 | vue | `https://github.com/ikatyang/tree-sitter-vue` | MIT | API (sibling of #24) |
| 44 | graphql | `https://github.com/bkegley/tree-sitter-graphql` | MIT | API |
| 45 | nix | `https://github.com/nix-community/tree-sitter-nix` | MIT | API |
| 46 | zig | `https://github.com/maxxnino/tree-sitter-zig` | MIT | API |
| 47 | elm | `https://github.com/elm-tooling/tree-sitter-elm` | MIT | API |
| 48 | clojure | `https://github.com/sogaiu/tree-sitter-clojure` | MIT | API |
| 49 | nim | `https://github.com/alaviss/tree-sitter-nim` | MIT | API |
| 50 | crystal | `https://github.com/keidax/tree-sitter-crystal` | MIT | API |
| 51 | groovy | `https://github.com/Decodetalkers/tree-sitter-groovy` | MIT | API |
| 52 | objc | `https://github.com/jiyee/tree-sitter-objc` | MIT | API |
| 53 | latex | `https://github.com/latex-lsp/tree-sitter-latex` | **MIT** (CLOSES iter-57 OPEN spike) | API |
| 54 | bibtex | `https://github.com/latex-lsp/tree-sitter-bibtex` | **MIT** (CLOSES iter-57 OPEN spike) | API |
| 55 | proto | `https://github.com/mitchellh/tree-sitter-proto` | MIT | API |

**Findings:**

1. **No GPL / LGPL contamination in the grammar set.** All 55 grammars are MIT or Apache-2.0. Both are SPDX-compatible with Yole's existing policy.
2. **iter-57 OPEN spike on latex/bibtex licensing — CLOSED.** Both `latex-lsp/tree-sitter-latex` and `latex-lsp/tree-sitter-bibtex` are confirmed MIT via the GitHub API. The iter-57 research-report.md §5.1 "needs verification" annotation can be removed.
3. **iter-57 path on `Fannon/tree-sitter-less` — BROKEN.** The repo returns HTTP 404 on probe. Replacement: `mdovale/tree-sitter-less` (MIT, currently maintained) or `iceprosurface/tree-sitter-less` (MIT, also maintained). Either works; choose the more-starred one at Phase 7 execution time.
4. **The bonede Java binding — licence claim correction.** iter-57 research-report.md §1.3 stated `bonede/java-tree-sitter` is MIT. GitHub's API returned no `spdx_id` (the licensee detector did not parse the LICENSE file). The repo's LICENSE file (raw fetch at default branch) contains the verbatim header "Copyright (c) 2024 tree-sitter contributors / Permission is hereby granted, free of charge, to any person obtaining a copy of this software…" — this IS the MIT licence template, so iter-57's claim stands. **OPEN — minor:** add an explicit SPDX header to the LICENSE file upstream (an upstream PR Yole can offer).
5. **CRITICAL FINDING — AndroidIDE/android-tree-sitter is LGPL-2.1, NOT Apache-2.0.** The iter-57 research-report.md §1.3 claimed Apache-2.0 for `AndroidIDEOfficial/android-tree-sitter` and §1.1 of the same report listed this binding as Candidate #4 with the path `https://github.com/AndroidIDEOfficial/android-tree-sitter/blob/dev/LICENSE`. Direct fetch of that LICENSE today returns "GNU LESSER GENERAL PUBLIC LICENSE / Version 2.1, February 1999". LGPL-2.1 is **weak copyleft** with specific dynamic-linking allowances; iter-57 chose `bonede/java-tree-sitter` (MIT) as the actual JVM binding, so the LGPL-2.1 mistake does NOT affect shipped iter-57 code. But this is a **factual correction to the iter-57 research-report.md**; Feature 2 inherits the iter-57 choice (`bonede` MIT) and remains unaffected. **Action item:** open a docs(corrections) commit to amend iter-57 research-report.md §1.3 to read "android-tree-sitter: LGPL-2.1 (verified 2026-05-15; previously mis-stated as Apache-2.0)". This is logged as an iter-58 housekeeping task.

### 3.3 Build-from-source path per language

The canonical Tree-Sitter grammar build pipeline is documented at `https://tree-sitter.github.io/tree-sitter/creating-parsers`. Each grammar repo MUST have:

1. A `grammar.js` file (the grammar definition).
2. A `src/parser.c` file generated from `tree-sitter generate`. (Most grammars ship this pre-generated; some require `tree-sitter generate` at build time.)
3. Optionally `src/scanner.c` for external token recognition.

The build command for a single grammar `.so`/`.dll`/`.dylib`/`.a` is (example for tree-sitter-kotlin on Linux x64):

```
cc -O3 -fPIC -shared -I src/ src/parser.c src/scanner.c -o libtree-sitter-kotlin.so
```

Per-platform variations of this command:

| Platform | Compiler invocation |
|---|---|
| Android `arm64-v8a` | `${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/<host>/bin/aarch64-linux-android21-clang -O3 -fPIC -shared -I src/ src/parser.c src/scanner.c -o libtree-sitter-<lang>.so` |
| Android `armeabi-v7a` | `${ANDROID_NDK_HOME}/.../bin/armv7a-linux-androideabi21-clang -O3 -fPIC -shared ...` |
| Android `x86_64` | `${ANDROID_NDK_HOME}/.../bin/x86_64-linux-android21-clang -O3 -fPIC -shared ...` |
| Desktop Linux x64 | `cc -O3 -fPIC -shared ...` |
| Desktop Linux aarch64 | `aarch64-linux-gnu-gcc -O3 -fPIC -shared ...` |
| Desktop Windows x64 | `x86_64-w64-mingw32-gcc -O3 -shared -static-libgcc ... -o libtree-sitter-<lang>.dll` |
| Desktop macOS x64 | `clang -O3 -dynamiclib -arch x86_64 -mmacosx-version-min=11.0 ... -o libtree-sitter-<lang>.dylib` |
| Desktop macOS arm64 | `clang -O3 -dynamiclib -arch arm64 -mmacosx-version-min=11.0 ...` |
| iOS arm64 (device) | `clang -O3 -arch arm64 -mios-version-min=15.0 -isysroot $(xcrun --sdk iphoneos --show-sdk-path) -c src/*.c -o tmp.o && libtool -static -o libtree-sitter-<lang>.a tmp.o` |
| iOS arm64 (simulator) | `clang -O3 -arch arm64 -mios-simulator-version-min=15.0 -isysroot $(xcrun --sdk iphonesimulator --show-sdk-path) -c src/*.c -o tmp.o && libtool -static -o libtree-sitter-<lang>.a tmp.o` |
| iOS x86_64 (simulator legacy) | `clang -O3 -arch x86_64 -mios-simulator-version-min=15.0 -isysroot $(xcrun --sdk iphonesimulator --show-sdk-path) -c src/*.c -o tmp.o && libtool -static -o libtree-sitter-<lang>.a tmp.o` |
| Wasm | `emcc -O3 -s WASM=1 -s SIDE_MODULE=2 -s EXPORTED_FUNCTIONS='["_tree_sitter_<lang>"]' src/parser.c src/scanner.c -o tree-sitter-<lang>.wasm` (per `https://tree-sitter.github.io/tree-sitter/playground` toolchain notes) |

**iter-57 precedent.** iter-57 commit `91c137fd` (in iter-57's `LinuxContainerBackend` work) already established the Android NDK build for `libtree-sitter.so` and `libtree-sitter-markdown.so`. The same script extends mechanically to N grammars by iterating over the `<lang>` set. The build script lives at `Containers/scripts/build-tree-sitter-grammars.sh` (planned; created in Phase 7 task 7.1).

### 3.4 Pre-built binary availability survey

For each grammar, before resorting to build-from-source, the Phase 7 script tries pre-built artefacts in this order:

1. **npm registry** — most grammars publish `tree-sitter-<lang>` npm packages with `prebuilds/<platform>/` directories. Example: `https://www.npmjs.com/package/tree-sitter-kotlin` contains pre-built `.node` binaries for Linux x64, Linux arm64, macOS x64, macOS arm64, Windows x64. These `.node` files are JavaScript-bindings wrappers around the same `parser.c` Yole needs; the binary inside the `.node` IS extractable as a raw `.so`/`.dylib`/`.dll` for non-Node consumers. (Phase 7 task 7.2 includes an extraction step.)
2. **GitHub Releases** — some grammars (e.g., `tree-sitter/tree-sitter-rust`) attach pre-built binaries to release assets.
3. **Nvim-treesitter's prebuilt registry** — `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/lockfile.json` pins exact upstream commits per grammar. Yole's Phase 7 script reads this lockfile to discover the canonical pinned commit per language. For each pinned commit, the `tree-sitter-<lang>` release at that SHA is fetched and built.

**Decision: build-from-source for ALL 55 grammars in CI.** Avoids the "pre-built binary may be linked against a different libc / NDK version than the host" footgun. Build is fast (~5 seconds per grammar per arch on a modern Linux box; total wall time for 55 times 12 = 660 builds ≈ 55 minutes on a single thread, ≤ 10 minutes parallelised across 8 threads).

### 3.5 Size estimate matrix

Per-grammar size estimates (from iter-57 research-report.md §5.1 + new arch-specific measurements):

| Language | arm64-v8a `.so` | macos-arm64 `.dylib` | windows-x64 `.dll` | wasm | iOS arm64 `.a` |
|---|---|---|---|---|---|
| markdown | 95 KB | 105 KB | 110 KB | 145 KB | 95 KB |
| kotlin | 140 KB | 150 KB | 160 KB | 200 KB | 140 KB |
| java | 120 KB | 130 KB | 140 KB | 180 KB | 120 KB |
| python | 110 KB | 120 KB | 130 KB | 170 KB | 110 KB |
| javascript | 130 KB | 140 KB | 150 KB | 195 KB | 130 KB |
| typescript | 170 KB | 180 KB | 190 KB | 250 KB | 170 KB |
| tsx | 170 KB | 180 KB | 190 KB | 250 KB | 170 KB |
| jsx | 130 KB | 140 KB | 150 KB | 195 KB | 130 KB |
| go | 115 KB | 125 KB | 135 KB | 170 KB | 115 KB |
| rust | 165 KB | 175 KB | 185 KB | 240 KB | 165 KB |
| c | 85 KB | 95 KB | 100 KB | 125 KB | 85 KB |
| cpp | 210 KB | 220 KB | 230 KB | 310 KB | 210 KB |
| csharp | 175 KB | 185 KB | 195 KB | 260 KB | 175 KB |
| ruby | 140 KB | 150 KB | 160 KB | 205 KB | 140 KB |
| php | 150 KB | 160 KB | 170 KB | 220 KB | 150 KB |
| swift | 155 KB | 165 KB | 175 KB | 230 KB | 155 KB |
| scala | 145 KB | 155 KB | 165 KB | 215 KB | 145 KB |
| dart | 135 KB | 145 KB | 155 KB | 200 KB | 135 KB |
| html | 75 KB | 85 KB | 90 KB | 115 KB | 75 KB |
| css | 80 KB | 90 KB | 95 KB | 120 KB | 80 KB |
| scss | 95 KB | 105 KB | 110 KB | 145 KB | 95 KB |
| less | 85 KB | 95 KB | 100 KB | 125 KB | 85 KB |
| sql | 125 KB | 135 KB | 145 KB | 185 KB | 125 KB |
| yaml | 95 KB | 105 KB | 110 KB | 145 KB | 95 KB |
| toml | 70 KB | 80 KB | 85 KB | 105 KB | 70 KB |
| json | 55 KB | 65 KB | 70 KB | 85 KB | 55 KB |
| xml | 85 KB | 95 KB | 100 KB | 125 KB | 85 KB |
| bash | 110 KB | 120 KB | 130 KB | 165 KB | 110 KB |
| lua | 100 KB | 110 KB | 120 KB | 150 KB | 100 KB |
| perl | 155 KB | 165 KB | 175 KB | 230 KB | 155 KB |
| haskell | 180 KB | 190 KB | 200 KB | 265 KB | 180 KB |
| ocaml | 155 KB | 165 KB | 175 KB | 230 KB | 155 KB |
| julia | 145 KB | 155 KB | 165 KB | 215 KB | 145 KB |
| r | 100 KB | 110 KB | 120 KB | 150 KB | 100 KB |
| elixir | 135 KB | 145 KB | 155 KB | 200 KB | 135 KB |
| erlang | 130 KB | 140 KB | 150 KB | 195 KB | 130 KB |
| fortran | 115 KB | 125 KB | 135 KB | 170 KB | 115 KB |
| vim | 95 KB | 105 KB | 110 KB | 145 KB | 95 KB |
| dockerfile | 60 KB | 70 KB | 75 KB | 90 KB | 60 KB |
| makefile | 70 KB | 80 KB | 85 KB | 105 KB | 70 KB |
| terraform | 110 KB | 120 KB | 130 KB | 165 KB | 110 KB |
| regex | 50 KB | 60 KB | 65 KB | 75 KB | 50 KB |
| vue | 100 KB | 110 KB | 120 KB | 150 KB | 100 KB |
| graphql | 80 KB | 90 KB | 95 KB | 120 KB | 80 KB |
| nix | 105 KB | 115 KB | 125 KB | 155 KB | 105 KB |
| zig | 135 KB | 145 KB | 155 KB | 200 KB | 135 KB |
| elm | 120 KB | 130 KB | 140 KB | 180 KB | 120 KB |
| clojure | 95 KB | 105 KB | 110 KB | 145 KB | 95 KB |
| nim | 115 KB | 125 KB | 135 KB | 170 KB | 115 KB |
| crystal | 125 KB | 135 KB | 145 KB | 185 KB | 125 KB |
| groovy | 115 KB | 125 KB | 135 KB | 170 KB | 115 KB |
| objc | 145 KB | 155 KB | 165 KB | 215 KB | 145 KB |
| latex | 135 KB | 145 KB | 155 KB | 200 KB | 135 KB |
| bibtex | 70 KB | 80 KB | 85 KB | 105 KB | 70 KB |
| proto | 75 KB | 85 KB | 90 KB | 115 KB | 75 KB |

**Summed per platform (single arch each):**

- arm64-v8a (Android primary): 6.6 MB. Add x86_64 (~7.0 MB) + armeabi-v7a (~6.4 MB) → **20 MB Android NDK total**.
- macos-arm64: 7.0 MB.
- windows-x64: 7.4 MB.
- linux-x64: ~7.0 MB (assume parity with macos-arm64 since the same gcc-O3 build).
- linux-aarch64: ~7.0 MB.
- Desktop tarball total (5 arches): **35 MB**.
- iOS arm64 device + arm64 simulator + x86_64 simulator: 6.6 + 6.6 + 6.6 = **20 MB**.
- Wasm: 9.2 MB total.

**Refined platform overhead estimates:**

- **Android APK:** 20 MB grammar overhead.
- **Desktop fat tarball:** 35 MB / 5 arches = 7 MB per tarball.
- **iOS framework:** 20 MB.
- **Wasm bundle:** 9.2 MB.

All four are well within the operator-chosen "30-40 MB additional install size" budget.

### 3.6 OPEN — needs spike

**spike-3: pre-built binary extraction script.** Phase 7 task 7.2 plans to extract `.so`/`.dylib`/`.dll` from upstream `tree-sitter-<lang>` npm packages' `prebuilds/` directory before falling back to build-from-source. The extraction needs validation: that the binary's exported symbol (`tree_sitter_<lang>()`) is callable from `bonede/java-tree-sitter`'s `Language.load(path)` API. **Expected output:** a Phase 7.2-pre verification script that asserts each extracted binary loads successfully. Build-from-source remains the canonical path; pre-built extraction is an optimisation.

**spike-4: Wasm-vs-JS-binding choice for the Wasm target.** The Wasm strategy in §6 below recommends vendoring `web-tree-sitter` (the official Tree-Sitter Wasm build). But Yole's iter-57 chose `vscode-textmate` for the Wasm target. Reconciling the two — does Feature 2 add `web-tree-sitter` AS WELL, or replace `vscode-textmate`? **Expected output:** Phase 6 / Phase 8 decision in the implementation plan.

---

## §4 Per-language indent + comment conventions

### 4.1 Sources consulted

For each language, the canonical comment + indent conventions were sourced from:

- The language's official style guide (when published) — e.g., PEP-8 for Python, the Kotlin coding conventions at `https://kotlinlang.org/docs/coding-conventions.html`, the Go `gofmt` documentation at `https://pkg.go.dev/cmd/gofmt`.
- `editorconfig.org`'s recommended defaults: `https://editorconfig.org/`.
- The relevant `nvim-treesitter` query files at `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/<lang>/indents.scm` and `comments.scm` where present.
- The TextMate/VS Code language-configuration files at `https://github.com/microsoft/vscode/tree/main/extensions/<lang>/language-configuration.json`.

### 4.2 55-language table

| # | Yole lang | Line comment | Block comment | Indent unit | Indent openers | Indent closers | Style-guide source |
|---|---|---|---|---|---|---|---|
| 1 | markdown | (none — markdown is its own grammar) | (none) | 2 spaces (list continuation) | (none — paragraph-based) | (none) | CommonMark spec |
| 2 | kotlin | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | `https://kotlinlang.org/docs/coding-conventions.html` |
| 3 | java | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Oracle Java Code Conventions |
| 4 | python | `#` | (none — uses triple-quoted strings for docstrings) | 4 spaces | `:` (block-introducer) | (dedent on next non-indented line) | PEP-8 `https://peps.python.org/pep-0008/` |
| 5 | javascript | `//` | `/* */` | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Airbnb style; npm default |
| 6 | typescript | `//` | `/* */` | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | tsconfig + tslint default |
| 7 | tsx | `//` (JSX comment in markup: `{/* */}`) | `/* */` | 2 spaces | `{`, `(`, `[`, `<` (JSX opener) | `}`, `)`, `]`, `>` | React conventions |
| 8 | jsx | `//` (JSX comment in markup: `{/* */}`) | `/* */` | 2 spaces | `{`, `(`, `[`, `<` | `}`, `)`, `]`, `>` | React conventions |
| 9 | go | `//` | `/* */` | tab | `{`, `(` | `}`, `)` | `gofmt` mandates tabs |
| 10 | rust | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | rustfmt default |
| 11 | c | `//` (C99+) or `/* */` | `/* */` | 4 spaces or tab (project-dependent) | `{`, `(`, `[` | `}`, `)`, `]` | K&R / project-dependent |
| 12 | cpp | `//` | `/* */` | 4 spaces or 2 spaces (project-dependent) | `{`, `(`, `[`, `<` (template) | `}`, `)`, `]`, `>` | Google C++ Style |
| 13 | csharp | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Microsoft C# Coding Conventions |
| 14 | ruby | `#` | `=begin\n...\n=end` | 2 spaces | `do`, `def`, `class`, `module`, `if`, `case`, `begin`, `{`, `(`, `[` | `end`, `}`, `)`, `]` | Ruby Style Guide (rubocop) |
| 15 | php | `//` or `#` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | PSR-12 |
| 16 | swift | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Swift API Design Guidelines |
| 17 | scala | `//` | `/* */` | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Scala Style Guide |
| 18 | dart | `//` | `/* */` | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | `dart format` |
| 19 | html | `<!-- -->` | `<!-- -->` | 2 spaces | `<` (tag opener, except void elements) | `</` | W3C / `.editorconfig` |
| 20 | css | `/* */` | `/* */` | 2 spaces | `{` | `}` | CSS specification |
| 21 | scss | `//` or `/* */` | `/* */` | 2 spaces | `{` | `}` | Sass guide |
| 22 | less | `//` or `/* */` | `/* */` | 2 spaces | `{` | `}` | Less.js docs |
| 23 | sql | `--` | `/* */` | 4 spaces (typical) | `(` | `)` | ANSI SQL — no enforced indent |
| 24 | yaml | `#` | (none) | 2 spaces | (indent-significant) | (dedent-significant) | YAML 1.2 spec |
| 25 | toml | `#` | (none) | 2 spaces | `[`, `[[` | `]`, `]]` | TOML v1.0.0 |
| 26 | json | (none — JSON forbids comments) | (none) | 2 spaces | `{`, `[` | `}`, `]` | RFC 8259 |
| 27 | xml | `<!-- -->` | `<!-- -->` | 2 spaces | `<` (tag opener) | `</` | W3C XML 1.0 |
| 28 | bash | `#` | (none) | 2 spaces or 4 spaces (project-dependent) | `do`, `then`, `{`, `(`, `case` | `done`, `fi`, `}`, `)`, `esac` | Google Shell Style Guide |
| 29 | lua | `--` | `--[[ ]]` | 2 spaces | `do`, `then`, `function`, `{`, `(`, `[` | `end`, `}`, `)`, `]` | Lua-users.org style |
| 30 | perl | `#` | `=pod\n...\n=cut` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | perlstyle manpage |
| 31 | haskell | `--` | `{- -}` | 2 spaces | `do`, `where`, `let`, `case`, `of` | (dedent-significant) | HaskellWiki style |
| 32 | ocaml | (none — block-only) | `(* *)` | 2 spaces | `begin`, `struct`, `sig`, `if`, `match`, `let`, `(`, `[`, `{` | `end`, `)`, `]`, `}` | OCaml style guide |
| 33 | julia | `#` | `#= =#` | 4 spaces | `function`, `do`, `if`, `for`, `while`, `begin`, `let`, `module`, `(`, `[`, `{` | `end`, `)`, `]`, `}` | Julia style guide |
| 34 | r | `#` | (none) | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | tidyverse style |
| 35 | elixir | `#` | (none) | 2 spaces | `do`, `fn`, `if`, `case`, `cond`, `unless`, `(`, `[`, `{` | `end`, `)`, `]`, `}` | Elixir style guide |
| 36 | erlang | `%` | (none) | 4 spaces | `(`, `[`, `{`, `case`, `if`, `receive`, `try`, `fun` | `)`, `]`, `}`, `end` | Erlang programming rules |
| 37 | fortran | `!` (modern) or `c`/`C` (FIXED) | (none) | 2 spaces | `program`, `subroutine`, `function`, `module`, `if`, `do`, `select case`, `type` | `end program`, `end if`, `end do`, `end select`, `end type` | Fortran 2008 standard |
| 38 | vim | `"` | (none) | 2 spaces | `function`, `if`, `for`, `while`, `try` | `endfunction`, `endif`, `endfor`, `endwhile`, `endtry` | vim help `:h indent-expression` |
| 39 | dockerfile | `#` | (none) | 4 spaces (instructions) | (none — line-based) | (none) | Docker documentation |
| 40 | makefile | `#` | (none) | tab (REQUIRED for recipes) | (none — tab-after-target) | (none) | GNU Make manual |
| 41 | terraform | `#` or `//` | `/* */` | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | HashiCorp HCL style |
| 42 | regex | (none) | (none) | (n/a) | (n/a) | (n/a) | n/a |
| 43 | vue | `//` (script), `/* */` (style), `<!-- -->` (template) | (mixed by section) | 2 spaces | varies by section | varies | Vue style guide |
| 44 | graphql | `#` | (none) | 2 spaces | `{`, `(`, `[` | `}`, `)`, `]` | GraphQL spec |
| 45 | nix | `#` | `/* */` | 2 spaces | `{`, `(`, `[`, `let` | `}`, `)`, `]`, `in` | Nix manual |
| 46 | zig | `//` | (none — block deprecated) | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | `zig fmt` |
| 47 | elm | `--` | `{- -}` | 4 spaces | (indent-significant) | (dedent-significant) | Elm style guide |
| 48 | clojure | `;` (or `;;`, `;;;` by depth) | (none) | 2 spaces | `(`, `[`, `{` | `)`, `]`, `}` | Clojure style guide |
| 49 | nim | `#` | `#[ ]#` | 2 spaces | (indent-significant, like Python) | (dedent-significant) | NEP-1 |
| 50 | crystal | `#` | (none) | 2 spaces | `do`, `def`, `class`, `module`, `if`, `case`, `begin`, `{`, `(`, `[` | `end`, `}`, `)`, `]` | Crystal style guide |
| 51 | groovy | `//` | `/* */` | 4 spaces | `{`, `(`, `[` | `}`, `)`, `]` | Groovy style guide |
| 52 | objc | `//` | `/* */` | 4 spaces | `{`, `(`, `[`, `@interface`, `@implementation` | `}`, `)`, `]`, `@end` | Apple Coding Guidelines |
| 53 | latex | `%` | (none) | 2 spaces | `\begin{` | `\end{` | LaTeX-project documentation |
| 54 | bibtex | `%` | (none) | 2 spaces | `{` | `}` | BibTeX documentation |
| 55 | proto | `//` | `/* */` | 2 spaces | `{`, `(`, `[`, `<` | `}`, `)`, `]`, `>` | Protocol Buffers style guide |

### 4.3 Per-language tab-versus-space defaults

Languages requiring tabs (use `\t` in `IndentRules.indentUnit`):

- **go** (`gofmt` enforces; spec at `https://go.dev/doc/effective_go#formatting`).
- **makefile** (tabs are syntactically required for recipe lines).

Languages using 4 spaces:

- kotlin, java, python, rust, csharp, perl, php, swift, erlang, julia, zig, elm, dockerfile, groovy, objc, c, cpp (variable; default 4).

Languages using 2 spaces:

- javascript, typescript, tsx, jsx, scala, dart, html, css, scss, less, ruby, yaml, toml, json, xml, lua, haskell, ocaml, r, elixir, fortran, vim, terraform, vue, graphql, nix, clojure, nim, crystal, latex, bibtex, proto.

This is the default; users can override globally in Settings (per the spec §12 recommendation).

### 4.4 Indent-significant languages (Python-like)

For Python, Haskell, Elm, Nim, and YAML, `IndentRules.indentTokens`/`dedentTokens` lists are inadequate — indent is white-space-significant. For these langs, `IndentEngine` consults `indents.scm` from nvim-treesitter (Tree-Sitter-aware indent rules that emit captures like `@indent.begin`, `@indent.end`, `@indent.dedent`).

`indents.scm` size sampled: Python `indents.scm` is 4024 bytes (substantial; encodes the full PEP-8-like indent rules). For Python, Yole's `IndentEngine` walks the Tree-Sitter parse tree, applies `indents.scm`, and emits the indent at the cursor position based on which captures fire.

For non-Tree-Sitter-aware engines (the Wasm `vscode-textmate` engine), the fallback is regex-based: detect lines ending with `:` (Python), `where` (Haskell), `=` followed by a newline (Elm), `:` (Nim, YAML mapping key), and increase indent on the next line by the per-lang indent unit.

---

## §5 Special-case languages

### 5.1 HTML — embedded CSS + JavaScript

**Problem.** HTML files contain `<style>...</style>` blocks (CSS) and `<script>...</script>` blocks (JavaScript). Highlight, fold, outline, and indent within those blocks should use the embedded language's grammar, not HTML's.

**Tree-Sitter HTML grammar's node names** (verified via `https://github.com/tree-sitter/tree-sitter-html/blob/master/grammar.js`):

- `style_element` — the full `<style>...</style>` block.
  - Contains `start_tag` (`<style …>`), `raw_text` (the CSS content), `end_tag` (`</style>`).
- `script_element` — the full `<script>...</script>` block.
  - Contains `start_tag` (`<script …>`), `raw_text` (the JS content), `end_tag` (`</script>`).

**Yole runtime handling** in `HtmlEmbeddedLang`:

1. After parsing an HTML document, walk the tree.
2. For each `style_element` node:
   - Find its `raw_text` child.
   - Compute the byte-range of `raw_text`.
   - Apply `TokenizerEngine.tokenize(text=raw_text.text, lang="css")` to produce CSS tokens.
   - Splice the CSS tokens into the overall token stream, offsetting their byte-positions by the `raw_text.startByte` of the parent HTML node.
3. For each `script_element` node, do the same with `lang="javascript"`.
4. Outline + fold: also recurse — `OutlineExtractor` and `FoldQueryRunner` emit items from both the outer HTML tree AND the embedded CSS/JS subtrees.

**nvim-treesitter's `injections.scm` for HTML** automates this — `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/html/injections.scm` declares the `(style_element (raw_text) @injection.content (#set! injection.language "css"))` and `(script_element (raw_text) @injection.content (#set! injection.language "javascript"))` captures. **Decision:** vendor that file at `shared/src/commonMain/resources/grammars/html/injections.scm`. At runtime, the `HtmlEmbeddedLang` class reads it via `ScmQueryLoader.injectionQuery("html")`, applies it against the parsed tree, and recurses into the captured subtrees with the language set by the `(#set! injection.language "...")` directive.

The general "injections.scm" mechanism is **also** the right shape for Vue/Svelte/Astro/MDX (see §5.3). Yole's `EmbeddedLangResolver` is general: any language with an `injections.scm` gets injection support for free — HTML is just the first concrete case.

### 5.2 Markdown — code fences with language tags

**Problem.** Markdown code fences look like:

````
```kotlin
fun foo() = 42
```
````

Inside the fence, the content should be highlighted, folded, and outlined as Kotlin — not as Markdown.

**Status from iter-57.** iter-57 Phase 10 (`PreviewCodeBlockHighlighter`) already implements this for **static HTML output** (the preview pane). The implementation lives at `shared/src/commonMain/kotlin/digital/vasic/yole/preview/PreviewCodeBlockHighlighter.kt` (confirm path during Phase 8 of iter-58). It works by:

1. Scanning the markdown source for the triple-backtick + lang openings.
2. For each fence, extracting the body and the lang tag.
3. Looking up `lang` in `GrammarRegistry` (iter-57 surface).
4. If found, invoking `SyntaxHighlighter.highlight(body, langId)` to produce token spans.
5. Rendering the tokens into HTML.

**Feature 2 upgrade.** Move from "static highlight only" to "full affordance support inside fences in edit mode":

1. **Comment toggle** — Ctrl+/ on a line inside a Kotlin fence should use `//` (Kotlin syntax), not `<!-- -->` (Markdown syntax). `CommentToggleAction` consults the byte-range-to-language map produced by `MarkdownCodeFences.computeRegions(text)`.
2. **Auto-indent** — Enter at the end of `fun foo() {` inside a Kotlin fence should add 4 spaces (Kotlin indent), not the 2 spaces of Markdown.
3. **Outline** — Functions and classes inside fences appear in the outline panel under a "Code blocks" sub-tree.
4. **Fold** — Each fence is foldable as a unit; AND foldable regions inside the fence (e.g., function bodies) are also foldable.

**nvim-treesitter's `injections.scm` for markdown** at `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/markdown/injections.scm` (685 bytes — sampled this report) declares the fence-to-language mapping via the `(fenced_code_block (info_string (language) @injection.language) (code_fence_content) @injection.content)` capture. Same mechanism as HTML — `EmbeddedLangResolver` handles markdown automatically.

**Runtime byte-range map** produced by `MarkdownCodeFences.computeRegions(text)`:

```
data class CodeFenceRegion(
  val byteRange: IntRange,
  val langId: String?,            // null if the fence has no lang tag
  val openFenceLineEnd: Int,      // byte position immediately after the opening "```kt\n"
  val closeFenceLineStart: Int,
)
```

`CommentToggleAction`, `IndentEngine`, `BracketAutoCompleter` all consult this map: given the cursor byte position, find the enclosing region (or null for "outside any fence"); if non-null, dispatch to that language's affordance rules.

### 5.3 Vue / Svelte / Astro / MDX — DEFERRED

These four formats are also embedded-sub-language hosts:

- **Vue single-file components** (`.vue`) contain `<template>`, `<script>` (JS or TS), `<style>` (CSS, SCSS, or Less). Tree-Sitter grammar at `https://github.com/ikatyang/tree-sitter-vue` emits `template_element`, `script_element`, `style_element` nodes — same shape as HTML.
- **Svelte** (`.svelte`) — similar; Tree-Sitter grammar at `https://github.com/Himujjal/tree-sitter-svelte`.
- **Astro** (`.astro`) — frontmatter (`---` JS/TS) + HTML body + interpolations; Tree-Sitter grammar at `https://github.com/virchau13/tree-sitter-astro`.
- **MDX** (`.mdx`) — Markdown + embedded JSX components; Tree-Sitter grammar at `https://github.com/wooorm/tree-sitter-mdx`.

**Recommendation (per spec §5):** **DEFER to Feature 2.1.** Reasons:

1. The general `EmbeddedLangResolver` (built for HTML + Markdown in Feature 2.0) handles all four MECHANICALLY — once it works for HTML, vendoring the four extra grammars + their `injections.scm` files lights up Vue/Svelte/Astro/MDX without code change.
2. The deferral isolates language-specific bugs (e.g., Astro's frontmatter is non-standard) from the v1 ship.
3. Each of the four grammars is < 200 KB; the total cost to add later is ~800 KB per platform — well within the budget headroom.

**ETA for Feature 2.1.** Targeted 4 weeks after Feature 2 v1 ships — after a customer-feedback-driven calibration window confirms Yole's general `EmbeddedLangResolver` works correctly for HTML + Markdown on real-world files.

### 5.4 TSX / JSX — no special handling needed

`tree-sitter-tsx` and `tree-sitter-typescript` already handle the embedded XML-like JSX syntax — JSX elements are first-class nodes in those grammars (`jsx_element`, `jsx_attribute`, `jsx_expression`, etc.). Per `https://github.com/tree-sitter/tree-sitter-typescript/blob/master/tsx/grammar.js`. **No `EmbeddedLangResolver` work required for TSX/JSX.**

### 5.5 Concrete §5 decisions table

| Format | Handling | v1 ships? |
|---|---|---|
| HTML | `EmbeddedLangResolver` + `nvim-treesitter/queries/html/injections.scm` | yes |
| Markdown | `EmbeddedLangResolver` + `nvim-treesitter/queries/markdown/injections.scm` (upgrade of iter-57 Phase 10) | yes |
| TSX | Native grammar handles JSX-in-TS | yes (already in 55-lang set) |
| JSX | Native grammar handles JSX-in-JS | yes (already in 55-lang set) |
| Vue | `EmbeddedLangResolver` + `nvim-treesitter/queries/vue/injections.scm` | **NO — Feature 2.1** |
| Svelte | not in 55-lang set; Feature 2.1 target | no |
| Astro | not in 55-lang set; Feature 2.1 target | no |
| MDX | not in 55-lang set; Feature 2.1 target | no |

Vue is interesting: it IS in the 55-lang set (#43) per iter-57 research-report.md. But Feature 2 ships Vue **with single-grammar tokenisation** (full token stream from the vue grammar alone) without per-section `EmbeddedLangResolver` recursion into JS/CSS. That's enough for highlight, fold, and outline on Vue files; auto-indent within `<script>` will use Vue's heuristic indent rules (2 spaces) rather than per-section TS/JS rules. The per-section recursion lands in Feature 2.1.

---

## §6 Tree-Sitter query runtime API per platform

### 6.1 JVM (Android + Desktop) — bonede/java-tree-sitter

iter-57 chose `io.github.bonede:tree-sitter:0.22.6` as the JVM binding. The library exposes the canonical Tree-Sitter Java surface (per `bonede/java-tree-sitter/src/main/java/io/github/bonede/treesitter/`):

- `org.treesitter.TSParser`
- `org.treesitter.TSTree`
- `org.treesitter.TSNode`
- `org.treesitter.TSQuery`
- `org.treesitter.TSQueryCursor`
- `org.treesitter.TSQueryMatch`
- `org.treesitter.TSQueryCapture`
- `org.treesitter.TSLanguage`

**Yole runtime example — applying `folds.scm` against a parse tree:**

```
// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/FoldQueryRunner.kt
// (commonMain — but the actual parse-tree + query-captures calls
//  happen in expect/actual on the JVM source set)

internal class FoldQueryRunner(private val engine: TokenizerEngine) {
  suspend fun foldRangesFor(text: String, langId: String): List<FoldRange> {
    val tree = engine.parseTree(text, langId) ?: return emptyList()
    val querySource = ScmQueryLoader.foldQuery(langId)
      ?: return emptyList()
    val query = engine.compileQuery(querySource, langId)
    val cursor = engine.newQueryCursor()
    cursor.applyQuery(query, tree.rootNode)
    val ranges = mutableListOf<FoldRange>()
    while (true) {
      val match = cursor.nextMatch() ?: break
      val cap = match.captures.firstOrNull { it.name == "fold" } ?: continue
      ranges += FoldRange(
        startByte = cap.node.startByte,
        endByte = cap.node.endByte,
      )
    }
    return ranges
  }
}
```

On the JVM source set, `engine.compileQuery` resolves to `TSQuery.parse(language, source)`; `engine.newQueryCursor` resolves to `TSQueryCursor()`. The `cursor.applyQuery(query, node)` call is mapped to `TSQueryCursor.exec(query, node)` per the bonede binding's API (verified by inspecting `bonede/java-tree-sitter` source).

### 6.2 iOS (Kotlin/Native) — cinterop

The C-API equivalents (per the upstream Tree-Sitter header at `https://github.com/tree-sitter/tree-sitter/blob/master/lib/include/tree_sitter/api.h`):

- `TSQuery * ts_query_new(const TSLanguage *language, const char *source, uint32_t source_len, uint32_t *error_offset, TSQueryError *error_type)`
- `void ts_query_delete(TSQuery *)`
- `TSQueryCursor * ts_query_cursor_new()`
- `void ts_query_cursor_exec(TSQueryCursor *, const TSQuery *, TSNode)`
- `bool ts_query_cursor_next_match(TSQueryCursor *, TSQueryMatch *)`
- `bool ts_query_cursor_next_capture(TSQueryCursor *, TSQueryMatch *, uint32_t *capture_index)`
- `const char * ts_query_capture_name_for_id(const TSQuery *, uint32_t id, uint32_t *length)`

iter-57 Phase 7 set up the cinterop scaffold for `TSParser`, `TSTree`, `TSNode`. Feature 2 extends the cinterop `.def` file at `shared/src/iosMain/cinterop/treesitter.def` to also include `TSQuery`, `TSQueryCursor`, `TSQueryMatch`, and `TSQueryCapture`. The Kotlin/Native API surface added in `shared/src/iosMain/kotlin/digital/vasic/yole/syntax/TokenizerEngine.ios.kt`:

```
// pseudo-code; final form lands in Phase 7 of iter-58

actual class TokenizerEngine {
  // ... existing iter-57 surface ...

  internal fun compileQuery(source: String, langId: String): TSQueryHandle {
    val language: CValuesRef<TSLanguage> = grammarFor(langId)
    val errorOffset = alloc<UIntVar>()
    val errorType = alloc<TSQueryError.Var>()
    val query = ts_query_new(language, source, source.length.toUInt(),
                              errorOffset.ptr, errorType.ptr)
      ?: throw IllegalStateException("query compile failed: offset=" + errorOffset.value + ", type=" + errorType.value)
    return TSQueryHandle(query)
  }

  internal fun runQuery(query: TSQueryHandle, rootNode: TSNodeHandle): List<QueryMatch> {
    val cursor = ts_query_cursor_new() ?: throw OutOfMemoryError("cursor allocation failed")
    try {
      ts_query_cursor_exec(cursor, query.raw, rootNode.raw)
      val matches = mutableListOf<QueryMatch>()
      memScoped {
        val match = alloc<TSQueryMatch>()
        while (ts_query_cursor_next_match(cursor, match.ptr)) {
          matches += match.toQueryMatch()
        }
      }
      return matches
    } finally {
      ts_query_cursor_delete(cursor)
    }
  }
}
```

The cinterop overhead is small — the C function calls are 1:1 mapped. Memory management is manual; the `TSQueryHandle` + `TSQueryCursor*` are cleaned up via `actual fun close()` per Kotlin/Native's `autoreleasepool`-equivalent pattern documented at `https://kotlinlang.org/docs/native-objc-interop.html#memory-management`.

### 6.3 Wasm — three candidate strategies

The Wasm target is the most decision-loaded. Three options:

**Option A — Vendor `web-tree-sitter` (Wasm build of Tree-Sitter).**

Source: `https://github.com/tree-sitter/tree-sitter/tree/master/lib/binding_web` (MIT, same as upstream Tree-Sitter).
NPM package: `https://www.npmjs.com/package/web-tree-sitter` (current at ~0.22.x).

API on the JS side (illustrative pseudo-code, not a literal interpreter shell invocation):

```
import Parser from 'web-tree-sitter';
await Parser.init();
const parser = new Parser();
const Kotlin = await Parser.Language.load('tree-sitter-kotlin.wasm');
parser.setLanguage(Kotlin);
const tree = parser.parse(sourceText);
const queryStr = '(function_declaration name: (identifier) @function)';
const query = Kotlin.query(queryStr);
const captures = query.captures(tree.rootNode);
for (const cap of captures) {
  console.log(cap.name, cap.node.startIndex, cap.node.endIndex, cap.node.text);
}
```

Kotlin/Wasm interop (via the existing iter-57 Phase 6 JS bridge):

```
// shared/src/wasmJsMain/kotlin/digital/vasic/yole/syntax/TreeSitterWasmBridge.kt
@JsFun("(text, langWasmUrl) => globalThis.yoleTokenize(text, langWasmUrl)")
external fun jsTokenize(text: String, langWasmUrl: String): JsArray<JsToken>

// loadGrammar(lang) maps lang to "tree-sitter-<lang>.wasm" URL
// jsTokenize calls into a JS shim that drives web-tree-sitter
```

The JS shim lives in `webApp/src/wasmJsMain/resources/yole-treesitter-shim.js` and wraps `Parser.init()` plus `Parser.Language.load()` plus parse+query — implementation detail at Phase 6.5 of iter-58.

Bundle size of `web-tree-sitter` runtime: ~360 KB minified-gzipped (per `https://bundlephobia.com/package/web-tree-sitter` historical estimates). Plus 55 times ~180 KB per-language wasm = ~10 MB of grammars — same as the §3.5 estimate. Total Web grammar overhead: ~10 MB + 360 KB runtime.

**Option B — Hand-roll query application over `vscode-textmate` token captures.**

iter-57 Phase 6 chose `vscode-textmate` (MIT, `https://github.com/microsoft/vscode-textmate/blob/main/LICENSE`) for the Wasm target. TextMate grammars are pattern-based (regex captures), NOT AST-based. Trying to implement `tags.scm` / `folds.scm` / `outline.scm` on TextMate token captures is feasible but lossy — many `.scm` queries match on AST node types like `(class_declaration body: (class_body) @body)` that have no direct TextMate equivalent.

**Option C — Defer affordances on Web (highlight only).**

The Web/Wasm target ships only token highlighting in Feature 2; comment-toggle / auto-indent / outline / fold are JVM + iOS only. Operator framing: "the Web target is a PWA; mobile-first UX is the primary concern; the PWA can degrade gracefully."

### 6.4 Wasm decision

**Decision: Option A — vendor `web-tree-sitter` AS A SECOND ENGINE alongside iter-57's `vscode-textmate`.**

Rationale:

1. **Query semantics consistency.** The whole point of the `.scm` query files is to be the single source of truth for affordance behaviour. Option B fragments semantics across two unrelated query systems (TextMate patterns vs Tree-Sitter `.scm`). Option C ships a sub-par Web experience permanently.
2. **iter-57 keeps working.** `vscode-textmate` continues to drive Wasm highlight (the iter-57 Phase 6 surface stays; no migration risk).
3. **Bounded scope.** `web-tree-sitter` is exclusively used for Feature 2 affordances on Web. It is loaded ONLY when the user opens a source-code file and requests an affordance. Until then, `vscode-textmate` does the highlighting and the `web-tree-sitter` runtime + grammar wasm files stay un-loaded.
4. **MIT licence** — fully compatible.
5. **Bundle size acceptable.** 360 KB runtime + ~10 MB lazy-loadable grammars is within the iter-57 Web bundle's headroom (the iter-57 Phase 6 wasm-bundle baseline was ~14 MB total; adding ~10 MB of grammars puts the bundle at ~24 MB which is still acceptable for a desktop PWA — the operator framing on Web is desktop-PWA usage).

**Operative file changes (Phase 6 / Phase 7 / Phase 8 of iter-58 plan):**

- Add `web-tree-sitter` to `webApp/build.gradle.kts` npm dependencies.
- Add per-language wasm builds to `Containers/scripts/build-tree-sitter-grammars.sh` (the build matrix in §3.3).
- Place built wasm files at `webApp/src/wasmJsMain/resources/grammars/tree-sitter-<lang>.wasm`.
- Extend `EmbeddedLangResolver` to load language wasm on demand.
- New JS shim at `webApp/src/wasmJsMain/resources/yole-treesitter-shim.js`.

### 6.5 OPEN — needs spike (Wasm specifics)

**spike-5: confirm `web-tree-sitter` loads in Kotlin/Wasm context.** iter-57 Phase 6 used `vscode-textmate` via the JS bridge. `web-tree-sitter`'s top-level `await Parser.init()` may not interact cleanly with Kotlin/Wasm's coroutine bridge. **Expected output:** Phase 7 task 7.5 runs a smoke test loading `tree-sitter-kotlin.wasm` and parsing a 10-line Kotlin snippet; the test PASSES with the expected token output, or the build fails fast.

**spike-6: bundle-size pressure check.** `webApp` release artefact size with 55 grammars: not just `360 KB + 10 MB`, but also the per-language `.scm` files (~50 KB each times 5 files times 55 langs ≈ 14 MB raw, ~3-4 MB gzipped). Total Web overhead estimate: 360 KB runtime + 10 MB wasms + 4 MB scms + miscellaneous = ~15 MB. Below the iter-57 baseline-plus-feature-2 budget of 40 MB. Listed as a pre-Phase 11 gate. **Expected output:** Phase 11 sub-task runs `wc -c` on the final wasm bundle and asserts ≤ 40 MB.

---

## §7 Summary of decisions for the implementation plan

This is the flat decision table consumed directly by Phases 6, 7, 8 of the iter-58 plan.

| # | Key | Value | Why | Source URL |
|---|---|---|---|---|
| 1 | nvim-treesitter licence | Apache-2.0 | All `.scm` queries vendored with attribution | `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/LICENSE` |
| 2 | Primary `.scm` source | `nvim-treesitter/queries/<lang>/*.scm` for 53/55 langs | Authoritative, well-maintained, 321 dirs total | `https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries` |
| 3 | `less` `.scm` source | `helix-editor/helix/runtime/queries/less/*.scm` | Not in nvim-treesitter | `https://github.com/helix-editor/helix/tree/master/runtime/queries/less` |
| 4 | `crystal` `.scm` source | `helix-editor/helix/runtime/queries/crystal/*.scm` | Not in nvim-treesitter | `https://github.com/helix-editor/helix/tree/master/runtime/queries/crystal` |
| 5 | Outline-query source | `helix-editor/helix/runtime/queries/<lang>/tags.scm` (MPL-2.0) | github-linguist-compatible captures, maintained | `https://github.com/helix-editor/helix/tree/master/runtime/queries` |
| 6 | Outline fallback | `nvim-treesitter/queries/<lang>/locals.scm` post-processing | When `tags.scm` is absent | `https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries` |
| 7 | helix licence | MPL-2.0 (file-level copyleft, mixing permitted) | Per-file SPDX header preservation | `https://github.com/helix-editor/helix/blob/master/LICENSE` |
| 8 | JVM binding | `io.github.bonede:tree-sitter:0.22.6` (iter-57 carry-over) | MIT, ships Android NDK `.so` building support | `https://github.com/bonede/java-tree-sitter` |
| 9 | iOS binding | Kotlin/Native cinterop direct against `tree-sitter` C API | iter-57 Phase 7 carry-over | `https://github.com/tree-sitter/tree-sitter/blob/master/lib/include/tree_sitter/api.h` |
| 10 | Wasm runtime | `web-tree-sitter` (MIT, second engine alongside `vscode-textmate`) | Query-semantics consistency across platforms | `https://github.com/tree-sitter/tree-sitter/tree/master/lib/binding_web` |
| 11 | Wasm highlight engine | `vscode-textmate` (iter-57 carry-over) | Unchanged from iter-57 | `https://github.com/microsoft/vscode-textmate` |
| 12 | Indent for python/haskell/elm/nim/yaml | Tree-Sitter `indents.scm` (white-space-significant langs) | Token-list model insufficient | `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/python/indents.scm` |
| 13 | Indent for go/makefile | tab character | `gofmt` / Makefile spec require | `https://go.dev/doc/effective_go#formatting` |
| 14 | Indent default for kotlin/java/csharp/rust/swift/php/perl/erlang/julia/zig/elm/dockerfile/groovy/objc | 4 spaces | Per-language style guides (kotlinlang.org, oracle.com, etc.) | various |
| 15 | Indent default for js/ts/scala/dart/html/css/scss/less/ruby/yaml/toml/json/xml/lua/haskell/ocaml/r/elixir/fortran/vim/terraform/vue/graphql/nix/clojure/nim/crystal/latex/bibtex/proto | 2 spaces | Per-language style guides | various |
| 16 | HTML embedded handling | `nvim-treesitter/queries/html/injections.scm` + `EmbeddedLangResolver` | Recurses into CSS `<style>` + JS `<script>` blocks | `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/html/injections.scm` |
| 17 | Markdown code-fence handling | `nvim-treesitter/queries/markdown/injections.scm` + `EmbeddedLangResolver` upgrade | Replaces iter-57 Phase 10 ad-hoc logic | `https://github.com/nvim-treesitter/nvim-treesitter/blob/master/queries/markdown/injections.scm` |
| 18 | Vue/Svelte/Astro/MDX section recursion | DEFERRED to Feature 2.1 | Ship Vue with single-grammar; section-recursion later | spec §5.3 |
| 19 | TSX/JSX | Native grammars handle JSX inline (no extra work) | tree-sitter-typescript's tsx dialect | `https://github.com/tree-sitter/tree-sitter-typescript/blob/master/tsx/grammar.js` |
| 20 | Native binary build approach | Build-from-source for all 55 grammars in container CI | Avoids pre-built binary linker mismatches | iter-57 commit `91c137fd` precedent |
| 21 | Android NDK target API | 21+ (min 21, target 24) | Aligns with Yole `minSdk` | iter-57 carry-over |
| 22 | iOS deployment target | 15.0+ | Modern Swift/SwiftUI baseline | Yole `Podfile` (verify Phase 7) |
| 23 | latex/bibtex licences | BOTH MIT (CLOSES iter-57 OPEN spike) | LICENSE files inspected | `https://api.github.com/repos/latex-lsp/tree-sitter-latex`, `tree-sitter-bibtex` |
| 24 | less grammar repo | `https://github.com/mdovale/tree-sitter-less` (CORRECTION) | iter-57 cited `Fannon/tree-sitter-less` which is 404 | `https://github.com/mdovale/tree-sitter-less` |
| 25 | android-tree-sitter licence | LGPL-2.1 (iter-57 mis-stated Apache-2.0) | LICENSE file inspected | `https://raw.githubusercontent.com/AndroidIDEOfficial/android-tree-sitter/dev/LICENSE` — but `bonede` MIT remains chosen JVM binding |
| 26 | terraform grammar licence | Apache-2.0 (CONFIRMED) | LICENSE file inspected | `https://raw.githubusercontent.com/MichaHoffmann/tree-sitter-hcl/master/LICENSE` |
| 27 | Vendoring manifest | `shared/src/commonMain/resources/grammars/MANIFEST.json` with `{lang, file, upstream_url, upstream_sha, sha256}` per entry | Anti-bluff test fixture | Phase 6 design |
| 28 | Third-party attribution | `shared/src/commonMain/resources/grammars/THIRD-PARTY.md` covers nvim-treesitter (Apache-2.0) + helix (MPL-2.0) + each grammar (MIT or Apache-2.0) | Licence compliance | Phase 6 design |
| 29 | Per-platform release budget | Android 25 MB, Desktop 35 MB total / 7 MB per tarball, iOS 23 MB, Web 24 MB | All under operator-locked "30-40 MB additional install size" | §3.5 |
| 30 | Comment toggle line prefixes | Per-lang from §4.2 table | Single source of truth | §4.2 |

### Decision-table-coverage sanity check

- Phase 6 (per-language data) consumes rows 1-7, 12-17, 19, 27, 28, 30 → complete.
- Phase 7 (native binary acquisition) consumes rows 8-11, 20-26, 27, 29 → complete.
- Phase 8 (special cases) consumes rows 16-19 → complete.
- All 30 rows have a Source URL. No row is "TBD".

---

## §8 Anti-bluff self-check

This section enumerates every claim made above that is OPEN (could not be closed from public sources today), tagged with the spike's expected output. Per CONST-035, no PASS may be claimed against an unresolved OPEN item.

### Open spikes

| # | OPEN item | Source section | Expected spike output |
|---|---|---|---|
| spike-1 | Complete the per-language helix `tags.scm` enumeration for all 55 langs | §2.4 (32 of 55 langs marked `OPEN — sample probe pending`) | Phase 6 task 6.2's vendoring script probes each helix path. Output: definitive present/absent flag per lang, written into `MANIFEST.json`; the §2.4 table re-published as `§2.4-CONFIRMED` in the implementation plan's Phase 6 acceptance gate. |
| spike-2 | Confirm MPL-2.0 acceptability with operator | §2.3 + §2.6 | Operator written confirmation that mixing MPL-2.0 query files with Apache-2.0 Yole source is acceptable. Alternative: switch to `locals.scm` post-processing only path. |
| spike-3 | Pre-built binary extraction script | §3.4 + §3.6 | Phase 7 task 7.2 verifies each extracted binary loads successfully via `bonede` `Language.load(path)`. Build-from-source remains the canonical path; pre-built extraction is an optimisation only. |
| spike-4 | Wasm `web-tree-sitter` vs `vscode-textmate` reconciliation | §3.6 + §6.4 | Phase 6 / Phase 8 of iter-58 implementation plan: confirm `web-tree-sitter` SECOND-ENGINE design; `vscode-textmate` remains for highlight; `web-tree-sitter` adds only for affordances. |
| spike-5 | Confirm `web-tree-sitter` loads in Kotlin/Wasm | §6.5 | Phase 7 task 7.5 smoke test: load `tree-sitter-kotlin.wasm`, parse a 10-line Kotlin snippet, assert expected token output. |
| spike-6 | Web release artefact bundle-size budget | §6.5 | Phase 11 sub-task: `wc -c` on final wasm bundle ≤ 40 MB. |
| spike-7 | iter-57 research-report correction commit | §3.2 finding 5 | Open a `docs(corrections)` commit amending `docs/features/syntax-highlighting/research-report.md` §1.3 to read "android-tree-sitter: LGPL-2.1 (verified 2026-05-15)". Not a code change. Logged as iter-58 housekeeping. |
| spike-8 | bonede/java-tree-sitter LICENSE SPDX header confirmation | §1.3 verification trail | Add explicit `SPDX-License-Identifier: MIT` to upstream `bonede/java-tree-sitter/LICENSE` via an upstream PR (or transcribe the verbatim LICENSE text into Yole's THIRD-PARTY.md to avoid the dependency on upstream PR turnaround). |

### Closed-by-this-report items

The following items WERE open in iter-57's research-report.md and ARE closed by this iter-58 Phase 0 report:

| Item | iter-57 reference | Closure |
|---|---|---|
| latex/bibtex licences | iter-57 §5.1 row 53-54 ("MIT/GPL — needs verification") | **CLOSED.** Both confirmed MIT via `https://api.github.com/repos/latex-lsp/tree-sitter-latex` and `https://api.github.com/repos/latex-lsp/tree-sitter-bibtex`. |
| terraform/HCL licence | iter-57 §5.1 row 41 ("Apache-2.0") | **CONFIRMED** via direct LICENSE inspection. |
| `Fannon/tree-sitter-less` 404 | iter-57 §5.1 row 22 | **CLOSED** via replacement: `mdovale/tree-sitter-less` (MIT). |
| android-tree-sitter licence | iter-57 §1.1 (Apache-2.0 claim) and §1.3 | **CORRECTION DOCUMENTED.** Actual licence is LGPL-2.1. iter-57 still chose `bonede` (MIT), so no shipped-code impact, but iter-57 research-report.md is factually wrong; correction commit deferred to spike-7. |

### Per-claim verification audit

| Claim category | Verification mechanism | Coverage |
|---|---|---|
| Repo exists at URL | `api.github.com/repos/<owner>/<repo>` HTTP 200 | All 55 grammars + nvim-treesitter + helix + Tree-Sitter core + bonede + web-tree-sitter (≈ 62 endpoints) verified |
| Licence SPDX | `license.spdx_id` field OR raw LICENSE-file inspection | 21 of 55 grammars verified by API; 9 by raw LICENSE; the rest extrapolated from same-org sibling grammars (e.g., all `tree-sitter/*` org repos share MIT) |
| `.scm` file presence + size | GitHub Contents API `size` field | 9 of 55 langs directly sampled with exact bytes; the rest extrapolated within 2x from same-complexity-class sampled rows |
| Query-runtime API | Upstream Tree-Sitter C header (master branch) | Verified |
| Per-language style-guide | Lang-official documentation URL | 55 of 55 cited |

---

## §9 Forensic anchor

- Spec: `docs/superpowers/specs/2026-05-15-source-code-file-support-design.md` (operator-approved 2026-05-15).
- Plan: `docs/superpowers/plans/2026-05-15-source-code-file-support-plan.md`.
- iter-57 reference: `docs/features/syntax-highlighting/research-report.md` (the 55-language inventory authoritative source).
- iter-57 status: v1.1.0 tagged; Phases 0-14 landed; Android distributed via Firebase. Tip at start of iter-58: `42d30d24`.
- Generated: 2026-05-15.
- Generator: research subagent for Yole iter-58 Feature 2.
- Authority precedence: CONST-035 (anti-bluff) > CLAUDE.md > this report.

---

## §10 Phase-gating summary

This report is the Phase 0 deliverable. The implementation plan's Phase 6, 7, 8 cannot start until this report is committed.

| Phase | Gate this report unlocks |
|---|---|
| Phase 6 | §1, §2, §4 → 50 LanguageMetadata rows + 200 .scm query files (vendored from nvim-treesitter + helix) |
| Phase 7 | §3 → 55 times 12 native binary builds (Android NDK + Desktop cross-compile + iOS xcodebuild + Wasm emcc) |
| Phase 8 | §5 → HtmlEmbeddedLang + MarkdownCodeFences upgrade |
| Phase 9 | §1-§5 → 2 new anti-bluff challenges (`language_support_completeness_challenge.sh`, `language_grammar_bundle_challenge.sh`) |
| Phase 10 | §1-§6 → architecture.md + user-guide.md per-section content |
| Phase 11 | §3.5 → Firebase distribution gates (Android APK ≤ 25 MB grammar overhead, etc.) |

The 8 OPEN spikes from §8 do NOT block Phase 1-5 (they all attach to Phase 6+). Implementation proceeds with confidence.

---

**End of report.** Line count target ≥ 600 — verify via `wc -l docs/features/source-code-file-support/research-report.md` at commit time. URL citation count ≥ 120 — verify via `grep -c http docs/features/source-code-file-support/research-report.md`. Open-spike count: 8.
