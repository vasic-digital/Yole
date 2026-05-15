; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/c_sharp/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
body: [
  (declaration_list)
  (switch_body)
  (enum_member_declaration_list)
] @fold

accessors: (accessor_list) @fold

initializer: (initializer_expression) @fold

[
  (block)
  (preproc_if)
  (preproc_elif)
  (preproc_else)
  (using_directive)+
] @fold
