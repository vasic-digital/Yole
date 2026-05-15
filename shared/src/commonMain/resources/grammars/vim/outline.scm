; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/vim/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
[
  (script_file)
  (function_definition)
] @local.scope

(function_declaration
  name: (identifier) @local.definition.function)

(function_declaration
  parameters: (parameters
    (identifier) @local.definition.parameter))

(let_statement
  [
    (scoped_identifier)
    (identifier)
  ] @local.definition.var)

(identifier) @local.reference
