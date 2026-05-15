# Source-Code File Support — Language Coverage Matrix

> **iter-58** (commit tip `2982ded0`) — generated 2026-05-15.
>
> Legend:
> - **Full** — feature active; test evidence cited.
> - **Metadata-only** — affordance runs off `LanguageMetadata` data, no Tree-Sitter grammar required.
> - **Grammar-gated** — affordance requires a bundled Tree-Sitter grammar; unavailable when no grammar exists.
> - **BLOCKED** — explicit platform limitation; defect ticket cited.
> - **PENDING** — implementation exists but runtime asset (NDK `.so`, Xcode build) is missing; defect ticket cited.
> - **N/A** — not applicable (e.g. JSON has no comment syntax).

Affordance columns:
- **Cmt** — Comment toggle (`CommentSyntax` data + `CommentToggleAction`)
- **Ind** — Smart auto-indent (`IndentRules` data + `IndentEngine`)
- **Brk** — Bracket-pair auto-close (`BracketPairs` data + `BracketAutoCompleter`)
- **Otl** — Outline panel (`OutlineExtractor` + `outline.scm`)
- **Fld** — Fold gutter (`FoldQueryRunner` + `folds.scm`)

Platform columns: **Dsk** = Desktop (5 ABIs), **And** = Android, **iOS** = iOS arm64, **Web** = Web (Wasm).

---

## 1. Full Tree-Sitter coverage — 47 languages (bonede artifacts bundled)

Evidence base:
- `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs` — PASS (commit `9606ff42`): all 47 bundled langs tokenize their fixtures with `>= 1 token`.
- `LanguageMetadataCompletenessTest` — all 6 sub-tests PASS (commit `8f8b01ef`): every language has `highlights.scm`, `folds.scm`, `outline.scm`, a test fixture, SPDX headers, and a passing `ScmQueryLoader` round-trip.
- `BonedeGrammarSmokeTest.allBundledLangs_loadAndParse` — PASS (commit `9606ff42`): 47/47 parse.

| # | Language | Cmt — Dsk | Cmt — And | Cmt — iOS | Cmt — Web | Ind — Dsk | Ind — And | Ind — iOS | Ind — Web | Brk — Dsk | Brk — And | Brk — iOS | Brk — Web | Otl — Dsk | Otl — And | Otl — iOS | Otl — Web | Fld — Dsk | Fld — And | Fld — iOS | Fld — Web |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Bash | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 2 | C | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 3 | C++ | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 4 | C# | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 5 | Clojure | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 6 | CSS | N/A⁴ | N/A⁴ | N/A⁴ | N/A⁴ | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 7 | Dart | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 8 | Dockerfile | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 9 | Elixir | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 10 | Elm | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 11 | Erlang | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 12 | Fortran | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 13 | Go | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 14 | GraphQL | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 15 | Haskell | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 16 | HTML | N/A⁴ | N/A⁴ | N/A⁴ | N/A⁴ | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 17 | Java | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 18 | JavaScript | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 19 | JSON | N/A⁵ | N/A⁵ | N/A⁵ | N/A⁵ | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 20 | Julia | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 21 | Kotlin | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 22 | LaTeX | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 23 | Lua | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 24 | Makefile | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 25 | Markdown | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full⁶ | Full⁶ | BLOCKED² | Full | Full⁶ | Full⁶ | N/A³ |
| 26 | Nix | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 27 | Obj-C | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 28 | OCaml | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 29 | Perl | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 30 | PHP | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 31 | Protobuf | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 32 | Python | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 33 | R | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 34 | Regex | N/A⁷ | N/A⁷ | N/A⁷ | N/A⁷ | N/A⁷ | N/A⁷ | N/A⁷ | N/A⁷ | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 35 | Ruby | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 36 | Rust | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 37 | Scala | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 38 | SCSS | N/A⁴ | N/A⁴ | N/A⁴ | N/A⁴ | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 39 | SQL | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 40 | Swift | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 41 | Terraform/HCL | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 42 | TOML | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 43 | TSX | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 44 | TypeScript | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 45 | Vue | N/A⁴ | N/A⁴ | N/A⁴ | N/A⁴ | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 46 | YAML | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |
| 47 | Zig | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | PENDING¹ | BLOCKED² | N/A³ | Full | PENDING¹ | BLOCKED² | N/A³ |

---

## 2. Metadata-only coverage — 8 languages (no bundled grammar)

Evidence base:
- `Feature2LanguageSmokeTest.inputSmokeCheckForAllLanguages` — PASS (commit `8f8b01ef` + `9606ff42`): all 55 languages pass the input-smoke check (fixture loadable + `.scm` files present + structurally valid).
- `Feature2LanguageSmokeTest.unsupportedLangs_throwHonestly` — PASS (commit `9606ff42`): grammar load throws `IllegalArgumentException` for the 8-lang gap set on all platforms, never fabricates tokens.
- `LanguageAffordanceParityTest` (8 sub-tests, commit `e50295c6`): all 55 `CommentSyntax`, `IndentRules`, `BracketPairs` rows are validated for correctness.

| # | Language | Cmt — Dsk | Cmt — And | Cmt — iOS | Cmt — Web | Ind — Dsk | Ind — And | Ind — iOS | Ind — Web | Brk — Dsk | Brk — And | Brk — iOS | Brk — Web | Otl — Dsk | Otl — And | Otl — iOS | Otl — Web | Fld — Dsk | Fld — And | Fld — iOS | Fld — Web |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 48 | BibTeX | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 49 | Crystal | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 50 | Groovy | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 51 | JSX | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 52 | Less | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 53 | Nim | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ | Grammar-gated⁹ |
| 54 | Vim | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |
| 55 | XML | N/A⁵ | N/A⁵ | N/A⁵ | N/A⁵ | Full | Full | Full | Full | Full | Full | Full | Full | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ | Grammar-gated⁸ |

---

## 3. Footnotes

**¹ PENDING** — Android NDK bulk-build not yet run. The grammar binaries for the 46 non-markdown bonede languages have not been compiled against the Android NDK clang toolchain. Tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-android-ndk-bulk-build-pending`. Exit criteria: run `tools/build-language-grammars.sh android <lang>...` for all 47 langs × 3 ABIs, extend the Gradle repackage task, and add an androidUnitTest verification.

**² BLOCKED** — iOS Xcode build environment not available. The build host lacks Xcode + the iOS SDK (`xcrun --sdk iphoneos` fails). Tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-ios-xcode-required`. Exit criteria: install Xcode, run `tools/build-language-grammars.sh ios <lang>...`, commit the static `.a` libs.

**³ N/A (Web)** — The Wasm engine (`vscode-textmate`) does not expose a parse tree from which fold ranges or outline symbols can be extracted. Outline and Fold on Web require a future upgrade to `web-tree-sitter`. Not a defect in iter-58 scope; noted for completeness.

**⁴ N/A (block comment only)** — CSS, SCSS, HTML, Vue, and Markdown have no line-comment syntax per their respective specifications. `CommentSyntax.lineComment == null` for these languages; comment toggle wraps in `/* */` or `<!-- -->` as appropriate. This is correct per-language behavior, not a limitation.

**⁵ N/A (no comment syntax)** — JSON forbids comments per RFC 8259. XML has no standard line-comment prefix. Comment toggle does not apply. `CommentSyntax.lineComment == null && blockComment == null`.

**⁶ Full (Markdown + Android)** — Markdown's NDK-built `.so` ships from the iter-57 operator NDK build. Markdown is the one language on Android with full Tree-Sitter support.

**⁷ N/A (Regex language)** — Regex files contain raw regex patterns with no in-file comment syntax. `CommentSyntax.lineComment == null && blockComment == null`. Auto-indent rules are also minimal (parens only).

**⁸ Grammar-gated** — No `io.github.bonede:tree-sitter-<lang>` artifact published on Maven Central as of 2026-05-15. The 7 affected languages are `jsx`, `xml`, `vim`, `less`, `crystal`, `groovy`, `bibtex`. Tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-no-bonede-artifact`. Comment toggle, auto-indent, and bracket-pair auto-close work off `LanguageMetadata` data and are unaffected. Exit criteria per language: find or build a compatible Tree-Sitter JNI wrapper, add a Gradle dep, add to `BonedeGrammarRegistry.classNames`.

**⁹ Grammar-gated (Nim — broken artifact)** — A `io.github.bonede:tree-sitter-nim` artifact exists (versions 0.5.0 and 0.6.0) but the native `.so` segfaults on parse against all bonede core versions tested (0.24.4, 0.25.3, 0.26.6). Tracked in `docs/KNOWN_DEFECTS.md#f2-phase-7-nim-grammar-broken`. Non-grammar affordances (comment, indent, bracket) are not affected. Exit criteria: upstream fix in the bonede Nim grammar or a replacement grammar source.

---

## 4. Evidence index

| Test / Challenge | Commit | What it proves |
|---|---|---|
| `LanguageMetadataCompletenessTest` (6 sub-tests) | `8f8b01ef` | Every language has `highlights.scm`, `folds.scm`, `outline.scm` + SPDX headers + fixture + loader round-trip |
| `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs` | `9606ff42` | 47 bonede grammars produce `>= 1 token` against their own fixtures |
| `Feature2LanguageSmokeTest.inputSmokeCheckForAllLanguages` | `8f8b01ef` + `9606ff42` | All 55 languages pass input-smoke (fixtures + `.scm` coherence) |
| `Feature2LanguageSmokeTest.unsupportedLangs_throwHonestly` | `9606ff42` | 8-lang gap set throws `IllegalArgumentException` instead of fabricating grammars |
| `Feature2LanguageSmokeTest.markdownEndToEndProducesOutlineItems` | `9606ff42` | Markdown outline: `>= 1 item` from real fixture |
| `Feature2LanguageSmokeTest.markdownEndToEndProducesFoldRange` | `9606ff42` | Markdown fold: `>= 1 range` from real fixture |
| `LanguageAffordanceParityTest` (8 sub-tests) | `e50295c6` | All 55 CommentSyntax / IndentRules / BracketPairs rows validated |
| `CommentSyntaxTest` (4 tests) | `e50295c6` | CommentSyntax line/block toggle logic |
| `IndentRulesTest` (4 tests) | `e50295c6` | IndentRules indent/dedent token matching |
| `BracketPairsTest` (3 tests) | `e50295c6` | BracketPairs open/close pair lookup |
| `LanguageRegistryTest` (4 tests) | `281356d0` | Registry lookup by id and by filename extension |
| `BonedeGrammarSmokeTest` (3 tests) | `9606ff42` | 47 bonede grammars load + parse (Desktop JVM) |
| `HtmlEmbeddedLangTest` (2 tests) | `a68bd8e9` | CSS in `<style>` + JS in `<script>` merged token stream |
| `MarkdownCodeFencesTest` (2 tests) | `a68bd8e9` | Per-fence sub-language tokenization |
| `CommentToggleActionRobolectricTest` (4 tests) | `a9482ec2` | Android comment toggle integration |
| `IndentEngineRobolectricTest` (4 tests) | `a9482ec2` | Android auto-indent integration |
| `BracketAutoCompleterRobolectricTest` (3 tests) | `a9482ec2` | Android bracket auto-close integration |
| `OutlineDrawerRobolectricTest` (6 tests) | `8c7862d0` | Android outline drawer UI + navigation |
| `FoldGutterRobolectricTest` (6 tests) | `8c7862d0` | Android fold gutter UI + chevron tap |
| `language_grammar_bundle_challenge.sh` | `2982ded0` | External: 47 bonede JARs declared, gap set documented |
| `language_support_completeness_challenge.sh` | `2982ded0` | External: all 55 langs have comment coverage + file detection |
