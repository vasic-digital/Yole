; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/erlang/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; Modules
(attribute
  name: (atom) @_attr
  (arguments (atom) @definition.module)
 (#eq? @_attr "module"))

; Constants
((attribute
    name: (atom) @_attr
    (arguments
      .
      [
        (atom) @definition.constant
        (call function: [(variable) (atom)] @definition.macro)
      ]))
 (#eq? @_attr "define"))

; Record definitions
((attribute
   name: (atom) @_attr
   (arguments
     .
     (atom) @definition.struct))
 (#eq? @_attr "record"))

(attribute
  name: (atom) @_attr
  (arguments
    .
    [(atom) (macro)] ; Record name
    [
      ; Just the field name:
      (tuple (atom)? @definition.field)
      ; Field name, type OR default:
      (tuple
        (binary_operator
          left: (atom) @definition.field
          operator: ["=" "::"]))
      ; Field name, type AND default:
      (tuple
        (binary_operator
          left:
            (binary_operator
              left: (atom) @definition.field
              operator: "=")
          operator: "::"))
    ])
 (#eq? @_attr "record"))

; Function specs
((attribute
    name: (atom) @_attr
    (stab_clause name: (atom) @definition.interface))
 (#any-of? @_attr "spec" "callback"))

; Types
((attribute
    name: (atom) @_attr
    (arguments
      (binary_operator
        left: [
          (atom) @definition.type
          (call function: (atom) @definition.type)
        ]
        operator: "::")))
 (#any-of? @_attr "type" "opaque"))

; Functions
(function_clause name: (atom) @definition.function)
