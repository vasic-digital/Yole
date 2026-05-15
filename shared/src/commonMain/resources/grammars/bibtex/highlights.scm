; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/bibtex/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; CREDITS @pfoerster (adapted from https://github.com/latex-lsp/tree-sitter-bibtex)
[
  (string_type)
  (preamble_type)
  (entry_type)
] @keyword

[
  (junk)
  (comment)
] @comment

(comment) @spell

[
  "="
  "#"
] @operator

(command) @function.builtin

(number) @number

(field
  name: (identifier) @property)

(token
  (identifier) @variable.parameter)

[
  (brace_word)
  (quote_word)
] @string

((field
  name: (identifier) @_url
  value: (value
    (token
      (brace_word) @string.special.url)))
  (#any-of? @_url "url" "doi"))

[
  (key_brace)
  (key_paren)
] @markup.link.label

(string
  name: (identifier) @constant)

[
  "{"
  "}"
  "("
  ")"
] @punctuation.bracket

"," @punctuation.delimiter
