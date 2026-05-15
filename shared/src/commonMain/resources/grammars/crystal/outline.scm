; SPDX-FileCopyrightText: 2026 Milos Vasic
; SPDX-License-Identifier: Apache-2.0
; Yole-authored stub for iter-58 Feature 2 Phase 6.
; Upstream helix runtime/queries/crystal/tags.scm at commit
; 8c41b11607924f7584b77c8a6e6b16439a2f559f is itself empty (body is
; just `;;` — helix tracks the file as a placeholder pending real
; community-authored content). Vendoring the empty upstream as-is
; would inherit a no-match query that effectively returns nothing
; anyway. We replace it with this explicit stub so anti-bluff
; (CONST-035) consumers can detect "no real outline coverage" via
; the `Yole-authored stub` marker. When helix lands real crystal
; tags.scm content, re-vendor verbatim.
;
; Capture name: @definition.symbol (empty body — emits no matches).
