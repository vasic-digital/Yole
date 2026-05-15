; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/c/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(function_declarator
  declarator: [(identifier) (field_identifier)] @definition.function)

(preproc_function_def name: (identifier) @definition.function)

(preproc_def name: (identifier) @definition.constant)

(type_definition
  declarator: (type_identifier) @definition.type)

(struct_specifier
  name: (type_identifier) @definition.struct)

(enum_specifier
  name: (type_identifier) @definition.enum)
