/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 2: HoverInfo — forward-declared data class.
 *
 * Phase 3 finalizes the LSP4J→HoverInfo mapping (MarkedString /
 * MarkupContent sanitization). Phase 2 uses this as the return
 * type of LspServerHost.hover() so callers and stubs compile.
 *
 * Cross-platform (CONST-037):
 *   - Desktop/Android: populated by LspServerHost.hover() JVM actual.
 *   - iOS/Wasm:        hover() returns null; HoverInfo never instantiated.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Sanitized hover result returned by [LspServerHost.hover].
 *
 * @param contents Markdown string extracted from the LSP hover response.
 *                 Phase 3 HoverMarkdownRenderer will parse this via Flexmark.
 * @param range    Source range for the hovered token, or null when not
 *                 provided by the LSP server (Phase 3 finalizes via LspRangeMapping).
 */
data class HoverInfo(val contents: String, val range: IntRange? = null)
