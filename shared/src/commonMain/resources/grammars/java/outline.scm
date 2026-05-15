; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/java/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(class_declaration
  name: (identifier) @definition.class)

(interface_declaration
  name: (identifier) @definition.interface)

(record_declaration
  name: (identifier) @definition.class)

(enum_declaration
  name: (identifier) @defintion.class)

(method_declaration
  name: (identifier) @definition.function)

(constructor_declaration
  name: (identifier) @definition.function)

(compact_constructor_declaration
  name: (identifier) @definition.function)

(field_declaration
  declarator: (variable_declarator
    name: (identifier) @definition.constant))

(enum_constant
  name: (identifier) @definition.constant)
