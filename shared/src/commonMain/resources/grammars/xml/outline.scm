; SPDX-FileCopyrightText: nvim-treesitter contributors
; SPDX-License-Identifier: Apache-2.0
; Source: https://github.com/nvim-treesitter/nvim-treesitter/blob/cf12346a3414fa1b06af75c79faebe7f76df080a/queries/xml/locals.scm
; Vendored 2026-05-15 for Yole iter-58 Feature 2 Phase 6.
;
; tags
(elementdecl
  (Name) @local.definition.type)

(elementdecl
  (contentspec
    (children
      (Name) @local.reference)))

(AttlistDecl
  .
  (Name) @local.reference)

(STag
  (Name) @local.reference)

(ETag
  (Name) @local.reference)

(EmptyElemTag
  (Name) @local.reference)

; attributes
(AttDef
  (Name) @local.definition.field)

(Attribute
  (Name) @local.reference)

; entities
(GEDecl
  (Name) @local.definition.macro)

(EntityRef
  (Name) @local.reference)
