; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/less/highlights.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: css

[
  "@import"
  "@namespace"
  "@charset"
] @keyword

(js_comment) @comment

(function_name) @function

[
  ">="
  "<="
] @operator

(plain_value) @string

(keyword_query) @function

(identifier) @variable

(variable) @variable

(arguments
  (variable) @variable.parameter)

[
  "["
  "]"
] @punctuation.bracket

(import_statement
  (identifier) @function)
