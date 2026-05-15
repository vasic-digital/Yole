; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/regex/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; Forked from tree-sitter-regex
; The MIT License (MIT) Copyright (c) 2014 Max Brunsfeld
[
  "("
  ")"
  "(?"
  "(?:"
  "(?<"
  "<"
  ">"
  "["
  "]"
  "{"
  "}"
] @punctuation.bracket

(group_name) @property

; These are escaped special characters that lost their special meaning
; -> no special highlighting
(identity_escape) @string.regexp

(class_character) @constant

(decimal_digits) @number

[
  (control_letter_escape)
  (character_class_escape)
  (control_escape)
  (boundary_assertion)
  (non_boundary_assertion)
] @string.escape

[
  "*"
  "+"
  "?"
  "|"
  "="
  "!"
  "-"
  "\\k"
  (lazy)
] @operator

[
  (start_assertion)
  (end_assertion)
  ","
] @punctuation.delimiter

(any_character) @variable.builtin
