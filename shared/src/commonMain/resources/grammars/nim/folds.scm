; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/nim/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
[
  (const_section)
  (var_section)
  (let_section)
  (type_section)
  (using_section)
  (object_declaration)
  (tuple_type)
  (enum_declaration)
  (case)
  (if)
  (when)
  (conditional_declaration)
  (variant_declaration)
  (of_branch)
  (elif_branch)
  (else_branch)
  (for)
  (while)
  (block)
  (static_statement)
  (pragma_statement)
  (try)
  (except_branch)
  (finally_branch)
  (do_block)
  (call
    (argument_list
      (statement_list)))
  (proc_declaration)
  (func_declaration)
  (method_declaration)
  (iterator_declaration)
  (converter_declaration)
  (template_declaration)
  (macro_declaration)
  (proc_expression)
  (func_expression)
  (iterator_expression)
  (concept_declaration)
] @fold
