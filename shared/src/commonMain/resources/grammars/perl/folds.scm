; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/perl/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(comment)+ @fold

(pod) @fold

; fold the block-typed package and class statements only
(package_statement
  (block)) @fold

(class_statement
  (block)) @fold

[
  (subroutine_declaration_statement)
  (method_declaration_statement)
  (conditional_statement)
  (loop_statement)
  (for_statement)
  (cstyle_for_statement)
  (block_statement)
  (defer_statement)
  (phaser_statement)
] @fold

(try_statement
  (block) @fold)

(eval_expression
  (block) @fold)

(anonymous_subroutine_expression) @fold

; perhaps folks want to fold these too?
[
  (anonymous_array_expression)
  (anonymous_hash_expression)
] @fold
