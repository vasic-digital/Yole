; SPDX-FileCopyrightText: helix-editor contributors
; SPDX-License-Identifier: MPL-2.0
; Source: https://github.com/helix-editor/helix/blob/8c41b11607924f7584b77c8a6e6b16439a2f559f/runtime/queries/rust/tags.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
(struct_item
  name: (type_identifier) @definition.struct)

(const_item
  name: (identifier) @definition.constant)

(trait_item
  name: (type_identifier) @definition.interface)

(function_item
  name: (identifier) @definition.function)

(function_signature_item
  name: (identifier) @definition.function)

(enum_item
  name: (type_identifier) @definition.enum)

(enum_variant
  name: (identifier) @definition.struct)

(type_item
  name: (type_identifier) @definition.type)

(mod_item
  name: (identifier) @definition.module)

(macro_definition
  name: (identifier) @definition.macro)
