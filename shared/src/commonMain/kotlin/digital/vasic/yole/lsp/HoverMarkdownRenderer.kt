/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer — expect declaration.
 *
 * Flexmark is JVM-only; iOS and Wasm targets use an honest
 * FallbackText stub (CONST-035: raw text rather than silent empty).
 *
 * Cross-platform impact (CONST-037):
 *   - Android:  JVM actual — full Flexmark walker.
 *   - Desktop:  JVM actual — full Flexmark walker.
 *   - iOS:      stub — emits FallbackText(markdown) to show raw text.
 *   - Web/Wasm: stub — emits FallbackText(markdown) to show raw text.
 *
 * Submodules: not touched (CONST-038).
 *
 * Mutation procedure (CONST-035):
 *   Stub render() in the Desktop actual to always return
 *   [HoverBlock.FallbackText(markdown)]. Re-run desktopTest:
 *   ≥ 4 of the 8 tests FAIL (paragraph, heading, fencedCodeBlock,
 *   mixedContent_orderedCorrectly all expect non-FallbackText types).
 *   Revert stub → all 8 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Parses an LSP hover Markdown string into a list of [HoverBlock]
 * items ready for Compose rendering.
 *
 * Empty input always returns an empty list on all platforms.
 */
expect object HoverMarkdownRenderer {
    fun render(markdown: String): List<HoverBlock>
}
