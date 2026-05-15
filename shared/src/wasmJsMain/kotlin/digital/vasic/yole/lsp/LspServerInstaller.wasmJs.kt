/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: Wasm/JS stub for LspServerInstaller.
 *
 * Returns NotSupported for all specs. Native LSP server binaries cannot
 * be launched inside a browser Wasm sandbox. This is architecturally
 * permanent — not a deferred feature. Callers should disable LSP
 * completion on Web/Wasm and rely on the token/snippet providers
 * (iter-60).
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.Path

/**
 * Wasm/JS stub: always returns [LspInstallError.NotSupported].
 *
 * Native LSP server binaries cannot be launched inside a browser Wasm
 * sandbox. LSP completion is permanently unavailable on Web/Wasm; callers
 * degrade gracefully to the token/snippet completion providers from iter-60.
 */
actual class LspServerInstaller actual constructor(private val spec: LspServerSpec) {

    actual suspend fun ensureInstalled(): Result<Path> {
        val langId = spec.langIds.firstOrNull() ?: "unknown"
        return Result.failure(LspInstallError.NotSupported(langId, "Web/Wasm"))
    }
}
