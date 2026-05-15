; Vendored verbatim from helix-editor/helix
;   runtime/queries/markdown/tags.scm
;   https://github.com/helix-editor/helix/blob/master/runtime/queries/markdown/tags.scm
; SPDX-License-Identifier: MPL-2.0
; SPDX-FileCopyrightText: helix-editor contributors
; Yole iter-58 Phase 3: bundled for the markdown outline-query runner.
; Per research-report.md §2: helix's `tags.scm` is vendored as Yole's
; `outline.scm` because it carries github-linguist-compatible captures
; suitable for Yole's outline/breadcrumb UI.
; Captures: @definition.section

(atx_heading) @definition.section
