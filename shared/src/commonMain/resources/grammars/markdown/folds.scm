; SPDX-License-Identifier: Apache-2.0
; SPDX-FileCopyrightText: 2026 Milos Vasic
;
; Yole iter-58 Phase 3: markdown fold-query, Yole-authored to match the
; node types emitted by the bundled tree-sitter-markdown 0.7.1 grammar
; (the JAR pinned via libs.versions.toml). Captures: @fold.
;
; History + attribution:
;   Upstream nvim-treesitter/runtime/queries/markdown/folds.scm
;   (Apache-2.0, https://github.com/nvim-treesitter/nvim-treesitter)
;   targets a NEWER tree-sitter-markdown grammar that emits `(section)`
;   and `(list)` nodes. Those node types DO NOT EXIST in the bundled
;   0.7.1 grammar (verified empirically via TSQuery rejection with
;   `Invalid query: TSQueryErrorField at offset N`). The actual node
;   types this grammar emits are:
;       document, atx_heading, paragraph, fenced_code_block,
;       indented_code_block, tight_list, list_item, ...
;   This file uses only node types confirmed valid in that grammar.
;   When iter-58 Phase 6 lands the build-from-source matrix for the
;   55-grammar set, we re-vendor upstream folds.scm verbatim (and
;   honor Apache-2.0 attribution) for the new grammars that support
;   `(section)`.
;
; Per CONST-035 anti-bluff: this query MUST produce >=1 capture for
; the test input `# Heading\n\nLine 1\nLine 2\n` -- and it does,
; because `paragraph` matches the two-line body of every section.
; That property is exercised by FoldQueryRunnerTest.

([
  (fenced_code_block)
  (indented_code_block)
  (paragraph)
  (tight_list)
] @fold)
