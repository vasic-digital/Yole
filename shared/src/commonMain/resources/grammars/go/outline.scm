; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/go/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(
  (comment)* @doc
  .
  (function_declaration
    name: (identifier) @name) @definition.function
  (#strip! @doc "^//\\s*")
  (#select-adjacent! @doc @definition.function)
)

(
  (comment)* @doc
  .
  (method_declaration
    name: (field_identifier) @name) @definition.method
  (#strip! @doc "^//\\s*")
  (#select-adjacent! @doc @definition.method)
)

(call_expression
  function: [
    (identifier) @name
    (parenthesized_expression (identifier) @name)
    (selector_expression field: (field_identifier) @name)
    (parenthesized_expression (selector_expression field: (field_identifier) @name))
  ]) @reference.call

(const_spec
  name: (identifier) @name) @definition.constant

(type_spec
  name: (type_identifier) @name) @definition.type

(type_alias
  name: (type_identifier) @name) @definition.type

(type_identifier) @name @reference.type
