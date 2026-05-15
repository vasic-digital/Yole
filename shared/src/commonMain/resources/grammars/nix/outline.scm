; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/nix/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; let bindings
(let_expression
  (binding_set
    (binding
      .
      (attrpath) @local.definition.var))) @local.scope

; rec attrsets
(rec_attrset_expression
  (binding_set
    (binding
      .
      (attrpath) @local.definition.field))) @local.scope

; functions and parameters
(function_expression
  .
  [
    (identifier) @local.definition.parameter
    (formals
      (formal
        .
        (identifier) @local.definition.parameter))
  ]) @local.scope

((formals)
  "@"
  (identifier) @local.definition.parameter) ; I couldn't get this to work properly inside the (function)

(variable_expression
  (identifier) @local.reference)

(inherited_attrs
  attr: (identifier) @local.reference)
