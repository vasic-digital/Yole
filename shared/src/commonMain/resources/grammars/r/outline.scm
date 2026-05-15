; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/r/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; locals.scm
(function_definition) @local.scope

(argument
  name: (identifier) @local.definition)

(parameter
  name: (identifier) @local.definition)

(binary_operator
  lhs: (identifier) @local.definition
  operator: "<-")

(binary_operator
  lhs: (identifier) @local.definition
  operator: "=")

(binary_operator
  operator: "->"
  rhs: (identifier) @local.definition)

(identifier) @local.reference
