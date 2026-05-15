/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 5: LspCompletionProvider — Wasm/JS honest stub.
 *
 * Browser Wasm sandbox cannot run native LSP server binaries. Returns
 * emptyList() per CONST-035 honest-degradation policy. Other providers
 * (token + snippet) remain unaffected and continue to serve completions.
 *
 * Cross-platform impact (CONST-037):
 *   - Web/Wasm: this stub — emptyList(), no native subprocess.
 *   - Desktop/Android: see respective actual files.
 *   - iOS: see LspCompletionProvider.ios.kt.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider
import digital.vasic.yole.lsp.LspServerHost

actual class LspCompletionProvider actual constructor(
    @Suppress("UNUSED_PARAMETER") host: LspServerHost,
) : CompletionProvider {
    actual override val id: String = "lsp"
    actual override suspend fun complete(ctx: CompletionContext): List<CompletionItem> = emptyList()
}
