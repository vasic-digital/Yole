; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/javascript/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: ecma,jsx

; Both properties are matched here.
;
;   class Foo {
;     this.#bar = "baz";
;     this.quuz = "qux";
;   }
(field_definition
  property: [
    (property_identifier)
    (private_property_identifier)
  ] @local.definition.var)

; this.foo = "bar"
(assignment_expression
  left: (member_expression
    object: (this)
    property: (property_identifier) @local.definition.var))

(formal_parameters
  (identifier) @local.definition.parameter)

; function(arg = []) {
(formal_parameters
  (assignment_pattern
    left: (identifier) @local.definition.parameter))

; x => x
(arrow_function
  parameter: (identifier) @local.definition.parameter)

; ({ a }) => null
(formal_parameters
  (object_pattern
    (shorthand_property_identifier_pattern) @local.definition.parameter))

; ({ a: b }) => null
(formal_parameters
  (object_pattern
    (pair_pattern
      value: (identifier) @local.definition.parameter)))

; ([ a ]) => null
(formal_parameters
  (array_pattern
    (identifier) @local.definition.parameter))

(formal_parameters
  (rest_pattern
    (identifier) @local.definition.parameter))

; Both methods are matched here.
;
;   class Foo {
;     #bar(x) { x }
;     baz(y) { y }
;   }
(method_definition
  [
    (property_identifier)
    (private_property_identifier)
  ] @local.definition.function
  (#set! definition.var.scope parent))

; this.foo()
(member_expression
  object: (this)
  property: (property_identifier) @local.reference)
