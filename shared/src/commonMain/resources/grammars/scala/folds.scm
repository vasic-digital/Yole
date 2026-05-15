; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/scala/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(call_expression
  (block) @fold)

[
  (class_definition)
  (trait_definition)
  (object_definition)
  (function_definition)
  (val_definition)
  (import_declaration)
  (while_expression)
  (do_while_expression)
  (for_expression)
  (try_expression)
  (match_expression)
] @fold
