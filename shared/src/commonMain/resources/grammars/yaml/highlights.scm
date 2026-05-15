; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/yaml/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(boolean_scalar) @boolean

(null_scalar) @constant.builtin

(double_quote_scalar) @string

(single_quote_scalar) @string

((block_scalar) @string
  (#set! priority 99))

(string_scalar) @string

(escape_sequence) @string.escape

(integer_scalar) @number

(float_scalar) @number

(comment) @comment @spell

[
  (anchor_name)
  (alias_name)
] @label

(tag) @type

[
  (yaml_directive)
  (tag_directive)
  (reserved_directive)
] @keyword.directive

(block_mapping_pair
  key: (flow_node
    [
      (double_quote_scalar)
      (single_quote_scalar)
    ] @property))

(block_mapping_pair
  key: (flow_node
    (plain_scalar
      (string_scalar) @property)))

(flow_mapping
  (_
    key: (flow_node
      [
        (double_quote_scalar)
        (single_quote_scalar)
      ] @property)))

(flow_mapping
  (_
    key: (flow_node
      (plain_scalar
        (string_scalar) @property))))

[
  ","
  "-"
  ":"
  ">"
  "?"
  "|"
] @punctuation.delimiter

[
  "["
  "]"
  "{"
  "}"
] @punctuation.bracket

[
  "*"
  "&"
  "---"
  "..."
] @punctuation.special

; help deal with for yaml's norway problem https://www.bram.us/2022/01/11/yaml-the-norway-problem/
; only using `true` and `false`, since Treesitter parser targets YAML spec 1.2 https://github.com/nvim-treesitter/nvim-treesitter/pull/7512#issuecomment-2565397302
(block_mapping_pair
  value: (block_node
    (block_sequence
      (block_sequence_item
        (flow_node
          (plain_scalar
            (string_scalar) @boolean
            (#any-of? @boolean "TRUE" "FALSE" "True" "False")))))))

(block_mapping_pair
  value: (flow_node
    (plain_scalar
      (string_scalar) @boolean
      (#any-of? @boolean "TRUE" "FALSE" "True" "False"))))
