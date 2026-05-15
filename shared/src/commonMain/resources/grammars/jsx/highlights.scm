; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/javascript/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: ecma,jsx

; Parameters
(formal_parameters
  (identifier) @variable.parameter)

(formal_parameters
  (rest_pattern
    (identifier) @variable.parameter))

; ({ a }) => null
(formal_parameters
  (object_pattern
    (shorthand_property_identifier_pattern) @variable.parameter))

; ({ a = b }) => null
(formal_parameters
  (object_pattern
    (object_assignment_pattern
      (shorthand_property_identifier_pattern) @variable.parameter)))

; ({ a: b }) => null
(formal_parameters
  (object_pattern
    (pair_pattern
      value: (identifier) @variable.parameter)))

; ([ a ]) => null
(formal_parameters
  (array_pattern
    (identifier) @variable.parameter))

; ({ a } = { a }) => null
(formal_parameters
  (assignment_pattern
    (object_pattern
      (shorthand_property_identifier_pattern) @variable.parameter)))

; ({ a = b } = { a }) => null
(formal_parameters
  (assignment_pattern
    (object_pattern
      (object_assignment_pattern
        (shorthand_property_identifier_pattern) @variable.parameter))))

; a => null
(arrow_function
  parameter: (identifier) @variable.parameter)

; optional parameters
(formal_parameters
  (assignment_pattern
    left: (identifier) @variable.parameter))

; punctuation
(optional_chain) @punctuation.delimiter
