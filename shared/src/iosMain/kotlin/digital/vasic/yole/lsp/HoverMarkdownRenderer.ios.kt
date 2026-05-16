/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer — iOS stub.
 *
 * Flexmark is JVM-only; no Kotlin/Native equivalent exists. This stub
 * returns FallbackText(markdown) so the Phase 6 HoverPopup still shows
 * the raw hover string rather than silently dropping it (CONST-035:
 * honest fallback, not empty-on-failure).
 *
 * Cross-platform impact (CONST-037):
 *   - iOS: this file — FallbackText stub.
 *   - Android/Desktop: full Flexmark walker actuals.
 *   - Wasm: identical FallbackText stub in HoverMarkdownRenderer.wasmJs.kt.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

actual object HoverMarkdownRenderer {
    actual fun render(markdown: String): List<HoverBlock> =
        if (markdown.isEmpty()) emptyList() else listOf(HoverBlock.FallbackText(markdown))
}
