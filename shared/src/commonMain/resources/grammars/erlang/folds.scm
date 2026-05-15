; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/erlang/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
[
  (fun_decl)
  (anonymous_fun)
  (case_expr)
  (maybe_expr)
  (map_expr)
  (export_attribute)
  (export_type_attribute)
] @fold
