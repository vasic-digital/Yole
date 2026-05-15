; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/objc/folds.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: c

[
  (class_declaration)
  (class_interface)
  (class_implementation)
  (protocol_declaration)
  (property_declaration)
  (method_declaration)
  (struct_declaration)
  (struct_declarator)
  (try_statement)
  (catch_clause)
  (finally_clause)
  (throw_statement)
  (block_literal)
  (ms_asm_block)
  (dictionary_literal)
  (array_literal)
] @fold
