; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/cpp/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; inherits: c

(function_declarator
  declarator: (qualified_identifier name: (identifier) @definition.function))

(class_specifier
  name: (type_identifier) @definition.class
  body: (field_declaration_list))
