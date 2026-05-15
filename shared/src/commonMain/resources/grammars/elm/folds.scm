; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/elm/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
((function_call_expr) @_fn
  (#not-has-parent? @_fn parenthesized_expr)) @fold

[
  (case_of_branch)
  (case_of_expr)
  (value_declaration)
  (type_declaration)
  (type_alias_declaration)
  (list_expr)
  (record_expr)
  (parenthesized_expr)
  (import_clause)+
] @fold
