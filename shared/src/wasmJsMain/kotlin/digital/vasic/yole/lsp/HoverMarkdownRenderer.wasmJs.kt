/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer — Wasm/JS stub.
 *
 * Flexmark is JVM-only. The Wasm target renders raw hover text as
 * FallbackText so the Phase 6 HoverPopup shows *something* rather
 * than silently dropping hover content (CONST-035: honest fallback).
 *
 * Cross-platform impact (CONST-037):
 *   - Web/Wasm: this file — FallbackText stub.
 *   - Android/Desktop: full Flexmark walker actuals.
 *   - iOS: identical FallbackText stub in HoverMarkdownRenderer.ios.kt.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

actual object HoverMarkdownRenderer {
    actual fun render(markdown: String): List<HoverBlock> =
        if (markdown.isEmpty()) emptyList() else listOf(HoverBlock.FallbackText(markdown))
}
