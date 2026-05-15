; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/toml/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(bare_key) @property

[
  (string)
  (quoted_key)
] @string

(boolean) @boolean

(comment) @comment @spell

(escape_sequence) @string.escape

(integer) @number

(float) @number.float

[
  (local_date)
  (local_date_time)
  (local_time)
  (offset_date_time)
] @string.special

"=" @operator

[
  "."
  ","
] @punctuation.delimiter

[
  "["
  "]"
  "[["
  "]]"
  "{"
  "}"
] @punctuation.bracket
