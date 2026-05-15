; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/dart/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; Definitions
(function_signature
  name: (identifier) @local.definition.function)

(formal_parameter
  name: (identifier) @local.definition.parameter)

(initialized_variable_definition
  name: (identifier) @local.definition.var)

(initialized_identifier
  (identifier) @local.definition.var)

(static_final_declaration
  (identifier) @local.definition.var)

; References
(identifier) @local.reference

; Scopes
(class_definition
  body: (_) @local.scope)

[
  (block)
  (if_statement)
  (for_statement)
  (while_statement)
  (try_statement)
  (catch_clause)
  (finally_clause)
] @local.scope
