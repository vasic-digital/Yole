; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/lua/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; Scopes
[
  (chunk)
  (do_statement)
  (while_statement)
  (repeat_statement)
  (if_statement)
  (for_statement)
  (function_declaration)
  (function_definition)
] @local.scope

; Definitions
(assignment_statement
  (variable_list
    (identifier) @local.definition.var))

(assignment_statement
  (variable_list
    (dot_index_expression
      .
      (_) @local.definition.associated
      (identifier) @local.definition.var)))

((function_declaration
  name: (identifier) @local.definition.function)
  (#set! definition.function.scope "parent"))

((function_declaration
  name: (dot_index_expression
    .
    (_) @local.definition.associated
    (identifier) @local.definition.function))
  (#set! definition.method.scope "parent"))

((function_declaration
  name: (method_index_expression
    .
    (_) @local.definition.associated
    (identifier) @local.definition.method))
  (#set! definition.method.scope "parent"))

(for_generic_clause
  (variable_list
    (identifier) @local.definition.var))

(for_numeric_clause
  name: (identifier) @local.definition.var)

(parameters
  (identifier) @local.definition.parameter)

; References
(identifier) @local.reference
