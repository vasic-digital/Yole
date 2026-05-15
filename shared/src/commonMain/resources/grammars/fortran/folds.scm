; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/fortran/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; by @oponkork
[
  (if_statement)
  (where_statement)
  (enum_statement)
  (do_loop_statement)
  (derived_type_definition)
  (function)
  (subroutine)
  (interface)
] @fold
