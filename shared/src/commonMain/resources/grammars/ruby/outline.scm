; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/ruby/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; Method definitions

(
  (comment)* @doc
  .
  [
    (method
      name: (_) @name) @definition.method
    (singleton_method
      name: (_) @name) @definition.method
  ]
  (#strip! @doc "^#\\s*")
  (#select-adjacent! @doc @definition.method)
)

(alias
  name: (_) @name) @definition.method

(setter
  (identifier) @ignore)

; Class definitions

(
  (comment)* @doc
  .
  [
    (class
      name: [
        (constant) @name
        (scope_resolution
          name: (_) @name)
      ]) @definition.class
    (singleton_class
      value: [
        (constant) @name
        (scope_resolution
          name: (_) @name)
      ]) @definition.class
  ]
  (#strip! @doc "^#\\s*")
  (#select-adjacent! @doc @definition.class)
)

; Module definitions

(
  (module
    name: [
      (constant) @name
      (scope_resolution
        name: (_) @name)
    ]) @definition.module
)

; Calls

(call method: (identifier) @name) @reference.call

(
  [(identifier) (constant)] @name @reference.call
  (#is-not? local)
  (#not-match? @name "^(lambda|load|require|require_relative|__FILE__|__LINE__)$")
)
