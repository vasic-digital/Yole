; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/zig/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
[
  (block)
  (switch_expression)
  (initializer_list)
  (asm_expression)
  (multiline_string)
  (if_statement)
  (while_statement)
  (for_statement)
  (if_expression)
  (else_clause)
  (for_expression)
  (while_expression)
  (if_type_expression)
  (function_signature)
  (parameters)
  (call_expression)
  (struct_declaration)
  (opaque_declaration)
  (enum_declaration)
  (union_declaration)
  (error_set_declaration)
] @fold
